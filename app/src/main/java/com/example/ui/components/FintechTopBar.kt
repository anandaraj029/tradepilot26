package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.db.PortfolioEntity
import com.example.data.model.MarketType
import com.example.data.model.RiskStatus
import com.example.data.model.StockQuote
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

/**
 * Pinned Top Navigation Bar for AI TradePilot.
 *
 * Ensures full visibility above all scrollable content across screens and is
 * properly insets-aware (safely offset below the system status bar and camera notch).
 * Fixes any text wrapping issues by using single-line constraints with ellipsis.
 */
@Composable
fun FintechTopBar(
    portfolio: PortfolioEntity,
    onKillSwitchClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    currentScreen: Screen = Screen.DASHBOARD,
    isViewingStockDetail: Boolean = false,
    selectedStock: StockQuote? = null,
    onBackClick: (() -> Unit)? = null,
    isLiveMode: Boolean = false,
    liveMargin: Double = 50000.0,
    onOpenBrokerDialog: () -> Unit = {},
    onOpenAddFunds: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showKillSwitchConfirm by remember { mutableStateOf(false) }

    val status = when {
        portfolio.killSwitchTriggered || portfolio.isLocked -> RiskStatus.LOCKED
        portfolio.realizedPnlToday <= -portfolio.maxDailyLoss -> RiskStatus.LOCKED
        portfolio.realizedPnlToday < -200.0 -> RiskStatus.CAUTION
        else -> RiskStatus.SAFE
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(999f),
        color = BackgroundDark,
        tonalElevation = 8.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, BorderDark.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Area: Back Button or Menu Button + Brand / Screen Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    if (isViewingStockDetail && onBackClick != null) {
                        // Back Button when in Stock Detail View
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .border(1.dp, BorderDark, CircleShape)
                                .testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ElectricTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedStock?.symbol ?: "Stock Details",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                selectedStock?.let { st ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (st.market == MarketType.INDIA) ElectricTeal.copy(alpha = 0.15f) else CyanBlue.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, if (st.market == MarketType.INDIA) ElectricTeal else CyanBlue)
                                    ) {
                                        Text(
                                            text = if (st.market == MarketType.INDIA) "NSE" else "NYSE",
                                            color = if (st.market == MarketType.INDIA) ElectricTeal else CyanBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = selectedStock?.companyName ?: "Deep AI Analysis",
                                color = TextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Primary Top Navigation with Drawer Menu trigger
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                .testTag("top_bar_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = ElectricTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Logo Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, ElectricTeal.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onMenuClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TP",
                                color = ElectricTeal,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onMenuClick)
                        ) {
                            Text(
                                text = if (currentScreen == Screen.DASHBOARD) "AI TradePilot" else currentScreen.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLiveMode) BearishRed else BullishGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLiveMode) "Live Mode" else "Paper Mode",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Right Area: Live Mode Switcher + Risk Status + Kill Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Broker / Live Mode Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLiveMode) BearishRed.copy(alpha = 0.18f) else SurfaceElevated,
                        border = BorderStroke(1.dp, if (isLiveMode) BearishRed else BorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenBrokerDialog)
                            .testTag("top_bar_broker_mode_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveMode) BearishRed else CyanBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveMode) "LIVE" else "PAPER",
                                color = if (isLiveMode) BearishRed else CyanBlue,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Compact Risk Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (status) {
                            RiskStatus.SAFE -> BullishGreen.copy(alpha = 0.15f)
                            RiskStatus.CAUTION -> AmberWarning.copy(alpha = 0.15f)
                            RiskStatus.CRITICAL, RiskStatus.LOCKED -> BearishRed.copy(alpha = 0.2f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when (status) {
                                RiskStatus.SAFE -> BullishGreen.copy(alpha = 0.5f)
                                RiskStatus.CAUTION -> AmberWarning.copy(alpha = 0.5f)
                                RiskStatus.CRITICAL, RiskStatus.LOCKED -> BearishRed.copy(alpha = 0.6f)
                            }
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (status == RiskStatus.LOCKED) Icons.Default.Dangerous else Icons.Default.Shield,
                                contentDescription = null,
                                tint = when (status) {
                                    RiskStatus.SAFE -> BullishGreen
                                    RiskStatus.CAUTION -> AmberWarning
                                    RiskStatus.CRITICAL, RiskStatus.LOCKED -> BearishRed
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (status) {
                                    RiskStatus.SAFE -> "SAFE"
                                    RiskStatus.CAUTION -> "CAUTION"
                                    RiskStatus.CRITICAL, RiskStatus.LOCKED -> "LOCKED"
                                },
                                color = when (status) {
                                    RiskStatus.SAFE -> BullishGreen
                                    RiskStatus.CAUTION -> AmberWarning
                                    RiskStatus.CRITICAL, RiskStatus.LOCKED -> BearishRed
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Kill Switch Button
                    val isKillOn = portfolio.killSwitchTriggered
                    IconButton(
                        onClick = { showKillSwitchConfirm = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isKillOn) BearishRed else SurfaceElevated)
                            .border(1.dp, if (isKillOn) BearishRed else BorderDark, CircleShape)
                            .testTag("kill_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Emergency Kill Switch",
                            tint = if (isKillOn) Color.White else BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Locked banner if active
            AnimatedVisibility(
                visible = portfolio.isLocked || portfolio.killSwitchTriggered,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        color = BearishRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dangerous,
                                    contentDescription = null,
                                    tint = BearishRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = portfolio.lockReason.ifEmpty { "TRADING PAUSED BY RISK ENGINE" },
                                    color = BearishRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            TextButton(
                                onClick = onUnlockClick,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = ElectricTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RESET LOCK",
                                    color = ElectricTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKillSwitchConfirm) {
        AlertDialog(
            onDismissRequest = { showKillSwitchConfirm = false },
            title = {
                Text(
                    text = if (portfolio.killSwitchTriggered) "Disengage Kill Switch?" else "🚨 Activate Emergency Kill Switch?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (portfolio.killSwitchTriggered)
                        "This will re-enable trading validations and permit order execution."
                    else
                        "Activating the Emergency Kill Switch immediately blocks all automated, assisted, and paper trades across all 1,020 assets. Use this if market conditions become abnormal or volatile.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onKillSwitchClick()
                        showKillSwitchConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (portfolio.killSwitchTriggered) ElectricTeal else BearishRed
                    )
                ) {
                    Text(
                        text = if (portfolio.killSwitchTriggered) "Disengage Kill Switch" else "ACTIVATE KILL SWITCH",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchConfirm = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevated
        )
    }
}
