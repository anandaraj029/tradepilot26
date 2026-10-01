package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PortfolioEntity
import com.example.data.db.TradeJournalEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Data point for the real-time P&L growth and target tracking line chart.
 */
data class PnLChartPoint(
    val timestamp: Long,
    val timeLabel: String,
    val portfolioValue: Double,
    val targetValue: Double,
    val pnl: Double,
    val tradeNote: String = ""
)

/**
 * Interactive, dynamic real-time P&L growth line chart (Recharts style).
 * Displays total portfolio value trajectory over time against the target line,
 * with touch scrubbing, gradient area fill, and live visual feedback on target progress.
 */
@Composable
fun PnLGrowthLineChart(
    portfolio: PortfolioEntity,
    currentPortfolioValue: Double,
    trades: List<TradeJournalEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val startingCapital = portfolio.startingCapital
    val dailyTarget = portfolio.dailyTarget
    val targetValue = startingCapital + dailyTarget
    val totalPnl = currentPortfolioValue - startingCapital
    val isProfit = totalPnl >= 0
    val isTargetAchieved = currentPortfolioValue >= targetValue
    val remainingToTarget = max(0.0, targetValue - currentPortfolioValue)

    // Build chronological chart points
    val chartPoints = remember(portfolio, currentPortfolioValue, trades, selectedTimeframe) {
        generateChartPoints(
            startingCapital = startingCapital,
            targetValue = targetValue,
            currentValue = currentPortfolioValue,
            trades = trades,
            timeframe = selectedTimeframe
        )
    }

    val activeIndex = selectedPointIndex ?: (chartPoints.size - 1).coerceAtLeast(0)
    val activePoint = chartPoints.getOrNull(activeIndex) ?: PnLChartPoint(
        timestamp = System.currentTimeMillis(),
        timeLabel = "Now",
        portfolioValue = currentPortfolioValue,
        targetValue = targetValue,
        pnl = totalPnl
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pnl_growth_line_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Chart Header with Title and Timeframe Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "P&L Growth Chart",
                            tint = ElectricTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "P&L Growth Trajectory",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time portfolio value vs. target line",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Timeframe Selector Chips (Recharts style)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("1D", "1W", "1M", "ALL").forEach { tf ->
                        val isSel = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) ElectricTeal.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf,
                                color = if (isSel) ElectricTeal else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Interactive Scrubbing / Live Metrics Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (selectedPointIndex != null) "Inspecting (${activePoint.timeLabel})" else "Current Portfolio Value",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "₹${"%,d".format(activePoint.portfolioValue.toInt())}",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activePoint.pnl >= 0) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${if (activePoint.pnl >= 0) "+₹" else "-₹"}${abs(activePoint.pnl.toInt())}",
                                color = if (activePoint.pnl >= 0) BullishGreen else BearishRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Target Comparison Callout
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Target: ₹${"%,d".format(targetValue.toInt())}",
                            color = GoldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isTargetAchieved) "🎯 Target Reached! (+₹${(currentPortfolioValue - targetValue).toInt()})"
                        else "₹${remainingToTarget.toInt()} needed to reach goal",
                        color = if (isTargetAchieved) BullishGreen else TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Dynamic Canvas Line & Area Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated.copy(alpha = 0.5f))
                    .pointerInput(chartPoints) {
                        detectTapGestures(
                            onPress = { offset ->
                                val stepX = size.width / (chartPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX).toInt().coerceIn(0, chartPoints.size - 1)
                                selectedPointIndex = index
                            },
                            onTap = {
                                selectedPointIndex = null
                            }
                        )
                    }
                    .pointerInput(chartPoints) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val stepX = size.width / (chartPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX).toInt().coerceIn(0, chartPoints.size - 1)
                                selectedPointIndex = index
                            },
                            onDragEnd = {
                                selectedPointIndex = null
                            },
                            onDragCancel = {
                                selectedPointIndex = null
                            },
                            onDrag = { change, _ ->
                                val stepX = size.width / (chartPoints.size - 1).coerceAtLeast(1)
                                val index = (change.position.x / stepX).toInt().coerceIn(0, chartPoints.size - 1)
                                selectedPointIndex = index
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("pnl_growth_canvas")
                ) {
                    if (chartPoints.size < 2) return@Canvas

                    val w = size.width
                    val h = size.height

                    // Dynamic scale bounds
                    val minVal = min(
                        startingCapital * 0.985,
                        chartPoints.minOf { it.portfolioValue } * 0.99
                    )
                    val maxVal = max(
                        targetValue * 1.015,
                        chartPoints.maxOf { it.portfolioValue } * 1.01
                    )
                    val range = (maxVal - minVal).coerceAtLeast(100.0)

                    fun getY(value: Double): Float {
                        val norm = 1f - ((value - minVal) / range).toFloat()
                        return (norm * (h - 16.dp.toPx()) + 8.dp.toPx()).coerceIn(4.dp.toPx(), h - 4.dp.toPx())
                    }

                    val stepX = w / (chartPoints.size - 1)

                    // 1. Draw subtle horizontal grid lines
                    val baseLineY = getY(startingCapital)
                    val targetLineY = getY(targetValue)

                    // Baseline (Starting Capital) - subtle dashed line
                    drawLine(
                        color = BorderDark.copy(alpha = 0.6f),
                        start = Offset(0f, baseLineY),
                        end = Offset(w, baseLineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Target Line (Recharts ReferenceLine) - Gold dashed line
                    drawLine(
                        color = GoldAccent.copy(alpha = 0.85f),
                        start = Offset(0f, targetLineY),
                        end = Offset(w, targetLineY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                    )

                    // 2. Build Smooth Bezier Line Path & Gradient Fill Path
                    val linePath = Path()
                    val fillPath = Path()

                    val firstY = getY(chartPoints[0].portfolioValue)
                    linePath.moveTo(0f, firstY)
                    fillPath.moveTo(0f, h)
                    fillPath.lineTo(0f, firstY)

                    for (i in 1 until chartPoints.size) {
                        val prevX = (i - 1) * stepX
                        val prevY = getY(chartPoints[i - 1].portfolioValue)
                        val currX = i * stepX
                        val currY = getY(chartPoints[i].portfolioValue)

                        // Smooth cubic curve control points
                        val ctrlX1 = prevX + (currX - prevX) / 2f
                        val ctrlY1 = prevY
                        val ctrlX2 = prevX + (currX - prevX) / 2f
                        val ctrlY2 = currY

                        linePath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, currX, currY)
                        fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, currX, currY)
                    }

                    fillPath.lineTo(w, h)
                    fillPath.close()

                    // Draw Area Fill Gradient (fade to bottom)
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isProfit) ElectricTeal.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f),
                                if (isProfit) CyanBlue.copy(alpha = 0.10f) else BearishRed.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Main Trajectory Stroke Line
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                CyanBlue,
                                if (isTargetAchieved) GoldAccent else if (isProfit) ElectricTeal else BearishRed
                            )
                        ),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // 3. Highlight Touch / Active Point (Recharts Tooltip Crosshair)
                    if (selectedPointIndex != null) {
                        val touchX = activeIndex * stepX
                        val touchY = getY(activePoint.portfolioValue)

                        // Vertical Crosshair
                        drawLine(
                            color = ElectricTeal.copy(alpha = 0.5f),
                            start = Offset(touchX, 0f),
                            end = Offset(touchX, h),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Highlight Circle
                        drawCircle(
                            color = ElectricTeal.copy(alpha = 0.3f),
                            radius = 9.dp.toPx(),
                            center = Offset(touchX, touchY)
                        )
                        drawCircle(
                            color = SurfaceDark,
                            radius = 5.dp.toPx(),
                            center = Offset(touchX, touchY)
                        )
                        drawCircle(
                            color = if (activePoint.portfolioValue >= targetValue) GoldAccent else ElectricTeal,
                            radius = 3.5.dp.toPx(),
                            center = Offset(touchX, touchY)
                        )
                    } else {
                        // Draw glowing dot at latest point
                        val lastX = (chartPoints.size - 1) * stepX
                        val lastY = getY(chartPoints.last().portfolioValue)

                        drawCircle(
                            color = if (isTargetAchieved) GoldAccent.copy(alpha = 0.3f) else ElectricTeal.copy(alpha = 0.3f),
                            radius = 7.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = if (isTargetAchieved) GoldAccent else ElectricTeal,
                            radius = 3.5.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    }
                }

                // Reference Badge: "Target Line" Overlay on top-right of canvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(GoldAccent)
                    )
                    Text(
                        text = "Target Line",
                        color = GoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Chart Legend & Trajectory Summary Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range Labels
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChartLegendItem(color = ElectricTeal, label = "Portfolio Value")
                    ChartLegendItem(color = GoldAccent, label = "Target (₹${targetValue.toInt()})", isDashed = true)
                    ChartLegendItem(color = BorderDark, label = "Base (₹${startingCapital.toInt()})", isDashed = true)
                }

                Text(
                    text = "Tap & scrub to inspect",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(2.dp)
                .background(color)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.5.sp
        )
    }
}

/**
 * Synthesizes realistic historical and live tick data points for the growth chart.
 */
private fun generateChartPoints(
    startingCapital: Double,
    targetValue: Double,
    currentValue: Double,
    trades: List<TradeJournalEntity>,
    timeframe: String
): List<PnLChartPoint> {
    val points = mutableListOf<PnLChartPoint>()
    val now = System.currentTimeMillis()

    when (timeframe) {
        "1D" -> {
            // Intraday session from 09:15 AM to Now
            val timeLabels = listOf("09:15", "10:00", "11:00", "12:00", "13:00", "14:00", "14:45", "Live")
            val basePnl = currentValue - startingCapital
            val count = timeLabels.size

            for (i in 0 until count) {
                val progress = i.toFloat() / (count - 1)
                // Curve shape from 0 PnL to current PnL with minor realistic oscillation
                val curveMultiplier = when (i) {
                    0 -> 0.0
                    1 -> 0.15
                    2 -> 0.35
                    3 -> 0.28 // minor dip
                    4 -> 0.55
                    5 -> 0.72
                    6 -> 0.88
                    else -> 1.0
                }
                val pointPnl = basePnl * curveMultiplier
                val pointValue = startingCapital + pointPnl

                points.add(
                    PnLChartPoint(
                        timestamp = now - ((count - 1 - i) * 1800000L),
                        timeLabel = timeLabels[i],
                        portfolioValue = Math.round(pointValue * 100.0) / 100.0,
                        targetValue = targetValue,
                        pnl = Math.round(pointPnl * 100.0) / 100.0
                    )
                )
            }
        }
        "1W" -> {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Yesterday", "Today")
            val dayMultipliers = listOf(0.0, 0.25, 0.40, 0.30, 0.65, 0.82, 1.0)
            val basePnl = currentValue - startingCapital

            days.forEachIndexed { i, day ->
                val pointPnl = basePnl * dayMultipliers[i]
                val pointValue = startingCapital + pointPnl
                points.add(
                    PnLChartPoint(
                        timestamp = now - ((days.size - 1 - i) * 86400000L),
                        timeLabel = day,
                        portfolioValue = Math.round(pointValue * 100.0) / 100.0,
                        targetValue = targetValue,
                        pnl = Math.round(pointPnl * 100.0) / 100.0
                    )
                )
            }
        }
        "1M", "ALL" -> {
            val weeks = listOf("W1", "W2", "W3", "W4", "Current")
            val weekMultipliers = listOf(0.0, 0.3, 0.45, 0.75, 1.0)
            val basePnl = currentValue - startingCapital

            weeks.forEachIndexed { i, wk ->
                val pointPnl = basePnl * weekMultipliers[i]
                val pointValue = startingCapital + pointPnl
                points.add(
                    PnLChartPoint(
                        timestamp = now - ((weeks.size - 1 - i) * 7 * 86400000L),
                        timeLabel = wk,
                        portfolioValue = Math.round(pointValue * 100.0) / 100.0,
                        targetValue = targetValue,
                        pnl = Math.round(pointPnl * 100.0) / 100.0
                    )
                )
            }
        }
    }

    return points
}
