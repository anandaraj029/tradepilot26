package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'trades' core table.
 */
@Dao
interface TradeDao {

    @Query("SELECT * FROM trades ORDER BY closedAt DESC")
    fun getAllTradesFlow(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades ORDER BY closedAt DESC")
    suspend fun getAllTradesSync(): List<TradeEntity>

    @Query("SELECT * FROM trades WHERE status = 'CLOSED' ORDER BY closedAt DESC")
    fun getClosedTradesFlow(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE status = 'CLOSED' ORDER BY closedAt DESC")
    suspend fun getClosedTradesSync(): List<TradeEntity>

    @Query("SELECT SUM(pnl) FROM trades WHERE status = 'CLOSED' AND closedAt >= :startOfDayTimestamp")
    fun getRealizedPnlSinceFlow(startOfDayTimestamp: Long): Flow<Double?>

    @Query("SELECT * FROM trades WHERE status = 'CLOSED' AND closedAt >= :startOfDayTimestamp ORDER BY closedAt DESC")
    fun getTradesSinceFlow(startOfDayTimestamp: Long): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE market = :market ORDER BY closedAt DESC")
    fun getTradesByMarketFlow(market: String): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE status = 'OPEN' ORDER BY entryTime DESC")
    fun getOpenTradesFlow(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE id = :id")
    suspend fun getTradeById(id: Long): TradeEntity?

    @Query("SELECT * FROM trades WHERE symbol = :symbol ORDER BY closedAt DESC")
    fun getTradesBySymbolFlow(symbol: String): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Delete
    suspend fun deleteTrade(trade: TradeEntity)

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("DELETE FROM trades")
    suspend fun deleteAllTrades()
}
