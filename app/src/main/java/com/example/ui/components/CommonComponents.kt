package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CDDanger
import com.example.ui.theme.CDDangerBg
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.CDSuccess
import com.example.ui.theme.CDSuccessBg
import com.example.ui.theme.CDWarning
import com.example.ui.theme.CDWarningBg

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true,
    isLight: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.isotipo),
            contentDescription = "Cobranzas CD Logo",
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cobranzas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isLight) Color.White else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "CD",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isLight) Color(0xFFBAE6FD) else CDPrimary
                )
            }
            if (showSubtitle) {
                Text(
                    text = "REGISTRO DE PAGOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = if (isLight) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SyncStatusBanner(
    isSyncing: Boolean,
    syncMessage: String,
    offlineQueueCount: Int,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sync_status_banner"),
        shape = RoundedCornerShape(12.dp),
        color = if (offlineQueueCount > 0) CDWarningBg else CDSuccessBg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (offlineQueueCount > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (offlineQueueCount > 0) CDWarning else CDSuccess,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (offlineQueueCount > 0) "$offlineQueueCount cobro(s) pendientes de envío" else "Sincronizado con la web",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (offlineQueueCount > 0) CDWarning else CDSuccess
                    )
                    Text(
                        text = if (offlineQueueCount > 0) "Se enviarán automáticamente a cobranzascd.freebuff.app" else syncMessage,
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_sync_now")
            ) {
                if (isSyncing) {
                    val transition = rememberInfiniteTransition(label = "sync_spin")
                    val angle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
                        label = "spin_angle"
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizando",
                        tint = CDPrimary,
                        modifier = Modifier.rotate(angle)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sincronizar ahora",
                        tint = CDPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    hint: String? = null,
    tone: String = "neutral" // "positive", "warning", "neutral"
) {
    Card(
        modifier = modifier.testTag("metric_card_$label"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (tone) {
                                "positive" -> CDSuccessBg
                                "warning" -> CDWarningBg
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when (tone) {
                            "positive" -> CDSuccess
                            "warning" -> CDWarning
                            else -> CDPrimary
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = when (tone) {
                        "positive" -> CDSuccess
                        "warning" -> CDWarning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, textCol) = when {
        status.contains("Pagada", ignoreCase = true) || status.contains("Al día", ignoreCase = true) ->
            CDSuccessBg to CDSuccess
        status.contains("Vencida", ignoreCase = true) || status.contains("atraso", ignoreCase = true) ->
            CDDangerBg to CDDanger
        status.contains("hoy", ignoreCase = true) ->
            CDWarningBg to CDWarning
        else ->
            Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
