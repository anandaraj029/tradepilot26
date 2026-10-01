package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modal dialog for configuring comprehensive trading notifications.
 */
@Composable
fun NotificationSetupDialog(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var targetAlerts by remember { mutableStateOf(userProfile.dailyTargetAlerts) }
    var killSwitchAlerts by remember { mutableStateOf(userProfile.killSwitchAlerts) }
    var consensusAlerts by remember { mutableStateOf(userProfile.aiConsensusAlerts) }
    var profitLockAlerts by remember { mutableStateOf(userProfile.profitLockAlerts) }
    var soundEnabled by remember { mutableStateOf(userProfile.soundEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("notification_setup_dialog"),
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = CyanBlue.copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Notification Setup", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Real-time telemetry & safety alerts", color = TextMuted, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotificationToggleRow(
                    title = "Daily Target Milestones",
                    description = "Pushes alert when 50%, 80%, and 100% of the ₹1,000 daily goal is achieved.",
                    checked = targetAlerts,
                    onCheckedChange = { targetAlerts = it }
                )

                NotificationToggleRow(
                    title = "Kill Switch & Loss Limit",
                    description = "Urgent alert if daily drawdown approaches the -₹500 capital protection boundary.",
                    checked = killSwitchAlerts,
                    onCheckedChange = { killSwitchAlerts = it }
                )

                NotificationToggleRow(
                    title = "AI Council Consensus",
                    description = "Alerts when Gemini, Claude, and ChatGPT achieve ≥80% confidence alignment.",
                    checked = consensusAlerts,
                    onCheckedChange = { consensusAlerts = it }
                )

                NotificationToggleRow(
                    title = "Profit Lock Activation",
                    description = "Alerts when trading is automatically locked to protect realized daily profits.",
                    checked = profitLockAlerts,
                    onCheckedChange = { profitLockAlerts = it }
                )

                NotificationToggleRow(
                    title = "Sound & Haptic Feedback",
                    description = "Audio tone and haptic tick on order fill or safety trigger.",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        userProfile.copy(
                            dailyTargetAlerts = targetAlerts,
                            killSwitchAlerts = killSwitchAlerts,
                            aiConsensusAlerts = consensusAlerts,
                            profitLockAlerts = profitLockAlerts,
                            soundEnabled = soundEnabled
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Notification Rules", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Modal dialog for configuring Security, 2FA, Biometrics, Live Broker Mode, and Settings.
 */
@Composable
fun SecuritySettingsDialog(
    userProfile: UserProfile,
    isLiveMode: Boolean = false,
    onToggleLiveMode: (Boolean) -> Unit = {},
    onNavigateToBrokers: () -> Unit = {},
    onSaveProfile: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var is2fa by remember { mutableStateOf(userProfile.is2faEnabled) }
    var isBiometric by remember { mutableStateOf(userProfile.isBiometricEnabled) }
    var selectedBroker by remember { mutableStateOf(userProfile.brokerIntegration) }
    var showBiometricPromptForLive by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("security_settings_dialog"),
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = ElectricTeal.copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Security & Settings", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Trader credentials & execution routing", color = TextMuted, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trader Info Readout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("AUTHENTICATED IDENTITY", color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Text(userProfile.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(userProfile.email, color = CyanBlue, fontSize = 11.sp)
                        Text(userProfile.tier, color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 1. LIVE TRADING SWITCH (Biometrically Protected)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLiveMode) BearishRed.copy(alpha = 0.12f) else SurfaceDark,
                    border = BorderStroke(1.5.dp, if (isLiveMode) BearishRed.copy(alpha = 0.6f) else BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isLiveMode) "🔴 Live Broker Order Routing" else "📄 Live Broker Execution",
                                    color = if (isLiveMode) BearishRed else TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (isLiveMode) BearishRed else CyanBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (isLiveMode) "REAL CAPITAL ACTIVE: Orders routed to linked broker. Biometric authorization confirmed."
                                else "When toggled, enables real broker order routing. Protected by biometric confirmation.",
                                color = if (isLiveMode) BearishRed.copy(alpha = 0.85f) else TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                        Switch(
                            checked = isLiveMode,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    showBiometricPromptForLive = true
                                } else {
                                    onToggleLiveMode(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = BearishRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceElevated
                            ),
                            modifier = Modifier.testTag("settings_live_trading_switch")
                        )
                    }
                }

                // Manage Broker Connections Shortcut
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onNavigateToBrokers()
                        }
                        .testTag("settings_manage_brokers_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Broker Connections & OAuth2", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Zerodha, Angel One, Upstox, KeyStore", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }

                NotificationToggleRow(
                    title = "Two-Factor Authentication (2FA)",
                    description = "Requires 6-digit TOTP code on terminal startup and risk limit changes.",
                    checked = is2fa,
                    onCheckedChange = { is2fa = it }
                )

                NotificationToggleRow(
                    title = "Biometric Quick Unlock",
                    description = "Allow Fingerprint / Face ID for faster session resumption.",
                    checked = isBiometric,
                    onCheckedChange = { isBiometric = it }
                )

                // Broker Routing Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Execution Gateway", color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    listOf("Zerodha Kite Connect v3 (Live)", "Paper Trading Simulator", "Angel One SmartAPI").forEach { broker ->
                        val isSel = selectedBroker == broker
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ElectricTeal.copy(alpha = 0.18f) else SurfaceDark,
                            border = BorderStroke(1.dp, if (isSel) ElectricTeal else BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBroker = broker }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(broker, color = if (isSel) ElectricTeal else TextSecondary, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        userProfile.copy(
                            is2faEnabled = is2fa,
                            isBiometricEnabled = isBiometric,
                            brokerIntegration = selectedBroker
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Settings", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 12.sp)
            }
        }
    )

    // Secondary Biometric Confirmation Dialog for Live Mode Switch
    if (showBiometricPromptForLive) {
        BiometricLiveConfirmationDialog(
            onConfirm = {
                onToggleLiveMode(true)
                showBiometricPromptForLive = false
            },
            onDismiss = { showBiometricPromptForLive = false }
        )
    }
}

/**
 * Secondary Biometric Confirmation Sheet protecting real money live execution.
 */
@Composable
fun BiometricLiveConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isVerifying by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biometric_live_confirmation_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.5.dp, BearishRed.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BearishRed.copy(alpha = 0.2f))
                        .border(1.5.dp, BearishRed.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = BearishRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Authorize Live Broker Mode",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "You are enabling REAL execution paths routed to your connected broker. Capital is at market risk.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BackgroundDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "SAFEGUARDS ENFORCED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "• 1% Max Risk Per Trade\n• ₹500 Daily Drawdown Circuit Breaker\n• Instant Emergency Kill-Switch Armed", color = TextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }

                if (isVerifying) {
                    CircularProgressIndicator(color = BearishRed, modifier = Modifier.size(24.dp))
                } else {
                    Button(
                        onClick = {
                            isVerifying = true
                            coroutineScope.launch {
                                delay(400)
                                isVerifying = false
                                onConfirm()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("confirm_biometric_live_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm with Biometrics / Touch ID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel & Keep Paper Trading", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(description, color = TextMuted, fontSize = 10.sp, lineHeight = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = ElectricTeal,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceElevated
                )
            )
        }
    }
}
