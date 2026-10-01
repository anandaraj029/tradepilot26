package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
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
import com.example.data.db.TradeEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Recharts-inspired Data point for DailyPnLChart.
 */
data class DailyPnLPoint(
    val timestamp: Long,
    val timeLabel: String,
    val portfolioValue: Double,
    val targetThreshold: Double,
    val pnl: Double,
    val note: String = ""
)

/**
 * DailyPnLChart Component
 *
 * Implements a rich Recharts-style declarative line chart in Jetpack Compose:
 * - Portfolio value evolution curve throughout the current trading day
 * - Target threshold reference line (<ReferenceLine y={dailyTarget} />)
 * - Cartesian grid (<CartesianGrid strokeDasharray="3 3" />)
 * - Interactive cursor tooltip with touch scrubbing
 * - Gradient area fill under curve (<Area type="monotone" fill="url(#colorPnl)" />)
 * - Dynamic legend & timeframe selectors
 */
@Composable
fun DailyPnLChart(
    portfolio: PortfolioEntity,
    currentPortfolioValue: Double,
    trades: List<TradeEntity> = emptyList(),
    onTargetThresholdClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }

    val startingCapital = portfolio.startingCapital
    val dailyTarget = portfolio.dailyTarget
    val targetThreshold = startingCapital + dailyTarget
    val totalPnl = currentPortfolioValue - startingCapital
    val isProfit = totalPnl >= 0
    val isTargetAchieved = currentPortfolioValue >= targetThreshold
    val remainingToTarget = max(0.0, targetThreshold - currentPortfolioValue)

    // Generate chronological intraday points based on closed trades and initial baseline
    val dataPoints = remember(portfolio, currentPortfolioValue, trades, selectedTimeframe) {
        generateDailyPnLPoints(
            startingCapital = startingCapital,
            targetThreshold = targetThreshold,
            currentValue = currentPortfolioValue,
            trades = trades,
            timeframe = selectedTimeframe
        )
    }

    val activeIndex = hoveredIndex ?: (dataPoints.size - 1).coerceAtLeast(0)
    val activePoint = dataPoints.getOrNull(activeIndex) ?: DailyPnLPoint(
        timestamp = System.currentTimeMillis(),
        timeLabel = "Now",
        portfolioValue = currentPortfolioValue,
        targetThreshold = targetThreshold,
        pnl = totalPnl
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_pnl_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceElevated.copy(alpha = 0.45f), SurfaceDark)
                    )
                )
                .padding(16.dp)
        ) {
            // 1. Chart Header: Title, Live Target Tag, & Timeframe Toggle
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Daily P&L Chart",
                            tint = ElectricTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily P&L Evolution",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Portfolio trajectory vs. Daily Target",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Timeframe Selector Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("1D", "1W", "1M").forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ElectricTeal else Color.Transparent)
                                .clickable {
                                    selectedTimeframe = tf
                                    hoveredIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf,
                                color = if (isSelected) BackgroundDark else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Active Scrubbing / Hover Value Tooltip Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, if (activePoint.pnl >= 0) BullishGreen.copy(alpha = 0.3f) else BearishRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Time: ${activePoint.timeLabel}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (activePoint.note.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ElectricTeal.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = activePoint.note,
                                        color = ElectricTeal,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${"%,d".format(activePoint.portfolioValue.toInt())}",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net P&L",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${if (activePoint.pnl >= 0) "+₹" else "-₹"}${"%,d".format(abs(activePoint.pnl).toInt())}",
                            color = if (activePoint.pnl >= 0) BullishGreen else BearishRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Recharts-style Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("daily_pnl_chart_canvas")
            ) {
                RechartsDailyPnLCanvas(
                    points = dataPoints,
                    targetThreshold = targetThreshold,
                    startingCapital = startingCapital,
                    selectedIndex = activeIndex,
                    onPointSelected = { index -> hoveredIndex = index }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Recharts Legend & Reference Line Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legend Items
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Line 1: Portfolio Curve
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isProfit) BullishGreen else ElectricTeal)
                        )
                        Text(
                            text = "Portfolio Value",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Line 2: Target Reference Line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onTargetThresholdClick)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 2.dp)
                                .background(GoldAccent)
                        )
                        Text(
                            text = "Target (₹${"%,d".format(targetThreshold.toInt())})",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Achievement Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isTargetAchieved) GoldAccent.copy(alpha = 0.15f) else SurfaceElevated,
                    border = BorderStroke(1.dp, if (isTargetAchieved) GoldAccent.copy(alpha = 0.4f) else BorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (isTargetAchieved) GoldAccent else TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isTargetAchieved) "Target Reached 🎉" else "₹${"%,d".format(remainingToTarget.toInt())} to go",
                            color = if (isTargetAchieved) GoldAccent else TextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas renderer for the Daily P&L Chart conforming to Recharts semantics.
 */
@Composable
private fun RechartsDailyPnLCanvas(
    points: List<DailyPnLPoint>,
    targetThreshold: Double,
    startingCapital: Double,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "chartAnim"
    )

    val minVal = min(startingCapital - 400.0, points.minOf { it.portfolioValue } - 200.0)
    val maxVal = max(targetThreshold + 400.0, points.maxOf { it.portfolioValue } + 200.0)
    val valRange = max(100.0, maxVal - minVal)

    val strokeColor = if (points.lastOrNull()?.pnl ?: 0.0 >= 0.0) BullishGreen else ElectricTeal
    val targetColor = GoldAccent

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val index = findNearestPointIndex(offset.x, size.width.toFloat(), points.size)
                    onPointSelected(index)
                }
            }
            .pointerInput(points) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val index = findNearestPointIndex(change.position.x, size.width.toFloat(), points.size)
                    onPointSelected(index)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = 24.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val chartHeight = height - paddingBottom - paddingTop

        fun getY(value: Double): Float {
            val normalized = ((value - minVal) / valRange).toFloat().coerceIn(0f, 1f)
            return paddingTop + chartHeight * (1f - normalized)
        }

        fun getX(index: Int): Float {
            if (points.size <= 1) return width / 2f
            return (index.toFloat() / (points.size - 1)) * width
        }

        // 1. Cartesian Grid Horizontal Lines (Recharts <CartesianGrid strokeDasharray="3 3" />)
        val gridLines = 4
        for (i in 0..gridLines) {
            val gridY = paddingTop + (chartHeight / gridLines) * i
            drawLine(
                color = BorderDark.copy(alpha = 0.5f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 2. Daily Target Threshold Reference Line (<ReferenceLine y={targetThreshold} />)
        val targetY = getY(targetThreshold)
        drawLine(
            color = targetColor.copy(alpha = 0.85f),
            start = Offset(0f, targetY),
            end = Offset(width, targetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // 3. Baseline Capital Starting Line
        val baselineY = getY(startingCapital)
        drawLine(
            color = TextMuted.copy(alpha = 0.4f),
            start = Offset(0f, baselineY),
            end = Offset(width, baselineY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        // 4. Cubic Bezier Evolution Path & Area Gradient (<Area /> + <Line />)
        val linePath = Path()
        val areaPath = Path()

        val renderedPointCount = (points.size * animatedProgress).toInt().coerceAtLeast(1)

        for (i in 0 until renderedPointCount) {
            val x = getX(i)
            val y = getY(points[i].portfolioValue)

            if (i == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(x, height - paddingBottom)
                areaPath.lineTo(x, y)
            } else {
                val prevX = getX(i - 1)
                val prevY = getY(points[i - 1].portfolioValue)
                val controlX1 = prevX + (x - prevX) / 2f
                val controlX2 = prevX + (x - prevX) / 2f

                linePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                areaPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        if (renderedPointCount > 0) {
            val lastX = getX(renderedPointCount - 1)
            areaPath.lineTo(lastX, height - paddingBottom)
            areaPath.close()

            // Draw Area Gradient Fill
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        strokeColor.copy(alpha = 0.35f),
                        strokeColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = height - paddingBottom
                )
            )

            // Draw Line Stroke
            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // 5. Active Cursor Crosshair & Selected Data Point Dot
        if (selectedIndex in points.indices) {
            val selX = getX(selectedIndex)
            val selY = getY(points[selectedIndex].portfolioValue)

            // Vertical Cursor Line
            drawLine(
                color = TextSecondary.copy(alpha = 0.6f),
                start = Offset(selX, paddingTop),
                end = Offset(selX, height - paddingBottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // Outer Glowing Circle
            drawCircle(
                color = strokeColor.copy(alpha = 0.25f),
                radius = 8.dp.toPx(),
                center = Offset(selX, selY)
            )

            // Inner Solid Dot
            drawCircle(
                color = strokeColor,
                radius = 4.5.dp.toPx(),
                center = Offset(selX, selY)
            )

            // Center White Dot
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(selX, selY)
            )
        }
    }
}

/**
 * Utility function to locate the closest data point on touch scrub.
 */
private fun findNearestPointIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val ratio = (touchX / width).coerceIn(0f, 1f)
    return (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
}

/**
 * Generate simulated intraday trajectory points leading up to real-time current portfolio value.
 */
private fun generateDailyPnLPoints(
    startingCapital: Double,
    targetThreshold: Double,
    currentValue: Double,
    trades: List<TradeEntity>,
    timeframe: String
): List<DailyPnLPoint> {
    val points = mutableListOf<DailyPnLPoint>()
    val now = System.currentTimeMillis()

    when (timeframe) {
        "1D" -> {
            val marketOpen = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val times = listOf(
                "09:15" to 0.0,
                "10:00" to (currentValue - startingCapital) * 0.15,
                "11:00" to (currentValue - startingCapital) * 0.35,
                "12:30" to (currentValue - startingCapital) * 0.28,
                "14:00" to (currentValue - startingCapital) * 0.75,
                "15:00" to (currentValue - startingCapital) * 0.90,
                "Live" to (currentValue - startingCapital)
            )

            times.forEachIndexed { i, (label, pnlDelta) ->
                val valAtStep = startingCapital + pnlDelta
                points.add(
                    DailyPnLPoint(
                        timestamp = marketOpen + i * 3600000L,
                        timeLabel = label,
                        portfolioValue = valAtStep,
                        targetThreshold = targetThreshold,
                        pnl = pnlDelta,
                        note = if (i == 0) "Open" else if (i == times.size - 1) "Now" else "Tick $i"
                    )
                )
            }
        }
        "1W" -> {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Today")
            days.forEachIndexed { idx, day ->
                val factor = (idx + 1).toDouble() / days.size
                val pnl = (currentValue - startingCapital) * factor
                points.add(
                    DailyPnLPoint(
                        timestamp = now - (days.size - 1 - idx) * 86400000L,
                        timeLabel = day,
                        portfolioValue = startingCapital + pnl,
                        targetThreshold = targetThreshold,
                        pnl = pnl,
                        note = day
                    )
                )
            }
        }
        "1M" -> {
            val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Current")
            weeks.forEachIndexed { idx, wk ->
                val factor = (idx + 1).toDouble() / weeks.size
                val pnl = (currentValue - startingCapital) * factor
                points.add(
                    DailyPnLPoint(
                        timestamp = now - (weeks.size - 1 - idx) * 7 * 86400000L,
                        timeLabel = wk,
                        portfolioValue = startingCapital + pnl,
                        targetThreshold = targetThreshold,
                        pnl = pnl,
                        note = wk
                    )
                )
            }
        }
    }

    return points
}
