package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.db.TradeEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * DailyTradeStatsSummaryCard aggregates daily trade statistics:
 * 1. Total Number of Trades (Closed, Won, Lost, Breakeven, Open)
 * 2. Win/Loss Ratio & Win Rate Percentage with proportional visual indicator
 * 3. Net P&L Movement (Realized P&L, Unrealized P&L, Net Daily Flow)
 * 4. Key Performance Insights (Profit Factor, Avg Win, Avg Loss, Execution Type breakdown)
 */
@Composable
fun DailyTradeStatsSummaryCard(
    portfolio: PortfolioEntity,
    trades: List<TradeEntity>,
    openPositions: List<PositionEntity> = emptyList(),
    onViewJournal: () -> Unit = {},
    onViewDailyReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Calculate start of day (midnight) in local time
    val startOfDayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Filter trades executed/closed today
    val todayClosedTrades = remember(trades, startOfDayMillis) {
        trades.filter { trade ->
            trade.status.equals("CLOSED", ignoreCase = true) &&
                (trade.closedAt >= startOfDayMillis || trade.entryTime >= startOfDayMillis)
        }
    }

    val totalClosedCount = todayClosedTrades.size
    val winningTrades = todayClosedTrades.filter { it.pnl > 0.0 }
    val losingTrades = todayClosedTrades.filter { it.pnl < 0.0 }
    val breakevenTrades = todayClosedTrades.filter { it.pnl == 0.0 }

    val winCount = winningTrades.size
    val lossCount = losingTrades.size
    val breakevenCount = breakevenTrades.size
    val totalTradeCount = totalClosedCount + openPositions.size

    // Win Rate & Ratio
    val winRatePercent = if (totalClosedCount > 0) {
        round((winCount.toDouble() / totalClosedCount * 100.0) * 10.0) / 10.0
    } else if (portfolio.realizedPnlToday > 0) {
        100.0
    } else {
        0.0
    }

    val winLossRatioText = when {
        lossCount == 0 && winCount > 0 -> "${winCount}:0 (100% Win)"
        lossCount == 0 && winCount == 0 -> "0:0 (No Trades)"
        else -> {
            val ratio = round((winCount.toDouble() / lossCount.toDouble()) * 10.0) / 10.0
            "${winCount}W / ${lossCount}L (${ratio}:1)"
        }
    }

    // P&L Calculations
    val realizedPnlToday = if (todayClosedTrades.isNotEmpty()) {
        todayClosedTrades.sumOf { it.pnl }
    } else {
        portfolio.realizedPnlToday
    }

    val unrealizedPnlToday = openPositions.sumOf { pos ->
        val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
        (pos.currentPrice - pos.buyPrice) * pos.quantity * mult
    }

    val netMovementInr = realizedPnlToday + unrealizedPnlToday
    val isNetProfit = netMovementInr >= 0

    // Gross profits and losses for Profit Factor
    val grossProfit = winningTrades.sumOf { it.pnl }
    val grossLoss = abs(losingTrades.sumOf { it.pnl })
    val profitFactor = if (grossLoss > 0.0) {
        round((grossProfit / grossLoss) * 100.0) / 100.0
    } else if (grossProfit > 0.0) {
        99.99 // Infinity indicator
    } else {
        0.0
    }

    val avgWin = if (winCount > 0) grossProfit / winCount else 0.0
    val avgLoss = if (lossCount > 0) grossLoss / lossCount else 0.0
    val bestTrade = winningTrades.maxByOrNull { it.pnl }

    // Color animations
    val netPnlColor by animateColorAsState(
        targetValue = when {
            netMovementInr > 0 -> BullishGreen
            netMovementInr < 0 -> BearishRed
            else -> TextPrimary
        },
        animationSpec = tween(400),
        label = "netPnlColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_trade_stats_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    BorderDark,
                    if (isNetProfit) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceElevated.copy(alpha = 0.5f), SurfaceDark)
                    )
                )
                .padding(16.dp)
        ) {
            // Header Row: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricTeal.copy(alpha = 0.2f), CyanBlue.copy(alpha = 0.2f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Daily Trade Stats",
                            tint = ElectricTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Trade Statistics",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isNetProfit) BullishGreen else BearishRed)
                            )
                            Text(
                                text = "Aggregated Today • ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Expand / Collapse toggle button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp).testTag("daily_stats_expand_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Three Main Aggregate Pillars: Trades | Win/Loss Ratio | Net P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Total Trades Box
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("daily_stats_total_trades"),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = CyanBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "TRADES",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalTradeCount",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${totalClosedCount} Closed • ${openPositions.size} Open",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // 2. Win/Loss Ratio Box
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("daily_stats_win_loss_ratio"),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = if (winRatePercent >= 50) BullishGreen else GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "WIN / LOSS",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$winRatePercent%",
                                color = if (winRatePercent >= 50) BullishGreen else BearishRed,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = winLossRatioText,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // 3. Net P&L Movement Box
                Surface(
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("daily_stats_net_pnl"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isNetProfit) BullishGreen.copy(alpha = 0.12f) else BearishRed.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isNetProfit) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isNetProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = netPnlColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "NET MOVEMENT",
                                color = if (isNetProfit) BullishGreen else BearishRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${if (netMovementInr >= 0) "+₹" else "-₹"}${"%,d".format(abs(netMovementInr).toInt())}",
                            color = netPnlColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Realized: ${if (realizedPnlToday >= 0) "+₹" else "-₹"}${abs(realizedPnlToday).toInt()}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proportional Win/Loss/Breakeven Visual Bar
            if (totalClosedCount > 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Outcome Distribution",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${winCount} Won • ${lossCount} Lost${if (breakevenCount > 0) " • ${breakevenCount} BE" else ""}",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceElevated)
                    ) {
                        if (winCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(winCount.toFloat())
                                    .fillMaxHeight()
                                    .background(BullishGreen)
                            )
                        }
                        if (breakevenCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(breakevenCount.toFloat())
                                    .fillMaxHeight()
                                    .background(GoldAccent)
                            )
                        }
                        if (lossCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(lossCount.toFloat())
                                    .fillMaxHeight()
                                    .background(BearishRed)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, BorderDark.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Daily statistics update automatically in real-time on every trade completion.",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // Expandable Granular Analytics Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(color = BorderDark, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PERFORMANCE METRICS BREAKDOWN",
                        color = TextMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metric Row 1: Profit Factor & Unrealized
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatBox(
                            label = "Profit Factor",
                            value = if (profitFactor >= 99.0) "∞ (Max)" else "$profitFactor",
                            accentColor = if (profitFactor >= 1.5) BullishGreen else if (profitFactor >= 1.0) GoldAccent else BearishRed,
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatBox(
                            label = "Unrealized P&L",
                            value = "${if (unrealizedPnlToday >= 0) "+₹" else "-₹"}${abs(unrealizedPnlToday).toInt()}",
                            accentColor = if (unrealizedPnlToday >= 0) BullishGreen else BearishRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metric Row 2: Average Win vs Average Loss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatBox(
                            label = "Avg Win / Trade",
                            value = "+₹${avgWin.toInt()}",
                            accentColor = BullishGreen,
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatBox(
                            label = "Avg Loss / Trade",
                            value = "-₹${avgLoss.toInt()}",
                            accentColor = BearishRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (bestTrade != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text("Best Trade Today", color = TextMuted, fontSize = 10.sp)
                                        Text(bestTrade.symbol, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "+₹${bestTrade.pnl.toInt()} (${bestTrade.pnlPercent}%)",
                                    color = BullishGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewJournal,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_view_journal_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricTeal),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trade Journal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onViewDailyReport,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_view_report_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Daily Report", color = BackgroundDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
