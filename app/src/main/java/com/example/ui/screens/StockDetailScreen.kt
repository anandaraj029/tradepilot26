package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import com.example.ui.components.AiExplainabilityCard
import com.example.ui.components.DecisionBadge
import com.example.ui.components.FintechCard
import com.example.ui.components.MetricProgressBar
import com.example.ui.components.Sparkline
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState

@Composable
fun StockDetailScreen(
    uiState: TradePilotUiState,
    onBackClick: () -> Unit,
    onRunAiCouncil: (StockQuote) -> Unit,
    onOpenPaperTrade: () -> Unit,
    onToggleWatchlist: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stock = uiState.selectedStock ?: return
    val deliberation = uiState.currentDeliberation
    val riskEval = uiState.currentRiskEvaluation
    val isPositive = stock.priceChangePercent >= 0
    val isWatchlisted = uiState.watchlist.any { it.symbol == stock.symbol }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. TOP NAVIGATION & HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, CircleShape)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceElevated
                    ) {
                        Text(
                            text = stock.market.displayName,
                            color = CyanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onToggleWatchlist(stock.symbol, stock.market.name, stock.companyName) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatchlisted) GoldAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. STOCK TITLE & LIVE PRICE
        item {
            FintechCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = stock.symbol,
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = stock.companyName,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sector: ${stock.sector}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stock.market.currencySymbol}${stock.currentPrice}",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isPositive) BullishGreen else BearishRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${stock.priceChangePercent}% (${if (isPositive) "+" else ""}${stock.priceChange})",
                                color = if (isPositive) BullishGreen else BearishRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (stock.market == MarketType.USA) {
                            Text(
                                text = "≈ ₹${(stock.currentPrice * uiState.portfolio.usdInrRate).toInt()}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Interactive Price Chart
                Sparkline(
                    data = stock.sparklineData,
                    isPositive = isPositive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chart Range Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "52W Low: ${stock.market.currencySymbol}${stock.low52}", color = TextMuted, fontSize = 10.sp)
                    Text(text = "Support: ${stock.market.currencySymbol}${stock.support}", color = CyanBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Resist: ${stock.market.currencySymbol}${stock.resistance}", color = AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "52W High: ${stock.market.currencySymbol}${stock.high52}", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        // 3. TECHNICAL & FUNDAMENTAL KEY METRICS
        item {
            FintechCard {
                Text(
                    text = "Key Technical & Valuation Metrics",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(label = "RSI (14)", value = stock.rsi.toString(), modifier = Modifier.weight(1f))
                    MetricCell(label = "MACD", value = stock.macd.toString(), modifier = Modifier.weight(1f))
                    MetricCell(label = "20 SMA", value = stock.sma20.toString(), modifier = Modifier.weight(1f))
                    MetricCell(label = "50 SMA", value = stock.sma50.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(label = "P/E Ratio", value = "${stock.peRatio}x", modifier = Modifier.weight(1f))
                    MetricCell(label = "200 SMA", value = stock.sma200.toString(), modifier = Modifier.weight(1f))
                    MetricCell(label = "Volume", value = stock.volume, modifier = Modifier.weight(1f))
                    MetricCell(label = "Currency", value = stock.currency, modifier = Modifier.weight(1f))
                }
            }
        }

        // 4. MULTI-AI COUNCIL DELIBERATION CARD
        item {
            FintechCard(
                borderColor = if (deliberation?.isNoTradeAdvised == true) BearishRed else ElectricTeal
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Council Consensus",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Gemini + Claude + ChatGPT synthesized",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    if (deliberation != null) {
                        DecisionBadge(decision = deliberation.consensusDecision)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (deliberation != null) {
                    // Voting Tally Pill Bar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "BUY: ${deliberation.buyVotes}", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "HOLD: ${deliberation.holdVotes}", color = CyanBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "WAIT: ${deliberation.waitVotes}", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "SELL: ${deliberation.sellVotes}", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Confidence: ${deliberation.overallConfidence}%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Scores
                    MetricProgressBar(label = "Technical Strength (Gemini)", value = deliberation.techScoreAvg, accentColor = ElectricTeal)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricProgressBar(label = "Fundamental Quality (Claude)", value = deliberation.fundScoreAvg, accentColor = CyanBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricProgressBar(label = "Risk Assessment (ChatGPT)", value = deliberation.riskScoreAvg, accentColor = if (deliberation.riskScoreAvg > 60) BearishRed else BullishGreen)

                    Spacer(modifier = Modifier.height(14.dp))

                    // AI EXPLAINABILITY SECTION
                    Text(
                        text = "AI EXPLAINABILITY",
                        color = ElectricTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExplainSection(label = "WHAT?", content = "${deliberation.consensusDecision.label} — ${deliberation.timeHorizon}")
                    ExplainSection(label = "WHY?", content = deliberation.synthesizedReason)

                    if (deliberation.keyCatalysts.isNotEmpty()) {
                        ExplainSection(label = "KEY CATALYSTS", content = deliberation.keyCatalysts.joinToString(" • "))
                    }
                    if (deliberation.riskFactors.isNotEmpty()) {
                        ExplainSection(label = "RISK FACTORS", content = deliberation.riskFactors.joinToString(" • "))
                    }
                    if (deliberation.invalidatingConditions.isNotEmpty()) {
                        ExplainSection(label = "WHAT INVALIDATES THIS?", content = deliberation.invalidatingConditions.joinToString(" • "))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricTeal, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // 4B. DEDICATED 4-PILLAR AI EXPLAINABILITY PROTOCOL
        if (deliberation != null) {
            item {
                AiExplainabilityCard(deliberation = deliberation)
            }
        }

        // 5. DETERMINISTIC RISK ENGINE VERDICT
        item {
            FintechCard(
                borderColor = if (riskEval?.isApproved == true) BullishGreen else AmberWarning
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (riskEval?.isApproved == true) BullishGreen else AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deterministic Risk Engine",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (riskEval?.isApproved == true) BullishGreen.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (riskEval?.isApproved == true) BullishGreen else AmberWarning)
                    ) {
                        Text(
                            text = if (riskEval?.isApproved == true) "PASSED" else "BLOCKED",
                            color = if (riskEval?.isApproved == true) BullishGreen else AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (riskEval?.isApproved == true) {
                    Text(
                        text = "✓ 1% Trade Risk Rule Satisfied: Max permissible allocation is ${riskEval.maxPermittedQuantity} shares (~₹${riskEval.estimatedRiskRupees.toInt()} max calculated risk).",
                        color = BullishGreen,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Suggested Stop Loss: ${stock.market.currencySymbol}${riskEval.suggestedStopLoss}  |  Target: ${stock.market.currencySymbol}${riskEval.suggestedTakeProfit}",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                } else {
                    Text(
                        text = "Notice: ${riskEval?.rejectionReason ?: "Evaluating risk limits..."}",
                        color = AmberWarning,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 6. ACTION BUTTONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onRunAiCouncil(stock) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rerun_council_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = CyanBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Re-Run AI", color = CyanBlue, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenPaperTrade,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("paper_trade_buy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = BackgroundDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Paper Trade (Buy)", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExplainSection(label: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = CyanBlue, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = content, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
