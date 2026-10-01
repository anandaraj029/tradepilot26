package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Broker platforms supported for live execution.
 */
enum class BrokerType(
    val displayName: String,
    val shortName: String,
    val apiDocsUrl: String,
    val defaultUserId: String
) {
    ZERODHA("Zerodha Kite Connect v3", "Zerodha Kite", "https://kite.trade/docs/connect/v3", "ZL9824"),
    ANGEL_ONE("Angel One SmartAPI", "Angel One", "https://smartapi.angelbroking.com", "AO5510"),
    UPSTOX("Upstox Pro API v2", "Upstox", "https://upstox.com/developer/api-documentation", "UP7102"),
    DHAN("DhanHQ API v2", "Dhan", "https://dhanhq.co/docs", "DH4491"),
    GROWW("Groww Brokerage API", "Groww", "https://groww.in/developer", "GR2034"),
    IBKR("Interactive Brokers", "IBKR", "https://interactivebrokers.github.io/cpwebapi/", "U99281")
}

/**
 * Live Broker Account connection parameters and margin telemetry.
 */
data class BrokerAccountState(
    val brokerType: BrokerType = BrokerType.ZERODHA,
    val apiKey: String = "kite_prod_live_key_994",
    val apiSecret: String = "••••••••••••••••",
    val requestToken: String = "req_tok_99184",
    val accessToken: String = "kite_acc_tok_98241029",
    val userId: String = "ZL9824",
    val userName: String = "AI TradePilot Live Trader",
    val userEmail: String = "ebdc.org@gmail.com",
    val isConnected: Boolean = true,
    val isLiveTradingMode: Boolean = false, // false = Paper Trading, true = Real Broker Live Trading
    val autoPilotAutonomousLive: Boolean = true, // When true, AutoPilot acts autonomously on Live Broker
    val availableCash: Double = 50000.0, // Real cash margin available in broker account
    val usedMargin: Double = 0.0,
    val availableCollateral: Double = 25000.0,
    val openingBalance: Double = 50000.0,
    val todayRealizedPnl: Double = 0.0,
    val todayUnrealizedPnl: Double = 0.0,
    val latencyMs: Long = 18L,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Live Order placement request payload conforming to Zerodha Kite Connect v3 REST API.
 */
data class LiveOrderRequest(
    val variety: String = "regular", // "regular", "amo", "co", "iceberg"
    val tradingSymbol: String,
    val exchange: String = "NSE", // "NSE", "BSE", "NFO"
    val transactionType: String, // "BUY", "SELL"
    val orderType: String = "MARKET", // "MARKET", "LIMIT", "SL", "SL-M"
    val quantity: Int,
    val product: String = "MIS", // "MIS" (Intraday with leverage) or "CNC" (Delivery)
    val price: Double = 0.0,
    val triggerPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val validity: String = "DAY",
    val tag: String = "TradePilotAI"
)

/**
 * Result returned after order routing through Kite Connect API.
 */
data class LiveOrderResponse(
    val orderId: String,
    val exchangeOrderId: String,
    val status: String, // "COMPLETE", "TRIGGER PENDING", "REJECTED", "CANCELLED"
    val tradingSymbol: String,
    val transactionType: String,
    val quantity: Int,
    val averagePrice: Double,
    val brokerageFee: Double,
    val sttTax: Double,
    val totalCharges: Double,
    val placedAt: Long = System.currentTimeMillis(),
    val broker: String = "Zerodha Kite Connect",
    val statusMessage: String = "Order executed successfully on NSE"
)

/**
 * Detailed Indian regulatory & broker charge breakdown.
 */
data class IndianBrokerageCharges(
    val turnover: Double,
    val brokerage: Double,
    val sttCtt: Double,
    val exchangeTurnoverCharges: Double,
    val sebiTurnoverCharges: Double,
    val stampDuty: Double,
    val gst: Double,
    val totalTaxAndCharges: Double,
    val breakevenPoints: Double
)

/**
 * Fund transfer / Deposit / Withdrawal ledger record.
 */
data class FundTransaction(
    val id: Long = 0,
    val txId: String,
    val type: String, // "DEPOSIT", "WITHDRAWAL"
    val amount: Double,
    val paymentMethod: String, // "Instant UPI (PhonePe / GPay / BHIM)", "NetBanking (HDFC / SBI / ICICI)", "IMPS/NEFT"
    val utrNumber: String,
    val status: String = "SUCCESS", // "SUCCESS", "PENDING", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val brokerName: String = "Zerodha Kite",
    val notes: String = "Live Margin Credit"
)

enum class BrokerConnectionStatus(
    val title: String,
    val description: String
) {
    ACTIVE("Active & Connected", "Live market feed and execution pipeline operational"),
    DISCONNECTED("Disconnected", "OAuth2 session inactive. Tap to authenticate"),
    TOKEN_EXPIRING("Token Expiring", "OAuth2 token expires soon. Refresh recommended"),
    AUTHENTICATING("Authenticating", "Completing OAuth2 authorization code handshake...")
}

data class BrokerDefinition(
    val code: String,
    val name: String,
    val tagline: String,
    val authType: String,
    val defaultScopes: List<String>,
    val oauthAuthUrl: String,
    val oauthTokenUrl: String,
    val docUrl: String,
    val isPrimaryInIndia: Boolean = true
)

data class LiveBrokerItem(
    val code: String,
    val name: String,
    val tagline: String,
    val authType: String,
    val isConnected: Boolean,
    val isLiveRoutingActive: Boolean,
    val userId: String,
    val userName: String,
    val liveMargin: Double,
    val availableCollateral: Double,
    val latencyMs: Int,
    val tokenExpiresAt: Long,
    val isEncryptedInVault: Boolean,
    val scopes: List<String>,
    val status: BrokerConnectionStatus
)

object SupportedBrokersCatalog {
    val definitions = listOf(
        BrokerDefinition(
            code = "ZERODHA",
            name = "Zerodha Kite Connect v3",
            tagline = "India's largest retail broker • Superfast HTTP & WebSocket APIs",
            authType = "OAuth2 Authorization Code",
            defaultScopes = listOf("orders:create", "orders:cancel", "portfolio:read", "market:quotes", "funds:read"),
            oauthAuthUrl = "https://kite.zerodha.com/connect/login?v=3",
            oauthTokenUrl = "https://api.kite.trade/session/token",
            docUrl = "https://kite.trade/docs/connect/v3/"
        ),
        BrokerDefinition(
            code = "ANGEL_ONE",
            name = "Angel One SmartAPI",
            tagline = "Institutional algorithmic execution • SmartAPI OAuth2 & TOTP",
            authType = "OAuth2 + TOTP MFA",
            defaultScopes = listOf("trade:write", "position:read", "historic:feed", "orderbook:sync"),
            oauthAuthUrl = "https://smartapi.angelbroking.com/publisher-login",
            oauthTokenUrl = "https://apiconnect.angelbroking.com/rest/auth/partner/token",
            docUrl = "https://smartapi.angelbroking.com/docs"
        ),
        BrokerDefinition(
            code = "UPSTOX",
            name = "Upstox Pro API v2",
            tagline = "Ultra-low latency OpenAPI • Complete OAuth2 flow",
            authType = "OAuth2 Authorization Code",
            defaultScopes = listOf("orders:write", "holdings:read", "market:stream"),
            oauthAuthUrl = "https://api.upstox.com/v2/login/authorization/dialog",
            oauthTokenUrl = "https://api.upstox.com/v2/login/authorization/token",
            docUrl = "https://upstox.com/developer/api-documentation"
        ),
        BrokerDefinition(
            code = "DHAN",
            name = "DhanHQ Superfast v2",
            tagline = "Direct exchange routing • Microsecond DMA execution",
            authType = "OAuth2 Access Token",
            defaultScopes = listOf("orders:dma", "portfolio:sync", "margin:realtime"),
            oauthAuthUrl = "https://auth.dhan.co/login/consent",
            oauthTokenUrl = "https://api.dhan.co/v2/token",
            docUrl = "https://dhanhq.co/docs"
        ),
        BrokerDefinition(
            code = "GROWW",
            name = "Groww Brokerage API",
            tagline = "Modern zero-brokerage direct investment platform",
            authType = "OAuth2 Bearer Token",
            defaultScopes = listOf("orders:intraday", "portfolio:holdings"),
            oauthAuthUrl = "https://groww.in/oauth/authorize",
            oauthTokenUrl = "https://api.groww.in/v1/auth/token",
            docUrl = "https://groww.in/developer"
        ),
        BrokerDefinition(
            code = "IBKR",
            name = "Interactive Brokers (IBKR)",
            tagline = "Global equities & US stock trading • Client Portal OAuth2 Gateway",
            authType = "OAuth2 Web API Gateway",
            defaultScopes = listOf("us_equities:trade", "forex:convert", "nyse:orders"),
            oauthAuthUrl = "https://www.interactivebrokers.com/oauth2/authorize",
            oauthTokenUrl = "https://api.ibkr.com/v1/oauth/token",
            docUrl = "https://interactivebrokers.github.io/cpwebapi/",
            isPrimaryInIndia = false
        )
    )
}
