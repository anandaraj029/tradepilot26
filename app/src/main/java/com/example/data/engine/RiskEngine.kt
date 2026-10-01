package com.example.data.engine

import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.model.DecisionType
import com.example.data.model.MarketType
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.RiskStatus
import com.example.data.model.StockQuote
import kotlin.math.floor
import kotlin.math.max

object RiskEngine {

    /**
     * Evaluates a trade proposed by the AI Council against deterministic capital protection rules.
     * AI recommendations CANNOT override the risk rules.
     */
    fun evaluateTradeProposal(
        stock: StockQuote,
        decision: DecisionType,
        portfolio: PortfolioEntity,
        currentPositions: List<PositionEntity>,
        requestedQty: Int? = null
    ): RiskEvaluationResult {
        // 1. Emergency Kill Switch Check
        if (portfolio.killSwitchTriggered) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "EMERGENCY PROTECTION ACTIVE: Kill Switch has been triggered. All trading is halted.",
                currentRiskStatus = RiskStatus.LOCKED,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // 2. Daily Loss Limit Check
        if (portfolio.realizedPnlToday <= -portfolio.maxDailyLoss) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "DAILY LOSS LIMIT REACHED (-₹${portfolio.maxDailyLoss.toInt()}): Trading locked to prevent emotional/algorithmic drawdown.",
                currentRiskStatus = RiskStatus.LOCKED,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // 3. Daily Profit Target Lock Check
        if (portfolio.profitLockEnabled && portfolio.realizedPnlToday >= portfolio.dailyTarget) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "DAILY TARGET ACHIEVED (+₹${portfolio.dailyTarget.toInt()}): Trading locked to protect realized gains for the day.",
                currentRiskStatus = RiskStatus.LOCKED,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // 4. Portfolio Manual Lock Check
        if (portfolio.isLocked) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "TRADING SYSTEM LOCKED: ${portfolio.lockReason.ifEmpty { "Manual lock activated." }}",
                currentRiskStatus = RiskStatus.LOCKED,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // 5. Decision Type Check
        if (decision == DecisionType.NO_TRADE || decision == DecisionType.WAIT || decision == DecisionType.HOLD) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "AI Council returned ${decision.label}. Capital preservation rule: preserve cash and wait for favorable risk/reward setup.",
                currentRiskStatus = RiskStatus.SAFE,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // 6. Max Open Positions Check
        if (currentPositions.size >= portfolio.maxOpenPositions) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "MAX OPEN POSITIONS REACHED: Currently holding ${currentPositions.size}/${portfolio.maxOpenPositions} allowed positions.",
                currentRiskStatus = RiskStatus.CAUTION,
                maxPermittedQuantity = 0,
                suggestedStopLoss = stock.support,
                suggestedTakeProfit = stock.resistance,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        // Price in INR
        val priceInInr = if (stock.market == MarketType.USA) {
            stock.currentPrice * portfolio.usdInrRate
        } else {
            stock.currentPrice
        }

        // 7. Max Risk Per Trade (1% of starting capital = ₹250 default)
        val maxPermittedRiskRupees = portfolio.startingCapital * (portfolio.maxRiskPerTradePercent / 100.0)

        // Stop-Loss Calculation: ~2% below current price or at key support
        val stopLossDistPercent = 0.025
        val suggestedStopLoss = Math.round((stock.currentPrice * (1.0 - stopLossDistPercent)) * 100.0) / 100.0
        val suggestedTakeProfit = Math.round((stock.currentPrice * (1.0 + (stopLossDistPercent * 2.2))) * 100.0) / 100.0

        val riskPerShareInr = priceInInr * stopLossDistPercent
        val maxQtyByRisk = if (riskPerShareInr > 0) floor(maxPermittedRiskRupees / riskPerShareInr).toInt() else 1
        val maxQtyByCash = floor(portfolio.availableCash / priceInInr).toInt()

        val maxAllowedQty = max(0, minOf(maxQtyByRisk, maxQtyByCash))

        // Check if any shares can be purchased
        if (maxAllowedQty <= 0) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "INSUFFICIENT CAPITAL / RISK SIZING: Available cash ₹${portfolio.availableCash.toInt()} cannot absorb 1 unit within 1% risk limit.",
                currentRiskStatus = RiskStatus.CAUTION,
                maxPermittedQuantity = 0,
                suggestedStopLoss = suggestedStopLoss,
                suggestedTakeProfit = suggestedTakeProfit,
                estimatedRiskRupees = 0.0,
                exposurePercentageAfter = 0.0
            )
        }

        val targetQty = requestedQty ?: maxAllowedQty
        if (targetQty > maxAllowedQty) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "REQUESTED QUANTITY ($targetQty) EXCEEDS RISK LIMIT: Max permissible is $maxAllowedQty shares based on 1% trade risk rule.",
                currentRiskStatus = RiskStatus.CAUTION,
                maxPermittedQuantity = maxAllowedQty,
                suggestedStopLoss = suggestedStopLoss,
                suggestedTakeProfit = suggestedTakeProfit,
                estimatedRiskRupees = targetQty * riskPerShareInr,
                exposurePercentageAfter = 0.0
            )
        }

        val tradeAmountInr = targetQty * priceInInr
        val currentInvestedInr = currentPositions.sumOf {
            val mult = if (it.market == "USA") portfolio.usdInrRate else 1.0
            it.quantity * it.currentPrice * mult
        }
        val totalCapital = portfolio.availableCash + currentInvestedInr
        val newExposurePercent = if (totalCapital > 0) ((currentInvestedInr + tradeAmountInr) / totalCapital) * 100.0 else 0.0

        // Exposure limit check: Max 80% total exposure
        if (newExposurePercent > 80.0) {
            return RiskEvaluationResult(
                isApproved = false,
                rejectionReason = "PORTFOLIO EXPOSURE LIMIT: Trade would result in ${newExposurePercent.toInt()}% allocation (Max permitted: 80%).",
                currentRiskStatus = RiskStatus.CAUTION,
                maxPermittedQuantity = maxAllowedQty,
                suggestedStopLoss = suggestedStopLoss,
                suggestedTakeProfit = suggestedTakeProfit,
                estimatedRiskRupees = targetQty * riskPerShareInr,
                exposurePercentageAfter = newExposurePercent
            )
        }

        // Approved!
        return RiskEvaluationResult(
            isApproved = true,
            rejectionReason = null,
            currentRiskStatus = if (portfolio.realizedPnlToday < -250.0) RiskStatus.CAUTION else RiskStatus.SAFE,
            maxPermittedQuantity = maxAllowedQty,
            suggestedStopLoss = suggestedStopLoss,
            suggestedTakeProfit = suggestedTakeProfit,
            estimatedRiskRupees = targetQty * riskPerShareInr,
            exposurePercentageAfter = newExposurePercent
        )
    }
}
