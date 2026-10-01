import { Router } from 'express';
import {
  User, Portfolio, Position, Trade, FundTransaction, CustomStrategy, BrokerConnection,
  CopilotSession, Broker, AuditLog, getSettings,
} from '../models/index.js';
import { requireRole } from '../middleware/auth.js';
import { logAdminAction } from '../utils/audit.js';

/**
 * Domain-specific admin operations that go beyond plain CRUD:
 * user lifecycle, risk controls (locks / kill switch), position force-close,
 * fund approvals, strategy review, broker revocation and platform settings.
 */
const router = Router();
const risk = requireRole('RISK_OFFICER');
const support = requireRole('RISK_OFFICER', 'SUPPORT');

// ---------------------------------------------------------------- Users
router.get('/users/:id/overview', async (req, res) => {
  const user = await User.findById(req.params.id).lean();
  if (!user) return res.status(404).json({ error: 'User not found' });
  const [portfolio, positions, trades, funds, brokers, audit, tradeStats, copilot, strategies] = await Promise.all([
    Portfolio.findOne({ user: user._id }).lean(),
    Position.find({ user: user._id }).lean(),
    Trade.find({ user: user._id }).sort('-entryTime').limit(25).lean(),
    FundTransaction.find({ user: user._id }).sort('-createdAt').limit(20).lean(),
    BrokerConnection.find({ user: user._id }).lean(),
    AuditLog.find({ user: user._id }).sort('-createdAt').limit(30).lean(),
    Trade.aggregate([
      { $match: { user: user._id, status: 'CLOSED' } },
      { $group: { _id: null, count: { $sum: 1 }, pnl: { $sum: '$pnl' }, wins: { $sum: { $cond: [{ $eq: ['$outcome', 'PROFIT'] }, 1, 0] } } } },
    ]),
    CopilotSession.countDocuments({ user: user._id }),
    CustomStrategy.find({ user: user._id }).lean(),
  ]);
  res.json({ user, portfolio, positions, trades, funds, brokers, audit, tradeStats: tradeStats[0] || { count: 0, pnl: 0, wins: 0 }, copilotSessions: copilot, strategies });
});

router.post('/users/:id/status', support, async (req, res) => {
  const { status, reason } = req.body;
  if (!['ACTIVE', 'SUSPENDED', 'PENDING_KYC'].includes(status)) return res.status(400).json({ error: 'Invalid status' });
  const user = await User.findByIdAndUpdate(req.params.id, { status }, { new: true });
  if (!user) return res.status(404).json({ error: 'User not found' });
  if (status === 'SUSPENDED') {
    await Portfolio.updateOne({ user: user._id }, { isLocked: true, lockReason: `Account suspended by admin: ${reason || 'policy review'}` });
    await User.updateOne({ _id: user._id }, { 'autoPilot.enabled': false, 'autoPilot.state': 'OFF' });
  }
  await logAdminAction(req, `ADMIN_USER_${status}`, { user: user._id, severity: status === 'SUSPENDED' ? 'WARNING' : 'INFO', details: reason || '' });
  res.json(user);
});

router.post('/users/:id/trading-mode', risk, async (req, res) => {
  const { tradingMode } = req.body;
  const user = await User.findByIdAndUpdate(req.params.id, { tradingMode }, { new: true, runValidators: true });
  if (!user) return res.status(404).json({ error: 'User not found' });
  await Portfolio.updateOne({ user: user._id }, { tradingMode });
  await logAdminAction(req, 'ADMIN_TRADING_MODE_CHANGED', { user: user._id, details: `Mode set to ${tradingMode}` });
  res.json(user);
});

router.post('/users/:id/autopilot', risk, async (req, res) => {
  const enabled = !!req.body.enabled;
  const update = { 'autoPilot.enabled': enabled };
  if (!enabled) Object.assign(update, { 'autoPilot.state': 'OFF', 'autoPilot.autonomousLive': false, 'autoPilot.lastMessage': 'Disabled by admin' });
  const user = await User.findByIdAndUpdate(req.params.id, update, { new: true });
  if (!user) return res.status(404).json({ error: 'User not found' });
  await logAdminAction(req, enabled ? 'ADMIN_AUTOPILOT_ENABLED' : 'ADMIN_AUTOPILOT_DISABLED', { user: user._id, severity: enabled ? 'INFO' : 'WARNING' });
  res.json(user);
});

// ---------------------------------------------------------------- Risk controls
router.patch('/portfolios/:id/limits', risk, async (req, res) => {
  const allowed = ['dailyTarget', 'maxDailyLoss', 'maxRiskPerTradePercent', 'maxOpenPositions', 'maxExposurePercent', 'profitLockEnabled', 'startingCapital'];
  const update = Object.fromEntries(Object.entries(req.body).filter(([k]) => allowed.includes(k)));
  const p = await Portfolio.findByIdAndUpdate(req.params.id, update, { new: true });
  if (!p) return res.status(404).json({ error: 'Portfolio not found' });
  await logAdminAction(req, 'ADMIN_RISK_LIMITS_UPDATED', { user: p.user, details: JSON.stringify(update) });
  res.json(p);
});

router.post('/portfolios/:id/lock', risk, async (req, res) => {
  const lock = !!req.body.lock;
  const p = await Portfolio.findByIdAndUpdate(
    req.params.id,
    { isLocked: lock, lockReason: lock ? req.body.reason || 'Manual lock by risk officer' : '' },
    { new: true }
  );
  if (!p) return res.status(404).json({ error: 'Portfolio not found' });
  await logAdminAction(req, lock ? 'ADMIN_TRADING_LOCKED' : 'ADMIN_TRADING_UNLOCKED', { user: p.user, severity: 'WARNING', details: p.lockReason });
  res.json(p);
});

router.post('/portfolios/:id/kill-switch', risk, async (req, res) => {
  const engage = !!req.body.engage;
  const p = await Portfolio.findByIdAndUpdate(req.params.id, { killSwitchTriggered: engage }, { new: true });
  if (!p) return res.status(404).json({ error: 'Portfolio not found' });
  if (engage) await User.updateOne({ _id: p.user }, { 'autoPilot.enabled': false, 'autoPilot.state': 'LOCKED' });
  await logAdminAction(req, engage ? 'ADMIN_KILL_SWITCH_ENGAGED' : 'ADMIN_KILL_SWITCH_RELEASED', {
    user: p.user, severity: 'CRITICAL', details: req.body.reason || '',
  });
  res.json(p);
});

router.post('/risk/global-kill-switch', requireRole(), async (req, res) => {
  const engage = !!req.body.engage;
  const s = await getSettings();
  s.globalKillSwitch = engage;
  s.globalKillSwitchReason = engage ? req.body.reason || 'Emergency halt' : '';
  await s.save();
  await Portfolio.updateMany({}, { killSwitchTriggered: engage });
  if (engage) await User.updateMany({}, { 'autoPilot.enabled': false, 'autoPilot.state': 'LOCKED' });
  await logAdminAction(req, engage ? 'ADMIN_GLOBAL_KILL_SWITCH_ENGAGED' : 'ADMIN_GLOBAL_KILL_SWITCH_RELEASED', {
    severity: 'CRITICAL', details: s.globalKillSwitchReason,
  });
  res.json(s);
});

router.post('/risk/reset-daily', requireRole(), async (req, res) => {
  const r = await Portfolio.updateMany({}, { realizedPnlToday: 0 });
  await logAdminAction(req, 'ADMIN_DAILY_PNL_RESET', { severity: 'WARNING', details: `${r.modifiedCount} portfolios reset` });
  res.json({ modified: r.modifiedCount });
});

// ---------------------------------------------------------------- Positions
router.post('/positions/:id/force-close', risk, async (req, res) => {
  const pos = await Position.findById(req.params.id);
  if (!pos) return res.status(404).json({ error: 'Position not found' });
  const portfolio = await Portfolio.findOne({ user: pos.user });
  const mult = pos.market === 'USA' ? portfolio?.usdInrRate || 83.5 : 1;
  const pnl = (pos.currentPrice - pos.buyPrice) * pos.quantity * mult;
  const pnlPercent = ((pos.currentPrice - pos.buyPrice) / pos.buyPrice) * 100;
  await Trade.create({
    user: pos.user,
    tradeId: `ADM-${Date.now().toString(36).toUpperCase()}`,
    symbol: pos.symbol, market: pos.market, companyName: pos.companyName,
    strategy: 'Admin Force Close', entryPrice: pos.buyPrice, exitPrice: pos.currentPrice,
    quantity: pos.quantity, stopLoss: pos.stopLoss, takeProfit: pos.takeProfit,
    pnl, pnlPercent, status: 'CLOSED', orderType: 'BUY', executionType: pos.executionType,
    entryTime: pos.openedAt, closedAt: new Date(),
    outcome: pnl > 0 ? 'PROFIT' : pnl < 0 ? 'LOSS' : 'BREAKEVEN',
    aiDecision: 'EXIT', userOverride: true, overrideReason: `Force-closed by admin: ${req.body.reason || 'risk intervention'}`,
    tags: 'Admin Intervention',
  });
  if (portfolio) {
    portfolio.availableCash += pos.currentPrice * pos.quantity * mult;
    portfolio.investedValue = Math.max(0, portfolio.investedValue - pos.buyPrice * pos.quantity * mult);
    portfolio.realizedPnlToday += pnl;
    portfolio.totalRealizedPnl += pnl;
    await portfolio.save();
  }
  await pos.deleteOne();
  await logAdminAction(req, 'ADMIN_POSITION_FORCE_CLOSED', {
    user: pos.user, symbol: pos.symbol, market: pos.market, severity: 'WARNING',
    details: `${pos.quantity} @ ${pos.currentPrice}, P&L ${pnl.toFixed(2)}. ${req.body.reason || ''}`,
  });
  res.json({ ok: true, pnl });
});

// ---------------------------------------------------------------- Funds
router.post('/funds/:id/review', risk, async (req, res) => {
  const { decision, note } = req.body; // APPROVE | REJECT
  const tx = await FundTransaction.findById(req.params.id);
  if (!tx) return res.status(404).json({ error: 'Transaction not found' });
  if (tx.status !== 'PENDING') return res.status(400).json({ error: 'Only pending transactions can be reviewed' });
  tx.status = decision === 'APPROVE' ? 'SUCCESS' : 'REJECTED';
  tx.reviewedBy = req.admin.email;
  if (note) tx.notes = `${tx.notes || ''} | Admin: ${note}`;
  await tx.save();
  if (tx.status === 'SUCCESS') {
    const delta = tx.type === 'DEPOSIT' ? tx.amount : -tx.amount;
    await Portfolio.updateOne({ user: tx.user }, { $inc: { availableCash: delta } });
  }
  await logAdminAction(req, `ADMIN_FUND_${tx.type}_${tx.status}`, { user: tx.user, details: `${tx.txId} ₹${tx.amount} ${note || ''}` });
  res.json(tx);
});

// ---------------------------------------------------------------- Strategy review
router.post('/custom-strategies/:id/review', risk, async (req, res) => {
  const { status, note } = req.body;
  const s = await CustomStrategy.findByIdAndUpdate(req.params.id, { status, reviewNote: note }, { new: true, runValidators: true });
  if (!s) return res.status(404).json({ error: 'Strategy not found' });
  await logAdminAction(req, `ADMIN_STRATEGY_${status}`, { user: s.user, details: `${s.name}: ${note || ''}` });
  res.json(s);
});

// ---------------------------------------------------------------- Brokers
router.post('/broker-connections/:id/revoke', risk, async (req, res) => {
  const c = await BrokerConnection.findByIdAndUpdate(
    req.params.id,
    { status: 'REVOKED', isConnected: false, isLiveRoutingActive: false, autoPilotAutonomousLive: false },
    { new: true }
  );
  if (!c) return res.status(404).json({ error: 'Connection not found' });
  await logAdminAction(req, 'ADMIN_BROKER_TOKEN_REVOKED', { user: c.user, severity: 'WARNING', details: `${c.brokerName} (${c.brokerUserId})` });
  res.json(c);
});

router.post('/brokers/:id/toggle', risk, async (req, res) => {
  const b = await Broker.findById(req.params.id);
  if (!b) return res.status(404).json({ error: 'Broker not found' });
  if ('enabled' in req.body) b.enabled = !!req.body.enabled;
  if ('liveTradingAllowed' in req.body) b.liveTradingAllowed = !!req.body.liveTradingAllowed;
  await b.save();
  await logAdminAction(req, 'ADMIN_BROKER_CATALOG_UPDATED', { details: `${b.code}: enabled=${b.enabled}, live=${b.liveTradingAllowed}` });
  res.json(b);
});

// ---------------------------------------------------------------- Copilot moderation
router.post('/copilot-sessions/:id/flag', support, async (req, res) => {
  const s = await CopilotSession.findByIdAndUpdate(
    req.params.id,
    { flagged: !!req.body.flagged, flagReason: req.body.reason || '' },
    { new: true }
  );
  if (!s) return res.status(404).json({ error: 'Session not found' });
  await logAdminAction(req, s.flagged ? 'ADMIN_COPILOT_SESSION_FLAGGED' : 'ADMIN_COPILOT_SESSION_UNFLAGGED', { user: s.user, details: s.flagReason });
  res.json(s);
});

// ---------------------------------------------------------------- Settings
router.get('/settings', async (req, res) => res.json(await getSettings()));

router.put('/settings', requireRole(), async (req, res) => {
  const s = await getSettings();
  const { _id, key, createdAt, updatedAt, __v, globalKillSwitch, ...body } = req.body;
  s.set(body);
  await s.save();
  await logAdminAction(req, 'ADMIN_PLATFORM_SETTINGS_UPDATED', { details: Object.keys(body).join(', ') });
  res.json(s);
});

// ---------------------------------------------------------------- CSV export
const csvCell = (v) => {
  if (v === null || v === undefined) return '';
  const s = v instanceof Date ? v.toISOString() : String(v);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

router.get('/export/trades.csv', async (req, res) => {
  const q = {};
  for (const f of ['status', 'executionType', 'market', 'outcome', 'strategy']) if (req.query[f] && req.query[f] !== 'ALL') q[f] = req.query[f];
  const trades = await Trade.find(q).sort('-entryTime').limit(10000).populate('user', 'name email').lean();
  const cols = ['tradeId', 'user', 'symbol', 'market', 'strategy', 'orderType', 'executionType', 'quantity', 'entryPrice', 'exitPrice', 'stopLoss', 'takeProfit', 'pnl', 'pnlPercent', 'outcome', 'status', 'aiDecision', 'aiConfidence', 'riskScore', 'userOverride', 'overrideReason', 'entryTime', 'closedAt'];
  const rows = trades.map((t) => cols.map((c) => csvCell(c === 'user' ? t.user?.email : t[c])).join(','));
  await logAdminAction(req, 'ADMIN_TRADES_EXPORTED', { details: `${trades.length} rows` });
  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', `attachment; filename="tradepilot-trades-${new Date().toISOString().slice(0, 10)}.csv"`);
  res.send([cols.join(','), ...rows].join('\n'));
});

export default router;
