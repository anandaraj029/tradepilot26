package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.BrokerConnectionEntity
import com.example.data.db.FundTransactionEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZerodhaBrokerDialog(
    broker: BrokerConnectionEntity,
    fundTransactions: List<FundTransactionEntity>,
    isLiveMode: Boolean,
    isAutonomousAi: Boolean,
    onDismiss: () -> Unit,
    onToggleLiveMode: (Boolean) -> Unit,
    onToggleAutonomousAi: (Boolean) -> Unit,
    onConnectZerodha: (apiKey: String, apiSecret: String, requestToken: String, userId: String) -> Unit,
    onDisconnectZerodha: () -> Unit,
    onOpenAddFunds: () -> Unit,
    onOpenWithdrawFunds: () -> Unit
) {
    var apiKey by remember { mutableStateOf(broker.apiKey.ifEmpty { "kite_prod_live_key_994" }) }
    var apiSecret by remember { mutableStateOf(broker.apiSecret.ifEmpty { "kite_secret_prod_x9" }) }
    var requestToken by remember { mutableStateOf(broker.requestToken.ifEmpty { "req_tok_99184" }) }
    var userId by remember { mutableStateOf(broker.userId.ifEmpty { "ZL9824" }) }
    var isEditingKeys by remember { mutableStateOf(!broker.isConnected) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("zerodha_broker_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.5.dp, if (isLiveMode) BearishRed.copy(alpha = 0.8f) else ElectricTeal.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isLiveMode) BearishRed.copy(alpha = 0.2f) else ElectricTeal.copy(alpha = 0.2f))
                                .border(1.dp, if (isLiveMode) BearishRed else ElectricTeal, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLiveMode) Icons.Default.Bolt else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (isLiveMode) BearishRed else ElectricTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Zerodha Kite Connect v3",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (broker.isConnected) BullishGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, if (broker.isConnected) BullishGreen else TextMuted)
                                ) {
                                    Text(
                                        text = if (broker.isConnected) "LINKED • 18ms" else "DISCONNECTED",
                                        color = if (broker.isConnected) BullishGreen else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "NSE / BSE Live Broker Integration & Capital Gateway",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. LIVE VS PAPER MODE SWITCHER
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            border = BorderStroke(1.dp, if (isLiveMode) BearishRed.copy(alpha = 0.6f) else BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isLiveMode) BearishRed else CyanBlue)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isLiveMode) "🔴 LIVE TRADING MODE (REAL CAPITAL)" else "📄 PAPER TRADING MODE (VIRTUAL)",
                                                color = if (isLiveMode) BearishRed else CyanBlue,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isLiveMode)
                                                "Active orders and Auto-Pilot bot route real trades directly to Zerodha Kite on NSE/BSE."
                                            else
                                                "Orders execute in simulated sandbox using ₹25,000 virtual capital without real financial risk.",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = isLiveMode,
                                        onCheckedChange = { onToggleLiveMode(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = BearishRed,
                                            uncheckedThumbColor = ElectricTeal,
                                            uncheckedTrackColor = SurfaceDark
                                        ),
                                        modifier = Modifier.testTag("toggle_live_trading_switch")
                                    )
                                }
                            }
                        }
                    }

                    // 2. AI AUTONOMOUS RESPONSIBILITY BANNER & TOGGLE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isAutonomousAi) ElectricTeal.copy(alpha = 0.08f) else SurfaceElevated),
                            border = BorderStroke(1.dp, if (isAutonomousAi) ElectricTeal.copy(alpha = 0.6f) else BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "AI Full Autonomous Responsibility",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "AI takes full authority for market scanning, entry timing, trailing stop-loss, and daily ₹1,000 profit lock.",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isAutonomousAi,
                                        onCheckedChange = { onToggleAutonomousAi(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BackgroundDark,
                                            checkedTrackColor = ElectricTeal,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = SurfaceDark
                                        ),
                                        modifier = Modifier.testTag("toggle_autonomous_ai_switch")
                                    )
                                }
                            }
                        }
                    }

                    // 3. LIVE BROKER FUNDS & MARGIN CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ZERODHA LIVE MARGIN (AVAILABLE CASH)",
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "₹${"%,d".format(broker.liveEquityMargin.toInt())}",
                                            color = BullishGreen,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = onOpenAddFunds,
                                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("add_live_money_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Money", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = onOpenWithdrawFunds,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, BorderDark),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Withdraw", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BorderDark.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Used Margin", color = TextMuted, fontSize = 10.sp)
                                        Text("₹${"%,d".format(broker.usedMargin.toInt())}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Collateral Margin", color = TextMuted, fontSize = 10.sp)
                                        Text("₹${"%,d".format(broker.availableCollateral.toInt())}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Kite Client ID", color = TextMuted, fontSize = 10.sp)
                                        Text(broker.userId, color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. BROKER CREDENTIALS & CONNECTION CONFIG
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "API Configuration",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    TextButton(
                                        onClick = { isEditingKeys = !isEditingKeys },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (isEditingKeys) "Cancel" else "Edit Credentials", color = ElectricTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isEditingKeys) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = userId,
                                        onValueChange = { userId = it },
                                        label = { Text("Zerodha User ID / Client Code") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { apiKey = it },
                                        label = { Text("Kite Connect API Key") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = apiSecret,
                                        onValueChange = { apiSecret = it },
                                        label = { Text("Kite API Secret") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            onConnectZerodha(apiKey, apiSecret, requestToken, userId)
                                            isEditingKeys = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save & Connect Zerodha API", color = BackgroundDark, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("API Key", color = TextMuted, fontSize = 11.sp)
                                            Text(broker.apiKey.take(8) + "••••••••", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Client ID", color = TextMuted, fontSize = 11.sp)
                                            Text(broker.userId, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Segment", color = TextMuted, fontSize = 11.sp)
                                            Text("NSE_EQ • BSE_EQ", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. FUND TRANSACTION LEDGER (DEPOSIT HISTORY)
                    item {
                        Text(
                            text = "Recent Fund Additions & Margin Credits",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (fundTransactions.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text("No fund transfers recorded yet. Click 'Add Money' above.", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(fundTransactions.take(5)) { tx ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (tx.type == "DEPOSIT") BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (tx.type == "DEPOSIT") BullishGreen else BearishRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (tx.type == "DEPOSIT") "Margin Added (${tx.paymentMethod})" else "Payout to Bank",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "UTR: ${tx.utrNumber} • ${dateFormat.format(Date(tx.timestamp))}",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (tx.type == "DEPOSIT") "+" else "-"}₹${"%,d".format(tx.amount.toInt())}",
                                            color = if (tx.type == "DEPOSIT") BullishGreen else BearishRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BullishGreen.copy(alpha = 0.12f)
                                        ) {
                                            Text("CREDITED", color = BullishGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
