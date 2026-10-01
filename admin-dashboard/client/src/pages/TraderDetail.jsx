import { Fragment, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Ban, CheckCircle2, Lock, Power, Rocket, SlidersHorizontal, Unlock } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { ago, date, dateTime, inr, initials, label, money, pct, pnlClass, signedInr } from '../lib/format.js';
import {
  Badge, Card, Confirm, DataTable, ErrorBox, PageHeader, Progress, Select, Spinner, Stat, StatusBadge, Tabs, Toggle, useAction,
} from '../components/ui.jsx';
import RiskLimitsModal from '../components/RiskLimitsModal.jsx';
import { MODES } from './Traders.jsx';

export default function TraderDetail() {
  const { id } = useParams();
  const { data, loading, error, reload } = useApi(`/users/${id}/overview`);
  const { can } = useAuth();
  const run = useAction();
  const [tab, setTab] = useState('portfolio');
  const [dialog, setDialog] = useState(null);

  if (error) return <ErrorBox error={error} />;
  if (loading && !data) return <Spinner />;
  const { user: u, portfolio: p, positions, trades, funds, brokers, audit, tradeStats, strategies } = data;
  const post = (path, body, msg) => run(() => api(path, { method: 'POST', body }), msg).then(reload);
  const isRisk = can('RISK_OFFICER');

  const progress = p ? (p.realizedPnlToday / p.dailyTarget) * 100 : 0;

  return (
    <div className="stack" style={{ gap: 20 }}>
      <Link to="/traders" className="small row muted" style={{ gap: 6 }}><ArrowLeft size={15} /> All traders</Link>
      <div className="card card-pad row wrap" style={{ gap: 18 }}>
        <span className="avatar lg">{initials(u.name)}</span>
        <div className="grow">
          <div className="row wrap" style={{ gap: 8 }}>
            <h1 className="page-title" style={{ fontSize: 20 }}>{u.name}</h1>
            <StatusBadge value={u.status} dot />
            <StatusBadge value={u.tradingMode} />
            {p?.killSwitchTriggered && <Badge tone="red">Kill switch</Badge>}
            {p?.isLocked && <Badge tone="red">Locked</Badge>}
          </div>
          <div className="small muted">{u.email} · {u.phone} · {u.role} · {u.tier}</div>
          <div className="xs dim">Joined {date(u.createdAt)} · last login {ago(u.lastLoginAt)} · {u.device?.model} ({u.device?.os}, app v{u.device?.appVersion})</div>
        </div>
        <div className="row wrap">
          {can('SUPPORT', 'RISK_OFFICER') && (u.status === 'SUSPENDED' ? (
            <button className="btn" onClick={() => post(`/users/${id}/status`, { status: 'ACTIVE' }, 'Trader reactivated')}><CheckCircle2 size={16} /> Reactivate</button>
          ) : (
            <button className="btn" onClick={() => setDialog('suspend')}><Ban size={16} /> Suspend</button>
          ))}
          {isRisk && p && (
            <>
              <button className="btn" onClick={() => setDialog('limits')}><SlidersHorizontal size={16} /> Risk limits</button>
              <button className="btn" onClick={() => (p.isLocked ? post(`/portfolios/${p._id}/lock`, { lock: false }, 'Trading unlocked') : setDialog('lock'))}>
                {p.isLocked ? <><Unlock size={16} /> Unlock</> : <><Lock size={16} /> Lock trading</>}
              </button>
              <button className={`btn ${p.killSwitchTriggered ? '' : 'danger'}`} onClick={() => setDialog('kill')}>
                <Power size={16} /> {p.killSwitchTriggered ? 'Release kill switch' : 'Kill switch'}
              </button>
            </>
          )}
        </div>
      </div>

      {p && (
        <div className="grid g-4">
          <Stat label="Portfolio value" value={inr(p.availableCash + p.investedValue)} foot={`${inr(p.availableCash)} cash · ${inr(p.investedValue)} invested`} />
          <Stat label="Today's realized P&L" valueClass={pnlClass(p.realizedPnlToday)} value={signedInr(p.realizedPnlToday)} foot={<><Progress value={p.realizedPnlToday >= 0 ? progress : (-p.realizedPnlToday / p.maxDailyLoss) * 100} color={p.realizedPnlToday < 0 ? 'var(--red)' : progress >= 100 ? 'var(--green)' : undefined} /><span>{p.realizedPnlToday >= 0 ? `${Math.round(progress)}% of ${inr(p.dailyTarget)} target` : `${Math.round((-p.realizedPnlToday / p.maxDailyLoss) * 100)}% of ${inr(p.maxDailyLoss)} loss limit used`}</span></>} />
          <Stat label="Lifetime P&L" valueClass={pnlClass(tradeStats.pnl)} value={signedInr(tradeStats.pnl)} foot={`${tradeStats.count} trades · ${tradeStats.count ? Math.round((tradeStats.wins / tradeStats.count) * 100) : 0}% win rate`} />
          <Stat label="Unrealized P&L" valueClass={pnlClass(p.unrealizedPnl)} value={signedInr(p.unrealizedPnl)} foot={`${positions.length}/${p.maxOpenPositions} positions open`} />
        </div>
      )}

      <div>
        <Tabs value={tab} onChange={setTab} tabs={[
          ['portfolio', 'Risk & settings'], ['positions', `Positions (${positions.length})`], ['trades', 'Trades'],
          ['funds', 'Funds'], ['brokers', 'Brokers'], ['strategies', 'Strategies'], ['audit', 'Activity'],
        ]} />

        {tab === 'portfolio' && p && (
          <div className="grid g-3">
            <Card title="Risk engine limits" sub="Deterministic guardrails AI cannot override">
              <dl className="kv">
                <dt>Starting capital</dt><dd className="mono">{inr(p.startingCapital)}</dd>
                <dt>Daily target</dt><dd className="mono pos">{inr(p.dailyTarget)}</dd>
                <dt>Max daily loss</dt><dd className="mono neg">{inr(p.maxDailyLoss)}</dd>
                <dt>Risk per trade</dt><dd className="mono">{p.maxRiskPerTradePercent}%</dd>
                <dt>Max positions</dt><dd className="mono">{p.maxOpenPositions}</dd>
                <dt>Max exposure</dt><dd className="mono">{p.maxExposurePercent}%</dd>
                <dt>Profit lock</dt><dd>{p.profitLockEnabled ? 'Enabled' : 'Disabled'}</dd>
                <dt>USD/INR</dt><dd className="mono">{p.usdInrRate}</dd>
              </dl>
              {p.lockReason && <div className="banner amber small" style={{ marginTop: 14 }}>{p.lockReason}</div>}
            </Card>
            <Card title="Trading mode & AutoPilot">
              <div className="stack">
                <div className="field">
                  <label>Execution mode</label>
                  <Select value={u.tradingMode} onChange={(v) => post(`/users/${id}/trading-mode`, { tradingMode: v }, `Mode set to ${label(v)}`)} options={MODES} />
                </div>
                <div className="row between">
                  <div><div className="bold row" style={{ gap: 6 }}><Rocket size={15} /> AutoPilot</div><div className="xs dim">{u.autoPilot?.lastMessage || 'Not running'}</div></div>
                  <Toggle checked={u.autoPilot?.enabled} disabled={!isRisk} onChange={(v) => post(`/users/${id}/autopilot`, { enabled: v }, v ? 'AutoPilot enabled' : 'AutoPilot disabled')} />
                </div>
                <div className="row between small"><span className="muted">State</span><StatusBadge value={u.autoPilot?.state} /></div>
                <div className="row between small"><span className="muted">Autonomous live</span><span>{u.autoPilot?.autonomousLive ? 'Yes' : 'No'}</span></div>
                <div className="row between small"><span className="muted">Broker integration</span><span>{u.brokerIntegration}</span></div>
              </div>
            </Card>
            <Card title="Security & notifications">
              <dl className="kv">
                <dt>2FA</dt><dd>{u.is2faEnabled ? <Badge tone="green">On</Badge> : <Badge tone="red">Off</Badge>}</dd>
                <dt>Biometric</dt><dd>{u.isBiometricEnabled ? <Badge tone="green">On</Badge> : <Badge>Off</Badge>}</dd>
                <dt>PIN lock</dt><dd>{u.pinLockEnabled ? <Badge tone="green">On</Badge> : <Badge>Off</Badge>}</dd>
                <dt>KYC</dt><dd><StatusBadge value={u.kycStatus} /></dd>
                {Object.entries(u.notifications || {}).map(([k, v]) => (
                  <Fragment key={k}><dt>{label(k.replace(/([A-Z])/g, '_$1'))}</dt><dd>{v ? 'On' : 'Off'}</dd></Fragment>
                ))}
              </dl>
            </Card>
          </div>
        )}

        {tab === 'positions' && (
          <div className="card">
            <DataTable rows={positions} empty="No open positions" columns={[
              { title: 'Symbol', render: (x) => <><div className="bold">{x.symbol}</div><div className="xs dim">{x.companyName}</div></> },
              { title: 'Exec', render: (x) => <StatusBadge value={x.executionType} /> },
              { title: 'Qty', key: 'quantity', right: true },
              { title: 'Avg price', right: true, render: (x) => <span className="mono">{money(x.buyPrice, x.market)}</span> },
              { title: 'LTP', right: true, render: (x) => <span className="mono">{money(x.currentPrice, x.market)}</span> },
              { title: 'SL / TP', right: true, render: (x) => <span className="mono small">{x.stopLoss} / {x.takeProfit}</span> },
              { title: 'P&L', right: true, render: (x) => { const v = ((x.currentPrice - x.buyPrice) / x.buyPrice) * 100; return <span className={`mono ${pnlClass(v)}`}>{pct(v, 2)}</span>; } },
              { title: 'Opened', render: (x) => <span className="small muted">{dateTime(x.openedAt)}</span> },
            ]} />
          </div>
        )}

        {tab === 'trades' && (
          <div className="card">
            <DataTable rows={trades} columns={tradeColumns} empty="No trades yet" />
            <div className="pager"><span>Latest 25 trades</span><Link to={`/trades?user=${id}`}>View all →</Link></div>
          </div>
        )}

        {tab === 'funds' && (
          <div className="card">
            <DataTable rows={funds} empty="No fund transactions" columns={[
              { title: 'Tx ID', render: (f) => <span className="mono small">{f.txId}</span> },
              { title: 'Type', render: (f) => <StatusBadge value={f.type} /> },
              { title: 'Amount', right: true, render: (f) => <span className="mono">{inr(f.amount)}</span> },
              { title: 'Method', key: 'paymentMethod' },
              { title: 'Status', render: (f) => <StatusBadge value={f.status} dot /> },
              { title: 'Date', render: (f) => <span className="small muted">{dateTime(f.createdAt)}</span> },
            ]} />
          </div>
        )}

        {tab === 'brokers' && (
          <div className="card">
            <DataTable rows={brokers} empty="Using simulated broker (paper trading)" columns={[
              { title: 'Broker', render: (b) => <><div className="bold">{b.brokerName}</div><div className="xs dim">{b.brokerUserId}</div></> },
              { title: 'Status', render: (b) => <StatusBadge value={b.status} dot /> },
              { title: 'Live routing', render: (b) => (b.isLiveRoutingActive ? <Badge tone="green">Active</Badge> : <Badge>Off</Badge>) },
              { title: 'Margin', right: true, render: (b) => <span className="mono">{inr(b.liveMargin)}</span> },
              { title: 'Latency', right: true, render: (b) => <span className="mono">{b.latencyMs} ms</span> },
              { title: 'Token expiry', render: (b) => <span className="small">{ago(b.tokenExpiresAt)}</span> },
            ]} />
          </div>
        )}

        {tab === 'strategies' && (
          <div className="card">
            <DataTable rows={strategies} empty="No custom strategies" columns={[
              { title: 'Name', render: (s) => <span className="bold">{s.name}</span> },
              { title: 'Prompt', render: (s) => <span className="small muted ellipsis" style={{ maxWidth: 360, display: 'inline-block' }}>{s.prompt}</span> },
              { title: 'Capital', right: true, render: (s) => <span className="mono">{inr(s.capitalAllocation)}</span> },
              { title: 'Status', render: (s) => <StatusBadge value={s.status} /> },
            ]} />
          </div>
        )}

        {tab === 'audit' && (
          <div className="card">
            <DataTable rows={audit} columns={[
              { title: 'Time', render: (a) => <span className="small muted">{dateTime(a.createdAt)}</span> },
              { title: 'Action', render: (a) => <span className="small bold">{label(a.action)}</span> },
              { title: 'Actor', render: (a) => <StatusBadge value={a.actorType} /> },
              { title: 'Severity', render: (a) => <StatusBadge value={a.severity} /> },
              { title: 'Details', render: (a) => <span className="small muted">{a.details}</span> },
            ]} />
          </div>
        )}
      </div>

      {dialog === 'limits' && <RiskLimitsModal portfolio={p} onClose={() => setDialog(null)} onSaved={reload} />}
      {dialog === 'suspend' && (
        <Confirm title="Suspend trader" danger withReason confirmLabel="Suspend"
          message="Suspending locks the portfolio, disables AutoPilot and blocks app trading until reactivated."
          onConfirm={(reason) => post(`/users/${id}/status`, { status: 'SUSPENDED', reason }, 'Trader suspended')}
          onClose={() => setDialog(null)} />
      )}
      {dialog === 'lock' && (
        <Confirm title="Lock trading" withReason confirmLabel="Lock"
          message="The risk engine will reject every new order for this trader until unlocked."
          onConfirm={(reason) => post(`/portfolios/${p._id}/lock`, { lock: true, reason }, 'Trading locked')}
          onClose={() => setDialog(null)} />
      )}
      {dialog === 'kill' && (
        <Confirm title={p.killSwitchTriggered ? 'Release kill switch' : 'Engage kill switch'} danger={!p.killSwitchTriggered} withReason
          confirmLabel={p.killSwitchTriggered ? 'Release' : 'Engage'}
          message={p.killSwitchTriggered ? 'Trading will resume subject to the normal risk engine checks.' : 'Emergency halt: all trading for this trader stops immediately and AutoPilot is disabled.'}
          onConfirm={(reason) => post(`/portfolios/${p._id}/kill-switch`, { engage: !p.killSwitchTriggered, reason }, p.killSwitchTriggered ? 'Kill switch released' : 'Kill switch engaged')}
          onClose={() => setDialog(null)} />
      )}
    </div>
  );
}

export const tradeColumns = [
  { title: 'Trade', render: (t) => <><div className="bold">{t.symbol}</div><div className="xs dim mono">{t.tradeId}</div></> },
  { title: 'Strategy', render: (t) => <span className="small">{t.strategy}</span> },
  { title: 'Side', render: (t) => <StatusBadge value={t.orderType} /> },
  { title: 'Exec', render: (t) => <StatusBadge value={t.executionType} /> },
  { title: 'Qty', key: 'quantity', right: true },
  { title: 'Entry → Exit', right: true, render: (t) => <span className="mono small">{t.entryPrice} → {t.exitPrice || '—'}</span> },
  { title: 'P&L', right: true, render: (t) => <><div className={`mono ${pnlClass(t.pnl)}`}>{signedInr(t.pnl)}</div><div className={`xs mono ${pnlClass(t.pnlPercent)}`}>{pct(t.pnlPercent, 2)}</div></> },
  { title: 'AI', render: (t) => <span className="small"><StatusBadge value={t.aiDecision} /> <span className="mono dim">{t.aiConfidence}%</span></span> },
  { title: 'Override', render: (t) => (t.userOverride ? <Badge tone="amber">Override</Badge> : <span className="dim">—</span>) },
  { title: 'Closed', render: (t) => <span className="small muted">{dateTime(t.closedAt)}</span> },
];
