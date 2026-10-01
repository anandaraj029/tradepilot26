package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.window.Dialog
import com.example.data.db.BrokerConnectionEntity
import com.example.data.db.PortfolioEntity
import com.example.data.engine.ZerodhaKiteService
import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import com.example.ui.theme.*

@Composable
fun PaperTradeDialog(
    stock: StockQuote,
    portfolio: PortfolioEntity,
    broker: BrokerConnectionEntity = BrokerConnectionEntity(),
    isLiveModeDefault: Boolean = false,
    onDismiss: () -> Unit,
    onExecuteOrder: (quantity: Int, stopLoss: Double, takeProfit: Double, isLive: Boolean, product: String) -> Unit
) {
    val usdRate = portfolio.usdInrRate
    val priceInInr = if (stock.market == MarketType.USA) stock.currentPrice * usdRate else stock.currentPrice

    val defaultSL = Math.round((stock.currentPrice * 0.985) * 100.0) / 100.0
    val defaultTP = Math.round((stock.currentPrice * 1.035) * 100.0) / 100.0

    var isLiveOrder by remember { mutableStateOf(isLiveModeDefault || broker.isLiveTradingActive) }
    var selectedProduct by remember { mutableStateOf("MIS") } // "MIS" (Intraday) vs "CNC" (Delivery)
    var quantity by remember { mutableIntStateOf(1) }
    var stopLossPriceText by remember { mutableStateOf(defaultSL.toString()) }
    var takeProfitPriceText by remember { mutableStateOf(defaultTP.toString()) }

    val totalInvestmentInr = quantity * priceInInr
    val requiredMarginInr = if (isLiveOrder && selectedProduct == "MIS") totalInvestmentInr * 0.20 else totalInvestmentInr

    val availableCapital = if (isLiveOrder) broker.liveEquityMargin else portfolio.availableCash
    val max1PercentRiskRupees = portfolio.startingCapital * (portfolio.maxRiskPerTradePercent / 100.0) // ₹250 default

    val currentStopLoss = stopLossPriceText.toDoubleOrNull() ?: defaultSL
    val lossPerShareInr = (stock.currentPrice - currentStopLoss) * (if (stock.market == MarketType.USA) usdRate else 1.0)
    val calculatedRiskRupees = (lossPerShareInr * quantity).coerceAtLeast(0.0)

    val isRiskExceeded = calculatedRiskRupees > (max1PercentRiskRupees * 1.15)
    val isCashExceeded = requiredMarginInr > availableCapital

    val charges = remember(stock.currentPrice, quantity, selectedProduct) {
        ZerodhaKiteService.calculateCharges(
            buyPrice = stock.currentPrice,
            sellPrice = stock.currentPrice * 1.02,
            quantity = quantity,
            isDelivery = selectedProduct == "CNC"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("paper_trade_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.5.dp, if (isLiveOrder) BearishRed.copy(alpha = 0.7f) else ElectricTeal.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLiveOrder) "🔴 Zerodha Live Order" else "📄 Paper Trade Order",
                                color = if (isLiveOrder) BearishRed else TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "${stock.symbol} • ${stock.companyName}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Execution Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isLiveOrder) ElectricTeal else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isLiveOrder = false }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "📄 Paper Mode",
                                color = if (!isLiveOrder) BackgroundDark else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLiveOrder) BearishRed else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isLiveOrder = true }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "🔴 Live Zerodha Kite",
                                color = if (isLiveOrder) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Type (MIS vs CNC) for Indian Equities
                if (stock.market == MarketType.INDIA) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedProduct == "MIS") SurfaceElevated else SurfaceDark,
                            border = BorderStroke(1.dp, if (selectedProduct == "MIS") ElectricTeal else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedProduct = "MIS" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("MIS (Intraday)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("5x Margin • Auto Squareoff", color = TextMuted, fontSize = 9.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedProduct == "CNC") SurfaceElevated else SurfaceDark,
                            border = BorderStroke(1.dp, if (selectedProduct == "CNC") ElectricTeal else BorderDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedProduct = "CNC" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("CNC (Delivery)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("1x Margin • Zero Brokerage", color = TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Price & Margin Summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Market Price", color = TextMuted, fontSize = 10.5.sp)
                            Text(
                                text = "${stock.market.currencySymbol}${stock.currentPrice}",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = if (isLiveOrder) "Zerodha Live Margin" else "Virtual Cash Available", color = TextMuted, fontSize = 10.5.sp)
                            Text(
                                text = "₹${"%,d".format(availableCapital.toInt())}",
                                color = if (isLiveOrder) BullishGreen else ElectricTeal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Quantity", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = quantity.toString(),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = { quantity++ },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stop Loss & Take Profit Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossPriceText,
                        onValueChange = { stopLossPriceText = it },
                        label = { Text("Stop Loss (${stock.market.currencySymbol})", color = TextMuted, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = takeProfitPriceText,
                        onValueChange = { takeProfitPriceText = it },
                        label = { Text("Take Profit (${stock.market.currencySymbol})", color = TextMuted, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculated Investment & Risk Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRiskExceeded || isCashExceeded) BearishRed.copy(alpha = 0.15f) else SurfaceElevated,
                    border = BorderStroke(1.dp, if (isRiskExceeded || isCashExceeded) BearishRed.copy(alpha = 0.5f) else BorderDark)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (selectedProduct == "MIS") "Required Margin (5x MIS):" else "Total Capital Needed:", color = TextMuted, fontSize = 11.sp)
                            Text(text = "₹${"%,d".format(requiredMarginInr.toInt())}", color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        if (isLiveOrder) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Zerodha Brokerage + Taxes:", color = TextMuted, fontSize = 10.5.sp)
                                Text(text = "₹${charges.totalTaxAndCharges} (₹${charges.brokerage} brokrg)", color = CyanBlue, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Estimated Risk at Stop Loss:", color = TextMuted, fontSize = 10.5.sp)
                            Text(
                                text = "₹${calculatedRiskRupees.toInt()} (Max 1%: ₹${max1PercentRiskRupees.toInt()})",
                                color = if (isRiskExceeded) BearishRed else BullishGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isCashExceeded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Required margin exceeds available funds (₹${availableCapital.toInt()})",
                                color = BearishRed,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Order Button
                Button(
                    onClick = {
                        val sl = stopLossPriceText.toDoubleOrNull() ?: defaultSL
                        val tp = takeProfitPriceText.toDoubleOrNull() ?: defaultTP
                        onExecuteOrder(quantity, sl, tp, isLiveOrder, selectedProduct)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("submit_paper_order_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCashExceeded) SurfaceElevated else if (isLiveOrder) BearishRed else ElectricTeal
                    ),
                    enabled = !isCashExceeded
                ) {
                    Text(
                        text = if (isLiveOrder) "🚀 Route Live Order to Zerodha Kite" else "Submit Paper Buy Order",
                        color = if (isCashExceeded) TextMuted else if (isLiveOrder) Color.White else BackgroundDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
