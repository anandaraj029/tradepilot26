package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    MARKETS("Markets", Icons.AutoMirrored.Filled.TrendingUp),
    AI_COUNCIL("AI Council", Icons.Default.AutoAwesome),
    STRATEGIES("Strategy Lab", Icons.Default.Science),
    JOURNAL("Journal", Icons.Default.Book),
    RISK_CENTER("Risk Center", Icons.Default.Shield),
    DAILY_SUMMARY("Daily Report", Icons.Default.Assessment),
    SCANNER("AI Scanner", Icons.Default.Radar),
    BACKTEST("Backtest", Icons.Default.Timeline),
    COPILOT("AI Copilot", Icons.Default.SmartToy),
    BROKER_CONNECTIONS("Broker Connections", Icons.Default.AccountBalance);

    companion object {
        val primaryNavItems = listOf(
            DASHBOARD,
            MARKETS,
            COPILOT,
            AI_COUNCIL,
            STRATEGIES,
            RISK_CENTER
        )
    }
}
