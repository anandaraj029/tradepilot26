package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core table: portfolios
 * Tracks portfolio capital, cash reserves, configurable daily target (₹1,000),
 * loss limit (₹500), risk limits, and lock states.
 */
@Entity(tableName = "portfolios")
data class PortfolioEntity(
    @PrimaryKey val id: Int = 1,
    val startingCapital: Double = 25000.0,
    val availableCash: Double = 25000.0,
    val dailyTarget: Double = 1000.0,
    val maxDailyLoss: Double = 500.0,
    val maxRiskPerTradePercent: Double = 1.0,
    val maxOpenPositions: Int = 4,
    val realizedPnlToday: Double = 0.0,
    val isLocked: Boolean = false,
    val lockReason: String = "",
    val killSwitchTriggered: Boolean = false,
    val tradingMode: String = "PAPER_TRADING",
    val profitLockEnabled: Boolean = true,
    val usdInrRate: Double = 83.5,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Core table: assets
 * Stores tracked instruments across India (NSE/BSE) and USA (NYSE/NASDAQ),
 * technical levels (support/resistance/RSI/MACD), and market metadata.
 */
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val market: String, // "INDIA", "USA"
    val exchange: String = "NSE", // "NSE", "BSE", "NASDAQ", "NYSE"
    val assetClass: String = "EQUITY", // "EQUITY", "INDEX", "ETF", "COMMODITY"
    val currentPrice: Double,
    val priceChange: Double = 0.0,
    val priceChangePercent: Double = 0.0,
    val volume: String = "",
    val high52: Double = 0.0,
    val low52: Double = 0.0,
    val peRatio: Double = 0.0,
    val rsi: Double = 50.0,
    val macd: Double = 0.0,
    val support: Double = 0.0,
    val resistance: Double = 0.0,
    val sector: String = "",
    val currency: String = "INR",
    val isWatchlisted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Core table: trades
 * Complete execution and post-mortem trade record with prices, P&L,
 * AI multi-agent decision and confidence, user overrides, and outcomes.
 */
@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: String,
    val symbol: String,
    val market: String,
    val companyName: String,
    val strategy: String,
    val entryPrice: Double,
    val exitPrice: Double = 0.0,
    val quantity: Int,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val aiDecision: String = "BUY",
    val aiConfidence: Int = 75,
    val riskScore: Int = 50,
    val status: String = "CLOSED", // "OPEN", "CLOSED", "CANCELLED"
    val orderType: String = "BUY", // "BUY", "SELL"
    val executionType: String = "PAPER", // "PAPER", "LIVE"
    val entryTime: Long = System.currentTimeMillis() - 86400000,
    val closedAt: Long = System.currentTimeMillis(),
    val outcome: String = "PROFIT", // "PROFIT", "LOSS", "BREAKEVEN"
    val aiObservation: String = "",
    val userOverride: Boolean = false,
    val overrideReason: String = "",
    val userNotes: String = "",
    val tags: String = "Followed Plan",
    val changeFactors: String = "",
    val invalidationTriggers: String = ""
)

/**
 * Typealias for backward compatibility with trade journal UI & repository.
 */
typealias TradeJournalEntity = TradeEntity

/**
 * Core table: audit_logs
 * Immutable trail of AI consensus decisions, deterministic risk engine evaluations,
 * and execution orders.
 */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val symbol: String,
    val market: String,
    val aiConsensusVerdict: String,
    val riskEngineStatus: String,
    val details: String
)

/**
 * Active open positions currently held in paper or broker execution.
 */
@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val market: String,
    val companyName: String,
    val quantity: Int,
    val buyPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Custom user/AI generated strategies configured in the Strategy Lab.
 */
@Entity(tableName = "custom_strategies")
data class CustomStrategyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val prompt: String,
    val capitalAllocation: Double,
    val maxRiskPercent: Double,
    val entryCriteria: String,
    val exitCriteria: String,
    val status: String = "PAPER_TRADING_ONLY",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * User watchlist for quick market tracking.
 */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val market: String,
    val companyName: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Live Broker Account connection and margin allocation.
 */
@Entity(tableName = "broker_connections")
data class BrokerConnectionEntity(
    @PrimaryKey val id: Int = 1,
    val brokerName: String = "Zerodha Kite Connect v3",
    val brokerCode: String = "ZERODHA", // "ZERODHA", "ANGEL_ONE", "UPSTOX", "DHAN"
    val apiKey: String = "kite_prod_live_key_994",
    val apiSecret: String = "••••••••••••••••",
    val requestToken: String = "req_tok_99184",
    val accessToken: String = "kite_acc_tok_98241029",
    val userId: String = "ZL9824",
    val userName: String = "AI TradePilot Live",
    val isConnected: Boolean = true,
    val liveEquityMargin: Double = 50000.0,
    val usedMargin: Double = 0.0,
    val availableCollateral: Double = 25000.0,
    val isLiveTradingActive: Boolean = false, // false = Paper mode, true = Live Broker mode
    val autoPilotAutonomousLive: Boolean = true, // When true, AI takes full autonomous responsibility
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Live fund transfers / deposits / withdrawals.
 */
@Entity(tableName = "fund_transactions")
data class FundTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txId: String,
    val type: String, // "DEPOSIT", "WITHDRAWAL"
    val amount: Double,
    val paymentMethod: String, // "Instant UPI (PhonePe / GPay)", "NetBanking", "Bank IMPS"
    val utrNumber: String,
    val status: String = "SUCCESS",
    val timestamp: Long = System.currentTimeMillis(),
    val brokerName: String = "Zerodha Kite",
    val notes: String = "Live margin top-up"
)

