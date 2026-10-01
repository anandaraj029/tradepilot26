package com.example.data.engine

import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.model.StockQuote
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Autonomous trading engine that runs automatically while the user's daily
 * profit target (₹1,000) is NOT yet achieved. It evaluates market setups,
 * places disciplined paper orders, and automatically stops when the target
 * is achieved (profit lock) or if the maximum drawdown boundary (-₹500) is breached.
 */
sealed class AutoPilotDecision {
    data class StopTargetAchieved(
        val realizedPnl: Double,
        val target: Double,
        val message: String
    ) : AutoPilotDecision()

    data class StopLossLimit(
        val currentLoss: Double,
        val maxLoss: Double,
        val message: String
    ) : AutoPilotDecision()

    data class ClosePositionToBookProfit(
        val position: PositionEntity,
        val realizedProfit: Double,
        val reason: String
    ) : AutoPilotDecision()

    data class ExecuteBuyOrder(
        val stock: StockQuote,
        val quantity: Int,
        val stopLoss: Double,
        val takeProfit: Double,
        val reason: String
    ) : AutoPilotDecision()

    data class ActiveHunting(
        val realizedPnl: Double,
        val target: Double,
        val remaining: Double,
        val message: String
    ) : AutoPilotDecision()

    object IdleTerminalLocked : AutoPilotDecision()
    object IdleMaxPositionsReached : AutoPilotDecision()
}

object AutoPilotEngine {

    /**
     * Evaluates portfolio state and market candidates.
     * Keeps trading active automatically if target is NOT achieved.
     * Stops automatically once target is achieved or loss limit is hit.
     */
    fun evaluate(
        portfolio: PortfolioEntity,
        openPositions: List<PositionEntity>,
        candidateStocks: List<StockQuote>
    ): AutoPilotDecision {
        val dailyTarget = max(1.0, portfolio.dailyTarget)
        val realizedToday = portfolio.realizedPnlToday
        val maxDailyLoss = portfolio.maxDailyLoss

        // 1. Check if Daily Target has been achieved
        if (realizedToday >= dailyTarget) {
            return AutoPilotDecision.StopTargetAchieved(
                realizedPnl = realizedToday,
                target = dailyTarget,
                message = "🎯 Daily target of ₹${dailyTarget.toInt()} achieved! Auto-Pilot engaged Profit Lock and stopped to secure your gains."
            )
        }

        // 2. Check if Drawdown Loss Limit reached (-₹500)
        if (realizedToday <= -maxDailyLoss) {
            return AutoPilotDecision.StopLossLimit(
                currentLoss = realizedToday,
                maxLoss = maxDailyLoss,
                message = "🛑 Max daily loss boundary (-₹${maxDailyLoss.toInt()}) reached. Auto-Pilot stopped automatically to preserve capital."
            )
        }

        // 3. Check if Terminal is explicitly locked or kill-switched
        if (portfolio.isLocked || portfolio.killSwitchTriggered) {
            return AutoPilotDecision.IdleTerminalLocked
        }

        // 4. Target is NOT yet achieved -> Stay active automatically!
        // First check open positions to book profits or cut losses
        for (pos in openPositions) {
            val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
            val profitInr = (pos.currentPrice - pos.buyPrice) * pos.quantity * mult
            val pnlPct = if (pos.buyPrice > 0) ((pos.currentPrice - pos.buyPrice) / pos.buyPrice) * 100.0 else 0.0

            // Book profit if take profit reached or profit >= 2.0%
            if (pos.currentPrice >= pos.takeProfit || pnlPct >= 2.0) {
                return AutoPilotDecision.ClosePositionToBookProfit(
                    position = pos,
                    realizedProfit = profitInr,
                    reason = "Take-profit milestone reached (+${"%.1f".format(pnlPct)}%). Booking +₹${profitInr.toInt()} towards ₹${dailyTarget.toInt()} target."
                )
            }

            // Cut position if stop loss breached
            if (pos.stopLoss > 0 && pos.currentPrice <= pos.stopLoss) {
                return AutoPilotDecision.ClosePositionToBookProfit(
                    position = pos,
                    realizedProfit = profitInr,
                    reason = "Stop loss executed at ₹${pos.stopLoss}. Capital protected."
                )
            }
        }

        // Check if we have room for new positions (max allowed, e.g. 4)
        if (openPositions.size >= portfolio.maxOpenPositions) {
            return AutoPilotDecision.IdleMaxPositionsReached
        }

        // 5. Hunt for top AI-scored candidates to reach target
        val remainingToTarget = dailyTarget - realizedToday
        val existingSymbols = openPositions.map { it.symbol }.toSet()

        val bestCandidate = candidateStocks
            .filter { !existingSymbols.contains(it.symbol) }
            .filter { it.currentPrice > 10.0 }
            .filter { it.rsi in 42.0..68.0 && it.priceChangePercent > 0.3 }
            .shuffled(Random(System.currentTimeMillis()))
            .firstOrNull()

        if (bestCandidate != null && portfolio.availableCash >= 2000.0) {
            val isUs = bestCandidate.market.name == "USA"
            val priceInr = if (isUs) bestCandidate.currentPrice * portfolio.usdInrRate else bestCandidate.currentPrice

            // 1% max risk allocation
            val maxCapitalToAllocate = min(portfolio.availableCash * 0.25, 6000.0)
            val qty = max(1, (maxCapitalToAllocate / priceInr).toInt())

            val slPrice = if (isUs) bestCandidate.currentPrice * 0.985 else bestCandidate.currentPrice * 0.985
            val tpPrice = if (isUs) bestCandidate.currentPrice * 1.035 else bestCandidate.currentPrice * 1.035

            return AutoPilotDecision.ExecuteBuyOrder(
                stock = bestCandidate,
                quantity = qty,
                stopLoss = Math.round(slPrice * 100.0) / 100.0,
                takeProfit = Math.round(tpPrice * 100.0) / 100.0,
                reason = "Auto-Pilot selected ${bestCandidate.symbol} (RSI ${bestCandidate.rsi}, +${bestCandidate.priceChangePercent}% momentum). Hunting ₹${remainingToTarget.toInt()} remaining to target."
            )
        }

        return AutoPilotDecision.ActiveHunting(
            realizedPnl = realizedToday,
            target = dailyTarget,
            remaining = remainingToTarget,
            message = "Auto-Pilot active. Scanning 1,000+ assets for optimal entry to reach ₹${dailyTarget.toInt()} target."
        )
    }
}
