package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Interactive Auto-Pilot controller card with Start / Stop options,
 * Live Zerodha Kite integration, and Autonomous AI Responsibility telemetry.
 */
@Composable
fun AutoPilotCard(
    portfolio: PortfolioEntity,
    isAutoPilotActive: Boolean,
    autoPilotStatus: String,
    autoPilotLastAction: String,
    autoActivateWhenTargetPending: Boolean = true,
    isLiveMode: Boolean = false,
    isAutonomousLive: Boolean = true,
    onToggleAutoPilot: () -> Unit,
    onStartAutoPilot: () -> Unit = onToggleAutoPilot,
    onStopAutoPilot: () -> Unit = onToggleAutoPilot,
    onToggleAutoActivate: (Boolean) -> Unit = {},
    onRunManualCycle: () -> Unit = {},
    onOpenBrokerSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dailyTarget = max(1.0, portfolio.dailyTarget)
    val realizedToday = portfolio.realizedPnlToday
    val remainingToTarget = max(0.0, dailyTarget - realizedToday)
    val isTargetAchieved = realizedToday >= dailyTarget
    val isLossLimitReached = realizedToday <= -portfolio.maxDailyLoss

    val progressRaw = (realizedToday / dailyTarget).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressRaw, label = "autoPilotProgress")

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isTargetAchieved -> GoldAccent
            isLossLimitReached -> BearishRed
            isLiveMode && isAutoPilotActive -> BearishRed
            isAutoPilotActive -> ElectricTeal
            else -> BorderDark
        },
        label = "autoPilotBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("auto_pilot_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Broker Routing Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLiveMode) BearishRed.copy(alpha = 0.12f) else SurfaceDark,
                border = BorderStroke(1.dp, if (isLiveMode) BearishRed.copy(alpha = 0.5f) else BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBrokerSettings)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLiveMode) BearishRed else CyanBlue)
                        )
                        Text(
                            text = if (isLiveMode) "🔴 ROUTING: ZERODHA KITE LIVE (NSE/BSE)" else "📄 ROUTING: PAPER TRADING (VIRTUAL)",
                            color = if (isLiveMode) BearishRed else CyanBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isLiveMode && isAutonomousLive) "AI RESPONSIBILITY: FULL" else "CONFIG",
                            color = if (isLiveMode && isAutonomousLive) ElectricTeal else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Header: Bot Icon, Title, and Live Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAutoPilotActive) Brush.linearGradient(listOf(if (isLiveMode) BearishRed.copy(alpha = 0.3f) else ElectricTeal.copy(alpha = 0.3f), SurfaceDark))
                                else Brush.linearGradient(listOf(SurfaceDark, SurfaceCard))
                            )
                            .border(
                                1.5.dp,
                                if (isAutoPilotActive) (if (isLiveMode) BearishRed else ElectricTeal) else BorderDark,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAutoPilotActive) Icons.Default.SmartToy else Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = if (isAutoPilotActive) (if (isLiveMode) BearishRed else ElectricTeal) else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Auto-Pilot Trading Bot",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (isAutoPilotActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isTargetAchieved) GoldAccent else if (isLiveMode) BearishRed.copy(alpha = pulseAlpha) else BullishGreen.copy(alpha = pulseAlpha))
                                )
                            }
                        }
                        Text(
                            text = if (isLiveMode) "Autonomous Zerodha execution for ₹${dailyTarget.toInt()} target" else "Automated ₹${dailyTarget.toInt()} target hunting & profit locking",
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isTargetAchieved -> GoldAccent.copy(alpha = 0.15f)
                        isLossLimitReached -> BearishRed.copy(alpha = 0.15f)
                        isAutoPilotActive -> (if (isLiveMode) BearishRed.copy(alpha = 0.15f) else BullishGreen.copy(alpha = 0.15f))
                        else -> SurfaceDark
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isTargetAchieved -> GoldAccent
                            isLossLimitReached -> BearishRed
                            isAutoPilotActive -> (if (isLiveMode) BearishRed else BullishGreen)
                            else -> BorderDark
                        }
                    )
                ) {
                    Text(
                        text = when {
                            isTargetAchieved -> "TARGET ACHIEVED 🏆"
                            isLossLimitReached -> "LOSS STOPPED 🛑"
                            isAutoPilotActive -> if (isLiveMode) "LIVE BOT ⚡" else "ACTIVE ⚡"
                            else -> "STOPPED ⏸️"
                        },
                        color = when {
                            isTargetAchieved -> GoldAccent
                            isLossLimitReached -> BearishRed
                            isAutoPilotActive -> (if (isLiveMode) BearishRed else BullishGreen)
                            else -> TextMuted
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation / Target Condition Callout
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BackgroundDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isTargetAchieved) Icons.Default.Lock else if (isLiveMode) Icons.Default.Bolt else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isTargetAchieved) GoldAccent else if (isLiveMode) BearishRed else ElectricTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            isTargetAchieved -> "Daily target reached! Auto-Pilot has engaged Profit Lock and automatically halted trading to protect profits."
                            isLossLimitReached -> "Daily loss limit (-₹${portfolio.maxDailyLoss.toInt()}) reached. Auto-Pilot stopped automatically to preserve capital."
                            isAutoPilotActive && isLiveMode -> "AI has full autonomous responsibility on Zerodha Kite. It actively scans 1,020 assets, enters MIS orders, trails stop loss, and locks ₹${dailyTarget.toInt()} profit."
                            isAutoPilotActive -> "Auto-Pilot remains ACTIVE AUTOMATICALLY until the ₹${dailyTarget.toInt()} target is achieved. It scans 1,000+ assets and books profits."
                            else -> "Auto-Pilot is currently STOPPED. Tap 'START AUTO-PILOT' to automatically trade until today's target is achieved."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-Activate When Target Not Achieved Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (autoActivateWhenTargetPending) ElectricTeal else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Auto-Active While Target Pending",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isTargetAchieved) "Target achieved! Auto-pilot locked until next session."
                            else "Hunts automatically until ₹${dailyTarget.toInt()} target is achieved",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }

                    Switch(
                        checked = autoActivateWhenTargetPending,
                        onCheckedChange = onToggleAutoActivate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = ElectricTeal,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("auto_pilot_auto_active_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress towards Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Today's Realized Progress", color = TextMuted, fontSize = 10.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${if (realizedToday >= 0) "+₹" else "-₹"}${abs(realizedToday.toInt())}",
                            color = if (realizedToday >= 0) BullishGreen else BearishRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "/ ₹${dailyTarget.toInt()} Target",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isTargetAchieved) {
                        Text(
                            text = "₹${remainingToTarget.toInt()} remaining to reach target",
                            color = GoldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${((realizedToday / dailyTarget) * 100).toInt()}%",
                        color = if (isTargetAchieved) GoldAccent else ElectricTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "1,020 Assets Live",
                        color = CyanBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BackgroundDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isTargetAchieved) Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.7f), GoldAccent))
                            else Brush.horizontalGradient(listOf(ElectricTeal.copy(alpha = 0.7f), ElectricTeal))
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last telemetry event message
            Text(
                text = "Bot Log: $autoPilotLastAction",
                color = CyanBlue,
                fontSize = 10.5.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Start / Stop & Quick Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAutoPilotActive) {
                    // STOP Button
                    Button(
                        onClick = onStopAutoPilot,
                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("auto_pilot_stop_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP BOT", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    // START Button
                    Button(
                        onClick = onStartAutoPilot,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLiveMode) BearishRed else ElectricTeal),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTargetAchieved,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("auto_pilot_start_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isLiveMode) "START LIVE BOT" else "START BOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // Run Single Evaluation Cycle Button
                OutlinedButton(
                    onClick = onRunManualCycle,
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(44.dp)
                        .testTag("auto_pilot_evaluate_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Evaluate", color = CyanBlue, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
