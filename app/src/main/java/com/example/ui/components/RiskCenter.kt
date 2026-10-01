package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.model.RiskStatus
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * RiskCenter component that visually displays current daily loss, daily target progress,
 * and portfolio exposure metrics, linking them directly to the Risk Engine's status.
 */
@Composable
fun RiskCenter(
    portfolio: PortfolioEntity,
    positions: List<PositionEntity>,
    onToggleKillSwitch: () -> Unit = {},
    onUnlockTrading: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1. Calculations
    val realizedToday = portfolio.realizedPnlToday
    val target = max(1.0, portfolio.dailyTarget)
    val maxLoss = max(1.0, portfolio.maxDailyLoss)

    // Daily target progress (0.0 to 1.0)
    val rawTargetProgress = if (realizedToday > 0) (realizedToday / target).toFloat() else 0f
    val targetProgress by animateFloatAsState(targetValue = min(1f, rawTargetProgress), label = "targetProgress")

    // Daily loss progress towards limit (0.0 to 1.0)
    val rawLossProgress = if (realizedToday < 0) (abs(realizedToday) / maxLoss).toFloat() else 0f
    val lossProgress by animateFloatAsState(targetValue = min(1f, rawLossProgress), label = "lossProgress")

    // Portfolio Exposure calculations
    val investedCapital = positions.sumOf { it.buyPrice * it.quantity }
    val totalCapital = max(1.0, portfolio.startingCapital)
    val rawExposure = (investedCapital / totalCapital).toFloat()
    val exposureProgress by animateFloatAsState(targetValue = min(1f, rawExposure), label = "exposureProgress")

    // Value at Risk (VaR) from stop losses
    val valueAtRisk = positions.sumOf {
        val riskPerShare = max(0.0, it.buyPrice - it.stopLoss)
        riskPerShare * it.quantity
    }
    val varPercent = (valueAtRisk / totalCapital) * 100.0

    // Determine Risk Engine Status
    val (engineStatus, statusColor, statusIcon, statusHeadline, statusExplanation) = when {
        portfolio.killSwitchTriggered -> {
            Tuple5(
                RiskStatus.LOCKED,
                BearishRed,
                Icons.Default.Dangerous,
                "EMERGENCY KILL SWITCH ENGAGED",
                "All order routing, paper simulation, and execution are completely halted by master override."
            )
        }
        portfolio.isLocked -> {
            Tuple5(
                RiskStatus.LOCKED,
                BearishRed,
                Icons.Default.Lock,
                "TRADING HALTED BY RISK ENGINE",
                portfolio.lockReason.ifEmpty { "Safety threshold breached. All new trade submissions blocked." }
            )
        }
        realizedToday <= -portfolio.maxDailyLoss -> {
            Tuple5(
                RiskStatus.LOCKED,
                BearishRed,
                Icons.Default.Warning,
                "MAX DAILY LOSS LIMIT BREACHED (-₹${portfolio.maxDailyLoss.toInt()})",
                "Trading is locked to protect remaining capital and prevent emotional tilt."
            )
        }
        portfolio.profitLockEnabled && realizedToday >= portfolio.dailyTarget -> {
            Tuple5(
                RiskStatus.LOCKED,
                GoldAccent,
                Icons.Default.CheckCircle,
                "DAILY TARGET ACHIEVED (PROFIT LOCKED)",
                "Realized gains of ₹${realizedToday.toInt()} reached the ₹${portfolio.dailyTarget.toInt()} goal. Trading locked to prevent giving back profits."
            )
        }
        rawLossProgress >= 0.7f -> {
            Tuple5(
                RiskStatus.CAUTION,
                AmberWarning,
                Icons.Default.WarningAmber,
                "ELEVATED RISK: DRAWDOWN WARNING",
                "Current drawdown is near the safety boundary. Position sizing is automatically constrained."
            )
        }
        else -> {
            Tuple5(
                RiskStatus.SAFE,
                BullishGreen,
                Icons.Default.Shield,
                "RISK ENGINE: NORMAL OPERATION",
                "Deterministic capital protection active. AI Council trades validated against 1% risk limit."
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("risk_center_component"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- SECTION 1: RISK ENGINE STATUS BANNER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("risk_engine_status_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.8f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.18f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Risk Engine Status",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = statusHeadline,
                                color = statusColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = engineStatus.name,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = statusExplanation,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (portfolio.killSwitchTriggered || portfolio.isLocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onUnlockTrading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("risk_engine_unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Safety Locks & Resume Trading", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SECTION 2: DAILY TARGET PROGRESS (₹1,000 GOAL) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_target_card"),
            shape = RoundedCornerShape(14.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Daily Target Progress",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Goal: ₹${portfolio.dailyTarget.toInt()}/day",
                        color = ElectricTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (realizedToday >= 0) "+₹${realizedToday.toInt()}" else "-₹${abs(realizedToday).toInt()}",
                        color = if (realizedToday >= 0) BullishGreen else BearishRed,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${(targetProgress * 100).toInt()}% completed",
                        color = if (targetProgress >= 1f) GoldAccent else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { targetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (targetProgress >= 1f) GoldAccent else ElectricTeal,
                    trackColor = BackgroundDark,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (portfolio.profitLockEnabled) "🔒 Profit Lock: ENABLED" else "Profit Lock: Disabled",
                        color = if (portfolio.profitLockEnabled) BullishGreen else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Non-guaranteed experimental target",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // --- SECTION 3: CURRENT DAILY LOSS METER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_loss_card"),
            shape = RoundedCornerShape(14.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = BearishRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Current Daily Drawdown",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Max Limit: -₹${portfolio.maxDailyLoss.toInt()}",
                        color = BearishRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val currentLossAmount = if (realizedToday < 0) abs(realizedToday) else 0.0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "-₹${currentLossAmount.toInt()}",
                        color = if (currentLossAmount > 0) BearishRed else TextMuted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${(lossProgress * 100).toInt()}% of loss budget used",
                        color = if (lossProgress >= 0.8f) BearishRed else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { lossProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (lossProgress >= 0.8f) BearishRed else AmberWarning,
                    trackColor = BackgroundDark,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If drawdown reaches -₹${portfolio.maxDailyLoss.toInt()}, Risk Engine locks all further trades until reset.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // --- SECTION 4: PORTFOLIO EXPOSURE & VALUE AT RISK (VaR) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("portfolio_exposure_card"),
            shape = RoundedCornerShape(14.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = CyanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Portfolio Exposure Metrics",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyanBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(exposureProgress * 100).toInt()}% Allocated",
                            color = CyanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Available Cash", color = TextMuted, fontSize = 11.sp)
                        Text("₹${portfolio.availableCash.toInt()}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invested Margin", color = TextMuted, fontSize = 11.sp)
                        Text("₹${investedCapital.toInt()}", color = CyanBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Open Positions", color = TextMuted, fontSize = 11.sp)
                        Text("${positions.size} / ${portfolio.maxOpenPositions}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Value At Risk Callout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                            Text("Total Stop Loss Risk (VaR):", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            text = "₹${valueAtRisk.toInt()} (${(varPercent * 10).toInt() / 10.0}%)",
                            color = if (varPercent > 2.0) BearishRed else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
