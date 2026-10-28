package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClientEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        OfflinePaymentEntity::class,
        TransferAccountEntity::class,
        HolidayEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CobranzasDatabase : RoomDatabase() {
    abstract fun dao(): CobranzasDao

    companion object {
        @Volatile
        private var INSTANCE: CobranzasDatabase? = null

        fun getInstance(context: Context): CobranzasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CobranzasDatabase::class.java,
                    "cobranzas_cd_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
