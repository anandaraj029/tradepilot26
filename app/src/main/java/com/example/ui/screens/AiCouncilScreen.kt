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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAgentResult
import com.example.data.model.DecisionType
import com.example.data.model.StockQuote
import com.example.ui.components.AiExplainabilityCard
import com.example.ui.components.DecisionBadge
import com.example.ui.components.FintechCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState

@Composable
fun AiCouncilScreen(
    uiState: TradePilotUiState,
    onSelectStock: (StockQuote) -> Unit,
    onOpenPaperTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedStock = uiState.selectedStock
    val deliberation = uiState.currentDeliberation
    val allStocks = uiState.indianStocks + uiState.usStocks

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. INTRO BANNER
        item {
            FintechCard(
                borderColor = ElectricTeal.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ElectricTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tri-Agent AI Council",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gemini + Claude + ChatGPT Consensus Architecture",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Instead of a single AI prompt, each asset is sent to three specialized agents. The consensus is passed through deterministic risk & policy engines before any action is permitted.",
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // 2. STOCK SELECTOR CHIPS
        item {
            Column {
                Text(
                    text = "Select Asset for Council Deliberation:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(allStocks) { stock ->
                        val isSelected = selectedStock?.symbol == stock.symbol
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectStock(stock) },
                            color = if (isSelected) ElectricTeal else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stock.symbol,
                                    color = if (isSelected) BackgroundDark else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (stock.market.name == "USA") "🇺🇸" else "🇮🇳",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CONSENSUS CARD
        if (deliberation != null && selectedStock != null) {
            item {
                FintechCard(
                    borderColor = if (deliberation.isNoTradeAdvised) BearishRed else BullishGreen
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CONSENSUS VERDICT",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedStock.symbol,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        DecisionBadge(decision = deliberation.consensusDecision)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = deliberation.synthesizedReason,
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vote pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VoteBox(label = "BUY", count = deliberation.buyVotes, color = BullishGreen)
                        VoteBox(label = "HOLD", count = deliberation.holdVotes, color = CyanBlue)
                        VoteBox(label = "WAIT", count = deliberation.waitVotes, color = AmberWarning)
                        VoteBox(label = "NO TRADE", count = deliberation.noTradeVotes, color = BearishRed)
                    }

                    if (deliberation.isNoTradeAdvised) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BearishRed.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = BearishRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NO TRADE Intelligence Layer: Capital preservation engaged. Standing aside preserves experimental ₹25,000 capital until high-probability confluence appears.",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3B. DEDICATED 4-PILLAR AI EXPLAINABILITY INSPECTOR
            item {
                AiExplainabilityCard(deliberation = deliberation)
            }

            // 4. INDIVIDUAL AGENT BREAKDOWNS
            item {
                Text(
                    text = "Agent Council Deliberations",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(deliberation.agentBreakdowns) { agent ->
                AgentCard(agent = agent)
            }

            // 5. ACTION
            item {
                Button(
                    onClick = onOpenPaperTrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("council_trade_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deliberation.isNoTradeAdvised) SurfaceElevated else ElectricTeal
                    )
                ) {
                    Text(
                        text = if (deliberation.isNoTradeAdvised) "Open Paper Trade (Review Risk Warning)" else "Execute Paper Trade on Consensus",
                        color = if (deliberation.isNoTradeAdvised) TextSecondary else BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AgentCard(agent: AiAgentResult) {
    FintechCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = agent.agentName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = agent.agentRole,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            DecisionBadge(decision = agent.decision)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = agent.reasoning,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScorePill(label = "Confidence", value = "${agent.confidence}%")
            ScorePill(label = "Tech Score", value = "${agent.technicalScore}/100")
            ScorePill(label = "Fund Score", value = "${agent.fundamentalScore}/100")
            ScorePill(label = "Risk Score", value = "${agent.riskScore}/100")
        }
    }
}

@Composable
fun VoteBox(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceElevated
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = count.toString(), color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ScorePill(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
