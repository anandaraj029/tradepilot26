// Shared Recharts styling — CSS variables keep charts in step with the theme.
export const C = {
  teal: 'var(--teal)',
  cyan: 'var(--cyan)',
  purple: 'var(--purple)',
  green: 'var(--green)',
  red: 'var(--red)',
  amber: 'var(--amber)',
  gold: 'var(--gold)',
  muted: 'var(--text-3)',
  grid: 'var(--border-soft)',
};
export const SERIES = [C.teal, C.purple, C.cyan, C.amber, C.green, C.red, C.gold];
export const axis = { stroke: 'var(--text-3)', fontSize: 11, tickLine: false, axisLine: false };
export const DECISION_COLORS = { BUY: C.green, SELL: C.red, HOLD: C.purple, WAIT: C.amber, NO_TRADE: C.muted, EXIT: C.red };
export const MODE_COLORS = { OBSERVE: C.muted, PAPER_TRADING: C.teal, ASSISTED_TRADING: C.cyan, AUTOMATED_TRADING: C.purple };
export const shortDate = (d) => new Date(`${d}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
