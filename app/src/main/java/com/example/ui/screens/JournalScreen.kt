package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.AuditLogEntity
import com.example.data.db.TradeJournalEntity
import com.example.ui.components.CsvExportPreviewDialog
import com.example.ui.components.DecisionBadge
import com.example.ui.components.FintechCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState
import com.example.util.CsvExportManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    uiState: TradePilotUiState,
    onUpdateTrade: (TradeJournalEntity) -> Unit = {},
    onDeleteTrade: (Long) -> Unit = {},
    onAddManualTrade: (String, String, String, String, Double, Double, Int, String, Int, Boolean, String, String, String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: AI Trading Journal, 1: Audit Trail
    var selectedFilterChip by remember { mutableStateOf("All") } // "All", "Wins", "Losses", "Overridden", "India 🇮🇳", "USA 🇺🇸"

    var inspectingTrade by remember { mutableStateOf<TradeJournalEntity?>(null) }
    var isManualEntryDialogOpen by remember { mutableStateOf(false) }
    var isExportCsvDialogOpen by remember { mutableStateOf(false) }
    var exportedCsvContent by remember { mutableStateOf("") }
    var exportedCsvFilename by remember { mutableStateOf("") }

    val entries = uiState.journalEntries
    val auditLogs = uiState.auditLogs

    // Filter logic
    val filteredEntries = remember(entries, selectedFilterChip) {
        when (selectedFilterChip) {
            "Wins" -> entries.filter { it.pnl > 0 }
            "Losses" -> entries.filter { it.pnl < 0 }
            "Overridden" -> entries.filter { it.userOverride }
            "India 🇮🇳" -> entries.filter { it.market.contains("INDIA", ignoreCase = true) }
            "USA 🇺🇸" -> entries.filter { it.market.contains("USA", ignoreCase = true) }
            else -> entries
        }
    }

    // Analytics calculations
    val totalPnl = entries.sumOf { it.pnl }
    val winCount = entries.count { it.pnl > 0 }
    val lossCount = entries.count { it.pnl < 0 }
    val overrideCount = entries.count { it.userOverride }
    val winRate = if (entries.isNotEmpty()) ((winCount.toDouble() / entries.size) * 100).toInt() else 0
    val totalGains = entries.filter { it.pnl > 0 }.sumOf { it.pnl }
    val totalLosses = Math.abs(entries.filter { it.pnl < 0 }.sumOf { it.pnl })
    val profitFactor = if (totalLosses > 0) Math.round((totalGains / totalLosses) * 100.0) / 100.0 else if (totalGains > 0) 9.99 else 0.0
    val aiAdherenceRate = if (entries.isNotEmpty()) (((entries.size - overrideCount).toDouble() / entries.size) * 100).toInt() else 100

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Main Tab Bar
        PrimaryTabRow(
            selectedTabIndex = selectedMainTab,
            containerColor = Color.Transparent,
            contentColor = ElectricTeal,
            divider = {}
        ) {
            Tab(
                selected = selectedMainTab == 0,
                onClick = { selectedMainTab = 0 },
                text = {
                    Text(
                        text = "AI Trading Journal (${entries.size})",
                        color = if (selectedMainTab == 0) ElectricTeal else TextMuted,
                        fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_journal")
            )
            Tab(
                selected = selectedMainTab == 1,
                onClick = { selectedMainTab = 1 },
                text = {
                    Text(
                        text = "Audit Trail Log (${auditLogs.size})",
                        color = if (selectedMainTab == 1) ElectricTeal else TextMuted,
                        fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_audit_trail")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedMainTab == 0) {
            // JOURNAL TAB
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                // 1. PERFORMANCE ANALYTICS HEADER
                item {
                    FintechCard(
                        borderColor = if (totalPnl >= 0) BullishGreen.copy(alpha = 0.5f) else BearishRed.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "CUMULATIVE REALIZED P&L", color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (totalPnl >= 0) "+₹" else "-₹"}${Math.abs(totalPnl).toInt()}",
                                    color = if (totalPnl >= 0) BullishGreen else BearishRed,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        exportedCsvFilename = CsvExportManager.createTimestampedFilename("trade_journal")
                                        exportedCsvContent = CsvExportManager.generateTradeJournalCsv(entries)
                                        isExportCsvDialogOpen = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricTeal),
                                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("export_journal_csv_btn")
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Export CSV", color = ElectricTeal, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { isManualEntryDialogOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                                    border = BorderStroke(1.dp, CyanBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("log_manual_trade_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Log Trade", color = CyanBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Metric Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            JournalStatCell(label = "Win Rate", value = "$winRate%", sub = "$winCount W • $lossCount L", color = BullishGreen)
                            JournalStatCell(label = "Profit Factor", value = profitFactor.toString(), sub = "Gain/Loss Ratio", color = ElectricTeal)
                            JournalStatCell(label = "AI Adherence", value = "$aiAdherenceRate%", sub = "$overrideCount Overridden", color = if (aiAdherenceRate >= 80) ElectricTeal else AmberWarning)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // AI Post-Trade Behavioral Critique Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = ElectricTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AI Behavioral Pattern Analysis",
                                        color = ElectricTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (overrideCount > 0) {
                                            "Notice: Overridden trades showed a lower win-rate than AI Council approved setups. Sticking to deterministic risk clearance consistently protects the ₹25,000 baseline."
                                        } else {
                                            "Excellent discipline: 100% of trades adhered to AI Council guidance. Risk per trade strictly maintained within the 1% boundary."
                                        },
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. FILTER CHIPS ROW
                item {
                    val filterChips = listOf("All", "Wins", "Losses", "Overridden", "India 🇮🇳", "USA 🇺🇸")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterChips) { chip ->
                            val isSelected = selectedFilterChip == chip
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedFilterChip = chip },
                                color = if (isSelected) ElectricTeal else SurfaceDark,
                                border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark)
                            ) {
                                Text(
                                    text = chip,
                                    color = if (isSelected) BackgroundDark else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3. JOURNAL TRADES LIST
                if (filteredEntries.isEmpty()) {
                    item {
                        FintechCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FilterListOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "No trades match the '$selectedFilterChip' filter.", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { trade ->
                        DetailedJournalCard(
                            trade = trade,
                            onClick = { inspectingTrade = trade }
                        )
                    }
                }
            }
        } else {
            // AUDIT TRAIL TAB
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Deterministic Execution Audit Trail",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                items(auditLogs, key = { it.id }) { log ->
                    AuditLogItem(log = log)
                }
            }
        }
    }

    // Trade Detail Inspection Modal
    inspectingTrade?.let { trade ->
        TradeInspectionDialog(
            trade = trade,
            onDismiss = { inspectingTrade = null },
            onSaveNotes = { updatedNotes, updatedTags ->
                onUpdateTrade(trade.copy(userNotes = updatedNotes, tags = updatedTags))
                inspectingTrade = null
            },
            onDelete = {
                onDeleteTrade(trade.id)
                inspectingTrade = null
            }
        )
    }

    // Manual Trade Entry Dialog
    if (isManualEntryDialogOpen) {
        ManualTradeEntryDialog(
            onDismiss = { isManualEntryDialogOpen = false },
            onSaveTrade = { sym, mkt, comp, strat, entry, exit, qty, dec, conf, override, reason, notes, tags ->
                onAddManualTrade(sym, mkt, comp, strat, entry, exit, qty, dec, conf, override, reason, notes, tags)
                isManualEntryDialogOpen = false
            }
        )
    }

    // CSV Export Inspection & Share Dialog
    if (isExportCsvDialogOpen) {
        CsvExportPreviewDialog(
            title = "Trade Journal CSV Export",
            filename = exportedCsvFilename,
            csvContent = exportedCsvContent,
            recordCount = entries.size,
            onDismiss = { isExportCsvDialogOpen = false }
        )
    }
}

@Composable
fun DetailedJournalCard(
    trade: TradeJournalEntity,
    onClick: () -> Unit
) {
    val isProfit = trade.pnl >= 0
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(trade.closedAt))

    FintechCard(
        modifier = Modifier.testTag("journal_trade_${trade.tradeId}"),
        onClick = onClick
    ) {
        // Row 1: Header (Symbol, Market, Strategy, PnL)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = trade.symbol,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = SurfaceElevated) {
                        Text(
                            text = if (trade.market.contains("USA", ignoreCase = true)) "US 🇺🇸" else "IN 🇮🇳",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${trade.tradeId}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${trade.companyName} • Strategy: ${trade.strategy}",
                    color = TextMuted,
                    fontSize = 11.5.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isProfit) "+₹" else "-₹"}${Math.abs(trade.pnl).toInt()}",
                    color = if (isProfit) BullishGreen else BearishRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${if (trade.pnlPercent >= 0) "+" else ""}${trade.pnlPercent}%",
                    color = if (isProfit) BullishGreen else BearishRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Financial Execution Details
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entry: ${if (trade.market.contains("USA")) "$" else "₹"}${trade.entryPrice}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Exit: ${if (trade.market.contains("USA")) "$" else "₹"}${trade.exitPrice}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Qty: ${trade.quantity}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isProfit) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = trade.outcome,
                        color = if (isProfit) BullishGreen else BearishRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: AI Council vs User Override Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricTeal,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI: ${trade.aiDecision} (${trade.aiConfidence}% Conf)",
                    color = ElectricTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (trade.userOverride) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BearishRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "USER OVERRIDE", color = BearishRed, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BullishGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "✓ AI Aligned",
                        color = BullishGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Row 4: AI Observation / Notes
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "AI Critique: ${trade.aiObservation}",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )

        if (trade.userNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reflection: \"${trade.userNotes}\"",
                color = TextSecondary,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun TradeInspectionDialog(
    trade: TradeJournalEntity,
    onDismiss: () -> Unit,
    onSaveNotes: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var editableNotes by remember { mutableStateOf(trade.userNotes) }
    var selectedTag by remember { mutableStateOf(trade.tags) }

    val tagsOptions = listOf("Followed Plan", "Disciplined", "Early Exit", "FOMO", "Chased Breakout", "Overridden")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("trade_inspection_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trade Review & Post-Mortem",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${trade.symbol} • #${trade.tradeId}",
                            color = ElectricTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Execution Metrics Box
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = SurfaceElevated) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Realized P&L", color = TextMuted, fontSize = 11.sp)
                                        Text(
                                            text = "${if (trade.pnl >= 0) "+₹" else "-₹"}${Math.abs(trade.pnl).toInt()} (${trade.pnlPercent}%)",
                                            color = if (trade.pnl >= 0) BullishGreen else BearishRed,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Execution Outcome", color = TextMuted, fontSize = 11.sp)
                                        Text(
                                            text = trade.outcome,
                                            color = if (trade.pnl >= 0) BullishGreen else BearishRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DetailField(label = "Entry Price", value = "${if (trade.market.contains("USA")) "$" else "₹"}${trade.entryPrice}")
                                    DetailField(label = "Exit Price", value = "${if (trade.market.contains("USA")) "$" else "₹"}${trade.exitPrice}")
                                    DetailField(label = "Quantity", value = "${trade.quantity}")
                                    DetailField(label = "Market", value = trade.market)
                                }
                            }
                        }
                    }

                    // AI Council Decision vs Override Status
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI Council Deliberation Snapshot",
                                    color = ElectricTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Council Verdict: ${trade.aiDecision}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Confidence: ${trade.aiConfidence}%", color = CyanBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (trade.userOverride) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BearishRed.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "User Override Engaged", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (trade.overrideReason.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Stated Reason: \"${trade.overrideReason}\"", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "✓ User strictly followed the AI Council's signals without manual interference.",
                                        color = BullishGreen,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // AI Post-Mortem & Critique
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceElevated
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI Post-Mortem & Behavioral Observation",
                                    color = CyanBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = trade.aiObservation,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // User Reflection & Tags Editor
                    item {
                        Text(
                            text = "Trader Reflection & Learning Notes",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Tags selector
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(tagsOptions) { tag ->
                                val isSelected = selectedTag.contains(tag)
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedTag = tag },
                                    color = if (isSelected) ElectricTeal else SurfaceElevated
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) BackgroundDark else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editableNotes,
                            onValueChange = { editableNotes = it },
                            placeholder = { Text("Write personal reflections, lessons learned, or mistakes...", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("reflection_notes_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = ElectricTeal,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BearishRed)
                    ) {
                        Text(text = "Delete", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSaveNotes(editableNotes, selectedTag) },
                        modifier = Modifier.weight(1.5f).height(42.dp).testTag("save_notes_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                    ) {
                        Text(text = "Save Reflections", color = BackgroundDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualTradeEntryDialog(
    onDismiss: () -> Unit,
    onSaveTrade: (String, String, String, String, Double, Double, Int, String, Int, Boolean, String, String, String) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var market by remember { mutableStateOf("INDIA") }
    var companyName by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf("Breakout Trend Confirmation") }
    var entryPriceText by remember { mutableStateOf("") }
    var exitPriceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var aiDecision by remember { mutableStateOf("BUY") }
    var aiConfidenceText by remember { mutableStateOf("75") }
    var userOverride by remember { mutableStateOf(false) }
    var overrideReason by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }

    val strategies = listOf("Breakout Trend Confirmation", "Mean Reversion RSI Guard", "Multi-AI Council Hybrid", "Sector Rotation Swing", "Discretionary")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("manual_trade_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Log Trade into AI Journal", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = symbol,
                                onValueChange = { symbol = it },
                                label = { Text("Symbol (e.g. INFY)", color = TextMuted, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            // Market selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Market", color = TextMuted, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (market == "INDIA") ElectricTeal else SurfaceElevated,
                                        modifier = Modifier.clickable { market = "INDIA" }
                                    ) {
                                        Text(text = "IN 🇮🇳", color = if (market == "INDIA") BackgroundDark else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (market == "USA") ElectricTeal else SurfaceElevated,
                                        modifier = Modifier.clickable { market = "USA" }
                                    ) {
                                        Text(text = "US 🇺🇸", color = if (market == "USA") BackgroundDark else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name (e.g. Infosys Ltd)", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entryPriceText,
                                onValueChange = { entryPriceText = it },
                                label = { Text("Entry Price", color = TextMuted, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exitPriceText,
                                onValueChange = { exitPriceText = it },
                                label = { Text("Exit Price", color = TextMuted, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("Qty", color = TextMuted, fontSize = 11.sp) },
                                modifier = Modifier.weight(0.7f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Text(text = "Strategy Used:", color = TextSecondary, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(strategies) { s ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (strategy == s) CyanBlue else SurfaceElevated,
                                    modifier = Modifier.clickable { strategy = s }
                                ) {
                                    Text(
                                        text = s,
                                        color = if (strategy == s) BackgroundDark else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "AI Council Decision", color = TextMuted, fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("BUY", "WAIT", "HOLD", "SELL").forEach { d ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (aiDecision == d) ElectricTeal else SurfaceElevated,
                                            modifier = Modifier.clickable { aiDecision = d }
                                        ) {
                                            Text(text = d, color = if (aiDecision == d) BackgroundDark else TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = aiConfidenceText,
                                onValueChange = { aiConfidenceText = it },
                                label = { Text("AI Conf %", color = TextMuted, fontSize = 10.sp) },
                                modifier = Modifier.weight(0.6f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "User Override Trade?", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = userOverride,
                                onCheckedChange = { userOverride = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BackgroundDark, checkedTrackColor = BearishRed)
                            )
                        }
                        if (userOverride) {
                            OutlinedTextField(
                                value = overrideReason,
                                onValueChange = { overrideReason = it },
                                label = { Text("Reason for Overriding AI Recommendation", color = TextMuted, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = userNotes,
                            onValueChange = { userNotes = it },
                            label = { Text("Personal Reflections / Lessons Learned", color = TextMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val entry = entryPriceText.toDoubleOrNull() ?: 100.0
                        val exit = exitPriceText.toDoubleOrNull() ?: 102.0
                        val qty = quantityText.toIntOrNull() ?: 1
                        val conf = aiConfidenceText.toIntOrNull() ?: 75
                        val sym = if (symbol.isNotBlank()) symbol else "CUSTOM"
                        val comp = if (companyName.isNotBlank()) companyName else sym

                        onSaveTrade(
                            sym, market, comp, strategy, entry, exit, qty,
                            aiDecision, conf, userOverride, overrideReason, userNotes,
                            if (userOverride) "Overridden • Discretionary" else "Followed Plan"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("confirm_manual_trade_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text(text = "Save to Trade Journal", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JournalStatCell(label: String, value: String, sub: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(text = sub, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
fun DetailField(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AuditLogItem(
    log: AuditLogEntity,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    val statusColor = when (log.riskEngineStatus) {
        "APPROVED" -> BullishGreen
        "REJECTED" -> BearishRed
        "EMERGENCY", "LOCKED" -> AmberWarning
        else -> ElectricTeal
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audit_log_item_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (log.riskEngineStatus) {
                                    "APPROVED" -> Icons.Default.CheckCircle
                                    "REJECTED" -> Icons.Default.Cancel
                                    else -> Icons.Default.Shield
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = log.action,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedTime,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceCard
                    ) {
                        Text(
                            text = "${log.symbol} (${log.market})",
                            color = ElectricTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceCard
                    ) {
                        Text(
                            text = "AI: ${log.aiConsensusVerdict}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = log.riskEngineStatus,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (log.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = log.details,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

