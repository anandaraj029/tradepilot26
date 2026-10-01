import { Bot, CircleStop, Crosshair, Rocket, Target } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { ago, label } from '../lib/format.js';
import { Card, DataTable, Pager, PageHeader, Select, Stat, StatusBadge, Toggle, UserCell, useAction } from '../components/ui.jsx';

const STATES = ['ACTIVE_HUNTING', 'TARGET_ACHIEVED', 'LOSS_LIMIT', 'LOCKED', 'MAX_POSITIONS'];

export default function AutoPilot() {
  const { can } = useAuth();
  const run = useAction();
  const list = useList('users', { 'autoPilot.enabled': 'true', 'autoPilot.state': 'ALL' }, { limit: 20, sort: '-autoPilot.lastRunAt' });
  const settings = useApi('/settings');
  const summary = useApi("/stats/autopilot", [list.data]);
  const counts = { ...Object.fromEntries(STATES.map((s) => [s, 0])), ...summary.data?.states };

  const columns = [
    { title: 'Trader', render: (u) => <UserCell user={u} /> },
    { title: 'Engine state', render: (u) => <StatusBadge value={u.autoPilot?.state} dot /> },
    { title: 'Mode', render: (u) => <StatusBadge value={u.tradingMode} /> },
    { title: 'Autonomous live', render: (u) => (u.autoPilot?.autonomousLive ? <span className="badge purple">Live broker</span> : <span className="badge">Paper</span>) },
    { title: 'Last engine message', render: (u) => <span className="small muted ellipsis" style={{ maxWidth: 420, display: 'inline-block' }}>{u.autoPilot?.lastMessage}</span> },
    { title: 'Last run', render: (u) => <span className="small muted">{ago(u.autoPilot?.lastRunAt)}</span> },
    {
      title: 'Enabled',
      render: (u) => (
        <Toggle checked={u.autoPilot?.enabled} disabled={!can('RISK_OFFICER')}
          onChange={(v) => run(() => api(`/users/${u._id}/autopilot`, { method: 'POST', body: { enabled: v } }), v ? 'AutoPilot enabled' : 'AutoPilot stopped').then(list.reload)} />
      ),
    },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="AutoPilot" sub="Autonomous engine that hunts setups until the daily target is reached, then engages profit lock." />
      <div className="grid g-4">
        <Stat label="AutoPilot engaged" icon={Rocket} tone="purple" value={summary.data?.total ?? '–'} foot={`Platform AutoPilot ${settings.data?.autoPilotEnabled ? 'enabled' : 'disabled'}`} />
        <Stat label="Actively hunting" icon={Crosshair} tone="teal" value={counts.ACTIVE_HUNTING} foot="Scanning for RSI 42–68 momentum entries" />
        <Stat label="Target achieved" icon={Target} tone="green" value={counts.TARGET_ACHIEVED} foot="Stopped with profit lock" />
        <Stat label="Stopped on loss / limits" icon={CircleStop} tone="red" value={counts.LOSS_LIMIT + counts.LOCKED + counts.MAX_POSITIONS} foot="Loss boundary, lock or max positions" />
      </div>
      <Card title="Engine decision flow" sub="AutoPilotEngine.evaluate() — run on every tick per trader">
        <div className="row wrap" style={{ gap: 8 }}>
          {['Target reached? → stop & lock', 'Loss limit hit? → stop', 'Locked / kill switch? → idle', 'Book profit at TP or +2%', 'Cut at stop loss', 'Max positions? → idle', 'Buy best RSI 42–68 candidate (≤25% cash, max ₹6,000)'].map((s, i) => (
            <span key={s} className="badge" style={{ padding: '6px 10px' }}><span className="mono teal" style={{ color: 'var(--teal)' }}>{i + 1}</span> {s}</span>
          ))}
        </div>
      </Card>
      <div className="card">
        <div className="toolbar">
          <Bot size={18} color="var(--purple)" />
          <span className="bold grow">AutoPilot traders</span>
          <Select value={list.filters['autoPilot.enabled']} onChange={(v) => list.setFilter('autoPilot.enabled', v)} allLabel="Enabled & disabled" options={[['true', 'Enabled'], ['false', 'Disabled']]} />
          <Select value={list.filters['autoPilot.state']} onChange={(v) => list.setFilter('autoPilot.state', v)} allLabel="All states" options={STATES.map((s) => [s, label(s)])} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
    </div>
  );
}
