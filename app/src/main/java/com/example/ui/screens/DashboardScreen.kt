package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BrokerConnectionEntity
import com.example.data.db.FundTransactionEntity
import com.example.data.db.PositionEntity
import com.example.data.model.DecisionType
import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import com.example.ui.components.AutoPilotCard
import com.example.ui.components.DailyPnLChart
import com.example.ui.components.DailyPnLTracker
import com.example.ui.components.DailyTradeStatsSummaryCard
import com.example.ui.components.DecisionBadge
import com.example.ui.components.FintechCard
import com.example.ui.components.PaperTradingPositionsSummary
import com.example.ui.components.PnLGrowthLineChart
import com.example.ui.components.Sparkline
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState

@Composable
fun DashboardScreen(
    uiState: TradePilotUiState,
    onNavigate: (Screen) -> Unit,
    onSelectStock: (StockQuote) -> Unit,
    onClosePosition: (PositionEntity, Double) -> Unit,
    onResetCapital: () -> Unit,
    onToggleAutoPilot: () -> Unit = {},
    onStartAutoPilot: () -> Unit = onToggleAutoPilot,
    onStopAutoPilot: () -> Unit = onToggleAutoPilot,
    onToggleAutoActivate: (Boolean) -> Unit = {},
    onRunAutoPilotCycle: () -> Unit = {},
    onOpenBrokerDialog: () -> Unit = {},
    onOpenAddFunds: () -> Unit = {},
    onToggleLiveMode: (Boolean) -> Unit = {},
    onToggleAutonomousAi: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val portfolio = uiState.portfolio
    val openPositions = uiState.positions
    val indianStocks = uiState.indianStocks
    val usStocks = uiState.usStocks
    val isLive = uiState.isLiveTradingMode
    val broker = uiState.brokerConnection

    // Calculate current portfolio value in INR
    val investedValueInr = openPositions.sumOf { pos ->
        val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
        pos.quantity * pos.currentPrice * mult
    }
    val activeCash = if (isLive) broker.liveEquityMargin else portfolio.availableCash
    val currentPortfolioValue = activeCash + investedValueInr
    val totalPnlToday = portfolio.realizedPnlToday
    val targetPercent = ((portfolio.realizedPnlToday / portfolio.dailyTarget) * 100.0).coerceIn(0.0, 100.0).toInt()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { screenPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {

            // 0. LIVE BROKER & MODE SELECTION BAR
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isLive) BearishRed.copy(alpha = 0.12f) else SurfaceElevated,
                    border = BorderStroke(1.dp, if (isLive) BearishRed.copy(alpha = 0.6f) else BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onOpenBrokerDialog)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLive) BearishRed.copy(alpha = 0.2f) else ElectricTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLive) Icons.Default.Bolt else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isLive) BearishRed else ElectricTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isLive) "🔴 LIVE: ZERODHA KITE" else "📄 PAPER TRADING",
                                        color = if (isLive) BearishRed else TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isLive) "(₹${"%,d".format(broker.liveEquityMargin.toInt())})" else "(₹25k Virtual)",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isLive) "Real orders routed to NSE/BSE • AI Autonomous" else "Safe sandbox simulation mode",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Add Live Money Button
                            Button(
                                onClick = onOpenAddFunds,
                                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("dashboard_add_funds_chip")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add Funds", color = BackgroundDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Switch to toggle live mode
                            Switch(
                                checked = isLive,
                                onCheckedChange = onToggleLiveMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BearishRed,
                                    uncheckedThumbColor = ElectricTeal,
                                    uncheckedTrackColor = SurfaceDark
                                ),
                                modifier = Modifier.testTag("dashboard_live_mode_switch")
                            )
                        }
                    }
                }
            }

            // 1. HERO CAPITAL & TARGET CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portfolio_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(BorderDark, ElectricTeal.copy(alpha = 0.3f)))
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceElevated.copy(alpha = 0.6f), SurfaceDark)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isLive) "LIVE BROKER CAPITAL (${broker.brokerName.uppercase()})" else "VIRTUAL CAPITAL (PAPER TRADING)",
                                    color = if (isLive) BearishRed else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${"%,d".format(currentPortfolioValue.toInt())}",
                                    color = TextPrimary,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Today's Realized P&L
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (totalPnlToday >= 0) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (totalPnlToday >= 0) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Today's P&L",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${if (totalPnlToday >= 0) "+₹" else "-₹"}${Math.abs(totalPnlToday).toInt()}",
                                        color = if (totalPnlToday >= 0) BullishGreen else BearishRed,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Daily Target Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Daily Target: ₹${portfolio.dailyTarget.toInt()}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$targetPercent% Achieved",
                                color = if (targetPercent >= 100) BullishGreen else ElectricTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Target Progress Indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(targetPercent / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(CyanBlue, ElectricTeal)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Critical Disclaimer & Risk Rules Notice
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundDark.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, BorderDark.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CyanBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Target is configurable; not guaranteed. Capital preservation rules enforce lock at -₹${portfolio.maxDailyLoss.toInt()} or +₹${portfolio.dailyTarget.toInt()}.",
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Capital Breakdown Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = if (isLive) "Live Margin" else "Available Cash",
                                value = "₹${"%,d".format((if (isLive) broker.liveEquityMargin else portfolio.availableCash).toInt())}"
                            )
                            MetricItem(label = "Invested", value = "₹${investedValueInr.toInt()}")
                            MetricItem(label = "Loss Limit", value = "-₹${portfolio.maxDailyLoss.toInt()}")
                            MetricItem(label = "Risk / Trade", value = "${portfolio.maxRiskPerTradePercent}%")
                        }
                    }
                }
            }
        }

        // 1.15. REAL-TIME DAILY P&L CHART (RECHARTS-STYLE LINE CHART: VALUE EVOLUTION VS DAILY TARGET THRESHOLD)
        item {
            DailyPnLChart(
                portfolio = portfolio,
                currentPortfolioValue = currentPortfolioValue,
                trades = uiState.journalEntries,
                onTargetThresholdClick = { onNavigate(Screen.RISK_CENTER) }
            )
        }

        // 1.25. REAL-TIME DAILY PNL TRACKER (READS ROOM DATABASE)
        item {
            DailyPnLTracker(
                portfolio = portfolio,
                trades = uiState.journalEntries,
                onTargetClick = { onNavigate(Screen.RISK_CENTER) }
            )
        }

        // 1.30. AGGREGATED DAILY TRADE STATISTICS SUMMARY CARD (TRADES, WIN/LOSS RATIO, NET P&L)
        item {
            DailyTradeStatsSummaryCard(
                portfolio = portfolio,
                trades = uiState.journalEntries,
                openPositions = openPositions,
                onViewJournal = { onNavigate(Screen.JOURNAL) },
                onViewDailyReport = { onNavigate(Screen.DAILY_SUMMARY) }
            )
        }

        // 1.35. AUTONOMOUS AUTO-PILOT TRADING BOT (ACTIVE IF TARGET NOT ACHIEVED, STOP/START OPTIONS)
        item {
            AutoPilotCard(
                portfolio = portfolio,
                isAutoPilotActive = uiState.isAutoPilotActive,
                autoPilotStatus = uiState.autoPilotStatus,
                autoPilotLastAction = uiState.autoPilotLastAction,
                autoActivateWhenTargetPending = uiState.autoActivateWhenTargetPending,
                isLiveMode = uiState.isLiveTradingMode,
                isAutonomousLive = uiState.autoPilotAutonomousLive,
                onToggleAutoPilot = onToggleAutoPilot,
                onStartAutoPilot = onStartAutoPilot,
                onStopAutoPilot = onStopAutoPilot,
                onToggleAutoActivate = onToggleAutoActivate,
                onRunManualCycle = onRunAutoPilotCycle,
                onOpenBrokerSettings = onOpenBrokerDialog
            )
        }

        // 1.5. DAILY SUMMARY PERFORMANCE REPORT BANNER
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate(Screen.DAILY_SUMMARY) }
                    .testTag("daily_summary_banner_button"),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Daily Performance Report", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Realized vs. Unrealized gains, win rate & target progress", color = TextMuted, fontSize = 10.5.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 1.6. BACKTEST SIMULATOR BANNER
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate(Screen.BACKTEST) }
                    .testTag("backtest_simulator_banner_button"),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Backtest Simulator", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Simulate strategy performance over historical OHLCV data", color = TextMuted, fontSize = 10.5.sp)
                        }
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 2. QUICK ACTIONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    label = "AI Council",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.AI_COUNCIL) }
                )
                QuickActionChip(
                    label = "AI Scanner",
                    icon = Icons.Default.Radar,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SCANNER) }
                )
                QuickActionChip(
                    label = "Markets",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.MARKETS) }
                )
                QuickActionChip(
                    label = "Strategy Lab",
                    icon = Icons.Default.Science,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.STRATEGIES) }
                )
            }
        }

        // 3. ACTIVE OPEN POSITIONS & PAPER TRADING SUMMARY
        item {
            PaperTradingPositionsSummary(
                positions = openPositions,
                availableCash = activeCash,
                startingCapital = portfolio.startingCapital,
                dailyTarget = portfolio.dailyTarget,
                realizedPnlToday = portfolio.realizedPnlToday,
                usdInrRate = portfolio.usdInrRate,
                onClosePosition = onClosePosition,
                onResetCapital = onResetCapital,
                onNavigateToScanner = { onNavigate(Screen.SCANNER) },
                onNavigateToRiskCenter = { onNavigate(Screen.RISK_CENTER) }
            )
        }

        // 4. TOP AI OPPORTUNITIES (MARKET SCANNER)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Council Scanner",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tri-Agent consensus (Gemini + Claude + ChatGPT)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = { onNavigate(Screen.MARKETS) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(text = "View All", color = ElectricTeal, fontSize = 12.sp)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        val topScans = (indianStocks.take(2) + usStocks.take(2))
        items(topScans) { stock ->
            AiStockScanItem(
                stock = stock,
                onClick = {
                    onSelectStock(stock)
                    onNavigate(Screen.AI_COUNCIL)
                }
            )
        }
    }
}
}

@Composable
fun PositionCardItem(
    position: PositionEntity,
    usdRate: Double,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mult = if (position.market == "USA") usdRate else 1.0
    val pnlTotalInr = (position.currentPrice - position.buyPrice) * position.quantity * mult
    val pnlPct = if (position.buyPrice > 0) ((position.currentPrice - position.buyPrice) / position.buyPrice) * 100.0 else 0.0
    val isProfit = pnlTotalInr >= 0

    FintechCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = position.symbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceElevated
                    ) {
                        Text(
                            text = if (position.market == "USA") "US 🇺🇸" else "IN 🇮🇳",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${position.quantity} Qty • Avg: ${if (position.market == "USA") "$" else "₹"}${position.buyPrice}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isProfit) "+₹" else "-₹"}${Math.abs(pnlTotalInr).toInt()}",
                    color = if (isProfit) BullishGreen else BearishRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (pnlPct >= 0) "+" else ""}${"%.2f".format(pnlPct)}%",
                    color = if (isProfit) BullishGreen else BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SL: ${if (position.market == "USA") "$" else "₹"}${position.stopLoss}  |  TP: ${if (position.market == "USA") "$" else "₹"}${position.takeProfit}",
                color = TextMuted,
                fontSize = 11.sp
            )

            Button(
                onClick = onCloseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProfit) BullishGreenContainer else SurfaceElevated
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "Close Trade",
                    color = if (isProfit) BullishGreen else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AiStockScanItem(
    stock: StockQuote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FintechCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceElevated
                    ) {
                        Text(
                            text = stock.sector,
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${stock.market.currencySymbol}${stock.currentPrice}  (${if (stock.priceChangePercent >= 0) "+" else ""}${stock.priceChangePercent}%)",
                    color = if (stock.priceChangePercent >= 0) BullishGreen else BearishRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Mini sparkline
            Sparkline(
                data = stock.sparklineData,
                isPositive = stock.priceChangePercent >= 0,
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // AI Decision badge
            val rec = if (stock.rsi in 50.0..66.0 && stock.priceChangePercent > 0.5) DecisionType.BUY else DecisionType.WAIT
            DecisionBadge(decision = rec)
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ElectricTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
