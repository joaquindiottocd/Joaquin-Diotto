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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
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
import com.example.data.model.Formatters
import com.example.data.model.Holiday
import com.example.data.repository.CobranzasRepository
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDPrimary
import kotlinx.coroutines.launch

@Composable
fun HolidaysScreen(
    repository: CobranzasRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val holidays by repository.getHolidays().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var holidayToDelete by remember { mutableStateOf<Holiday?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("holidays_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Feriados",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Los feriados se saltean automáticamente en el cálculo de vencimientos de cuotas.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (holidays.isEmpty()) {
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
                                text = "Todavía no hay feriados registrados.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(holidays) { hol ->
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
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CDPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = Formatters.formatDate(hol.date),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = hol.reason ?: "Feriado",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { holidayToDelete = hol }) {
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
                .testTag("fab_add_holiday"),
            containerColor = CDPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar feriado")
        }
    }

    if (showAddDialog) {
        var date by remember { mutableStateOf(Formatters.getTodayIso()) }
        var reason by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar un feriado", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Fecha (AAAA-MM-DD) *") },
                        placeholder = { Text("2026-11-20") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo (opcional)") },
                        placeholder = { Text("Ej: Día de la Soberanía") },
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
                        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(date)) {
                            error = "Ingresá una fecha válida en formato AAAA-MM-DD."
                            return@Button
                        }
                        scope.launch {
                            repository.addHoliday(date, reason.takeIf { it.isNotBlank() })
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
                ) {
                    Text("Agregar feriado")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    holidayToDelete?.let { hol ->
        AlertDialog(
            onDismissRequest = { holidayToDelete = null },
            title = { Text("¿Eliminar el feriado?") },
            text = { Text("Se eliminará el feriado del ${Formatters.formatDate(hol.date)}.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.removeHoliday(hol.id)
                            holidayToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CDDanger)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { holidayToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
