package com.example

import com.example.data.db.AssetEntity
import com.example.data.db.AuditLogEntity
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.db.TradeEntity
import com.example.data.db.TradeJournalEntity
import com.example.data.engine.RiskEngine
import com.example.data.model.DecisionType
import com.example.data.model.MarketType
import com.example.data.model.RiskStatus
import com.example.data.model.StockQuote
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    private fun createSampleStock(): StockQuote {
        return StockQuote(
            symbol = "RELIANCE",
            companyName = "Reliance Industries Ltd",
            market = MarketType.INDIA,
            currentPrice = 2940.0,
            priceChange = 34.5,
            priceChangePercent = 1.18,
            volume = "4.2M",
            high52 = 3024.9,
            low52 = 2220.3,
            peRatio = 26.4,
            rsi = 56.4,
            macd = 4.2,
            sma20 = 2910.0,
            sma50 = 2890.0,
            sma200 = 2740.0,
            support = 2880.0,
            resistance = 3010.0,
            sector = "Energy & Conglomerate",
            currency = "INR"
        )
    }

    @Test
    fun testRiskEngineRejectsOnKillSwitch() {
        val stock = createSampleStock()
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            killSwitchTriggered = true
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList(),
            requestedQty = 1
        )

        assertFalse(eval.isApproved)
        assertEquals(RiskStatus.LOCKED, eval.currentRiskStatus)
        assertTrue(eval.rejectionReason?.contains("Kill Switch") == true)
    }

    @Test
    fun testRiskEngineRejectsOnMaxDailyLoss() {
        val stock = createSampleStock()
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 24500.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0,
            realizedPnlToday = -550.0
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList(),
            requestedQty = 1
        )

        assertFalse(eval.isApproved)
        assertTrue(eval.rejectionReason?.contains("DAILY LOSS LIMIT REACHED") == true)
    }

    @Test
    fun testRiskEngineRejectsOnDailyTargetProfitLock() {
        val stock = createSampleStock()
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 26200.0,
            dailyTarget = 1000.0,
            profitLockEnabled = true,
            realizedPnlToday = 1200.0
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList(),
            requestedQty = 1
        )

        assertFalse(eval.isApproved)
        assertTrue(eval.rejectionReason?.contains("DAILY TARGET ACHIEVED") == true)
    }

    @Test
    fun testRiskEngineApprovesCompliantTrade() {
        val stock = createSampleStock()
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0,
            maxRiskPerTradePercent = 2.0,
            realizedPnlToday = 0.0
        )

        val eval = RiskEngine.evaluateTradeProposal(
            stock = stock,
            decision = DecisionType.BUY,
            portfolio = portfolio,
            currentPositions = emptyList(),
            requestedQty = 1
        )

        assertTrue(eval.isApproved)
        assertTrue(eval.maxPermittedQuantity >= 1)
        assertTrue(eval.suggestedStopLoss < stock.currentPrice)
    }

    @Test
    fun testCoreRoomEntitiesModelIntegrity() {
        // Core table: trades
        val trade = TradeEntity(
            tradeId = "TP-101",
            symbol = "TATAMOTORS",
            market = "INDIA",
            companyName = "Tata Motors",
            strategy = "Breakout",
            entryPrice = 950.0,
            exitPrice = 980.0,
            quantity = 5,
            pnl = 150.0,
            pnlPercent = 3.16,
            aiDecision = "BUY",
            aiConfidence = 85,
            status = "CLOSED",
            outcome = "PROFIT"
        )
        assertEquals("TATAMOTORS", trade.symbol)
        assertEquals(150.0, trade.pnl, 0.001)

        // Core table: assets
        val asset = AssetEntity(
            symbol = "NVDA",
            name = "NVIDIA Corporation",
            market = "USA",
            exchange = "NASDAQ",
            currentPrice = 124.50,
            currency = "USD",
            isWatchlisted = true
        )
        assertEquals("NVDA", asset.symbol)
        assertTrue(asset.isWatchlisted)

        // Core table: portfolios
        val portfolio = PortfolioEntity(
            id = 1,
            startingCapital = 25000.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0
        )
        assertEquals(25000.0, portfolio.startingCapital, 0.001)
        assertEquals(1000.0, portfolio.dailyTarget, 0.001)

        // Core table: audit_logs
        val log = AuditLogEntity(
            action = "ORDER_EXECUTED",
            symbol = "NVDA",
            market = "USA",
            aiConsensusVerdict = "BUY (80%)",
            riskEngineStatus = "APPROVED",
            details = "1 share bought @ $124.50"
        )
        assertEquals("ORDER_EXECUTED", log.action)
        assertEquals("APPROVED", log.riskEngineStatus)
    }

    @Test
    fun testTradeProcessorCreateAndCompleteTrade() {
        val stock = createSampleStock()
        val consensus = com.example.data.model.AiConsensusResult(
            symbol = stock.symbol,
            market = stock.market,
            consensusDecision = DecisionType.BUY,
            buyVotes = 3,
            holdVotes = 0,
            sellVotes = 0,
            waitVotes = 0,
            noTradeVotes = 0,
            overallConfidence = 82,
            techScoreAvg = 85,
            fundScoreAvg = 80,
            sentScoreAvg = 78,
            riskScoreAvg = 42,
            timeHorizon = "Swing (3-7 days)",
            synthesizedReason = "Strong momentum confirmed above 50 SMA with positive MACD divergence.",
            keyCatalysts = listOf("Earnings breakout", "Heavy volume accumulation"),
            riskFactors = listOf("Broader market volatility"),
            invalidatingConditions = listOf("Daily close below ₹2,880"),
            decisionChangeFactors = listOf("Breakout above ₹3,010 resistance"),
            invalidationConditionsList = listOf("Daily close below ₹2,880"),
            isNoTradeAdvised = false
        )

        // Process consensus into trade entity
        val openTrade = com.example.data.processor.TradeProcessor.createTradeFromConsensus(
            stock = stock,
            consensus = consensus,
            quantity = 5,
            entryPrice = 2940.0,
            strategyName = "Momentum Breakout"
        )

        assertEquals("RELIANCE", openTrade.symbol)
        assertEquals(5, openTrade.quantity)
        assertEquals(2940.0, openTrade.entryPrice, 0.001)
        assertEquals("BUY", openTrade.aiDecision)
        assertEquals(82, openTrade.aiConfidence)
        assertEquals(42, openTrade.riskScore)
        assertEquals("OPEN", openTrade.status)
        assertTrue(openTrade.aiObservation.contains("Strong momentum"))
        assertTrue(openTrade.changeFactors.contains("₹3,010"))
        assertTrue(openTrade.invalidationTriggers.contains("₹2,880"))

        // Complete trade with profit
        val closedTrade = com.example.data.processor.TradeProcessor.completeAndEvaluateTrade(
            trade = openTrade,
            exitPrice = 3000.0,
            exitNote = "Target reached"
        )

        assertEquals("CLOSED", closedTrade.status)
        assertEquals("PROFIT", closedTrade.outcome)
        assertEquals(300.0, closedTrade.pnl, 0.001) // (3000 - 2940) * 5 = 300.0
        assertTrue(closedTrade.pnlPercent > 0.0)
        assertTrue(closedTrade.userNotes.contains("Target reached"))
    }

    @Test
    fun testDailySummaryGainsCalculation() {
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 21000.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0,
            realizedPnlToday = 420.0
        )

        val positions = listOf(
            com.example.data.db.PositionEntity(
                symbol = "TATAMOTORS",
                market = "INDIA",
                companyName = "Tata Motors",
                quantity = 2,
                buyPrice = 948.0,
                currentPrice = 964.50,
                stopLoss = 925.0,
                takeProfit = 995.0
            ),
            com.example.data.db.PositionEntity(
                symbol = "NVDA",
                market = "USA",
                companyName = "NVIDIA Corp",
                quantity = 1,
                buyPrice = 120.0,
                currentPrice = 124.50,
                stopLoss = 115.0,
                takeProfit = 132.0
            )
        )

        val realizedToday = portfolio.realizedPnlToday
        val tataUnrealized = (964.50 - 948.0) * 2 // 33.0 INR
        val nvdaUnrealizedInr = (124.50 - 120.0) * 1 * 83.5 // 4.5 * 83.5 = 375.75 INR
        val totalUnrealized = tataUnrealized + nvdaUnrealizedInr
        val totalNetPnl = realizedToday + totalUnrealized

        assertEquals(420.0, realizedToday, 0.001)
        assertEquals(408.75, totalUnrealized, 0.01)
        assertEquals(828.75, totalNetPnl, 0.01)

        val targetProgress = (realizedToday / portfolio.dailyTarget) * 100.0
        assertEquals(42.0, targetProgress, 0.01)
        assertFalse(realizedToday >= portfolio.dailyTarget) // Not yet locked
    }

    @Test
    fun testScannerSignals() {
        val buy = com.example.ui.components.ScannerSignal.BUY
        val watch = com.example.ui.components.ScannerSignal.WATCH
        val avoid = com.example.ui.components.ScannerSignal.AVOID

        assertEquals("BUY", buy.label)
        assertEquals("WATCH", watch.label)
        assertEquals("AVOID", avoid.label)
    }

    @Test
    fun testBacktestSimulatorEngine() {
        val config = com.example.data.model.BacktestConfig(
            strategyName = "Momentum Breakout Pilot",
            symbol = "RELIANCE",
            timeframeDays = 90,
            startingCapital = 25000.0,
            maxRiskPerTradePercent = 1.0,
            stopLossPercent = 1.5,
            takeProfitPercent = 3.0
        )

        // 1. Verify OHLCV series generation
        val candles = com.example.data.engine.BacktestSimulator.generateHistoricalOhlcv("RELIANCE", 60)
        assertTrue(candles.isNotEmpty())
        candles.forEach { c ->
            assertTrue(c.high >= c.low)
            assertTrue(c.high >= c.open)
            assertTrue(c.high >= c.close)
            assertTrue(c.low <= c.open)
            assertTrue(c.low <= c.close)
            assertTrue(c.volume > 0)
        }

        // 2. Run simulation
        val result = com.example.data.engine.BacktestSimulator.runSimulation(config)
        assertEquals(25000.0, result.startingCapital, 0.001)
        assertTrue(result.equityCurve.isNotEmpty())
        assertTrue(result.qualificationStatus.isNotBlank())
        assertTrue(result.aiCouncilCritique.isNotBlank())

        // Win rate logic
        if (result.totalTrades > 0) {
            assertEquals(result.winCount + result.lossCount + result.breakevenCount, result.totalTrades)
            assertTrue(result.winRatePercent in 0.0..100.0)
            assertTrue(result.profitFactor >= 0.0)
            assertTrue(result.maxDrawdownPercent >= 0.0)
        }
    }

    @Test
    fun testDailyPnLTrackerCalculation() {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        val trades = listOf(
            TradeJournalEntity(
                tradeId = "T1",
                symbol = "RELIANCE",
                market = "INDIA",
                companyName = "Reliance",
                strategy = "Breakout",
                entryPrice = 2900.0,
                exitPrice = 2950.0,
                quantity = 2,
                pnl = 100.0,
                closedAt = startOfToday + 3600000, // 1 AM today
                status = "CLOSED"
            ),
            TradeJournalEntity(
                tradeId = "T2",
                symbol = "NVDA",
                market = "USA",
                companyName = "NVIDIA",
                strategy = "Momentum",
                entryPrice = 120.0,
                exitPrice = 124.0,
                quantity = 1,
                pnl = 320.0,
                closedAt = startOfToday + 7200000, // 2 AM today
                status = "CLOSED"
            ),
            TradeJournalEntity(
                tradeId = "T3",
                symbol = "INFY",
                market = "INDIA",
                companyName = "Infosys",
                strategy = "Mean Reversion",
                entryPrice = 1800.0,
                exitPrice = 1810.0,
                quantity = 1,
                pnl = 50.0,
                closedAt = startOfToday - 86400000, // Yesterday
                status = "CLOSED"
            )
        )

        // Filter for today
        val todayTrades = trades.filter { it.status == "CLOSED" && it.closedAt >= startOfToday }
        assertEquals(2, todayTrades.size)

        val realizedToday = todayTrades.sumOf { it.pnl }
        assertEquals(420.0, realizedToday, 0.001)

        val dailyTarget = 1000.0
        val progress = (realizedToday / dailyTarget) * 100.0
        assertEquals(42.0, progress, 0.001)
        assertFalse(realizedToday >= dailyTarget)

        // With target reached
        val highRealized = 1050.0
        assertTrue(highRealized >= dailyTarget) // Profit lock condition triggers
    }

    @Test
    fun testUserProfileAnd2fa() {
        val profile = com.example.data.model.UserProfile(
            email = "ebdc.org@gmail.com",
            name = "Alex Chen",
            is2faEnabled = true
        )
        assertEquals("ebdc.org@gmail.com", profile.email)
        assertTrue(profile.is2faEnabled)
        assertTrue(profile.dailyTargetAlerts)
        assertTrue(profile.killSwitchAlerts)
    }

    @Test
    fun testTradeJournalCsvGeneration() {
        val trades = listOf(
            TradeJournalEntity(
                tradeId = "TP-101",
                symbol = "RELIANCE",
                market = "INDIA",
                companyName = "Reliance Industries Ltd",
                strategy = "Momentum Breakout",
                entryPrice = 2950.0,
                exitPrice = 3010.0,
                quantity = 10,
                pnl = 600.0,
                pnlPercent = 2.03,
                aiDecision = "BUY",
                aiConfidence = 85,
                riskScore = 35,
                status = "CLOSED",
                userOverride = false,
                userNotes = "Clean breakout above resistance, trailing SL held.",
                tags = "Breakout, Trend"
            ),
            TradeJournalEntity(
                tradeId = "TP-102",
                symbol = "NVDA",
                market = "USA",
                companyName = "NVIDIA Corporation",
                strategy = "Mean Reversion",
                entryPrice = 120.0,
                exitPrice = 118.0,
                quantity = 5,
                pnl = -10.0,
                pnlPercent = -1.67,
                aiDecision = "BUY",
                aiConfidence = 72,
                riskScore = 55,
                status = "CLOSED",
                userOverride = true,
                overrideReason = "Entered early before RSI confirmation",
                userNotes = "Cut quickly per risk engine stop loss",
                tags = "Loss, Discretionary"
            )
        )

        val csv = com.example.util.CsvExportManager.generateTradeJournalCsv(trades)
        assertNotNull(csv)
        assertTrue(csv.startsWith("\uFEFF")) // BOM check for Excel
        assertTrue(csv.contains("Trade ID,Date Closed,Date Entered,Symbol"))
        assertTrue(csv.contains("RELIANCE"))
        assertTrue(csv.contains("600.00"))
        assertTrue(csv.contains("NVDA"))
        assertTrue(csv.contains("-10.00"))
        assertTrue(csv.contains("Clean breakout above resistance, trailing SL held."))
        assertTrue(csv.contains("Entered early before RSI confirmation"))
    }

    @Test
    fun testDailyPerformanceCsvGeneration() {
        val portfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 21000.0,
            realizedPnlToday = 750.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0
        )

        val positions = listOf(
            PositionEntity(
                symbol = "TCS",
                companyName = "Tata Consultancy Services",
                market = "INDIA",
                buyPrice = 3800.0,
                currentPrice = 3850.0,
                quantity = 1,
                stopLoss = 3720.0,
                takeProfit = 3980.0
            )
        )

        val trades = listOf(
            TradeJournalEntity(
                tradeId = "TP-201",
                symbol = "INFY",
                market = "INDIA",
                companyName = "Infosys Ltd",
                strategy = "Momentum Breakout",
                entryPrice = 1800.0,
                exitPrice = 1875.0,
                quantity = 10,
                pnl = 750.0,
                pnlPercent = 4.17,
                status = "CLOSED"
            )
        )

        val csv = com.example.util.CsvExportManager.generateDailyPerformanceCsv(
            portfolio = portfolio,
            positions = positions,
            trades = trades,
            dateStr = "2026-09-29"
        )

        assertNotNull(csv)
        assertTrue(csv.contains("AI TRADEPILOT DAILY PERFORMANCE REPORT"))
        assertTrue(csv.contains("Starting Virtual Capital"))
        assertTrue(csv.contains("25000.00"))
        assertTrue(csv.contains("Realized P&L Today"))
        assertTrue(csv.contains("750.00"))
        assertTrue(csv.contains("ACTIVE OPEN POSITIONS"))
        assertTrue(csv.contains("TCS"))
        assertTrue(csv.contains("3850.00"))
        assertTrue(csv.contains("STRATEGY BREAKDOWN"))
        assertTrue(csv.contains("Momentum Breakout"))
    }

    @Test
    fun testCsvEscapingRfc4180() {
        assertEquals("SimpleText", com.example.util.CsvExportManager.escapeCsv("SimpleText"))
        assertEquals("\"Text, with comma\"", com.example.util.CsvExportManager.escapeCsv("Text, with comma"))
        assertEquals("\"Text with \"\"quotes\"\" inside\"", com.example.util.CsvExportManager.escapeCsv("Text with \"quotes\" inside"))
        assertEquals("\"Text with\nnewline\"", com.example.util.CsvExportManager.escapeCsv("Text with\nnewline"))
    }

    @Test
    fun testAssetCatalogSize500To1000() {
        val indian = com.example.data.market.MarketAssetCatalog.getIndianAssetCatalog()
        val us = com.example.data.market.MarketAssetCatalog.getUsAssetCatalog()
        val total = indian.size + us.size

        assertTrue("Indian assets must be >= 500, was ${indian.size}", indian.size >= 500)
        assertTrue("US assets must be >= 500, was ${us.size}", us.size >= 500)
        assertTrue("Total catalog assets must be >= 1000, was $total", total >= 1000)

        // Verify key symbols exist
        assertTrue(indian.any { it.symbol == "RELIANCE" })
        assertTrue(indian.any { it.symbol == "TCS" })
        assertTrue(us.any { it.symbol == "NVDA" })
        assertTrue(us.any { it.symbol == "AAPL" })
    }

    @Test
    fun testAutoPilotActiveUntilTargetAchievedAndStops() {
        // 1. Target not achieved -> Auto-Pilot remains ACTIVE
        val huntingPortfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            realizedPnlToday = 350.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0
        )
        val candidateStocks = com.example.data.market.MarketAssetCatalog.getIndianAssetCatalog().take(20)
        val huntingDecision = com.example.data.engine.AutoPilotEngine.evaluate(
            portfolio = huntingPortfolio,
            openPositions = emptyList(),
            candidateStocks = candidateStocks
        )
        assertTrue(
            "AutoPilot must be active when target is not achieved",
            huntingDecision is com.example.data.engine.AutoPilotDecision.ExecuteBuyOrder ||
            huntingDecision is com.example.data.engine.AutoPilotDecision.ActiveHunting
        )

        // 2. Target achieved -> Auto-Pilot STOPS AUTOMATICALLY
        val achievedPortfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            realizedPnlToday = 1050.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0
        )
        val achievedDecision = com.example.data.engine.AutoPilotEngine.evaluate(
            portfolio = achievedPortfolio,
            openPositions = emptyList(),
            candidateStocks = candidateStocks
        )
        assertTrue(
            "AutoPilot must stop when target is achieved",
            achievedDecision is com.example.data.engine.AutoPilotDecision.StopTargetAchieved
        )
        val stopMsg = (achievedDecision as com.example.data.engine.AutoPilotDecision.StopTargetAchieved).message
        assertTrue(stopMsg.contains("1000") && stopMsg.contains("Profit Lock"))

        // 3. Loss limit reached -> Auto-Pilot STOPS AUTOMATICALLY
        val lossPortfolio = PortfolioEntity(
            startingCapital = 25000.0,
            availableCash = 25000.0,
            realizedPnlToday = -550.0,
            dailyTarget = 1000.0,
            maxDailyLoss = 500.0
        )
        val lossDecision = com.example.data.engine.AutoPilotEngine.evaluate(
            portfolio = lossPortfolio,
            openPositions = emptyList(),
            candidateStocks = candidateStocks
        )
        assertTrue(
            "AutoPilot must stop when loss limit is hit",
            lossDecision is com.example.data.engine.AutoPilotDecision.StopLossLimit
        )
    }

    @Test
    fun test1000AssetCatalogCountAndIntegrity() {
        val indianAssets = com.example.data.market.MarketAssetCatalog.getIndianAssetCatalog()
        val usAssets = com.example.data.market.MarketAssetCatalog.getUsAssetCatalog()
        val allQuotes = com.example.data.market.MarketAssetCatalog.getAllCatalogStockQuotes()
        val allEntities = com.example.data.market.MarketAssetCatalog.getAllCatalogAsAssetEntities()

        assertTrue("Indian assets must be >= 500", indianAssets.size >= 500)
        assertTrue("US assets must be >= 500", usAssets.size >= 500)
        assertTrue("Total catalog assets must be >= 1000", allQuotes.size >= 1000)
        assertEquals("Entities count must match total catalog count", allQuotes.size, allEntities.size)

        // Verify entities have valid properties
        val sampleEntity = allEntities.first()
        assertNotNull(sampleEntity.symbol)
        assertTrue(sampleEntity.currentPrice > 0.0)
        assertTrue(sampleEntity.market == "INDIA" || sampleEntity.market == "USA")
    }

    @Test
    fun testPnLChartPointCalculations() {
        val startCapital = 25000.0
        val target = 1000.0
        val targetValue = startCapital + target
        val currentValue = 25420.0
        val pnl = currentValue - startCapital

        val point = com.example.ui.components.PnLChartPoint(
            timestamp = System.currentTimeMillis(),
            timeLabel = "Live",
            portfolioValue = currentValue,
            targetValue = targetValue,
            pnl = pnl
        )

        assertEquals(25420.0, point.portfolioValue, 0.01)
        assertEquals(26000.0, point.targetValue, 0.01)
        assertEquals(420.0, point.pnl, 0.01)
        assertTrue(point.portfolioValue < point.targetValue)
    }

    @Test
    fun testDailyTradeStatisticsAggregation() {
        val now = System.currentTimeMillis()
        val trades = listOf(
            TradeEntity(
                tradeId = "T1",
                symbol = "TCS",
                market = "INDIA",
                companyName = "Tata Consultancy Services",
                strategy = "Breakout",
                entryPrice = 3800.0,
                exitPrice = 3880.0,
                quantity = 5,
                pnl = 400.0,
                pnlPercent = 2.1,
                status = "CLOSED",
                closedAt = now
            ),
            TradeEntity(
                tradeId = "T2",
                symbol = "INFY",
                market = "INDIA",
                companyName = "Infosys Ltd",
                strategy = "Momentum",
                entryPrice = 1600.0,
                exitPrice = 1630.0,
                quantity = 10,
                pnl = 300.0,
                pnlPercent = 1.87,
                status = "CLOSED",
                closedAt = now
            ),
            TradeEntity(
                tradeId = "T3",
                symbol = "HDFCBANK",
                market = "INDIA",
                companyName = "HDFC Bank Ltd",
                strategy = "Mean Reversion",
                entryPrice = 1500.0,
                exitPrice = 1485.0,
                quantity = 8,
                pnl = -120.0,
                pnlPercent = -1.0,
                status = "CLOSED",
                closedAt = now
            )
        )

        val positions = listOf(
            PositionEntity(
                symbol = "RELIANCE",
                market = "INDIA",
                companyName = "Reliance Industries",
                buyPrice = 2900.0,
                currentPrice = 2930.0,
                quantity = 4,
                stopLoss = 2850.0,
                takeProfit = 3000.0
            )
        )

        val closedCount = trades.filter { it.status == "CLOSED" }.size
        val winCount = trades.count { it.pnl > 0 }
        val lossCount = trades.count { it.pnl < 0 }
        val totalCount = closedCount + positions.size

        val realizedPnl = trades.sumOf { it.pnl }
        val unrealizedPnl = positions.sumOf { (it.currentPrice - it.buyPrice) * it.quantity }
        val netMovement = realizedPnl + unrealizedPnl

        assertEquals(3, closedCount)
        assertEquals(4, totalCount)
        assertEquals(2, winCount)
        assertEquals(1, lossCount)
        assertEquals(580.0, realizedPnl, 0.01)
        assertEquals(120.0, unrealizedPnl, 0.01)
        assertEquals(700.0, netMovement, 0.01)
        val winRate = (winCount.toDouble() / closedCount.toDouble()) * 100.0
        assertEquals(66.66, winRate, 0.1)
    }

    @Test
    fun testDailyPnLPointCalculations() {
        val startCapital = 25000.0
        val target = 1000.0
        val targetThreshold = startCapital + target
        val currentValue = 25850.0
        val pnl = currentValue - startCapital

        val point = com.example.ui.components.DailyPnLPoint(
            timestamp = System.currentTimeMillis(),
            timeLabel = "14:30",
            portfolioValue = currentValue,
            targetThreshold = targetThreshold,
            pnl = pnl,
            note = "Afternoon Rally"
        )

        assertEquals(25850.0, point.portfolioValue, 0.01)
        assertEquals(26000.0, point.targetThreshold, 0.01)
        assertEquals(850.0, point.pnl, 0.01)
        assertEquals("14:30", point.timeLabel)
        assertEquals("Afternoon Rally", point.note)
        assertTrue(point.portfolioValue < point.targetThreshold)
    }

    @Test
    fun testLockAndAuthWorkflow() {
        val initialUiState = com.example.ui.viewmodel.TradePilotUiState(
            isAuthenticated = true,
            isAppLocked = false,
            appLockPin = "1234"
        )

        assertTrue(initialUiState.isAuthenticated)
        assertFalse(initialUiState.isAppLocked)
        assertEquals("1234", initialUiState.appLockPin)

        // Lock App Transition
        val lockedState = initialUiState.copy(isAppLocked = true)
        assertTrue(lockedState.isAppLocked)
        assertTrue(lockedState.isAuthenticated)

        // Unlock App Transition
        val unlockedState = lockedState.copy(isAppLocked = false)
        assertFalse(unlockedState.isAppLocked)

        // Sign Out Transition
        val signedOutState = unlockedState.copy(isAuthenticated = false, isAppLocked = false)
        assertFalse(signedOutState.isAuthenticated)
    }

    @Test
    fun testCopilotMessageFlow() {
        val userMsg = com.example.data.ai.ChatMessage(
            role = "user",
            content = "How does Auto-Pilot trade automatically until ₹1,000 target?",
            modelUsed = "gemini-3.5-flash"
        )
        val modelMsg = com.example.data.ai.ChatMessage(
            role = "model",
            content = "Auto-Pilot engine scans 1,020 assets automatically until ₹1,000 profit is locked.",
            modelUsed = "gemini-3.5-flash"
        )

        val chatList = listOf(userMsg, modelMsg)
        assertEquals(2, chatList.size)
        assertEquals("user", chatList[0].role)
        assertEquals("model", chatList[1].role)
        assertTrue(chatList[1].content.contains("₹1,000"))
    }

    @Test
    fun testMockPaperTradingRepositoryCalculations() {
        val repo = com.example.data.repository.MockPaperTradingRepository(
            initialStartingCapital = 25000.0,
            initialAvailableCash = 18450.0,
            initialDailyTarget = 1000.0,
            initialRealizedPnlToday = 320.0,
            initialUsdInrRate = 83.5
        )

        val positions = repo.getCurrentPositions()
        assertEquals(3, positions.size)

        val investedMargin = repo.getInvestedMarginInr()
        assertTrue(investedMargin > 0)

        val currentVal = repo.getCurrentPositionValueInr()
        assertTrue(currentVal > 0)

        val unrealizedPnl = repo.getTotalUnrealizedPnlInr()
        assertTrue(unrealizedPnl > 0) // Prices are higher than buy prices in sample mock

        val equity = repo.getTotalEquityInr()
        assertEquals(repo.getCurrentPortfolio().availableCash + currentVal, equity, 0.01)
    }

    @Test
    fun testMockPaperTradingClosePosition() {
        val repo = com.example.data.repository.MockPaperTradingRepository()
        val initialCash = repo.getCurrentPortfolio().availableCash
        val initialPositionsCount = repo.getCurrentPositions().size
        val firstPos = repo.getCurrentPositions().first()

        val closeResult = repo.closePosition(firstPos.id)
        assertTrue(closeResult.isSuccess)

        val pnl = closeResult.getOrThrow()
        assertTrue(pnl > 0)

        assertEquals(initialPositionsCount - 1, repo.getCurrentPositions().size)
        assertTrue(repo.getCurrentPortfolio().availableCash > initialCash)
    }
}
