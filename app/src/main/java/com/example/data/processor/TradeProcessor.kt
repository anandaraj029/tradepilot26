package com.example.data.processor

import com.example.data.db.TradeEntity
import com.example.data.model.AiConsensusResult
import com.example.data.model.DecisionType
import com.example.data.model.MarketType
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.StockQuote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * TradeProcessor utility class that acts as the interface between the AI consensus output
 * and the Room database, ensuring all required fields like strategy, risk scores,
 * multi-agent reasoning, change factors, and invalidation triggers are persisted correctly.
 */
object TradeProcessor {

    private val idDateFormat = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())

    /**
     * Converts an AI deliberation and risk evaluation into a persistent TradeEntity.
     */
    fun createTradeFromConsensus(
        stock: StockQuote,
        consensus: AiConsensusResult,
        riskEval: RiskEvaluationResult? = null,
        strategyName: String = "AI Council Deliberation",
        quantity: Int,
        entryPrice: Double = stock.currentPrice,
        stopLoss: Double = riskEval?.suggestedStopLoss ?: stock.support,
        takeProfit: Double = riskEval?.suggestedTakeProfit ?: stock.resistance,
        orderType: String = if (consensus.consensusDecision == DecisionType.SELL) "SELL" else "BUY",
        executionType: String = "PAPER",
        userOverride: Boolean = false,
        overrideReason: String = "",
        userNotes: String = "",
        tags: String = if (userOverride) "Overridden • Discretionary" else "Followed Plan"
    ): TradeEntity {
        val uniqueTradeId = "TP-${idDateFormat.format(Date())}-${(100..999).random()}"

        // Synthesize detailed multi-agent reasoning
        val agentBreakdownText = if (consensus.agentBreakdowns.isNotEmpty()) {
            consensus.agentBreakdowns.joinToString(" | ") { "${it.agentName}: ${it.decision.name} (${it.confidence}%)" }
        } else {
            "Consensus: ${consensus.buyVotes} Buy / ${consensus.holdVotes} Hold / ${consensus.sellVotes} Sell"
        }

        val fullAiObservation = buildString {
            append(consensus.synthesizedReason)
            if (agentBreakdownText.isNotBlank()) {
                append(" [Agents: $agentBreakdownText]")
            }
            if (consensus.isNoTradeAdvised) {
                append(" (Note: AI Council advised NO TRADE due to risk asymmetry)")
            }
        }

        // Format change factors and invalidation triggers
        val changeFactorsStr = if (consensus.decisionChangeFactors.isNotEmpty()) {
            consensus.decisionChangeFactors.joinToString(" • ")
        } else {
            consensus.keyCatalysts.joinToString(" • ")
        }

        val invalidationStr = if (consensus.invalidationConditionsList.isNotEmpty()) {
            consensus.invalidationConditionsList.joinToString(" • ")
        } else {
            consensus.riskFactors.joinToString(" • ")
        }

        return TradeEntity(
            tradeId = uniqueTradeId,
            symbol = stock.symbol,
            market = stock.market.name,
            companyName = stock.companyName,
            strategy = strategyName,
            entryPrice = round(entryPrice * 100.0) / 100.0,
            exitPrice = 0.0,
            quantity = quantity,
            stopLoss = round(stopLoss * 100.0) / 100.0,
            takeProfit = round(takeProfit * 100.0) / 100.0,
            pnl = 0.0,
            pnlPercent = 0.0,
            aiDecision = consensus.consensusDecision.name,
            aiConfidence = consensus.overallConfidence,
            riskScore = consensus.riskScoreAvg,
            status = "OPEN",
            orderType = orderType,
            executionType = executionType,
            entryTime = System.currentTimeMillis(),
            closedAt = 0L,
            outcome = "OPEN",
            aiObservation = fullAiObservation,
            userOverride = userOverride,
            overrideReason = overrideReason,
            userNotes = userNotes,
            tags = tags,
            changeFactors = changeFactorsStr,
            invalidationTriggers = invalidationStr
        )
    }

    /**
     * Closes an active trade, calculating P&L in base INR currency with USD conversion where applicable.
     */
    fun completeAndEvaluateTrade(
        trade: TradeEntity,
        exitPrice: Double,
        closedAt: Long = System.currentTimeMillis(),
        usdInrRate: Double = 83.5,
        exitNote: String = ""
    ): TradeEntity {
        val currencyMultiplier = if (trade.market.equals("USA", ignoreCase = true)) usdInrRate else 1.0

        val priceDiff = if (trade.orderType.equals("SELL", ignoreCase = true)) {
            trade.entryPrice - exitPrice
        } else {
            exitPrice - trade.entryPrice
        }

        val netPnlInr = priceDiff * trade.quantity * currencyMultiplier
        val netPnlPercent = if (trade.entryPrice > 0.0) {
            (priceDiff / trade.entryPrice) * 100.0
        } else {
            0.0
        }

        val outcome = when {
            netPnlInr > 0.01 -> "PROFIT"
            netPnlInr < -0.01 -> "LOSS"
            else -> "BREAKEVEN"
        }

        val updatedNotes = if (exitNote.isNotBlank()) {
            if (trade.userNotes.isNotBlank()) "${trade.userNotes} | Exit: $exitNote" else "Exit: $exitNote"
        } else {
            trade.userNotes
        }

        return trade.copy(
            exitPrice = round(exitPrice * 100.0) / 100.0,
            closedAt = closedAt,
            pnl = round(netPnlInr * 100.0) / 100.0,
            pnlPercent = round(netPnlPercent * 100.0) / 100.0,
            status = "CLOSED",
            outcome = outcome,
            userNotes = updatedNotes
        )
    }

    /**
     * Validates that trade data is mathematically and structurally valid before DB insertion.
     */
    fun validateTradeData(trade: TradeEntity): Result<Unit> {
        if (trade.symbol.isBlank()) return Result.failure(IllegalArgumentException("Trade symbol cannot be empty."))
        if (trade.quantity <= 0) return Result.failure(IllegalArgumentException("Trade quantity must be positive."))
        if (trade.entryPrice <= 0.0) return Result.failure(IllegalArgumentException("Entry price must be positive."))
        if (trade.stopLoss < 0.0) return Result.failure(IllegalArgumentException("Stop loss cannot be negative."))
        return Result.success(Unit)
    }
}
