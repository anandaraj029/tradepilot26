package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.CustomStrategyEntity
import com.example.data.model.StrategyModel
import com.example.ui.components.FintechCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState

/**
 * StrategyLab composable that lists active paper trading strategies and provides
 * a form to initiate new strategies with configurable risk parameters.
 */
@Composable
fun StrategyLab(
    uiState: TradePilotUiState,
    onGenerateStrategy: (String) -> Unit = {},
    onCreatePaperStrategy: (String, Double, Double, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteCustomStrategy: (Long) -> Unit = {},
    onNavigateToBacktest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isNewStrategyFormExpanded by remember { mutableStateOf(false) }
    var selectedStrategyFilter by remember { mutableStateOf("All Active") } // "All Active", "AI Council", "Custom"

    // Form State for Initiating New Strategy with Configurable Risk Parameters
    var newStrategyName by remember { mutableStateOf("") }
    var newMarket by remember { mutableStateOf("India 🇮🇳 (NSE/BSE)") }
    var newAllocationAmount by remember { mutableStateOf("5000") }
    var newMaxRiskPercent by remember { mutableDoubleStateOf(1.0) }
    var newStopLossPercent by remember { mutableDoubleStateOf(1.5) }
    var newTakeProfitPercent by remember { mutableDoubleStateOf(3.0) }
    var newEntryRules by remember { mutableStateOf("RSI < 35 oversold bounce + 50-EMA support confirmation") }
    var newExitRules by remember { mutableStateOf("2R target hit or close below stop-loss boundary") }
    var newAiDeliberationMode by remember { mutableStateOf("Full Multi-AI Council") }

    // Natural language prompt state
    var naturalLanguagePrompt by remember { mutableStateOf("") }

    val samplePrompts = listOf(
        "Low-risk swing strategy for ₹25,000",
        "High-volume breakout trend pilot",
        "Statistical mean reversion RSI guard"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("strategy_lab_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // --- 1. HERO BANNER ---
        item {
            FintechCard(
                borderColor = NeonPurple.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Strategy Lab & Paper Incubator",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Paper Simulation & Configurable Risk Sandbox",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = "${uiState.standardStrategies.size + uiState.customStrategies.size} Models",
                            color = CyanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "All strategies operate in isolated Paper Simulation. Strategies are strictly governed by the Risk Engine and must observe portfolio risk capping.",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // --- 1.5. BACKTEST SIMULATOR LAUNCH CARD ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToBacktest() }
                    .testTag("launch_backtest_banner"),
                color = SurfaceElevated,
                border = BorderStroke(1.2.dp, ElectricTeal.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = ElectricTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Historical Backtest Simulator",
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simulate strategy win rates over 90D/180D OHLCV data",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricTeal
                    ) {
                        Text(
                            text = "Simulate",
                            color = BackgroundDark,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // --- 2. INITIATE NEW STRATEGY FORM (WITH CONFIGURABLE RISK PARAMETERS) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("initiate_strategy_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.5.dp, if (isNewStrategyFormExpanded) ElectricTeal else BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNewStrategyFormExpanded = !isNewStrategyFormExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricTeal.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Addchart,
                                        contentDescription = null,
                                        tint = ElectricTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Initiate New Paper Strategy",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Define execution rules & configure deterministic risk bounds",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { isNewStrategyFormExpanded = !isNewStrategyFormExpanded },
                            modifier = Modifier.testTag("toggle_strategy_form_button")
                        ) {
                            Icon(
                                imageVector = if (isNewStrategyFormExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isNewStrategyFormExpanded) "Collapse" else "Expand",
                                tint = ElectricTeal
                            )
                        }
                    }

                    AnimatedVisibility(visible = isNewStrategyFormExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Strategy Name
                            OutlinedTextField(
                                value = newStrategyName,
                                onValueChange = { newStrategyName = it },
                                label = { Text("Strategy Name *", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("e.g. Nifty Volatility Breakout", color = TextMuted, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("strategy_name_input"),
                                shape = RoundedCornerShape(10.dp),
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

                            // Market Universe Selector
                            Text("Target Market Universe", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("India 🇮🇳 (NSE/BSE)", "USA 🇺🇸 (NYSE/NASDAQ)", "Cross-Market").forEach { mkt ->
                                    val isSelected = newMarket == mkt
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { newMarket = mkt },
                                        color = if (isSelected) CyanBlue.copy(alpha = 0.2f) else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isSelected) CyanBlue else BorderDark)
                                    ) {
                                        Text(
                                            text = mkt,
                                            color = if (isSelected) CyanBlue else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Capital Allocation (₹)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Capital Allocation", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("₹$newAllocationAmount", color = ElectricTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("2500", "5000", "10000", "25000").forEach { amount ->
                                        val isSel = newAllocationAmount == amount
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { newAllocationAmount = amount },
                                            color = if (isSel) ElectricTeal.copy(alpha = 0.2f) else SurfaceDark,
                                            border = BorderStroke(1.dp, if (isSel) ElectricTeal else BorderDark)
                                        ) {
                                            Text(
                                                text = "₹$amount",
                                                color = if (isSel) ElectricTeal else TextMuted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Configurable Risk: Max Risk per Trade %
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Max Risk Per Trade (%)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("$newMaxRiskPercent%", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = newMaxRiskPercent.toFloat(),
                                    onValueChange = { newMaxRiskPercent = kotlin.math.round(it * 10.0) / 10.0 },
                                    valueRange = 0.5f..2.0f,
                                    steps = 2,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BullishGreen,
                                        activeTrackColor = BullishGreen,
                                        inactiveTrackColor = BackgroundDark
                                    ),
                                    modifier = Modifier.testTag("strategy_risk_slider")
                                )
                            }

                            // Risk-to-Reward Ratio Configuration (SL % vs TP %)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Stop Loss: $newStopLossPercent%", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = newStopLossPercent.toFloat(),
                                        onValueChange = { newStopLossPercent = kotlin.math.round(it * 10.0) / 10.0 },
                                        valueRange = 0.5f..4.0f,
                                        colors = SliderDefaults.colors(thumbColor = BearishRed, activeTrackColor = BearishRed)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    val rrRatio = if (newStopLossPercent > 0) kotlin.math.round((newTakeProfitPercent / newStopLossPercent) * 10.0) / 10.0 else 2.0
                                    Text("Take Profit: $newTakeProfitPercent% (1:${rrRatio} R:R)", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = newTakeProfitPercent.toFloat(),
                                        onValueChange = { newTakeProfitPercent = kotlin.math.round(it * 10.0) / 10.0 },
                                        valueRange = 1.0f..8.0f,
                                        colors = SliderDefaults.colors(thumbColor = BullishGreen, activeTrackColor = BullishGreen)
                                    )
                                }
                            }

                            // Entry Trigger Rules
                            OutlinedTextField(
                                value = newEntryRules,
                                onValueChange = { newEntryRules = it },
                                label = { Text("Entry Rules & Indicators *", color = TextSecondary, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("strategy_entry_rules_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark,
                                    focusedBorderColor = ElectricTeal,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                maxLines = 2
                            )

                            // Exit Criteria
                            OutlinedTextField(
                                value = newExitRules,
                                onValueChange = { newExitRules = it },
                                label = { Text("Exit Criteria & Stop Conditions *", color = TextSecondary, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("strategy_exit_rules_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark,
                                    focusedBorderColor = ElectricTeal,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                maxLines = 2
                            )

                            // Deploy Button
                            Button(
                                onClick = {
                                    if (newStrategyName.isNotBlank()) {
                                        onCreatePaperStrategy(
                                            newStrategyName,
                                            newAllocationAmount.toDoubleOrNull() ?: 5000.0,
                                            newMaxRiskPercent,
                                            newEntryRules,
                                            newExitRules,
                                            "Target $newMarket, R:R 1:${kotlin.math.round((newTakeProfitPercent / newStopLossPercent) * 10.0) / 10.0}"
                                        )
                                        newStrategyName = ""
                                        isNewStrategyFormExpanded = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("deploy_strategy_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                                enabled = newStrategyName.isNotBlank()
                            ) {
                                Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Initiate & Deploy Paper Strategy",
                                    color = BackgroundDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. NATURAL LANGUAGE AI BUILDER (QUICK GENERATOR) ---
        item {
            FintechCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Quick AI Strategy Synthesizer", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Prompt the Multi-AI Council to synthesize strategy parameters:", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = naturalLanguagePrompt,
                    onValueChange = { naturalLanguagePrompt = it },
                    placeholder = { Text("e.g. Create a low-risk swing strategy for ₹25,000...", color = TextMuted, fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("strategy_prompt_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = ElectricTeal,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(samplePrompts) { prompt ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { naturalLanguagePrompt = prompt },
                            color = SurfaceElevated
                        ) {
                            Text(text = prompt, color = CyanBlue, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (naturalLanguagePrompt.isNotBlank()) {
                            onGenerateStrategy(naturalLanguagePrompt)
                            naturalLanguagePrompt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("generate_strategy_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBlue),
                    enabled = naturalLanguagePrompt.isNotBlank()
                ) {
                    Text("Synthesize via AI Council", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- 4. ACTIVE PAPER TRADING STRATEGIES LIST ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Paper Strategies",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Filter Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All Active", "Core AI", "Custom").forEach { tab ->
                        val isSel = selectedStrategyFilter == tab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) ElectricTeal.copy(alpha = 0.2f) else SurfaceElevated,
                            modifier = Modifier.clickable { selectedStrategyFilter = tab }
                        ) {
                            Text(
                                text = tab,
                                color = if (isSel) ElectricTeal else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Initiated Strategies
        if (selectedStrategyFilter != "Core AI") {
            items(uiState.customStrategies, key = { "custom_${it.id}" }) { custom ->
                CustomStrategyCard(
                    custom = custom,
                    onDelete = { onDeleteCustomStrategy(custom.id) }
                )
            }
        }

        // Prebuilt Standard Core Strategies
        if (selectedStrategyFilter != "Custom") {
            items(uiState.standardStrategies, key = { "standard_${it.id}" }) { strat ->
                StandardStrategyCard(strategy = strat)
            }
        }
    }
}

/**
 * Backward compatibility alias for StrategyLabScreen.
 */
@Composable
fun StrategyLabScreen(
    uiState: TradePilotUiState,
    onGenerateStrategy: (String) -> Unit,
    onCreatePaperStrategy: (String, Double, Double, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteCustomStrategy: (Long) -> Unit = {},
    onNavigateToBacktest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    StrategyLab(
        uiState = uiState,
        onGenerateStrategy = onGenerateStrategy,
        onCreatePaperStrategy = onCreatePaperStrategy,
        onDeleteCustomStrategy = onDeleteCustomStrategy,
        onNavigateToBacktest = onNavigateToBacktest,
        modifier = modifier
    )
}

@Composable
fun StandardStrategyCard(strategy: StrategyModel) {
    FintechCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strategy.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strategy.category,
                    color = CyanBlue,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BullishGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BullishGreen))
                    Text(
                        text = "PAPER ACTIVE",
                        color = BullishGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strategy.description,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StratStat(label = "Win Rate", value = "${strategy.winRate}%", color = BullishGreen)
            StratStat(label = "Profit Factor", value = strategy.profitFactor.toString(), color = ElectricTeal)
            StratStat(label = "Max Drawdown", value = "-${strategy.maxDrawdown}%", color = AmberWarning)
            StratStat(label = "Paper Trades", value = strategy.tradesCount.toString(), color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SurfaceElevated
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Trigger: ${strategy.entryRules}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Exit: ${strategy.exitRules}",
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
fun CustomStrategyCard(
    custom: CustomStrategyEntity,
    onDelete: () -> Unit = {}
) {
    FintechCard(
        borderColor = ElectricTeal.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = custom.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = custom.prompt,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BullishGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BullishGreen.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "ACTIVE PAPER",
                        color = BullishGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Delete Strategy", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StratStat(label = "Allocation", value = "₹${custom.capitalAllocation.toInt()}", color = TextPrimary)
            StratStat(label = "Risk / Trade", value = "${custom.maxRiskPercent}%", color = BullishGreen)
            StratStat(label = "Mode", value = "PAPER ONLY", color = CyanBlue)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Entry: ${custom.entryCriteria}",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = "Exit: ${custom.exitCriteria}",
            color = TextMuted,
            fontSize = 10.5.sp
        )
    }
}

@Composable
fun StratStat(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 9.5.sp)
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
