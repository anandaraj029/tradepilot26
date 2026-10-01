package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.db.TradeEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * DailyPnLTracker component that reads from the Room database, calculates the sum
 * of all realized trades for the current date, and compares it against the user-defined
 * daily target to display real-time progress.
 */
@Composable
fun DailyPnLTracker(
    portfolio: PortfolioEntity,
    trades: List<TradeEntity>,
    onTargetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Calculate beginning of today in milliseconds
    val startOfDayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Filter trades realized today directly from Room database records
    val todayRealizedTrades = remember(trades, startOfDayMillis) {
        trades.filter { trade ->
            trade.status.equals("CLOSED", ignoreCase = true) && trade.closedAt >= startOfDayMillis
        }
    }

    // Real-time sum of all realized trade P&L for current date
    val realizedPnlToday = remember(todayRealizedTrades, portfolio.realizedPnlToday) {
        val calculatedSum = todayRealizedTrades.sumOf { it.pnl }
        // Fall back to portfolio.realizedPnlToday if no individual trade rows are populated yet
        if (todayRealizedTrades.isNotEmpty()) calculatedSum else portfolio.realizedPnlToday
    }

    val dailyTarget = max(1.0, portfolio.dailyTarget)
    val isTargetAchieved = realizedPnlToday >= dailyTarget

    val progressRaw = (realizedPnlToday / dailyTarget).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressRaw,
        animationSpec = tween(durationMillis = 600),
        label = "dailyPnlProgress"
    )

    val progressPercent = round((realizedPnlToday / dailyTarget * 100.0) * 10.0) / 10.0
    val remainingToTarget = max(0.0, dailyTarget - realizedPnlToday)

    val winTradesCount = todayRealizedTrades.count { it.pnl > 0.0 }
    val lossTradesCount = todayRealizedTrades.count { it.pnl < 0.0 }
    val todayWinRate = if (todayRealizedTrades.isNotEmpty()) {
        round((winTradesCount.toDouble() / todayRealizedTrades.size * 100.0) * 10.0) / 10.0
    } else 100.0

    val barColor by animateColorAsState(
        targetValue = when {
            isTargetAchieved -> GoldAccent
            realizedPnlToday < 0 -> BearishRed
            realizedPnlToday > 0 -> ElectricTeal
            else -> TextMuted
        },
        label = "barColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_pnl_tracker_component"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            1.2.dp,
            if (isTargetAchieved) GoldAccent else ElectricTeal.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title + Room DB Live Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isTargetAchieved) GoldAccent.copy(alpha = 0.2f) else ElectricTeal.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isTargetAchieved) Icons.Default.EmojiEvents else Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = if (isTargetAchieved) GoldAccent else ElectricTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Daily P&L Tracker",
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BullishGreen))
                            Text(
                                text = "Room DB Real-time Sync",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Target Amount Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.clickable(onClick = onTargetClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Target:", color = TextMuted, fontSize = 10.5.sp)
                        Text(
                            text = "₹${dailyTarget.toInt()}",
                            color = if (isTargetAchieved) GoldAccent else ElectricTeal,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Numbers & Progress Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Realized Today (Room DB)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${if (realizedPnlToday >= 0) "+₹" else "-₹"}${abs(realizedPnlToday.toInt())}",
                            color = if (realizedPnlToday >= 0) BullishGreen else BearishRed,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Icon(
                            imageVector = if (realizedPnlToday >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (realizedPnlToday >= 0) BullishGreen else BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${progressPercent}%",
                        color = if (isTargetAchieved) GoldAccent else TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isTargetAchieved) "Target Reached! 🏆" else "₹${remainingToTarget.toInt()} remaining",
                        color = if (isTargetAchieved) GoldAccent else TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BackgroundDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(barColor.copy(alpha = 0.7f), barColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Callout / Profit Lock Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isTargetAchieved) GoldAccent.copy(alpha = 0.12f) else SurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isTargetAchieved) GoldAccent.copy(alpha = 0.4f) else BorderDark
                )
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
                            imageVector = if (isTargetAchieved) Icons.Default.Lock else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isTargetAchieved) GoldAccent else BullishGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isTargetAchieved) {
                                "Profit Lock: Daily target met. Gains protected."
                            } else if (portfolio.profitLockEnabled) {
                                "Profit Lock Armed: Freezes trading at ₹${dailyTarget.toInt()} target"
                            } else {
                                "Discretionary Mode: Profit Lock disabled"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = if (todayRealizedTrades.isNotEmpty()) "${todayRealizedTrades.size} trades" else "1 trade",
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Expandable Today's Realized Trades Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = BorderDark, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Today's Realized Records", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Win Rate: $todayWinRate%", color = CyanBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (todayRealizedTrades.isEmpty()) {
                        Text("No individual trade logs today yet. Initial baseline pnl logged.", color = TextMuted, fontSize = 10.5.sp)
                    } else {
                        todayRealizedTrades.forEach { trade ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BackgroundDark)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "${trade.symbol} (${trade.market})", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "${trade.strategy} • Qty ${trade.quantity}", color = TextMuted, fontSize = 9.5.sp)
                                }
                                Text(
                                    text = "${if (trade.pnl >= 0) "+₹" else "-₹"}${abs(trade.pnl.toInt())} (${trade.pnlPercent}%)",
                                    color = if (trade.pnl >= 0) BullishGreen else BearishRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Expand / Collapse Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Real-time Breakdown" else "Inspect Today's Realized Trades",
                    color = CyanBlue,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyanBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
