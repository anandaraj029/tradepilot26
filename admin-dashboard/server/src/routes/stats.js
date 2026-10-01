import { Router } from 'express';
import {
  User, Portfolio, Trade, Position, CouncilDecision, FundTransaction, BrokerConnection,
  CopilotSession, AuditLog, Strategy, CustomStrategy, Backtest, getSettings,
} from '../models/index.js';

const router = Router();
const DAY = 86400000;
const startOfDay = (d = new Date()) => new Date(d.getFullYear(), d.getMonth(), d.getDate());
const TZ = process.env.TZ_NAME || Intl.DateTimeFormat().resolvedOptions().timeZone;
const dayKey = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

/** Headline KPIs + chart series for the overview page. */
router.get('/overview', async (req, res) => {
  const days = Math.min(90, parseInt(req.query.days) || 30);
  const since = new Date(startOfDay().getTime() - (days - 1) * DAY);
  const today = startOfDay();

  const [
    totalUsers, activeUsers, suspendedUsers, newUsers, modeBreakdown, autoPilotOn,
    portfolioAgg, lockedCount, killSwitchCount, openPositions, tradeAgg, todayTrades,
    pnlSeries, signupSeries, decisionBreakdown, pendingFunds, fundAgg, brokerAgg,
    copilotAgg, strategyPerf, recentAlerts, settings, pendingStrategies,
  ] = await Promise.all([
    User.countDocuments(),
    User.countDocuments({ status: 'ACTIVE' }),
    User.countDocuments({ status: 'SUSPENDED' }),
    User.countDocuments({ createdAt: { $gte: since } }),
    User.aggregate([{ $group: { _id: '$tradingMode', count: { $sum: 1 } } }]),
    User.countDocuments({ 'autoPilot.enabled': true }),
    Portfolio.aggregate([{
      $group: {
        _id: null,
        capital: { $sum: '$startingCapital' },
        cash: { $sum: '$availableCash' },
        invested: { $sum: '$investedValue' },
        realizedToday: { $sum: '$realizedPnlToday' },
        unrealized: { $sum: '$unrealizedPnl' },
        targetsHit: { $sum: { $cond: [{ $gte: ['$realizedPnlToday', '$dailyTarget'] }, 1, 0] } },
        lossLimitsHit: { $sum: { $cond: [{ $lte: ['$realizedPnlToday', { $multiply: ['$maxDailyLoss', -1] }] }, 1, 0] } },
      },
    }]),
    Portfolio.countDocuments({ isLocked: true }),
    Portfolio.countDocuments({ killSwitchTriggered: true }),
    Position.countDocuments(),
    Trade.aggregate([
      { $match: { status: 'CLOSED' } },
      {
        $group: {
          _id: null,
          count: { $sum: 1 },
          pnl: { $sum: '$pnl' },
          wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } },
          live: { $sum: { $cond: [{ $eq: ['$executionType', 'LIVE'] }, 1, 0] } },
          overrides: { $sum: { $cond: ['$userOverride', 1, 0] } },
          avgConfidence: { $avg: '$aiConfidence' },
        },
      },
    ]),
    Trade.countDocuments({ entryTime: { $gte: today } }),
    Trade.aggregate([
      { $match: { status: 'CLOSED', closedAt: { $gte: since } } },
      {
        $group: {
          _id: { $dateToString: { format: '%Y-%m-%d', timezone: TZ, date: '$closedAt' } },
          pnl: { $sum: '$pnl' },
          paper: { $sum: { $cond: [{ $eq: ['$executionType', 'PAPER'] }, '$pnl', 0] } },
          live: { $sum: { $cond: [{ $eq: ['$executionType', 'LIVE'] }, '$pnl', 0] } },
          trades: { $sum: 1 },
          wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } },
        },
      },
      { $sort: { _id: 1 } },
    ]),
    User.aggregate([
      { $match: { createdAt: { $gte: since } } },
      { $group: { _id: { $dateToString: { format: '%Y-%m-%d', timezone: TZ, date: '$createdAt' } }, count: { $sum: 1 } } },
      { $sort: { _id: 1 } },
    ]),
    CouncilDecision.aggregate([{ $group: { _id: '$consensusDecision', count: { $sum: 1 } } }]),
    FundTransaction.countDocuments({ status: 'PENDING' }),
    FundTransaction.aggregate([
      { $match: { status: 'SUCCESS' } },
      { $group: { _id: '$type', total: { $sum: '$amount' }, count: { $sum: 1 } } },
    ]),
    BrokerConnection.aggregate([{ $group: { _id: '$status', count: { $sum: 1 }, margin: { $sum: '$liveMargin' } } }]),
    CopilotSession.aggregate([
      { $group: { _id: null, sessions: { $sum: 1 }, messages: { $sum: '$messageCount' }, tokens: { $sum: '$tokensUsed' }, flagged: { $sum: { $cond: ['$flagged', 1, 0] } } } },
    ]),
    Trade.aggregate([
      { $match: { status: 'CLOSED' } },
      {
        $group: {
          _id: '$strategy',
          pnl: { $sum: '$pnl' },
          trades: { $sum: 1 },
          wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } },
        },
      },
      { $sort: { pnl: -1 } },
    ]),
    AuditLog.find({ severity: { $in: ['WARNING', 'CRITICAL'] } }).sort('-createdAt').limit(8).populate('user', 'name email').lean(),
    getSettings(),
    CustomStrategy.countDocuments({ status: 'UNDER_REVIEW' }),
  ]);

  // Fill missing days so charts are continuous
  const pnlMap = Object.fromEntries(pnlSeries.map((d) => [d._id, d]));
  const signupMap = Object.fromEntries(signupSeries.map((d) => [d._id, d.count]));
  const series = [];
  let cumulative = 0;
  for (let i = 0; i < days; i++) {
    const d = new Date(since.getTime() + i * DAY);
    const key = dayKey(d);
    const p = pnlMap[key] || { pnl: 0, paper: 0, live: 0, trades: 0, wins: 0 };
    cumulative += p.pnl;
    series.push({
      date: key, pnl: Math.round(p.pnl), paper: Math.round(p.paper), live: Math.round(p.live),
      trades: p.trades, winRate: p.trades ? Math.round((p.wins / p.trades) * 100) : null,
      cumulative: Math.round(cumulative), signups: signupMap[key] || 0,
    });
  }

  const t = tradeAgg[0] || { count: 0, pnl: 0, wins: 0, live: 0, overrides: 0, avgConfidence: 0 };
  const p = portfolioAgg[0] || {};
  const funds = Object.fromEntries(fundAgg.map((f) => [f._id, f]));

  res.json({
    kpis: {
      totalUsers, activeUsers, suspendedUsers, newUsers, autoPilotOn,
      totalCapital: p.capital || 0, totalCash: p.cash || 0, totalInvested: p.invested || 0,
      realizedToday: p.realizedToday || 0, unrealized: p.unrealized || 0,
      targetsHit: p.targetsHit || 0, lossLimitsHit: p.lossLimitsHit || 0,
      lockedCount, killSwitchCount, openPositions, todayTrades,
      closedTrades: t.count, totalPnl: t.pnl, winRate: t.count ? (t.wins / t.count) * 100 : 0,
      liveTrades: t.live, overrides: t.overrides, avgConfidence: t.avgConfidence || 0,
      pendingFunds, deposits: funds.DEPOSIT?.total || 0, withdrawals: funds.WITHDRAWAL?.total || 0,
      pendingStrategies,
    },
    series,
    modeBreakdown: modeBreakdown.map((m) => ({ mode: m._id, count: m.count })),
    decisionBreakdown: decisionBreakdown.map((d) => ({ decision: d._id, count: d.count })),
    brokerBreakdown: brokerAgg.map((b) => ({ status: b._id, count: b.count, margin: b.margin })),
    copilot: copilotAgg[0] || { sessions: 0, messages: 0, tokens: 0, flagged: 0 },
    strategyPerf: strategyPerf.map((s) => ({
      strategy: s._id, pnl: Math.round(s.pnl), trades: s.trades, winRate: Math.round((s.wins / s.trades) * 100),
    })),
    recentAlerts,
    platform: {
      globalKillSwitch: settings.globalKillSwitch,
      maintenanceMode: settings.maintenanceMode,
      liveTradingEnabled: settings.liveTradingEnabled,
      autoPilotEnabled: settings.autoPilotEnabled,
    },
  });
});

/** AI Council analytics: per-agent accuracy & vote mix. */
router.get('/ai-council', async (req, res) => {
  const [agents, verdictVsAction, totals, liveShare] = await Promise.all([
    CouncilDecision.aggregate([
      { $unwind: '$agentBreakdowns' },
      {
        $group: {
          _id: '$agentBreakdowns.agentName',
          role: { $first: '$agentBreakdowns.agentRole' },
          calls: { $sum: 1 },
          avgConfidence: { $avg: '$agentBreakdowns.confidence' },
          avgRisk: { $avg: '$agentBreakdowns.riskScore' },
          avgTech: { $avg: '$agentBreakdowns.technicalScore' },
          avgFund: { $avg: '$agentBreakdowns.fundamentalScore' },
          avgSent: { $avg: '$agentBreakdowns.sentimentScore' },
          buy: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', 'BUY'] }, 1, 0] } },
          sell: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', 'SELL'] }, 1, 0] } },
          hold: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', 'HOLD'] }, 1, 0] } },
          wait: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', 'WAIT'] }, 1, 0] } },
          noTrade: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', 'NO_TRADE'] }, 1, 0] } },
          agreed: { $sum: { $cond: [{ $eq: ['$agentBreakdowns.decision', '$consensusDecision'] }, 1, 0] } },
        },
      },
      { $sort: { _id: 1 } },
    ]),
    CouncilDecision.aggregate([{ $group: { _id: '$actedOn', count: { $sum: 1 } } }]),
    CouncilDecision.aggregate([
      {
        $group: {
          _id: null,
          count: { $sum: 1 },
          avgConfidence: { $avg: '$overallConfidence' },
          riskApproved: { $sum: { $cond: ['$riskApproved', 1, 0] } },
          noTrade: { $sum: { $cond: ['$isNoTradeAdvised', 1, 0] } },
          avgLatency: { $avg: '$latencyMs' },
        },
      },
    ]),
    CouncilDecision.countDocuments({ liveApi: true }),
  ]);
  res.json({
    agents,
    actions: verdictVsAction.map((a) => ({ action: a._id, count: a.count })),
    totals: { ...(totals[0] || {}), liveApi: liveShare },
  });
});

/** Daily report: mirrors the app's Daily Summary screen, aggregated across all traders. */
router.get('/daily-report', async (req, res) => {
  const day = req.query.date ? new Date(`${req.query.date}T00:00:00`) : new Date();
  const from = startOfDay(day);
  const to = new Date(from.getTime() + DAY);
  const match = { status: 'CLOSED', closedAt: { $gte: from, $lt: to } };

  const [summary, byStrategy, byUser, byMarket, behaviour, trades] = await Promise.all([
    Trade.aggregate([
      { $match: match },
      {
        $group: {
          _id: null,
          trades: { $sum: 1 },
          pnl: { $sum: '$pnl' },
          wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } },
          losses: { $sum: { $cond: [{ $eq: ['$outcome', 'LOSS'] }, 1, 0] } },
          grossProfit: { $sum: { $cond: [{ $gt: ['$pnl', 0] }, '$pnl', 0] } },
          grossLoss: { $sum: { $cond: [{ $lt: ['$pnl', 0] }, '$pnl', 0] } },
          best: { $max: '$pnl' },
          worst: { $min: '$pnl' },
          charges: { $sum: '$charges' },
        },
      },
    ]),
    Trade.aggregate([
      { $match: match },
      { $group: { _id: '$strategy', pnl: { $sum: '$pnl' }, trades: { $sum: 1 }, wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } } } },
      { $sort: { pnl: -1 } },
    ]),
    Trade.aggregate([
      { $match: match },
      { $group: { _id: '$user', pnl: { $sum: '$pnl' }, trades: { $sum: 1 } } },
      { $sort: { pnl: -1 } },
      { $limit: 10 },
      { $lookup: { from: 'users', localField: '_id', foreignField: '_id', as: 'user' } },
      { $unwind: '$user' },
      { $project: { pnl: 1, trades: 1, name: '$user.name', email: '$user.email' } },
    ]),
    Trade.aggregate([{ $match: match }, { $group: { _id: '$market', pnl: { $sum: '$pnl' }, trades: { $sum: 1 } } }]),
    Trade.aggregate([
      { $match: match },
      {
        $group: {
          _id: '$userOverride',
          trades: { $sum: 1 },
          wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } },
          pnl: { $sum: '$pnl' },
        },
      },
    ]),
    Trade.find(match).sort('-closedAt').limit(50).populate('user', 'name email').lean(),
  ]);

  res.json({
    date: dayKey(from),
    summary: summary[0] || { trades: 0, pnl: 0, wins: 0, losses: 0, grossProfit: 0, grossLoss: 0, best: 0, worst: 0, charges: 0 },
    byStrategy: byStrategy.map((s) => ({ strategy: s._id, pnl: Math.round(s.pnl), trades: s.trades, winRate: Math.round((s.wins / s.trades) * 100) })),
    topTraders: byUser,
    byMarket: byMarket.map((m) => ({ market: m._id, pnl: Math.round(m.pnl), trades: m.trades })),
    behaviour: {
      aligned: behaviour.find((b) => b._id === false) || { trades: 0, wins: 0, pnl: 0 },
      overrides: behaviour.find((b) => b._id === true) || { trades: 0, wins: 0, pnl: 0 },
    },
    trades,
  });
});

/** AutoPilot engine state counts across all traders. */
router.get("/autopilot", async (req, res) => {
  const states = await User.aggregate([{ $match: { "autoPilot.enabled": true } }, { $group: { _id: "$autoPilot.state", count: { $sum: 1 } } }]);
  res.json({ total: states.reduce((a, x) => a + x.count, 0), states: Object.fromEntries(states.map((x) => [x._id, x.count])) });
});

/** Strategy Lab totals for the strategies page header. */
router.get('/strategies', async (req, res) => {
  const [system, custom, backtests] = await Promise.all([
    Strategy.countDocuments({ status: 'ACTIVE' }),
    CustomStrategy.aggregate([{ $group: { _id: '$status', count: { $sum: 1 } } }]),
    Backtest.aggregate([{ $group: { _id: '$qualificationStatus', count: { $sum: 1 }, avgReturn: { $avg: '$totalReturnPercent' } } }]),
  ]);
  res.json({ activeSystem: system, custom, backtests });
});

export default router;
