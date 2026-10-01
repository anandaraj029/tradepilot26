package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.db.TradeJournalEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * Utility for generating RFC 4180 compliant CSV exports for the AI TradePilot journal
 * and daily performance reports, enabling seamless analysis in Microsoft Excel,
 * Google Sheets, LibreOffice, or Python pandas.
 */
object CsvExportManager {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Generates a comprehensive CSV string for all trade journal records.
     */
    fun generateTradeJournalCsv(trades: List<TradeJournalEntity>): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens with proper encoding automatically
        sb.append("\uFEFF")

        // CSV Header
        val headers = listOf(
            "Trade ID",
            "Date Closed",
            "Date Entered",
            "Symbol",
            "Company Name",
            "Market",
            "Strategy",
            "Order Type",
            "Execution Type",
            "Quantity",
            "Entry Price",
            "Exit Price",
            "Stop Loss",
            "Take Profit",
            "Realized PnL (INR)",
            "PnL %",
            "Outcome",
            "AI Decision",
            "AI Confidence %",
            "Risk Score",
            "Discretionary Override",
            "Override Reason",
            "Tags",
            "AI Observation",
            "User Notes"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        // Rows
        for (trade in trades) {
            val dateClosed = if (trade.closedAt > 0) dateTimeFormat.format(Date(trade.closedAt)) else ""
            val dateEntered = if (trade.entryTime > 0) dateTimeFormat.format(Date(trade.entryTime)) else ""

            val row = listOf(
                trade.tradeId,
                dateClosed,
                dateEntered,
                trade.symbol,
                trade.companyName,
                trade.market,
                trade.strategy,
                trade.orderType,
                trade.executionType,
                trade.quantity.toString(),
                "%.2f".format(Locale.US, trade.entryPrice),
                "%.2f".format(Locale.US, trade.exitPrice),
                "%.2f".format(Locale.US, trade.stopLoss),
                "%.2f".format(Locale.US, trade.takeProfit),
                "%.2f".format(Locale.US, trade.pnl),
                "%.2f".format(Locale.US, trade.pnlPercent),
                trade.outcome,
                trade.aiDecision,
                trade.aiConfidence.toString(),
                trade.riskScore.toString(),
                if (trade.userOverride) "YES" else "NO",
                trade.overrideReason,
                trade.tags,
                trade.aiObservation,
                trade.userNotes
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Generates a multi-section performance report CSV with summary metrics,
     * active open positions, realized closed trades, and strategy breakdown.
     */
    fun generateDailyPerformanceCsv(
        portfolio: PortfolioEntity,
        positions: List<PositionEntity>,
        trades: List<TradeJournalEntity>,
        dateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): String {
        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM

        // SECTION 1: EXECUTIVE DAILY PERFORMANCE SUMMARY
        sb.append(escapeCsv("=== AI TRADEPILOT DAILY PERFORMANCE REPORT ===")).append("\r\n")
        sb.append(escapeCsv("Report Date")).append(",").append(escapeCsv(dateStr)).append("\r\n")
        sb.append(escapeCsv("Generated At")).append(",").append(escapeCsv(dateTimeFormat.format(Date()))).append("\r\n")
        sb.append("\r\n")

        sb.append(listOf("Metric", "Value", "Currency/Unit", "Notes").joinToString(",") { escapeCsv(it) }).append("\r\n")

        val realizedToday = portfolio.realizedPnlToday
        val realizedPct = if (portfolio.startingCapital > 0) (realizedToday / portfolio.startingCapital * 100.0) else 0.0

        val unrealizedToday = positions.sumOf { pos ->
            val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
            (pos.currentPrice - pos.buyPrice) * pos.quantity * mult
        }
        val unrealizedPct = if (portfolio.startingCapital > 0) (unrealizedToday / portfolio.startingCapital * 100.0) else 0.0

        val netDailyPnl = realizedToday + unrealizedToday
        val netDailyPct = realizedPct + unrealizedPct
        val dailyTarget = max(1.0, portfolio.dailyTarget)
        val targetProgressPct = (realizedToday / dailyTarget) * 100.0
        val isTargetAchieved = realizedToday >= dailyTarget

        val winCount = trades.count { it.pnl > 0 }
        val lossCount = trades.count { it.pnl < 0 }
        val winRate = if (trades.isNotEmpty()) (winCount.toDouble() / trades.size * 100.0) else 0.0
        val totalGains = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
        val totalLosses = abs(trades.filter { it.pnl < 0 }.sumOf { it.pnl })
        val profitFactor = if (totalLosses > 0) totalGains / totalLosses else if (totalGains > 0) 9.99 else 0.0

        val overrideCount = trades.count { it.userOverride }
        val adherenceRate = if (trades.isNotEmpty()) ((trades.size - overrideCount).toDouble() / trades.size * 100.0) else 100.0

        val summaryMetrics = listOf(
            listOf("Starting Virtual Capital", "%.2f".format(Locale.US, portfolio.startingCapital), "INR", "Baseline account allocation"),
            listOf("Available Cash Balance", "%.2f".format(Locale.US, portfolio.availableCash), "INR", "Unallocated liquid capital"),
            listOf("Realized P&L Today", "%.2f".format(Locale.US, realizedToday), "INR", if (realizedToday >= 0) "Profit" else "Drawdown"),
            listOf("Realized Return %", "%.2f%%".format(Locale.US, realizedPct), "%", "Return on starting capital"),
            listOf("Unrealized P&L (Open Positions)", "%.2f".format(Locale.US, unrealizedToday), "INR", "Mark-to-market open risk"),
            listOf("Unrealized Return %", "%.2f%%".format(Locale.US, unrealizedPct), "%", "Floating equity delta"),
            listOf("Combined Net Daily P&L", "%.2f".format(Locale.US, netDailyPnl), "INR", "Total daily value delta"),
            listOf("Combined Daily Return %", "%.2f%%".format(Locale.US, netDailyPct), "%", "Aggregate daily return"),
            listOf("Daily Profit Target", "%.2f".format(Locale.US, dailyTarget), "INR", "User-defined daily milestone"),
            listOf("Daily Target Achievement", "%.1f%%".format(Locale.US, targetProgressPct), "%", if (isTargetAchieved) "TARGET ACHIEVED" else "In Progress"),
            listOf("Profit Lock Armed", if (portfolio.profitLockEnabled) "ENABLED" else "DISABLED", "State", "Protects gains once goal is met"),
            listOf("Daily Loss Limit", "-%.2f".format(Locale.US, portfolio.maxDailyLoss), "INR", "Hard stop risk barrier"),
            listOf("Total Closed Trades Logged", trades.size.toString(), "Count", "$winCount Wins / $lossCount Losses"),
            listOf("Strategy Win Rate", "%.1f%%".format(Locale.US, winRate), "%", "Closed trades accuracy"),
            listOf("Profit Factor", "%.2f".format(Locale.US, profitFactor), "Ratio", "Gross Profit / Gross Loss"),
            listOf("AI Council Adherence", "%.1f%%".format(Locale.US, adherenceRate), "%", "$overrideCount manual overrides")
        )

        for (m in summaryMetrics) {
            sb.append(m.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        sb.append("\r\n")

        // SECTION 2: ACTIVE OPEN POSITIONS
        sb.append(escapeCsv("=== ACTIVE OPEN POSITIONS (UNREALIZED) ===")).append("\r\n")
        val positionHeaders = listOf(
            "Symbol",
            "Company Name",
            "Market",
            "Quantity",
            "Buy Price",
            "Current Price",
            "Invested Value (INR)",
            "Unrealized P&L (INR)",
            "Return %",
            "Stop Loss",
            "Take Profit",
            "Current Risk Level"
        )
        sb.append(positionHeaders.joinToString(",") { escapeCsv(it) }).append("\r\n")

        for (pos in positions) {
            val mult = if (pos.market == "USA") portfolio.usdInrRate else 1.0
            val pnl = (pos.currentPrice - pos.buyPrice) * pos.quantity * mult
            val pnlPct = if (pos.buyPrice > 0) ((pos.currentPrice - pos.buyPrice) / pos.buyPrice * 100.0) else 0.0
            val invested = pos.buyPrice * pos.quantity * mult

            val riskDesc = if (pos.stopLoss > 0 && pos.currentPrice <= pos.stopLoss * 1.01) "HIGH (Near SL)" else if (pnl < 0) "ELEVATED" else "NORMAL"

            val row = listOf(
                pos.symbol,
                pos.companyName,
                pos.market,
                pos.quantity.toString(),
                "%.2f".format(Locale.US, pos.buyPrice),
                "%.2f".format(Locale.US, pos.currentPrice),
                "%.2f".format(Locale.US, invested),
                "%.2f".format(Locale.US, pnl),
                "%.2f%%".format(Locale.US, pnlPct),
                "%.2f".format(Locale.US, pos.stopLoss),
                "%.2f".format(Locale.US, pos.takeProfit),
                riskDesc
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        sb.append("\r\n")

        // SECTION 3: PERFORMANCE BY STRATEGY
        sb.append(escapeCsv("=== STRATEGY BREAKDOWN ===")).append("\r\n")
        val stratHeaders = listOf("Strategy", "Total Trades", "Wins", "Losses", "Win Rate %", "Total Realized P&L (INR)")
        sb.append(stratHeaders.joinToString(",") { escapeCsv(it) }).append("\r\n")

        val grouped = trades.groupBy { it.strategy }
        for ((strat, list) in grouped) {
            val sWins = list.count { it.pnl > 0 }
            val sLosses = list.count { it.pnl < 0 }
            val sWr = if (list.isNotEmpty()) (sWins.toDouble() / list.size * 100.0) else 0.0
            val sPnl = list.sumOf { it.pnl }

            val row = listOf(
                strat,
                list.size.toString(),
                sWins.toString(),
                sLosses.toString(),
                "%.1f%%".format(Locale.US, sWr),
                "%.2f".format(Locale.US, sPnl)
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV string into the app's cache directory under "exports".
     */
    fun writeCsvToCache(context: Context, filename: String, content: String): File {
        val exportsDir = File(context.cacheDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        val file = File(exportsDir, filename)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Gets shareable Content URI using Android FileProvider.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Initiates standard Android system share intent with the exported CSV attached.
     */
    fun shareCsvFile(
        context: Context,
        filename: String,
        content: String,
        chooserTitle: String = "Export CSV to Sheets / Excel"
    ) {
        try {
            val file = writeCsvToCache(context, filename, content)
            val uri = getShareableUri(context, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, filename.removeSuffix(".csv"))
                putExtra(Intent.EXTRA_TEXT, "Exported from AI TradePilot terminal.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback: Copy to clipboard if sharing intent fails
            copyCsvToClipboard(context, filename, content)
        }
    }

    /**
     * Copies CSV text directly to the system clipboard.
     */
    fun copyCsvToClipboard(context: Context, label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, content)
        clipboard.setPrimaryClip(clip)
    }

    /**
     * Generates a safe, dated filename for the export.
     */
    fun createTimestampedFilename(prefix: String): String {
        val ts = fileTimestampFormat.format(Date())
        return "${prefix}_${ts}.csv"
    }

    /**
     * Escapes a single CSV value following RFC 4180 rules.
     */
    fun escapeCsv(value: String): String {
        var str = value
        if (str.contains("\"")) {
            str = str.replace("\"", "\"\"")
        }
        if (str.contains(",") || str.contains("\n") || str.contains("\r") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }
}
