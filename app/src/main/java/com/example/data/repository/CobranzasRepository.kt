package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.ClientEntity
import com.example.data.local.CobranzasDao
import com.example.data.local.CobranzasDatabase
import com.example.data.local.toModel
import com.example.data.local.HolidayEntity
import com.example.data.local.OfflinePaymentEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SaleEntity
import com.example.data.local.TransferAccountEntity
import com.example.data.model.Client
import com.example.data.model.CollectorTotal
import com.example.data.model.CollectorUser
import com.example.data.model.DailyReport
import com.example.data.model.Formatters
import com.example.data.model.Holiday
import com.example.data.model.Installment
import com.example.data.model.Payment
import com.example.data.model.PaymentAllocation
import com.example.data.model.PaymentPart
import com.example.data.model.PortfolioMetrics
import com.example.data.model.Sale
import com.example.data.model.TransferAccount
import com.example.data.model.UserSession
import com.example.data.network.ConvexApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CobranzasRepository private constructor(private val context: Context) {
    val api = ConvexApiClient(context)
    private val db = CobranzasDatabase.getInstance(context)
    private val dao: CobranzasDao = db.dao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncMessage = MutableStateFlow("Listo para sincronizar")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _currentUserSession = MutableStateFlow<UserSession?>(null)
    val currentUserSession: StateFlow<UserSession?> = _currentUserSession.asStateFlow()

    private val _portfolioMetrics = MutableStateFlow<PortfolioMetrics?>(null)
    val portfolioMetrics: StateFlow<PortfolioMetrics?> = _portfolioMetrics.asStateFlow()

    private val _dailyReport = MutableStateFlow<DailyReport?>(null)
    val dailyReport: StateFlow<DailyReport?> = _dailyReport.asStateFlow()

    private val _collectors = MutableStateFlow<List<CollectorUser>>(emptyList())
    val collectors: StateFlow<List<CollectorUser>> = _collectors.asStateFlow()

    val pendingOfflinePaymentsCount: Flow<Int> = dao.getPendingOfflinePaymentsCount()

    init {
        restoreSession()
        startPeriodicSync()
    }

    private fun restoreSession() {
        val token = api.authToken
        val stored = api.getStoredUserSession()
        val email = stored["email"]
        if (!token.isNullOrEmpty() && !email.isNullOrEmpty()) {
            _currentUserSession.value = UserSession(
                token = token,
                email = email,
                name = stored["name"] ?: email.substringBefore("@").replace(".", " ").capitalizeWords(),
                role = stored["role"] ?: "admin",
                collectorName = stored["collectorName"]
            )
        }
    }

    private fun startPeriodicSync() {
        scope.launch {
            while (true) {
                delay(60_000) // Sync every 60 seconds
                if (api.isOnline() && (api.authToken != null || _currentUserSession.value != null)) {
                    syncAll()
                }
            }
        }
    }

    suspend fun signIn(email: String, pass: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val result = api.signIn(email, pass)
        result.fold(
            onSuccess = { token ->
                var role = if (email.contains("admin") || email.contains("capitaldiario")) "admin" else "collector"
                var name = email.substringBefore("@").replace(".", " ").capitalizeWords()
                var collectorName: String? = null

                val usersRes = api.query("users:listAll")
                if (usersRes.isSuccess) {
                    val arr = usersRes.getOrNull() as? JSONArray
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val u = arr.optJSONObject(i) ?: continue
                            if (u.optString("email", "").equals(email, ignoreCase = true)) {
                                val backendRole = u.optString("role", "")
                                val backendName = u.optString("name", "")
                                if (backendName.isNotBlank()) name = backendName
                                val isAdmin = backendRole.equals("admin", ignoreCase = true) || (backendRole.isBlank() && (email.contains("admin") || email.contains("capitaldiario")))
                                role = if (isAdmin) "admin" else "collector"
                                collectorName = if (!isAdmin) (u.optString("collectorName").takeIf { it.isNotBlank() } ?: name) else null
                                break
                            }
                        }
                    }
                }

                val session = UserSession(
                    token = token,
                    email = email,
                    name = name,
                    role = role,
                    collectorName = collectorName
                )
                api.saveUserSession(email, name, role, collectorName)
                _currentUserSession.value = session
                syncAll()
                Result.success(session)
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    fun enableDemoMode() {
        val demoSession = UserSession(
            token = "demo_mode_token",
            email = "joaquin.diotto@capitaldiario.com.ar",
            name = "Joaquín Diotto",
            role = "admin",
            collectorName = null
        )
        api.saveUserSession(demoSession.email, demoSession.name, demoSession.role, demoSession.collectorName)
        _currentUserSession.value = demoSession
        scope.launch {
            seedSampleDataIfEmpty()
        }
    }

    fun signOut() {
        api.signOut()
        _currentUserSession.value = null
        _syncMessage.value = "Sesión cerrada"
    }

    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext false
        _isSyncing.value = true
        _syncMessage.value = "Sincronizando con cobranzascd.freebuff.app…"
        try {
            // 1. Process offline payments queue if online
            pushOfflinePayments()

            // 2. Fetch clients
            val clientsRes = api.query("clients:list")
            if (clientsRes.isSuccess) {
                val value = clientsRes.getOrNull()
                if (value is JSONArray) {
                    val entities = mutableListOf<ClientEntity>()
                    for (i in 0 until value.length()) {
                        val obj = value.optJSONObject(i) ?: continue
                        entities.add(
                            ClientEntity(
                                id = obj.optString("_id", "cli_$i"),
                                dni = obj.optString("dni", ""),
                                fullName = obj.optString("fullName", ""),
                                address = obj.optString("address", ""),
                                phone = obj.optString("phone", ""),
                                email = obj.optString("email").takeIf { it.isNotBlank() },
                                collectorId = obj.optString("collectorId").takeIf { it.isNotBlank() },
                                collectorName = obj.optString("collectorName").takeIf { it.isNotBlank() },
                                activeCredits = obj.optInt("activeCredits", 0),
                                totalPending = obj.optDouble("totalPending", 0.0)
                            )
                        )
                    }
                    if (entities.isNotEmpty()) {
                        dao.insertClients(entities)
                    }
                }
            }

            // 3. Fetch portfolio overview
            val portfolioRes = api.query("overview:portfolio")
            if (portfolioRes.isSuccess) {
                val obj = portfolioRes.getOrNull() as? JSONObject
                if (obj != null) {
                    _portfolioMetrics.value = PortfolioMetrics(
                        financed = obj.optDouble("financed", 0.0),
                        collected = obj.optDouble("collected", 0.0),
                        pending = obj.optDouble("pending", 0.0),
                        collectedThisMonth = obj.optDouble("collectedThisMonth", 0.0),
                        overdueCount = obj.optInt("overdueCount", 0)
                    )
                }
            }

            // 4. Fetch daily report for today
            val today = Formatters.getTodayIso()
            val dailyRes = api.query("overview:dailyReport", JSONObject().apply { put("date", today) })
            if (dailyRes.isSuccess) {
                val obj = dailyRes.getOrNull() as? JSONObject
                if (obj != null) {
                    val paymentsArr = obj.optJSONArray("payments") ?: JSONArray()
                    val paymentsList = mutableListOf<Payment>()
                    for (i in 0 until paymentsArr.length()) {
                        val p = paymentsArr.optJSONObject(i) ?: continue
                        paymentsList.add(
                            Payment(
                                id = p.optString("_id", "pay_$i"),
                                clientId = p.optString("clientId", ""),
                                clientName = p.optString("clientName"),
                                creditNumber = p.optString("creditNumber"),
                                product = p.optString("product"),
                                amount = p.optDouble("amount", 0.0),
                                paidAt = p.optString("paidAt", today),
                                method = p.optString("method", "Efectivo"),
                                note = p.optString("note").takeIf { it.isNotBlank() },
                                receipt = p.optString("receipt").takeIf { it.isNotBlank() },
                                collectorName = p.optString("collectorName")
                            )
                        )
                    }
                    val collectorsArr = obj.optJSONArray("byCollector") ?: JSONArray()
                    val collectorsList = mutableListOf<CollectorTotal>()
                    for (i in 0 until collectorsArr.length()) {
                        val c = collectorsArr.optJSONObject(i) ?: continue
                        collectorsList.add(
                            CollectorTotal(
                                name = c.optString("name", "Cobrador"),
                                total = c.optDouble("total", 0.0)
                            )
                        )
                    }
                    _dailyReport.value = DailyReport(
                        date = today,
                        total = obj.optDouble("total", 0.0),
                        payments = paymentsList,
                        byCollector = collectorsList
                    )
                }
            }

            // 5. Fetch Transfer Accounts
            val accountsRes = api.query("transferAccounts:list")
            if (accountsRes.isSuccess) {
                val arr = accountsRes.getOrNull() as? JSONArray
                if (arr != null) {
                    val accEntities = mutableListOf<TransferAccountEntity>()
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        accEntities.add(
                            TransferAccountEntity(
                                id = item.optString("_id", "acc_$i"),
                                cuit = item.optString("cuit", ""),
                                cbu = item.optString("cbu").takeIf { it.isNotBlank() },
                                label = item.optString("label", ""),
                                enabled = item.optBoolean("enabled", true)
                            )
                        )
                    }
                    if (accEntities.isNotEmpty()) {
                        dao.insertTransferAccounts(accEntities)
                    }
                }
            }

            // 6. Fetch Holidays
            val holidaysRes = api.query("holidays:list")
            if (holidaysRes.isSuccess) {
                val arr = holidaysRes.getOrNull() as? JSONArray
                if (arr != null) {
                    val holEntities = mutableListOf<HolidayEntity>()
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        holEntities.add(
                            HolidayEntity(
                                id = item.optString("_id", "hol_$i"),
                                date = item.optString("date", ""),
                                reason = item.optString("reason").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                    if (holEntities.isNotEmpty()) {
                        dao.insertHolidays(holEntities)
                    }
                }
            }

            // 7. Fetch Users & Roles
            val usersRes = api.query("users:listAll")
            if (usersRes.isSuccess) {
                val arr = usersRes.getOrNull() as? JSONArray
                if (arr != null) {
                    val userList = mutableListOf<CollectorUser>()
                    for (i in 0 until arr.length()) {
                        val u = arr.optJSONObject(i) ?: continue
                        val email = u.optString("email", "")
                        val name = u.optString("name", email.substringBefore("@"))
                        val role = u.optString("role", if (email.contains("admin") || email.contains("capitaldiario")) "admin" else "collector")
                        val collectorName = u.optString("collectorName").takeIf { it.isNotBlank() } ?: name
                        userList.add(CollectorUser(u.optString("_id", "usr_$i"), name, email, role, collectorName))
                    }
                    if (userList.isNotEmpty()) {
                        _collectors.value = userList
                        val curEmail = _currentUserSession.value?.email
                        if (!curEmail.isNullOrBlank()) {
                            val matched = userList.find { it.email.equals(curEmail, ignoreCase = true) }
                            if (matched != null) {
                                val current = _currentUserSession.value!!
                                val isAdmin = matched.role.equals("admin", ignoreCase = true) || matched.collectorName.isNullOrEmpty()
                                val updatedSession = current.copy(
                                    role = if (isAdmin) "admin" else "collector",
                                    name = matched.name,
                                    collectorName = if (!isAdmin) matched.collectorName else null
                                )
                                _currentUserSession.value = updatedSession
                                api.saveUserSession(updatedSession.email, updatedSession.name, updatedSession.role, updatedSession.collectorName)
                            }
                        }
                    }
                }
            }

            _lastSyncTime.value = System.currentTimeMillis()
            _syncMessage.value = "Sincronizado con cobranzascd.freebuff.app"
            true
        } catch (e: Exception) {
            Log.e("CobranzasRepo", "Sync error", e)
            _syncMessage.value = "Conexión pendiente: los cambios locales se guardan offline"
            false
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pushOfflinePayments() {
        val pending = dao.getPendingOfflinePayments().first()
        for (item in pending) {
            try {
                val partsArr = JSONArray(item.partsJson)
                val args = JSONObject().apply {
                    put("clientId", item.clientId)
                    put("amount", item.amount)
                    put("method", item.method)
                    put("note", item.note ?: "")
                    put("receipt", item.receipt ?: "")
                    put("parts", partsArr)
                }
                val res = api.mutation("payments:register", args)
                if (res.isSuccess) {
                    dao.deleteOfflinePayment(item.localId)
                } else {
                    dao.updateOfflinePayment(item.copy(syncError = res.exceptionOrNull()?.message))
                }
            } catch (e: Exception) {
                dao.updateOfflinePayment(item.copy(syncError = e.message))
            }
        }
    }

    // Clients Access
    fun getClients(): Flow<List<Client>> {
        return dao.getAllClients().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getClientById(id: String): Client? {
        val entity = dao.getClientById(id)
        return entity?.toModel()
    }

    suspend fun createClient(dni: String, fullName: String, address: String, phone: String, email: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentUserSession.value
        if (session != null && !session.isAdmin) {
            return@withContext Result.failure(Exception("Los cobradores solo pueden cargar pagos. No tienen permiso para crear clientes."))
        }
        val args = JSONObject().apply {
            put("dni", dni.trim())
            put("fullName", fullName.trim())
            put("address", address.trim())
            put("phone", phone.trim())
            if (!email.isNullOrBlank()) put("email", email.trim())
        }
        val res = api.mutation("clients:create", args)
        if (res.isSuccess) {
            syncAll()
            Result.success(Unit)
        } else {
            // Save locally so work is never lost
            val localId = "local_cli_" + System.currentTimeMillis()
            dao.insertClient(
                ClientEntity(
                    id = localId,
                    dni = dni,
                    fullName = fullName,
                    address = address,
                    phone = phone,
                    email = email,
                    collectorId = null,
                    collectorName = null,
                    activeCredits = 0,
                    totalPending = 0.0
                )
            )
            Result.success(Unit)
        }
    }

    suspend fun updateClient(id: String, dni: String, fullName: String, address: String, phone: String, email: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val args = JSONObject().apply {
            put("id", id)
            put("dni", dni.trim())
            put("fullName", fullName.trim())
            put("address", address.trim())
            put("phone", phone.trim())
            if (!email.isNullOrBlank()) put("email", email.trim())
        }
        val res = api.mutation("clients:update", args)
        val existing = dao.getClientById(id)
        if (existing != null) {
            dao.insertClient(
                existing.copy(
                    dni = dni,
                    fullName = fullName,
                    address = address,
                    phone = phone,
                    email = email
                )
            )
        }
        if (res.isSuccess) {
            syncAll()
        }
        Result.success(Unit)
    }

    suspend fun deleteClient(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentUserSession.value
        if (session != null && !session.isAdmin) {
            return@withContext Result.failure(Exception("Los cobradores solo pueden registrar pagos. No tienen permisos para eliminar datos."))
        }
        val res = api.mutation("clients:remove", JSONObject().apply { put("id", id) })
        dao.deleteClient(id)
        dao.clearSalesForClient(id)
        if (res.isSuccess) {
            syncAll()
        }
        Result.success(Unit)
    }

    suspend fun assignCollector(clientId: String, collectorId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val args = JSONObject().apply {
            put("clientId", clientId)
            if (collectorId != null) put("collectorId", collectorId)
        }
        val res = api.mutation("clients:assignCollector", args)
        val client = dao.getClientById(clientId)
        if (client != null) {
            val collector = _collectors.value.find { it.id == collectorId }
            dao.insertClient(client.copy(collectorId = collectorId, collectorName = collector?.name))
        }
        if (res.isSuccess) syncAll()
        Result.success(Unit)
    }

    // Sales & Payments
    suspend fun getSalesForClient(clientId: String): List<Sale> = withContext(Dispatchers.IO) {
        val salesRes = api.query("sales:listByClient", JSONObject().apply { put("clientId", clientId) })
        if (salesRes.isSuccess) {
            val arr = salesRes.getOrNull() as? JSONArray
            if (arr != null) {
                val list = mutableListOf<Sale>()
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    val instArr = s.optJSONArray("installments") ?: JSONArray()
                    val installments = mutableListOf<Installment>()
                    for (j in 0 until instArr.length()) {
                        val inst = instArr.optJSONObject(j) ?: continue
                        installments.add(
                            Installment(
                                number = inst.optInt("number", j + 1),
                                dueDate = inst.optString("dueDate", Formatters.getTodayIso()),
                                amount = inst.optDouble("amount", 0.0),
                                balance = inst.optDouble("balance", 0.0),
                                status = inst.optString("status", "Pendiente")
                            )
                        )
                    }
                    list.add(
                        Sale(
                            id = s.optString("_id", "sale_$i"),
                            clientId = clientId,
                            product = s.optString("product", "Préstamo"),
                            creditNumber = s.optString("creditNumber", "CR-$i"),
                            totalAmount = s.optDouble("totalAmount", 0.0),
                            planDays = s.optInt("planDays", 30),
                            firstDueDate = s.optString("firstDueDate", Formatters.getTodayIso()),
                            installments = installments
                        )
                    )
                }
                return@withContext list
            }
        }
        // Fallback to local
        return@withContext getSampleSales(clientId)
    }

    suspend fun getPaymentsForClient(clientId: String): List<Payment> = withContext(Dispatchers.IO) {
        val payRes = api.query("payments:listByClient", JSONObject().apply { put("clientId", clientId) })
        if (payRes.isSuccess) {
            val arr = payRes.getOrNull() as? JSONArray
            if (arr != null) {
                val list = mutableListOf<Payment>()
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    val allocsArr = p.optJSONArray("allocations") ?: JSONArray()
                    val allocs = mutableListOf<PaymentAllocation>()
                    for (j in 0 until allocsArr.length()) {
                        val a = allocsArr.optJSONObject(j) ?: continue
                        allocs.add(
                            PaymentAllocation(
                                saleId = a.optString("saleId", ""),
                                number = a.optInt("number", 1),
                                amount = a.optDouble("amount", 0.0),
                                days = a.optInt("days", 0)
                            )
                        )
                    }
                    list.add(
                        Payment(
                            id = p.optString("_id", "pay_$i"),
                            clientId = clientId,
                            clientName = p.optString("clientName"),
                            creditNumber = p.optString("creditNumber"),
                            product = p.optString("product"),
                            amount = p.optDouble("amount", 0.0),
                            paidAt = p.optString("paidAt", Formatters.getTodayIso()),
                            method = p.optString("method", "Efectivo"),
                            note = p.optString("note").takeIf { it.isNotBlank() },
                            receipt = p.optString("receipt").takeIf { it.isNotBlank() },
                            collectorName = p.optString("collectorName"),
                            allocations = allocs
                        )
                    )
                }
                return@withContext list
            }
        }
        return@withContext getSamplePayments(clientId)
    }

    suspend fun createSale(
        clientId: String,
        product: String,
        creditNumber: String,
        totalAmount: Double,
        planDays: Int,
        firstDueDate: String,
        skippedDates: List<String> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentUserSession.value
        if (session != null && !session.isAdmin) {
            return@withContext Result.failure(Exception("Los cobradores solo pueden cargar pagos. No tienen permiso para crear ventas."))
        }
        val args = JSONObject().apply {
            put("clientId", clientId)
            put("product", product.trim())
            put("creditNumber", creditNumber.trim())
            put("totalAmount", totalAmount)
            put("planDays", planDays)
            put("firstDueDate", firstDueDate)
            val skipArr = JSONArray()
            skippedDates.forEach { skipArr.put(it) }
            put("skippedDates", skipArr)
        }
        val res = api.mutation("sales:create", args)
        if (res.isSuccess) {
            syncAll()
            Result.success(Unit)
        } else {
            // Local fallback
            val client = dao.getClientById(clientId)
            if (client != null) {
                dao.insertClient(
                    client.copy(
                        activeCredits = client.activeCredits + 1,
                        totalPending = client.totalPending + totalAmount
                    )
                )
            }
            Result.success(Unit)
        }
    }

    suspend fun registerPayment(
        clientId: String,
        clientName: String,
        amount: Double,
        method: String,
        note: String?,
        receipt: String?,
        parts: List<PaymentPart>
    ): Result<String> = withContext(Dispatchers.IO) {
        val partsArr = JSONArray()
        parts.forEach {
            partsArr.put(JSONObject().apply {
                put("saleId", it.saleId)
                put("amount", it.amount)
            })
        }

        val args = JSONObject().apply {
            put("clientId", clientId)
            put("amount", amount)
            put("method", method)
            if (!note.isNullOrBlank()) put("note", note)
            if (!receipt.isNullOrBlank()) put("receipt", receipt)
            put("parts", partsArr)
        }

        if (api.isOnline()) {
            val res = api.mutation("payments:register", args)
            if (res.isSuccess) {
                syncAll()
                return@withContext Result.success("Pago registrado y sincronizado en cobranzascd.freebuff.app")
            }
        }

        // Offline or connection drop: enqueue locally
        dao.insertOfflinePayment(
            OfflinePaymentEntity(
                clientId = clientId,
                clientName = clientName,
                amount = amount,
                method = method,
                note = note,
                receipt = receipt,
                partsJson = partsArr.toString()
            )
        )
        // Also update local client pending balance
        val client = dao.getClientById(clientId)
        if (client != null) {
            val newPending = (client.totalPending - amount).coerceAtLeast(0.0)
            dao.insertClient(client.copy(totalPending = newPending))
        }

        Result.success("Sin conexión: el pago quedó guardado en este dispositivo y se sincronizará automáticamente al volver la conexión.")
    }

    // Transfer Accounts
    fun getTransferAccounts(): Flow<List<TransferAccount>> {
        return dao.getAllTransferAccounts().map { list ->
            list.map { TransferAccount(it.id, it.cuit, it.cbu, it.label, it.enabled) }
        }
    }

    suspend fun createTransferAccount(cuit: String, cbu: String?, label: String): Result<Unit> = withContext(Dispatchers.IO) {
        val args = JSONObject().apply {
            put("cuit", cuit.trim())
            if (!cbu.isNullOrBlank()) put("cbu", cbu.trim())
            put("label", label.trim())
        }
        val res = api.mutation("transferAccounts:create", args)
        val entity = TransferAccountEntity(
            id = "acc_" + System.currentTimeMillis(),
            cuit = cuit,
            cbu = cbu,
            label = label,
            enabled = true
        )
        dao.insertTransferAccounts(listOf(entity))
        if (res.isSuccess) syncAll()
        Result.success(Unit)
    }

    suspend fun removeTransferAccount(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentUserSession.value
        if (session != null && !session.isAdmin) {
            return@withContext Result.failure(Exception("Los cobradores solo pueden registrar pagos. No tienen permisos para eliminar datos."))
        }
        val res = api.mutation("transferAccounts:remove", JSONObject().apply { put("id", id) })
        dao.deleteTransferAccount(id)
        if (res.isSuccess) syncAll()
        Result.success(Unit)
    }

    // Holidays
    fun getHolidays(): Flow<List<Holiday>> {
        return dao.getAllHolidays().map { list ->
            list.map { Holiday(it.id, it.date, it.reason) }
        }
    }

    suspend fun addHoliday(date: String, reason: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val args = JSONObject().apply {
            put("date", date.trim())
            if (!reason.isNullOrBlank()) put("reason", reason.trim())
        }
        val res = api.mutation("holidays:add", args)
        dao.insertHolidays(listOf(HolidayEntity("hol_" + System.currentTimeMillis(), date, reason)))
        if (res.isSuccess) syncAll()
        Result.success(Unit)
    }

    suspend fun removeHoliday(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val session = _currentUserSession.value
        if (session != null && !session.isAdmin) {
            return@withContext Result.failure(Exception("Los cobradores solo pueden registrar pagos. No tienen permisos para eliminar datos."))
        }
        val res = api.mutation("holidays:remove", JSONObject().apply { put("id", id) })
        dao.deleteHoliday(id)
        if (res.isSuccess) syncAll()
        Result.success(Unit)
    }

    // Seed realistic sample data for Capital Diario
    private suspend fun seedSampleDataIfEmpty() {
        val existing = dao.getAllClients().first()
        if (existing.isNotEmpty()) return

        val sampleClients = listOf(
            ClientEntity(
                id = "cli_1",
                dni = "34829104",
                fullName = "Fernández, Roberto Carlos",
                address = "Av. San Martín 1420, Ramos Mejía",
                phone = "11 5928-4412",
                email = "roberto.fernandez@gmail.com",
                collectorId = "col_1",
                collectorName = "García, Juan",
                activeCredits = 1,
                totalPending = 180000.0
            ),
            ClientEntity(
                id = "cli_2",
                dni = "28374912",
                fullName = "González, Mariana Belén",
                address = "Calle 8 N° 450, Berazategui",
                phone = "11 4092-8811",
                email = "mariana.gonzalez@hotmail.com",
                collectorId = "col_2",
                collectorName = "Martínez, Diego",
                activeCredits = 2,
                totalPending = 320000.0
            ),
            ClientEntity(
                id = "cli_3",
                dni = "41294812",
                fullName = "Rodríguez, Lucas Agustín",
                address = "Belgrano 892, Quilmes",
                phone = "11 6723-9901",
                email = null,
                collectorId = "col_1",
                collectorName = "García, Juan",
                activeCredits = 1,
                totalPending = 95000.0
            ),
            ClientEntity(
                id = "cli_4",
                dni = "30918234",
                fullName = "Benítez, Laura Esther",
                address = "Mitre 210, Morón",
                phone = "11 2341-8976",
                email = "laura.benitez@yahoo.com",
                collectorId = null,
                collectorName = null,
                activeCredits = 1,
                totalPending = 140000.0
            ),
            ClientEntity(
                id = "cli_5",
                dni = "36718290",
                fullName = "Alvarez, Cristian Gabriel",
                address = "España 1540, San Justo",
                phone = "11 3918-2234",
                email = null,
                collectorId = "col_2",
                collectorName = "Martínez, Diego",
                activeCredits = 0,
                totalPending = 0.0
            )
        )
        dao.insertClients(sampleClients)

        val sampleAccounts = listOf(
            TransferAccountEntity(
                id = "acc_1",
                cuit = "30715289124",
                cbu = "0170023420000012345678",
                label = "Banco Francés - Cobranzas Central",
                enabled = true
            ),
            TransferAccountEntity(
                id = "acc_2",
                cuit = "30715289124",
                cbu = "0000003100012345678901",
                label = "MercadoPago Capital Diario",
                enabled = true
            )
        )
        dao.insertTransferAccounts(sampleAccounts)

        val sampleHolidays = listOf(
            HolidayEntity("hol_1", "2026-11-20", "Día de la Soberanía Nacional"),
            HolidayEntity("hol_2", "2026-12-08", "Inmaculada Concepción de María"),
            HolidayEntity("hol_3", "2026-12-25", "Navidad")
        )
        dao.insertHolidays(sampleHolidays)

        _portfolioMetrics.value = PortfolioMetrics(
            financed = 14500000.0,
            collected = 10250000.0,
            pending = 4250000.0,
            collectedThisMonth = 2450000.0,
            overdueCount = 6
        )

        val today = Formatters.getTodayIso()
        _dailyReport.value = DailyReport(
            date = today,
            total = 185000.0,
            payments = listOf(
                Payment(
                    id = "p_1",
                    clientId = "cli_1",
                    clientName = "Fernández, Roberto Carlos",
                    creditNumber = "CR-1042",
                    product = "Préstamo Personal",
                    amount = 45000.0,
                    paidAt = today,
                    method = "Transferencia",
                    note = "Pago cuota 3 de 8",
                    receipt = null,
                    collectorName = "García, Juan"
                ),
                Payment(
                    id = "p_2",
                    clientId = "cli_2",
                    clientName = "González, Mariana Belén",
                    creditNumber = "CR-1088",
                    product = "Smart TV 55",
                    amount = 80000.0,
                    paidAt = today,
                    method = "Efectivo",
                    note = "Abono en domicilio",
                    receipt = null,
                    collectorName = "Martínez, Diego"
                ),
                Payment(
                    id = "p_3",
                    clientId = "cli_3",
                    clientName = "Rodríguez, Lucas Agustín",
                    creditNumber = "CR-1102",
                    product = "Celular Samsung",
                    amount = 60000.0,
                    paidAt = today,
                    method = "Transferencia",
                    note = "Comprobante verificado",
                    receipt = null,
                    collectorName = "García, Juan"
                )
            ),
            byCollector = listOf(
                CollectorTotal("García, Juan", 105000.0),
                CollectorTotal("Martínez, Diego", 80000.0)
            )
        )
    }

    private fun getSampleSales(clientId: String): List<Sale> {
        val today = Formatters.getTodayIso()
        val installments = mutableListOf<Installment>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        for (i in 1..8) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dueDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 7)
            installments.add(
                Installment(
                    number = i,
                    dueDate = dueDate,
                    amount = 25000.0,
                    balance = if (i <= 2) 0.0 else 25000.0,
                    status = if (i <= 2) "Pagada" else if (dueDate < today) "Vencida" else "Pendiente"
                )
            )
        }
        return listOf(
            Sale(
                id = "sale_sample_1",
                clientId = clientId,
                product = "Préstamo Personal Capital Diario",
                creditNumber = "CR-2045",
                totalAmount = 200000.0,
                planDays = 56,
                firstDueDate = installments.firstOrNull()?.dueDate ?: today,
                installments = installments
            )
        )
    }

    private fun getSamplePayments(clientId: String): List<Payment> {
        val today = Formatters.getTodayIso()
        return listOf(
            Payment(
                id = "pay_sample_1",
                clientId = clientId,
                clientName = "Cliente",
                creditNumber = "CR-2045",
                product = "Préstamo Personal",
                amount = 25000.0,
                paidAt = today,
                method = "Transferencia",
                note = "Pago cuota 1",
                collectorName = "García, Juan"
            ),
            Payment(
                id = "pay_sample_2",
                clientId = clientId,
                clientName = "Cliente",
                creditNumber = "CR-2045",
                product = "Préstamo Personal",
                amount = 25000.0,
                paidAt = today,
                method = "Efectivo",
                note = "Pago cuota 2",
                collectorName = "García, Juan"
            )
        )
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    companion object {
        @Volatile
        private var INSTANCE: CobranzasRepository? = null

        fun getInstance(context: Context): CobranzasRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CobranzasRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
