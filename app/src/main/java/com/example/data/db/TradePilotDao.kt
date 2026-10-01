package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradePilotDao {

    // --- Portfolios ---
    @Query("SELECT * FROM portfolios WHERE id = 1")
    fun getPortfolioFlow(): Flow<PortfolioEntity?>

    @Query("SELECT * FROM portfolios WHERE id = 1")
    suspend fun getPortfolioSync(): PortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePortfolio(portfolio: PortfolioEntity)

    // --- Trades ---
    @Query("SELECT * FROM trades ORDER BY closedAt DESC")
    fun getJournalFlow(): Flow<List<TradeJournalEntity>>

    @Query("SELECT SUM(pnl) FROM trades WHERE status = 'CLOSED' AND closedAt >= :startOfDayTimestamp")
    fun getRealizedPnlTodayFlow(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT * FROM trades WHERE status = 'CLOSED' AND closedAt >= :startOfDayTimestamp ORDER BY closedAt DESC")
    fun getTodayRealizedTradesFlow(startOfDayTimestamp: Long): Flow<List<TradeJournalEntity>>

    @Query("SELECT COUNT(*) FROM trades WHERE status = 'CLOSED' AND closedAt >= :startOfDayTimestamp")
    fun getTodayRealizedTradesCountFlow(startOfDayTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalTrade(trade: TradeJournalEntity): Long

    @Update
    suspend fun updateJournalTrade(trade: TradeJournalEntity)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteJournalTradeById(id: Long)

    // --- Assets ---
    @Query("SELECT * FROM assets ORDER BY symbol ASC")
    fun getAllAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE market = :market ORDER BY symbol ASC")
    fun getAssetsByMarketFlow(market: String): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetsCount(): Int

    @Query("SELECT * FROM assets WHERE symbol = :symbol LIMIT 1")
    suspend fun getAssetBySymbol(symbol: String): AssetEntity?

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // --- Open Positions ---
    @Query("SELECT * FROM positions ORDER BY timestamp DESC")
    fun getPositionsFlow(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions ORDER BY timestamp DESC")
    suspend fun getPositionsSync(): List<PositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity): Long

    @Update
    suspend fun updatePosition(position: PositionEntity)

    @Delete
    suspend fun deletePosition(position: PositionEntity)

    @Query("DELETE FROM positions WHERE id = :positionId")
    suspend fun deletePositionById(positionId: Long)

    // --- Custom Strategies ---
    @Query("SELECT * FROM custom_strategies ORDER BY createdAt DESC")
    fun getCustomStrategiesFlow(): Flow<List<CustomStrategyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStrategy(strategy: CustomStrategyEntity): Long

    @Query("DELETE FROM custom_strategies WHERE id = :id")
    suspend fun deleteCustomStrategy(id: Long)

    // --- Watchlist ---
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlistFlow(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlistFlow(symbol: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    // --- Broker Connections (Zerodha Kite & Live API) ---
    @Query("SELECT * FROM broker_connections WHERE id = 1")
    fun getBrokerConnectionFlow(): Flow<BrokerConnectionEntity?>

    @Query("SELECT * FROM broker_connections WHERE id = 1")
    suspend fun getBrokerConnectionSync(): BrokerConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBrokerConnection(broker: BrokerConnectionEntity)

    @Query("UPDATE broker_connections SET isLiveTradingActive = :isLive WHERE id = 1")
    suspend fun updateLiveTradingMode(isLive: Boolean)

    @Query("UPDATE broker_connections SET autoPilotAutonomousLive = :isAutonomous WHERE id = 1")
    suspend fun updateAutoPilotAutonomousLive(isAutonomous: Boolean)

    @Query("UPDATE broker_connections SET liveEquityMargin = :newMargin WHERE id = 1")
    suspend fun updateLiveMargin(newMargin: Double)

    // --- Fund Transactions (Deposits / Add Money / Withdrawals) ---
    @Query("SELECT * FROM fund_transactions ORDER BY timestamp DESC")
    fun getFundTransactionsFlow(): Flow<List<FundTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFundTransaction(transaction: FundTransactionEntity): Long
}
