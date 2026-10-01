import { useState } from 'react';
import { AlertOctagon, Lock, Power, RotateCcw, ShieldAlert, ShieldCheck, SlidersHorizontal, Target, Unlock } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { inr, signedInr, pnlClass } from '../lib/format.js';
import {
  Badge, Card, Confirm, DataTable, Pager, PageHeader, Progress, Select, Stat, UserCell, useAction,
} from '../components/ui.jsx';
import RiskLimitsModal from '../components/RiskLimitsModal.jsx';

/** Mirrors RiskEngine.kt status derivation for an at-a-glance risk column. */
export function riskStatus(p) {
  if (p.killSwitchTriggered || p.isLocked) return ['LOCKED', 'red'];
  if (p.realizedPnlToday <= -p.maxDailyLoss) return ['LOSS LIMIT', 'red'];
  if (p.profitLockEnabled && p.realizedPnlToday >= p.dailyTarget) return ['PROFIT LOCKED', 'green'];
  if (p.realizedPnlToday < -p.maxDailyLoss / 2) return ['CAUTION', 'amber'];
  return ['SAFE', 'green'];
}

export default function RiskCenter() {
  const { can } = useAuth();
  const run = useAction();
  const overview = useApi('/stats/overview?days=7');
  const list = useList('portfolios', { isLocked: 'ALL', killSwitchTriggered: 'ALL', tradingMode: 'ALL' }, { limit: 20 });
  const [sort, setSort] = useState('realizedPnlToday');
  const [dialog, setDialog] = useState(null);
  const reload = () => { list.reload(); overview.reload(); };
  const k = overview.data?.kpis || {};
  const globalKill = overview.data?.platform?.globalKillSwitch;

  const rows = [...(list.data?.items || [])].sort((a, b) => (sort === 'exposure'
    ? b.investedValue / (b.investedValue + b.availableCash) - a.investedValue / (a.investedValue + a.availableCash)
    : a[sort] - b[sort]));

  const columns = [
    { title: 'Trader', render: (p) => <UserCell user={p.user} /> },
    { title: 'Status', render: (p) => { const [s, t] = riskStatus(p); return <Badge tone={t} dot>{s}</Badge>; } },
    {
      title: 'Today vs limits',
      render: (p) => {
        const v = p.realizedPnlToday;
        const pctT = v >= 0 ? (v / p.dailyTarget) * 100 : (-v / p.maxDailyLoss) * 100;
        return (
          <div style={{ width: 180 }}>
            <div className="row between xs"><span className={`mono ${pnlClass(v)}`}>{signedInr(v)}</span><span className="dim">{v >= 0 ? `target ${inr(p.dailyTarget)}` : `limit -${inr(p.maxDailyLoss)}`}</span></div>
            <Progress value={pctT} color={v >= 0 ? 'var(--green)' : 'var(--red)'} />
          </div>
        );
      },
    },
    {
      title: 'Exposure',
      right: true,
      render: (p) => {
        const e = (p.investedValue / Math.max(1, p.investedValue + p.availableCash)) * 100;
        return <span className={`mono ${e > p.maxExposurePercent ? 'neg' : e > p.maxExposurePercent * 0.8 ? 'warn' : ''}`}>{e.toFixed(0)}% / {p.maxExposurePercent}%</span>;
      },
    },
    { title: 'Risk/trade', right: true, render: (p) => <span className="mono">{p.maxRiskPerTradePercent}%</span> },
    { title: 'Max pos.', right: true, render: (p) => <span className="mono">{p.maxOpenPositions}</span> },
    { title: 'Capital', right: true, render: (p) => <span className="mono">{inr(p.startingCapital)}</span> },
    {
      title: '',
      render: (p) => can('RISK_OFFICER') && (
        <div className="row" style={{ gap: 4 }}>
          <button className="btn sm icon ghost" title="Edit limits" onClick={() => setDialog({ type: 'limits', p })}><SlidersHorizontal size={15} /></button>
          <button className="btn sm icon ghost" title={p.isLocked ? 'Unlock' : 'Lock'} onClick={() => setDialog({ type: 'lock', p })}>{p.isLocked ? <Unlock size={15} /> : <Lock size={15} />}</button>
          <button className="btn sm icon ghost" title="Kill switch" onClick={() => setDialog({ type: 'kill', p })}><Power size={15} color={p.killSwitchTriggered ? 'var(--red)' : undefined} /></button>
        </div>
      ),
    },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Risk Center" sub="Deterministic capital protection: daily target & loss locks, position sizing, exposure caps and kill switches.">
        {can() && (
          <>
            <button className="btn" onClick={() => setDialog({ type: 'reset' })}><RotateCcw size={16} /> Reset daily P&L</button>
            <button className={`btn ${globalKill ? 'primary' : 'danger'}`} onClick={() => setDialog({ type: 'global' })}>
              <AlertOctagon size={16} /> {globalKill ? 'Release global kill switch' : 'Global kill switch'}
            </button>
          </>
        )}
      </PageHeader>

      {globalKill && <div className="banner red"><AlertOctagon size={18} /> Global kill switch engaged — every portfolio is halted and AutoPilot is disabled platform-wide.</div>}

      <div className="grid g-4">
        <Stat label="Profit-locked today" icon={Target} tone="green" value={k.targetsHit ?? '–'} foot="Reached daily target, trading paused" />
        <Stat label="Loss limit breached" icon={ShieldAlert} tone="red" value={k.lossLimitsHit ?? '–'} foot="Hit max daily loss" />
        <Stat label="Manually locked" icon={Lock} tone="amber" value={k.lockedCount ?? '–'} foot="Locked by admin or risk engine" />
        <Stat label="Kill switches" icon={Power} tone="red" value={k.killSwitchCount ?? '–'} foot="Emergency halts active" />
      </div>

      <Card title="Risk engine rule chain" sub="Evaluated in order for every AI proposal — the AI cannot override these">
        <div className="grid g-4" style={{ gap: 10 }}>
          {[
            ['1', 'Kill switch', 'Emergency halt check'], ['2', 'Daily loss limit', 'Locks at −max daily loss'],
            ['3', 'Profit target lock', 'Locks once target is banked'], ['4', 'Manual lock', 'Admin / user lock'],
            ['5', 'Council verdict', 'NO TRADE / WAIT / HOLD rejected'], ['6', 'Max open positions', 'Concurrent position cap'],
            ['7', 'Risk per trade', 'Size by % of capital & 2.5% stop'], ['8', 'Exposure cap', 'Reject above max exposure'],
          ].map(([n, t, d]) => (
            <div key={n} className="row" style={{ alignItems: 'flex-start', gap: 10, padding: 10, borderRadius: 10, background: 'var(--surface-2)' }}>
              <span className="badge teal mono">{n}</span>
              <div><div className="small bold">{t}</div><div className="xs dim">{d}</div></div>
            </div>
          ))}
        </div>
      </Card>

      <div className="card">
        <div className="toolbar">
          <ShieldCheck size={18} color="var(--teal)" />
          <span className="bold grow">Portfolio risk monitor</span>
          <Select value={list.filters.isLocked} onChange={(v) => list.setFilter('isLocked', v)} allLabel="Lock: any" options={[['true', 'Locked'], ['false', 'Unlocked']]} />
          <Select value={list.filters.killSwitchTriggered} onChange={(v) => list.setFilter('killSwitchTriggered', v)} allLabel="Kill switch: any" options={[['true', 'Engaged'], ['false', 'Not engaged']]} />
          <Select value={sort} onChange={setSort} options={[['realizedPnlToday', 'Sort: worst P&L first'], ['exposure', 'Sort: highest exposure']]} />
        </div>
        <DataTable columns={columns} rows={rows} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>

      {dialog?.type === 'limits' && <RiskLimitsModal portfolio={dialog.p} onClose={() => setDialog(null)} onSaved={reload} />}
      {dialog?.type === 'lock' && (
        <Confirm title={dialog.p.isLocked ? 'Unlock trading' : 'Lock trading'} withReason={!dialog.p.isLocked} confirmLabel={dialog.p.isLocked ? 'Unlock' : 'Lock'}
          message={`${dialog.p.user?.name}: ${dialog.p.isLocked ? 'allow new orders again' : 'the risk engine will reject all new orders'}.`}
          onConfirm={(reason) => run(() => api(`/portfolios/${dialog.p._id}/lock`, { method: 'POST', body: { lock: !dialog.p.isLocked, reason } }), 'Updated').then(reload)}
          onClose={() => setDialog(null)} />
      )}
      {dialog?.type === 'kill' && (
        <Confirm title={dialog.p.killSwitchTriggered ? 'Release kill switch' : 'Engage kill switch'} danger={!dialog.p.killSwitchTriggered} withReason
          confirmLabel={dialog.p.killSwitchTriggered ? 'Release' : 'Engage'} message={`${dialog.p.user?.name}: ${dialog.p.killSwitchTriggered ? 'resume trading' : 'halt all trading immediately'}.`}
          onConfirm={(reason) => run(() => api(`/portfolios/${dialog.p._id}/kill-switch`, { method: 'POST', body: { engage: !dialog.p.killSwitchTriggered, reason } }), 'Kill switch updated').then(reload)}
          onClose={() => setDialog(null)} />
      )}
      {dialog?.type === 'global' && (
        <Confirm title={globalKill ? 'Release global kill switch' : 'Engage GLOBAL kill switch'} danger={!globalKill} withReason
          confirmLabel={globalKill ? 'Release for all traders' : 'Halt all trading'}
          message={globalKill ? 'Releases the kill switch on every portfolio. AutoPilot stays off until traders re-enable it.' : 'Immediately halts trading for EVERY trader on the platform and disables AutoPilot. Use only in emergencies.'}
          onConfirm={(reason) => run(() => api('/risk/global-kill-switch', { method: 'POST', body: { engage: !globalKill, reason } }), 'Global kill switch updated').then(reload)}
          onClose={() => setDialog(null)} />
      )}
      {dialog?.type === 'reset' && (
        <Confirm title="Reset daily P&L" danger confirmLabel="Reset"
          message="Sets today's realized P&L to zero on every portfolio (normally done by the end-of-day job). Profit and loss locks will clear."
          onConfirm={() => run(() => api('/risk/reset-daily', { method: 'POST' }), 'Daily P&L reset').then(reload)}
          onClose={() => setDialog(null)} />
      )}
    </div>
  );
}
