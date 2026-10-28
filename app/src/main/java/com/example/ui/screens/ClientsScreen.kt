package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg
import com.example.ui.theme.CDWarning
import com.example.ui.theme.CDWarningBg
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(
    repository: CobranzasRepository,
    onClientClick: (String) -> Unit,
    showAddDialogInitial: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clients by repository.getClients().collectAsState(initial = emptyList())

    val userSession by repository.currentUserSession.collectAsState()
    val isAdmin = userSession?.isAdmin == true

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("all") } // "all", "active_debt", "unassigned"

    var showAddClientDialog by remember { mutableStateOf(if (isAdmin) showAddDialogInitial else false) }

    val filteredClients = remember(clients, searchQuery, filterMode) {
        clients.filter { client ->
            val matchesSearch = searchQuery.isBlank() ||
                    client.fullName.contains(searchQuery, ignoreCase = true) ||
                    client.dni.contains(searchQuery, ignoreCase = true) ||
                    client.phone.contains(searchQuery, ignoreCase = true) ||
                    client.address.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterMode) {
                "active_debt" -> client.totalPending > 0
                "unassigned" -> client.collectorId.isNullOrEmpty()
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("clients_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_clients_input"),
                    placeholder = { Text("Buscar por nombre, DNI, teléfono o dirección…") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == "all",
                        onClick = { filterMode = "all" },
                        label = { Text("Todos (${clients.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = CDPrimary
                        )
                    )
                    FilterChip(
                        selected = filterMode == "active_debt",
                        onClick = { filterMode = "active_debt" },
                        label = { Text("Con deuda") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = CDPrimary
                        )
                    )
                    FilterChip(
                        selected = filterMode == "unassigned",
                        onClick = { filterMode = "unassigned" },
                        label = { Text("Sin asignar") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = CDPrimary
                        )
                    )
                }
            }

            if (filteredClients.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No se encontraron clientes para '$searchQuery'" else "No hay clientes registrados.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredClients, key = { it.id }) { client ->
                    ClientCardItem(
                        client = client,
                        onClick = { onClientClick(client.id) },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                            context.startActivity(intent)
                        },
                        onWhatsApp = {
                            val clean = client.phone.filter { it.isDigit() }
                            val phoneWithCode = if (clean.startsWith("54")) clean else "549$clean"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phoneWithCode"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button (Only for Admin)
        if (isAdmin) {
            FloatingActionButton(
                onClick = { showAddClientDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_add_client"),
                containerColor = CDPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Cargar cliente")
            }
        }
    }

    // Add Client Dialog
    if (showAddClientDialog) {
        ClientFormDialog(
            title = "Cargar cliente",
            initialClient = null,
            onDismiss = { showAddClientDialog = false },
            onConfirm = { dni, name, addr, phone, email ->
                scope.launch {
                    repository.createClient(dni, name, addr, phone, email)
                    showAddClientDialog = false
                }
            }
        )
    }
}

@Composable
fun ClientCardItem(
    client: Client,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Formatters.getInitials(client.fullName),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CDPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = client.fullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "DNI ${client.dni}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (client.activeCredits > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CDPrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${client.activeCredits} ${if (client.activeCredits == 1) "activo" else "activos"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CDPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }


            }

            // Address and Phone
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (client.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = client.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (client.phone.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Formatters.formatPhone(client.phone),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Quick Call and WhatsApp buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CDSuccessBg,
                                modifier = Modifier.clickable { onWhatsApp() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = CDSuccess, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, color = CDSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onCall() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Llamar", tint = CDPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Llamar", fontSize = 11.sp, color = CDPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Collector and Pending Balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (client.collectorName.isNullOrBlank()) "Sin cobrador asignado" else "Cobrador: ${client.collectorName}",
                    fontSize = 12.sp,
                    color = if (client.collectorName.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else CDPrimary,
                    fontWeight = FontWeight.Medium
                )

                if (client.totalPending > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Saldo pendiente",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatCurrency(client.totalPending),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CDWarning
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CDSuccessBg
                    ) {
                        Text(
                            text = "Al día",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CDSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientFormDialog(
    title: String,
    initialClient: Client?,
    onDismiss: () -> Unit,
    onConfirm: (dni: String, fullName: String, address: String, phone: String, email: String?) -> Unit
) {
    var dni by remember { mutableStateOf(initialClient?.dni ?: "") }
    var fullName by remember { mutableStateOf(initialClient?.fullName ?: "") }
    var address by remember { mutableStateOf(initialClient?.address ?: "") }
    var phone by remember { mutableStateOf(initialClient?.phone ?: "") }
    var email by remember { mutableStateOf(initialClient?.email ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Apellido y Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dni,
                    onValueChange = { dni = it },
                    label = { Text("DNI *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = CDDanger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || dni.isBlank() || address.isBlank() || phone.isBlank()) {
                        errorMessage = "Por favor completá los campos obligatorios (*)."
                        return@Button
                    }
                    onConfirm(dni, fullName, address, phone, email.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
