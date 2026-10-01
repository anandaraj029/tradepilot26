import { useState } from 'react';
import { api } from '../lib/api.js';
import { Field, Modal, Toggle, useAction } from './ui.jsx';

const FIELDS = [
  ['startingCapital', 'Starting capital (₹)', 'Virtual / allocated capital base'],
  ['dailyTarget', 'Daily profit target (₹)', 'Profit lock engages when reached'],
  ['maxDailyLoss', 'Max daily loss (₹)', 'Trading locks when breached'],
  ['maxRiskPerTradePercent', 'Max risk per trade (%)', 'Position sizing cap of capital'],
  ['maxOpenPositions', 'Max open positions', 'Concurrent position cap'],
  ['maxExposurePercent', 'Max portfolio exposure (%)', 'Risk engine rejects above this'],
];

/** Edits the deterministic RiskEngine limits of one portfolio. */
export default function RiskLimitsModal({ portfolio, onClose, onSaved }) {
  const [form, setForm] = useState(() => Object.fromEntries([...FIELDS.map(([k]) => [k, portfolio[k]]), ['profitLockEnabled', portfolio.profitLockEnabled]]));
  const run = useAction();
  const save = async () => {
    const body = { ...form };
    FIELDS.forEach(([k]) => { body[k] = Number(body[k]); });
    await run(() => api(`/portfolios/${portfolio._id}/limits`, { method: 'PATCH', body }), 'Risk limits updated');
    onSaved?.();
    onClose();
  };
  return (
    <Modal
      title={`Risk limits${portfolio.user?.name ? ` · ${portfolio.user.name}` : ''}`}
      onClose={onClose}
      footer={<><button className="btn" onClick={onClose}>Cancel</button><button className="btn primary" onClick={save}>Save limits</button></>}
    >
      <div className="grid g-2">
        {FIELDS.map(([k, l, hint]) => (
          <Field key={k} label={l} hint={hint}>
            <input className="input mono" type="number" step="any" value={form[k]} onChange={(e) => setForm({ ...form, [k]: e.target.value })} />
          </Field>
        ))}
      </div>
      <div className="row between card card-pad" style={{ boxShadow: 'none' }}>
        <div>
          <div className="bold">Profit lock</div>
          <div className="xs dim">Stop trading for the day once the daily target is achieved</div>
        </div>
        <Toggle checked={form.profitLockEnabled} onChange={(v) => setForm({ ...form, profitLockEnabled: v })} />
      </div>
    </Modal>
  );
}
