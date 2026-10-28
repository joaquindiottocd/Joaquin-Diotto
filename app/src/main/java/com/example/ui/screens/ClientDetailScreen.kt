package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.PdfGenerator
import com.example.util.ReceiptAnalysisResult
import com.example.util.ReceiptAnalyzer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Formatters
import com.example.data.model.Installment
import com.example.data.model.Payment
import com.example.data.model.PaymentPart
import com.example.data.model.Sale
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.BrandLogo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg
import com.example.ui.theme.CDWarning
import com.example.ui.theme.CDWarningBg
import kotlinx.coroutines.launch

val WhatsAppGreen = Color(0xFF25D366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    repository: CobranzasRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userSession by repository.currentUserSession.collectAsState()
    val isAdmin = userSession?.isAdmin == true

    var client by remember { mutableStateOf<Client?>(null) }
    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedSaleId by remember { mutableStateOf<String?>(null) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showNewSaleDialog by remember { mutableStateOf(false) }

    // Android Hardware Back button handling: return to product list first if inside a product
    BackHandler(enabled = selectedSaleId != null) {
        selectedSaleId = null
    }

    fun refreshData() {
        scope.launch {
            isLoading = true
            client = repository.getClientById(clientId)
            sales = repository.getSalesForClient(clientId)
            payments = repository.getPaymentsForClient(clientId)
            isLoading = false
        }
    }

    LaunchedEffect(clientId) {
        refreshData()
    }

    fun shareFichaText(saleToShare: Sale? = null) {
        val currentClient = client ?: return
        val currentSale = saleToShare ?: sales.find { it.id == selectedSaleId } ?: sales.firstOrNull()

        val sb = StringBuilder()
        sb.appendLine("CAPITAL DIARIO · FICHA DE PAGO")
        sb.appendLine()
        sb.appendLine("Cliente: ${currentClient.fullName}")
        sb.appendLine("DNI: ${currentClient.dni} · Tel: ${currentClient.phone}")
        currentClient.email?.let { sb.appendLine("Mail: $it") }
        sb.appendLine("Domicilio: ${currentClient.address}")
        sb.appendLine()
        if (currentSale != null) {
            sb.appendLine("Producto: ${currentSale.product}")
            sb.appendLine("Número de crédito: ${currentSale.creditNumber}")
            sb.appendLine("Total financiado: ${Formatters.formatCurrency(currentSale.totalAmount)}")
            sb.appendLine()
            sb.appendLine("Pago | Vencimiento | Estado | Monto | Saldo")
            currentSale.installments.forEach { inst ->
                val paidAmount = (inst.amount - inst.balance).coerceAtLeast(0.0)
                val statusLabel = when {
                    inst.isPaid -> "Pagada"
                    paidAmount > 0 -> "Parcial"
                    else -> Formatters.getDueDateLabel(inst.dueDate)
                }
                sb.appendLine("${inst.number} | ${Formatters.formatDate(inst.dueDate)} | $statusLabel | ${Formatters.formatCurrency(inst.amount)} | ${Formatters.formatCurrency(inst.balance)}")
            }
        }
        sb.appendLine()
        sb.appendLine("Saldo pendiente total: ${Formatters.formatCurrency(currentClient.totalPending)}")
        sb.appendLine("Registrado vía Cobranzas CD · Capital Diario")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir Ficha de Pago")
        context.startActivity(shareIntent)
    }

    val selectedSale = remember(sales, selectedSaleId) {
        sales.find { it.id == selectedSaleId }
    }

    Scaffold(
        modifier = modifier.testTag("client_detail_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedSale != null) selectedSale.product.uppercase() else (client?.fullName ?: "Ficha de Cliente"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (selectedSale != null && client != null) {
                            Text(
                                text = "Cliente: ${client!!.fullName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedSaleId != null) {
                                selectedSaleId = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { shareFichaText(selectedSale) }, modifier = Modifier.testTag("btn_share_ficha")) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir Texto Ficha", tint = CDPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = { showNewSaleDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_new_sale"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva Venta")
                        }
                    }

                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_record_payment"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cargar Pago", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading && client == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CDPrimary)
            }
        } else {
            val curClient = client
            if (curClient == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cliente no encontrado.")
                }
            } else if (selectedSaleId == null) {
                // VIEW 1: PRODUCT LIST FOR CLIENT
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Client Info Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = curClient.fullName.uppercase(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("DNI ${curClient.dni}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    Text(Formatters.formatPhone(curClient.phone), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = curClient.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                curClient.collectorName?.let {
                                    Text(text = "Cobrador asignado: $it", fontSize = 11.sp, color = CDPrimary, fontWeight = FontWeight.Medium)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Saldo Pendiente Total:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatCurrency(curClient.totalPending), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CDPrimary)
                                }
                            }
                        }
                    }

                    // Button below Client Info to send ALL active product PDFs in a single file via WhatsApp
                    if (sales.isNotEmpty()) {
                        item {
                            Button(
                                onClick = { PdfGenerator.generateAndShareAllProductsPdf(context, curClient, sales) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_share_all_pdf_whatsapp"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enviar Fichas de Todos los Productos (PDF)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Header for products list
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRODUCTOS DEL CLIENTE (${sales.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (sales.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Este cliente no posee productos o ventas registradas.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(sales, key = { it.id }) { sale ->
                            val totalCuotas = sale.installments.size
                            val pagadas = sale.installments.count { it.isPaid }
                            val pendingBalance = sale.installments.filter { !it.isPaid }.sumOf { it.balance }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_card_${sale.id}")
                                    .clickable { selectedSaleId = sale.id },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sale.product.uppercase(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0C2D57).copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "Crédito #${sale.creditNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0C2D57),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Total financiado: ${Formatters.formatCurrency(sale.totalAmount)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Cuotas: $pagadas/$totalCuotas",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CDSuccess
                                        )
                                    }

                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Saldo producto:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = Formatters.formatCurrency(pendingBalance),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pendingBalance > 0) Color(0xFF0C2D57) else CDSuccess
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Ver Ficha de Pago",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CDPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = CDPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Button inside product card to send this product's PDF via WhatsApp
                                    OutlinedButton(
                                        onClick = { PdfGenerator.generateAndShareProductPdf(context, curClient, sale) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_share_product_pdf_${sale.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, WhatsAppGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = WhatsAppGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Enviar Ficha PDF por WhatsApp",
                                            color = WhatsAppGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // VIEW 2: FICHA DE PAGO FOR THE SELECTED PRODUCT
                val sale = selectedSale
                if (sale == null) {
                    selectedSaleId = null
                } else {
                    val totalCuotas = sale.installments.size
                    val totales = sale.installments.count { it.isPaid }
                    val parciales = sale.installments.count { !it.isPaid && (it.amount - it.balance) > 0 }
                    val pendientes = sale.installments.count { !it.isPaid && (it.amount - it.balance) <= 0 }
                    val productPending = sale.installments.filter { !it.isPaid }.sumOf { it.balance }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // PDF Header Banner Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2D57))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BrandLogo()
                                    }
                                    Text(
                                        text = "FICHA DE PAGO",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // WhatsApp PDF Share button for this specific product
                        item {
                            Button(
                                onClick = { PdfGenerator.generateAndShareProductPdf(context, curClient, sale) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_share_single_pdf_whatsapp"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enviar Ficha en PDF por WhatsApp",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Client Info Section (PDF Model)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = curClient.fullName.uppercase(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text("DNI ${curClient.dni}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                        Text(Formatters.formatPhone(curClient.phone), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = curClient.address,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    curClient.collectorName?.let {
                                        Text(text = "Cobrador: $it", fontSize = 11.sp, color = CDPrimary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Selected Product Details & Metrics
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sale.product.uppercase(),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Crédito ${sale.creditNumber}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Total financiado: ${Formatters.formatCurrency(sale.totalAmount)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 4 Summary Metric Cards matching PDF
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$totalCuotas", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0C2D57))
                                            Text("CUOTAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = CDSuccessBg.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$totales", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CDSuccess)
                                            Text("TOTALES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CDSuccess)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = CDWarningBg.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$parciales", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CDWarning)
                                            Text("PARCIALES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CDWarning)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("$pendientes", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0C2D57))
                                            Text("PENDIENTES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Render 2 Cuota cards per row in grid matching PDF
                        val chunks = sale.installments.chunked(2)
                        chunks.forEach { chunk ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FichaCuotaCard(
                                        inst = chunk[0],
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (chunk.size > 1) {
                                        FichaCuotaCard(
                                            inst = chunk[1],
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // PDF Footer replica
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Documento informativo.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Saldo producto: ${Formatters.formatCurrency(productPending)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Cargar Pago
    if (showPaymentDialog && client != null) {
        val curClient = client!!
        var paymentAmountStr by remember { mutableStateOf("") }
        var paymentMethod by remember { mutableStateOf("Efectivo") } // "Efectivo", "Transferencia"
        var paymentNote by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var paymentError by remember { mutableStateOf<String?>(null) }

        // Synced Transfer Accounts from backend
        val transferAccounts by repository.getTransferAccounts().collectAsState(initial = emptyList())
        val enabledAccounts = remember(transferAccounts) { transferAccounts.filter { it.enabled } }

        var selectedReceiptUri by remember { mutableStateOf<Uri?>(null) }
        var isAnalyzingReceipt by remember { mutableStateOf(false) }
        var receiptAnalysisResult by remember { mutableStateOf<ReceiptAnalysisResult?>(null) }

        val receiptPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedReceiptUri = uri
                isAnalyzingReceipt = true
                paymentError = null
                scope.launch {
                    val result = ReceiptAnalyzer.analyzeReceipt(context, uri, enabledAccounts)
                    isAnalyzingReceipt = false
                    receiptAnalysisResult = result
                    if (result.isValid && result.totalAmount != null && result.totalAmount > 0) {
                        paymentAmountStr = result.totalAmount.toString()
                        if (paymentNote.isBlank()) {
                            paymentNote = "Verificado por IA: ${result.matchedAccountLabel}"
                        }
                    } else if (!result.isValid) {
                        paymentAmountStr = ""
                        paymentError = result.errorMessage
                    }
                }
            }
        }

        // Multiple payment distribution mode if client has > 1 product
        var isMultiplePayment by remember { mutableStateOf(selectedSaleId == null && sales.size > 1) }
        var activeSelectedSaleId by remember { mutableStateOf(selectedSaleId ?: sales.firstOrNull()?.id ?: "") }

        val totalIntegerAmount = paymentAmountStr.toIntOrNull() ?: 0

        // Calculate distribution among products
        val partsToRegister = remember(sales, isMultiplePayment, activeSelectedSaleId, totalIntegerAmount) {
            if (sales.isEmpty() || totalIntegerAmount <= 0) {
                emptyList()
            } else if (sales.size == 1 || !isMultiplePayment) {
                val saleId = if (sales.size == 1) sales[0].id else activeSelectedSaleId
                listOf(PaymentPart(saleId, totalIntegerAmount.toDouble()))
            } else {
                // Distribute equally among active sales
                val count = sales.size
                val base = totalIntegerAmount / count
                val remainder = totalIntegerAmount % count
                sales.mapIndexed { idx, sale ->
                    val allocated = base + if (idx < remainder) 1 else 0
                    PaymentPart(sale.id, allocated.toDouble())
                }
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPaymentDialog = false },
            title = { Text("Cargar pago a ${curClient.fullName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Saldo pendiente actual: ${Formatters.formatCurrency(curClient.totalPending)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (sales.size > 1) {
                        Text("Tipo de pago:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isMultiplePayment,
                                onClick = { isMultiplePayment = true },
                                label = { Text("Pago Múltiple (${sales.size} prod.)") }
                            )
                            FilterChip(
                                selected = !isMultiplePayment,
                                onClick = { isMultiplePayment = false },
                                label = { Text("Producto único") }
                            )
                        }
                    }

                    if (sales.size > 1 && !isMultiplePayment) {
                        Text("Seleccionar producto:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            sales.forEach { sale ->
                                FilterChip(
                                    selected = activeSelectedSaleId == sale.id,
                                    onClick = { activeSelectedSaleId = sale.id },
                                    label = { Text("${sale.product} (${sale.creditNumber})") }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paymentAmountStr,
                        onValueChange = { paymentAmountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Monto cobrado total ($ enteros) *") },
                        placeholder = { Text("Ej: 50000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (sales.size > 1 && isMultiplePayment && totalIntegerAmount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Distribución en partes iguales (${sales.size} productos):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CDPrimary
                                )
                                partsToRegister.forEach { part ->
                                    val sale = sales.find { it.id == part.saleId }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${sale?.product ?: "Producto"} (${sale?.creditNumber ?: ""})",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(part.amount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CDSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Medio de pago:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentMethod == "Efectivo",
                            onClick = { paymentMethod = "Efectivo" },
                            label = { Text("Efectivo") }
                        )
                        FilterChip(
                            selected = paymentMethod == "Transferencia",
                            onClick = { paymentMethod = "Transferencia" },
                            label = { Text("Transferencia") }
                        )
                    }

                    if (paymentMethod == "Transferencia") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    receiptPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedReceiptUri == null) "Cargar comprobante de pago" else "Cambiar comprobante")
                            }

                            if (isAnalyzingReceipt) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CDPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analizando comprobante con IA...", fontSize = 12.sp, color = CDPrimary)
                                }
                            }

                            receiptAnalysisResult?.let { res ->
                                if (res.isValid) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CDSuccessBg)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CDSuccess, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Comprobante verificado ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CDSuccess)
                                            }
                                            Text("Cuenta de destino: ${res.matchedAccountLabel ?: "Válida"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            res.totalAmount?.let { amt ->
                                                Text("Monto detectado automáticamente: ${Formatters.formatCurrency(amt.toDouble())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CDPrimary)
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Comprobante rechazado ✕", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                            Text(res.errorMessage ?: "La cuenta de destino no coincide con ninguna cuenta habilitada de Capital Diario.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    paymentError?.let {
                        Text(it, color = CDDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = totalIntegerAmount.toDouble()
                        if (amount <= 0) {
                            paymentError = "Ingresá un monto entero mayor a cero."
                            return@Button
                        }
                        if (paymentMethod == "Transferencia" && selectedReceiptUri != null && receiptAnalysisResult?.isValid == false) {
                            paymentError = "No se puede confirmar: El comprobante fue rechazado porque la cuenta no coincide."
                            return@Button
                        }
                        isSubmitting = true
                        paymentError = null
                        scope.launch {
                            val res = repository.registerPayment(
                                clientId = curClient.id,
                                clientName = curClient.fullName,
                                amount = amount,
                                method = paymentMethod,
                                note = paymentNote.takeIf { it.isNotBlank() },
                                receipt = selectedReceiptUri?.toString(),
                                parts = partsToRegister
                            )
                            isSubmitting = false
                            showPaymentDialog = false
                            snackbarHostState.showSnackbar(res.getOrDefault("Pago registrado"))
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDPrimary),
                    enabled = !isSubmitting && !isAnalyzingReceipt
                ) {
                    Text(if (isSubmitting) "Guardando…" else "Confirmar Pago")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Nueva Venta / Crédito
    if (showNewSaleDialog && client != null) {
        val curClient = client!!
        var product by remember { mutableStateOf("") }
        var creditNumber by remember { mutableStateOf("") }
        var totalAmountStr by remember { mutableStateOf("") }
        var planDaysStr by remember { mutableStateOf("30") }
        var firstDueDate by remember { mutableStateOf(Formatters.getTodayIso()) }
        var saleError by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showNewSaleDialog = false },
            title = { Text("Nueva Venta / Crédito", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = product,
                        onValueChange = { product = it },
                        label = { Text("Producto o descripción *") },
                        placeholder = { Text("Ej: Préstamo Personal, TV 50', Moto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = creditNumber,
                        onValueChange = { creditNumber = it },
                        label = { Text("Número de crédito *") },
                        placeholder = { Text("Ej: 62391") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalAmountStr,
                        onValueChange = { totalAmountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Total financiado ($) *") },
                        placeholder = { Text("Ej: 200000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = planDaysStr,
                        onValueChange = { planDaysStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Cantidad de cuotas *") },
                        placeholder = { Text("Ej: 30") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    saleError?.let {
                        Text(it, color = CDDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = totalAmountStr.toDoubleOrNull()
                        val days = planDaysStr.toIntOrNull()
                        if (product.isBlank() || creditNumber.isBlank() || amount == null || amount <= 0 || days == null || days <= 0) {
                            saleError = "Completá todos los campos correctamente."
                            return@Button
                        }
                        isSubmitting = true
                        saleError = null
                        scope.launch {
                            repository.createSale(
                                clientId = curClient.id,
                                product = product,
                                creditNumber = creditNumber,
                                totalAmount = amount,
                                planDays = days,
                                firstDueDate = firstDueDate
                            )
                            isSubmitting = false
                            showNewSaleDialog = false
                            snackbarHostState.showSnackbar("Venta cargada correctamente")
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDPrimary),
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "Guardando…" else "Crear Venta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewSaleDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FichaCuotaCard(
    inst: Installment,
    modifier: Modifier = Modifier
) {
    val paidAmount = (inst.amount - inst.balance).coerceAtLeast(0.0)

    val statusText = when {
        inst.isPaid -> "PAGADA"
        paidAmount > 0 -> "PARCIAL"
        else -> "PENDIENTE"
    }

    val statusContainerColor = when {
        inst.isPaid -> CDSuccessBg
        paidAmount > 0 -> CDWarningBg
        else -> Color(0xFFE8EEF5)
    }

    val statusTextColor = when {
        inst.isPaid -> CDSuccess
        paidAmount > 0 -> CDWarning
        else -> Color(0xFF0C2D57)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cuota ${inst.number}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusContainerColor
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Vence ${Formatters.formatDate(inst.dueDate)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = Formatters.formatCurrency(inst.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inst.isPaid) {
                    Text("Abonó ${Formatters.formatCurrency(paidAmount)}", fontSize = 10.sp, color = CDSuccess, fontWeight = FontWeight.Bold)
                    Text("Pagada", fontSize = 10.sp, color = CDSuccess, fontWeight = FontWeight.Bold)
                } else if (paidAmount > 0) {
                    Text("Abonó ${Formatters.formatCurrency(paidAmount)}", fontSize = 10.sp, color = CDWarning, fontWeight = FontWeight.SemiBold)
                    Text("Saldo ${Formatters.formatCurrency(inst.balance)}", fontSize = 10.sp, color = CDWarning, fontWeight = FontWeight.Bold)
                } else {
                    Text("Sin abonos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sin pagar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
