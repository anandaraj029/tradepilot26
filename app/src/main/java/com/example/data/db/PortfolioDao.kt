package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'portfolios' core table.
 */
@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolios WHERE id = :id LIMIT 1")
    fun getPortfolioFlow(id: Int = 1): Flow<PortfolioEntity?>

    @Query("SELECT * FROM portfolios WHERE id = :id LIMIT 1")
    suspend fun getPortfolioSync(id: Int = 1): PortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePortfolio(portfolio: PortfolioEntity)

    @Query("UPDATE portfolios SET availableCash = :cash, realizedPnlToday = :realizedPnl, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCashAndPnl(id: Int = 1, cash: Double, realizedPnl: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE portfolios SET isLocked = :isLocked, lockReason = :lockReason, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLockStatus(id: Int = 1, isLocked: Boolean, lockReason: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE portfolios SET killSwitchTriggered = :triggered, updatedAt = :timestamp WHERE id = :id")
    suspend fun setKillSwitch(id: Int = 1, triggered: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM portfolios WHERE id = :id")
    suspend fun deletePortfolio(id: Int = 1)
}
