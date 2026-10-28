package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransferAccount
import com.example.data.repository.CobranzasRepository
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg
import kotlinx.coroutines.launch

@Composable
fun TransferAccountsScreen(
    repository: CobranzasRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val accounts by repository.getTransferAccounts().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<TransferAccount?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("transfer_accounts_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Cuentas de Transferencia",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cuentas bancarias y billeteras virtuales para recibir transferencias de cobro.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Todavía no hay cuentas cargadas.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(accounts) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CDPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = acc.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "CUIT: ${acc.cuit}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    acc.cbu?.let {
                                        Text(
                                            text = "CBU: $it",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { accountToDelete = acc }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = CDDanger)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_account"),
            containerColor = CDPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar cuenta")
        }
    }

    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var cuit by remember { mutableStateOf("") }
        var cbu by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar una cuenta", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Nombre o alias de la cuenta *") },
                        placeholder = { Text("Ej: Banco Francés pesos / MercadoPago") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cuit,
                        onValueChange = { cuit = it.filter { c -> c.isDigit() } },
                        label = { Text("CUIT / CUIL (11 dígitos) *") },
                        placeholder = { Text("30715289124") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cbu,
                        onValueChange = { cbu = it.filter { c -> c.isDigit() } },
                        label = { Text("CBU / CVU (22 dígitos opcional)") },
                        placeholder = { Text("0170023420000012345678") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    error?.let {
                        Text(it, color = CDDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCuit = cuit.filter { it.isDigit() }
                        if (label.isBlank() || cleanCuit.length != 11) {
                            error = "El CUIT tiene que tener 11 dígitos y el nombre es obligatorio."
                            return@Button
                        }
                        scope.launch {
                            repository.createTransferAccount(cleanCuit, cbu.takeIf { it.isNotBlank() }, label)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
                ) {
                    Text("Guardar cuenta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("¿Eliminar la cuenta?") },
            text = { Text("Se eliminará '${acc.label}'. Ya no se mostrará como opción para transferencias.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.removeTransferAccount(acc.id)
                            accountToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDDanger)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
