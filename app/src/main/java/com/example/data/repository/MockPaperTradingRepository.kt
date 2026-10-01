package com.example.data.repository

import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Mock Repository for initial testing and prototyping of Paper Trading Dashboard UI.
 * Provides simulated virtual cash balances, multi-market open positions (India NSE & US NASDAQ),
 * and dynamic close-position and balance-update capabilities.
 */
class MockPaperTradingRepository(
    initialStartingCapital: Double = 25000.0,
    initialAvailableCash: Double = 18450.0,
    initialDailyTarget: Double = 1000.0,
    initialMaxDailyLoss: Double = 500.0,
    initialRealizedPnlToday: Double = 320.0,
    initialUsdInrRate: Double = 83.5
) {
    private val _portfolioState = MutableStateFlow(
        PortfolioEntity(
            id = 1,
            startingCapital = initialStartingCapital,
            availableCash = initialAvailableCash,
            dailyTarget = initialDailyTarget,
            maxDailyLoss = initialMaxDailyLoss,
            realizedPnlToday = initialRealizedPnlToday,
            usdInrRate = initialUsdInrRate,
            tradingMode = "PAPER_TRADING"
        )
    )
    val portfolio: Flow<PortfolioEntity> = _portfolioState.asStateFlow()

    private val _positionsState = MutableStateFlow(sampleMockPositions)
    val positions: Flow<List<PositionEntity>> = _positionsState.asStateFlow()

    companion object {
        val sampleMockPositions = listOf(
            PositionEntity(
                id = 101L,
                symbol = "RELIANCE",
                market = "INDIA",
                companyName = "Reliance Industries Ltd",
                quantity = 15,
                buyPrice = 2890.0,
                currentPrice = 2945.5,
                stopLoss = 2840.0,
                takeProfit = 3010.0,
                timestamp = System.currentTimeMillis() - 7200000
            ),
            PositionEntity(
                id = 102L,
                symbol = "TCS",
                market = "INDIA",
                companyName = "Tata Consultancy Services",
                quantity = 8,
                buyPrice = 3980.0,
                currentPrice = 4025.0,
                stopLoss = 3920.0,
                takeProfit = 4120.0,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            PositionEntity(
                id = 103L,
                symbol = "NVDA",
                market = "USA",
                companyName = "NVIDIA Corporation",
                quantity = 2,
                buyPrice = 124.50,
                currentPrice = 129.80,
                stopLoss = 120.00,
                takeProfit = 138.00,
                timestamp = System.currentTimeMillis() - 1800000
            )
        )
    }

    fun getCurrentPortfolio(): PortfolioEntity = _portfolioState.value

    fun getCurrentPositions(): List<PositionEntity> = _positionsState.value

    /**
     * Calculate total invested capital in INR across all open paper positions
     */
    fun getInvestedMarginInr(): Double {
        val rate = _portfolioState.value.usdInrRate
        return _positionsState.value.sumOf { pos ->
            val mult = if (pos.market == "USA") rate else 1.0
            pos.quantity * pos.buyPrice * mult
        }
    }

    /**
     * Calculate total current market value in INR of all open paper positions
     */
    fun getCurrentPositionValueInr(): Double {
        val rate = _portfolioState.value.usdInrRate
        return _positionsState.value.sumOf { pos ->
            val mult = if (pos.market == "USA") rate else 1.0
            pos.quantity * pos.currentPrice * mult
        }
    }

    /**
     * Calculate total net unrealized P&L in INR
     */
    fun getTotalUnrealizedPnlInr(): Double {
        return getCurrentPositionValueInr() - getInvestedMarginInr()
    }

    /**
     * Total simulated equity (Available Cash + Current Position Value)
     */
    fun getTotalEquityInr(): Double {
        return _portfolioState.value.availableCash + getCurrentPositionValueInr()
    }

    /**
     * Closes an open position, calculates realized P&L, returns cash to available balance,
     * and updates today's realized P&L.
     */
    fun closePosition(positionId: Long): Result<Double> {
        val currentList = _positionsState.value
        val position = currentList.find { it.id == positionId }
            ?: return Result.failure(IllegalArgumentException("Position with ID $positionId not found"))

        val mult = if (position.market == "USA") _portfolioState.value.usdInrRate else 1.0
        val realizedPnlInr = (position.currentPrice - position.buyPrice) * position.quantity * mult
        val cashReturned = (position.currentPrice * position.quantity * mult)

        _positionsState.update { it.filter { pos -> pos.id != positionId } }
        _portfolioState.update { current ->
            current.copy(
                availableCash = current.availableCash + cashReturned,
                realizedPnlToday = current.realizedPnlToday + realizedPnlInr,
                updatedAt = System.currentTimeMillis()
            )
        }

        return Result.success(realizedPnlInr)
    }

    /**
     * Adds virtual paper trading funds to the simulated balance
     */
    fun addPaperFunds(amount: Double) {
        if (amount <= 0) return
        _portfolioState.update { current ->
            current.copy(
                availableCash = current.availableCash + amount,
                startingCapital = current.startingCapital + amount,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Resets paper trading portfolio back to the baseline ₹25,000 initial capital state
     */
    fun resetPortfolio(capital: Double = 25000.0) {
        _positionsState.value = emptyList()
        _portfolioState.value = PortfolioEntity(
            id = 1,
            startingCapital = capital,
            availableCash = capital,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0,
            realizedPnlToday = 0.0,
            usdInrRate = 83.5,
            tradingMode = "PAPER_TRADING",
            updatedAt = System.currentTimeMillis()
        )
    }
}
