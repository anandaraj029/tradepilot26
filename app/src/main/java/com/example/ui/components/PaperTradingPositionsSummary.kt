package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PositionEntity
import com.example.data.repository.MockPaperTradingRepository
import com.example.ui.theme.*

/**
 * Main Dashboard UI Component: Paper Trading Summary & Positions Tracker
 *
 * Displays:
 * 1. Available Virtual Balance & Margin Allocation Breakdown
 * 2. Real-Time Net Unrealized P&L Tracker & Daily Target Progress Bar (₹1,000 Target)
 * 3. List of Active Open Paper Positions across Indian & US Equities
 * 4. 1-Tap Position Close Action with Realized P&L Calculation
 * 5. Standalone preview / testing support via [MockPaperTradingRepository].
 */
@Composable
fun PaperTradingPositionsSummary(
    mockRepository: MockPaperTradingRepository = remember { MockPaperTradingRepository() },
    onSelectPosition: ((PositionEntity) -> Unit)? = null,
    onNavigateToScanner: (() -> Unit)? = null,
    onNavigateToRiskCenter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val portfolioState by mockRepository.portfolio.collectAsState(initial = mockRepository.getCurrentPortfolio())
    val positionsState by mockRepository.positions.collectAsState(initial = mockRepository.getCurrentPositions())

    PaperTradingPositionsSummary(
        positions = positionsState,
        availableCash = portfolioState.availableCash,
        startingCapital = portfolioState.startingCapital,
        dailyTarget = portfolioState.dailyTarget,
        realizedPnlToday = portfolioState.realizedPnlToday,
        usdInrRate = portfolioState.usdInrRate,
        onClosePosition = { position, _ ->
            mockRepository.closePosition(position.id)
        },
        onResetCapital = {
            mockRepository.resetPortfolio()
        },
        onSelectPosition = onSelectPosition,
        onNavigateToScanner = onNavigateToScanner,
        onNavigateToRiskCenter = onNavigateToRiskCenter,
        modifier = modifier
    )
}

/**
 * Core Composable accepting explicit state parameters for integration into the main DashboardScreen
 */
@Composable
fun PaperTradingPositionsSummary(
    positions: List<PositionEntity>,
    availableCash: Double,
    startingCapital: Double,
    dailyTarget: Double = 1000.0,
    realizedPnlToday: Double = 0.0,
    usdInrRate: Double = 83.5,
    onClosePosition: (PositionEntity, Double) -> Unit,
    onResetCapital: (() -> Unit)? = null,
    onSelectPosition: ((PositionEntity) -> Unit)? = null,
    onNavigateToScanner: (() -> Unit)? = null,
    onNavigateToRiskCenter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Computations
    val investedValueInr = remember(positions, usdInrRate) {
        positions.sumOf { pos ->
            val mult = if (pos.market == "USA") usdInrRate else 1.0
            pos.quantity * pos.buyPrice * mult
        }
    }
    val currentPositionsValueInr = remember(positions, usdInrRate) {
        positions.sumOf { pos ->
            val mult = if (pos.market == "USA") usdInrRate else 1.0
            pos.quantity * pos.currentPrice * mult
        }
    }
    val unrealizedPnlInr = currentPositionsValueInr - investedValueInr
    val totalEquityInr = availableCash + currentPositionsValueInr
    val targetPercent = ((realizedPnlToday / dailyTarget) * 100.0).coerceIn(0.0, 100.0).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("paper_trading_positions_summary_component"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. PAPER TRADING BALANCE & MARGIN METRICS CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("paper_balance_hero_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(BorderDark, ElectricTeal.copy(alpha = 0.35f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceElevated.copy(alpha = 0.65f), SurfaceDark)
                        )
                    )
                    .padding(18.dp)
            ) {
                // Header badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricTeal.copy(alpha = 0.15f))
                                .border(1.dp, ElectricTeal.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = ElectricTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "PAPER TRADING ACCOUNT",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyanBlue.copy(alpha = 0.18f),
                                    border = BorderStroke(0.5.dp, CyanBlue)
                                ) {
                                    Text(
                                        text = "VIRTUAL SIMULATION",
                                        color = CyanBlue,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Zero-Risk Algorithm Execution",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (onResetCapital != null) {
                        IconButton(
                            onClick = onResetCapital,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("reset_paper_capital_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Capital to ₹25,000",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Value & Unrealized P&L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Total Simulated Equity",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${"%,d".format(totalEquityInr.toInt())}",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("paper_total_equity_text")
                        )
                    }

                    // Live Unrealized P&L badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (unrealizedPnlInr >= 0) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (unrealizedPnlInr >= 0) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Unrealized P&L",
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(
                                    imageVector = if (unrealizedPnlInr >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (unrealizedPnlInr >= 0) BullishGreen else BearishRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                val unrealizedPct = if (investedValueInr > 0) (unrealizedPnlInr / investedValueInr) * 100.0 else 0.0
                                Text(
                                    text = "${if (unrealizedPnlInr >= 0) "+₹" else "-₹"}${Math.abs(unrealizedPnlInr).toInt()} (${if (unrealizedPct >= 0) "+" else ""}${"%.1f".format(unrealizedPct)}%)",
                                    color = if (unrealizedPnlInr >= 0) BullishGreen else BearishRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderDark.copy(alpha = 0.7f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown: Available Cash vs Invested Margin vs Realized Today
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Available Balance
                    Column {
                        Text(
                            text = "AVAILABLE CASH",
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${"%,d".format(availableCash.toInt())}",
                            color = ElectricTeal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("paper_available_cash_text")
                        )
                    }

                    // Invested in Positions
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "INVESTED MARGIN",
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${"%,d".format(investedValueInr.toInt())}",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("paper_invested_margin_text")
                        )
                    }

                    // Today's Realized Gain
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TODAY'S REALIZED",
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (realizedPnlToday >= 0) "+₹" else "-₹"}${Math.abs(realizedPnlToday).toInt()}",
                            color = if (realizedPnlToday >= 0) BullishGreen else BearishRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("paper_realized_pnl_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily ₹1,000 Target Progress Indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Daily Target: ₹${"%,d".format(dailyTarget.toInt())}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (realizedPnlToday >= dailyTarget) "100% ACHIEVED (LOCKED)" else "$targetPercent% (${if (realizedPnlToday >= 0) "₹" else "-₹"}${Math.abs(realizedPnlToday).toInt()}/₹${dailyTarget.toInt()})",
                            color = if (realizedPnlToday >= dailyTarget) BullishGreen else AmberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (realizedPnlToday / dailyTarget).coerceIn(0.0, 1.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (realizedPnlToday >= dailyTarget) BullishGreen else ElectricTeal,
                        trackColor = BorderDark,
                    )
                }
            }
        }

        // --- 2. ACTIVE OPEN PAPER POSITIONS HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Open Paper Positions",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Text(
                        text = "${positions.size} Active",
                        color = ElectricTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (onNavigateToRiskCenter != null) {
                TextButton(
                    onClick = onNavigateToRiskCenter,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = CyanBlue, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Risk Limits", color = CyanBlue, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- 3. POSITIONS LIST / EMPTY STATE ---
        if (positions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paper_positions_empty_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricTeal.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "No Active Paper Positions",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "100% of your ₹${"%,d".format(availableCash.toInt())} virtual balance is free in cash reserve. Scan assets or let Auto-Pilot deploy trades.",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    if (onNavigateToScanner != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToScanner,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan 1,020 Assets for Setups", color = BackgroundDark, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                positions.forEach { position ->
                    PaperPositionItemCard(
                        position = position,
                        usdInrRate = usdInrRate,
                        onClose = { onClosePosition(position, position.currentPrice) },
                        onSelect = { onSelectPosition?.invoke(position) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Paper Trading Position Card
 */
@Composable
private fun PaperPositionItemCard(
    position: PositionEntity,
    usdInrRate: Double,
    onClose: () -> Unit,
    onSelect: () -> Unit
) {
    val mult = if (position.market == "USA") usdInrRate else 1.0
    val currencySym = if (position.market == "USA") "$" else "₹"
    val investedInr = position.quantity * position.buyPrice * mult
    val currentValInr = position.quantity * position.currentPrice * mult
    val pnlInr = currentValInr - investedInr
    val pnlPercent = if (position.buyPrice > 0) ((position.currentPrice - position.buyPrice) / position.buyPrice) * 100.0 else 0.0
    val isProfit = pnlInr >= 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (isProfit) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("paper_position_item_${position.symbol}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Symbol, Market Pill, Quantity & P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (position.market == "USA") CyanBlue.copy(alpha = 0.15f) else ElectricTeal.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, if (position.market == "USA") CyanBlue else ElectricTeal)
                    ) {
                        Text(
                            text = if (position.market == "USA") "🇺🇸 US" else "🇮🇳 NSE",
                            color = if (position.market == "USA") CyanBlue else ElectricTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = position.symbol,
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${position.quantity} Qty • ${position.companyName.take(18)}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Unrealized P&L badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isProfit) BullishGreen.copy(alpha = 0.18f) else BearishRed.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isProfit) BullishGreen else BearishRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${if (isProfit) "+₹" else "-₹"}${Math.abs(pnlInr).toInt()} (${if (pnlPercent >= 0) "+" else ""}${"%.1f".format(pnlPercent)}%)",
                            color = if (isProfit) BullishGreen else BearishRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Price stats breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Avg Buy Price", color = TextMuted, fontSize = 9.5.sp)
                    Text("$currencySym${"%.2f".format(position.buyPrice)}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CMP (Live)", color = TextMuted, fontSize = 9.5.sp)
                    Text("$currencySym${"%.2f".format(position.currentPrice)}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Position Value", color = TextMuted, fontSize = 9.5.sp)
                    Text("₹${"%,d".format(currentValInr.toInt())}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Stop-loss & Take-profit target indicators + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BearishRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, BearishRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "SL: $currencySym${"%.1f".format(position.stopLoss)}",
                            color = BearishRed,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BullishGreen.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, BullishGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "TP: $currencySym${"%.1f".format(position.takeProfit)}",
                            color = BullishGreen,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // 1-Tap Close Position Button
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isProfit) BullishGreen.copy(alpha = 0.12f) else BearishRed.copy(alpha = 0.12f),
                        contentColor = if (isProfit) BullishGreen else BearishRed
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isProfit) BullishGreen.copy(alpha = 0.6f) else BearishRed.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("close_paper_position_button_${position.symbol}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Position",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Book Exit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PaperTradingPositionsSummaryPreview() {
    MyApplicationTheme {
        PaperTradingPositionsSummary(
            mockRepository = MockPaperTradingRepository()
        )
    }
}
