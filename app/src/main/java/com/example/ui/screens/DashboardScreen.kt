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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Formatters
import com.example.data.model.Payment
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.BrandLogo
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyncStatusBanner
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: CobranzasRepository,
    onNavigateToClients: () -> Unit,
    onOpenAddClient: () -> Unit,
    onOpenClientDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSyncing by repository.isSyncing.collectAsState()
    val syncMessage by repository.syncMessage.collectAsState()
    val offlinePending by repository.pendingOfflinePaymentsCount.collectAsState(initial = 0)
    val userSession by repository.currentUserSession.collectAsState()
    val metrics by repository.portfolioMetrics.collectAsState()
    val dailyReport by repository.dailyReport.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedCollectorFilter by remember { mutableStateOf<String?>(null) }
    var collectorDropdownExpanded by remember { mutableStateOf(false) }

    val filteredPayments = remember(dailyReport, selectedCollectorFilter) {
        val payments = dailyReport?.payments ?: emptyList()
        if (selectedCollectorFilter == null) payments
        else payments.filter { it.collectorName == selectedCollectorFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Sync Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val firstName = userSession?.name?.split(" ")?.firstOrNull() ?: "bienvenido"
                        Text(
                            text = "Hola, $firstName",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Estado de la cartera, cobrado del mes y próximos vencimientos.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Sync Status with cobranzascd.freebuff.app
                SyncStatusBanner(
                    isSyncing = isSyncing,
                    syncMessage = syncMessage,
                    offlineQueueCount = offlinePending,
                    onSyncClick = {
                        scope.launch {
                            repository.syncAll()
                        }
                    }
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToClients,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_view_clients"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver clientes")
                    }

                    if (userSession?.isAdmin == true) {
                        Button(
                            onClick = onOpenAddClient,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_add_client"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CDPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargar cliente")
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val financed = metrics?.financed ?: 0.0
                    val collected = metrics?.collected ?: 0.0
                    val collectedPercent = if (financed > 0) ((collected / financed) * 100).toInt() else 0

                    MetricCard(
                        label = "Total financiado",
                        value = Formatters.formatCurrency(financed),
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Cobrado",
                        value = Formatters.formatCurrency(collected),
                        icon = Icons.Default.CheckCircle,
                        hint = "$collectedPercent% de la cartera",
                        tone = "positive",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pending = metrics?.pending ?: 0.0
                    val overdueCount = metrics?.overdueCount ?: 0

                    MetricCard(
                        label = "Saldo pendiente",
                        value = Formatters.formatCurrency(pending),
                        icon = Icons.Default.ReceiptLong,
                        hint = if (overdueCount > 0) "$overdueCount cuotas vencidas" else "Al día",
                        tone = if (overdueCount > 0) "warning" else "neutral",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Cobrado este mes",
                        value = Formatters.formatCurrency(metrics?.collectedThisMonth ?: 0.0),
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Daily Report Section Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_daily_report_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Informe del día",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cobranzas de ${Formatters.formatDate(Formatters.getTodayIso())}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CDSuccessBg
                        ) {
                            Text(
                                text = Formatters.formatCurrency(dailyReport?.total ?: 0.0),
                                color = CDSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // By collector summary chips
                    val collectors = dailyReport?.byCollector ?: emptyList()
                    if (collectors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            collectors.forEach { col ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedCollectorFilter == col.name) CDPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = col.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (selectedCollectorFilter == col.name) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(col.total),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCollectorFilter == col.name) Color.White else CDPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Payments List
        if (filteredPayments.isEmpty()) {
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
                            text = "No se registraron cobranzas para el filtro seleccionado hoy.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPayments) { payment ->
                PaymentItemCard(
                    payment = payment,
                    onClick = {
                        onOpenClientDetail(payment.clientId)
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentItemCard(
    payment: Payment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_item_${payment.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        text = Formatters.getInitials(payment.clientName ?: "Cliente"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CDPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = payment.clientName ?: "Cliente",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        payment.creditNumber?.let {
                            Text(
                                text = it,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        payment.product?.let {
                            Text(
                                text = "• $it",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!payment.collectorName.isNullOrBlank()) {
                        Text(
                            text = "Cobró: ${payment.collectorName}",
                            fontSize = 11.sp,
                            color = CDPrimary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(payment.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CDSuccess
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = payment.method)
            }
        }
    }
}
