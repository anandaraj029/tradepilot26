import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowDownLeft, ArrowUpRight, Check, Clock, X } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { compactInr, dateTime, inr } from '../lib/format.js';
import { Confirm, DataTable, Pager, PageHeader, SearchInput, Select, Stat, StatusBadge, UserCell, useAction } from '../components/ui.jsx';

export default function Funds() {
  const [params] = useSearchParams();
  const { can } = useAuth();
  const run = useAction();
  const list = useList('funds', { type: 'ALL', status: params.get('status') || 'ALL' }, { limit: 20 });
  const stats = useApi('/stats/overview?days=30');
  const [review, setReview] = useState(null);
  const k = stats.data?.kpis || {};

  const columns = [
    { title: 'Tx ID', render: (f) => <><div className="mono small bold">{f.txId}</div><div className="xs dim mono">UTR {f.utrNumber}</div></> },
    { title: 'Trader', render: (f) => <UserCell user={f.user} /> },
    { title: 'Type', render: (f) => <StatusBadge value={f.type} /> },
    { title: 'Amount', right: true, render: (f) => <span className={`mono bold ${f.type === 'DEPOSIT' ? 'pos' : ''}`}>{f.type === 'DEPOSIT' ? '+' : '−'}{inr(f.amount)}</span> },
    { title: 'Method', render: (f) => <span className="small">{f.paymentMethod}</span> },
    { title: 'Broker', render: (f) => <span className="small muted">{f.brokerName}</span> },
    { title: 'Status', render: (f) => <StatusBadge value={f.status} dot /> },
    { title: 'Date', render: (f) => <span className="small muted">{dateTime(f.createdAt)}</span> },
    {
      title: '',
      render: (f) => f.status === 'PENDING' && can('RISK_OFFICER') && (
        <div className="row" style={{ gap: 4 }}>
          <button className="btn sm" onClick={() => setReview({ f, decision: 'APPROVE' })}><Check size={14} color="var(--green)" /> Approve</button>
          <button className="btn sm" onClick={() => setReview({ f, decision: 'REJECT' })}><X size={14} color="var(--red)" /> Reject</button>
        </div>
      ),
    },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Funds" sub="Live margin deposits and withdrawals (UPI, NetBanking, IMPS/NEFT) routed to broker accounts." />
      <div className="grid g-3">
        <Stat label="Total deposits" icon={ArrowDownLeft} tone="green" value={compactInr(k.deposits)} foot="Successful credits" />
        <Stat label="Total withdrawals" icon={ArrowUpRight} tone="cyan" value={compactInr(k.withdrawals)} foot="Successful payouts" />
        <Stat label="Pending review" icon={Clock} tone="amber" value={k.pendingFunds ?? '–'} foot="Awaiting risk desk approval" />
      </div>
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Tx ID, UTR, method…" />
          <Select value={list.filters.type} onChange={(v) => list.setFilter('type', v)} allLabel="Deposits & withdrawals" options={['DEPOSIT', 'WITHDRAWAL']} />
          <Select value={list.filters.status} onChange={(v) => list.setFilter('status', v)} allLabel="All statuses" options={['PENDING', 'SUCCESS', 'FAILED', 'REJECTED']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {review && (
        <Confirm
          title={`${review.decision === 'APPROVE' ? 'Approve' : 'Reject'} ${review.f.type.toLowerCase()}`}
          danger={review.decision === 'REJECT'} withReason={review.decision === 'REJECT'}
          confirmLabel={review.decision === 'APPROVE' ? 'Approve' : 'Reject'}
          message={`${inr(review.f.amount)} ${review.f.type.toLowerCase()} for ${review.f.user?.name} via ${review.f.paymentMethod}. ${review.decision === 'APPROVE' ? 'The trader’s available cash will be updated.' : ''}`}
          onConfirm={(note) => run(() => api(`/funds/${review.f._id}/review`, { method: 'POST', body: { decision: review.decision, note } }), 'Transaction reviewed').then(() => { list.reload(); stats.reload(); })}
          onClose={() => setReview(null)} />
      )}
    </div>
  );
}
