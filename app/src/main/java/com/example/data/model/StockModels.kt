package com.example.data.model

data class StockQuote(
    val symbol: String,
    val companyName: String,
    val market: MarketType,
    val currentPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val volume: String,
    val high52: Double,
    val low52: Double,
    val peRatio: Double,
    val rsi: Double,
    val macd: Double,
    val sma20: Double,
    val sma50: Double,
    val sma200: Double,
    val support: Double,
    val resistance: Double,
    val sector: String,
    val currency: String,
    val sparklineData: List<Float> = emptyList()
)

data class AiAgentResult(
    val agentName: String,
    val agentRole: String,
    val agentIcon: String,
    val decision: DecisionType,
    val confidence: Int,
    val technicalScore: Int,
    val fundamentalScore: Int,
    val sentimentScore: Int,
    val riskScore: Int,
    val timeHorizon: String,
    val reasoning: String,
    val riskFactors: List<String>,
    val invalidatingConditions: List<String>
)

data class AiConsensusResult(
    val symbol: String,
    val market: MarketType,
    val consensusDecision: DecisionType,
    val buyVotes: Int,
    val holdVotes: Int,
    val sellVotes: Int,
    val waitVotes: Int,
    val noTradeVotes: Int,
    val overallConfidence: Int,
    val techScoreAvg: Int,
    val fundScoreAvg: Int,
    val sentScoreAvg: Int,
    val riskScoreAvg: Int,
    val timeHorizon: String,
    val synthesizedReason: String,
    val keyCatalysts: List<String>,
    val riskFactors: List<String>,
    val invalidatingConditions: List<String>,
    val whatDecision: String = "",
    val whyReasoning: List<String> = emptyList(),
    val decisionChangeFactors: List<String> = emptyList(),
    val invalidationConditionsList: List<String> = emptyList(),
    val isNoTradeAdvised: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val agentBreakdowns: List<AiAgentResult> = emptyList()
)

data class RiskEvaluationResult(
    val isApproved: Boolean,
    val rejectionReason: String?,
    val currentRiskStatus: RiskStatus,
    val maxPermittedQuantity: Int,
    val suggestedStopLoss: Double,
    val suggestedTakeProfit: Double,
    val estimatedRiskRupees: Double,
    val exposurePercentageAfter: Double
)

data class StrategyModel(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val winRate: Int,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val tradesCount: Int,
    val avgHoldingDays: Int,
    val status: String,
    val entryRules: String,
    val exitRules: String
)
