import { useState } from 'react';
import { XCircle } from 'lucide-react';
import { api } from '../lib/api.js';
import { useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { dateTime, money, pct, pnlClass } from '../lib/format.js';
import { Confirm, DataTable, Pager, PageHeader, SearchInput, Select, StatusBadge, UserCell, useAction } from '../components/ui.jsx';

export default function Positions() {
  const { can } = useAuth();
  const run = useAction();
  const list = useList('positions', { market: 'ALL', executionType: 'ALL' }, { limit: 20, sort: '-openedAt' });
  const [closing, setClosing] = useState(null);

  const columns = [
    { title: 'Trader', render: (p) => <UserCell user={p.user} /> },
    { title: 'Symbol', render: (p) => <><div className="bold">{p.symbol}</div><div className="xs dim">{p.companyName}</div></> },
    { title: 'Market', render: (p) => <span className="small">{p.market}</span> },
    { title: 'Exec', render: (p) => <StatusBadge value={p.executionType} /> },
    { title: 'Qty', key: 'quantity', right: true },
    { title: 'Avg', right: true, render: (p) => <span className="mono">{money(p.buyPrice, p.market)}</span> },
    { title: 'LTP', right: true, render: (p) => <span className="mono">{money(p.currentPrice, p.market)}</span> },
    { title: 'Stop / Target', right: true, render: (p) => <span className="mono small"><span className="neg">{p.stopLoss}</span> / <span className="pos">{p.takeProfit}</span></span> },
    {
      title: 'Unrealized',
      right: true,
      render: (p) => {
        const v = (p.currentPrice - p.buyPrice) * p.quantity;
        const r = ((p.currentPrice - p.buyPrice) / p.buyPrice) * 100;
        return <><div className={`mono ${pnlClass(v)}`}>{money(v, p.market)}</div><div className={`xs mono ${pnlClass(r)}`}>{pct(r, 2)}</div></>;
      },
    },
    { title: 'Opened', render: (p) => <span className="small muted">{dateTime(p.openedAt)}</span> },
    { title: '', render: (p) => can('RISK_OFFICER') && <button className="btn sm" onClick={() => setClosing(p)}><XCircle size={14} /> Force close</button> },
  ];

  return (
    <>
      <PageHeader title="Open positions" sub="Live and paper positions held across all traders. Force-close books the P&L and logs an admin intervention." />
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Search symbol or company…" />
          <Select value={list.filters.market} onChange={(v) => list.setFilter('market', v)} allLabel="All markets" options={[['INDIA', 'India (NSE/BSE)'], ['USA', 'USA (NYSE/NASDAQ)']]} />
          <Select value={list.filters.executionType} onChange={(v) => list.setFilter('executionType', v)} allLabel="Paper & live" options={['PAPER', 'LIVE']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} empty="No open positions" />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {closing && (
        <Confirm title={`Force close ${closing.symbol}`} danger withReason confirmLabel="Close position"
          message={`Closes ${closing.quantity} × ${closing.symbol} for ${closing.user?.name} at the last traded price (${money(closing.currentPrice, closing.market)}).${closing.executionType === 'LIVE' ? ' A market exit order is routed to the broker.' : ''}`}
          onConfirm={(reason) => run(() => api(`/positions/${closing._id}/force-close`, { method: 'POST', body: { reason } }), 'Position closed').then(list.reload)}
          onClose={() => setClosing(null)} />
      )}
    </>
  );
}
