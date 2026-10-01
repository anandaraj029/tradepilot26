import { useState } from 'react';
import { ExternalLink, KeyRound, Plug, ShieldOff } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { ago, compactInr, inr } from '../lib/format.js';
import { Badge, Confirm, DataTable, Pager, PageHeader, SearchInput, Select, StatusBadge, Tabs, Toggle, UserCell, useAction } from '../components/ui.jsx';

export default function Brokers() {
  const [tab, setTab] = useState('connections');
  return (
    <>
      <PageHeader title="Broker Connections" sub="OAuth2 broker integrations (Zerodha, Angel One, Upstox, Dhan, Groww, IBKR) and live order routing." />
      <Tabs value={tab} onChange={setTab} tabs={[['connections', 'Trader connections'], ['catalog', 'Supported brokers']]} />
      {tab === 'connections' ? <Connections /> : <Catalog />}
    </>
  );
}

function Connections() {
  const { can } = useAuth();
  const run = useAction();
  const list = useList('broker-connections', { status: 'ALL', brokerCode: 'ALL', isLiveRoutingActive: 'ALL' }, { limit: 20 });
  const [revoke, setRevoke] = useState(null);

  const columns = [
    { title: 'Trader', render: (c) => <UserCell user={c.user} /> },
    { title: 'Broker', render: (c) => <><div className="bold small">{c.brokerName}</div><div className="xs dim mono">{c.brokerUserId}</div></> },
    { title: 'Status', render: (c) => <StatusBadge value={c.status} dot /> },
    { title: 'Live routing', render: (c) => (c.isLiveRoutingActive ? <Badge tone="green">Active</Badge> : <Badge>Off</Badge>) },
    { title: 'Autonomous', render: (c) => (c.autoPilotAutonomousLive ? <Badge tone="purple">AutoPilot live</Badge> : <span className="dim small">—</span>) },
    { title: 'Margin', right: true, render: (c) => <><div className="mono">{inr(c.liveMargin)}</div><div className="xs dim mono">used {compactInr(c.usedMargin)}</div></> },
    { title: 'Latency', right: true, render: (c) => <span className={`mono ${c.latencyMs > 80 ? 'warn' : 'pos'}`}>{c.latencyMs} ms</span> },
    { title: 'Token', render: (c) => <span className="small row" style={{ gap: 4 }}><KeyRound size={13} color={c.isEncryptedInVault ? 'var(--green)' : 'var(--red)'} /> {c.status === 'REVOKED' ? 'revoked' : `expires ${ago(c.tokenExpiresAt)}`}</span> },
    { title: 'Synced', render: (c) => <span className="small muted">{ago(c.lastSyncedAt)}</span> },
    { title: '', render: (c) => can('RISK_OFFICER') && c.status !== 'REVOKED' && <button className="btn sm" onClick={() => setRevoke(c)}><ShieldOff size={14} /> Revoke</button> },
  ];

  return (
    <div className="card">
      <div className="toolbar">
        <SearchInput value={list.q} onChange={list.setQ} placeholder="Broker, client ID…" />
        <Select value={list.filters.brokerCode} onChange={(v) => list.setFilter('brokerCode', v)} allLabel="All brokers" options={['ZERODHA', 'ANGEL_ONE', 'UPSTOX', 'DHAN', 'GROWW', 'IBKR']} />
        <Select value={list.filters.status} onChange={(v) => list.setFilter('status', v)} allLabel="All statuses" options={['ACTIVE', 'TOKEN_EXPIRING', 'DISCONNECTED', 'AUTHENTICATING', 'REVOKED']} />
        <Select value={list.filters.isLiveRoutingActive} onChange={(v) => list.setFilter('isLiveRoutingActive', v)} allLabel="Routing: any" options={[['true', 'Live routing on'], ['false', 'Live routing off']]} />
      </div>
      <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
      <Pager data={list.data} page={list.page} setPage={list.setPage} />
      {revoke && (
        <Confirm title="Revoke broker access" danger withReason confirmLabel="Revoke token"
          message={`Invalidates ${revoke.user?.name}'s ${revoke.brokerName} session, stops live routing and autonomous AutoPilot. The trader must re-authenticate via OAuth2.`}
          onConfirm={() => run(() => api(`/broker-connections/${revoke._id}/revoke`, { method: 'POST' }), 'Broker access revoked').then(list.reload)}
          onClose={() => setRevoke(null)} />
      )}
    </div>
  );
}

function Catalog() {
  const { can } = useAuth();
  const run = useAction();
  const { data, reload } = useApi('/brokers?limit=50');
  const toggle = (b, body) => run(() => api(`/brokers/${b._id}/toggle`, { method: 'POST', body }), 'Broker updated').then(reload);
  return (
    <div className="grid g-3">
      {data?.items.map((b) => (
        <div key={b._id} className="card card-pad stack" style={{ gap: 12, opacity: b.enabled ? 1 : 0.6 }}>
          <div className="row">
            <span className="stat-icon" style={{ background: 'var(--cyan-soft, var(--surface-3))', color: 'var(--cyan)', width: 36, height: 36 }}><Plug size={18} /></span>
            <div className="grow"><div className="bold">{b.name}</div><div className="xs dim">{b.authType}</div></div>
            {!b.isPrimaryInIndia && <Badge tone="purple">Global</Badge>}
          </div>
          <p className="small muted" style={{ margin: 0 }}>{b.tagline}</p>
          <div className="row wrap" style={{ gap: 4 }}>{b.defaultScopes.map((s) => <span key={s} className="badge mono" style={{ fontWeight: 500 }}>{s}</span>)}</div>
          <div className="divider" />
          <div className="row between small"><span>Available in app</span><Toggle checked={b.enabled} disabled={!can('RISK_OFFICER')} onChange={(v) => toggle(b, { enabled: v })} /></div>
          <div className="row between small"><span>Live trading allowed</span><Toggle checked={b.liveTradingAllowed} disabled={!can('RISK_OFFICER')} onChange={(v) => toggle(b, { liveTradingAllowed: v })} /></div>
          <a href={b.docUrl} target="_blank" rel="noreferrer" className="small row" style={{ gap: 4 }}>API docs <ExternalLink size={12} /></a>
        </div>
      ))}
    </div>
  );
}
