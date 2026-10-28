package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CobranzasDao {
    // Clients
    @Query("SELECT * FROM clients ORDER BY fullName ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :clientId LIMIT 1")
    suspend fun getClientById(clientId: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)

    @Query("DELETE FROM clients")
    suspend fun clearClients()

    // Sales
    @Query("SELECT * FROM sales WHERE clientId = :clientId")
    fun getSalesByClient(clientId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Query("DELETE FROM sales WHERE clientId = :clientId")
    suspend fun clearSalesForClient(clientId: String)

    // Payments
    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY paidAt DESC")
    fun getPaymentsByClient(clientId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE paidAt = :date ORDER BY createdAt DESC")
    fun getPaymentsByDate(date: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    // Offline payments queue
    @Query("SELECT * FROM offline_payments WHERE isSynced = 0 ORDER BY createdAt ASC")
    fun getPendingOfflinePayments(): Flow<List<OfflinePaymentEntity>>

    @Query("SELECT COUNT(*) FROM offline_payments WHERE isSynced = 0")
    fun getPendingOfflinePaymentsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflinePayment(payment: OfflinePaymentEntity): Long

    @Update
    suspend fun updateOfflinePayment(payment: OfflinePaymentEntity)

    @Query("DELETE FROM offline_payments WHERE localId = :localId")
    suspend fun deleteOfflinePayment(localId: Long)

    // Transfer Accounts
    @Query("SELECT * FROM transfer_accounts")
    fun getAllTransferAccounts(): Flow<List<TransferAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferAccounts(accounts: List<TransferAccountEntity>)

    @Query("DELETE FROM transfer_accounts WHERE id = :id")
    suspend fun deleteTransferAccount(id: String)

    // Holidays
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteHoliday(id: String)
}
