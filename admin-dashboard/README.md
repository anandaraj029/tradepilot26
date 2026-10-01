# AI TradePilot — Admin Dashboard

Web admin console for the AI TradePilot Android app. **React (Vite) + Express + MongoDB (Mongoose).**

## Features (mapped to the mobile app)

| Admin page | App feature it oversees |
|---|---|
| **Overview** | Platform KPIs: traders, capital under management, today's realized P&L, win rate, paper vs live P&L curve, trading-mode mix, AI verdicts, strategy performance, risk alerts |
| **Traders** / trader detail | Accounts (`UserProfile`): status, KYC, 2FA/biometric/PIN, tier, device; per-trader portfolio, positions, trades, funds, brokers, strategies, activity; suspend/reactivate, change trading mode |
| **Risk Center** | `RiskEngine` limits per portfolio (daily target, max daily loss, risk/trade, max positions, exposure, profit lock); lock/unlock, per-trader & **global kill switch**, daily P&L reset |
| **AutoPilot** | `AutoPilotEngine` state per trader (hunting / target achieved / loss limit / max positions); enable/disable |
| **Open Positions** | Paper & live positions; admin **force close** (books P&L, logs intervention) |
| **Trades & Journal** | `TradeEntity` journal with AI decision/confidence, overrides, tags, notes; filters + **CSV export** |
| **Funds** | Deposits/withdrawals (UPI/NetBanking/IMPS); approve/reject pending |
| **AI Council** | Gemini / Claude / ChatGPT agent analytics, vote mix, score radar, consensus + risk-engine outcome log with full breakdown |
| **AI Copilot** | Gemini chat sessions, tokens, ratings, moderation flags |
| **Markets & Scanner** | NSE/BSE/NYSE/NASDAQ asset catalog with technicals and AI scanner score; add/edit/halt |
| **Strategy Lab** | Platform strategies (CRUD) + review of user custom strategies (approve for live / reject) |
| **Backtests** | Simulator runs, equity curves, AI Council qualification verdicts |
| **Broker Connections** | Zerodha, Angel One, Upstox, Dhan, Groww, IBKR: connections, margin, latency, token expiry, revoke; broker catalog toggles |
| **Daily Report** | Platform-wide Daily Summary: P&L by strategy/market, top traders, council-adherence audit |
| **Audit Log** | Immutable trail of AI, risk engine, user and admin actions |
| **Settings** | Live trading / AutoPilot / maintenance switches, allowed modes, default risk profile, AI agent weights & models, Copilot, security policy, admin team |

Every admin write is recorded in the audit log. Roles: `SUPER_ADMIN`, `RISK_OFFICER`, `ANALYST`, `SUPPORT`.

## Run locally

```bash
npm run install:all
cp server/.env.example server/.env   # set MONGODB_URI, JWT_SECRET
npm run dev:server                   # API on :4000
npm run dev:client                   # UI on :5173 (proxies /api)
```

- If `MONGODB_URI` is empty, the server starts an **in-memory MongoDB** and seeds demo data (resets on restart).
- With a real database, set `SEED_ON_START=true` to seed once when no admins exist, or run `npm run seed` to **wipe and reseed** the configured database.
- Production: `npm run build && npm start` — the API serves `client/dist`.

Demo logins (password `Admin@123`): `admin@`, `risk@`, `analyst@`, `support@tradepilot.ai`.
