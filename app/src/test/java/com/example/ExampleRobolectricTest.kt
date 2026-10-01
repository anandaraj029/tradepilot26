package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.PortfolioEntity
import com.example.data.engine.RiskEngine
import com.example.data.market.MarketDataProvider
import com.example.data.model.DecisionType
import com.example.data.model.RiskStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI TradePilot", appName)
    }

    @Test
    fun `risk engine halts orders when kill switch is active`() {
        val stock = MarketDataProvider.getAllIndianStocks().first { it.symbol == "TATAMOTORS" }
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            killSwitchTriggered = true
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList()
        )

        assertFalse(eval.isApproved)
        assertEquals(RiskStatus.LOCKED, eval.currentRiskStatus)
        assertTrue(eval.rejectionReason?.contains("KILL SWITCH", ignoreCase = true) == true)
    }

    @Test
    fun `risk engine locks trading when daily loss limit is hit`() {
        val stock = MarketDataProvider.getAllIndianStocks().first { it.symbol == "TATAMOTORS" }
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 24400.0,
            maxDailyLoss = 500.0,
            realizedPnlToday = -550.0
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList()
        )

        assertFalse(eval.isApproved)
        assertEquals(RiskStatus.LOCKED, eval.currentRiskStatus)
        assertTrue(eval.rejectionReason?.contains("LOSS LIMIT", ignoreCase = true) == true)
    }

    @Test
    fun `risk engine locks trading when daily target is achieved and profit lock is on`() {
        val stock = MarketDataProvider.getAllIndianStocks().first { it.symbol == "TATAMOTORS" }
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 26200.0,
            dailyTarget = 1000.0,
            profitLockEnabled = true,
            realizedPnlToday = 1100.0
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList()
        )

        assertFalse(eval.isApproved)
        assertEquals(RiskStatus.LOCKED, eval.currentRiskStatus)
        assertTrue(eval.rejectionReason?.contains("TARGET ACHIEVED", ignoreCase = true) == true)
    }

    @Test
    fun `risk engine enforces 1 percent max risk per trade for eligible equities`() {
        val stock = MarketDataProvider.getAllIndianStocks().first { it.symbol == "TATAMOTORS" } // ~₹964
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            maxRiskPerTradePercent = 1.0 // 1% = ₹250
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList()
        )

        assertTrue(eval.isApproved)
        assertTrue("Max permitted quantity should be at least 1", eval.maxPermittedQuantity >= 1)
        assertTrue("Calculated trade risk in rupees should not exceed ₹275", eval.estimatedRiskRupees <= 275.0)
    }

    @Test
    fun `risk engine rejects high priced assets that exceed 1 percent risk limit`() {
        val indexQuote = MarketDataProvider.getAllIndianStocks().first { it.symbol == "NIFTY 50" } // ₹24,820
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            maxRiskPerTradePercent = 1.0 // 1% = ₹250
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = indexQuote,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList()
        )

        assertFalse("High-priced asset with single-unit risk > ₹250 must be rejected", eval.isApproved)
        assertEquals(0, eval.maxPermittedQuantity)
    }
}
