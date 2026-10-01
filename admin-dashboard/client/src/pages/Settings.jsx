import { useEffect, useState } from 'react';
import { Brain, Save, Trash2, UserPlus } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { ago, initials, label } from '../lib/format.js';
import { Badge, Card, ErrorBox, Field, Modal, PageHeader, Select, Spinner, Tabs, Toggle, useAction } from '../components/ui.jsx';

const MODES = ['OBSERVE', 'PAPER_TRADING', 'ASSISTED_TRADING', 'AUTOMATED_TRADING'];

export default function SettingsPage() {
  const { can } = useAuth();
  const { data, loading, error, reload } = useApi('/settings');
  const [s, setS] = useState(null);
  const [tab, setTab] = useState('platform');
  const run = useAction();
  useEffect(() => { if (data) setS(structuredClone(data)); }, [data]);
  if (error) return <ErrorBox error={error} />;
  if (loading || !s) return <Spinner />;

  const editable = can();
  const set = (path, v) => {
    const next = structuredClone(s);
    const keys = path.split('.');
    keys.slice(0, -1).reduce((o, k) => o[k], next)[keys.at(-1)] = v;
    setS(next);
  };
  const numField = (path, l, hint) => (
    <Field label={l} hint={hint}>
      <input className="input mono" type="number" step="any" disabled={!editable} value={path.split('.').reduce((o, k) => o[k], s)} onChange={(e) => set(path, Number(e.target.value))} />
    </Field>
  );
  const toggleRow = (path, title, desc, danger) => (
    <div className="row between">
      <div><div className="bold">{title}</div><div className="xs dim">{desc}</div></div>
      <Toggle danger={danger} disabled={!editable} checked={path.split('.').reduce((o, k) => o[k], s)} onChange={(v) => set(path, v)} />
    </div>
  );
  const dirty = JSON.stringify(s) !== JSON.stringify(data);
  const save = () => run(() => api('/settings', { method: 'PUT', body: s }), 'Settings saved').then(reload);

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Settings" sub={editable ? 'Platform-wide configuration pushed to every TradePilot app.' : 'Read-only — only super admins can change platform settings.'}>
        {editable && tab !== 'team' && <button className="btn primary" disabled={!dirty} onClick={save}><Save size={16} /> Save changes</button>}
      </PageHeader>
      <Tabs value={tab} onChange={setTab} tabs={[['platform', 'Platform'], ['risk', 'Default risk'], ['ai', 'AI Council & Copilot'], ['security', 'Security'], ['team', 'Admin team']]} />

      {tab === 'platform' && (
        <div className="grid g-2">
          <Card title="Trading controls">
            <div className="stack">
              {toggleRow('liveTradingEnabled', 'Live trading', 'Allow broker-routed live orders. Off forces every trader to paper mode.')}
              {toggleRow('autoPilotEnabled', 'AutoPilot engine', 'Allow autonomous daily-target trading.')}
              {toggleRow('maintenanceMode', 'Maintenance mode', 'App shows a maintenance banner and blocks new orders.', true)}
              <div className="banner amber small">The global kill switch lives in the Risk Center.</div>
            </div>
          </Card>
          <Card title="Allowed trading modes" sub="Modes traders can choose in the app">
            <div className="stack">
              {MODES.map((m) => (
                <div key={m} className="row between">
                  <span>{label(m)}</span>
                  <Toggle disabled={!editable} checked={s.allowedTradingModes.includes(m)}
                    onChange={(v) => set('allowedTradingModes', v ? [...s.allowedTradingModes, m] : s.allowedTradingModes.filter((x) => x !== m))} />
                </div>
              ))}
              <div className="divider" />
              {numField('usdInrRate', 'USD / INR rate', 'Used to convert US positions for risk sizing')}
            </div>
          </Card>
        </div>
      )}

      {tab === 'risk' && (
        <Card title="Default risk profile" sub="Applied to new portfolios (₹25,000 paper capital, ₹1,000 target, ₹500 loss limit by default)">
          <div className="grid g-3">
            {numField('defaultRisk.startingCapital', 'Starting capital (₹)')}
            {numField('defaultRisk.dailyTarget', 'Daily target (₹)')}
            {numField('defaultRisk.maxDailyLoss', 'Max daily loss (₹)')}
            {numField('defaultRisk.maxRiskPerTradePercent', 'Max risk per trade (%)')}
            {numField('defaultRisk.maxOpenPositions', 'Max open positions')}
            {numField('defaultRisk.maxExposurePercent', 'Max exposure (%)')}
            {numField('defaultRisk.stopLossPercent', 'Default stop-loss distance (%)')}
          </div>
          <div style={{ marginTop: 16 }}>{toggleRow('defaultRisk.profitLockEnabled', 'Profit lock by default', 'Stop trading once the daily target is banked')}</div>
        </Card>
      )}

      {tab === 'ai' && (
        <div className="stack">
          <Card title="Council agents" sub="Enable/disable agents and tune their vote weight">
            <div className="stack">
              {s.aiAgents.map((a, i) => (
                <div key={a.key} className="row wrap" style={{ gap: 14, padding: 12, borderRadius: 12, background: 'var(--surface-2)' }}>
                  <Brain size={20} color="var(--purple)" />
                  <div className="grow" style={{ minWidth: 200 }}><div className="bold">{a.name}</div><div className="xs dim">{a.role}</div></div>
                  <Field label="Model"><input className="input mono" style={{ width: 180 }} disabled={!editable} value={a.model || ''} onChange={(e) => set(`aiAgents.${i}.model`, e.target.value)} /></Field>
                  <Field label="Weight"><input className="input mono" style={{ width: 80 }} type="number" step="0.1" disabled={!editable} value={a.weight} onChange={(e) => set(`aiAgents.${i}.weight`, Number(e.target.value))} /></Field>
                  <Toggle disabled={!editable} checked={a.enabled} onChange={(v) => set(`aiAgents.${i}.enabled`, v)} />
                </div>
              ))}
            </div>
          </Card>
          <div className="grid g-2">
            <Card title="Consensus thresholds">
              <div className="grid g-3">
                {numField('council.minBuyVotes', 'Min BUY votes')}
                {numField('council.minConfidence', 'Min confidence %')}
                {numField('council.maxRiskScore', 'Max risk score')}
              </div>
            </Card>
            <Card title="AI Copilot (Gemini chat)">
              <div className="stack">
                {toggleRow('copilot.enabled', 'Copilot enabled', 'Show the AI Copilot tab in the app')}
                <div className="grid g-2">
                  <Field label="Model"><Select value={s.copilot.model} onChange={(v) => set('copilot.model', v)} options={[['gemini-2.5-flash', 'gemini-2.5-flash'], ['gemini-2.5-pro', 'gemini-2.5-pro']]} /></Field>
                  {numField('copilot.dailyMessageLimit', 'Daily messages / trader')}
                </div>
              </div>
            </Card>
          </div>
        </div>
      )}

      {tab === 'security' && (
        <Card title="Security policy">
          <div className="stack">
            {toggleRow('security.require2fa', 'Require 2FA', 'All traders must verify a 6-digit code at sign-in')}
            {toggleRow('security.requireBiometricForLive', 'Biometric for live trading', 'Require fingerprint/face unlock before live orders')}
            <div style={{ maxWidth: 260 }}>{numField('security.sessionTimeoutMinutes', 'App lock timeout (minutes)', 'PIN / biometric lock screen after inactivity')}</div>
          </div>
        </Card>
      )}

      {tab === 'team' && <Team />}
    </div>
  );
}

function Team() {
  const { admin, can } = useAuth();
  const { data, reload } = useApi('/auth/admins');
  const [adding, setAdding] = useState(false);
  const [f, setF] = useState({ name: '', email: '', password: '', role: 'ANALYST' });
  const run = useAction();
  const add = async () => {
    await run(() => api('/auth/admins', { method: 'POST', body: f }), 'Admin added');
    setAdding(false);
    setF({ name: '', email: '', password: '', role: 'ANALYST' });
    reload();
  };
  return (
    <Card title="Admin team" sub="Roles: Super admin (everything) · Risk officer (risk, funds, brokers, strategies) · Analyst (read + markets) · Support (traders, Copilot moderation)"
      action={can() && <button className="btn primary sm" onClick={() => setAdding(true)}><UserPlus size={15} /> Add admin</button>} pad={false}>
      <div style={{ marginTop: 8 }}>
        {data?.map((a) => (
          <div key={a._id} className="list-item">
            <span className="avatar">{initials(a.name)}</span>
            <div className="grow"><div className="bold">{a.name} {a._id === admin._id && <Badge tone="teal">You</Badge>}</div><div className="xs dim">{a.email}</div></div>
            <Badge tone={a.role === 'SUPER_ADMIN' ? 'purple' : a.role === 'RISK_OFFICER' ? 'red' : 'cyan'}>{label(a.role)}</Badge>
            <span className="xs dim" style={{ width: 110, textAlign: 'right' }}>{a.lastLoginAt ? `seen ${ago(a.lastLoginAt)}` : 'never signed in'}</span>
            {can() && a._id !== admin._id && (
              <button className="btn sm icon ghost" title="Remove" onClick={() => run(() => api(`/auth/admins/${a._id}`, { method: 'DELETE' }), 'Admin removed').then(reload)}><Trash2 size={14} /></button>
            )}
          </div>
        ))}
      </div>
      {adding && (
        <Modal title="Add admin" onClose={() => setAdding(false)}
          footer={<><button className="btn" onClick={() => setAdding(false)}>Cancel</button><button className="btn primary" onClick={add} disabled={!f.name || !f.email || f.password.length < 8}>Add</button></>}>
          <Field label="Name"><input className="input" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
          <Field label="Email"><input className="input" type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
          <Field label="Temporary password" hint="At least 8 characters"><input className="input" type="password" autoComplete="new-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>
          <Field label="Role"><Select value={f.role} onChange={(v) => setF({ ...f, role: v })} options={['SUPER_ADMIN', 'RISK_OFFICER', 'ANALYST', 'SUPPORT']} /></Field>
        </Modal>
      )}
    </Card>
  );
}
