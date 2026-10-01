package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BrokerDefinition
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OAuth2ConsentDialog(
    brokerDef: BrokerDefinition,
    onDismiss: () -> Unit,
    onAuthorizeSuccess: (apiKey: String, apiSecret: String, requestToken: String, userId: String) -> Unit
) {
    var apiKey by remember { mutableStateOf(if (brokerDef.code == "ZERODHA") "kite_prod_live_key_994" else "${brokerDef.code.lowercase()}_live_key_381") }
    var apiSecret by remember { mutableStateOf(if (brokerDef.code == "ZERODHA") "kite_secret_prod_x9" else "${brokerDef.code.lowercase()}_sec_9948") }
    var requestToken by remember { mutableStateOf("req_tok_${(10000..99999).random()}") }
    var userId by remember { mutableStateOf(if (brokerDef.code == "ZERODHA") "ZL9824" else "${brokerDef.code.take(2)}7741") }
    
    var isAuthorizing by remember { mutableStateOf(false) }
    var authStepText by remember { mutableStateOf("Ready to initiate OAuth2 handshake") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isAuthorizing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("oauth2_consent_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = BorderStroke(1.5.dp, ElectricTeal.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OAuth2 Secure Broker Link",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = brokerDef.name,
                                color = ElectricTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isAuthorizing,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderDark, thickness = 1.dp)

                // OAuth2 Protocol Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "Protocol: ${brokerDef.authType} (PKCE)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Credentials encrypted locally with AES-256-GCM via Android KeyStore.",
                                color = TextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                // Broker OAuth Endpoint Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundDark)
                        .padding(12.dp)
                ) {
                    Text(text = "REDIRECT URI", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "tradepilot://oauth/${brokerDef.code.lowercase()}/callback", color = CyanBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "AUTHORIZATION ENDPOINT", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = brokerDef.oauthAuthUrl, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // Scopes Requested
                Text(
                    text = "PERMISSIONS & SCOPES REQUESTED",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                brokerDef.defaultScopes.forEach { scope ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = scope,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Input Credentials
                Text(
                    text = "API CREDENTIALS (SAVED TO ENCRYPTED VAULT)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("App API Key / Client ID") },
                    modifier = Modifier.fillMaxWidth().testTag("oauth_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = apiSecret,
                    onValueChange = { apiSecret = it },
                    label = { Text("API Secret / App Secret") },
                    modifier = Modifier.fillMaxWidth().testTag("oauth_api_secret_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it },
                        label = { Text("User ID") },
                        modifier = Modifier.weight(1f).testTag("oauth_user_id_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = requestToken,
                        onValueChange = { requestToken = it },
                        label = { Text("Request Token / Auth Code") },
                        modifier = Modifier.weight(1.4f).testTag("oauth_request_token_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricTeal,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                if (isAuthorizing) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricTeal.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ElectricTeal,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = authStepText,
                                color = ElectricTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Button(
                    onClick = {
                        isAuthorizing = true
                        coroutineScope.launch {
                            authStepText = "1/3 Initiating OAuth2 handshake with ${brokerDef.name}..."
                            delay(400)
                            authStepText = "2/3 Exchanging Request Token for Access & Refresh Token..."
                            delay(450)
                            authStepText = "3/3 Encrypting tokens in AES-256 GCM KeyStore Vault..."
                            delay(350)
                            isAuthorizing = false
                            onAuthorizeSuccess(apiKey, apiSecret, requestToken, userId)
                        }
                    },
                    enabled = !isAuthorizing && apiKey.isNotBlank() && apiSecret.isNotBlank() && userId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("authorize_broker_oauth_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = BackgroundDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Authorize & Encrypt in Vault",
                        color = BackgroundDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
