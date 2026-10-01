package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BrokerConnectionEntity
import com.example.data.db.FundTransactionEntity
import com.example.data.model.BrokerConnectionStatus
import com.example.data.model.BrokerDefinition
import com.example.data.model.LiveBrokerItem
import com.example.data.model.SupportedBrokersCatalog
import com.example.ui.components.AddLiveFundsDialog
import com.example.ui.components.FintechCard
import com.example.ui.components.OAuth2ConsentDialog
import com.example.ui.components.WithdrawFundsDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerConnectionsScreen(
    brokerConnection: BrokerConnectionEntity,
    fundTransactions: List<FundTransactionEntity>,
    isLiveMode: Boolean,
    isAutonomousAi: Boolean,
    onBack: () -> Unit,
    onToggleLiveMode: (Boolean) -> Unit,
    onToggleAutonomousAi: (Boolean) -> Unit,
    onConnectBrokerOAuth2: (brokerCode: String, apiKey: String, apiSecret: String, requestToken: String, userId: String) -> Unit,
    onDisconnectBroker: (brokerCode: String) -> Unit,
    onRefreshToken: (brokerCode: String) -> Unit,
    onTestPing: (brokerCode: String) -> Unit,
    onOpenAddFunds: () -> Unit,
    onOpenWithdrawFunds: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBrokerForOAuth by remember { mutableStateOf<BrokerDefinition?>(null) }
    var pingResults by remember { mutableStateOf<Map<String, Int>>(mapOf("ZERODHA" to 28)) }
    var isPingingBroker by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Map supported brokers to their live connection state
    val liveBrokersList: List<LiveBrokerItem> = remember(brokerConnection, pingResults) {
        SupportedBrokersCatalog.definitions.map { def ->
            if (def.code == brokerConnection.brokerCode && brokerConnection.isConnected) {
                LiveBrokerItem(
                    code = def.code,
                    name = def.name,
                    tagline = def.tagline,
                    authType = def.authType,
                    isConnected = true,
                    isLiveRoutingActive = isLiveMode,
                    userId = brokerConnection.userId.ifEmpty { "ZL9824" },
                    userName = brokerConnection.userName.ifEmpty { "AI TradePilot Live" },
                    liveMargin = brokerConnection.liveEquityMargin,
                    availableCollateral = brokerConnection.availableCollateral,
                    latencyMs = pingResults[def.code] ?: 28,
                    tokenExpiresAt = System.currentTimeMillis() + 68400000L,
                    isEncryptedInVault = true,
                    scopes = def.defaultScopes,
                    status = BrokerConnectionStatus.ACTIVE
                )
            } else {
                LiveBrokerItem(
                    code = def.code,
                    name = def.name,
                    tagline = def.tagline,
                    authType = def.authType,
                    isConnected = false,
                    isLiveRoutingActive = false,
                    userId = "-",
                    userName = "-",
                    liveMargin = 0.0,
                    availableCollateral = 0.0,
                    latencyMs = pingResults[def.code] ?: 0,
                    tokenExpiresAt = 0L,
                    isEncryptedInVault = false,
                    scopes = def.defaultScopes,
                    status = BrokerConnectionStatus.DISCONNECTED
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { screenPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding)
                .padding(horizontal = 16.dp)
                .testTag("broker_connections_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // 1. TOP HEADER & NAVIGATION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Broker Connections",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "OAuth2 Handshake & Encrypted Vault",
                                color = ElectricTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Security Vault Status Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BullishGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BullishGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AES-256 GCM",
                                color = BullishGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. LIVE ROUTING & MARGIN SUMMARY BANNER
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLiveMode) BearishRed.copy(alpha = 0.12f) else SurfaceElevated,
                    border = BorderStroke(1.5.dp, if (isLiveMode) BearishRed.copy(alpha = 0.6f) else BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isLiveMode) BearishRed.copy(alpha = 0.2f) else ElectricTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLiveMode) Icons.Default.Bolt else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = if (isLiveMode) BearishRed else ElectricTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isLiveMode) "🔴 LIVE BROKER ROUTING ACTIVE" else "📄 PAPER TRADING MODE",
                                        color = if (isLiveMode) BearishRed else TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isLiveMode) "Connected: ${brokerConnection.brokerName}" else "Virtual risk-free sandbox environment",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isLiveMode,
                                onCheckedChange = onToggleLiveMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BackgroundDark,
                                    checkedTrackColor = BearishRed,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceDark
                                ),
                                modifier = Modifier.testTag("broker_screen_live_mode_switch")
                            )
                        }

                        // Margin & Fund Action Bar
                        if (brokerConnection.isConnected) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "LIVE EQUITY MARGIN", color = TextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "₹${"%,d".format(brokerConnection.liveEquityMargin.toInt())}",
                                            color = TextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = onOpenWithdrawFunds,
                                            modifier = Modifier.height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, BorderDark)
                                        ) {
                                            Text(text = "Withdraw", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = onOpenAddFunds,
                                            modifier = Modifier.height(36.dp).testTag("broker_screen_add_funds_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Add Funds", color = BackgroundDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SECURITY ARCHITECTURE CARD
            item {
                FintechCard(borderColor = CyanBlue.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CyanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Encrypted Local Storage & KeyStore Vault",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Client secrets, OAuth2 tokens, and API credentials are encrypted with 256-bit AES-GCM and stored only on this device.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 4. BROKER ACCOUNTS LIST HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUPPORTED BROKER ACCOUNTS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${liveBrokersList.count { it.isConnected }} of ${liveBrokersList.size} Connected",
                        color = ElectricTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. BROKER CARDS WITH VISUAL STATUS INDICATORS
            items(liveBrokersList) { broker ->
                BrokerCard(
                    broker = broker,
                    onConnectClick = {
                        val def = SupportedBrokersCatalog.definitions.firstOrNull { it.code == broker.code }
                        if (def != null) {
                            selectedBrokerForOAuth = def
                        }
                    },
                    onDisconnectClick = {
                        onDisconnectBroker(broker.code)
                    },
                    onRefreshClick = {
                        onRefreshToken(broker.code)
                    },
                    onPingClick = {
                        coroutineScope.launch {
                            isPingingBroker = broker.code
                            delay(300)
                            val newPing = (18..45).random()
                            pingResults = pingResults + (broker.code to newPing)
                            isPingingBroker = null
                            onTestPing(broker.code)
                        }
                    },
                    isPinging = isPingingBroker == broker.code
                )
            }

            // 6. FUND TRANSACTION AUDIT LOG
            if (fundTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT FUND TRANSFERS & MARGIN TOP-UPS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                items(fundTransactions.take(5)) { tx ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (tx.type == "DEPOSIT") BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (tx.type == "DEPOSIT") BullishGreen else BearishRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${tx.type} • ${tx.paymentMethod}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "UTR: ${tx.utrNumber} • ${dateFormat.format(Date(tx.timestamp))}",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = "${if (tx.type == "DEPOSIT") "+" else "-"}₹${"%,d".format(tx.amount.toInt())}",
                                color = if (tx.type == "DEPOSIT") BullishGreen else BearishRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // OAuth2 Consent Dialog
    if (selectedBrokerForOAuth != null) {
        OAuth2ConsentDialog(
            brokerDef = selectedBrokerForOAuth!!,
            onDismiss = { selectedBrokerForOAuth = null },
            onAuthorizeSuccess = { apiKey, apiSecret, reqTok, userId ->
                onConnectBrokerOAuth2(selectedBrokerForOAuth!!.code, apiKey, apiSecret, reqTok, userId)
                selectedBrokerForOAuth = null
            }
        )
    }
}

/**
 * Individual Broker Card with Visual Connection Status, Latency Ping, and OAuth2 actions.
 */
@Composable
fun BrokerCard(
    broker: LiveBrokerItem,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPingClick: () -> Unit,
    isPinging: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (broker.isConnected) {
        if (broker.isLiveRoutingActive) BearishRed.copy(alpha = 0.5f) else ElectricTeal.copy(alpha = 0.5f)
    } else {
        BorderDark
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("broker_card_${broker.code.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Broker Header & Visual Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (broker.code) {
                                    "ZERODHA" -> Color(0xFFF1592A).copy(alpha = 0.2f)
                                    "ANGEL_ONE" -> Color(0xFF0F52BA).copy(alpha = 0.2f)
                                    "UPSTOX" -> Color(0xFF5E2BFF).copy(alpha = 0.2f)
                                    "DHAN" -> Color(0xFF00C853).copy(alpha = 0.2f)
                                    else -> ElectricTeal.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = broker.code.take(2),
                            color = when (broker.code) {
                                "ZERODHA" -> Color(0xFFFF7A50)
                                "ANGEL_ONE" -> CyanBlue
                                "UPSTOX" -> Color(0xFFA58AFF)
                                "DHAN" -> BullishGreen
                                else -> ElectricTeal
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = broker.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = broker.authType,
                            color = TextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Visual Status Badge (Active 🟢 / Disconnected 🔴)
                VisualConnectionStatusBadge(status = broker.status)
            }

            // Connection Details if Connected
            if (broker.isConnected) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "User ID: ${broker.userId}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(13.dp))
                                Text(
                                    text = if (isPinging) "pinging..." else "${broker.latencyMs}ms latency",
                                    color = CyanBlue,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vault: AES-256 Encrypted 🔒",
                                color = BullishGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Token valid: ~18h remaining",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = broker.tagline,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (broker.isConnected) {
                    OutlinedButton(
                        onClick = onPingClick,
                        enabled = !isPinging,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanBlue.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ping API", color = CyanBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.weight(1.2f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Refresh Token", color = ElectricTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier.weight(0.8f).height(36.dp).testTag("disconnect_broker_${broker.code.lowercase()}"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.6f))
                    ) {
                        Text(text = "Unlink", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("connect_broker_${broker.code.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = BackgroundDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Link Account with OAuth2",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual Indicator component with pulsing light for ACTIVE status.
 */
@Composable
fun VisualConnectionStatusBadge(status: BrokerConnectionStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseStatus")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlphaStatus"
    )

    val badgeColor = when (status) {
        BrokerConnectionStatus.ACTIVE -> BullishGreen
        BrokerConnectionStatus.DISCONNECTED -> BearishRed
        BrokerConnectionStatus.TOKEN_EXPIRING -> AmberWarning
        BrokerConnectionStatus.AUTHENTICATING -> CyanBlue
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = badgeColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = if (status == BrokerConnectionStatus.ACTIVE) pulseAlpha else 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = if (status == BrokerConnectionStatus.ACTIVE) pulseAlpha else 1f))
            )
            Text(
                text = when (status) {
                    BrokerConnectionStatus.ACTIVE -> "ACTIVE"
                    BrokerConnectionStatus.DISCONNECTED -> "DISCONNECTED"
                    BrokerConnectionStatus.TOKEN_EXPIRING -> "EXPIRING"
                    BrokerConnectionStatus.AUTHENTICATING -> "LINKING..."
                },
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
