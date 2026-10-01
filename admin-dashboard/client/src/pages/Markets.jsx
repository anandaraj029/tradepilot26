import { useState } from 'react';
import { Pencil, Plus, Radar, Star } from 'lucide-react';
import { api } from '../lib/api.js';
import { useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { money, pct, pnlClass } from '../lib/format.js';
import { Badge, DataTable, Field, Modal, Pager, PageHeader, SearchInput, Select, Sparkline, Toggle, useAction } from '../components/ui.jsx';

/** Same heuristic family as AiMarketScanner.evaluateAssetOpportunity in the app. */
export function scanScore(a) {
  let s = 50;
  if (a.rsi >= 42 && a.rsi <= 68) s += 15; else if (a.rsi < 30) s += 8; else if (a.rsi > 72) s -= 15;
  if (a.macd > 0) s += 10; else s -= 6;
  if (a.currentPrice > a.sma20) s += 8;
  if (a.currentPrice > a.sma50) s += 6;
  if (a.currentPrice > a.sma200) s += 5;
  if (a.priceChangePercent > 0.3) s += 6;
  const room = (a.resistance - a.currentPrice) / Math.max(0.01, a.currentPrice - a.support);
  if (room > 1.5) s += 6;
  return Math.max(0, Math.min(100, Math.round(s)));
}
const signal = (sc) => (sc >= 75 ? ['STRONG BUY', 'green'] : sc >= 62 ? ['BUY', 'teal'] : sc >= 45 ? ['WATCH', 'amber'] : ['AVOID', 'red']);

export default function Markets() {
  const { can } = useAuth();
  const list = useList('assets', { market: 'ALL', assetClass: 'ALL', exchange: 'ALL' }, { limit: 25 });
  const [edit, setEdit] = useState(null);
  const canEdit = can('RISK_OFFICER', 'ANALYST');

  const columns = [
    { title: 'Asset', render: (a) => <><div className="bold">{a.symbol}</div><div className="xs dim">{a.name}</div></> },
    { title: 'Exch.', render: (a) => <span className="small">{a.exchange}</span> },
    { title: 'Class', render: (a) => <Badge>{a.assetClass}</Badge> },
    { title: 'Sector', render: (a) => <span className="small muted">{a.sector}</span> },
    { title: 'Price', right: true, render: (a) => <span className="mono">{money(a.currentPrice, a.market)}</span> },
    { title: 'Change', right: true, render: (a) => <span className={`mono ${pnlClass(a.priceChangePercent)}`}>{pct(a.priceChangePercent, 2)}</span> },
    { title: 'Trend', render: (a) => <Sparkline data={a.sparkline} /> },
    { title: 'RSI', right: true, render: (a) => <span className={`mono ${a.rsi > 70 ? 'neg' : a.rsi < 30 ? 'pos' : ''}`}>{a.rsi}</span> },
    { title: 'MACD', right: true, render: (a) => <span className={`mono ${pnlClass(a.macd)}`}>{a.macd}</span> },
    { title: 'S / R', right: true, render: (a) => <span className="mono xs">{a.support} / {a.resistance}</span> },
    { title: 'Scanner', render: (a) => { const sc = scanScore(a); const [l, t] = signal(sc); return <span className="row" style={{ gap: 6 }}><span className="mono bold">{sc}</span><Badge tone={t}>{l}</Badge></span>; } },
    { title: 'Watchers', right: true, render: (a) => <span className="mono small row" style={{ gap: 4, justifyContent: 'flex-end' }}><Star size={12} /> {a.watchlistCount}</span> },
    { title: 'Tradable', render: (a) => (a.isTradable ? <Badge tone="green">Yes</Badge> : <Badge tone="red">Halted</Badge>) },
    { title: '', render: (a) => canEdit && <button className="btn sm icon ghost" onClick={() => setEdit(a)} title="Edit"><Pencil size={14} /></button> },
  ];

  return (
    <>
      <PageHeader title="Markets & Scanner" sub="Instrument catalog across NSE/BSE and NYSE/NASDAQ with technicals and AI scanner scores.">
        {canEdit && <button className="btn primary" onClick={() => setEdit({})}><Plus size={16} /> Add asset</button>}
      </PageHeader>
      <div className="card">
        <div className="toolbar">
          <Radar size={18} color="var(--teal)" />
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Symbol, name, sector…" />
          <Select value={list.filters.market} onChange={(v) => list.setFilter('market', v)} allLabel="All markets" options={[['INDIA', 'India (NSE/BSE)'], ['USA', 'USA (NYSE/NASDAQ)']]} />
          <Select value={list.filters.exchange} onChange={(v) => list.setFilter('exchange', v)} allLabel="All exchanges" options={['NSE', 'BSE', 'NYSE', 'NASDAQ']} />
          <Select value={list.filters.assetClass} onChange={(v) => list.setFilter('assetClass', v)} allLabel="All classes" options={['EQUITY', 'INDEX', 'ETF', 'COMMODITY']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {edit && <AssetModal asset={edit} onClose={() => setEdit(null)} onSaved={list.reload} />}
    </>
  );
}

const NUMS = ['currentPrice', 'priceChangePercent', 'high52', 'low52', 'peRatio', 'rsi', 'macd', 'sma20', 'sma50', 'sma200', 'support', 'resistance'];

function AssetModal({ asset, onClose, onSaved }) {
  const isNew = !asset._id;
  const [f, setF] = useState({ market: 'INDIA', exchange: 'NSE', assetClass: 'EQUITY', isTradable: true, ...asset });
  const run = useAction();
  const set = (k, v) => setF({ ...f, [k]: v });
  const save = async () => {
    const body = { ...f, currency: f.market === 'USA' ? 'USD' : 'INR' };
    NUMS.forEach((k) => { if (body[k] !== undefined && body[k] !== '') body[k] = Number(body[k]); });
    await run(() => api(isNew ? '/assets' : `/assets/${asset._id}`, { method: isNew ? 'POST' : 'PATCH', body }), isNew ? 'Asset added' : 'Asset updated');
    onSaved();
    onClose();
  };
  return (
    <Modal title={isNew ? 'Add asset' : `Edit ${asset.symbol}`} onClose={onClose} wide
      footer={<><button className="btn" onClick={onClose}>Cancel</button><button className="btn primary" onClick={save} disabled={!f.symbol || !f.name}>Save</button></>}>
      <div className="grid g-3">
        <Field label="Symbol"><input className="input" value={f.symbol || ''} disabled={!isNew} onChange={(e) => set('symbol', e.target.value.toUpperCase())} /></Field>
        <Field label="Name"><input className="input" value={f.name || ''} onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="Sector"><input className="input" value={f.sector || ''} onChange={(e) => set('sector', e.target.value)} /></Field>
        <Field label="Market"><Select value={f.market} onChange={(v) => set('market', v)} options={['INDIA', 'USA']} /></Field>
        <Field label="Exchange"><Select value={f.exchange} onChange={(v) => set('exchange', v)} options={['NSE', 'BSE', 'NYSE', 'NASDAQ']} /></Field>
        <Field label="Asset class"><Select value={f.assetClass} onChange={(v) => set('assetClass', v)} options={['EQUITY', 'INDEX', 'ETF', 'COMMODITY']} /></Field>
        {NUMS.map((k) => (
          <Field key={k} label={k.replace(/([A-Z0-9]+)/g, ' $1').replace(/^./, (c) => c.toUpperCase())}>
            <input className="input mono" type="number" step="any" value={f[k] ?? ''} onChange={(e) => set(k, e.target.value)} />
          </Field>
        ))}
      </div>
      <div className="row between"><div><div className="bold">Tradable</div><div className="xs dim">Halted assets are hidden from the scanner and rejected by AutoPilot</div></div><Toggle checked={f.isTradable} onChange={(v) => set('isTradable', v)} /></div>
    </Modal>
  );
}
