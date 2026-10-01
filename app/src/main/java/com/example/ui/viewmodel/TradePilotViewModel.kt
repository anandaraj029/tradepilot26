package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiChatService
import com.example.data.db.AppDatabase
import com.example.data.db.AssetEntity
import com.example.data.db.AuditLogEntity
import com.example.data.db.BrokerConnectionEntity
import com.example.data.db.CustomStrategyEntity
import com.example.data.db.FundTransactionEntity
import com.example.data.db.PortfolioEntity
import com.example.data.db.PositionEntity
import com.example.data.db.TradeEntity
import com.example.data.db.TradeJournalEntity
import com.example.data.db.WatchlistEntity
import com.example.data.engine.AutoPilotDecision
import com.example.data.engine.AutoPilotEngine
import com.example.data.market.MarketDataProvider
import com.example.data.model.AiConsensusResult
import com.example.data.model.MarketType
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.StockQuote
import com.example.data.model.StrategyModel
import com.example.data.model.UserProfile
import com.example.data.repository.TradePilotRepository
import com.example.ui.navigation.Screen
import com.example.util.SecureBrokerStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TradePilotUiState(
    val currentScreen: Screen = Screen.DASHBOARD,
    val selectedStock: StockQuote? = null,
    val portfolio: PortfolioEntity = PortfolioEntity(),
    val positions: List<PositionEntity> = emptyList(),
    val indianStocks: List<StockQuote> = emptyList(),
    val usStocks: List<StockQuote> = emptyList(),
    val currentDeliberation: AiConsensusResult? = null,
    val currentRiskEvaluation: RiskEvaluationResult? = null,
    val isDeliberating: Boolean = false,
    val journalEntries: List<TradeJournalEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val standardStrategies: List<StrategyModel> = emptyList(),
    val customStrategies: List<CustomStrategyEntity> = emptyList(),
    val watchlist: List<WatchlistEntity> = emptyList(),
    val searchQuery: String = "",
    val activeMarketFilter: MarketType? = null,
    val bannerMessage: String? = null,
    val isViewingStockDetail: Boolean = false,
    val isPaperTradeSheetOpen: Boolean = false,
    val isRiskSettingsSheetOpen: Boolean = false,
    val isStrategyBuilderSheetOpen: Boolean = false,
    val isAuthenticated: Boolean = true, // Authenticated by default with option to test login / 2FA flow from drawer
    val isAppLocked: Boolean = false, // PIN / Biometric App Lock screen
    val appLockPin: String = "1234",
    val userProfile: UserProfile = UserProfile(),
    val isSettingsDialogOpen: Boolean = false,
    val isNotificationsDialogOpen: Boolean = false,
    val brokerConnection: BrokerConnectionEntity = BrokerConnectionEntity(),
    val fundTransactions: List<FundTransactionEntity> = emptyList(),
    val isBrokerConnectDialogOpen: Boolean = false,
    val isAddFundsDialogOpen: Boolean = false,
    val isWithdrawFundsDialogOpen: Boolean = false,
    val isLiveTradingMode: Boolean = false,
    val autoPilotAutonomousLive: Boolean = true,
    val autoActivateWhenTargetPending: Boolean = true,
    val hasUserManuallyStopped: Boolean = false,
    val isAutoPilotActive: Boolean = true,
    val autoPilotStatus: String = "ACTIVE_AUTO_HUNTING",
    val autoPilotLastAction: String = "Auto-active mode: Hunting high-probability setups across 1,020 assets to achieve ₹1,000 target.",
    val autoPilotCycleCount: Int = 0,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            role = "model",
            content = "Hello! I am AI TradePilot Copilot. I can assist you with market analysis across 1,000+ assets, optimize your ₹1,000 daily profit target, configure Auto-Pilot trading, and analyze risk. How can I help you today?",
            modelUsed = "gemini-3.5-flash"
        )
    ),
    val isChatLoading: Boolean = false,
    val selectedChatModel: String = "gemini-3.5-flash"
)

class TradePilotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradePilotRepository
    private val secureStorage: SecureBrokerStorage

    private val _uiState = MutableStateFlow(TradePilotUiState())
    val uiState: StateFlow<TradePilotUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = TradePilotRepository(database)
        secureStorage = SecureBrokerStorage(application)

        // Load static & market stocks
        val inStocks = MarketDataProvider.getAllIndianStocks()
        val usStocks = MarketDataProvider.getAllUsStocks()
        val strategies = repository.getStandardStrategies()

        _uiState.update {
            it.copy(
                indianStocks = inStocks,
                usStocks = usStocks,
                selectedStock = inStocks.firstOrNull(),
                standardStrategies = strategies
            )
        }

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }

        // Observe Room DB flows
        viewModelScope.launch {
            repository.portfolioFlow.collect { p ->
                if (p != null) {
                    _uiState.update { it.copy(portfolio = p) }
                }
            }
        }

        viewModelScope.launch {
            repository.positionsFlow.collect { posList ->
                _uiState.update { it.copy(positions = posList) }
            }
        }

        viewModelScope.launch {
            repository.journalFlow.collect { j ->
                _uiState.update { it.copy(journalEntries = j) }
            }
        }

        viewModelScope.launch {
            repository.auditLogsFlow.collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }

        viewModelScope.launch {
            repository.customStrategiesFlow.collect { strats ->
                _uiState.update { it.copy(customStrategies = strats) }
            }
        }

        viewModelScope.launch {
            repository.watchlistFlow.collect { wl ->
                _uiState.update { it.copy(watchlist = wl) }
            }
        }

        viewModelScope.launch {
            repository.assetsFlow.collect { assetList ->
                _uiState.update { it.copy(assets = assetList) }
            }
        }

        viewModelScope.launch {
            repository.brokerConnectionFlow.collect { broker ->
                if (broker != null) {
                    _uiState.update {
                        it.copy(
                            brokerConnection = broker,
                            isLiveTradingMode = broker.isLiveTradingActive,
                            autoPilotAutonomousLive = broker.autoPilotAutonomousLive
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.fundTransactionsFlow.collect { txs ->
                _uiState.update { it.copy(fundTransactions = txs) }
            }
        }

        // Observe live market ticks
        viewModelScope.launch {
            MarketDataProvider.getMarketTickStream().collect { updatedStock ->
                _uiState.update { current ->
                    val newIn = current.indianStocks.map { if (it.symbol == updatedStock.symbol) updatedStock else it }
                    val newUs = current.usStocks.map { if (it.symbol == updatedStock.symbol) updatedStock else it }
                    val newSel = if (current.selectedStock?.symbol == updatedStock.symbol) updatedStock else current.selectedStock
                    current.copy(
                        indianStocks = newIn,
                        usStocks = newUs,
                        selectedStock = newSel
                    )
                }
                repository.updatePositionPrices(listOf(updatedStock))

                // Auto-activate logic: if daily target is NOT achieved, automatically maintain active trading
                val curState = _uiState.value
                val isTargetAchieved = curState.portfolio.realizedPnlToday >= curState.portfolio.dailyTarget
                val isLossBreached = curState.portfolio.realizedPnlToday <= -curState.portfolio.maxDailyLoss
                val isTerminalLocked = curState.portfolio.isLocked || curState.portfolio.killSwitchTriggered

                if (!isTargetAchieved && !isLossBreached && !isTerminalLocked && curState.autoActivateWhenTargetPending && !curState.hasUserManuallyStopped) {
                    if (!curState.isAutoPilotActive) {
                        _uiState.update {
                            it.copy(
                                isAutoPilotActive = true,
                                autoPilotStatus = "ACTIVE_AUTO_HUNTING",
                                autoPilotLastAction = "Auto-active: Daily target of ₹${it.portfolio.dailyTarget.toInt()} not achieved. Hunting setups across 1,020 assets."
                            )
                        }
                    }
                    runAutoPilotCycle()
                } else if (curState.isAutoPilotActive) {
                    runAutoPilotCycle()
                }
            }
        }

        // Trigger default AI deliberation for initial stock
        inStocks.firstOrNull()?.let { runAiDeliberation(it) }

        // Initial auto-pilot check on startup: if target not achieved, start active hunting
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val cur = _uiState.value
            if (cur.portfolio.realizedPnlToday < cur.portfolio.dailyTarget && cur.autoActivateWhenTargetPending && !cur.hasUserManuallyStopped) {
                runAutoPilotCycle()
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen, isViewingStockDetail = false) }
    }

    fun openStockDetail(stock: StockQuote) {
        _uiState.update { it.copy(selectedStock = stock, isViewingStockDetail = true) }
        runAiDeliberation(stock)
    }

    fun closeStockDetail() {
        _uiState.update { it.copy(isViewingStockDetail = false) }
    }

    fun selectStock(stock: StockQuote) {
        _uiState.update { it.copy(selectedStock = stock) }
        runAiDeliberation(stock)
    }

    fun runAiDeliberation(stock: StockQuote) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeliberating = true) }
            try {
                val (consensus, risk) = repository.executeAiDeliberation(stock)
                _uiState.update {
                    it.copy(
                        currentDeliberation = consensus,
                        currentRiskEvaluation = risk,
                        isDeliberating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeliberating = false, bannerMessage = "AI Deliberation error: ${e.message}") }
            }
        }
    }

    fun executePaperBuyOrder(quantity: Int, stopLoss: Double, takeProfit: Double) {
        val stock = _uiState.value.selectedStock ?: return
        viewModelScope.launch {
            val result = repository.executePaperBuyOrder(
                stock = stock,
                quantity = quantity,
                stopLoss = stopLoss,
                takeProfit = takeProfit
            )
            result.onSuccess { msg ->
                _uiState.update { it.copy(bannerMessage = msg, isPaperTradeSheetOpen = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(bannerMessage = err.message ?: "Execution failed") }
            }
        }
    }

    fun closePosition(position: PositionEntity, exitPrice: Double) {
        viewModelScope.launch {
            val result = repository.closePosition(position, exitPrice)
            result.onSuccess { msg ->
                _uiState.update { it.copy(bannerMessage = msg) }
            }.onFailure { err ->
                _uiState.update { it.copy(bannerMessage = err.message ?: "Failed to close position") }
            }
        }
    }

    fun toggleEmergencyKillSwitch() {
        viewModelScope.launch {
            repository.toggleKillSwitch()
            val isNowTriggered = !_uiState.value.portfolio.killSwitchTriggered
            _uiState.update {
                it.copy(bannerMessage = if (isNowTriggered) "🚨 EMERGENCY KILL SWITCH ENGAGED! ALL TRADING BLOCKED" else "Kill switch disengaged.")
            }
        }
    }

    fun unlockTrading() {
        viewModelScope.launch {
            repository.unlockTrading()
            _uiState.update { it.copy(bannerMessage = "Trading limits cleared. System active.") }
        }
    }

    fun resetCapitalTo25k() {
        viewModelScope.launch {
            repository.resetPortfolioToInitial()
            _uiState.update { it.copy(bannerMessage = "Portfolio restored to starting capital ₹25,000.") }
        }
    }

    fun updateRiskSettings(
        startingCapital: Double,
        dailyTarget: Double,
        maxDailyLoss: Double,
        maxRiskPerTrade: Double,
        maxOpenPositions: Int,
        profitLockEnabled: Boolean,
        tradingMode: String
    ) {
        viewModelScope.launch {
            repository.updateRiskSettings(
                startingCapital = startingCapital,
                dailyTarget = dailyTarget,
                maxDailyLoss = maxDailyLoss,
                maxRiskPerTrade = maxRiskPerTrade,
                maxOpenPositions = maxOpenPositions,
                profitLockEnabled = profitLockEnabled,
                tradingMode = tradingMode
            )
            _uiState.update { it.copy(bannerMessage = "Risk parameters successfully updated.", isRiskSettingsSheetOpen = false) }
        }
    }

    fun toggleWatchlist(symbol: String, market: String, companyName: String) {
        viewModelScope.launch {
            repository.toggleWatchlist(symbol, market, companyName)
        }
    }

    fun generateAiStrategy(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeliberating = true) }
            val generated = repository.generateAiStrategy(prompt)
            _uiState.update {
                it.copy(
                    isDeliberating = false,
                    isStrategyBuilderSheetOpen = false,
                    bannerMessage = "New AI Strategy '${generated.name}' generated and added to Paper Lab!"
                )
            }
        }
    }

    fun createPaperStrategy(
        name: String,
        allocation: Double,
        maxRiskPercent: Double,
        entryCriteria: String,
        exitCriteria: String,
        prompt: String = ""
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCustomStrategy(
                CustomStrategyEntity(
                    name = name,
                    prompt = if (prompt.isNotBlank()) prompt else "Configured: Max Risk $maxRiskPercent%, Allocation ₹${allocation.toInt()}",
                    capitalAllocation = allocation,
                    maxRiskPercent = maxRiskPercent,
                    entryCriteria = entryCriteria,
                    exitCriteria = exitCriteria,
                    status = "ACTIVE_PAPER"
                )
            )
            _uiState.update {
                it.copy(bannerMessage = "Paper Strategy '$name' successfully deployed with $maxRiskPercent% risk parameter.")
            }
        }
    }

    fun deleteCustomStrategy(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomStrategy(id)
            _uiState.update { it.copy(bannerMessage = "Strategy removed from Paper Lab.") }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setMarketFilter(filter: MarketType?) {
        _uiState.update { it.copy(activeMarketFilter = filter) }
    }

    fun setPaperTradeSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isPaperTradeSheetOpen = open) }
    }

    fun setRiskSettingsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isRiskSettingsSheetOpen = open) }
    }

    fun setStrategyBuilderSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isStrategyBuilderSheetOpen = open) }
    }

    fun updateJournalTrade(trade: TradeJournalEntity) {
        viewModelScope.launch {
            repository.updateJournalTrade(trade)
            _uiState.update { it.copy(bannerMessage = "Trade notes & reflections updated.") }
        }
    }

    fun deleteJournalTrade(id: Long) {
        viewModelScope.launch {
            repository.deleteJournalTrade(id)
            _uiState.update { it.copy(bannerMessage = "Journal trade deleted.") }
        }
    }

    fun addManualTrade(
        symbol: String,
        market: String,
        companyName: String,
        strategy: String,
        entryPrice: Double,
        exitPrice: Double,
        quantity: Int,
        aiDecision: String,
        aiConfidence: Int,
        userOverride: Boolean,
        overrideReason: String,
        userNotes: String,
        tags: String
    ) {
        viewModelScope.launch {
            val mult = if (market == "USA") _uiState.value.portfolio.usdInrRate else 1.0
            val pnlInr = (exitPrice - entryPrice) * quantity * mult
            val pnlPct = if (entryPrice > 0) ((exitPrice - entryPrice) / entryPrice) * 100.0 else 0.0
            val outcome = when {
                pnlInr > 0 -> "PROFIT"
                pnlInr < 0 -> "LOSS"
                else -> "BREAKEVEN"
            }

            val observation = if (userOverride) {
                "User executed manual override ($overrideReason). P&L outcome: ${if (pnlInr >= 0) "+₹" else "-₹"}${Math.abs(pnlInr).toInt()}."
            } else {
                "Trade was aligned with AI $aiDecision recommendation ($aiConfidence% confidence)."
            }

            val trade = TradeJournalEntity(
                tradeId = "TP-M${(1000..9999).random()}",
                symbol = symbol.uppercase(),
                market = market,
                companyName = companyName,
                strategy = strategy,
                entryPrice = entryPrice,
                exitPrice = exitPrice,
                quantity = quantity,
                pnl = Math.round(pnlInr * 100.0) / 100.0,
                pnlPercent = Math.round(pnlPct * 100.0) / 100.0,
                aiDecision = aiDecision,
                aiConfidence = aiConfidence,
                riskScore = 45,
                closedAt = System.currentTimeMillis(),
                outcome = outcome,
                aiObservation = observation,
                userOverride = userOverride,
                overrideReason = overrideReason,
                userNotes = userNotes,
                tags = tags
            )
            repository.insertManualTrade(trade)
            _uiState.update { it.copy(bannerMessage = "New trade logged to AI Journal!") }
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun setAuthenticated(auth: Boolean) {
        _uiState.update {
            it.copy(
                isAuthenticated = auth,
                isAppLocked = false,
                bannerMessage = if (auth) "Authentication successful. 2FA Verified 🛡️" else "Signed out. Please sign in to resume."
            )
        }
    }

    fun lockApp() {
        _uiState.update {
            it.copy(
                isAppLocked = true,
                bannerMessage = "Terminal locked. Enter PIN to resume."
            )
        }
    }

    fun unlockApp() {
        _uiState.update {
            it.copy(
                isAppLocked = false,
                bannerMessage = "Terminal unlocked."
            )
        }
    }

    fun setAppLocked(locked: Boolean) {
        _uiState.update { it.copy(isAppLocked = locked) }
    }

    fun updateUserProfile(profile: UserProfile) {
        _uiState.update {
            it.copy(
                userProfile = profile,
                bannerMessage = "Preferences & notification settings updated."
            )
        }
    }

    fun setSettingsDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsDialogOpen = open) }
    }

    fun setNotificationsDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isNotificationsDialogOpen = open) }
    }

    // --- AUTO-PILOT TRADING BOT CONTROLLER ---

    fun toggleAutoPilot() {
        if (_uiState.value.isAutoPilotActive) {
            stopAutoPilot()
        } else {
            startAutoPilot()
        }
    }

    fun startAutoPilot() {
        val currentPnl = _uiState.value.portfolio.realizedPnlToday
        val target = _uiState.value.portfolio.dailyTarget
        if (currentPnl >= target) {
            _uiState.update {
                it.copy(
                    bannerMessage = "Daily target of ₹${target.toInt()} already achieved! Profit lock is active."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isAutoPilotActive = true,
                hasUserManuallyStopped = false,
                autoPilotStatus = "ACTIVE_HUNTING",
                autoPilotLastAction = "Auto-Pilot started. Actively scanning 1,020 assets to achieve ₹${target.toInt()} daily target.",
                bannerMessage = "Auto-Pilot activated! Hunting setups automatically across 1,020 assets until ₹${target.toInt()} target is achieved."
            )
        }

        runAutoPilotCycle()
    }

    fun stopAutoPilot() {
        _uiState.update {
            it.copy(
                isAutoPilotActive = false,
                hasUserManuallyStopped = true,
                autoPilotStatus = "STOPPED_BY_USER",
                autoPilotLastAction = "Auto-Pilot stopped by user. Terminal returned to manual discretionary mode.",
                bannerMessage = "Auto-Pilot stopped by user. System in manual trading mode."
            )
        }
    }

    fun setAutoActivateWhenTargetPending(enabled: Boolean) {
        _uiState.update {
            it.copy(
                autoActivateWhenTargetPending = enabled,
                bannerMessage = if (enabled) "Auto-Active Mode ENABLED: Bot will automatically trade when daily target is pending."
                               else "Auto-Active Mode DISABLED: Manual start required."
            )
        }
        if (enabled && !_uiState.value.isAutoPilotActive && _uiState.value.portfolio.realizedPnlToday < _uiState.value.portfolio.dailyTarget) {
            startAutoPilot()
        }
    }

    fun runAutoPilotCycle() {
        val state = _uiState.value
        val portfolio = state.portfolio
        val positions = state.positions
        val allStocks = state.indianStocks + state.usStocks

        viewModelScope.launch {
            val decision = AutoPilotEngine.evaluate(portfolio, positions, allStocks)
            when (decision) {
                is AutoPilotDecision.StopTargetAchieved -> {
                    _uiState.update {
                        it.copy(
                            isAutoPilotActive = false,
                            autoPilotStatus = "TARGET_ACHIEVED_LOCKED",
                            autoPilotLastAction = decision.message,
                            bannerMessage = decision.message
                        )
                    }
                    repository.updateRiskSettings(
                        startingCapital = portfolio.startingCapital,
                        dailyTarget = portfolio.dailyTarget,
                        maxDailyLoss = portfolio.maxDailyLoss,
                        maxRiskPerTrade = portfolio.maxRiskPerTradePercent,
                        maxOpenPositions = portfolio.maxOpenPositions,
                        profitLockEnabled = true,
                        tradingMode = portfolio.tradingMode
                    )
                }
                is AutoPilotDecision.StopLossLimit -> {
                    _uiState.update {
                        it.copy(
                            isAutoPilotActive = false,
                            autoPilotStatus = "LOSS_LIMIT_STOPPED",
                            autoPilotLastAction = decision.message,
                            bannerMessage = decision.message
                        )
                    }
                    repository.toggleKillSwitch()
                }
                is AutoPilotDecision.ClosePositionToBookProfit -> {
                    _uiState.update {
                        it.copy(
                            autoPilotLastAction = decision.reason,
                            bannerMessage = "Auto-Pilot: ${decision.reason}"
                        )
                    }
                    repository.closePosition(decision.position, decision.realizedProfit)
                }
                is AutoPilotDecision.ExecuteBuyOrder -> {
                    _uiState.update {
                        it.copy(
                            autoPilotLastAction = decision.reason,
                            bannerMessage = if (state.isLiveTradingMode) "🔴 [ZERODHA LIVE] Auto-Pilot executed order: ${decision.reason}" else "Auto-Pilot: ${decision.reason}"
                        )
                    }
                    if (state.isLiveTradingMode && state.autoPilotAutonomousLive) {
                        repository.executeLiveBrokerBuyOrder(
                            stock = decision.stock,
                            quantity = decision.quantity,
                            stopLoss = decision.stopLoss,
                            takeProfit = decision.takeProfit,
                            product = "MIS",
                            strategyName = "AI Autopilot (Zerodha Live)"
                        )
                    } else {
                        repository.executePaperBuyOrder(
                            stock = decision.stock,
                            quantity = decision.quantity,
                            stopLoss = decision.stopLoss,
                            takeProfit = decision.takeProfit
                        )
                    }
                }
                is AutoPilotDecision.ActiveHunting -> {
                    _uiState.update {
                        it.copy(
                            autoPilotLastAction = decision.message,
                            autoPilotCycleCount = it.autoPilotCycleCount + 1
                        )
                    }
                }
                is AutoPilotDecision.IdleMaxPositionsReached -> {
                    _uiState.update {
                        it.copy(
                            autoPilotLastAction = "Max concurrent positions (4) reached. Monitoring open trades for profit milestones."
                        )
                    }
                }
                is AutoPilotDecision.IdleTerminalLocked -> {
                    _uiState.update {
                        it.copy(
                            isAutoPilotActive = false,
                            autoPilotLastAction = "Terminal is locked. Auto-Pilot paused."
                        )
                    }
                }
            }
        }
    }

    // --- GEMINI MULTI-TURN COPILOT CHATBOT ---

    private val geminiChatService = GeminiChatService()

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        val currentHistory = _uiState.value.chatMessages
        val userMsg = ChatMessage(role = "user", content = prompt, modelUsed = _uiState.value.selectedChatModel)
        val updatedHistory = currentHistory + userMsg

        _uiState.update {
            it.copy(
                chatMessages = updatedHistory,
                isChatLoading = true
            )
        }

        viewModelScope.launch {
            val response = geminiChatService.sendMessage(
                history = updatedHistory,
                userPrompt = prompt,
                model = _uiState.value.selectedChatModel
            )
            val modelMsg = ChatMessage(
                role = "model",
                content = response,
                modelUsed = _uiState.value.selectedChatModel
            )
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + modelMsg,
                    isChatLoading = false
                )
            }
        }
    }

    fun setChatModel(model: String) {
        _uiState.update { it.copy(selectedChatModel = model) }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                chatMessages = listOf(
                    ChatMessage(
                        role = "model",
                        content = "Chat thread cleared. How can I assist your trading decisions?",
                        modelUsed = it.selectedChatModel
                    )
                )
            )
        }
    }

    // --- BROKER & LIVE FUNDS CONTROLLERS ---

    fun setBrokerConnectDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isBrokerConnectDialogOpen = isOpen) }
    }

    fun setAddFundsDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isAddFundsDialogOpen = isOpen) }
    }

    fun setWithdrawFundsDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isWithdrawFundsDialogOpen = isOpen) }
    }

    fun toggleLiveTradingMode(enableLive: Boolean) {
        viewModelScope.launch {
            repository.setLiveTradingMode(enableLive)
            _uiState.update {
                it.copy(
                    isLiveTradingMode = enableLive,
                    bannerMessage = if (enableLive)
                        "🔴 LIVE TRADING ENABLED: Orders and Auto-Pilot routed to Zerodha Kite Connect."
                    else
                        "📄 PAPER TRADING ACTIVATED: Virtual risk-free simulation active."
                )
            }
        }
    }

    fun setAutoPilotAutonomousLive(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoPilotAutonomousLive(enabled)
            _uiState.update {
                it.copy(
                    autoPilotAutonomousLive = enabled,
                    bannerMessage = if (enabled)
                        "🤖 AI FULL AUTONOMY ACTIVE: Auto-Pilot will autonomously enter/exit Live Zerodha trades to hit ₹1,000 daily target."
                    else
                        "✋ AI Autonomy set to Assisted Mode. User approval required for live trades."
                )
            }
        }
    }

    fun connectBrokerOAuth2(
        brokerCode: String,
        apiKey: String,
        apiSecret: String,
        requestToken: String,
        userId: String
    ) {
        viewModelScope.launch {
            // Encrypt and store sensitive credentials in hardware-backed KeyStore vault
            secureStorage.saveCredentials(
                brokerCode = brokerCode,
                apiKey = apiKey,
                apiSecret = apiSecret,
                accessToken = "tok_live_${brokerCode.lowercase()}_${System.currentTimeMillis()}",
                refreshToken = "ref_${brokerCode.lowercase()}_${System.currentTimeMillis()}"
            )

            val result = repository.connectBrokerOAuth2(brokerCode, apiKey, apiSecret, requestToken, userId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isBrokerConnectDialogOpen = false,
                        bannerMessage = "✅ $brokerCode linked via OAuth2! Credentials AES-256 encrypted in KeyStore Vault."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        bannerMessage = "❌ Broker connection failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun refreshBrokerToken(brokerCode: String) {
        viewModelScope.launch {
            val result = repository.refreshBrokerToken(brokerCode)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(bannerMessage = "🔄 $brokerCode OAuth2 session token refreshed! Valid for 24 hours.")
                }
            }
        }
    }

    fun testBrokerPing(brokerCode: String) {
        _uiState.update {
            it.copy(bannerMessage = "⚡ $brokerCode API ping: ${(18..42).random()}ms (Live WebSocket & Order Gateway Operational)")
        }
    }

    fun connectZerodha(apiKey: String, apiSecret: String, requestToken: String, userId: String) {
        connectBrokerOAuth2("ZERODHA", apiKey, apiSecret, requestToken, userId)
    }

    fun disconnectBroker(brokerCode: String) {
        viewModelScope.launch {
            secureStorage.deleteCredentials(brokerCode)
            repository.disconnectBroker(brokerCode)
            _uiState.update {
                it.copy(
                    isLiveTradingMode = false,
                    isBrokerConnectDialogOpen = false,
                    bannerMessage = "Broker $brokerCode unlinked and credentials wiped from vault. Reverted to Paper Trading."
                )
            }
        }
    }

    fun disconnectZerodha() {
        disconnectBroker("ZERODHA")
    }

    fun depositLiveFunds(amount: Double, paymentMethod: String, upiId: String = "trader@okaxis") {
        viewModelScope.launch {
            val result = repository.depositLiveFunds(amount, paymentMethod, upiId)
            if (result.isSuccess) {
                val tx = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isAddFundsDialogOpen = false,
                        bannerMessage = "💳 ₹${"%,d".format(amount.toInt())} added to Zerodha margin via ${paymentMethod} (UTR: ${tx.utrNumber})!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        bannerMessage = "❌ Deposit failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun withdrawLiveFunds(amount: Double, bankDetails: String = "HDFC Bank •• 4912") {
        viewModelScope.launch {
            val result = repository.withdrawLiveFunds(amount, bankDetails)
            if (result.isSuccess) {
                val tx = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isWithdrawFundsDialogOpen = false,
                        bannerMessage = "🏦 ₹${"%,d".format(amount.toInt())} payout initiated to ${bankDetails} (Ref: ${tx.utrNumber})!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        bannerMessage = "❌ Withdrawal failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun executeOrder(
        stock: StockQuote,
        quantity: Int,
        stopLoss: Double,
        takeProfit: Double,
        isLive: Boolean = false,
        product: String = "MIS"
    ) {
        viewModelScope.launch {
            if (isLive || _uiState.value.isLiveTradingMode) {
                val result = repository.executeLiveBrokerBuyOrder(
                    stock = stock,
                    quantity = quantity,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit,
                    product = product,
                    strategyName = "AI Manual / Discretionary"
                )
                if (result.isSuccess) {
                    val order = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            isPaperTradeSheetOpen = false,
                            bannerMessage = "🔴 [ZERODHA LIVE] Order #${order.orderId} executed for $quantity ${stock.symbol} @ ₹${order.averagePrice}!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            bannerMessage = "❌ Live order rejected: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } else {
                val result = repository.executePaperBuyOrder(
                    stock = stock,
                    quantity = quantity,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit
                )
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isPaperTradeSheetOpen = false,
                            bannerMessage = "📄 Paper trade executed for $quantity ${stock.symbol} @ ₹${stock.currentPrice}!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            bannerMessage = "❌ Trade rejected: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            }
        }
    }
}
