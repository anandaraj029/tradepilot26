import { createContext, useCallback, useContext, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, ChevronLeft, ChevronRight, Search, X, XCircle } from 'lucide-react';
import { initials, label } from '../lib/format.js';

// ------------------------------------------------------------------ Toasts
const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const push = useCallback((message, type = 'success') => {
    const id = Math.random();
    setToasts((t) => [...t, { id, message, type }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3500);
  }, []);
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="toasts">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>
            {t.type === 'error' ? <XCircle size={18} color="var(--red)" /> : <CheckCircle2 size={18} color="var(--green)" />}
            {t.message}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

/** Wraps an async action with success/error toasts. */
export function useAction() {
  const toast = useToast();
  return async (fn, success) => {
    try {
      const r = await fn();
      if (success) toast(success);
      return r;
    } catch (e) {
      toast(e.message, 'error');
      throw e;
    }
  };
}

// ------------------------------------------------------------------ Layout bits
export const PageHeader = ({ title, sub, children }) => (
  <div className="page-head">
    <div>
      <h1 className="page-title">{title}</h1>
      {sub && <p className="page-sub">{sub}</p>}
    </div>
    {children && <div className="row wrap">{children}</div>}
  </div>
);

export const Card = ({ title, sub, action, children, pad = true, className = '', style }) => (
  <div className={`card ${className}`} style={style}>
    {(title || action) && (
      <div className="card-head">
        <div>
          {title && <h3 className="card-title">{title}</h3>}
          {sub && <p className="card-sub">{sub}</p>}
        </div>
        {action}
      </div>
    )}
    <div className={pad ? 'card-pad' : ''}>{children}</div>
  </div>
);

export const Stat = ({ label: l, value, foot, icon: Icon, tone = 'teal', valueClass = '' }) => (
  <div className="card stat">
    <div className="stat-label">
      {Icon && (
        <span className="stat-icon" style={{ background: `var(--${tone}-soft, var(--surface-3))`, color: `var(--${tone})` }}>
          <Icon size={15} />
        </span>
      )}
      {l}
    </div>
    <div className={`stat-value ${valueClass}`}>{value}</div>
    {foot && <div className="stat-foot">{foot}</div>}
  </div>
);

export const Spinner = () => <div className="center"><div className="spinner" /></div>;
export const ErrorBox = ({ error }) => <div className="banner red">{error}</div>;

// ------------------------------------------------------------------ Badges
const TONES = {
  green: ['ACTIVE', 'SUCCESS', 'PROFIT', 'BUY', 'SAFE', 'APPROVED_LIVE', 'QUALIFIED', 'EXECUTED', 'VERIFIED', 'TARGET_ACHIEVED', 'CLOSED', 'LIVE', 'INFO'],
  red: ['SUSPENDED', 'FAILED', 'LOSS', 'SELL', 'LOCKED', 'CRITICAL', 'REJECTED', 'REVOKED', 'BLOCKED', 'LOSS_LIMIT', 'DISCONNECTED', 'NO_TRADE', 'EXIT'],
  amber: ['PENDING', 'PENDING_KYC', 'CAUTION', 'WAIT', 'UNDER_REVIEW', 'NEEDS_TUNING', 'TOKEN_EXPIRING', 'WARNING', 'OVERRIDDEN', 'MAX_POSITIONS', 'OPEN', 'PAUSED'],
  teal: ['ACTIVE_HUNTING', 'PAPER', 'PAPER_TRADING', 'PAPER_TRADING_ONLY', 'AUTHENTICATING', 'DEPOSIT'],
  purple: ['AUTOMATED_TRADING', 'AI', 'AUTOPILOT', 'ADMIN', 'HOLD'],
  cyan: ['ASSISTED_TRADING', 'ASSISTED', 'USER', 'WITHDRAWAL'],
};
const toneOf = (v) => Object.keys(TONES).find((k) => TONES[k].includes(v)) || '';

export const Badge = ({ children, tone, dot }) => (
  <span className={`badge ${tone || ''}`}>
    {dot && <span className="dot" />}
    {children}
  </span>
);

export const StatusBadge = ({ value, dot }) =>
  value == null ? <span className="dim">—</span> : <Badge tone={toneOf(value)} dot={dot}>{label(String(value))}</Badge>;

// ------------------------------------------------------------------ Inputs
export const Toggle = ({ checked, onChange, danger, disabled }) => (
  <label className={`toggle ${danger ? 'danger' : ''}`}>
    <input type="checkbox" checked={!!checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} />
    <span />
  </label>
);

export const SearchInput = ({ value, onChange, placeholder = 'Search…' }) => (
  <div className="search">
    <Search size={15} />
    <input className="input" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
  </div>
);

export const Select = ({ value, onChange, options, allLabel }) => (
  <select className="select" value={value ?? 'ALL'} onChange={(e) => onChange(e.target.value)}>
    {allLabel && <option value="ALL">{allLabel}</option>}
    {options.map((o) => {
      const [v, l] = Array.isArray(o) ? o : [o, label(String(o))];
      return <option key={v} value={v}>{l}</option>;
    })}
  </select>
);

export const Field = ({ label: l, hint, children }) => (
  <div className="field">
    <label>{l}</label>
    {children}
    {hint && <span className="hint">{hint}</span>}
  </div>
);

export const Seg = ({ value, onChange, options }) => (
  <div className="seg">
    {options.map(([v, l]) => (
      <button key={v} className={value === v ? 'active' : ''} onClick={() => onChange(v)}>{l}</button>
    ))}
  </div>
);

export const Tabs = ({ value, onChange, tabs }) => (
  <div className="tabs">
    {tabs.map(([v, l]) => (
      <button key={v} className={`tab ${value === v ? 'active' : ''}`} onClick={() => onChange(v)}>{l}</button>
    ))}
  </div>
);

// ------------------------------------------------------------------ Modal
export function Modal({ title, onClose, children, footer, wide }) {
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true">
        <div className="modal-head">
          <h3 className="card-title" style={{ fontSize: 16 }}>{title}</h3>
          <button className="btn ghost icon sm" onClick={onClose} aria-label="Close"><X size={18} /></button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

/** Confirmation dialog with an optional reason field (logged to audit trail). */
export function Confirm({ title, message, confirmLabel = 'Confirm', danger, withReason, onConfirm, onClose }) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  return (
    <Modal
      title={title}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose}>Cancel</button>
          <button
            className={`btn ${danger ? 'danger' : 'primary'}`}
            disabled={busy || (withReason && !reason.trim())}
            onClick={async () => {
              setBusy(true);
              try { await onConfirm(reason); onClose(); } catch { setBusy(false); }
            }}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <p className="muted" style={{ margin: 0 }}>{message}</p>
      {withReason && (
        <Field label="Reason (recorded in audit log)">
          <textarea className="input" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} />
        </Field>
      )}
    </Modal>
  );
}

// ------------------------------------------------------------------ Table
export function DataTable({ columns, rows, onRowClick, empty = 'No records found', loading }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>{columns.map((c) => <th key={c.key || c.title} className={c.right ? 'r' : ''}>{c.title}</th>)}</tr>
        </thead>
        <tbody>
          {rows?.map((r, i) => (
            <tr key={r._id || i} className={onRowClick ? 'clickable' : ''} onClick={() => onRowClick?.(r)}>
              {columns.map((c) => (
                <td key={c.key || c.title} className={c.right ? 'r' : ''}>
                  {c.render ? c.render(r) : r[c.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {loading && !rows?.length && <Spinner />}
      {!loading && !rows?.length && <div className="empty">{empty}</div>}
    </div>
  );
}

export function Pager({ data, page, setPage }) {
  if (!data) return null;
  const from = data.total ? (page - 1) * data.limit + 1 : 0;
  const to = Math.min(page * data.limit, data.total);
  return (
    <div className="pager">
      <span>{from}–{to} of {data.total.toLocaleString('en-IN')}</span>
      <div className="row">
        <button className="btn sm icon" disabled={page <= 1} onClick={() => setPage(page - 1)} aria-label="Previous page"><ChevronLeft size={16} /></button>
        <span className="mono">{page} / {Math.max(1, data.pages)}</span>
        <button className="btn sm icon" disabled={page >= data.pages} onClick={() => setPage(page + 1)} aria-label="Next page"><ChevronRight size={16} /></button>
      </div>
    </div>
  );
}

export const UserCell = ({ user, sub }) =>
  user ? (
    <Link to={`/traders/${user._id}`} className="cell-user" onClick={(e) => e.stopPropagation()} style={{ color: 'inherit' }}>
      <span className="avatar">{initials(user.name)}</span>
      <span>
        <div style={{ fontWeight: 600 }}>{user.name}</div>
        <div className="xs dim">{sub ?? user.email}</div>
      </span>
    </Link>
  ) : <span className="dim">—</span>;

export const ChartTip = ({ active, payload, label: l, fmt = (v) => v }) =>
  active && payload?.length ? (
    <div className="chart-tip">
      <div className="bold" style={{ marginBottom: 4 }}>{l}</div>
      {payload.map((p) => (
        <div key={p.dataKey} className="row" style={{ gap: 6 }}>
          <span className="dot" style={{ color: p.color || p.fill }} />
          <span className="muted">{p.name}</span>
          <span className="mono" style={{ marginLeft: 'auto' }}>{fmt(p.value, p.dataKey)}</span>
        </div>
      ))}
    </div>
  ) : null;

export const Sparkline = ({ data = [], width = 90, height = 28 }) => {
  if (data.length < 2) return null;
  const min = Math.min(...data), max = Math.max(...data);
  const pts = data.map((v, i) => `${(i / (data.length - 1)) * width},${height - ((v - min) / (max - min || 1)) * (height - 4) - 2}`).join(' ');
  const up = data[data.length - 1] >= data[0];
  return (
    <svg width={width} height={height} aria-hidden>
      <polyline points={pts} fill="none" stroke={up ? 'var(--green)' : 'var(--red)'} strokeWidth="1.6" strokeLinejoin="round" />
    </svg>
  );
};

export const Progress = ({ value, max = 100, color }) => (
  <div className="progress">
    <div style={{ width: `${Math.max(0, Math.min(100, (value / max) * 100))}%`, background: color }} />
  </div>
);
