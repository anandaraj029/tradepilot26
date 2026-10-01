import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Download, Printer } from 'lucide-react';
import { download } from '../lib/api.js';
import { useApi } from '../lib/hooks.js';
import { C, axis } from '../lib/chart.js';
import { dateTime, inr, pnlClass, signedInr } from '../lib/format.js';
import { Card, ChartTip, DataTable, ErrorBox, PageHeader, Spinner, Stat, StatusBadge, UserCell, useAction } from '../components/ui.jsx';

const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export default function DailyReport() {
  const [day, setDay] = useState(today());
  const { data, loading, error } = useApi(`/stats/daily-report?date=${day}`);
  const run = useAction();
  if (error) return <ErrorBox error={error} />;

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Daily Report" sub="End-of-day performance across all traders — the platform-wide version of the app's Daily Summary.">
        <input type="date" className="input" value={day} max={today()} onChange={(e) => setDay(e.target.value)} />
        <button className="btn" onClick={() => window.print()}><Printer size={16} /> Print</button>
        <button className="btn" onClick={() => run(() => download('/export/trades.csv?status=CLOSED', `tradepilot-trades-${day}.csv`), 'CSV exported')}><Download size={16} /> CSV</button>
      </PageHeader>
      {loading && !data ? <Spinner /> : <Report r={data} />}
    </div>
  );
}

function Report({ r }) {
  const s = r.summary;
  const winRate = s.trades ? Math.round((s.wins / s.trades) * 100) : 0;
  const pf = s.grossLoss ? (s.grossProfit / Math.abs(s.grossLoss)).toFixed(2) : '—';
  const b = r.behaviour;
  const wr = (x) => (x.trades ? Math.round((x.wins / x.trades) * 100) : 0);

  return (
    <>
      <div className="grid g-4">
        <Stat label="Net realized P&L" valueClass={pnlClass(s.pnl)} value={signedInr(s.pnl)} foot={`Charges ${inr(s.charges)}`} />
        <Stat label="Trades closed" value={s.trades} foot={`${s.wins} wins · ${s.losses} losses`} />
        <Stat label="Win rate" tone="amber" value={`${winRate}%`} foot={`Profit factor ${pf}`} />
        <Stat label="Best / worst trade" value={<span className="row" style={{ gap: 8, fontSize: 18 }}><span className="pos">{signedInr(s.best)}</span><span className="neg">{signedInr(s.worst)}</span></span>} foot={`Gross ${signedInr(s.grossProfit)} / ${signedInr(s.grossLoss)}`} />
      </div>
      <div className="grid g-21">
        <Card title="P&L by strategy">
          {r.byStrategy.length ? (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={r.byStrategy} layout="vertical" margin={{ left: 10, right: 20 }}>
                <CartesianGrid stroke={C.grid} horizontal={false} />
                <XAxis type="number" tickFormatter={(v) => `₹${v}`} {...axis} />
                <YAxis type="category" dataKey="strategy" {...axis} width={170} />
                <Tooltip content={<ChartTip fmt={(v) => signedInr(v)} />} cursor={{ fill: 'var(--surface-2)' }} />
                <Bar dataKey="pnl" name="P&L" radius={[0, 6, 6, 0]} barSize={16}>
                  {r.byStrategy.map((x) => <Cell key={x.strategy} fill={x.pnl >= 0 ? C.green : C.red} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : <div className="empty">No closed trades on this day</div>}
        </Card>
        <Card title="Behavioural audit & council adherence">
          <div className="stack">
            <div className="row between"><div><div className="small muted">AI Council aligned</div><div className="mono bold pos">{b.aligned.trades} trades</div></div><div style={{ textAlign: 'right' }}><div className="small">{wr(b.aligned)}% win</div><div className={`xs mono ${pnlClass(b.aligned.pnl)}`}>{signedInr(b.aligned.pnl)}</div></div></div>
            <div className="divider" />
            <div className="row between"><div><div className="small muted">User overrides</div><div className="mono bold warn">{b.overrides.trades} trades</div></div><div style={{ textAlign: 'right' }}><div className="small">{wr(b.overrides)}% win</div><div className={`xs mono ${pnlClass(b.overrides.pnl)}`}>{signedInr(b.overrides.pnl)}</div></div></div>
            <div className="divider" />
            {r.byMarket.map((m) => <div key={m.market} className="row between small"><span>{m.market === 'USA' ? 'USA (NYSE/NASDAQ)' : 'India (NSE/BSE)'} · {m.trades}</span><span className={`mono ${pnlClass(m.pnl)}`}>{signedInr(m.pnl)}</span></div>)}
          </div>
        </Card>
      </div>
      <div className="grid g-12">
        <Card title="Top traders" pad={false}>
          <div style={{ marginTop: 8 }}>
            {r.topTraders.map((t, i) => (
              <div key={t._id} className="list-item">
                <span className="mono dim" style={{ width: 18 }}>{i + 1}</span>
                <div className="grow"><UserCell user={{ _id: t._id, name: t.name, email: t.email }} sub={`${t.trades} trades`} /></div>
                <span className={`mono ${pnlClass(t.pnl)}`}>{signedInr(t.pnl)}</span>
              </div>
            ))}
            {!r.topTraders.length && <div className="empty">No activity</div>}
          </div>
        </Card>
        <Card title="Closed trades" sub={`Latest ${r.trades.length}`} pad={false}>
          <DataTable rows={r.trades} columns={[
            { title: 'Time', render: (t) => <span className="small muted">{dateTime(t.closedAt)}</span> },
            { title: 'Trader', render: (t) => <UserCell user={t.user} /> },
            { title: 'Symbol', render: (t) => <span className="bold">{t.symbol}</span> },
            { title: 'Strategy', render: (t) => <span className="small">{t.strategy}</span> },
            { title: 'Exec', render: (t) => <StatusBadge value={t.executionType} /> },
            { title: 'P&L', right: true, render: (t) => <span className={`mono ${pnlClass(t.pnl)}`}>{signedInr(t.pnl)}</span> },
          ]} />
        </Card>
      </div>
    </>
  );
}
