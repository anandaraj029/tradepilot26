import { useState } from 'react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useApi, useList } from '../lib/hooks.js';
import { C, axis, shortDate } from '../lib/chart.js';
import { compactInr, date, inr, pct, pnlClass, signedInr } from '../lib/format.js';
import { Card, ChartTip, DataTable, Modal, Pager, PageHeader, SearchInput, Select, StatusBadge, UserCell } from '../components/ui.jsx';

export default function Backtests() {
  const list = useList('backtests', { qualificationStatus: 'ALL', 'config.market': 'ALL' }, { limit: 15 });
  const totals = useApi('/stats/strategies');
  const [open, setOpen] = useState(null);

  const columns = [
    { title: 'Strategy', render: (b) => <><div className="bold">{b.config.strategyName}</div><div className="xs dim">{b.config.symbol} · {b.config.timeframeDays}d</div></> },
    { title: 'Trader', render: (b) => <UserCell user={b.user} /> },
    { title: 'Trades', right: true, render: (b) => <span className="mono">{b.totalTrades}</span> },
    { title: 'Win rate', right: true, render: (b) => <span className="mono">{b.winRatePercent}%</span> },
    { title: 'Return', right: true, render: (b) => <span className={`mono ${pnlClass(b.totalReturnPercent)}`}>{pct(b.totalReturnPercent)}</span> },
    { title: 'Profit factor', right: true, render: (b) => <span className={`mono ${b.profitFactor >= 1.5 ? 'pos' : b.profitFactor < 1 ? 'neg' : ''}`}>{b.profitFactor}</span> },
    { title: 'Max DD', right: true, render: (b) => <span className="mono neg">{b.maxDrawdownPercent}%</span> },
    { title: 'Sharpe', right: true, render: (b) => <span className="mono">{b.sharpeRatio}</span> },
    { title: 'Verdict', render: (b) => <StatusBadge value={b.qualificationStatus} /> },
    { title: 'Run', render: (b) => <span className="small muted">{date(b.createdAt)}</span> },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Backtests" sub="Simulator runs from the app's Backtest screen with AI Council qualification verdicts." />
      <div className="grid g-3">
        {['QUALIFIED', 'NEEDS_TUNING', 'REJECTED'].map((q) => {
          const r = totals.data?.backtests.find((b) => b._id === q);
          return (
            <div key={q} className="card stat">
              <div className="stat-label"><StatusBadge value={q} /></div>
              <div className="stat-value">{r?.count ?? 0}</div>
              <div className="stat-foot">Avg return {pct(r?.avgReturn)}</div>
            </div>
          );
        })}
      </div>
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Symbol or strategy…" />
          <Select value={list.filters.qualificationStatus} onChange={(v) => list.setFilter('qualificationStatus', v)} allLabel="All verdicts" options={['QUALIFIED', 'NEEDS_TUNING', 'REJECTED']} />
          <Select value={list.filters['config.market']} onChange={(v) => list.setFilter('config.market', v)} allLabel="All markets" options={['INDIA', 'USA']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} onRowClick={setOpen} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {open && (
        <Modal title={`${open.config.strategyName} · ${open.config.symbol}`} onClose={() => setOpen(null)} wide>
          <div className="grid g-4">
            {[['Net P&L', signedInr(open.netPnl), pnlClass(open.netPnl)], ['Final equity', inr(open.finalEquity)], ['Payoff ratio', open.payoffRatio], ['Avg hold', `${open.avgHoldingDays}d`]].map(([l, v, c]) => (
              <div key={l} style={{ background: 'var(--surface-2)', borderRadius: 10, padding: 12 }}><div className="xs dim">{l}</div><div className={`mono bold ${c || ''}`}>{v}</div></div>
            ))}
          </div>
          <Card title="Equity curve" pad>
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={open.equityCurve}>
                <CartesianGrid stroke={C.grid} vertical={false} />
                <XAxis dataKey="date" tickFormatter={shortDate} {...axis} minTickGap={30} />
                <YAxis tickFormatter={compactInr} {...axis} width={60} domain={['auto', 'auto']} />
                <Tooltip content={<ChartTip fmt={(v) => inr(v)} />} labelFormatter={shortDate} />
                <Area dataKey="equity" name="Equity" stroke={open.netPnl >= 0 ? C.green : C.red} fill={open.netPnl >= 0 ? 'var(--green-soft)' : 'var(--red-soft)'} strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </Card>
          <dl className="kv">
            <dt>Wins / losses</dt><dd>{open.winCount} / {open.lossCount}</dd>
            <dt>Starting capital</dt><dd className="mono">{inr(open.config.startingCapital)}</dd>
            <dt>Risk per trade</dt><dd>{open.config.maxRiskPerTradePercent}%</dd>
            <dt>SL / TP</dt><dd>{open.config.stopLossPercent}% / {open.config.takeProfitPercent}%</dd>
            <dt>Max holding</dt><dd>{open.config.maxHoldingDays} days</dd>
          </dl>
          <div><div className="small bold" style={{ marginBottom: 6 }}>AI Council critique <StatusBadge value={open.qualificationStatus} /></div><p className="pre">{open.aiCouncilCritique}</p></div>
        </Modal>
      )}
    </div>
  );
}
