package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PortfolioEntity::class,
        AssetEntity::class,
        TradeEntity::class,
        AuditLogEntity::class,
        PositionEntity::class,
        CustomStrategyEntity::class,
        WatchlistEntity::class,
        BrokerConnectionEntity::class,
        FundTransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAOs for core tables
    abstract fun tradeDao(): TradeDao
    abstract fun assetDao(): AssetDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun auditLogDao(): AuditLogDao

    // Unified application DAO
    abstract fun tradePilotDao(): TradePilotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trade_pilot.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
