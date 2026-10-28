package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserSession(
    val token: String,
    val email: String,
    val name: String,
    val role: String, // "admin" or "collector"
    val collectorName: String? = null
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true) || collectorName.isNullOrEmpty()
}

data class Client(
    val id: String,
    val dni: String,
    val fullName: String,
    val address: String,
    val phone: String,
    val email: String? = null,
    val collectorId: String? = null,
    val collectorName: String? = null,
    val activeCredits: Int = 0,
    val totalPending: Double = 0.0,
    val notes: List<String> = emptyList()
)

data class Sale(
    val id: String,
    val clientId: String,
    val product: String,
    val creditNumber: String,
    val totalAmount: Double,
    val planDays: Int,
    val firstDueDate: String,
    val skippedDates: List<String> = emptyList(),
    val status: String = "active",
    val installments: List<Installment> = emptyList()
)

data class Installment(
    val number: Int,
    val dueDate: String,
    val amount: Double,
    val balance: Double,
    val status: String = "Pendiente"
) {
    val isPaid: Boolean get() = balance <= 0.01
}

data class Payment(
    val id: String,
    val clientId: String,
    val clientName: String? = null,
    val creditNumber: String? = null,
    val product: String? = null,
    val amount: Double,
    val paidAt: String,
    val method: String, // "Efectivo", "Transferencia"
    val note: String? = null,
    val receipt: String? = null,
    val collectorName: String? = null,
    val allocations: List<PaymentAllocation> = emptyList()
)

data class PaymentAllocation(
    val saleId: String,
    val number: Int,
    val amount: Double,
    val days: Int = 0
)

data class PaymentPart(
    val saleId: String,
    val amount: Double
)

data class TransferAccount(
    val id: String,
    val cuit: String,
    val cbu: String? = null,
    val label: String,
    val enabled: Boolean = true
)

data class Holiday(
    val id: String,
    val date: String, // YYYY-MM-DD
    val reason: String? = null
)

data class CollectorUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String, // "admin" or "collector"
    val collectorName: String? = null
)

data class PortfolioMetrics(
    val financed: Double,
    val collected: Double,
    val pending: Double,
    val collectedThisMonth: Double,
    val overdueCount: Int
)

data class DailyReport(
    val date: String,
    val total: Double,
    val payments: List<Payment>,
    val byCollector: List<CollectorTotal>
)

data class CollectorTotal(
    val name: String,
    val total: Double
)

// Formatting and calculation helpers matching the web app (format-lKF7TPMA.js)
object Formatters {
    private val localeAr = Locale("es", "AR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(localeAr).apply {
        maximumFractionDigits = 0
    }
    private val currencyExactFormat = NumberFormat.getCurrencyInstance(localeAr).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatCurrency(amount: Double): String {
        return try {
            currencyFormat.format(amount).replace("ARS", "$").trim()
        } catch (e: Exception) {
            "$ " + String.format(localeAr, "%,.0f", amount)
        }
    }

    fun formatCurrencyExact(amount: Double): String {
        return try {
            currencyExactFormat.format(amount).replace("ARS", "$").trim()
        } catch (e: Exception) {
            "$ " + String.format(localeAr, "%,.2f", amount)
        }
    }

    fun formatDate(isoDate: String): String {
        if (!isoDate.contains("-")) return isoDate
        val parts = isoDate.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
    }

    fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun formatPhone(phone: String): String {
        val clean = phone.filter { it.isDigit() }
        return when {
            clean.length == 10 -> "${clean.substring(0, 2)} ${clean.substring(2, 6)}-${clean.substring(6)}"
            clean.length == 11 -> "${clean.substring(0, 3)} ${clean.substring(3, 6)}-${clean.substring(6)}"
            else -> phone.trim()
        }
    }

    fun getInitials(name: String): String {
        val clean = name.replace(",", " ").trim()
        val parts = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.isEmpty()) return "?"
        val first = parts[0].firstOrNull()?.uppercaseChar() ?: '?'
        val second = if (parts.size > 1) parts[1].firstOrNull()?.uppercaseChar() else parts[0].getOrNull(1)?.uppercaseChar()
        return if (second != null) "$first$second" else "$first"
    }

    fun getDueDateLabel(dueDateIso: String): String {
        val today = getTodayIso()
        if (dueDateIso == today) return "Vence hoy"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d1 = sdf.parse(dueDateIso)
            val d2 = sdf.parse(today)
            if (d1 != null && d2 != null) {
                val diffDays = ((d1.time - d2.time) / (1000 * 60 * 60 * 24)).toInt()
                when {
                    diffDays < 0 -> {
                        val absDays = -diffDays
                        "Vencida hace $absDays ${if (absDays == 1) "día" else "días"}"
                    }
                    diffDays in 1..7 -> "Vence en $diffDays ${if (diffDays == 1) "día" else "días"}"
                    else -> "Vence el ${formatDate(dueDateIso)}"
                }
            } else {
                "Vence el ${formatDate(dueDateIso)}"
            }
        } catch (e: Exception) {
            "Vence el ${formatDate(dueDateIso)}"
        }
    }
}
