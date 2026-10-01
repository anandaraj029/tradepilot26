package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'assets' core table.
 */
@Dao
interface AssetDao {

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetsCount(): Int

    @Query("SELECT * FROM assets ORDER BY symbol ASC")
    fun getAllAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE market = :market ORDER BY symbol ASC")
    fun getAssetsByMarketFlow(market: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isWatchlisted = 1 ORDER BY symbol ASC")
    fun getWatchlistAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE symbol = :symbol LIMIT 1")
    suspend fun getAssetBySymbol(symbol: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE symbol = :symbol LIMIT 1")
    fun getAssetBySymbolFlow(symbol: String): Flow<AssetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("UPDATE assets SET currentPrice = :price, priceChange = :change, priceChangePercent = :changePct, lastUpdated = :timestamp WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, price: Double, change: Double, changePct: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE assets SET isWatchlisted = :isWatchlisted WHERE symbol = :symbol")
    suspend fun updateWatchlistStatus(symbol: String, isWatchlisted: Boolean)

    @Query("DELETE FROM assets WHERE symbol = :symbol")
    suspend fun deleteAsset(symbol: String)

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()
}
