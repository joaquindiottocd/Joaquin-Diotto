package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val dni: String,
    val fullName: String,
    val address: String,
    val phone: String,
    val email: String?,
    val collectorId: String?,
    val collectorName: String?,
    val activeCredits: Int,
    val totalPending: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val product: String,
    val creditNumber: String,
    val totalAmount: Double,
    val planDays: Int,
    val firstDueDate: String,
    val skippedDatesJson: String = "[]",
    val status: String = "active",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val clientName: String?,
    val creditNumber: String?,
    val product: String?,
    val amount: Double,
    val paidAt: String,
    val method: String,
    val note: String?,
    val receipt: String?,
    val collectorName: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_payments")
data class OfflinePaymentEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val clientId: String,
    val clientName: String,
    val amount: Double,
    val method: String, // "Efectivo", "Transferencia"
    val note: String?,
    val receipt: String?,
    val partsJson: String, // JSON array of parts {saleId, amount}
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncError: String? = null
)

@Entity(tableName = "transfer_accounts")
data class TransferAccountEntity(
    @PrimaryKey val id: String,
    val cuit: String,
    val cbu: String?,
    val label: String,
    val enabled: Boolean = true
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val id: String,
    val date: String,
    val reason: String?
)

fun ClientEntity.toModel() = com.example.data.model.Client(
    id = id,
    dni = dni,
    fullName = fullName,
    address = address,
    phone = phone,
    email = email,
    collectorId = collectorId,
    collectorName = collectorName,
    activeCredits = activeCredits,
    totalPending = totalPending
)
