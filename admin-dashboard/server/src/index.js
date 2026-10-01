import 'dotenv/config';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import express from 'express';
import cors from 'cors';
import morgan from 'morgan';
import { connectDb } from './db.js';
import { requireAuth } from './middleware/auth.js';
import { crudRouter } from './utils/crud.js';
import authRoutes from './routes/auth.js';
import statsRoutes from './routes/stats.js';
import actionRoutes from './routes/actions.js';
import { seedDatabase } from './seed.js';
import {
  User, Portfolio, Asset, Trade, Position, AuditLog, Strategy, CustomStrategy, Backtest,
  CouncilDecision, CopilotSession, Broker, BrokerConnection, FundTransaction, Admin,
} from './models/index.js';

const app = express();
app.use(cors({ origin: process.env.CLIENT_ORIGIN?.split(',') || true }));
app.use(express.json({ limit: '1mb' }));
app.use(morgan('dev'));

app.get('/api/health', (req, res) => res.json({ ok: true, time: new Date() }));
app.use('/api/auth', authRoutes);

const api = express.Router();
api.use(requireAuth);
api.use('/stats', statsRoutes);
api.use('/', actionRoutes);
api.use('/users', crudRouter(User, { searchFields: ['name', 'email', 'phone'], filters: ['status', 'tradingMode', 'tier', 'kycStatus', 'autoPilot.enabled', 'autoPilot.state'], writeRoles: ['SUPPORT', 'RISK_OFFICER'] }));
api.use('/portfolios', crudRouter(Portfolio, { filters: ['isLocked', 'killSwitchTriggered', 'tradingMode'], populate: 'user', defaultSort: 'realizedPnlToday' }));
api.use('/assets', crudRouter(Asset, { searchFields: ['symbol', 'name', 'sector'], filters: ['market', 'exchange', 'assetClass', 'sector', 'isTradable'], defaultSort: 'symbol', writeRoles: ['RISK_OFFICER', 'ANALYST'] }));
api.use('/trades', crudRouter(Trade, { searchFields: ['tradeId', 'symbol', 'companyName', 'strategy'], filters: ['status', 'executionType', 'market', 'outcome', 'strategy', 'source', 'userOverride', 'user'], populate: 'user', defaultSort: '-entryTime' }));
api.use('/positions', crudRouter(Position, { searchFields: ['symbol', 'companyName'], filters: ['market', 'executionType', 'user'], populate: 'user', writable: false }));
api.use('/audit-logs', crudRouter(AuditLog, { searchFields: ['action', 'symbol', 'details', 'actorName'], filters: ['actorType', 'severity', 'market', 'action', 'user'], populate: 'user', writable: false }));
api.use('/strategies', crudRouter(Strategy, { searchFields: ['name', 'category'], filters: ['status'], defaultSort: 'key' }));
api.use('/custom-strategies', crudRouter(CustomStrategy, { searchFields: ['name', 'prompt'], filters: ['status', 'user'], populate: 'user' }));
api.use('/backtests', crudRouter(Backtest, { searchFields: ['config.symbol', 'config.strategyName'], filters: ['qualificationStatus', 'config.market', 'user'], populate: 'user', writable: false }));
api.use('/council-decisions', crudRouter(CouncilDecision, { searchFields: ['symbol', 'synthesizedReason'], filters: ['consensusDecision', 'market', 'riskApproved', 'actedOn', 'riskStatus', 'user'], populate: 'user', writable: false }));
api.use('/copilot-sessions', crudRouter(CopilotSession, { searchFields: ['title', 'lastMessagePreview'], filters: ['topic', 'flagged', 'model', 'user'], populate: 'user', writable: false }));
api.use('/brokers', crudRouter(Broker, { searchFields: ['name', 'code'], filters: ['enabled'], defaultSort: 'code' }));
api.use('/broker-connections', crudRouter(BrokerConnection, { searchFields: ['brokerName', 'brokerUserId', 'brokerUserName'], filters: ['status', 'brokerCode', 'isLiveRoutingActive', 'user'], populate: 'user', writable: false }));
api.use('/funds', crudRouter(FundTransaction, { searchFields: ['txId', 'utrNumber', 'paymentMethod'], filters: ['type', 'status', 'brokerName', 'user'], populate: 'user', writable: false }));
app.use('/api', api);

// Serve the built React client in production
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const clientDist = path.resolve(__dirname, '../../client/dist');
if (fs.existsSync(clientDist)) {
  app.use(express.static(clientDist));
  app.get(/^\/(?!api).*/, (req, res) => res.sendFile('index.html', { root: clientDist }));
}

app.use((err, req, res, next) => {
  console.error(err);
  const status = err.status || (err.name === 'ValidationError' || err.name === 'CastError' ? 400 : err.code === 11000 ? 409 : 500);
  res.status(status).json({ error: err.code === 11000 ? 'Duplicate value' : err.message || 'Server error' });
});

const port = process.env.PORT || 4000;
const inMemory = await connectDb();
if (inMemory || process.env.SEED_ON_START === 'true') {
  if ((await Admin.countDocuments()) === 0) await seedDatabase();
}
app.listen(port, () => console.log(`[api] TradePilot admin API on http://localhost:${port}`));
