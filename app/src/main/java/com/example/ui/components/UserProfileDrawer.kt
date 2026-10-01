package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import com.example.data.model.UserProfile
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

/**
 * User Profile Left-Side Navigation Drawer Sheet containing the User Profile header,
 * navigation destinations, Settings & Notification setup modal triggers, and Logout.
 */
@Composable
fun UserProfileDrawerSheet(
    userProfile: UserProfile,
    portfolio: PortfolioEntity,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLockTerminal: () -> Unit = {},
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("user_profile_drawer"),
        drawerContainerColor = SurfaceDark,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- 1. USER PROFILE HEADER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_profile_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ElectricTeal.copy(alpha = 0.12f), SurfaceElevated)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                                    .border(1.5.dp, ElectricTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                                    color = ElectricTeal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // 2FA Verified Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BullishGreen.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(11.dp))
                                    Text("2FA ACTIVE", color = BullishGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = userProfile.name,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.email,
                            color = CyanBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = userProfile.role,
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Account Quick Snapshot
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Capital", color = TextMuted, fontSize = 9.5.sp)
                                    Text("₹${portfolio.startingCapital.toInt()}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Target Progress", color = TextMuted, fontSize = 9.5.sp)
                                    val progressPct = ((portfolio.realizedPnlToday / portfolio.dailyTarget) * 100.0).toInt().coerceAtLeast(0)
                                    Text("$progressPct%", color = if (portfolio.realizedPnlToday >= portfolio.dailyTarget) GoldAccent else ElectricTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Risk Engine", color = TextMuted, fontSize = 9.5.sp)
                                    Text("ARMED 🛡️", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // --- 2. PRIMARY DESTINATIONS ---
            Text(
                text = "NAVIGATION",
                color = TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            val navItems = listOf(
                Screen.DASHBOARD to Icons.Default.Dashboard,
                Screen.MARKETS to Icons.AutoMirrored.Filled.ShowChart,
                Screen.BROKER_CONNECTIONS to Icons.Default.AccountBalance,
                Screen.COPILOT to Icons.Default.SmartToy,
                Screen.AI_COUNCIL to Icons.Default.AutoAwesome,
                Screen.STRATEGIES to Icons.Default.Science,
                Screen.BACKTEST to Icons.Default.Timeline,
                Screen.JOURNAL to Icons.Default.Book,
                Screen.RISK_CENTER to Icons.Default.Shield,
                Screen.DAILY_SUMMARY to Icons.Default.Assessment,
                Screen.SCANNER to Icons.Default.Radar
            )

            navItems.forEach { (screen, icon) ->
                val isSelected = currentScreen == screen
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ElectricTeal.copy(alpha = 0.15f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigate(screen)
                            onCloseDrawer()
                        }
                        .testTag("drawer_nav_${screen.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            tint = if (isSelected) ElectricTeal else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = screen.title,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricTeal)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))

            // --- 3. SETTINGS & NOTIFICATION SETUP SECTION ---
            Text(
                text = "SYSTEM PREFERENCES",
                color = TextMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Broker Connections & OAuth2 Settings Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate(Screen.BROKER_CONNECTIONS)
                        onCloseDrawer()
                    }
                    .testTag("drawer_broker_connections_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Broker Connections", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("OAuth2, Zerodha Kite, KeyStore Vault", color = TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            // Notification Setup Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenNotifications()
                        onCloseDrawer()
                    }
                    .testTag("drawer_notification_setup_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Setup", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("Target alerts, kill-switch, consensus", color = TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            // General Settings Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenSettings()
                        onCloseDrawer()
                    }
                    .testTag("drawer_settings_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Security & Settings", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("2FA toggle, biometrics, broker API", color = TextMuted, fontSize = 10.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 1. Lock Terminal (PIN / Biometric)
            OutlinedButton(
                onClick = {
                    onLockTerminal()
                    onCloseDrawer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("drawer_lock_terminal_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricTeal)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock Terminal (PIN)", color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Sign Out & Re-Authenticate Button
            OutlinedButton(
                onClick = {
                    onLogout()
                    onCloseDrawer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("drawer_logout_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = BearishRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out & Return to Login", color = BearishRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
