import 'dotenv/config';
import bcrypt from 'bcryptjs';
import { fileURLToPath } from 'node:url';
import {
  Admin, User, Portfolio, Asset, Trade, Position, AuditLog, Strategy, CustomStrategy, Backtest,
  CouncilDecision, CopilotSession, Broker, BrokerConnection, FundTransaction, Settings,
} from './models/index.js';

// Deterministic PRNG so every seed produces the same demo data
let s = 20261001;
const rand = () => ((s = (s * 1664525 + 1013904223) % 4294967296) / 4294967296);
const ri = (a, b) => Math.floor(rand() * (b - a + 1)) + a;
const rf = (a, b, d = 2) => +(rand() * (b - a) + a).toFixed(d);
const pick = (arr) => arr[Math.floor(rand() * arr.length)];
const DAY = 86400000;
const ago = (days, hours = 0) => new Date(Date.now() - days * DAY - hours * 3600000);
const id = (p) => `${p}-${Math.floor(rand() * 1e10).toString(36).toUpperCase()}`;

// ------------------------------------------------------------- reference data (from the Android app)
const ASSETS = [
  ['NIFTY 50', 'Nifty 50 Benchmark Index', 'INDIA', 'NSE', 'INDEX', 'Index', 24820.5],
  ['BANKNIFTY', 'Bank Nifty Banking Index', 'INDIA', 'NSE', 'INDEX', 'Banking', 52140],
  ['RELIANCE', 'Reliance Industries Ltd', 'INDIA', 'NSE', 'EQUITY', 'Energy & Retail', 2942.3],
  ['TATAMOTORS', 'Tata Motors Limited', 'INDIA', 'NSE', 'EQUITY', 'Automotive', 964.5],
  ['TCS', 'Tata Consultancy Services', 'INDIA', 'NSE', 'EQUITY', 'Technology', 4185],
  ['HDFCBANK', 'HDFC Bank Limited', 'INDIA', 'NSE', 'EQUITY', 'Banking', 1682.75],
  ['INFY', 'Infosys Limited', 'INDIA', 'NSE', 'EQUITY', 'Technology', 1895.1],
  ['ICICIBANK', 'ICICI Bank Limited', 'INDIA', 'NSE', 'EQUITY', 'Banking', 1248.8],
  ['SBIN', 'State Bank of India', 'INDIA', 'NSE', 'EQUITY', 'Banking', 812.4],
  ['BHARTIARTL', 'Bharti Airtel Limited', 'INDIA', 'NSE', 'EQUITY', 'Telecom', 1598.2],
  ['ITC', 'ITC Limited', 'INDIA', 'NSE', 'EQUITY', 'FMCG', 498.6],
  ['LT', 'Larsen & Toubro Ltd', 'INDIA', 'NSE', 'EQUITY', 'Infrastructure', 3712],
  ['AXISBANK', 'Axis Bank Limited', 'INDIA', 'NSE', 'EQUITY', 'Banking', 1162.3],
  ['MARUTI', 'Maruti Suzuki India', 'INDIA', 'NSE', 'EQUITY', 'Automotive', 12480],
  ['SUNPHARMA', 'Sun Pharmaceutical Industries', 'INDIA', 'NSE', 'EQUITY', 'Pharma', 1795.4],
  ['WIPRO', 'Wipro Limited', 'INDIA', 'BSE', 'EQUITY', 'Technology', 548.9],
  ['ADANIENT', 'Adani Enterprises Ltd', 'INDIA', 'NSE', 'EQUITY', 'Conglomerate', 3088.7],
  ['TATASTEEL', 'Tata Steel Limited', 'INDIA', 'NSE', 'EQUITY', 'Metals', 158.3],
  ['ZOMATO', 'Zomato Limited', 'INDIA', 'NSE', 'EQUITY', 'Consumer Tech', 264.8],
  ['NIFTYBEES', 'Nippon India Nifty BeES', 'INDIA', 'NSE', 'ETF', 'Index', 271.4],
  ['GOLDBEES', 'Nippon India Gold BeES', 'INDIA', 'NSE', 'COMMODITY', 'Commodity', 64.2],
  ['AAPL', 'Apple Inc.', 'USA', 'NASDAQ', 'EQUITY', 'Technology', 228.4],
  ['MSFT', 'Microsoft Corporation', 'USA', 'NASDAQ', 'EQUITY', 'Technology', 431.2],
  ['NVDA', 'NVIDIA Corporation', 'USA', 'NASDAQ', 'EQUITY', 'Semiconductors', 121.8],
  ['TSLA', 'Tesla Inc.', 'USA', 'NASDAQ', 'EQUITY', 'Automotive', 248.5],
  ['AMZN', 'Amazon.com Inc.', 'USA', 'NASDAQ', 'EQUITY', 'Consumer Tech', 186.4],
  ['GOOGL', 'Alphabet Inc.', 'USA', 'NASDAQ', 'EQUITY', 'Technology', 165.9],
  ['META', 'Meta Platforms Inc.', 'USA', 'NASDAQ', 'EQUITY', 'Technology', 568.3],
  ['JPM', 'JPMorgan Chase & Co.', 'USA', 'NYSE', 'EQUITY', 'Banking', 214.7],
  ['XOM', 'Exxon Mobil Corporation', 'USA', 'NYSE', 'EQUITY', 'Energy', 118.2],
  ['SPY', 'SPDR S&P 500 ETF', 'USA', 'NYSE', 'ETF', 'Index', 571.6],
  ['QQQ', 'Invesco QQQ Trust', 'USA', 'NASDAQ', 'ETF', 'Index', 488.1],
];

const STRATEGIES = [
  { key: 'strat-1', name: 'Momentum Breakout Pilot', category: 'Trend & Volume', description: 'Identifies high-volume consolidation breaks with MACD acceleration and sector tailwinds.', winRate: 68, profitFactor: 2.14, maxDrawdown: 4.2, tradesCount: 142, avgHoldingDays: 3, entryRules: '20-day high break + RSI in 55-68 range + Volume > 2x average', exitRules: 'Trailing 8-EMA or 2.5R risk/reward target hit', allowLive: true },
  { key: 'strat-2', name: 'Mean Reversion RSI Guard', category: 'Statistical Reversion', description: 'Exploits short-term oversold bounces near structural support zones with strict stops.', winRate: 72, profitFactor: 1.95, maxDrawdown: 3.6, tradesCount: 98, avgHoldingDays: 2, entryRules: 'RSI < 33 + Candlestick hammer at key support + Council approval', exitRules: 'Return to 20-SMA or 1.5R target', allowLive: true },
  { key: 'strat-3', name: 'Multi-AI Council Hybrid', category: 'Consensus Engine', description: 'Combines Gemini technicals, Claude fundamentals, and ChatGPT portfolio correlation with deterministic risk filtering.', winRate: 76, profitFactor: 2.45, maxDrawdown: 2.8, tradesCount: 186, avgHoldingDays: 4, entryRules: '2+ Council BUY votes + Risk score < 50 + Risk Engine clearance', exitRules: 'Council SELL/WAIT or Stop Loss hit', allowLive: true },
  { key: 'strat-4', name: 'Sector Rotation Swing', category: 'Macro & Relative Strength', description: 'Monitors capital flows between banking, technology, energy, and automotive sectors.', winRate: 64, profitFactor: 1.82, maxDrawdown: 5.1, tradesCount: 74, avgHoldingDays: 6, entryRules: 'Relative strength index vs benchmark > 1.2 + 50-SMA support', exitRules: 'Sector momentum deceleration or 5-day holding period' },
  { key: 'strat-5', name: 'AutoPilot Daily Target', category: 'Autonomous', description: 'AutoPilot engine: hunts RSI 42-68 momentum setups until the daily target is hit, then engages profit lock.', winRate: 66, profitFactor: 1.74, maxDrawdown: 3.9, tradesCount: 311, avgHoldingDays: 1, entryRules: 'RSI 42-68 + >0.3% momentum + cash >= ₹2,000', exitRules: 'TP +3.5% / SL -1.5% / +2% profit booking' },
];
const STRAT_NAMES = STRATEGIES.map((x) => x.name);

const BROKERS = [
  { code: 'ZERODHA', name: 'Zerodha Kite Connect v3', shortName: 'Zerodha Kite', tagline: "India's largest retail broker • Superfast HTTP & WebSocket APIs", authType: 'OAuth2 Authorization Code', defaultScopes: ['orders:create', 'orders:cancel', 'portfolio:read', 'market:quotes', 'funds:read'], oauthAuthUrl: 'https://kite.zerodha.com/connect/login?v=3', oauthTokenUrl: 'https://api.kite.trade/session/token', docUrl: 'https://kite.trade/docs/connect/v3/' },
  { code: 'ANGEL_ONE', name: 'Angel One SmartAPI', shortName: 'Angel One', tagline: 'Institutional algorithmic execution • SmartAPI OAuth2 & TOTP', authType: 'OAuth2 + TOTP MFA', defaultScopes: ['trade:write', 'position:read', 'historic:feed', 'orderbook:sync'], oauthAuthUrl: 'https://smartapi.angelbroking.com/publisher-login', oauthTokenUrl: 'https://apiconnect.angelbroking.com/rest/auth/partner/token', docUrl: 'https://smartapi.angelbroking.com/docs' },
  { code: 'UPSTOX', name: 'Upstox Pro API v2', shortName: 'Upstox', tagline: 'Ultra-low latency OpenAPI • Complete OAuth2 flow', authType: 'OAuth2 Authorization Code', defaultScopes: ['orders:write', 'holdings:read', 'market:stream'], oauthAuthUrl: 'https://api.upstox.com/v2/login/authorization/dialog', oauthTokenUrl: 'https://api.upstox.com/v2/login/authorization/token', docUrl: 'https://upstox.com/developer/api-documentation' },
  { code: 'DHAN', name: 'DhanHQ Superfast v2', shortName: 'Dhan', tagline: 'Direct exchange routing • Microsecond DMA execution', authType: 'OAuth2 Access Token', defaultScopes: ['orders:dma', 'portfolio:sync', 'margin:realtime'], oauthAuthUrl: 'https://auth.dhan.co/login/consent', oauthTokenUrl: 'https://api.dhan.co/v2/token', docUrl: 'https://dhanhq.co/docs' },
  { code: 'GROWW', name: 'Groww Brokerage API', shortName: 'Groww', tagline: 'Modern zero-brokerage direct investment platform', authType: 'OAuth2 Bearer Token', defaultScopes: ['orders:intraday', 'portfolio:holdings'], oauthAuthUrl: 'https://groww.in/oauth/authorize', oauthTokenUrl: 'https://api.groww.in/v1/auth/token', docUrl: 'https://groww.in/developer' },
  { code: 'IBKR', name: 'Interactive Brokers (IBKR)', shortName: 'IBKR', tagline: 'Global equities & US stock trading • Client Portal OAuth2 Gateway', authType: 'OAuth2 Web API Gateway', defaultScopes: ['us_equities:trade', 'forex:convert', 'nyse:orders'], oauthAuthUrl: 'https://www.interactivebrokers.com/oauth2/authorize', oauthTokenUrl: 'https://api.ibkr.com/v1/oauth/token', docUrl: 'https://interactivebrokers.github.io/cpwebapi/', isPrimaryInIndia: false },
];

const AGENTS = [
  { key: 'gemini', name: 'Gemini Market Intelligence', role: 'Technical & Market Sentiment Analyst', model: 'gemini-2.5-flash', weight: 1 },
  { key: 'claude', name: 'Claude Research Agent', role: 'Fundamental & Valuation Specialist', model: 'claude-sonnet-5-5', weight: 1 },
  { key: 'chatgpt', name: 'ChatGPT Portfolio Agent', role: 'Portfolio & Risk/Reward Strategist', model: 'gpt-5', weight: 1 },
];

const AUTOPILOT_STATES = [
  ['ACTIVE_HUNTING', 'Auto-Pilot active. Scanning 1,000+ assets for optimal entry to reach the daily target.'],
  ['ACTIVE_HUNTING', 'Auto-Pilot selected momentum candidate (RSI 58, +0.9%). Hunting remaining target.'],
  ['TARGET_ACHIEVED', '🎯 Daily target achieved! Auto-Pilot engaged Profit Lock and stopped to secure gains.'],
  ['LOSS_LIMIT', '🛑 Max daily loss boundary reached. Auto-Pilot stopped automatically to preserve capital.'],
  ['MAX_POSITIONS', 'Max open positions reached. Waiting for exits before new entries.'],
];
const FIRST = ['Aarav', 'Vivaan', 'Aditya', 'Ananya', 'Diya', 'Ishaan', 'Kavya', 'Rohan', 'Priya', 'Arjun', 'Sneha', 'Rahul', 'Meera', 'Karan', 'Nisha', 'Vikram', 'Pooja', 'Siddharth', 'Riya', 'Aman', 'Tanvi', 'Dev', 'Alex', 'Sam', 'Jordan', 'Neha', 'Varun', 'Ira', 'Kabir', 'Zara'];
const LAST = ['Sharma', 'Patel', 'Iyer', 'Reddy', 'Mehta', 'Gupta', 'Nair', 'Kapoor', 'Singh', 'Rao', 'Chen', 'Desai', 'Joshi', 'Bose', 'Malhotra', 'Verma'];
const TIERS = ['Tier 1 Capital Shield', 'Tier 2 Growth', 'Tier 3 Pro Quant'];
const MODES = ['OBSERVE', 'PAPER_TRADING', 'PAPER_TRADING', 'PAPER_TRADING', 'ASSISTED_TRADING', 'ASSISTED_TRADING', 'AUTOMATED_TRADING'];
const DECISIONS = ['BUY', 'BUY', 'BUY', 'HOLD', 'WAIT', 'SELL', 'NO_TRADE'];
const PAYMENT = ['Instant UPI (PhonePe / GPay / BHIM)', 'NetBanking (HDFC / SBI / ICICI)', 'IMPS/NEFT'];
const TAGS = ['Followed Plan', 'Followed Plan', 'Followed Plan', 'FOMO Entry', 'Early Exit', 'Revenge Trade', 'Perfect Execution'];

export async function seedDatabase({ reset = false } = {}) {
  if (reset) {
    await Promise.all([Admin, User, Portfolio, Asset, Trade, Position, AuditLog, Strategy, CustomStrategy, Backtest, CouncilDecision, CopilotSession, Broker, BrokerConnection, FundTransaction, Settings].map((M) => M.deleteMany({})));
  }
  console.log('[seed] seeding demo data...');

  // Admin team
  const pw = await bcrypt.hash(process.env.ADMIN_PASSWORD || 'Admin@123', 10);
  await Admin.insertMany([
    { name: 'Platform Owner', email: process.env.ADMIN_EMAIL || 'admin@tradepilot.ai', passwordHash: pw, role: 'SUPER_ADMIN' },
    { name: 'Risk Desk', email: 'risk@tradepilot.ai', passwordHash: pw, role: 'RISK_OFFICER' },
    { name: 'Quant Analyst', email: 'analyst@tradepilot.ai', passwordHash: pw, role: 'ANALYST' },
    { name: 'Support Lead', email: 'support@tradepilot.ai', passwordHash: pw, role: 'SUPPORT' },
  ]);

  await Settings.create({ key: 'global', aiAgents: AGENTS });
  await Strategy.insertMany(STRATEGIES);
  await Broker.insertMany(BROKERS);

  // Assets with technicals
  const assets = await Asset.insertMany(ASSETS.map(([symbol, name, market, exchange, assetClass, sector, price]) => {
    const chg = rf(-3, 3.5);
    const spark = [];
    let p = price * (1 - chg / 100);
    for (let i = 0; i < 12; i++) { p *= 1 + rf(-0.01, 0.012, 4); spark.push(+p.toFixed(2)); }
    spark.push(price);
    return {
      symbol, name, market, exchange, assetClass, sector, currentPrice: price,
      currency: market === 'USA' ? 'USD' : 'INR',
      priceChange: +(price * chg / 100).toFixed(2), priceChangePercent: chg,
      volume: `${rf(0.5, 40, 1)}M`, high52: +(price * rf(1.05, 1.35)).toFixed(2), low52: +(price * rf(0.6, 0.92)).toFixed(2),
      peRatio: assetClass === 'EQUITY' ? rf(9, 65, 1) : rf(18, 26, 1), rsi: rf(24, 78, 1), macd: rf(-25, 40),
      sma20: +(price * rf(0.97, 1.02)).toFixed(2), sma50: +(price * rf(0.94, 1.03)).toFixed(2), sma200: +(price * rf(0.85, 1.02)).toFixed(2),
      support: +(price * rf(0.95, 0.985)).toFixed(2), resistance: +(price * rf(1.015, 1.06)).toFixed(2),
      watchlistCount: ri(0, 45), isTradable: true, sparkline: spark,
    };
  }));
  const tradables = assets.filter((a) => a.assetClass !== 'INDEX');

  // Traders
  const usersData = [];
  for (let i = 0; i < 64; i++) {
    const f = FIRST[i % FIRST.length], l = LAST[(i * 7) % LAST.length];
    const mode = pick(MODES);
    const createdAt = ago(ri(0, 120), ri(0, 23));
    const ap = mode === 'AUTOMATED_TRADING' || rand() < 0.2;
    usersData.push({
      name: `${f} ${l}`, email: `${f}.${l}${i}@example.com`.toLowerCase(), phone: `+91 9${ri(100000000, 999999999)}`,
      role: pick(['Retail Trader', 'Swing Trader', 'Options Learner', 'Chief Quantitative Strategist', 'Long-term Investor']),
      tier: pick(TIERS), status: i % 23 === 5 ? 'SUSPENDED' : i % 17 === 3 ? 'PENDING_KYC' : 'ACTIVE',
      kycStatus: i % 17 === 3 ? 'PENDING' : 'VERIFIED', is2faEnabled: rand() > 0.15, isBiometricEnabled: rand() > 0.3,
      pinLockEnabled: rand() > 0.2, tradingMode: mode,
      brokerIntegration: mode === 'PAPER_TRADING' || mode === 'OBSERVE' ? 'Simulated Direct API' : pick(['Zerodha Kite', 'Angel One', 'Upstox', 'Dhan', 'Interactive Brokers']),
      autoPilot: ap ? {
        enabled: true, autonomousLive: mode === 'AUTOMATED_TRADING' && rand() > 0.4,
        ...(() => {
          const [state, lastMessage] = pick(AUTOPILOT_STATES);
          return { state, lastMessage };
        })(),
        lastRunAt: ago(0, ri(0, 5)),
      } : { enabled: false, state: 'OFF' },
      device: { model: pick(['Pixel 9 Pro', 'Galaxy S25', 'OnePlus 13', 'Redmi Note 14', 'Nothing Phone 3']), os: `Android ${pick([14, 15, 16])}`, appVersion: pick(['1.4.2', '1.5.0', '1.5.1']) },
      lastLoginAt: ago(ri(0, 10), ri(0, 23)), createdAt, updatedAt: createdAt,
    });
  }
  const users = await User.insertMany(usersData, { timestamps: false });

  // Trades over the last 45 days
  const trades = [];
  const userPnlToday = {};
  for (const u of users) {
    const n = u.tradingMode === 'OBSERVE' ? ri(0, 3) : ri(8, 40);
    for (let k = 0; k < n; k++) {
      const a = pick(tradables);
      const daysAgo = ri(0, 44);
      const entry = +(a.currentPrice * rf(0.92, 1.05)).toFixed(2);
      const isUs = a.market === 'USA';
      const qty = isUs ? ri(1, 8) : Math.max(1, Math.floor(rf(2500, 7000) / entry));
      const win = rand() < 0.62;
      const movePct = win ? rf(0.4, 3.6) : -rf(0.3, 2.5);
      const exit = +(entry * (1 + movePct / 100)).toFixed(2);
      const pnl = +((exit - entry) * qty * (isUs ? 83.5 : 1)).toFixed(2);
      const override = rand() < 0.14;
      const live = u.tradingMode === 'ASSISTED_TRADING' || u.tradingMode === 'AUTOMATED_TRADING' ? rand() < 0.6 : false;
      const entryTime = ago(daysAgo, ri(2, 8));
      const closedAt = new Date(entryTime.getTime() + ri(1, 72) * 3600000);
      const outcome = Math.abs(pnl) < 1 ? 'BREAKEVEN' : pnl > 0 ? 'PROFIT' : 'LOSS';
      if (daysAgo === 0) userPnlToday[u._id] = (userPnlToday[u._id] || 0) + pnl;
      trades.push({
        user: u._id, tradeId: id('TP'), symbol: a.symbol, market: a.market, companyName: a.name,
        strategy: u.autoPilot.enabled && rand() < 0.5 ? 'AutoPilot Daily Target' : pick(STRAT_NAMES.slice(0, 4)),
        entryPrice: entry, exitPrice: exit, quantity: qty,
        stopLoss: +(entry * 0.975).toFixed(2), takeProfit: +(entry * 1.055).toFixed(2),
        pnl, pnlPercent: +movePct.toFixed(2), aiDecision: pick(['BUY', 'BUY', 'BUY', 'HOLD', 'WAIT']),
        aiConfidence: ri(48, 94), riskScore: ri(18, 78), status: rand() < 0.03 ? 'CANCELLED' : 'CLOSED',
        orderType: rand() < 0.88 ? 'BUY' : 'SELL', executionType: live ? 'LIVE' : 'PAPER',
        source: u.autoPilot.enabled && rand() < 0.5 ? 'AUTOPILOT' : live ? 'ASSISTED' : 'MANUAL',
        entryTime, closedAt, outcome,
        aiObservation: win ? 'Momentum continuation confirmed by council; risk engine approved within 1% sizing.' : 'Setup invalidated: close below support on rising volume.',
        userOverride: override, overrideReason: override ? pick(['Took trade despite WAIT verdict', 'Exited early on news', 'Increased size above suggestion']) : '',
        userNotes: rand() < 0.3 ? pick(['Clean breakout, followed rules.', 'Should have waited for the retest.', 'Sized correctly, stop respected.']) : '',
        tags: override ? pick(['FOMO Entry', 'Revenge Trade', 'Early Exit']) : pick(TAGS),
        charges: +(qty * entry * 0.0006 + 20 * (live ? 1 : 0)).toFixed(2),
        brokerOrderId: live ? `${ri(100000000, 999999999)}` : undefined,
      });
    }
  }
  await Trade.insertMany(trades);

  // Portfolios & open positions
  const portfolios = [];
  const positions = [];
  for (const u of users) {
    const cap = pick([25000, 25000, 50000, 100000, 250000]);
    const open = u.tradingMode === 'OBSERVE' ? 0 : ri(0, 4);
    let invested = 0, unrealized = 0;
    for (let k = 0; k < open; k++) {
      const a = pick(tradables);
      const isUs = a.market === 'USA';
      const buy = +(a.currentPrice * rf(0.96, 1.02)).toFixed(2);
      const qty = isUs ? ri(1, 5) : Math.max(1, Math.floor(rf(2500, 6000) / buy));
      const mult = isUs ? 83.5 : 1;
      invested += buy * qty * mult;
      unrealized += (a.currentPrice - buy) * qty * mult;
      positions.push({
        user: u._id, symbol: a.symbol, market: a.market, companyName: a.name, quantity: qty, buyPrice: buy,
        currentPrice: a.currentPrice, stopLoss: +(buy * 0.975).toFixed(2), takeProfit: +(buy * 1.055).toFixed(2),
        executionType: ['ASSISTED_TRADING', 'AUTOMATED_TRADING'].includes(u.tradingMode) && rand() < 0.5 ? 'LIVE' : 'PAPER',
        openedAt: ago(ri(0, 6), ri(0, 6)),
      });
    }
    const today = +(userPnlToday[u._id] || 0).toFixed(2);
    const total = trades.filter((t) => t.user === u._id).reduce((acc, t) => acc + t.pnl, 0);
    const suspended = u.status === 'SUSPENDED';
    const maxLoss = cap >= 100000 ? 1250 : 500;
    const kill = !suspended && rand() < 0.04;
    portfolios.push({
      user: u._id, startingCapital: cap, availableCash: +(cap + total - invested).toFixed(2),
      investedValue: +invested.toFixed(2), unrealizedPnl: +unrealized.toFixed(2), totalRealizedPnl: +total.toFixed(2),
      dailyTarget: cap >= 100000 ? 2500 : 1000, maxDailyLoss: maxLoss,
      maxRiskPerTradePercent: pick([0.5, 1, 1, 1, 1.5]), maxOpenPositions: pick([3, 4, 4, 5]),
      realizedPnlToday: today, isLocked: suspended || today <= -maxLoss,
      lockReason: suspended ? 'Account suspended by admin: KYC mismatch' : today <= -maxLoss ? 'Daily loss limit reached' : '',
      killSwitchTriggered: kill, tradingMode: u.tradingMode, profitLockEnabled: rand() > 0.1,
    });
  }
  await Portfolio.insertMany(portfolios);
  await Position.insertMany(positions);

  // AI Council decisions
  const decisions = [];
  for (let i = 0; i < 420; i++) {
    const u = pick(users), a = pick(assets);
    const breakdown = AGENTS.map((ag) => ({
      agentName: ag.name, agentRole: ag.role, decision: pick(DECISIONS), confidence: ri(40, 94),
      technicalScore: ri(35, 92), fundamentalScore: ri(30, 90), sentimentScore: ri(30, 90), riskScore: ri(20, 85),
      reasoning: ag.key === 'gemini' ? `RSI at ${a.rsi} with MACD ${a.macd > 0 ? 'bullish convergence' : 'negative divergence'}.` : ag.key === 'claude' ? `${a.name} trades at ${a.peRatio}x P/E; balance sheet supports defensive quality.` : `Risk-to-reward ~1:2.1 relative to support ${a.support} and target ${a.resistance}.`,
    }));
    const votes = { BUY: 0, HOLD: 0, SELL: 0, WAIT: 0, NO_TRADE: 0 };
    breakdown.forEach((b) => votes[b.decision]++);
    const consensus = Object.entries(votes).sort((x, y) => y[1] - x[1])[0][0];
    const conf = Math.round(breakdown.reduce((acc, b) => acc + b.confidence, 0) / 3);
    const riskAvg = Math.round(breakdown.reduce((acc, b) => acc + b.riskScore, 0) / 3);
    const approved = consensus === 'BUY' || consensus === 'SELL' ? rand() < 0.75 : false;
    const rejection = approved ? null : consensus === 'BUY' || consensus === 'SELL'
      ? pick(['MAX OPEN POSITIONS REACHED: Currently holding 4/4 allowed positions.', 'PORTFOLIO EXPOSURE LIMIT: Trade would result in 86% allocation (Max permitted: 80%).', 'DAILY TARGET ACHIEVED (+₹1000): Trading locked to protect realized gains.', 'DAILY LOSS LIMIT REACHED (-₹500): Trading locked.'])
      : `AI Council returned ${consensus.replace('_', ' ')}. Capital preservation rule: preserve cash.`;
    decisions.push({
      user: u._id, symbol: a.symbol, market: a.market, consensusDecision: consensus,
      buyVotes: votes.BUY, holdVotes: votes.HOLD, sellVotes: votes.SELL, waitVotes: votes.WAIT, noTradeVotes: votes.NO_TRADE,
      overallConfidence: conf, techScoreAvg: Math.round(breakdown.reduce((x, b) => x + b.technicalScore, 0) / 3),
      fundScoreAvg: Math.round(breakdown.reduce((x, b) => x + b.fundamentalScore, 0) / 3),
      sentScoreAvg: Math.round(breakdown.reduce((x, b) => x + b.sentimentScore, 0) / 3), riskScoreAvg: riskAvg,
      timeHorizon: pick(['Intraday', 'Intraday to Swing', 'Swing to Medium-Term']),
      synthesizedReason: `${votes.BUY}/3 agents favour BUY on ${a.symbol}. ${consensus === 'BUY' ? 'Technical momentum aligns with fundamentals.' : 'Mixed signals - capital preservation preferred.'}`,
      keyCatalysts: [pick(['Volume expansion above 20-day average', 'Sector rotation tailwind', 'Earnings beat expectations', 'Breakout above resistance'])],
      riskFactors: [`Overhead resistance at ${a.resistance}`, `Sector volatility in ${a.sector}`],
      invalidatingConditions: [`Close below ${a.support} on high volume`, 'MACD histogram turning negative on 4H chart'],
      isNoTradeAdvised: consensus === 'NO_TRADE' || consensus === 'WAIT', riskApproved: approved,
      riskStatus: approved ? (riskAvg > 55 ? 'CAUTION' : 'SAFE') : rejection.includes('LOCKED') || rejection.includes('locked') ? 'LOCKED' : 'CAUTION',
      rejectionReason: rejection, actedOn: approved ? pick(['EXECUTED', 'EXECUTED', 'IGNORED']) : rand() < 0.12 ? 'OVERRIDDEN' : 'BLOCKED',
      liveApi: rand() < 0.35, latencyMs: ri(380, 2400), agentBreakdowns: breakdown,
      createdAt: ago(ri(0, 30), ri(0, 23)),
    });
  }
  await CouncilDecision.insertMany(decisions);

  // Broker connections
  const conns = [];
  for (const u of users.filter((x) => x.brokerIntegration !== 'Simulated Direct API')) {
    const b = BROKERS.find((x) => x.shortName === u.brokerIntegration || x.name.includes(u.brokerIntegration.split(' ')[0])) || BROKERS[0];
    const status = pick(['ACTIVE', 'ACTIVE', 'ACTIVE', 'TOKEN_EXPIRING', 'DISCONNECTED']);
    const margin = pick([25000, 50000, 75000, 100000, 200000]);
    conns.push({
      user: u._id, brokerCode: b.code, brokerName: b.name, brokerUserId: `${b.code.slice(0, 2)}${ri(1000, 9999)}`,
      brokerUserName: u.name, status, isConnected: status !== 'DISCONNECTED',
      isLiveRoutingActive: status === 'ACTIVE' && rand() < 0.7, autoPilotAutonomousLive: u.autoPilot.autonomousLive,
      liveMargin: margin, usedMargin: +(margin * rf(0, 0.6)).toFixed(0), availableCollateral: +(margin * rf(0.2, 0.6)).toFixed(0),
      latencyMs: ri(9, 120), tokenExpiresAt: status === 'TOKEN_EXPIRING' ? ago(-0.1) : ago(-ri(1, 20)),
      isEncryptedInVault: true, scopes: b.defaultScopes, lastSyncedAt: ago(0, ri(0, 30)),
    });
  }
  await BrokerConnection.insertMany(conns);

  // Fund transactions
  const funds = [];
  for (let i = 0; i < 180; i++) {
    const u = pick(users);
    const type = rand() < 0.68 ? 'DEPOSIT' : 'WITHDRAWAL';
    const status = i < 9 ? 'PENDING' : rand() < 0.05 ? 'FAILED' : 'SUCCESS';
    funds.push({
      user: u._id, txId: id('TX'), type, amount: pick([2000, 5000, 10000, 15000, 25000, 50000]),
      paymentMethod: pick(PAYMENT), utrNumber: `${ri(100000, 999999)}${ri(100000, 999999)}`, status,
      brokerName: u.brokerIntegration === 'Simulated Direct API' ? 'Zerodha Kite' : u.brokerIntegration,
      notes: type === 'DEPOSIT' ? 'Live margin top-up' : 'Withdrawal to linked bank account',
      createdAt: ago(i < 9 ? 0 : ri(0, 60), ri(0, 23)),
    });
  }
  await FundTransaction.insertMany(funds);

  // Custom strategies (Strategy Lab)
  const prompts = [
    ['Gap-Up Fade', 'Short stocks gapping up >3% that fail to hold the opening range', 'Gap > 3% + first 15m candle red', 'VWAP touch or 1.5R'],
    ['Bank Nifty Opening Range', 'Trade the 9:15-9:30 range breakout on bank stocks', 'Close above ORH with volume', 'Opposite side of range or 2R'],
    ['Dividend Momentum', 'Buy dividend aristocrats breaking 50-day highs', '50-day high + yield > 1.5%', '10% trailing stop'],
    ['NASDAQ AI Leaders', 'Swing semiconductor leaders on pullbacks to 20-EMA', 'Pullback to 20-EMA + RSI > 45', 'Close below 50-SMA'],
    ['Low-Vol Defensive', 'FMCG & pharma names with low beta during market stress', 'Beta < 0.8 + index below 20-SMA', 'Index reclaims 20-SMA'],
  ];
  const customs = [];
  for (let i = 0; i < 26; i++) {
    const [name, prompt, entry, exit] = pick(prompts);
    customs.push({
      user: pick(users)._id, name: `${name}${i > 4 ? ` v${ri(2, 4)}` : ''}`, prompt, capitalAllocation: pick([5000, 10000, 15000, 25000]),
      maxRiskPercent: pick([0.5, 1, 1.5, 2]), entryCriteria: entry, exitCriteria: exit,
      status: i < 5 ? 'UNDER_REVIEW' : pick(['PAPER_TRADING_ONLY', 'PAPER_TRADING_ONLY', 'APPROVED_LIVE', 'REJECTED', 'ARCHIVED']),
      createdAt: ago(ri(0, 60)),
    });
  }
  await CustomStrategy.insertMany(customs);

  // Backtests
  const bts = [];
  for (let i = 0; i < 70; i++) {
    const st = pick(STRATEGIES), a = pick(tradables);
    const total = ri(8, 40), wins = ri(Math.floor(total * 0.35), Math.floor(total * 0.8));
    const ret = rf(-8, 18);
    const capital = 25000;
    const curve = [];
    let eq = capital;
    for (let d = 0; d < 30; d++) { eq += (capital * ret / 100) / 30 + rf(-180, 180); curve.push({ date: ago(30 - d).toISOString().slice(0, 10), equity: Math.round(eq) }); }
    const pf = rf(0.7, 2.8);
    bts.push({
      user: pick(users)._id,
      config: { strategyId: st.key, strategyName: st.name, symbol: a.symbol, market: a.market, timeframeDays: pick([30, 60, 90, 180]), startingCapital: capital, maxRiskPerTradePercent: 1, stopLossPercent: 1.5, takeProfitPercent: 3, maxHoldingDays: 8 },
      totalTrades: total, winCount: wins, lossCount: total - wins, breakevenCount: 0, winRatePercent: +((wins / total) * 100).toFixed(1),
      finalEquity: Math.round(capital * (1 + ret / 100)), netPnl: Math.round(capital * ret / 100), totalReturnPercent: ret,
      profitFactor: pf, maxDrawdownPercent: rf(1.2, 12), sharpeRatio: rf(-0.4, 2.6), avgHoldingDays: rf(1, 6, 1), payoffRatio: rf(0.8, 2.4),
      equityCurve: curve, qualificationStatus: ret > 5 && pf > 1.5 ? 'QUALIFIED' : ret > 0 ? 'NEEDS_TUNING' : 'REJECTED',
      aiCouncilCritique: ret > 5 ? 'Robust edge with controlled drawdown. Eligible for paper-to-live graduation.' : ret > 0 ? 'Marginal edge; tighten stop-loss and filter low-volume sessions.' : 'Negative expectancy. Strategy rejected by AI Council review.',
      createdAt: ago(ri(0, 45)),
    });
  }
  await Backtest.insertMany(bts);

  // AI Copilot sessions
  const topics = { MARKET_ANALYSIS: ['Is RELIANCE a buy today?', 'Explain NIFTY trend this week'], RISK: ['Why was my trade blocked?', 'How does the kill switch work?'], STRATEGY: ['Build a mean reversion strategy', 'Improve my breakout rules'], PORTFOLIO: ['Am I over-exposed to banking?', 'Rebalance my positions'], EDUCATION: ['What is MACD divergence?', 'Explain position sizing with 1% risk'], OTHER: ['How do I connect Zerodha?'] };
  const sessions = [];
  for (let i = 0; i < 240; i++) {
    const topic = pick(Object.keys(topics));
    const title = pick(topics[topic]);
    const flagged = rand() < 0.03;
    sessions.push({
      user: pick(users)._id, title, topic, model: pick(['gemini-2.5-flash', 'gemini-2.5-flash', 'gemini-2.5-pro']),
      messageCount: ri(2, 28), tokensUsed: ri(800, 24000), flagged, flagReason: flagged ? 'User requested guaranteed-return advice' : '',
      lastMessagePreview: `${title.slice(0, 60)} …`, rating: ri(3, 5), createdAt: ago(ri(0, 30), ri(0, 23)),
    });
  }
  await CopilotSession.insertMany(sessions);

  // Audit logs (system / AI / user events like the app's audit_logs table)
  const auditActions = [
    ['AI_CONSENSUS_GENERATED', 'AI', 'INFO'], ['RISK_ENGINE_APPROVED', 'SYSTEM', 'INFO'], ['RISK_ENGINE_REJECTED', 'SYSTEM', 'WARNING'],
    ['PAPER_ORDER_EXECUTED', 'SYSTEM', 'INFO'], ['LIVE_ORDER_ROUTED', 'SYSTEM', 'INFO'], ['AUTOPILOT_TARGET_ACHIEVED', 'SYSTEM', 'INFO'],
    ['AUTOPILOT_LOSS_LIMIT_STOP', 'SYSTEM', 'WARNING'], ['KILL_SWITCH_TRIGGERED', 'USER', 'CRITICAL'], ['DAILY_LOSS_LIMIT_LOCK', 'SYSTEM', 'CRITICAL'],
    ['MANUAL_JOURNAL_TRADE_LOGGED', 'USER', 'INFO'], ['BROKER_OAUTH_CONNECTED', 'USER', 'INFO'], ['BROKER_TOKEN_EXPIRING', 'SYSTEM', 'WARNING'],
    ['USER_LOGIN_2FA', 'USER', 'INFO'], ['BIOMETRIC_UNLOCK_FAILED', 'USER', 'WARNING'], ['FUNDS_DEPOSITED', 'USER', 'INFO'],
  ];
  const logs = [];
  for (let i = 0; i < 600; i++) {
    const [action, actorType, severity] = pick(auditActions);
    const u = pick(users), a = pick(assets);
    logs.push({
      user: u._id, actorType, actorName: actorType === 'USER' ? u.name : actorType === 'AI' ? 'AI Council' : 'Risk Engine',
      action, symbol: a.symbol, market: a.market, aiConsensusVerdict: pick(DECISIONS), riskEngineStatus: pick(['SAFE', 'SAFE', 'CAUTION', 'LOCKED']),
      severity, details: `${action.replace(/_/g, ' ').toLowerCase()} for ${a.symbol}`, createdAt: ago(ri(0, 30), ri(0, 23)),
    });
  }
  await AuditLog.insertMany(logs);

  console.log(`[seed] done: ${users.length} users, ${trades.length} trades, ${positions.length} positions, ${decisions.length} council decisions`);
}

// Allow `npm run seed` (wipes and reseeds the configured database)
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const { connectDb, disconnectDb } = await import('./db.js');
  if (!process.env.MONGODB_URI) {
    console.error('Set MONGODB_URI to seed a persistent database (in-memory mode seeds automatically on start).');
    process.exit(1);
  }
  await connectDb();
  await seedDatabase({ reset: true });
  await disconnectDb();
}
