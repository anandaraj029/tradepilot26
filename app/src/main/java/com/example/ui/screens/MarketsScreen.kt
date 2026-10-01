package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.MarketType
import com.example.data.model.StockQuote
import com.example.ui.components.AiMarketScanner
import com.example.ui.components.FintechCard
import com.example.ui.components.Sparkline
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotUiState
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    uiState: TradePilotUiState,
    onSelectStock: (StockQuote) -> Unit,
    onToggleWatchlist: (String, String, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMarketFilterChange: (MarketType?) -> Unit,
    onDeliberateInCouncil: (StockQuote) -> Unit = {},
    onOpenPaperTrade: (StockQuote) -> Unit = {},
    onToggleAutoPilot: () -> Unit = {},
    onStartAutoPilot: () -> Unit = onToggleAutoPilot,
    onStopAutoPilot: () -> Unit = onToggleAutoPilot,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: ALL, 1: INDIA, 2: USA, 3: WATCHLIST, 4: SCANNER
    var selectedSectorFilter by remember { mutableStateOf("ALL") }

    val allStocks = remember(uiState.indianStocks, uiState.usStocks) {
        uiState.indianStocks + uiState.usStocks
    }

    val watchlistSymbols = remember(uiState.watchlist) {
        uiState.watchlist.map { it.symbol }.toSet()
    }

    val dailyTarget = max(1.0, uiState.portfolio.dailyTarget)
    val realizedToday = uiState.portfolio.realizedPnlToday
    val isTargetAchieved = realizedToday >= dailyTarget
    val remainingToTarget = max(0.0, dailyTarget - realizedToday)

    val filteredStocks = remember(selectedTab, selectedSectorFilter, uiState.searchQuery, allStocks, watchlistSymbols) {
        val baseList = when (selectedTab) {
            1 -> uiState.indianStocks
            2 -> uiState.usStocks
            3 -> allStocks.filter { watchlistSymbols.contains(it.symbol) }
            else -> allStocks
        }

        val sectorFiltered = when (selectedSectorFilter) {
            "GAINERS" -> baseList.filter { it.priceChangePercent > 1.0 }.sortedByDescending { it.priceChangePercent }
            "LOSERS" -> baseList.filter { it.priceChangePercent < -0.5 }.sortedBy { it.priceChangePercent }
            "TECH" -> baseList.filter { it.sector.contains("Tech", ignoreCase = true) || it.sector.contains("Software", ignoreCase = true) || it.sector.contains("Semi", ignoreCase = true) }
            "BANK" -> baseList.filter { it.sector.contains("Bank", ignoreCase = true) || it.sector.contains("Fin", ignoreCase = true) }
            "ENERGY" -> baseList.filter { it.sector.contains("Energy", ignoreCase = true) || it.sector.contains("Power", ignoreCase = true) || it.sector.contains("Oil", ignoreCase = true) }
            "PHARMA" -> baseList.filter { it.sector.contains("Pharma", ignoreCase = true) || it.sector.contains("Health", ignoreCase = true) || it.sector.contains("Bio", ignoreCase = true) }
            "AUTO" -> baseList.filter { it.sector.contains("Auto", ignoreCase = true) || it.sector.contains("Motors", ignoreCase = true) }
            else -> baseList
        }

        if (uiState.searchQuery.isBlank()) {
            sectorFiltered
        } else {
            val q = uiState.searchQuery.trim().lowercase()
            sectorFiltered.filter {
                it.symbol.lowercase().contains(q) ||
                it.companyName.lowercase().contains(q) ||
                it.sector.lowercase().contains(q)
            }
        }
    }

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. TOP AUTOPILOT TARGET STATUS & QUICK START / STOP CONTROLS
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceElevated,
            border = BorderStroke(
                1.dp,
                if (isTargetAchieved) GoldAccent else if (uiState.isAutoPilotActive) ElectricTeal.copy(alpha = 0.6f) else BorderDark
            ),
            modifier = Modifier.fillMaxWidth().testTag("markets_autopilot_header_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isAutoPilotActive) ElectricTeal.copy(alpha = 0.2f) else SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isAutoPilotActive) Icons.Default.SmartToy else Icons.Default.PrecisionManufacturing,
                            contentDescription = null,
                            tint = if (uiState.isAutoPilotActive) ElectricTeal else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = if (isTargetAchieved) "Target Achieved (Profit Locked)"
                                else if (uiState.isAutoPilotActive) "Auto-Pilot: Active Hunting"
                                else "Auto-Pilot: Stopped",
                                color = if (isTargetAchieved) GoldAccent else if (uiState.isAutoPilotActive) BullishGreen else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.isAutoPilotActive && !isTargetAchieved) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BullishGreen))
                            }
                        }
                        Text(
                            text = if (isTargetAchieved) "₹${dailyTarget.toInt()} target achieved • Gains preserved"
                            else if (uiState.isAutoPilotActive) "Auto-active: ₹${remainingToTarget.toInt()} left to reach target"
                            else "Start bot to hunt setups across 1,020 assets",
                            color = TextSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Stop / Start Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (uiState.isAutoPilotActive) {
                        Button(
                            onClick = onStopAutoPilot,
                            colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("markets_stop_autopilot_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("STOP", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onStartAutoPilot,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTargetAchieved,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("markets_start_autopilot_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("START", color = BackgroundDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search 1,020 Indian & US securities, sectors...", color = TextMuted, fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("market_search_input"),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Selector Row with Asset Counts
        val inCount = uiState.indianStocks.size
        val usCount = uiState.usStocks.size
        val totalCount = allStocks.size
        val tabs = listOf(
            "All ($totalCount)",
            "India 🇮🇳 ($inCount)",
            "USA 🇺🇸 ($usCount)",
            "Watchlist ⭐ (${watchlistSymbols.size})",
            "AI Scanner 📡"
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = ElectricTeal,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) ElectricTeal else TextMuted
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedTab == 4) {
            // Render full AI Market Scanner
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
            ) {
                item {
                    AiMarketScanner(
                        stocks = allStocks,
                        watchlistSymbols = watchlistSymbols,
                        onSelectStock = onSelectStock,
                        onDeliberateInCouncil = onDeliberateInCouncil,
                        onOpenPaperTrade = onOpenPaperTrade,
                        onToggleWatchlist = onToggleWatchlist
                    )
                }
            }
        } else {
            // Horizontal Quick Filter Chips (Gainers, Losers, Sectors)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(label = "All Sectors", isSelected = selectedSectorFilter == "ALL", onClick = { selectedSectorFilter = "ALL" })
                FilterChipItem(label = "🚀 Gainers", isSelected = selectedSectorFilter == "GAINERS", onClick = { selectedSectorFilter = "GAINERS" })
                FilterChipItem(label = "🔻 Losers", isSelected = selectedSectorFilter == "LOSERS", onClick = { selectedSectorFilter = "LOSERS" })
                FilterChipItem(label = "Tech & AI", isSelected = selectedSectorFilter == "TECH", onClick = { selectedSectorFilter = "TECH" })
                FilterChipItem(label = "Banking & Fin", isSelected = selectedSectorFilter == "BANK", onClick = { selectedSectorFilter = "BANK" })
                FilterChipItem(label = "Energy & Power", isSelected = selectedSectorFilter == "ENERGY", onClick = { selectedSectorFilter = "ENERGY" })
                FilterChipItem(label = "Pharma & Health", isSelected = selectedSectorFilter == "PHARMA", onClick = { selectedSectorFilter = "PHARMA" })
                FilterChipItem(label = "Auto & EV", isSelected = selectedSectorFilter == "AUTO", onClick = { selectedSectorFilter = "AUTO" })
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Asset Counter & Exchange Rate Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Showing ${filteredStocks.size} of $totalCount assets",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElectricTeal.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "1,020 Total",
                            color = ElectricTeal,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated
                ) {
                    Text(
                        text = "USD/INR: ₹${uiState.portfolio.usdInrRate}",
                        color = CyanBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Virtualized List of 500-1000 Stocks
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredStocks, key = { it.symbol }) { stock ->
                    val isWatchlisted = watchlistSymbols.contains(stock.symbol)
                    MarketStockCard(
                        stock = stock,
                        isWatchlisted = isWatchlisted,
                        usdRate = uiState.portfolio.usdInrRate,
                        onClick = { onSelectStock(stock) },
                        onToggleWatchlist = { onToggleWatchlist(stock.symbol, stock.market.name, stock.companyName) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ElectricTeal.copy(alpha = 0.2f) else SurfaceDark,
        border = BorderStroke(1.dp, if (isSelected) ElectricTeal else BorderDark),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) ElectricTeal else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MarketStockCard(
    stock: StockQuote,
    isWatchlisted: Boolean,
    usdRate: Double,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = stock.priceChangePercent >= 0

    FintechCard(
        modifier = modifier.testTag("stock_item_${stock.symbol}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Symbol & Name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceElevated
                    ) {
                        Text(
                            text = if (stock.market == MarketType.USA) "US 🇺🇸" else "IN 🇮🇳",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.companyName,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Technical pills
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TechIndicatorPill(label = "RSI", value = stock.rsi.toInt().toString(), isBullish = stock.rsi in 45.0..68.0)
                    Spacer(modifier = Modifier.width(6.dp))
                    TechIndicatorPill(label = "MACD", value = if (stock.macd > 0) "+${stock.macd.toInt()}" else stock.macd.toInt().toString(), isBullish = stock.macd > 0)
                }
            }

            // Middle: Sparkline
            Sparkline(
                data = stock.sparklineData,
                isPositive = isPositive,
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Price & Watchlist Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stock.market.currencySymbol}${stock.currentPrice}",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${stock.priceChangePercent}%",
                        color = if (isPositive) BullishGreen else BearishRed,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (stock.market == MarketType.USA) {
                        val inrEquivalent = stock.currentPrice * usdRate
                        Text(
                            text = "≈ ₹${inrEquivalent.toInt()}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlisted) GoldAccent else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TechIndicatorPill(label: String, value: String, isBullish: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = if (isBullish) BullishGreen else BearishRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
