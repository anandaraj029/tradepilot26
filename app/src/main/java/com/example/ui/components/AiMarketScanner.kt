package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class ScannerSignal(val label: String, val color: Color) {
    ALL("All Signals", CyanBlue),
    BUY("BUY", BullishGreen),
    WATCH("WATCH", ElectricTeal),
    AVOID("AVOID", BearishRed)
}

data class ScannedAssetOpportunity(
    val stock: StockQuote,
    val signal: ScannerSignal,
    val compositeScore: Int,
    val technicalScore: Int,
    val fundamentalScore: Int,
    val sentimentScore: Int,
    val riskScore: Int,
    val suggestedEntry: Double,
    val suggestedStopLoss: Double,
    val suggestedTarget: Double,
    val riskRewardRatio: Double,
    val primaryReason: String,
    val triggerOrRiskCondition: String
)

/**
 * AI Market Scanner component that filters assets based on AI model scores
 * and highlights top opportunities based on 'WATCH', 'BUY', or 'AVOID' signals.
 */
@Composable
fun AiMarketScanner(
    stocks: List<StockQuote>,
    watchlistSymbols: Set<String>,
    onSelectStock: (StockQuote) -> Unit,
    onDeliberateInCouncil: (StockQuote) -> Unit,
    onOpenPaperTrade: (StockQuote) -> Unit,
    onToggleWatchlist: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSignalFilter by remember { mutableStateOf(ScannerSignal.ALL) }
    var selectedMarketFilter by remember { mutableStateOf<MarketType?>(null) } // null = All
    var minScoreThreshold by remember { mutableIntStateOf(50) }
    var sortBy by remember { mutableStateOf("Composite Score") } // "Composite Score", "Risk-to-Reward", "Price Change"
    var expandedSymbol by remember { mutableStateOf<String?>(null) }

    // Evaluate AI Model Scores and Signals deterministically for all assets
    val scannedOpportunities = remember(stocks) {
        stocks.map { stock ->
            evaluateAssetOpportunity(stock)
        }
    }

    // Filter and Sort
    val filteredOpportunities = remember(
        scannedOpportunities,
        selectedSignalFilter,
        selectedMarketFilter,
        minScoreThreshold,
        sortBy
    ) {
        scannedOpportunities
            .filter { opp ->
                val signalMatch = selectedSignalFilter == ScannerSignal.ALL || opp.signal == selectedSignalFilter
                val marketMatch = selectedMarketFilter == null || opp.stock.market == selectedMarketFilter
                val scoreMatch = opp.compositeScore >= minScoreThreshold
                signalMatch && marketMatch && scoreMatch
            }
            .sortedWith { a, b ->
                when (sortBy) {
                    "Risk-to-Reward" -> b.riskRewardRatio.compareTo(a.riskRewardRatio)
                    "Price Change" -> b.stock.priceChangePercent.compareTo(a.stock.priceChangePercent)
                    else -> b.compositeScore.compareTo(a.compositeScore)
                }
            }
    }

    // Signal Counts for tabs
    val buyCount = remember(scannedOpportunities) { scannedOpportunities.count { it.signal == ScannerSignal.BUY } }
    val watchCount = remember(scannedOpportunities) { scannedOpportunities.count { it.signal == ScannerSignal.WATCH } }
    val avoidCount = remember(scannedOpportunities) { scannedOpportunities.count { it.signal == ScannerSignal.AVOID } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_market_scanner_component"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. SCANNER HEADER & RADAR BANNER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(ElectricTeal.copy(alpha = 0.12f), SurfaceDark)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = ElectricTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "AI Market Scanner",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Multi-Model Technical, Fundamental & Risk Radar",
                                color = TextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = "${filteredOpportunities.size} Signals",
                            color = ElectricTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // --- 2. SIGNAL FILTER TABS (BUY, WATCH, AVOID) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScannerSignalTab(
                signal = ScannerSignal.ALL,
                count = scannedOpportunities.size,
                isSelected = selectedSignalFilter == ScannerSignal.ALL,
                onClick = { selectedSignalFilter = ScannerSignal.ALL },
                modifier = Modifier.weight(1f)
            )
            ScannerSignalTab(
                signal = ScannerSignal.BUY,
                count = buyCount,
                isSelected = selectedSignalFilter == ScannerSignal.BUY,
                onClick = { selectedSignalFilter = ScannerSignal.BUY },
                modifier = Modifier.weight(1f)
            )
            ScannerSignalTab(
                signal = ScannerSignal.WATCH,
                count = watchCount,
                isSelected = selectedSignalFilter == ScannerSignal.WATCH,
                onClick = { selectedSignalFilter = ScannerSignal.WATCH },
                modifier = Modifier.weight(1f)
            )
            ScannerSignalTab(
                signal = ScannerSignal.AVOID,
                count = avoidCount,
                isSelected = selectedSignalFilter == ScannerSignal.AVOID,
                onClick = { selectedSignalFilter = ScannerSignal.AVOID },
                modifier = Modifier.weight(1f)
            )
        }

        // --- 3. MARKET & SCORE FILTERS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Market Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair(null, "Global"),
                    Pair(MarketType.INDIA, "India 🇮🇳"),
                    Pair(MarketType.USA, "USA 🇺🇸")
                ).forEach { (mkt, label) ->
                    val isSel = selectedMarketFilter == mkt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) CyanBlue.copy(alpha = 0.2f) else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isSel) CyanBlue else BorderDark),
                        modifier = Modifier.clickable { selectedMarketFilter = mkt }
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) CyanBlue else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Min Score Pill Toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.clickable {
                    minScoreThreshold = when (minScoreThreshold) {
                        50 -> 65
                        65 -> 75
                        75 -> 80
                        else -> 50
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(12.dp))
                    Text(
                        text = "Score ≥ $minScoreThreshold",
                        color = ElectricTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 4. OPPORTUNITIES LIST ---
        if (filteredOpportunities.isEmpty()) {
            FintechCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No assets match current scanner filters.", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Try adjusting the minimum AI score or changing the signal filter.", color = TextMuted, fontSize = 11.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredOpportunities.forEach { opp ->
                    val isExpanded = expandedSymbol == opp.stock.symbol
                    val isWatchlisted = watchlistSymbols.contains(opp.stock.symbol)

                    ScannedOpportunityCard(
                        opportunity = opp,
                        isExpanded = isExpanded,
                        isWatchlisted = isWatchlisted,
                        onToggleExpand = {
                            expandedSymbol = if (isExpanded) null else opp.stock.symbol
                        },
                        onSelectStock = { onSelectStock(opp.stock) },
                        onDeliberateInCouncil = { onDeliberateInCouncil(opp.stock) },
                        onOpenPaperTrade = { onOpenPaperTrade(opp.stock) },
                        onToggleWatchlist = {
                            onToggleWatchlist(opp.stock.symbol, opp.stock.market.name, opp.stock.companyName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerSignalTab(
    signal: ScannerSignal,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("scanner_tab_${signal.name.lowercase()}"),
        color = if (isSelected) signal.color.copy(alpha = 0.18f) else SurfaceCard,
        border = BorderStroke(1.5.dp, if (isSelected) signal.color else BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = signal.label,
                color = if (isSelected) signal.color else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                color = if (isSelected) TextPrimary else TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ScannedOpportunityCard(
    opportunity: ScannedAssetOpportunity,
    isExpanded: Boolean,
    isWatchlisted: Boolean,
    onToggleExpand: () -> Unit,
    onSelectStock: () -> Unit,
    onDeliberateInCouncil: () -> Unit,
    onOpenPaperTrade: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    val stock = opportunity.stock
    val signal = opportunity.signal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("opportunity_card_${stock.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(
            1.2.dp,
            if (signal == ScannerSignal.BUY) BullishGreen.copy(alpha = 0.6f)
            else if (signal == ScannerSignal.AVOID) BearishRed.copy(alpha = 0.5f)
            else BorderDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Symbol, Name, Price & Signal Badge
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stock.symbol.take(2),
                            color = CyanBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stock.symbol,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (stock.market == MarketType.INDIA) "NSE" else "NASDAQ",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .background(SurfaceElevated, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = stock.companyName,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Price and Signal Badge
                Column(horizontalAlignment = Alignment.End) {
                    val curr = if (stock.currency == "USD") "$" else "₹"
                    Text(
                        text = "$curr${stock.currentPrice}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (stock.priceChangePercent >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (stock.priceChangePercent >= 0) BullishGreen else BearishRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${if (stock.priceChangePercent >= 0) "+" else ""}${stock.priceChangePercent}%",
                            color = if (stock.priceChangePercent >= 0) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- AI MODEL SCORES & SIGNAL BANNER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = signal.color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, signal.color.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (signal) {
                                ScannerSignal.BUY -> Icons.Default.CheckCircle
                                ScannerSignal.WATCH -> Icons.Default.Visibility
                                ScannerSignal.AVOID -> Icons.Default.Warning
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = signal.color,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = signal.label,
                            color = signal.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Mini Model Score Badges
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreBadge(label = "Tech", score = opportunity.technicalScore, color = ElectricTeal)
                    ScoreBadge(label = "Fund", score = opportunity.fundamentalScore, color = CyanBlue)
                    ScoreBadge(label = "Risk", score = opportunity.riskScore, color = if (opportunity.riskScore > 60) BearishRed else BullishGreen)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BackgroundDark
                    ) {
                        Text(
                            text = "AI: ${opportunity.compositeScore}",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Opportunity Reason
            Text(
                text = opportunity.primaryReason,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )

            // Expanded Setup & Execution Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = BorderDark, thickness = 0.8.dp)

                    // Trade Setup (Entry, SL, TP, R:R)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val curr = if (stock.currency == "USD") "$" else "₹"
                        TradeSetupPill(label = "Entry", value = "$curr${opportunity.suggestedEntry}", color = TextPrimary)
                        TradeSetupPill(label = "Stop Loss", value = "$curr${opportunity.suggestedStopLoss}", color = BearishRed)
                        TradeSetupPill(label = "Target", value = "$curr${opportunity.suggestedTarget}", color = BullishGreen)
                        TradeSetupPill(label = "R:R", value = "1:${opportunity.riskRewardRatio}", color = GoldAccent)
                    }

                    // Trigger / Risk Condition
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BackgroundDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (signal == ScannerSignal.AVOID) Icons.Default.Shield else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (signal == ScannerSignal.AVOID) BearishRed else ElectricTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = opportunity.triggerOrRiskCondition,
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeliberateInCouncil,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ElectricTeal),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Council", color = ElectricTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenPaperTrade,
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paper Trade", color = BackgroundDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatchlisted) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Watchlist",
                                tint = if (isWatchlisted) GoldAccent else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Expand / Collapse Chevron Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Show Less" else "Inspect Setup & AI Breakdown",
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

@Composable
private fun ScoreBadge(label: String, score: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = "$label:", color = TextMuted, fontSize = 10.sp)
        Text(text = "$score", color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TradeSetupPill(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(text = value, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Deterministic multi-factor AI scoring algorithm translating stock indicators into
 * Technical, Fundamental, Sentiment, Risk, and Composite scores + Signals.
 */
private fun evaluateAssetOpportunity(stock: StockQuote): ScannedAssetOpportunity {
    // 1. Technical Score (0-100)
    var tech = 50
    if (stock.rsi in 40.0..62.0) tech += 20 // Sweet spot
    else if (stock.rsi < 35.0) tech += 15 // Oversold bounce candidate
    else if (stock.rsi > 70.0) tech -= 15 // Overbought

    if (stock.macd > 0) tech += 15 else tech -= 10
    if (stock.currentPrice > stock.sma20) tech += 10
    if (stock.currentPrice > stock.sma50) tech += 5
    val technicalScore = tech.coerceIn(10, 95)

    // 2. Fundamental Score (0-100)
    var fund = 60
    if (stock.peRatio in 15.0..32.0) fund += 20
    else if (stock.peRatio > 50.0) fund -= 15
    if (stock.sector == "Index" || stock.sector == "Semiconductors & AI" || stock.sector == "Energy & Conglomerate") fund += 10
    val fundamentalScore = fund.coerceIn(15, 95)

    // 3. Sentiment Score (0-100)
    var sent = 55
    if (stock.priceChangePercent > 1.0) sent += 25
    else if (stock.priceChangePercent > 0.0) sent += 10
    else if (stock.priceChangePercent < -1.5) sent -= 20
    val sentimentScore = sent.coerceIn(10, 95)

    // 4. Risk Score (0-100, where lower is safer)
    var risk = 45
    if (stock.rsi > 68.0 || stock.rsi < 30.0) risk += 20
    if (stock.priceChangePercent < -2.0) risk += 15
    if (stock.sector == "Index") risk -= 15
    val riskScore = risk.coerceIn(15, 90)

    // 5. Composite AI Score (0-100)
    val composite = (technicalScore * 0.45 + fundamentalScore * 0.35 + sentimentScore * 0.20).toInt()

    // 6. Signal Determination
    val signal = when {
        composite >= 73 && technicalScore >= 68 && riskScore <= 55 -> ScannerSignal.BUY
        composite in 54..72 || (stock.rsi < 35.0 && stock.currentPrice >= stock.support * 0.98) -> ScannerSignal.WATCH
        else -> ScannerSignal.AVOID
    }

    // 7. Setup & R:R calculations
    val entry = stock.currentPrice
    val stopLoss = round((if (stock.support > 0 && stock.support < entry) stock.support * 0.985 else entry * 0.98) * 100.0) / 100.0
    val target = round((if (stock.resistance > entry) stock.resistance * 1.015 else entry * 1.04) * 100.0) / 100.0

    val riskAmt = max(0.01, entry - stopLoss)
    val rewardAmt = max(0.01, target - entry)
    val rr = round((rewardAmt / riskAmt) * 10.0) / 10.0

    // Reasoning
    val (reason, trigger) = when (signal) {
        ScannerSignal.BUY -> Pair(
            "Strong bullish alignment. RSI (${stock.rsi.toInt()}) in optimal momentum zone with MACD positive expansion.",
            "Trigger: Enter above ₹$entry. Invalidation: Daily close below ₹$stopLoss."
        )
        ScannerSignal.WATCH -> Pair(
            "Consolidating near key structural support (₹${stock.support.toInt()}). Volume accumulation pending.",
            "Watch for volume breakout above ₹${round(entry * 1.008 * 100.0) / 100.0} or bounce off support."
        )
        ScannerSignal.AVOID -> Pair(
            "Elevated risk asymmetry (Risk: $riskScore/100). Technical momentum showing exhaustion or resistance rejection.",
            "Avoid entry until clean base forms above 50-SMA. Prioritize capital protection."
        )
        else -> Pair("Neutral setup awaiting directional catalyst.", "Stand by.")
    }

    return ScannedAssetOpportunity(
        stock = stock,
        signal = signal,
        compositeScore = composite,
        technicalScore = technicalScore,
        fundamentalScore = fundamentalScore,
        sentimentScore = sentimentScore,
        riskScore = riskScore,
        suggestedEntry = entry,
        suggestedStopLoss = stopLoss,
        suggestedTarget = target,
        riskRewardRatio = rr,
        primaryReason = reason,
        triggerOrRiskCondition = trigger
    )
}
