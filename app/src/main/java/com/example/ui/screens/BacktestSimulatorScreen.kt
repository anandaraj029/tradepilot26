package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.BacktestSimulator
import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestSummaryResult
import com.example.data.model.BacktestTrade
import com.example.ui.components.FintechCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.round

/**
 * Backtest Simulator module screen that simulates past performance of user-defined
 * and benchmark AI strategies using historical OHLCV data and displays results
 * in an interactive performance summary table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestSimulatorScreen(
    uiState: TradePilotUiState,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Available strategies: Prebuilt + Custom
    val strategyOptions = remember(uiState.standardStrategies, uiState.customStrategies) {
        val std = uiState.standardStrategies.map { it.name }
        val cst = uiState.customStrategies.map { it.name }
        if (cst.isNotEmpty()) std + cst else std
    }

    val symbolOptions = listOf(
        "RELIANCE", "TATAMOTORS", "HDFCBANK", "INFY", "NVDA", "AAPL", "MSFT"
    )

    // Configuration state
    var selectedStrategy by remember { mutableStateOf(strategyOptions.firstOrNull() ?: "Momentum Breakout Pilot") }
    var selectedSymbol by remember { mutableStateOf("RELIANCE") }
    var selectedTimeframeDays by remember { mutableIntStateOf(90) }
    var stopLossPercent by remember { mutableDoubleStateOf(1.5) }
    var takeProfitPercent by remember { mutableDoubleStateOf(3.0) }
    var maxRiskPercent by remember { mutableDoubleStateOf(1.0) }

    // Simulation Execution state
    var isSimulating by remember { mutableStateOf(false) }
    var backtestResult by remember {
        mutableStateOf<BacktestSummaryResult?>(
            BacktestSimulator.runSimulation(
                BacktestConfig(
                    strategyName = selectedStrategy,
                    symbol = selectedSymbol,
                    timeframeDays = selectedTimeframeDays,
                    stopLossPercent = stopLossPercent,
                    takeProfitPercent = takeProfitPercent,
                    maxRiskPerTradePercent = maxRiskPercent
                )
            )
        )
    }

    var showConfigPanel by remember { mutableStateOf(true) }
    var showTradeHistoryTable by remember { mutableStateOf(false) }

    fun executeBacktest() {
        coroutineScope.launch {
            isSimulating = true
            delay(350) // Smooth simulated compute feedback
            val result = BacktestSimulator.runSimulation(
                BacktestConfig(
                    strategyName = selectedStrategy,
                    symbol = selectedSymbol,
                    timeframeDays = selectedTimeframeDays,
                    stopLossPercent = stopLossPercent,
                    takeProfitPercent = takeProfitPercent,
                    maxRiskPerTradePercent = maxRiskPercent
                )
            )
            backtestResult = result
            isSimulating = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("backtest_simulator_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // --- 1. TOP NAVIGATION HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("backtest_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Backtest Simulator",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { executeBacktest() },
                    modifier = Modifier.testTag("re_run_backtest_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rerun",
                        tint = ElectricTeal
                    )
                }
            }
        }

        // --- 2. HERO SIMULATOR BANNER ---
        item {
            FintechCard(
                borderColor = ElectricTeal.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = null,
                                    tint = ElectricTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Historical OHLCV Engine",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simulate past executions against deterministic risk rules",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = "${selectedTimeframeDays}D Period",
                            color = CyanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Backtest simulation tests candle-by-candle price action, evaluating slippage, stops, targets, and max drawdown limits before live paper allocation.",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // --- 3. CONFIGURATION PANEL ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backtest_config_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showConfigPanel = !showConfigPanel },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Simulation Parameters",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = if (showConfigPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showConfigPanel) "Collapse" else "Expand",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(visible = showConfigPanel) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Strategy Selector
                            Text("Strategy Under Test", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(strategyOptions) { strat ->
                                    val isSel = selectedStrategy == strat
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) ElectricTeal.copy(alpha = 0.2f) else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isSel) ElectricTeal else BorderDark),
                                        modifier = Modifier.clickable { selectedStrategy = strat }
                                    ) {
                                        Text(
                                            text = strat,
                                            color = if (isSel) ElectricTeal else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Asset / Symbol Selector
                            Text("Asset Historical Data", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(symbolOptions) { sym ->
                                    val isSel = selectedSymbol == sym
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CyanBlue.copy(alpha = 0.2f) else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isSel) CyanBlue else BorderDark),
                                        modifier = Modifier.clickable { selectedSymbol = sym }
                                    ) {
                                        Text(
                                            text = sym,
                                            color = if (isSel) CyanBlue else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Timeframe Horizon (30D, 90D, 180D, 365D)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Backtest Period", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(30 to "30D", 90 to "90D", 180 to "180D", 365 to "1 Year").forEach { (days, label) ->
                                        val isSel = selectedTimeframeDays == days
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) ElectricTeal.copy(alpha = 0.2f) else SurfaceDark,
                                            border = BorderStroke(1.dp, if (isSel) ElectricTeal else BorderDark),
                                            modifier = Modifier.clickable { selectedTimeframeDays = days }
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) ElectricTeal else TextMuted,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Risk Parameters (SL %, TP %, Max Risk %)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Stop Loss: $stopLossPercent%", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = stopLossPercent.toFloat(),
                                        onValueChange = { stopLossPercent = round(it * 10.0) / 10.0 },
                                        valueRange = 0.5f..3.5f,
                                        colors = SliderDefaults.colors(thumbColor = BearishRed, activeTrackColor = BearishRed)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val rr = if (stopLossPercent > 0) round((takeProfitPercent / stopLossPercent) * 10.0) / 10.0 else 2.0
                                    Text("Target: $takeProfitPercent% (1:${rr} R:R)", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = takeProfitPercent.toFloat(),
                                        onValueChange = { takeProfitPercent = round(it * 10.0) / 10.0 },
                                        valueRange = 1.0f..6.0f,
                                        colors = SliderDefaults.colors(thumbColor = BullishGreen, activeTrackColor = BullishGreen)
                                    )
                                }
                            }

                            // Run Simulation Button
                            Button(
                                onClick = { executeBacktest() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("run_backtest_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSimulating
                            ) {
                                if (isSimulating) {
                                    CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Computing OHLCV Walk...", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Backtest Simulation", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. RESULTS DISPLAY ---
        val result = backtestResult
        if (result != null) {
            // --- 4A. PERFORMANCE SUMMARY TABLE ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("performance_summary_table_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Performance Summary Table",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (result.netPnl >= 0) BullishGreen.copy(alpha = 0.18f) else BearishRed.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, if (result.netPnl >= 0) BullishGreen else BearishRed)
                            ) {
                                Text(
                                    text = "${if (result.netPnl >= 0) "+₹" else "-₹"}${abs(result.netPnl.toInt())} (${if (result.totalReturnPercent >= 0) "+" else ""}${result.totalReturnPercent}%)",
                                    color = if (result.netPnl >= 0) BullishGreen else BearishRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Structured Table Rows
                        PerformanceTableRow(
                            metric = "Total Return",
                            value = "${if (result.netPnl >= 0) "+₹" else "-₹"}${abs(result.netPnl)} (${result.totalReturnPercent}%)",
                            benchmark = "Starting ₹${result.startingCapital.toInt()} -> Final ₹${result.finalEquity.toInt()}",
                            isHighlight = true,
                            valueColor = if (result.netPnl >= 0) BullishGreen else BearishRed
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Strategy Win Rate",
                            value = "${result.winRatePercent}%",
                            benchmark = "${result.winCount} Wins / ${result.lossCount} Losses (${result.totalTrades} Total)",
                            valueColor = if (result.winRatePercent >= 55.0) BullishGreen else TextPrimary
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Profit Factor",
                            value = "${result.profitFactor}",
                            benchmark = "Gross Gains ₹${result.grossProfit.toInt()} / Losses ₹${result.grossLoss.toInt()}",
                            valueColor = if (result.profitFactor >= 1.5) ElectricTeal else AmberWarning
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Maximum Drawdown",
                            value = "-${result.maxDrawdownPercent}%",
                            benchmark = "-₹${result.maxDrawdownAmount.toInt()} Peak-to-Trough Decline",
                            valueColor = if (result.maxDrawdownPercent <= 5.0) BullishGreen else BearishRed
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Payoff Ratio (Avg W/L)",
                            value = "1:${result.payoffRatio}",
                            benchmark = "Avg Win ₹${result.avgWinPnl.toInt()} vs Avg Loss ₹${result.avgLossPnl.toInt()}",
                            valueColor = CyanBlue
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Sharpe Ratio (Annualized)",
                            value = "${result.sharpeRatio}",
                            benchmark = if (result.sharpeRatio >= 1.5) "High Risk-Adjusted Edge" else "Moderate Volatility",
                            valueColor = if (result.sharpeRatio >= 1.5) GoldAccent else TextSecondary
                        )
                        HorizontalDivider(color = BorderDark, thickness = 0.6.dp)

                        PerformanceTableRow(
                            metric = "Average Holding Period",
                            value = "${result.avgHoldingDays} Days",
                            benchmark = "Max Allowed: ${result.config.maxHoldingDays} Days",
                            valueColor = TextPrimary
                        )
                    }
                }
            }

            // --- 4B. AI COUNCIL QUALIFICATION VERDICT ---
            item {
                val isQualified = result.qualificationStatus.contains("QUALIFIED", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.2.dp, if (isQualified) BullishGreen else AmberWarning)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (isQualified) Icons.Default.Verified else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (isQualified) BullishGreen else AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Risk Engine Qualification Verdict",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isQualified) BullishGreen.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = result.qualificationStatus,
                                color = if (isQualified) BullishGreen else AmberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = result.aiCouncilCritique,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // --- 4C. SIMULATED TRADE HISTORY TABLE TOGGLE ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulated_trade_history_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTradeHistoryTable = !showTradeHistoryTable },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Simulated Execution Log (${result.trades.size} Trades)",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (showTradeHistoryTable) "Hide" else "Inspect Trades",
                                color = CyanBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        AnimatedVisibility(visible = showTradeHistoryTable) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (result.trades.isEmpty()) {
                                    Text("No simulated trades triggered in this horizon.", color = TextMuted, fontSize = 11.sp)
                                } else {
                                    result.trades.forEach { trade ->
                                        SimulatedTradeRow(trade = trade)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTableRow(
    metric: String,
    value: String,
    benchmark: String,
    valueColor: Color = TextPrimary,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric,
                color = if (isHighlight) TextPrimary else TextSecondary,
                fontSize = if (isHighlight) 13.sp else 12.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = benchmark,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SimulatedTradeRow(trade: BacktestTrade) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "#${trade.tradeNumber} ${trade.symbol}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trade.entryDate} -> ${trade.exitDate}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${trade.quantity} shares @ ₹${trade.entryPrice} -> Exit ₹${trade.exitPrice} (${trade.exitReason})",
                    color = TextSecondary,
                    fontSize = 10.5.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (trade.pnl >= 0) "+₹" else "-₹"}${abs(trade.pnl.toInt())}",
                    color = if (trade.pnl >= 0) BullishGreen else BearishRed,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${if (trade.pnlPercent >= 0) "+" else ""}${trade.pnlPercent}%",
                    color = if (trade.pnlPercent >= 0) BullishGreen else BearishRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
