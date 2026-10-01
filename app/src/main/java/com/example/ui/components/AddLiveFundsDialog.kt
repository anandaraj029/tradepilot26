package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddLiveFundsDialog(
    currentMargin: Double,
    onDismiss: () -> Unit,
    onDepositSuccess: (amount: Double, paymentMethod: String, upiId: String) -> Unit
) {
    var amountText by remember { mutableStateOf("10000") }
    var selectedPaymentMethod by remember { mutableStateOf("Instant UPI (Google Pay / PhonePe)") }
    var upiIdText by remember { mutableStateOf("trader@okaxis") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val quickAmounts = listOf(5000, 10000, 25000, 50000, 100000)
    val paymentMethods = listOf(
        "Instant UPI (Google Pay / PhonePe)" to Icons.Default.QrCodeScanner,
        "NetBanking (HDFC / SBI / ICICI)" to Icons.Default.AccountBalance,
        "Instant IMPS Bank Transfer" to Icons.Default.FlashOn
    )

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_funds_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.5.dp, BullishGreen.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BullishGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Add Live Trading Funds", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Zerodha Kite Instant Margin Gateway", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    if (!isProcessing) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Margin Callout
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Zerodha Margin", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("₹${"%,d".format(currentMargin.toInt())}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                Text("Enter Amount to Deposit (₹)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) amountText = it
                    },
                    leadingIcon = { Text("₹", color = BullishGreen, fontWeight = FontWeight.Black, fontSize = 18.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BullishGreen,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Amount Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickAmounts) { amt ->
                        val isSelected = amountText == amt.toString()
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BullishGreen.copy(alpha = 0.2f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) BullishGreen else BorderDark),
                            modifier = Modifier.clickable { amountText = amt.toString() }
                        ) {
                            Text(
                                text = "+₹${"%,d".format(amt)}",
                                color = if (isSelected) BullishGreen else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Selector
                Text("Select Payment Gateway", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paymentMethods.forEach { (method, icon) ->
                        val isSelected = selectedPaymentMethod == method
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ElectricTeal.copy(alpha = 0.12f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = ElectricTeal)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(icon, contentDescription = null, tint = if (isSelected) ElectricTeal else TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = method,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (selectedPaymentMethod.contains("UPI")) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = upiIdText,
                        onValueChange = { upiIdText = it },
                        label = { Text("UPI VPA / Handle") },
                        placeholder = { Text("e.g. mobile@upi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (parsedAmount < 100.0) {
                            errorMessage = "Minimum deposit amount is ₹100."
                            return@Button
                        }
                        isProcessing = true
                        errorMessage = null
                        coroutineScope.launch {
                            delay(600)
                            isProcessing = false
                            onDepositSuccess(parsedAmount, selectedPaymentMethod, upiIdText)
                        }
                    },
                    enabled = !isProcessing && parsedAmount >= 100.0,
                    colors = ButtonDefaults.buttonColors(containerColor = BullishGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_deposit_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying UPI Payment...", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay ₹${"%,d".format(parsedAmount.toInt())} & Credit Margin", color = BackgroundDark, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
