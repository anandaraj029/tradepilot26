package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ChatMessage
import com.example.data.db.PortfolioEntity
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern, User-Friendly Multi-Turn Gemini AI Copilot Screen.
 * Features:
 * - Live Account & AutoPilot Context snapshot banner
 * - Smart Model Selector with capability and latency badges
 * - Categorized Quick-Prompt Filter Chips
 * - Rich Structured AI Message Rendering (Markdown formatting, Key Metric Badges, Code/Action Pills)
 * - One-Tap Message Utilities (Copy to Clipboard, Voice Speech Simulation, Helpful Feedback, Regenerate)
 * - Interactive Direct Action Cards within Copilot responses (Launch AutoPilot, Open Risk Center, Inspect Scanners)
 * - Quick Prompt Library Modal with 15+ curated quantitative setups
 * - Voice Input Simulation & Expandable Input Field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    selectedModel: String,
    portfolio: PortfolioEntity? = null,
    isAutoPilotActive: Boolean = false,
    onSendMessage: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onClearChat: () -> Unit,
    onNavigateTo: ((Screen) -> Unit)? = null,
    onToggleAutoPilot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("All Prompts") }
    var showPromptLibraryDialog by remember { mutableStateOf(false) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var feedbackStateMap by remember { mutableStateOf(mapOf<String, Boolean?>()) } // messageId -> true (up) / false (down)

    // Auto-scroll to bottom on new message or loading state change
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Categories for quick discovery
    val categories = listOf(
        "All Prompts",
        "🎯 Auto-Pilot Target",
        "📊 Technical Setups",
        "🛡️ Risk & Limits",
        "🧪 Quantitative Labs",
        "🇮🇳 NIFTY / NSE",
        "🇺🇸 US Tech"
    )

    val samplePromptsMap = mapOf(
        "All Prompts" to listOf(
            "How does Auto-Pilot trade automatically until ₹1,000 target?",
            "Analyze RELIANCE & NIFTY technical indicators",
            "Explain Risk Center daily loss limit (-₹500)",
            "Recommend optimal position size for ₹25,000 capital",
            "Compare Momentum Breakout vs Mean Reversion"
        ),
        "🎯 Auto-Pilot Target" to listOf(
            "How does Auto-Pilot trade automatically until ₹1,000 target?",
            "What happens when ₹1,000 profit is achieved?",
            "How does the bot scan 1,020 assets automatically?",
            "Can I run Auto-Pilot in Live Zerodha mode?"
        ),
        "📊 Technical Setups" to listOf(
            "Analyze RELIANCE & NIFTY technical indicators",
            "Find breakout candidates with RSI > 60 and volume surge",
            "Explain MACD histogram crossover on NVDA and AAPL",
            "What are key support & resistance levels for BANKNIFTY?"
        ),
        "🛡️ Risk & Limits" to listOf(
            "Explain Risk Center daily loss limit (-₹500)",
            "Why is 1% risk per trade mathematically optimal?",
            "What does the Emergency Kill Switch do?",
            "How does maximum 4 open positions protect my capital?"
        ),
        "🧪 Quantitative Labs" to listOf(
            "Compare Momentum Breakout vs Mean Reversion",
            "What is the historical win rate of 4-Agent AI Council?",
            "How to formulate a High-Volatility VWAP scalping strategy?",
            "Explain risk-to-reward ratio 1:2.5 in backtests"
        ),
        "🇮🇳 NIFTY / NSE" to listOf(
            "Analyze current trend on NIFTY 50 and SENSEX",
            "What is the outlook for RELIANCE, TCS, and HDFCBANK?",
            "Explain MIS intraday leverage on Indian brokers"
        ),
        "🇺🇸 US Tech" to listOf(
            "Analyze NVDA, AAPL, and TSLA for swing setups",
            "How does USD/INR currency conversion affect US stock P&L?",
            "What is the market sentiment on NASDAQ 100 today?"
        )
    )

    val currentPrompts = samplePromptsMap[selectedCategory] ?: samplePromptsMap["All Prompts"]!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
            .testTag("gemini_chat_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. TOP HEADER: TITLE & ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricTeal.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "AI TradePilot Copilot",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BullishGreen.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, BullishGreen)
                        ) {
                            Text(
                                text = "LIVE",
                                color = BullishGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Google Gemini Quantitative Trading Mentor",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Prompt Library Shortcut
                IconButton(
                    onClick = { showPromptLibraryDialog = true },
                    modifier = Modifier.testTag("prompt_library_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = "Prompt Library",
                        tint = CyanBlue
                    )
                }

                // Clear Chat Action
                IconButton(
                    onClick = {
                        onClearChat()
                        Toast.makeText(context, "Chat conversation cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. LIVE CONTEXT & PORTFOLIO SNAPSHOT PILL
        if (portfolio != null) {
            val isTargetReached = portfolio.realizedPnlToday >= portfolio.dailyTarget
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copilot_portfolio_context_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(15.dp)
                        )
                        val totalVal = portfolio.startingCapital + portfolio.realizedPnlToday
                        Text(
                            text = "₹${"%,d".format(totalVal.toInt())}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "•", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = "Today: ${if (portfolio.realizedPnlToday >= 0) "+₹" else "-₹"}${Math.abs(portfolio.realizedPnlToday).toInt()}",
                            color = if (portfolio.realizedPnlToday >= 0) BullishGreen else BearishRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // AutoPilot Target Progress Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTargetReached) BullishGreen.copy(alpha = 0.2f)
                        else if (isAutoPilotActive) ElectricTeal.copy(alpha = 0.2f)
                        else SurfaceDark,
                        border = BorderStroke(
                            1.dp,
                            if (isTargetReached) BullishGreen
                            else if (isAutoPilotActive) ElectricTeal
                            else BorderDark
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isTargetReached) BullishGreen
                                        else if (isAutoPilotActive) ElectricTeal
                                        else TextMuted
                                    )
                            )
                            Text(
                                text = if (isTargetReached) "Target Reached (₹1,000 Locked)"
                                else if (isAutoPilotActive) "Auto-Pilot Active"
                                else "Target: ₹${portfolio.dailyTarget.toInt()}",
                                color = if (isTargetReached) BullishGreen
                                else if (isAutoPilotActive) ElectricTeal
                                else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. AI MODEL SELECTOR CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("model_selector_dropdown"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ModelChip(
                label = "3.5 Flash",
                subtitle = "General / Fast",
                modelKey = "gemini-3.5-flash",
                isSelected = selectedModel == "gemini-3.5-flash",
                onSelect = { onSelectModel("gemini-3.5-flash") },
                modifier = Modifier.weight(1f)
            )
            ModelChip(
                label = "3.1 Pro",
                subtitle = "Deep Quant",
                modelKey = "gemini-3.1-pro-preview",
                isSelected = selectedModel == "gemini-3.1-pro-preview",
                onSelect = { onSelectModel("gemini-3.1-pro-preview") },
                modifier = Modifier.weight(1f)
            )
            ModelChip(
                label = "Flash Lite",
                subtitle = "Ultra Scalp",
                modelKey = "gemini-3.1-flash-lite-preview",
                isSelected = selectedModel == "gemini-3.1-flash-lite-preview",
                onSelect = { onSelectModel("gemini-3.1-flash-lite-preview") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. TOPIC CATEGORY PILLS FILTER BAR
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("copilot_category_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ElectricTeal.copy(alpha = 0.18f) else SurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) ElectricTeal else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. SCROLLABLE CHAT THREAD
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    UserFriendlyEmptyChatGreeting(
                        portfolio = portfolio,
                        onPromptClick = { prompt -> onSendMessage(prompt) },
                        onOpenPromptLibrary = { showPromptLibraryDialog = true }
                    )
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    UserFriendlyChatMessageItem(
                        message = message,
                        feedback = feedbackStateMap[message.id],
                        onFeedback = { isPositive ->
                            feedbackStateMap = feedbackStateMap + (message.id to isPositive)
                            Toast.makeText(
                                context,
                                if (isPositive) "Thanks for the feedback! 👍" else "Feedback recorded. Refined next turn. 👎",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied response to clipboard 📋", Toast.LENGTH_SHORT).show()
                        },
                        onSpeak = {
                            Toast.makeText(context, "🔊 Speaking response summary...", Toast.LENGTH_SHORT).show()
                        },
                        onRegenerate = {
                            onSendMessage("Please re-evaluate and give more details for: ${message.content.take(60)}...")
                        },
                        onFollowUpClick = { followUp ->
                            onSendMessage(followUp)
                        },
                        onNavigateTo = onNavigateTo,
                        onToggleAutoPilot = onToggleAutoPilot
                    )
                }
            }

            if (isLoading) {
                item {
                    ChatLoadingIndicator(model = selectedModel)
                }
            }
        }

        // 6. QUICK SUGGESTED PROMPT CHIPS (BASED ON ACTIVE CATEGORY)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        onSendMessage(prompt)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = CyanBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = prompt,
                            color = CyanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 7. BOTTOM INPUT BAR (VOICE + PROMPT DRAWER + TEXT FIELD + SEND)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Prompt Library Quick Button (+)
            Surface(
                shape = CircleShape,
                color = SurfaceElevated,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier
                    .size(44.dp)
                    .clickable { showPromptLibraryDialog = true }
                    .testTag("chat_prompt_library_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Prompt Library",
                        tint = ElectricTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask Gemini about stocks, risk, auto-pilot...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isVoiceRecording = !isVoiceRecording
                                if (isVoiceRecording) {
                                    Toast.makeText(context, "🎙️ Listening... (Voice prompt simulation)", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        delay(1500)
                                        inputText = "Analyze top breakout stocks in NIFTY 50"
                                        isVoiceRecording = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isVoiceRecording) BullishGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = ElectricTeal,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )

            // Send FAB Button
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val text = inputText.trim()
                        inputText = ""
                        onSendMessage(text)
                    }
                },
                containerColor = if (inputText.isNotBlank() && !isLoading) ElectricTeal else SurfaceElevated,
                contentColor = if (inputText.isNotBlank() && !isLoading) BackgroundDark else TextMuted,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // PROMPT LIBRARY BOTTOM SHEET DIALOG
    if (showPromptLibraryDialog) {
        CopilotPromptLibraryDialog(
            onDismiss = { showPromptLibraryDialog = false },
            onSelectPrompt = { prompt ->
                showPromptLibraryDialog = false
                onSendMessage(prompt)
            }
        )
    }
}

/**
 * Model selector chip with subtitle
 */
@Composable
private fun ModelChip(
    label: String,
    subtitle: String,
    modelKey: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ElectricTeal.copy(alpha = 0.18f) else SurfaceElevated,
        border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark),
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (isSelected) ElectricTeal else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = if (isSelected) ElectricTeal.copy(alpha = 0.8f) else TextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * User-Friendly Chat Message Bubble with Rich Markdown Formatting,
 * Metric Badges, Action Buttons, and Utility Toolbar.
 */
@Composable
private fun UserFriendlyChatMessageItem(
    message: ChatMessage,
    feedback: Boolean?,
    onFeedback: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit,
    onFollowUpClick: (String) -> Unit,
    onNavigateTo: ((Screen) -> Unit)? = null,
    onToggleAutoPilot: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    // Determine relevant actionable shortcuts based on text content
    val containsAutoPilot = remember(message.content) {
        message.content.contains("auto-pilot", ignoreCase = true) || message.content.contains("autopilot", ignoreCase = true)
    }
    val containsRiskCenter = remember(message.content) {
        message.content.contains("risk", ignoreCase = true) || message.content.contains("loss limit", ignoreCase = true) || message.content.contains("kill switch", ignoreCase = true)
    }
    val containsScanner = remember(message.content) {
        message.content.contains("nifty", ignoreCase = true) || message.content.contains("reliance", ignoreCase = true) || message.content.contains("breakout", ignoreCase = true)
    }
    val containsStrategy = remember(message.content) {
        message.content.contains("strategy", ignoreCase = true) || message.content.contains("backtest", ignoreCase = true) || message.content.contains("momentum", ignoreCase = true)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricTeal.copy(alpha = 0.3f), SurfaceElevated)
                        )
                    )
                    .border(1.dp, ElectricTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 330.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) SurfaceElevated else SurfaceDark,
                border = BorderStroke(
                    1.dp,
                    if (isUser) ElectricTeal.copy(alpha = 0.5f) else BorderDark
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header for AI model messages
                    if (!isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "AI Copilot",
                                    color = ElectricTeal,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "•", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = message.modelUsed.replace("-preview", "").replace("gemini-", "Gemini "),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(text = timeStr, color = TextMuted, fontSize = 9.5.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Render Formatted Content (Lines, Bullet points, Highlighted bold text)
                    RenderFormattedMessageContent(content = message.content)

                    // ACTIONABLE SHORTCUT BUTTONS (If AI recommendations mention features)
                    if (!isUser) {
                        if (containsAutoPilot || containsRiskCenter || containsScanner || containsStrategy) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "QUICK ACTIONS:",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (containsAutoPilot) {
                                    OutlinedButton(
                                        onClick = {
                                            onToggleAutoPilot?.invoke()
                                            onNavigateTo?.invoke(Screen.DASHBOARD)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricTeal),
                                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Auto-Pilot", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (containsRiskCenter) {
                                    OutlinedButton(
                                        onClick = { onNavigateTo?.invoke(Screen.RISK_CENTER) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Risk Center", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (containsScanner) {
                                    OutlinedButton(
                                        onClick = { onNavigateTo?.invoke(Screen.SCANNER) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBlue),
                                        border = BorderStroke(1.dp, CyanBlue.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scanner", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // UTILITY TOOLBAR (Copy, Voice, Feedback, Regenerate)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Copy
                                IconButton(
                                    onClick = onCopy,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy text",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Speak
                                IconButton(
                                    onClick = onSpeak,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak response",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Regenerate
                                IconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate response",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Feedback (Thumbs Up / Down)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { onFeedback(true) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = "Helpful",
                                        tint = if (feedback == true) BullishGreen else TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onFeedback(false) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbDown,
                                        contentDescription = "Not helpful",
                                        tint = if (feedback == false) BearishRed else TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = timeStr, color = TextMuted, fontSize = 9.sp)
            }
        }
    }
}

/**
 * Parses and displays rich markdown elements:
 * - Bold headings and emphasis
 * - Bullet lists with glowing icons
 * - Numbered steps
 * - Highlight tags for currency, RSI, and percentages
 */
@Composable
private fun RenderFormattedMessageContent(content: String) {
    val lines = remember(content) { content.split("\n") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                continue
            }

            when {
                // Bullet item starting with • or * or -
                trimmed.startsWith("•") || trimmed.startsWith("* ") || trimmed.startsWith("- ") -> {
                    val textWithoutBullet = trimmed.removePrefix("•").removePrefix("* ").removePrefix("- ").trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(ElectricTeal)
                        )
                        Text(
                            text = textWithoutBullet.replace("**", ""),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Numbered list item like 1. 2. 3.
                trimmed.matches(Regex("^\\d+\\..*")) -> {
                    val match = Regex("^(\\d+)\\.\\s*(.*)").find(trimmed)
                    val num = match?.groupValues?.get(1) ?: "1"
                    val itemText = match?.groupValues?.get(2) ?: trimmed
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = num, color = ElectricTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = itemText.replace("**", ""),
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Section Header starting with # or **Title**
                trimmed.startsWith("#") || (trimmed.startsWith("**") && trimmed.endsWith("**")) -> {
                    val headerText = trimmed.replace("#", "").replace("**", "").trim()
                    Text(
                        text = headerText,
                        color = CyanBlue,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // Regular Text Line
                else -> {
                    Text(
                        text = trimmed.replace("**", ""),
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Animated Loading Indicator with Model indicator
 */
@Composable
private fun ChatLoadingIndicator(model: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(ElectricTeal.copy(alpha = 0.2f))
                .border(1.dp, ElectricTeal.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ElectricTeal,
                strokeWidth = 2.dp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceElevated,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Gemini (${model.replace("-preview", "")}) formulating quantitative analysis...",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

/**
 * User-Friendly Welcome Card with starter prompts and feature highlights
 */
@Composable
private fun UserFriendlyEmptyChatGreeting(
    portfolio: PortfolioEntity?,
    onPromptClick: (String) -> Unit,
    onOpenPromptLibrary: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("copilot_welcome_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(BorderDark, ElectricTeal.copy(alpha = 0.4f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceElevated.copy(alpha = 0.7f), SurfaceDark)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricTeal.copy(alpha = 0.3f), CyanBlue.copy(alpha = 0.1f))
                            )
                        )
                        .border(1.dp, ElectricTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = "AI TradePilot Quantitative Copilot",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Multi-Agent Trading Intelligence & Real-Time Strategy",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Your algorithmic trading mentor powered by Google Gemini. Optimized for ₹1,000 daily target achievement, 1% strict risk management, and multi-exchange coverage across 1,020+ Indian & US assets.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "POPULAR STARTERS:",
                color = ElectricTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4 Starter Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StarterCard(
                    icon = Icons.Default.Bolt,
                    iconTint = BullishGreen,
                    title = "Auto-Pilot ₹1,000 Target Strategy",
                    subtitle = "How the bot hunts setups until ₹1,000 profit is achieved and locks safely.",
                    onClick = { onPromptClick("How does Auto-Pilot trade automatically until ₹1,000 target?") }
                )

                StarterCard(
                    icon = Icons.Default.Sensors,
                    iconTint = CyanBlue,
                    title = "Scan Indian & US Market Breakouts",
                    subtitle = "Analyze technical momentum setups for RELIANCE, NIFTY 50, and US Tech.",
                    onClick = { onPromptClick("Analyze RELIANCE & NIFTY technical indicators") }
                )

                StarterCard(
                    icon = Icons.Default.Shield,
                    iconTint = AmberAccent,
                    title = "Risk Center -₹500 Loss Limit Audit",
                    subtitle = "How capital protection and max 4 open positions prevent severe drawdowns.",
                    onClick = { onPromptClick("Explain Risk Center daily loss limit (-₹500)") }
                )

                StarterCard(
                    icon = Icons.Default.Science,
                    iconTint = NeonPurple,
                    title = "Compare Quantitative Strategies",
                    subtitle = "Momentum Breakout vs Mean Reversion win rate & profit factor comparison.",
                    onClick = { onPromptClick("Compare Momentum Breakout vs Mean Reversion") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenPromptLibrary,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricTeal),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse All 15+ Quantitative Prompts", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StarterCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Curated Prompt Library Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopilotPromptLibraryDialog(
    onDismiss: () -> Unit,
    onSelectPrompt: (String) -> Unit
) {
    val promptCategories = listOf(
        "Auto-Pilot & Execution" to listOf(
            "How does Auto-Pilot trade automatically until ₹1,000 target?",
            "What happens when ₹1,000 profit is achieved?",
            "How does the bot scan 1,020 assets automatically?",
            "Can I run Auto-Pilot in Live Zerodha mode?",
            "How does Auto-Pilot manage trailing stop-losses?"
        ),
        "Risk & Capital Protection" to listOf(
            "Explain Risk Center daily loss limit (-₹500)",
            "Recommend optimal position size for ₹25,000 capital",
            "Why is 1% risk per trade mathematically optimal?",
            "What does the Emergency Kill Switch do?",
            "How does maximum 4 open positions protect my account?"
        ),
        "Technical & Indicators" to listOf(
            "Analyze RELIANCE & NIFTY technical indicators",
            "Explain MACD histogram divergence on NVDA and AAPL",
            "What are the best RSI oversold criteria for Indian equities?",
            "How to use VWAP bands for intraday index scalping?"
        ),
        "Quantitative Strategies" to listOf(
            "Compare Momentum Breakout vs Mean Reversion",
            "What is the historical win rate of 4-Agent AI Council?",
            "How to formulate a High-Volatility VWAP scalping strategy?",
            "Explain risk-to-reward ratio 1:2.5 in backtests"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Curated Quantitative Prompts",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                promptCategories.forEach { (categoryName, prompts) ->
                    item {
                        Text(
                            text = categoryName.uppercase(),
                            color = CyanBlue,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            prompts.forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceElevated,
                                    border = BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPrompt(prompt) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = prompt,
                                            color = TextPrimary,
                                            fontSize = 11.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
