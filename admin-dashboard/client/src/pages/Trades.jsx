import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Download } from 'lucide-react';
import { download, qs } from '../lib/api.js';
import { useList } from '../lib/hooks.js';
import { dateTime, label, money, pct, pnlClass, signedInr } from '../lib/format.js';
import { Badge, DataTable, Modal, Pager, PageHeader, SearchInput, Select, StatusBadge, UserCell, useAction } from '../components/ui.jsx';
import { tradeColumns } from './TraderDetail.jsx';

const STRATS = ['Momentum Breakout Pilot', 'Mean Reversion RSI Guard', 'Multi-AI Council Hybrid', 'Sector Rotation Swing', 'AutoPilot Daily Target', 'Admin Force Close'];

export default function Trades() {
  const [params] = useSearchParams();
  const run = useAction();
  const list = useList('trades', {
    status: 'ALL', executionType: 'ALL', market: 'ALL', outcome: 'ALL', strategy: 'ALL', source: 'ALL', userOverride: 'ALL', user: params.get('user') || undefined,
  }, { limit: 20 });
  const [open, setOpen] = useState(null);
  const f = list.filters;

  return (
    <>
      <PageHeader title="Trades & Journal" sub="Every paper and live execution with AI decision, confidence, user overrides and post-mortem notes.">
        <button className="btn" onClick={() => run(() => download(`/export/trades.csv${qs({ status: f.status, executionType: f.executionType, market: f.market, outcome: f.outcome, strategy: f.strategy })}`, 'tradepilot-trades.csv'), 'CSV exported')}>
          <Download size={16} /> Export CSV
        </button>
      </PageHeader>
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Symbol, trade ID, strategy…" />
          <Select value={f.status} onChange={(v) => list.setFilter('status', v)} allLabel="All statuses" options={['OPEN', 'CLOSED', 'CANCELLED']} />
          <Select value={f.executionType} onChange={(v) => list.setFilter('executionType', v)} allLabel="Paper & live" options={['PAPER', 'LIVE']} />
          <Select value={f.outcome} onChange={(v) => list.setFilter('outcome', v)} allLabel="All outcomes" options={['PROFIT', 'LOSS', 'BREAKEVEN']} />
          <Select value={f.market} onChange={(v) => list.setFilter('market', v)} allLabel="All markets" options={['INDIA', 'USA']} />
          <Select value={f.strategy} onChange={(v) => list.setFilter('strategy', v)} allLabel="All strategies" options={STRATS.map((s) => [s, s])} />
          <Select value={f.source} onChange={(v) => list.setFilter('source', v)} allLabel="All sources" options={['MANUAL', 'ASSISTED', 'AUTOPILOT']} />
          <Select value={f.userOverride} onChange={(v) => list.setFilter('userOverride', v)} allLabel="Overrides: any" options={[['true', 'Overrode AI'], ['false', 'Followed AI']]} />
          {f.user && <button className="btn sm" onClick={() => list.setFilter('user', undefined)}>Clear trader filter ×</button>}
        </div>
        <DataTable
          columns={[{ title: 'Trader', render: (t) => <UserCell user={t.user} /> }, ...tradeColumns]}
          rows={list.data?.items}
          loading={list.loading}
          onRowClick={setOpen}
        />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {open && <TradeModal t={open} onClose={() => setOpen(null)} />}
    </>
  );
}

function TradeModal({ t, onClose }) {
  return (
    <Modal title={`${t.symbol} · ${t.tradeId}`} onClose={onClose} wide>
      <div className="row wrap" style={{ gap: 8 }}>
        <StatusBadge value={t.status} /><StatusBadge value={t.executionType} /><StatusBadge value={t.outcome} /><StatusBadge value={t.source} />
        {t.userOverride && <Badge tone="amber">User override</Badge>}
        {t.tags && <Badge>{t.tags}</Badge>}
      </div>
      <div className="grid g-2">
        <dl className="kv card card-pad" style={{ boxShadow: 'none' }}>
          <dt>Trader</dt><dd><UserCell user={t.user} /></dd>
          <dt>Company</dt><dd>{t.companyName}</dd>
          <dt>Strategy</dt><dd>{t.strategy}</dd>
          <dt>Side × Qty</dt><dd>{t.orderType} × {t.quantity}</dd>
          <dt>Entry</dt><dd className="mono">{money(t.entryPrice, t.market)}</dd>
          <dt>Exit</dt><dd className="mono">{money(t.exitPrice, t.market)}</dd>
          <dt>Stop / Target</dt><dd className="mono">{t.stopLoss} / {t.takeProfit}</dd>
          <dt>P&L</dt><dd className={`mono ${pnlClass(t.pnl)}`}>{signedInr(t.pnl)} ({pct(t.pnlPercent, 2)})</dd>
          <dt>Charges</dt><dd className="mono">{money(t.charges, 'INDIA')}</dd>
          <dt>Opened</dt><dd>{dateTime(t.entryTime)}</dd>
          <dt>Closed</dt><dd>{dateTime(t.closedAt)}</dd>
          {t.brokerOrderId && <><dt>Broker order</dt><dd className="mono">{t.brokerOrderId}</dd></>}
        </dl>
        <div className="stack" style={{ gap: 12 }}>
          <div className="card card-pad" style={{ boxShadow: 'none' }}>
            <div className="row between"><span className="bold">AI Council decision</span><StatusBadge value={t.aiDecision} /></div>
            <div className="row between small" style={{ marginTop: 8 }}><span className="muted">Confidence</span><span className="mono">{t.aiConfidence}%</span></div>
            <div className="row between small"><span className="muted">Risk score</span><span className="mono">{t.riskScore}/100</span></div>
          </div>
          <div><div className="small bold" style={{ marginBottom: 6 }}>AI observation</div><p className="pre">{t.aiObservation || '—'}</p></div>
          {t.userOverride && <div><div className="small bold" style={{ marginBottom: 6 }}>Override reason</div><p className="pre">{t.overrideReason}</p></div>}
          <div><div className="small bold" style={{ marginBottom: 6 }}>Journal notes</div><p className="pre">{t.userNotes || 'No notes'}</p></div>
          <div className="xs dim">Market: {label(t.market)}</div>
        </div>
      </div>
    </Modal>
  );
}
