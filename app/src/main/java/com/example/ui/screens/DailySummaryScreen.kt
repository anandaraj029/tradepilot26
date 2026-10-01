package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.db.TradeJournalEntity
import com.example.ui.components.CsvExportPreviewDialog
import com.example.ui.components.FintechCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState
import com.example.util.CsvExportManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Daily Summary Screen that generates a comprehensive performance report showing
 * realized vs unrealized gains, strategy win rate, and total daily progress against the target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    uiState: TradePilotUiState,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val portfolio = uiState.portfolio
    val positions = uiState.positions
    val journalEntries = uiState.journalEntries

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val currentDateStr = remember { dateFormat.format(Date()) }

    var reportCopiedNotice by remember { mutableStateOf(false) }
    var isExportCsvDialogOpen by remember { mutableStateOf(false) }
    var exportedCsvContent by remember { mutableStateOf("") }
    var exportedCsvFilename by remember { mutableStateOf("") }

    // 1. REALIZED GAINS (Today)
    val realizedToday = portfolio.realizedPnlToday
    val realizedPercent = if (portfolio.startingCapital > 0) {
        round((realizedToday / portfolio.startingCapital * 100.0) * 100.0) / 100.0
    } else 0.0

    // 2. UNREALIZED GAINS (Active Open Positions)
    val unrealizedToday = remember(positions, portfolio.usdInrRate) {
        positions.sumOf { pos ->
            val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
            val pnl = (pos.currentPrice - pos.buyPrice) * pos.quantity * mult
            pnl
        }
    }
    val unrealizedPercent = if (portfolio.startingCapital > 0) {
        round((unrealizedToday / portfolio.startingCapital * 100.0) * 100.0) / 100.0
    } else 0.0

    // 3. COMBINED DAILY P&L
    val totalDailyPnl = round((realizedToday + unrealizedToday) * 100.0) / 100.0
    val totalDailyPercent = round((realizedPercent + unrealizedPercent) * 100.0) / 100.0

    // 4. DAILY TARGET PROGRESS (₹1,000 Target)
    val dailyTarget = max(1.0, portfolio.dailyTarget)
    val targetProgressRaw = (realizedToday / dailyTarget).toFloat().coerceIn(0f, 1f)
    val targetProgress by animateFloatAsState(targetValue = targetProgressRaw, label = "targetProgress")

    val combinedProgressRaw = (max(0.0, totalDailyPnl) / dailyTarget).toFloat().coerceIn(0f, 1f)
    val isTargetAchieved = realizedToday >= dailyTarget

    // 5. STRATEGY WIN RATE CALCULATIONS
    val totalClosedTrades = journalEntries.size
    val winTrades = journalEntries.filter { it.pnl > 0.0 }
    val lossTrades = journalEntries.filter { it.pnl < 0.0 }
    val winCount = winTrades.size
    val lossCount = lossTrades.size
    val winRate = if (totalClosedTrades > 0) {
        round((winCount.toDouble() / totalClosedTrades * 100.0) * 10.0) / 10.0
    } else 0.0

    val grossProfit = winTrades.sumOf { it.pnl }
    val grossLoss = abs(lossTrades.sumOf { it.pnl })
    val profitFactor = if (grossLoss > 0.0) {
        round((grossProfit / grossLoss) * 100.0) / 100.0
    } else if (grossProfit > 0.0) 9.99 else 0.0

    // Strategy Performance breakdown
    val strategyMap = remember(journalEntries) {
        journalEntries.groupBy { it.strategy }.mapValues { (_, trades) ->
            val w = trades.count { it.pnl > 0 }
            val total = trades.size
            val rPnl = round(trades.sumOf { it.pnl } * 100.0) / 100.0
            val wr = if (total > 0) round((w.toDouble() / total * 100.0) * 10.0) / 10.0 else 0.0
            Triple(total, wr, rPnl)
        }
    }

    // AI Adherence
    val followedTrades = journalEntries.filter { !it.userOverride }
    val overriddenTrades = journalEntries.filter { it.userOverride }
    val adherencePercent = if (totalClosedTrades > 0) {
        round((followedTrades.size.toDouble() / totalClosedTrades * 100.0) * 10.0) / 10.0
    } else 100.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("daily_summary_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // --- TOP NAVIGATION HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("daily_summary_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Daily Performance Report",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            exportedCsvFilename = CsvExportManager.createTimestampedFilename("daily_performance_report")
                            exportedCsvContent = CsvExportManager.generateDailyPerformanceCsv(
                                portfolio = portfolio,
                                positions = positions,
                                trades = journalEntries,
                                dateStr = currentDateStr
                            )
                            isExportCsvDialogOpen = true
                        },
                        modifier = Modifier.testTag("quick_export_csv_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = ElectricTeal
                        )
                    }

                    IconButton(
                        onClick = {
                            val reportText = buildSummaryReportText(
                                date = currentDateStr,
                                realized = realizedToday,
                                unrealized = unrealizedToday,
                                totalPnl = totalDailyPnl,
                                target = dailyTarget,
                                winRate = winRate,
                                totalTrades = totalClosedTrades,
                                profitFactor = profitFactor
                            )
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Daily Performance Report", reportText))
                            reportCopiedNotice = true
                        },
                        modifier = Modifier.testTag("copy_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Text Summary",
                            tint = TextMuted
                        )
                    }
                }
            }

            if (reportCopiedNotice) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BullishGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Executive Daily Report copied to clipboard!",
                        color = BullishGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- 1. EXECUTIVE REPORT BANNER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.5.dp, if (isTargetAchieved) GoldAccent else ElectricTeal.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isTargetAchieved) GoldAccent.copy(alpha = 0.12f) else ElectricTeal.copy(alpha = 0.1f),
                                    SurfaceDark
                                )
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
                                    text = "SESSION SUMMARY",
                                    color = TextMuted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = currentDateStr,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTargetAchieved) GoldAccent.copy(alpha = 0.2f) else BullishGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (isTargetAchieved) GoldAccent else BullishGreen)
                            ) {
                                Text(
                                    text = if (isTargetAchieved) "TARGET ACHIEVED 🏆" else "EXECUTION: ACTIVE 🛡️",
                                    color = if (isTargetAchieved) GoldAccent else BullishGreen,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Combined Net Daily P&L
                        Text(
                            text = "Net Daily P&L (Realized + Unrealized)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${if (totalDailyPnl >= 0) "+₹" else "-₹"}${abs(totalDailyPnl)}",
                                color = if (totalDailyPnl >= 0) BullishGreen else BearishRed,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "(${if (totalDailyPercent >= 0) "+" else ""}$totalDailyPercent%)",
                                color = if (totalDailyPercent >= 0) BullishGreen else BearishRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. REALIZED VS UNREALIZED GAINS COMPARISON ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("realized_unrealized_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Realized vs. Unrealized Breakdown",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Realized Gains Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, if (realizedToday >= 0) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(14.dp))
                                    Text("Realized P&L", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${if (realizedToday >= 0) "+₹" else "-₹"}${abs(realizedToday.toInt())}",
                                    color = if (realizedToday >= 0) BullishGreen else BearishRed,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${if (realizedPercent >= 0) "+" else ""}$realizedPercent% on capital",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "From $totalClosedTrades closed trades",
                                    color = TextMuted,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        // Unrealized Gains Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, if (unrealizedToday >= 0) CyanBlue.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(14.dp))
                                    Text("Unrealized P&L", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${if (unrealizedToday >= 0) "+₹" else "-₹"}${abs(unrealizedToday.toInt())}",
                                    color = if (unrealizedToday >= 0) CyanBlue else BearishRed,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${if (unrealizedPercent >= 0) "+" else ""}$unrealizedPercent% on capital",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Across ${positions.size} open positions",
                                    color = TextMuted,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. DAILY TARGET PROGRESS GAUGE (₹1,000 TARGET) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_progress_report_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.TrackChanges, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                            Text("Total Daily Progress Against Target", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Target: ₹${dailyTarget.toInt()}",
                            color = ElectricTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Realized Progress", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = "₹${realizedToday.toInt()} / ₹${dailyTarget.toInt()}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${(targetProgress * 100).toInt()}% Achieved",
                            color = if (isTargetAchieved) GoldAccent else ElectricTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { targetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (isTargetAchieved) GoldAccent else ElectricTeal,
                        trackColor = BackgroundDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Profit Lock Rule Callout
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (portfolio.profitLockEnabled) BullishGreen.copy(alpha = 0.12f) else SurfaceElevated,
                        border = BorderStroke(1.dp, if (portfolio.profitLockEnabled) BullishGreen.copy(alpha = 0.3f) else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isTargetAchieved) Icons.Default.Lock else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isTargetAchieved) GoldAccent else BullishGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isTargetAchieved) {
                                    "Profit Lock Engaged: Daily target met. Order routing locked to protect ₹${realizedToday.toInt()} gains."
                                } else {
                                    "Deterministic Profit Lock: Once daily target (₹${dailyTarget.toInt()}) is achieved, Risk Engine preserves profit."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. STRATEGY WIN RATE & QUANTITATIVE PERFORMANCE ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("strategy_win_rate_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Strategy Win Rate & Metrics",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryMetricPill(label = "Overall Win Rate", value = "$winRate%", color = BullishGreen)
                        SummaryMetricPill(label = "Profit Factor", value = "$profitFactor", color = ElectricTeal)
                        SummaryMetricPill(label = "Trades (W / L)", value = "$winCount / $lossCount", color = TextPrimary)
                        SummaryMetricPill(label = "AI Adherence", value = "$adherencePercent%", color = GoldAccent)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Performance by Strategy",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (strategyMap.isEmpty()) {
                        Text("No closed trades logged for strategy breakdown.", color = TextMuted, fontSize = 11.sp)
                    } else {
                        strategyMap.forEach { (stratName, stats) ->
                            val (count, sWinRate, sPnl) = stats
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = stratName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "$count trades executed", color = TextMuted, fontSize = 10.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(text = "$sWinRate% WR", color = CyanBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${if (sPnl >= 0) "+₹" else "-₹"}${abs(sPnl.toInt())}",
                                            color = if (sPnl >= 0) BullishGreen else BearishRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. BEHAVIORAL AUDIT & DISCIPLINE SCORE ---
        item {
            FintechCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Behavioral Audit & Council Adherence", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Council Aligned", color = TextMuted, fontSize = 11.sp)
                        Text("${followedTrades.size} trades", color = BullishGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val followedWr = if (followedTrades.isNotEmpty()) {
                            round((followedTrades.count { it.pnl > 0 }.toDouble() / followedTrades.size * 100.0) * 10.0) / 10.0
                        } else 0.0
                        Text("$followedWr% Win Rate", color = TextSecondary, fontSize = 10.5.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("User Overrides", color = TextMuted, fontSize = 11.sp)
                        Text("${overriddenTrades.size} trades", color = if (overriddenTrades.isNotEmpty()) AmberWarning else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val overrideWr = if (overriddenTrades.isNotEmpty()) {
                            round((overriddenTrades.count { it.pnl > 0 }.toDouble() / overriddenTrades.size * 100.0) * 10.0) / 10.0
                        } else 0.0
                        Text("$overrideWr% Win Rate", color = TextSecondary, fontSize = 10.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (overriddenTrades.isEmpty()) {
                            "✅ 100% Adherence to AI Council and Risk Engine boundaries. No emotional tilt or unvalidated entries detected."
                        } else {
                            "⚠️ Manual overrides exhibited lower win rate than Council recommendations. Adhere strictly to council voting."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // --- 6. EXPORT PERFORMANCE REPORT AS CSV ---
        item {
            FintechCard(
                borderColor = ElectricTeal.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Export Report as CSV", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("Detailed report with positions & strategies", color = TextMuted, fontSize = 10.5.sp)
                        }
                    }

                    Button(
                        onClick = {
                            exportedCsvFilename = CsvExportManager.createTimestampedFilename("daily_performance_report")
                            exportedCsvContent = CsvExportManager.generateDailyPerformanceCsv(
                                portfolio = portfolio,
                                positions = positions,
                                trades = journalEntries,
                                dateStr = currentDateStr
                            )
                            isExportCsvDialogOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("export_daily_summary_csv_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", color = BackgroundDark, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // CSV Export Inspection & Share Dialog
    if (isExportCsvDialogOpen) {
        CsvExportPreviewDialog(
            title = "Daily Performance Report CSV",
            filename = exportedCsvFilename,
            csvContent = exportedCsvContent,
            recordCount = journalEntries.size + positions.size + 16,
            onDismiss = { isExportCsvDialogOpen = false }
        )
    }
}

@Composable
private fun SummaryMetricPill(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.5.sp)
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

private fun buildSummaryReportText(
    date: String,
    realized: Double,
    unrealized: Double,
    totalPnl: Double,
    target: Double,
    winRate: Double,
    totalTrades: Int,
    profitFactor: Double
): String {
    return """
        ========================================
        AI TRADEPILOT • DAILY PERFORMANCE REPORT
        Date: $date
        ========================================
        • Realized P&L: ${if (realized >= 0) "+₹" else "-₹"}${abs(realized)}
        • Unrealized P&L: ${if (unrealized >= 0) "+₹" else "-₹"}${abs(unrealized)}
        • Net Daily P&L: ${if (totalPnl >= 0) "+₹" else "-₹"}${abs(totalPnl)}
        • Daily Target Goal: ₹${target.toInt()}
        • Target Progress: ${((realized / target) * 100).toInt()}%
        • Strategy Win Rate: $winRate% ($totalTrades closed trades)
        • Profit Factor: $profitFactor
        ========================================
        Generated by AI TradePilot Capital Engine
    """.trimIndent()
}
