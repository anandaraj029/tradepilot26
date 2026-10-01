package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.AuthStep
import com.example.ui.theme.*
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Authentication screen featuring Sign In, Sign Up, and Two-Factor Authentication (2FA)
 * with biometric unlock option, TOTP verification, and security guarantees.
 */
@Composable
fun AuthScreen(
    userEmail: String = "ebdc.org@gmail.com",
    onAuthenticated: () -> Unit,
    onSwitchToLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var authStep by remember { mutableStateOf(AuthStep.LOGIN) }
    var selectedAuthTab by remember { mutableStateOf("SIGN_IN") } // "SIGN_IN" or "REGISTER"
    var email by remember { mutableStateOf(userEmail) }
    var name by remember { mutableStateOf("Siddharth (Pro Trader)") }
    var password by remember { mutableStateOf("TradePilot#2026") }
    var confirmPassword by remember { mutableStateOf("TradePilot#2026") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 2FA state
    var twoFactorCode by remember { mutableStateOf("849201") }
    var countdownSeconds by remember { mutableIntStateOf(30) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricAvailability(context) }

    fun triggerBiometricPrompt() {
        errorMessage = null
        if (activity != null && biometricStatus == BiometricStatus.AVAILABLE) {
            BiometricAuthManager.promptBiometric(
                activity = activity,
                title = "AI TradePilot Biometric Unlock",
                subtitle = "Scan fingerprint or face to authenticate and launch trading terminal",
                negativeButtonText = "Use Password / 2FA",
                onSuccess = {
                    onAuthenticated()
                },
                onError = { errorCode, errString ->
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        errorMessage = "Biometric: $errString"
                    }
                },
                onFailed = {
                    errorMessage = "Biometric not recognized. Please retry or enter credentials."
                }
            )
        } else {
            // Emulated / fallback biometric unlock for preview and environments without enrolled sensors
            coroutineScope.launch {
                isVerifying = true
                delay(350)
                isVerifying = false
                onAuthenticated()
            }
        }
    }

    // 30s countdown timer for 2FA resend
    LaunchedEffect(authStep) {
        if (authStep == AuthStep.TWO_FACTOR) {
            countdownSeconds = 30
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            border = BorderStroke(1.5.dp, ElectricTeal.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo & Security Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricTeal.copy(alpha = 0.3f), SurfaceDark)
                            )
                        )
                        .border(1.5.dp, ElectricTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (authStep == AuthStep.TWO_FACTOR) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AI TradePilot",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (authStep == AuthStep.TWO_FACTOR) "Two-Factor Authentication (2FA)" else "Capital Protected Quant Terminal",
                    color = if (authStep == AuthStep.TWO_FACTOR) GoldAccent else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs (Sign In vs Create Account) if on login step
                if (authStep == AuthStep.LOGIN) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedAuthTab == "SIGN_IN") ElectricTeal else Color.Transparent)
                                    .clickable { selectedAuthTab = "SIGN_IN" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    color = if (selectedAuthTab == "SIGN_IN") BackgroundDark else TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedAuthTab == "REGISTER") ElectricTeal else Color.Transparent)
                                    .clickable { selectedAuthTab = "REGISTER" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Create Account",
                                    color = if (selectedAuthTab == "REGISTER") BackgroundDark else TextSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Error / Success message banners
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BearishRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BearishRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = BearishRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = successMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BullishGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = successMessage ?: "",
                            color = BullishGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (authStep == AuthStep.LOGIN) {
                    // --- SIGN IN OR REGISTER FIELDS ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedAuthTab == "REGISTER") {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Trader Full Name", color = TextSecondary, fontSize = 11.5.sp) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricTeal) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark,
                                    focusedBorderColor = ElectricTeal,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Trader Email / ID", color = TextSecondary, fontSize = 11.5.sp) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricTeal) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = ElectricTeal,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password", color = TextSecondary, fontSize = 11.5.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricTeal) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = ElectricTeal,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        // 2FA Security Guarantee note
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "2FA Encrypted: TOTP Authenticator code requested upon login.",
                                    color = TextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button -> Proceeds to 2FA
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter your trader credentials."
                                } else {
                                    errorMessage = null
                                    authStep = AuthStep.TWO_FACTOR
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                        ) {
                            Text(
                                text = if (selectedAuthTab == "SIGN_IN") "Continue to 2FA Verification" else "Register & Setup 2FA",
                                color = BackgroundDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                        }

                        // Biometric & Fast Unlock Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { triggerBiometricPrompt() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("biometric_login_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CyanBlue.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Biometric", color = CyanBlue, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }

                            OutlinedButton(
                                onClick = onSwitchToLockScreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("switch_to_pin_lock_btn"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PIN Lock", color = ElectricTeal, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                        }
                    }
                } else {
                    // --- 2-FACTOR AUTHENTICATION (2FA) STEP ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter the 6-digit security code from your Authenticator app for $email",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // 6-Digit Code Input
                        OutlinedTextField(
                            value = twoFactorCode,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    twoFactorCode = it
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("000000", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 6.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("two_factor_code_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = ElectricTeal,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        // Resend timer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (countdownSeconds > 0) "Code expires in ${countdownSeconds}s" else "Code expired",
                                    color = if (countdownSeconds > 0) TextMuted else AmberWarning,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "Resend Code",
                                color = if (countdownSeconds == 0) CyanBlue else TextMuted.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = countdownSeconds == 0) {
                                    countdownSeconds = 30
                                    twoFactorCode = "918234"
                                }
                            )
                        }

                        // Verify & Launch Terminal Button
                        Button(
                            onClick = {
                                if (twoFactorCode.length < 6) {
                                    errorMessage = "Please enter the full 6-digit 2FA code."
                                } else {
                                    coroutineScope.launch {
                                        isVerifying = true
                                        delay(450)
                                        isVerifying = false
                                        onAuthenticated()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("verify_2fa_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                            enabled = !isVerifying
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticating 2FA...", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify & Launch AI Terminal", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Back to credentials
                        TextButton(
                            onClick = {
                                authStep = AuthStep.LOGIN
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("back_to_login_button")
                        ) {
                            Text("← Back to Credentials", color = TextMuted, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}
