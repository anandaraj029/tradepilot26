package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.db.PortfolioEntity
import com.example.data.model.TradingMode
import com.example.ui.components.AutoPilotCard
import com.example.ui.components.FintechCard
import com.example.ui.components.RiskCenter
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState

@Composable
fun RiskCenterScreen(
    uiState: TradePilotUiState,
    onSaveRiskSettings: (Double, Double, Double, Double, Int, Boolean, String) -> Unit,
    onToggleKillSwitch: () -> Unit,
    onUnlockTrading: () -> Unit,
    onResetCapital: () -> Unit,
    onToggleAutoPilot: () -> Unit = {},
    onRunAutoPilotCycle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val portfolio = uiState.portfolio

    var startingCapitalText by remember(portfolio) { mutableStateOf(portfolio.startingCapital.toInt().toString()) }
    var dailyTargetText by remember(portfolio) { mutableStateOf(portfolio.dailyTarget.toInt().toString()) }
    var maxDailyLossText by remember(portfolio) { mutableStateOf(portfolio.maxDailyLoss.toInt().toString()) }
    var maxRiskPercentText by remember(portfolio) { mutableStateOf(portfolio.maxRiskPerTradePercent.toString()) }
    var maxPositionsText by remember(portfolio) { mutableStateOf(portfolio.maxOpenPositions.toString()) }
    var profitLockEnabled by remember(portfolio) { mutableStateOf(portfolio.profitLockEnabled) }
    var selectedTradingMode by remember(portfolio) { mutableStateOf(portfolio.tradingMode) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. VISUAL RISK CENTER METRICS & ENGINE STATUS
        item {
            RiskCenter(
                portfolio = portfolio,
                positions = uiState.positions,
                onToggleKillSwitch = onToggleKillSwitch,
                onUnlockTrading = onUnlockTrading
            )
        }

        // 1.5. AUTONOMOUS AUTO-PILOT TARGET ENGINE (STOP/START CONTROLS)
        item {
            AutoPilotCard(
                portfolio = portfolio,
                isAutoPilotActive = uiState.isAutoPilotActive,
                autoPilotStatus = uiState.autoPilotStatus,
                autoPilotLastAction = uiState.autoPilotLastAction,
                onToggleAutoPilot = onToggleAutoPilot,
                onRunManualCycle = onRunAutoPilotCycle
            )
        }
        // 1. RISK ENGINE HEADER
        item {
            FintechCard(
                borderColor = if (portfolio.killSwitchTriggered || portfolio.isLocked) BearishRed else BullishGreen
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (portfolio.killSwitchTriggered || portfolio.isLocked) BearishRed.copy(alpha = 0.2f) else BullishGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (portfolio.killSwitchTriggered || portfolio.isLocked) BearishRed else BullishGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Capital Protection Engine",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (portfolio.killSwitchTriggered) "HALTED: Emergency Kill Switch Active"
                                   else if (portfolio.isLocked) "LOCKED: ${portfolio.lockReason}"
                                   else "ACTIVE: Deterministic limits enforced",
                            color = if (portfolio.killSwitchTriggered || portfolio.isLocked) BearishRed else BullishGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Rule Hierarchy: 1. Capital Protection > 2. Broker / Risk Limits > 3. User Risk Limits > 4. Strategy Rules > 5. AI Council. The AI Council cannot override risk limits.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // 2. EMERGENCY KILL SWITCH
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (portfolio.killSwitchTriggered) BearishRedContainer else SurfaceDark
                ),
                border = BorderStroke(1.dp, BearishRed)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Emergency Kill Switch",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (portfolio.killSwitchTriggered) "ENGAGED: All new trades blocked" else "Disengaged: Normal operation",
                                color = if (portfolio.killSwitchTriggered) BearishRed else TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onToggleKillSwitch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (portfolio.killSwitchTriggered) ElectricTeal else BearishRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("emergency_kill_toggle")
                        ) {
                            Text(
                                text = if (portfolio.killSwitchTriggered) "DISENGAGE" else "TRIGGER KILL SWITCH",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (portfolio.isLocked && !portfolio.killSwitchTriggered) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onUnlockTrading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Clear Daily Lock (Resume Paper Trading)", color = ElectricTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. PARAMETERS CONFIGURATION
        item {
            FintechCard {
                Text(
                    text = "Risk Limits & Capital Parameters",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Starting Capital
                OutlinedTextField(
                    value = startingCapitalText,
                    onValueChange = { startingCapitalText = it },
                    label = { Text("Starting Capital (₹)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Daily Profit Target
                OutlinedTextField(
                    value = dailyTargetText,
                    onValueChange = { dailyTargetText = it },
                    label = { Text("Daily Profit Target (₹) [Configurable goal, not guaranteed]", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Maximum Daily Loss
                OutlinedTextField(
                    value = maxDailyLossText,
                    onValueChange = { maxDailyLossText = it },
                    label = { Text("Maximum Daily Loss Limit (₹)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = maxRiskPercentText,
                        onValueChange = { maxRiskPercentText = it },
                        label = { Text("Max Risk / Trade (%)", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = ElectricTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = maxPositionsText,
                        onValueChange = { maxPositionsText = it },
                        label = { Text("Max Open Positions", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = ElectricTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profit Lock Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Profit Lock",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lock trading and protect gains when target (₹${dailyTargetText}) is achieved",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = profitLockEnabled,
                        onCheckedChange = { profitLockEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = ElectricTeal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        val cap = startingCapitalText.toDoubleOrNull() ?: 25000.0
                        val target = dailyTargetText.toDoubleOrNull() ?: 1000.0
                        val loss = maxDailyLossText.toDoubleOrNull() ?: 500.0
                        val riskPct = maxRiskPercentText.toDoubleOrNull() ?: 1.0
                        val maxPos = maxPositionsText.toIntOrNull() ?: 4
                        onSaveRiskSettings(cap, target, loss, riskPct, maxPos, profitLockEnabled, selectedTradingMode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_risk_settings_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text(
                        text = "Update Risk Parameters",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 4. TRADING MODES
        item {
            FintechCard {
                Text(
                    text = "System Trading Mode",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automated execution requires sandbox verification and is kept disabled by default.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                TradingMode.values().forEach { mode ->
                    val isSelected = selectedTradingMode == mode.name
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SurfaceElevated else SurfaceDark,
                        border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    color = if (isSelected) ElectricTeal else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.description,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTradingMode = mode.name },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricTeal)
                            )
                        }
                    }
                }
            }
        }

        // 5. RESTORE CAPITAL
        item {
            OutlinedButton(
                onClick = onResetCapital,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Restore Virtual Portfolio to Initial ₹25,000", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
