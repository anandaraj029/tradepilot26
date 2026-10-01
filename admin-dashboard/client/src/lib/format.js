const inrFmt = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const inrFmt2 = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const usdFmt = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
const numFmt = new Intl.NumberFormat('en-IN');

export const inr = (v, precise = false) => (v == null ? '—' : (precise ? inrFmt2 : inrFmt).format(v));
export const signedInr = (v) => (v == null ? '—' : `${v > 0 ? '+' : ''}${inrFmt.format(v)}`);
export const money = (v, market) => (v == null ? '—' : market === 'USA' ? usdFmt.format(v) : inrFmt2.format(v));
export const num = (v) => (v == null ? '—' : numFmt.format(Math.round(v)));
export const pct = (v, d = 1) => (v == null ? '—' : `${v > 0 ? '+' : ''}${Number(v).toFixed(d)}%`);
export const plainPct = (v, d = 1) => (v == null ? '—' : `${Number(v).toFixed(d)}%`);
export const compactInr = (v) => {
  if (v == null) return '—';
  const a = Math.abs(v);
  const sign = v < 0 ? '-' : '';
  if (a >= 1e7) return `${sign}₹${(a / 1e7).toFixed(2)} Cr`;
  if (a >= 1e5) return `${sign}₹${(a / 1e5).toFixed(2)} L`;
  if (a >= 1e3) return `${sign}₹${(a / 1e3).toFixed(1)}K`;
  return `${sign}₹${a.toFixed(0)}`;
};
export const date = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—');
export const dateTime = (v) =>
  v ? new Date(v).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—';
export const ago = (v) => {
  if (!v) return '—';
  const s = (Date.now() - new Date(v).getTime()) / 1000;
  if (s < 0) return `in ${Math.round(-s / 3600)}h`;
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.round(s / 60)}m ago`;
  if (s < 86400) return `${Math.round(s / 3600)}h ago`;
  return `${Math.round(s / 86400)}d ago`;
};
export const initials = (name = '') => name.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
export const pnlClass = (v) => (v > 0 ? 'pos' : v < 0 ? 'neg' : 'muted');
export const label = (s = '') => s.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
