package com.example.data.repository

import com.example.data.db.AuditLogDao
import com.example.data.db.AuditLogEntity
import com.example.data.db.TradeDao
import com.example.data.db.TradeEntity
import com.example.data.model.AiConsensusResult
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.StockQuote
import com.example.data.processor.TradeProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.round

/**
 * Performance metrics summarizing trade history for analytics and review.
 */
data class TradePerformanceMetrics(
    val totalTrades: Int,
    val winCount: Int,
    val lossCount: Int,
    val breakevenCount: Int,
    val winRate: Double,
    val totalRealizedPnl: Double,
    val totalProfit: Double,
    val totalLoss: Double,
    val profitFactor: Double,
    val avgTradePnl: Double,
    val aiAdherencePercentage: Double,
    val aiAdherenceWinRate: Double,
    val overrideWinRate: Double,
    val topPerformingStrategy: String,
    val strategyBreakdown: Map<String, StrategyPerformance> = emptyMap(),
    val marketBreakdown: Map<String, MarketPerformance> = emptyMap()
)

data class StrategyPerformance(
    val strategyName: String,
    val tradesCount: Int,
    val winRate: Double,
    val netPnl: Double
)

data class MarketPerformance(
    val market: String,
    val tradesCount: Int,
    val winRate: Double,
    val netPnl: Double
)

/**
 * Repository class providing clean, abstracted access to trade data stored in the Room database,
 * incorporating functions for adding new trades, processing AI recommendations,
 * and retrieving trade history for deep post-mortem analysis.
 */
class TradeRepository(
    private val tradeDao: TradeDao,
    private val auditLogDao: AuditLogDao? = null
) {

    // --- Reactive Data Streams ---

    val allTradesFlow: Flow<List<TradeEntity>> = tradeDao.getAllTradesFlow()

    val openTradesFlow: Flow<List<TradeEntity>> = tradeDao.getOpenTradesFlow()

    val closedTradesFlow: Flow<List<TradeEntity>> = tradeDao.getClosedTradesFlow()

    fun getTradesByMarketFlow(market: String): Flow<List<TradeEntity>> {
        return tradeDao.getTradesByMarketFlow(market)
    }

    fun getTradesBySymbolFlow(symbol: String): Flow<List<TradeEntity>> {
        return tradeDao.getTradesBySymbolFlow(symbol)
    }

    // --- Trade Ingestion & Management ---

    /**
     * Inserts a raw trade entity into Room.
     */
    suspend fun addTrade(trade: TradeEntity): Long = withContext(Dispatchers.IO) {
        val validation = TradeProcessor.validateTradeData(trade)
        if (validation.isFailure) {
            throw validation.exceptionOrNull() ?: IllegalArgumentException("Invalid trade data")
        }
        val insertedId = tradeDao.insertTrade(trade)

        auditLogDao?.insertAuditLog(
            AuditLogEntity(
                action = "TRADE_RECORDED",
                symbol = trade.symbol,
                market = trade.market,
                aiConsensusVerdict = trade.aiDecision,
                riskEngineStatus = if (trade.userOverride) "MANUAL_OVERRIDE" else "COUNCIL_ALIGNED",
                details = "Trade #${trade.tradeId} recorded: ${trade.orderType} ${trade.quantity} @ ${trade.entryPrice} (${trade.strategy})"
            )
        )

        insertedId
    }

    /**
     * Translates an AI consensus result into a persisted trade using TradeProcessor.
     */
    suspend fun processAndRecordAiTrade(
        stock: StockQuote,
        consensus: AiConsensusResult,
        riskEval: RiskEvaluationResult? = null,
        strategyName: String = "AI Council Deliberation",
        quantity: Int,
        entryPrice: Double = stock.currentPrice,
        userOverride: Boolean = false,
        overrideReason: String = "",
        userNotes: String = ""
    ): Result<TradeEntity> = withContext(Dispatchers.IO) {
        try {
            val processedTrade = TradeProcessor.createTradeFromConsensus(
                stock = stock,
                consensus = consensus,
                riskEval = riskEval,
                strategyName = strategyName,
                quantity = quantity,
                entryPrice = entryPrice,
                userOverride = userOverride,
                overrideReason = overrideReason,
                userNotes = userNotes
            )

            val validation = TradeProcessor.validateTradeData(processedTrade)
            if (validation.isFailure) {
                return@withContext Result.failure(validation.exceptionOrNull()!!)
            }

            val rowId = tradeDao.insertTrade(processedTrade)
            val savedTrade = processedTrade.copy(id = rowId)

            auditLogDao?.insertAuditLog(
                AuditLogEntity(
                    action = "AI_TRADE_PROCESSED",
                    symbol = stock.symbol,
                    market = stock.market.name,
                    aiConsensusVerdict = "${consensus.consensusDecision.name} (${consensus.overallConfidence}%)",
                    riskEngineStatus = if (riskEval?.isApproved == true) "RISK_APPROVED" else "PROCESSED",
                    details = "AI Trade #${savedTrade.tradeId} created: ${savedTrade.quantity} shares @ ₹${savedTrade.entryPrice} (${savedTrade.strategy})"
                )
            )

            Result.success(savedTrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Closes an existing trade, executes P&L calculations via TradeProcessor, and persists the outcome.
     */
    suspend fun closeTrade(
        tradeId: Long,
        exitPrice: Double,
        usdInrRate: Double = 83.5,
        exitNote: String = ""
    ): Result<TradeEntity> = withContext(Dispatchers.IO) {
        try {
            val existingTrade = tradeDao.getTradeById(tradeId)
                ?: return@withContext Result.failure(IllegalArgumentException("Trade with ID $tradeId not found"))

            val evaluatedTrade = TradeProcessor.completeAndEvaluateTrade(
                trade = existingTrade,
                exitPrice = exitPrice,
                usdInrRate = usdInrRate,
                exitNote = exitNote
            )

            tradeDao.updateTrade(evaluatedTrade)

            auditLogDao?.insertAuditLog(
                AuditLogEntity(
                    action = "TRADE_CLOSED",
                    symbol = evaluatedTrade.symbol,
                    market = evaluatedTrade.market,
                    aiConsensusVerdict = evaluatedTrade.aiDecision,
                    riskEngineStatus = evaluatedTrade.outcome,
                    details = "Trade #${evaluatedTrade.tradeId} closed @ ${evaluatedTrade.exitPrice}. P&L: ₹${evaluatedTrade.pnl} (${evaluatedTrade.pnlPercent}%)"
                )
            )

            Result.success(evaluatedTrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrade(trade: TradeEntity) = withContext(Dispatchers.IO) {
        tradeDao.updateTrade(trade)
    }

    suspend fun deleteTrade(id: Long) = withContext(Dispatchers.IO) {
        tradeDao.deleteTradeById(id)
    }

    suspend fun getTradeById(id: Long): TradeEntity? = withContext(Dispatchers.IO) {
        tradeDao.getTradeById(id)
    }

    // --- Trade History Retrieval & Analytical Processing ---

    /**
     * Retrieves all historical closed trades for analytical post-mortems and performance evaluation.
     */
    suspend fun getTradeHistoryForAnalysis(): List<TradeEntity> = withContext(Dispatchers.IO) {
        tradeDao.getClosedTradesSync()
    }

    /**
     * Calculates comprehensive quantitative performance metrics across trade history.
     */
    suspend fun calculatePerformanceMetrics(): TradePerformanceMetrics = withContext(Dispatchers.IO) {
        val trades = tradeDao.getClosedTradesSync()
        if (trades.isEmpty()) {
            return@withContext TradePerformanceMetrics(
                totalTrades = 0,
                winCount = 0,
                lossCount = 0,
                breakevenCount = 0,
                winRate = 0.0,
                totalRealizedPnl = 0.0,
                totalProfit = 0.0,
                totalLoss = 0.0,
                profitFactor = 0.0,
                avgTradePnl = 0.0,
                aiAdherencePercentage = 100.0,
                aiAdherenceWinRate = 0.0,
                overrideWinRate = 0.0,
                topPerformingStrategy = "None"
            )
        }

        val totalTrades = trades.size
        val winTrades = trades.filter { it.pnl > 0.0 }
        val lossTrades = trades.filter { it.pnl < 0.0 }
        val breakevenTrades = trades.filter { it.pnl == 0.0 }

        val winCount = winTrades.size
        val lossCount = lossTrades.size
        val breakevenCount = breakevenTrades.size
        val winRate = round((winCount.toDouble() / totalTrades * 100.0) * 10.0) / 10.0

        val totalProfit = winTrades.sumOf { it.pnl }
        val totalLoss = abs(lossTrades.sumOf { it.pnl })
        val totalRealizedPnl = round((totalProfit - totalLoss) * 100.0) / 100.0

        val profitFactor = if (totalLoss > 0.0) {
            round((totalProfit / totalLoss) * 100.0) / 100.0
        } else if (totalProfit > 0.0) {
            9.99
        } else {
            0.0
        }

        val avgTradePnl = round((totalRealizedPnl / totalTrades) * 100.0) / 100.0

        // AI Adherence vs Override metrics
        val followedTrades = trades.filter { !it.userOverride }
        val overriddenTrades = trades.filter { it.userOverride }

        val aiAdherencePercentage = round((followedTrades.size.toDouble() / totalTrades * 100.0) * 10.0) / 10.0
        val aiAdherenceWinRate = if (followedTrades.isNotEmpty()) {
            round((followedTrades.count { it.pnl > 0 }.toDouble() / followedTrades.size * 100.0) * 10.0) / 10.0
        } else 0.0

        val overrideWinRate = if (overriddenTrades.isNotEmpty()) {
            round((overriddenTrades.count { it.pnl > 0 }.toDouble() / overriddenTrades.size * 100.0) * 10.0) / 10.0
        } else 0.0

        // Strategy Breakdown
        val strategyGroups = trades.groupBy { it.strategy }
        val strategyMap = strategyGroups.mapValues { (strat, sTrades) ->
            StrategyPerformance(
                strategyName = strat,
                tradesCount = sTrades.size,
                winRate = round((sTrades.count { it.pnl > 0 }.toDouble() / sTrades.size * 100.0) * 10.0) / 10.0,
                netPnl = round(sTrades.sumOf { it.pnl } * 100.0) / 100.0
            )
        }
        val topStrategy = strategyMap.values.maxByOrNull { it.netPnl }?.strategyName ?: "None"

        // Market Breakdown
        val marketGroups = trades.groupBy { it.market }
        val marketMap = marketGroups.mapValues { (mkt, mTrades) ->
            MarketPerformance(
                market = mkt,
                tradesCount = mTrades.size,
                winRate = round((mTrades.count { it.pnl > 0 }.toDouble() / mTrades.size * 100.0) * 10.0) / 10.0,
                netPnl = round(mTrades.sumOf { it.pnl } * 100.0) / 100.0
            )
        }

        TradePerformanceMetrics(
            totalTrades = totalTrades,
            winCount = winCount,
            lossCount = lossCount,
            breakevenCount = breakevenCount,
            winRate = winRate,
            totalRealizedPnl = totalRealizedPnl,
            totalProfit = round(totalProfit * 100.0) / 100.0,
            totalLoss = round(totalLoss * 100.0) / 100.0,
            profitFactor = profitFactor,
            avgTradePnl = avgTradePnl,
            aiAdherencePercentage = aiAdherencePercentage,
            aiAdherenceWinRate = aiAdherenceWinRate,
            overrideWinRate = overrideWinRate,
            topPerformingStrategy = topStrategy,
            strategyBreakdown = strategyMap,
            marketBreakdown = marketMap
        )
    }
}
