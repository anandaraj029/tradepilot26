package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradePilotViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: TradePilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // Display banner messages via Snackbar
                LaunchedEffect(uiState.bannerMessage) {
                    val message = uiState.bannerMessage
                    if (message != null) {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.dismissBanner()
                    }
                }

                // Handle Hardware / Gesture Back Navigation
                BackHandler(enabled = uiState.isViewingStockDetail || uiState.currentScreen != Screen.DASHBOARD) {
                    if (uiState.isViewingStockDetail) {
                        viewModel.closeStockDetail()
                    } else if (uiState.currentScreen != Screen.DASHBOARD) {
                        viewModel.navigateTo(Screen.DASHBOARD)
                    }
                }

                if (!uiState.isAuthenticated) {
                    AuthScreen(
                        userEmail = uiState.userProfile.email,
                        onAuthenticated = { viewModel.setAuthenticated(true) },
                        onSwitchToLockScreen = { viewModel.setAppLocked(true) }
                    )
                } else if (uiState.isAppLocked) {
                    LockScreen(
                        userProfile = uiState.userProfile,
                        correctPin = uiState.appLockPin,
                        onUnlock = { viewModel.unlockApp() },
                        onSwitchToLogin = { viewModel.setAuthenticated(false) }
                    )
                } else {
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val coroutineScope = rememberCoroutineScope()

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            UserProfileDrawerSheet(
                                userProfile = uiState.userProfile,
                                portfolio = uiState.portfolio,
                                currentScreen = uiState.currentScreen,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenSettings = { viewModel.setSettingsDialogOpen(true) },
                                onOpenNotifications = { viewModel.setNotificationsDialogOpen(true) },
                                onLockTerminal = { viewModel.lockApp() },
                                onLogout = { viewModel.setAuthenticated(false) },
                                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                            )
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundDark),
                            containerColor = BackgroundDark,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            topBar = {
                                FintechTopBar(
                                    portfolio = uiState.portfolio,
                                    currentScreen = uiState.currentScreen,
                                    isViewingStockDetail = uiState.isViewingStockDetail,
                                    selectedStock = uiState.selectedStock,
                                    onKillSwitchClick = { viewModel.toggleEmergencyKillSwitch() },
                                    onUnlockClick = { viewModel.unlockTrading() },
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                    onBackClick = {
                                        if (uiState.isViewingStockDetail) {
                                            viewModel.closeStockDetail()
                                        } else if (uiState.currentScreen != Screen.DASHBOARD) {
                                            viewModel.navigateTo(Screen.DASHBOARD)
                                        }
                                    },
                                    isLiveMode = uiState.isLiveTradingMode,
                                    liveMargin = uiState.brokerConnection.liveEquityMargin,
                                    onOpenBrokerDialog = { viewModel.navigateTo(Screen.BROKER_CONNECTIONS) },
                                    onOpenAddFunds = { viewModel.setAddFundsDialogOpen(true) }
                                )
                            },
                    bottomBar = {
                        if (!uiState.isViewingStockDetail) {
                            NavigationBar(
                                containerColor = SurfaceDark,
                                tonalElevation = 8.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                Screen.primaryNavItems.forEach { screen ->
                                    val isSelected = uiState.currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { viewModel.navigateTo(screen) },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = when (screen) {
                                                    Screen.STRATEGIES -> "Lab"
                                                    Screen.AI_COUNCIL -> "Council"
                                                    Screen.RISK_CENTER -> "Risk"
                                                    else -> screen.title
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BackgroundDark,
                                            unselectedIconColor = TextMuted,
                                            selectedTextColor = ElectricTeal,
                                            unselectedTextColor = TextMuted,
                                            indicatorColor = ElectricTeal
                                        ),
                                        modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = SurfaceElevated,
                                contentColor = TextPrimary,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (uiState.isViewingStockDetail && uiState.selectedStock != null) {
                            StockDetailScreen(
                                uiState = uiState,
                                onBackClick = { viewModel.closeStockDetail() },
                                onRunAiCouncil = { viewModel.runAiDeliberation(it) },
                                onOpenPaperTrade = { viewModel.setPaperTradeSheetOpen(true) },
                                onToggleWatchlist = { sym, mkt, name -> viewModel.toggleWatchlist(sym, mkt, name) }
                            )
                        } else {
                            when (uiState.currentScreen) {
                                Screen.DASHBOARD -> {
                                    DashboardScreen(
                                        uiState = uiState,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onSelectStock = { viewModel.openStockDetail(it) },
                                        onClosePosition = { pos, price -> viewModel.closePosition(pos, price) },
                                        onResetCapital = { viewModel.resetCapitalTo25k() },
                                        onToggleAutoPilot = { viewModel.toggleAutoPilot() },
                                        onStartAutoPilot = { viewModel.startAutoPilot() },
                                        onStopAutoPilot = { viewModel.stopAutoPilot() },
                                        onToggleAutoActivate = { viewModel.setAutoActivateWhenTargetPending(it) },
                                        onRunAutoPilotCycle = { viewModel.runAutoPilotCycle() },
                                        onOpenBrokerDialog = { viewModel.navigateTo(Screen.BROKER_CONNECTIONS) },
                                        onOpenAddFunds = { viewModel.setAddFundsDialogOpen(true) },
                                        onToggleLiveMode = { viewModel.toggleLiveTradingMode(it) },
                                        onToggleAutonomousAi = { viewModel.setAutoPilotAutonomousLive(it) }
                                    )
                                }
                                Screen.MARKETS -> {
                                    MarketsScreen(
                                        uiState = uiState,
                                        onSelectStock = { viewModel.openStockDetail(it) },
                                        onToggleWatchlist = { sym, mkt, name -> viewModel.toggleWatchlist(sym, mkt, name) },
                                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                        onMarketFilterChange = { viewModel.setMarketFilter(it) },
                                        onDeliberateInCouncil = { stock ->
                                            viewModel.openStockDetail(stock)
                                            viewModel.runAiDeliberation(stock)
                                            viewModel.navigateTo(Screen.AI_COUNCIL)
                                        },
                                        onOpenPaperTrade = { stock ->
                                            viewModel.openStockDetail(stock)
                                            viewModel.setPaperTradeSheetOpen(true)
                                        },
                                        onToggleAutoPilot = { viewModel.toggleAutoPilot() },
                                        onStartAutoPilot = { viewModel.startAutoPilot() },
                                        onStopAutoPilot = { viewModel.stopAutoPilot() }
                                    )
                                }
                                Screen.AI_COUNCIL -> {
                                    AiCouncilScreen(
                                        uiState = uiState,
                                        onSelectStock = { viewModel.selectStock(it) },
                                        onOpenPaperTrade = { viewModel.setPaperTradeSheetOpen(true) }
                                    )
                                }
                                Screen.STRATEGIES -> {
                                    StrategyLabScreen(
                                        uiState = uiState,
                                        onGenerateStrategy = { viewModel.generateAiStrategy(it) },
                                        onCreatePaperStrategy = { name, alloc, risk, entry, exit, prompt ->
                                            viewModel.createPaperStrategy(name, alloc, risk, entry, exit, prompt)
                                        },
                                        onDeleteCustomStrategy = { viewModel.deleteCustomStrategy(it) },
                                        onNavigateToBacktest = { viewModel.navigateTo(Screen.BACKTEST) }
                                    )
                                }
                                Screen.BACKTEST -> {
                                    BacktestSimulatorScreen(
                                        uiState = uiState,
                                        onBackClick = { viewModel.navigateTo(Screen.STRATEGIES) }
                                    )
                                }
                                Screen.JOURNAL -> {
                                    JournalScreen(
                                        uiState = uiState,
                                        onUpdateTrade = { viewModel.updateJournalTrade(it) },
                                        onDeleteTrade = { viewModel.deleteJournalTrade(it) },
                                        onAddManualTrade = { sym, mkt, comp, strat, entry, exit, qty, dec, conf, override, reason, notes, tags ->
                                            viewModel.addManualTrade(sym, mkt, comp, strat, entry, exit, qty, dec, conf, override, reason, notes, tags)
                                        }
                                    )
                                }
                                Screen.RISK_CENTER -> {
                                    RiskCenterScreen(
                                        uiState = uiState,
                                        onSaveRiskSettings = { cap, target, loss, risk, pos, lock, mode ->
                                            viewModel.updateRiskSettings(cap, target, loss, risk, pos, lock, mode)
                                        },
                                        onToggleKillSwitch = { viewModel.toggleEmergencyKillSwitch() },
                                        onUnlockTrading = { viewModel.unlockTrading() },
                                        onResetCapital = { viewModel.resetCapitalTo25k() },
                                        onToggleAutoPilot = { viewModel.toggleAutoPilot() },
                                        onRunAutoPilotCycle = { viewModel.runAutoPilotCycle() }
                                    )
                                }
                                Screen.DAILY_SUMMARY -> {
                                    DailySummaryScreen(
                                        uiState = uiState,
                                        onBackClick = { viewModel.navigateTo(Screen.DASHBOARD) }
                                    )
                                }
                                Screen.SCANNER -> {
                                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                        com.example.ui.components.AiMarketScanner(
                                            stocks = uiState.indianStocks + uiState.usStocks,
                                            watchlistSymbols = uiState.watchlist.map { it.symbol }.toSet(),
                                            onSelectStock = { viewModel.openStockDetail(it) },
                                            onDeliberateInCouncil = { stock ->
                                                viewModel.openStockDetail(stock)
                                                viewModel.runAiDeliberation(stock)
                                                viewModel.navigateTo(Screen.AI_COUNCIL)
                                            },
                                            onOpenPaperTrade = { stock ->
                                                viewModel.openStockDetail(stock)
                                                viewModel.setPaperTradeSheetOpen(true)
                                            },
                                            onToggleWatchlist = { sym, mkt, name -> viewModel.toggleWatchlist(sym, mkt, name) }
                                        )
                                    }
                                }
                                Screen.COPILOT -> {
                                    GeminiChatScreen(
                                        messages = uiState.chatMessages,
                                        isLoading = uiState.isChatLoading,
                                        selectedModel = uiState.selectedChatModel,
                                        portfolio = uiState.portfolio,
                                        isAutoPilotActive = uiState.isAutoPilotActive,
                                        onSendMessage = { viewModel.sendChatMessage(it) },
                                        onSelectModel = { viewModel.setChatModel(it) },
                                        onClearChat = { viewModel.clearChat() },
                                        onNavigateTo = { viewModel.navigateTo(it) },
                                        onToggleAutoPilot = { viewModel.toggleAutoPilot() }
                                    )
                                }
                                Screen.BROKER_CONNECTIONS -> {
                                    BrokerConnectionsScreen(
                                        brokerConnection = uiState.brokerConnection,
                                        fundTransactions = uiState.fundTransactions,
                                        isLiveMode = uiState.isLiveTradingMode,
                                        isAutonomousAi = uiState.autoPilotAutonomousLive,
                                        onBack = { viewModel.navigateTo(Screen.DASHBOARD) },
                                        onToggleLiveMode = { viewModel.toggleLiveTradingMode(it) },
                                        onToggleAutonomousAi = { viewModel.setAutoPilotAutonomousLive(it) },
                                        onConnectBrokerOAuth2 = { code, key, sec, tok, uid ->
                                            viewModel.connectBrokerOAuth2(code, key, sec, tok, uid)
                                        },
                                        onDisconnectBroker = { viewModel.disconnectBroker(it) },
                                        onRefreshToken = { viewModel.refreshBrokerToken(it) },
                                        onTestPing = { viewModel.testBrokerPing(it) },
                                        onOpenAddFunds = { viewModel.setAddFundsDialogOpen(true) },
                                        onOpenWithdrawFunds = { viewModel.setWithdrawFundsDialogOpen(true) }
                                    )
                                }
                            }
                        }

                        // Trade Execution Modal Dialog (Supports Paper & Live Zerodha Kite)
                        if (uiState.isPaperTradeSheetOpen && uiState.selectedStock != null) {
                            PaperTradeDialog(
                                stock = uiState.selectedStock!!,
                                portfolio = uiState.portfolio,
                                broker = uiState.brokerConnection,
                                isLiveModeDefault = uiState.isLiveTradingMode,
                                onDismiss = { viewModel.setPaperTradeSheetOpen(false) },
                                onExecuteOrder = { qty, sl, tp, isLive, product ->
                                    viewModel.executeOrder(
                                        stock = uiState.selectedStock!!,
                                        quantity = qty,
                                        stopLoss = sl,
                                        takeProfit = tp,
                                        isLive = isLive,
                                        product = product
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Zerodha Kite Broker Integration Dialog
            if (uiState.isBrokerConnectDialogOpen) {
                ZerodhaBrokerDialog(
                    broker = uiState.brokerConnection,
                    fundTransactions = uiState.fundTransactions,
                    isLiveMode = uiState.isLiveTradingMode,
                    isAutonomousAi = uiState.autoPilotAutonomousLive,
                    onDismiss = { viewModel.setBrokerConnectDialogOpen(false) },
                    onToggleLiveMode = { viewModel.toggleLiveTradingMode(it) },
                    onToggleAutonomousAi = { viewModel.setAutoPilotAutonomousLive(it) },
                    onConnectZerodha = { apiKey, apiSecret, reqTok, userId ->
                        viewModel.connectZerodha(apiKey, apiSecret, reqTok, userId)
                    },
                    onDisconnectZerodha = { viewModel.disconnectZerodha() },
                    onOpenAddFunds = {
                        viewModel.setBrokerConnectDialogOpen(false)
                        viewModel.setAddFundsDialogOpen(true)
                    },
                    onOpenWithdrawFunds = {
                        viewModel.setBrokerConnectDialogOpen(false)
                        viewModel.setWithdrawFundsDialogOpen(true)
                    }
                )
            }

            // Add Live Funds / UPI Top-Up Dialog
            if (uiState.isAddFundsDialogOpen) {
                AddLiveFundsDialog(
                    currentMargin = uiState.brokerConnection.liveEquityMargin,
                    onDismiss = { viewModel.setAddFundsDialogOpen(false) },
                    onDepositSuccess = { amount, method, upiId ->
                        viewModel.depositLiveFunds(amount, method, upiId)
                    }
                )
            }

            // Withdraw Funds Dialog
            if (uiState.isWithdrawFundsDialogOpen) {
                WithdrawFundsDialog(
                    availableMargin = uiState.brokerConnection.liveEquityMargin,
                    onDismiss = { viewModel.setWithdrawFundsDialogOpen(false) },
                    onWithdrawSuccess = { amount, bankDetails ->
                        viewModel.withdrawLiveFunds(amount, bankDetails)
                    }
                )
            }

            // Security & Settings Modal Dialog
            if (uiState.isSettingsDialogOpen) {
                SecuritySettingsDialog(
                    userProfile = uiState.userProfile,
                    isLiveMode = uiState.isLiveTradingMode,
                    onToggleLiveMode = { viewModel.toggleLiveTradingMode(it) },
                    onNavigateToBrokers = { viewModel.navigateTo(Screen.BROKER_CONNECTIONS) },
                    onSaveProfile = { viewModel.updateUserProfile(it) },
                    onDismiss = { viewModel.setSettingsDialogOpen(false) }
                )
            }

            // Notification Setup Modal Dialog
            if (uiState.isNotificationsDialogOpen) {
                NotificationSetupDialog(
                    userProfile = uiState.userProfile,
                    onSaveProfile = { viewModel.updateUserProfile(it) },
                    onDismiss = { viewModel.setNotificationsDialogOpen(false) }
                )
            }
        }
    }
}
}
}
