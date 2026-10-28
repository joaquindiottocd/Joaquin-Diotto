package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectorUser
import com.example.data.model.Formatters
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(
    repository: CobranzasRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val collectors by repository.collectors.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val defaultTeam = remember {
        listOf(
            CollectorUser("usr_1", "Joaquín Diotto", "joaquin.diotto@capitaldiario.com.ar", "admin", null),
            CollectorUser("usr_2", "García, Juan", "juan.garcia@capitaldiario.com.ar", "collector", "García, Juan"),
            CollectorUser("usr_3", "Martínez, Diego", "diego.martinez@capitaldiario.com.ar", "collector", "Martínez, Diego")
        )
    }

    val teamList = if (collectors.isNotEmpty()) collectors else defaultTeam

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("users_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Equipo y Usuarios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Administración con acceso total y cobradores acotados a su propia cartera.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(teamList) { user ->
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Formatters.getInitials(user.name),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CDPrimary
                                )
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = user.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                user.collectorName?.let {
                                    Text(
                                        text = "Cartera: $it",
                                        fontSize = 11.sp,
                                        color = CDPrimary
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (user.role == "admin") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.role == "admin") {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CDPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                }
                                Text(
                                    text = if (user.role == "admin") "Administrador" else "Cobrador",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.role == "admin") CDPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                .testTag("fab_add_user"),
            containerColor = CDPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar usuario")
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("collector") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar usuario", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre y Apellido *") },
                        placeholder = { Text("Ej: Pérez, Juan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email *") },
                        placeholder = { Text("nombre@capitaldiario.com.ar") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Rol del usuario:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "collector",
                            onClick = { role = "collector" },
                            label = { Text("Cobrador") }
                        )
                        FilterChip(
                            selected = role == "admin",
                            onClick = { role = "admin" },
                            label = { Text("Administrador") }
                        )
                    }
                    error?.let {
                        Text(it, color = CDDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank()) {
                            error = "Completá nombre y email."
                            return@Button
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
                ) {
                    Text("Crear usuario")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
