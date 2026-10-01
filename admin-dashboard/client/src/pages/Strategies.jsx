import { useState } from 'react';
import { Check, FlaskConical, Pencil, Plus, X } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { date, inr, label } from '../lib/format.js';
import {
  Badge, Confirm, DataTable, Field, Modal, Pager, PageHeader, SearchInput, Select, StatusBadge, Tabs, Toggle, UserCell, useAction,
} from '../components/ui.jsx';

export default function Strategies() {
  const [tab, setTab] = useState('system');
  const totals = useApi('/stats/strategies');
  const review = totals.data?.custom?.find((c) => c._id === 'UNDER_REVIEW')?.count || 0;
  return (
    <>
      <PageHeader title="Strategy Lab" sub="Platform strategies offered in the app, plus AI-generated custom strategies submitted by traders." />
      <Tabs value={tab} onChange={setTab} tabs={[['system', 'Platform strategies'], ['custom', `Custom strategies${review ? ` · ${review} to review` : ''}`]]} />
      {tab === 'system' ? <SystemStrategies /> : <CustomStrategies onChange={totals.reload} />}
    </>
  );
}

function SystemStrategies() {
  const { can } = useAuth();
  const { data, reload } = useApi('/strategies?limit=100');
  const [edit, setEdit] = useState(null);
  const canEdit = can('RISK_OFFICER');
  return (
    <div className="stack">
      {canEdit && <div className="row" style={{ justifyContent: 'flex-end' }}><button className="btn primary" onClick={() => setEdit({})}><Plus size={16} /> New strategy</button></div>}
      <div className="grid g-2">
        {data?.items.map((s) => (
          <div key={s._id} className="card card-pad stack" style={{ gap: 12 }}>
            <div className="row">
              <span className="stat-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)', width: 36, height: 36 }}><FlaskConical size={18} /></span>
              <div className="grow">
                <div className="bold">{s.name}</div>
                <div className="xs dim">{s.category} · {s.key}</div>
              </div>
              <StatusBadge value={s.status} dot />
              {canEdit && <button className="btn sm icon ghost" onClick={() => setEdit(s)} title="Edit"><Pencil size={14} /></button>}
            </div>
            <p className="small muted" style={{ margin: 0 }}>{s.description}</p>
            <div className="grid g-4" style={{ gap: 8 }}>
              {[['Win rate', `${s.winRate}%`], ['Profit factor', s.profitFactor], ['Max DD', `${s.maxDrawdown}%`], ['Trades', s.tradesCount]].map(([l, v]) => (
                <div key={l} style={{ background: 'var(--surface-2)', borderRadius: 10, padding: '8px 10px' }}>
                  <div className="xs dim">{l}</div><div className="mono bold">{v}</div>
                </div>
              ))}
            </div>
            <div className="small"><span className="dim">Entry · </span>{s.entryRules}</div>
            <div className="small"><span className="dim">Exit · </span>{s.exitRules}</div>
            <div className="row xs dim">Avg hold {s.avgHoldingDays}d · {s.allowLive ? <Badge tone="green">Live execution allowed</Badge> : <Badge>Paper only</Badge>}</div>
          </div>
        ))}
      </div>
      {edit && <StrategyModal s={edit} onClose={() => setEdit(null)} onSaved={reload} />}
    </div>
  );
}

function StrategyModal({ s, onClose, onSaved }) {
  const isNew = !s._id;
  const [f, setF] = useState({ status: 'ACTIVE', allowLive: false, winRate: 0, profitFactor: 0, maxDrawdown: 0, tradesCount: 0, avgHoldingDays: 1, ...s });
  const run = useAction();
  const set = (k, v) => setF({ ...f, [k]: v });
  const save = async () => {
    const body = { ...f };
    ['winRate', 'profitFactor', 'maxDrawdown', 'tradesCount', 'avgHoldingDays'].forEach((k) => { body[k] = Number(body[k]); });
    await run(() => api(isNew ? '/strategies' : `/strategies/${s._id}`, { method: isNew ? 'POST' : 'PATCH', body }), 'Strategy saved');
    onSaved();
    onClose();
  };
  return (
    <Modal title={isNew ? 'New platform strategy' : `Edit ${s.name}`} onClose={onClose} wide
      footer={<><button className="btn" onClick={onClose}>Cancel</button><button className="btn primary" disabled={!f.name || !f.key} onClick={save}>Save</button></>}>
      <div className="grid g-3">
        <Field label="Key"><input className="input mono" value={f.key || ''} disabled={!isNew} onChange={(e) => set('key', e.target.value)} placeholder="strat-6" /></Field>
        <Field label="Name"><input className="input" value={f.name || ''} onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="Category"><input className="input" value={f.category || ''} onChange={(e) => set('category', e.target.value)} /></Field>
      </div>
      <Field label="Description"><textarea className="input" rows={2} value={f.description || ''} onChange={(e) => set('description', e.target.value)} /></Field>
      <div className="grid g-2">
        <Field label="Entry rules"><textarea className="input" rows={2} value={f.entryRules || ''} onChange={(e) => set('entryRules', e.target.value)} /></Field>
        <Field label="Exit rules"><textarea className="input" rows={2} value={f.exitRules || ''} onChange={(e) => set('exitRules', e.target.value)} /></Field>
      </div>
      <div className="grid g-3">
        {[['winRate', 'Win rate %'], ['profitFactor', 'Profit factor'], ['maxDrawdown', 'Max drawdown %'], ['tradesCount', 'Trades'], ['avgHoldingDays', 'Avg holding days']].map(([k, l]) => (
          <Field key={k} label={l}><input className="input mono" type="number" step="any" value={f[k]} onChange={(e) => set(k, e.target.value)} /></Field>
        ))}
        <Field label="Status"><Select value={f.status} onChange={(v) => set('status', v)} options={['ACTIVE', 'PAUSED', 'DEPRECATED']} /></Field>
      </div>
      <div className="row between"><div><div className="bold">Allow live execution</div><div className="xs dim">Otherwise restricted to paper trading in the app</div></div><Toggle checked={f.allowLive} onChange={(v) => set('allowLive', v)} /></div>
    </Modal>
  );
}

function CustomStrategies({ onChange }) {
  const { can } = useAuth();
  const run = useAction();
  const list = useList('custom-strategies', { status: 'ALL' }, { limit: 15 });
  const [dialog, setDialog] = useState(null);
  const [view, setView] = useState(null);
  const decide = (s, status) => setDialog({ s, status });

  const columns = [
    { title: 'Strategy', render: (s) => <><div className="bold">{s.name}</div><div className="xs dim ellipsis" style={{ maxWidth: 300 }}>{s.prompt}</div></> },
    { title: 'Trader', render: (s) => <UserCell user={s.user} /> },
    { title: 'Capital', right: true, render: (s) => <span className="mono">{inr(s.capitalAllocation)}</span> },
    { title: 'Max risk', right: true, render: (s) => <span className="mono">{s.maxRiskPercent}%</span> },
    { title: 'Status', render: (s) => <StatusBadge value={s.status} /> },
    { title: 'Created', render: (s) => <span className="small muted">{date(s.createdAt)}</span> },
    {
      title: '',
      render: (s) => can('RISK_OFFICER') && (
        <div className="row" style={{ gap: 4 }} onClick={(e) => e.stopPropagation()}>
          {s.status !== 'APPROVED_LIVE' && <button className="btn sm" onClick={() => decide(s, 'APPROVED_LIVE')}><Check size={14} color="var(--green)" /> Approve live</button>}
          {s.status !== 'REJECTED' && <button className="btn sm" onClick={() => decide(s, 'REJECTED')}><X size={14} color="var(--red)" /> Reject</button>}
        </div>
      ),
    },
  ];

  return (
    <div className="card">
      <div className="toolbar">
        <SearchInput value={list.q} onChange={list.setQ} placeholder="Search name or prompt…" />
        <Select value={list.filters.status} onChange={(v) => list.setFilter('status', v)} allLabel="All statuses" options={['UNDER_REVIEW', 'PAPER_TRADING_ONLY', 'APPROVED_LIVE', 'REJECTED', 'ARCHIVED']} />
      </div>
      <DataTable columns={columns} rows={list.data?.items} loading={list.loading} onRowClick={setView} />
      <Pager data={list.data} page={list.page} setPage={list.setPage} />
      {view && (
        <Modal title={view.name} onClose={() => setView(null)}>
          <StatusBadge value={view.status} />
          <div><div className="small bold">Prompt</div><p className="pre">{view.prompt}</p></div>
          <div><div className="small bold">Entry criteria</div><p className="pre">{view.entryCriteria}</p></div>
          <div><div className="small bold">Exit criteria</div><p className="pre">{view.exitCriteria}</p></div>
          {view.reviewNote && <div><div className="small bold">Review note</div><p className="pre">{view.reviewNote}</p></div>}
        </Modal>
      )}
      {dialog && (
        <Confirm title={`${label(dialog.status)}: ${dialog.s.name}`} withReason danger={dialog.status === 'REJECTED'} confirmLabel={label(dialog.status)}
          message={dialog.status === 'APPROVED_LIVE' ? 'The trader can route this strategy to their live broker, still subject to risk engine limits.' : 'The strategy will be restricted and the trader notified.'}
          onConfirm={(note) => run(() => api(`/custom-strategies/${dialog.s._id}/review`, { method: 'POST', body: { status: dialog.status, note } }), 'Review saved').then(() => { list.reload(); onChange(); })}
          onClose={() => setDialog(null)} />
      )}
    </div>
  );
}
