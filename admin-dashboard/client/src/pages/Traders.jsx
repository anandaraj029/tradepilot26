import { useNavigate } from 'react-router-dom';
import { Fingerprint, ShieldCheck } from 'lucide-react';
import { useList } from '../lib/hooks.js';
import { ago, date, label } from '../lib/format.js';
import { Badge, DataTable, Pager, PageHeader, SearchInput, Select, StatusBadge, UserCell } from '../components/ui.jsx';

export const MODES = ['OBSERVE', 'PAPER_TRADING', 'ASSISTED_TRADING', 'AUTOMATED_TRADING'];

export default function Traders() {
  const nav = useNavigate();
  const list = useList('users', { status: 'ALL', tradingMode: 'ALL', tier: 'ALL' }, { limit: 20 });

  const columns = [
    { title: 'Trader', render: (u) => <UserCell user={u} /> },
    { title: 'Status', render: (u) => <StatusBadge value={u.status} dot /> },
    { title: 'Mode', render: (u) => <StatusBadge value={u.tradingMode} /> },
    { title: 'Tier', render: (u) => <span className="small">{u.tier}</span> },
    { title: 'Broker', render: (u) => <span className="small muted">{u.brokerIntegration}</span> },
    { title: 'AutoPilot', render: (u) => (u.autoPilot?.enabled ? <StatusBadge value={u.autoPilot.state} /> : <span className="dim small">Off</span>) },
    {
      title: 'Security',
      render: (u) => (
        <div className="row" style={{ gap: 6 }}>
          <span title="2FA"><ShieldCheck size={16} color={u.is2faEnabled ? 'var(--green)' : 'var(--text-3)'} /></span>
          <span title="Biometric"><Fingerprint size={16} color={u.isBiometricEnabled ? 'var(--green)' : 'var(--text-3)'} /></span>
          {u.kycStatus !== 'VERIFIED' && <Badge tone="amber">KYC {label(u.kycStatus)}</Badge>}
        </div>
      ),
    },
    { title: 'Device', render: (u) => <span className="xs muted">{u.device?.model} · v{u.device?.appVersion}</span> },
    { title: 'Last login', render: (u) => <span className="small muted">{ago(u.lastLoginAt)}</span> },
    { title: 'Joined', render: (u) => <span className="small muted">{date(u.createdAt)}</span> },
  ];

  return (
    <>
      <PageHeader title="Traders" sub="Every mobile app account with its trading mode, security posture and AutoPilot state." />
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Search name, email, phone…" />
          <Select value={list.filters.status} onChange={(v) => list.setFilter('status', v)} allLabel="All statuses" options={['ACTIVE', 'SUSPENDED', 'PENDING_KYC']} />
          <Select value={list.filters.tradingMode} onChange={(v) => list.setFilter('tradingMode', v)} allLabel="All modes" options={MODES} />
          <Select value={list.filters.tier} onChange={(v) => list.setFilter('tier', v)} allLabel="All tiers" options={['Tier 1 Capital Shield', 'Tier 2 Growth', 'Tier 3 Pro Quant'].map((t) => [t, t])} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} onRowClick={(u) => nav(`/traders/${u._id}`)} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
    </>
  );
}
