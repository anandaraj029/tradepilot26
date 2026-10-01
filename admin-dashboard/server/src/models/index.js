import mongoose from 'mongoose';

const { Schema, model } = mongoose;
const ref = (name) => ({ type: Schema.Types.ObjectId, ref: name, index: true });
const opts = { timestamps: true };

// ---------------------------------------------------------------------------
// Admin accounts (dashboard operators)
// ---------------------------------------------------------------------------
const AdminSchema = new Schema(
  {
    name: { type: String, required: true },
    email: { type: String, required: true, unique: true, lowercase: true },
    passwordHash: { type: String, required: true },
    role: { type: String, enum: ['SUPER_ADMIN', 'RISK_OFFICER', 'ANALYST', 'SUPPORT'], default: 'ANALYST' },
    lastLoginAt: Date,
  },
  opts
);

// ---------------------------------------------------------------------------
// Traders (mobile app users) — mirrors UserProfile in AuthModels.kt
// ---------------------------------------------------------------------------
const UserSchema = new Schema(
  {
    name: { type: String, required: true },
    email: { type: String, required: true, unique: true, lowercase: true },
    phone: String,
    role: { type: String, default: 'Retail Trader' },
    tier: { type: String, default: 'Tier 1 Capital Shield' },
    status: { type: String, enum: ['ACTIVE', 'SUSPENDED', 'PENDING_KYC'], default: 'ACTIVE', index: true },
    kycStatus: { type: String, enum: ['VERIFIED', 'PENDING', 'REJECTED'], default: 'VERIFIED' },
    is2faEnabled: { type: Boolean, default: true },
    isBiometricEnabled: { type: Boolean, default: true },
    pinLockEnabled: { type: Boolean, default: true },
    notifications: {
      dailyTargetAlerts: { type: Boolean, default: true },
      killSwitchAlerts: { type: Boolean, default: true },
      aiConsensusAlerts: { type: Boolean, default: true },
      profitLockAlerts: { type: Boolean, default: true },
      soundEnabled: { type: Boolean, default: true },
    },
    brokerIntegration: { type: String, default: 'Simulated Direct API' },
    tradingMode: {
      type: String,
      enum: ['OBSERVE', 'PAPER_TRADING', 'ASSISTED_TRADING', 'AUTOMATED_TRADING'],
      default: 'PAPER_TRADING',
      index: true,
    },
    autoPilot: {
      enabled: { type: Boolean, default: false },
      autonomousLive: { type: Boolean, default: false },
      state: {
        type: String,
        enum: ['ACTIVE_HUNTING', 'TARGET_ACHIEVED', 'LOSS_LIMIT', 'LOCKED', 'MAX_POSITIONS', 'OFF'],
        default: 'OFF',
      },
      lastMessage: String,
      lastRunAt: Date,
    },
    device: { model: String, appVersion: String, os: String },
    lastLoginAt: Date,
  },
  opts
);

// ---------------------------------------------------------------------------
// Portfolio & risk limits — mirrors PortfolioEntity
// ---------------------------------------------------------------------------
const PortfolioSchema = new Schema(
  {
    user: { ...ref('User'), unique: true },
    startingCapital: { type: Number, default: 25000 },
    availableCash: { type: Number, default: 25000 },
    investedValue: { type: Number, default: 0 },
    dailyTarget: { type: Number, default: 1000 },
    maxDailyLoss: { type: Number, default: 500 },
    maxRiskPerTradePercent: { type: Number, default: 1 },
    maxOpenPositions: { type: Number, default: 4 },
    maxExposurePercent: { type: Number, default: 80 },
    realizedPnlToday: { type: Number, default: 0 },
    unrealizedPnl: { type: Number, default: 0 },
    totalRealizedPnl: { type: Number, default: 0 },
    isLocked: { type: Boolean, default: false },
    lockReason: { type: String, default: '' },
    killSwitchTriggered: { type: Boolean, default: false },
    tradingMode: { type: String, default: 'PAPER_TRADING' },
    profitLockEnabled: { type: Boolean, default: true },
    usdInrRate: { type: Number, default: 83.5 },
  },
  opts
);

// ---------------------------------------------------------------------------
// Market assets — mirrors AssetEntity / StockQuote
// ---------------------------------------------------------------------------
const AssetSchema = new Schema(
  {
    symbol: { type: String, required: true, unique: true },
    name: { type: String, required: true },
    market: { type: String, enum: ['INDIA', 'USA'], index: true },
    exchange: { type: String, enum: ['NSE', 'BSE', 'NASDAQ', 'NYSE'], default: 'NSE' },
    assetClass: { type: String, enum: ['EQUITY', 'INDEX', 'ETF', 'COMMODITY'], default: 'EQUITY' },
    currentPrice: Number,
    priceChange: Number,
    priceChangePercent: Number,
    volume: String,
    high52: Number,
    low52: Number,
    peRatio: Number,
    rsi: Number,
    macd: Number,
    sma20: Number,
    sma50: Number,
    sma200: Number,
    support: Number,
    resistance: Number,
    sector: String,
    currency: { type: String, default: 'INR' },
    isTradable: { type: Boolean, default: true },
    watchlistCount: { type: Number, default: 0 },
    sparkline: [Number],
  },
  opts
);

// ---------------------------------------------------------------------------
// Trades / Journal — mirrors TradeEntity
// ---------------------------------------------------------------------------
const TradeSchema = new Schema(
  {
    user: ref('User'),
    tradeId: { type: String, required: true, unique: true },
    symbol: { type: String, index: true },
    market: String,
    companyName: String,
    strategy: { type: String, index: true },
    entryPrice: Number,
    exitPrice: Number,
    quantity: Number,
    stopLoss: Number,
    takeProfit: Number,
    pnl: Number,
    pnlPercent: Number,
    aiDecision: String,
    aiConfidence: Number,
    riskScore: Number,
    status: { type: String, enum: ['OPEN', 'CLOSED', 'CANCELLED'], index: true },
    orderType: { type: String, enum: ['BUY', 'SELL'] },
    executionType: { type: String, enum: ['PAPER', 'LIVE'], index: true },
    source: { type: String, enum: ['MANUAL', 'AUTOPILOT', 'ASSISTED'], default: 'MANUAL' },
    entryTime: Date,
    closedAt: Date,
    outcome: { type: String, enum: ['PROFIT', 'LOSS', 'BREAKEVEN', 'PENDING'] },
    aiObservation: String,
    userOverride: Boolean,
    overrideReason: String,
    userNotes: String,
    tags: String,
    charges: Number,
    brokerOrderId: String,
  },
  opts
);

// ---------------------------------------------------------------------------
// Open positions — mirrors PositionEntity
// ---------------------------------------------------------------------------
const PositionSchema = new Schema(
  {
    user: ref('User'),
    symbol: String,
    market: String,
    companyName: String,
    quantity: Number,
    buyPrice: Number,
    currentPrice: Number,
    stopLoss: Number,
    takeProfit: Number,
    executionType: { type: String, enum: ['PAPER', 'LIVE'], default: 'PAPER' },
    openedAt: { type: Date, default: Date.now },
  },
  opts
);

// ---------------------------------------------------------------------------
// Audit log — mirrors AuditLogEntity (+ admin actions)
// ---------------------------------------------------------------------------
const AuditLogSchema = new Schema(
  {
    user: ref('User'),
    actorType: { type: String, enum: ['SYSTEM', 'USER', 'ADMIN', 'AI'], default: 'SYSTEM' },
    actorName: String,
    action: { type: String, index: true },
    symbol: String,
    market: String,
    aiConsensusVerdict: String,
    riskEngineStatus: String,
    severity: { type: String, enum: ['INFO', 'WARNING', 'CRITICAL'], default: 'INFO' },
    details: String,
  },
  opts
);

// ---------------------------------------------------------------------------
// Strategy Lab — system strategies (StrategyModel) & user custom strategies
// ---------------------------------------------------------------------------
const StrategySchema = new Schema(
  {
    key: { type: String, unique: true },
    name: { type: String, required: true },
    category: String,
    description: String,
    winRate: Number,
    profitFactor: Number,
    maxDrawdown: Number,
    tradesCount: Number,
    avgHoldingDays: Number,
    status: { type: String, enum: ['ACTIVE', 'PAUSED', 'DEPRECATED'], default: 'ACTIVE' },
    entryRules: String,
    exitRules: String,
    allowLive: { type: Boolean, default: false },
  },
  opts
);

const CustomStrategySchema = new Schema(
  {
    user: ref('User'),
    name: String,
    prompt: String,
    capitalAllocation: Number,
    maxRiskPercent: Number,
    entryCriteria: String,
    exitCriteria: String,
    status: {
      type: String,
      enum: ['PAPER_TRADING_ONLY', 'UNDER_REVIEW', 'APPROVED_LIVE', 'REJECTED', 'ARCHIVED'],
      default: 'PAPER_TRADING_ONLY',
    },
    reviewNote: String,
  },
  opts
);

// ---------------------------------------------------------------------------
// Backtests — mirrors BacktestSummaryResult
// ---------------------------------------------------------------------------
const BacktestSchema = new Schema(
  {
    user: ref('User'),
    config: {
      strategyId: String,
      strategyName: String,
      symbol: String,
      market: String,
      timeframeDays: Number,
      startingCapital: Number,
      maxRiskPerTradePercent: Number,
      stopLossPercent: Number,
      takeProfitPercent: Number,
      maxHoldingDays: Number,
    },
    totalTrades: Number,
    winCount: Number,
    lossCount: Number,
    breakevenCount: Number,
    winRatePercent: Number,
    finalEquity: Number,
    netPnl: Number,
    totalReturnPercent: Number,
    profitFactor: Number,
    maxDrawdownPercent: Number,
    sharpeRatio: Number,
    avgHoldingDays: Number,
    payoffRatio: Number,
    equityCurve: [{ date: String, equity: Number }],
    qualificationStatus: { type: String, enum: ['QUALIFIED', 'NEEDS_TUNING', 'REJECTED'] },
    aiCouncilCritique: String,
  },
  opts
);

// ---------------------------------------------------------------------------
// AI Council decisions — mirrors AiConsensusResult + RiskEvaluationResult
// ---------------------------------------------------------------------------
const AgentResultSchema = new Schema(
  {
    agentName: String,
    agentRole: String,
    decision: String,
    confidence: Number,
    technicalScore: Number,
    fundamentalScore: Number,
    sentimentScore: Number,
    riskScore: Number,
    reasoning: String,
  },
  { _id: false }
);

const CouncilDecisionSchema = new Schema(
  {
    user: ref('User'),
    symbol: { type: String, index: true },
    market: String,
    consensusDecision: { type: String, index: true },
    buyVotes: Number,
    holdVotes: Number,
    sellVotes: Number,
    waitVotes: Number,
    noTradeVotes: Number,
    overallConfidence: Number,
    techScoreAvg: Number,
    fundScoreAvg: Number,
    sentScoreAvg: Number,
    riskScoreAvg: Number,
    timeHorizon: String,
    synthesizedReason: String,
    keyCatalysts: [String],
    riskFactors: [String],
    invalidatingConditions: [String],
    isNoTradeAdvised: Boolean,
    riskApproved: Boolean,
    riskStatus: { type: String, enum: ['SAFE', 'CAUTION', 'CRITICAL', 'LOCKED'] },
    rejectionReason: String,
    actedOn: { type: String, enum: ['EXECUTED', 'OVERRIDDEN', 'IGNORED', 'BLOCKED'], default: 'IGNORED' },
    liveApi: { type: Boolean, default: false },
    latencyMs: Number,
    agentBreakdowns: [AgentResultSchema],
  },
  opts
);

// ---------------------------------------------------------------------------
// AI Copilot (Gemini chat) sessions
// ---------------------------------------------------------------------------
const CopilotSessionSchema = new Schema(
  {
    user: ref('User'),
    title: String,
    topic: { type: String, enum: ['MARKET_ANALYSIS', 'RISK', 'STRATEGY', 'PORTFOLIO', 'EDUCATION', 'OTHER'] },
    model: { type: String, default: 'gemini-2.5-flash' },
    messageCount: Number,
    tokensUsed: Number,
    flagged: { type: Boolean, default: false },
    flagReason: String,
    lastMessagePreview: String,
    rating: Number,
  },
  opts
);

// ---------------------------------------------------------------------------
// Brokers — catalog (SupportedBrokersCatalog) and per-user connections
// ---------------------------------------------------------------------------
const BrokerSchema = new Schema(
  {
    code: { type: String, unique: true },
    name: String,
    shortName: String,
    tagline: String,
    authType: String,
    defaultScopes: [String],
    oauthAuthUrl: String,
    oauthTokenUrl: String,
    docUrl: String,
    isPrimaryInIndia: { type: Boolean, default: true },
    enabled: { type: Boolean, default: true },
    liveTradingAllowed: { type: Boolean, default: true },
  },
  opts
);

const BrokerConnectionSchema = new Schema(
  {
    user: ref('User'),
    brokerCode: { type: String, index: true },
    brokerName: String,
    brokerUserId: String,
    brokerUserName: String,
    status: { type: String, enum: ['ACTIVE', 'DISCONNECTED', 'TOKEN_EXPIRING', 'AUTHENTICATING', 'REVOKED'] },
    isConnected: Boolean,
    isLiveRoutingActive: Boolean,
    autoPilotAutonomousLive: Boolean,
    liveMargin: Number,
    usedMargin: Number,
    availableCollateral: Number,
    latencyMs: Number,
    tokenExpiresAt: Date,
    isEncryptedInVault: { type: Boolean, default: true },
    scopes: [String],
    lastSyncedAt: Date,
  },
  opts
);

// ---------------------------------------------------------------------------
// Fund transactions — mirrors FundTransactionEntity
// ---------------------------------------------------------------------------
const FundTransactionSchema = new Schema(
  {
    user: ref('User'),
    txId: { type: String, unique: true },
    type: { type: String, enum: ['DEPOSIT', 'WITHDRAWAL'], index: true },
    amount: Number,
    paymentMethod: String,
    utrNumber: String,
    status: { type: String, enum: ['SUCCESS', 'PENDING', 'FAILED', 'REJECTED'], index: true },
    brokerName: String,
    notes: String,
    reviewedBy: String,
  },
  opts
);

// ---------------------------------------------------------------------------
// Platform settings (singleton)
// ---------------------------------------------------------------------------
const SettingsSchema = new Schema(
  {
    key: { type: String, default: 'global', unique: true },
    globalKillSwitch: { type: Boolean, default: false },
    globalKillSwitchReason: String,
    maintenanceMode: { type: Boolean, default: false },
    liveTradingEnabled: { type: Boolean, default: true },
    autoPilotEnabled: { type: Boolean, default: true },
    allowedTradingModes: {
      type: [String],
      default: ['OBSERVE', 'PAPER_TRADING', 'ASSISTED_TRADING', 'AUTOMATED_TRADING'],
    },
    usdInrRate: { type: Number, default: 83.5 },
    defaultRisk: {
      startingCapital: { type: Number, default: 25000 },
      dailyTarget: { type: Number, default: 1000 },
      maxDailyLoss: { type: Number, default: 500 },
      maxRiskPerTradePercent: { type: Number, default: 1 },
      maxOpenPositions: { type: Number, default: 4 },
      maxExposurePercent: { type: Number, default: 80 },
      profitLockEnabled: { type: Boolean, default: true },
      stopLossPercent: { type: Number, default: 2.5 },
    },
    aiAgents: [
      {
        key: String,
        name: String,
        role: String,
        enabled: { type: Boolean, default: true },
        weight: { type: Number, default: 1 },
        model: String,
        _id: false,
      },
    ],
    council: {
      minBuyVotes: { type: Number, default: 2 },
      minConfidence: { type: Number, default: 60 },
      maxRiskScore: { type: Number, default: 50 },
    },
    copilot: {
      enabled: { type: Boolean, default: true },
      model: { type: String, default: 'gemini-2.5-flash' },
      dailyMessageLimit: { type: Number, default: 100 },
    },
    security: {
      require2fa: { type: Boolean, default: true },
      requireBiometricForLive: { type: Boolean, default: true },
      sessionTimeoutMinutes: { type: Number, default: 15 },
    },
  },
  opts
);

export const Admin = model('Admin', AdminSchema);
export const User = model('User', UserSchema);
export const Portfolio = model('Portfolio', PortfolioSchema);
export const Asset = model('Asset', AssetSchema);
export const Trade = model('Trade', TradeSchema);
export const Position = model('Position', PositionSchema);
export const AuditLog = model('AuditLog', AuditLogSchema);
export const Strategy = model('Strategy', StrategySchema);
export const CustomStrategy = model('CustomStrategy', CustomStrategySchema);
export const Backtest = model('Backtest', BacktestSchema);
export const CouncilDecision = model('CouncilDecision', CouncilDecisionSchema);
export const CopilotSession = model('CopilotSession', CopilotSessionSchema);
export const Broker = model('Broker', BrokerSchema);
export const BrokerConnection = model('BrokerConnection', BrokerConnectionSchema);
export const FundTransaction = model('FundTransaction', FundTransactionSchema);
export const Settings = model('Settings', SettingsSchema);

export async function getSettings() {
  let s = await Settings.findOne({ key: 'global' });
  if (!s) s = await Settings.create({ key: 'global' });
  return s;
}
