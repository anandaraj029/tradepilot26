import { useList } from '../lib/hooks.js';
import { dateTime, label } from '../lib/format.js';
import { DataTable, Pager, PageHeader, SearchInput, Select, StatusBadge, UserCell } from '../components/ui.jsx';

export default function AuditLog() {
  const list = useList('audit-logs', { actorType: 'ALL', severity: 'ALL', market: 'ALL' }, { limit: 25 });
  const columns = [
    { title: 'Time', render: (a) => <span className="small muted mono">{dateTime(a.createdAt)}</span> },
    { title: 'Severity', render: (a) => <StatusBadge value={a.severity} dot /> },
    { title: 'Action', render: (a) => <span className="small bold">{label(a.action)}</span> },
    { title: 'Actor', render: (a) => <><StatusBadge value={a.actorType} /> <span className="xs dim">{a.actorName}</span></> },
    { title: 'Trader', render: (a) => <UserCell user={a.user} /> },
    { title: 'Asset', render: (a) => (a.symbol ? <span className="small">{a.symbol} <span className="dim xs">{a.market}</span></span> : <span className="dim">—</span>) },
    { title: 'AI verdict', render: (a) => (a.aiConsensusVerdict && a.aiConsensusVerdict !== 'N/A' ? <StatusBadge value={a.aiConsensusVerdict} /> : <span className="dim">—</span>) },
    { title: 'Risk engine', render: (a) => (a.riskEngineStatus && a.riskEngineStatus !== 'N/A' ? <StatusBadge value={a.riskEngineStatus} /> : <span className="dim">—</span>) },
    { title: 'Details', render: (a) => <span className="small muted ellipsis" style={{ maxWidth: 340, display: 'inline-block' }} title={a.details}>{a.details}</span> },
  ];
  return (
    <>
      <PageHeader title="Audit Log" sub="Immutable trail of AI consensus decisions, risk engine evaluations, executions and admin interventions." />
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Action, symbol, details, actor…" />
          <Select value={list.filters.actorType} onChange={(v) => list.setFilter('actorType', v)} allLabel="All actors" options={['SYSTEM', 'AI', 'USER', 'ADMIN']} />
          <Select value={list.filters.severity} onChange={(v) => list.setFilter('severity', v)} allLabel="All severities" options={['INFO', 'WARNING', 'CRITICAL']} />
          <Select value={list.filters.market} onChange={(v) => list.setFilter('market', v)} allLabel="All markets" options={['INDIA', 'USA']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
    </>
  );
}
