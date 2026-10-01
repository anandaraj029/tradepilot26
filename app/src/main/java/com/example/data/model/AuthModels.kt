package com.example.data.model

enum class AuthStep {
    LOGIN,
    TWO_FACTOR,
    AUTHENTICATED
}

data class UserProfile(
    val email: String = "ebdc.org@gmail.com",
    val name: String = "Alex Chen",
    val role: String = "Chief Quantitative Strategist",
    val tier: String = "Tier 1 Capital Shield",
    val is2faEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val dailyTargetAlerts: Boolean = true,
    val killSwitchAlerts: Boolean = true,
    val aiConsensusAlerts: Boolean = true,
    val profitLockAlerts: Boolean = true,
    val soundEnabled: Boolean = true,
    val brokerIntegration: String = "Simulated Direct API" // "Zerodha Kite", "Interactive Brokers", "Simulated Direct API"
)
