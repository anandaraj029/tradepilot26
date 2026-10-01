package com.example.data.market

import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Real-time market data provider feeding 1,000+ asset quotes across
 * Indian (NSE / BSE) and US (NYSE / NASDAQ) exchanges with live tick simulation.
 */
object MarketDataProvider {

    private val initialIndianStocks = MarketAssetCatalog.getIndianAssetCatalog()
    private val initialUsStocks = MarketAssetCatalog.getUsAssetCatalog()

    private val stockCache = mutableMapOf<String, StockQuote>().apply {
        initialIndianStocks.forEach { put(it.symbol, it) }
        initialUsStocks.forEach { put(it.symbol, it) }
    }

    fun getAllIndianStocks(): List<StockQuote> = stockCache.values.filter { it.market == MarketType.INDIA }
    fun getAllUsStocks(): List<StockQuote> = stockCache.values.filter { it.market == MarketType.USA }

    fun getStockBySymbol(symbol: String): StockQuote? = stockCache[symbol]

    /**
     * Emits ticking market updates periodically to simulate active market depth.
     */
    fun getMarketTickStream(): Flow<StockQuote> = flow {
        val allSymbols = stockCache.keys.toList()
        while (true) {
            delay(3500)
            val randomSymbol = allSymbols.randomOrNull() ?: continue
            val current = stockCache[randomSymbol] ?: continue
            val deltaPercent = (Random.nextDouble(-0.35, 0.40))
            val priceDelta = current.currentPrice * (deltaPercent / 100.0)
            val newPrice = (current.currentPrice + priceDelta).coerceAtLeast(1.0)
            val newChange = current.priceChange + priceDelta
            val newChangePct = (newChange / (newPrice - newChange)) * 100.0

            val updatedSparkline = current.sparklineData.toMutableList()
            if (updatedSparkline.isNotEmpty()) {
                updatedSparkline.removeAt(0)
                updatedSparkline.add(newPrice.toFloat())
            }

            val updated = current.copy(
                currentPrice = Math.round(newPrice * 100.0) / 100.0,
                priceChange = Math.round(newChange * 100.0) / 100.0,
                priceChangePercent = Math.round(newChangePct * 100.0) / 100.0,
                sparklineData = updatedSparkline
            )
            stockCache[randomSymbol] = updated
            emit(updated)
        }
    }
}
