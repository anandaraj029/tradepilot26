package com.example.data.model

enum class MarketType(val displayName: String, val currencySymbol: String, val baseCurrency: String) {
    INDIA("India (NSE/BSE)", "₹", "INR"),
    USA("USA (NYSE/NASDAQ)", "$", "USD")
}

enum class DecisionType(val label: String) {
    BUY("BUY"),
    SELL("SELL"),
    HOLD("HOLD"),
    WAIT("WAIT"),
    EXIT("EXIT"),
    REDUCE_POSITION("REDUCE"),
    NO_TRADE("NO TRADE")
}

enum class TradingMode(val title: String, val description: String) {
    OBSERVE("Observe", "AI monitors & analyzes. No orders placed."),
    PAPER_TRADING("Paper Trading", "Simulated orders against virtual ₹25,000 capital."),
    ASSISTED_TRADING("Assisted Trading", "AI recommends -> User approves -> Broker execution."),
    AUTOMATED_TRADING("Automated Trading", "Strict risk-governed automated order routing (Requires kill-switch).")
}

enum class RiskStatus(val label: String) {
    SAFE("SAFE"),
    CAUTION("CAUTION"),
    CRITICAL("CRITICAL"),
    LOCKED("TRADING LOCKED")
}
