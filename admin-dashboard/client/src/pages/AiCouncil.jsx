import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, PolarAngleAxis, PolarGrid, Radar, RadarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Brain, Gauge, ShieldCheck, Sparkles, Timer } from 'lucide-react';
import { useApi, useList } from '../lib/hooks.js';
import { C, DECISION_COLORS, SERIES, axis } from '../lib/chart.js';
import { dateTime, label, num, plainPct } from '../lib/format.js';
import { Badge, Card, ChartTip, DataTable, Modal, Pager, PageHeader, SearchInput, Select, Spinner, Stat, StatusBadge, UserCell } from '../components/ui.jsx';

const DECISIONS = ['BUY', 'SELL', 'HOLD', 'WAIT', 'NO_TRADE'];

export function VoteBar({ d }) {
  const total = d.buyVotes + d.sellVotes + d.holdVotes + d.waitVotes + d.noTradeVotes || 1;
  const parts = [['BUY', d.buyVotes], ['SELL', d.sellVotes], ['HOLD', d.holdVotes], ['WAIT', d.waitVotes], ['NO_TRADE', d.noTradeVotes]];
  return (
    <div className="vote-bar" style={{ width: 90 }} title={parts.map(([k, v]) => `${k}: ${v}`).join(' · ')}>
      {parts.map(([k, v]) => v > 0 && <div key={k} style={{ width: `${(v / total) * 100}%`, background: DECISION_COLORS[k] }} />)}
    </div>
  );
}

export default function AiCouncil() {
  const stats = useApi('/stats/ai-council');
  const list = useList('council-decisions', { consensusDecision: 'ALL', riskApproved: 'ALL', actedOn: 'ALL', market: 'ALL' }, { limit: 15 });
  const [open, setOpen] = useState(null);

  if (stats.loading && !stats.data) return <Spinner />;
  const s = stats.data;
  const t = s.totals;
  const radar = ['avgTech', 'avgFund', 'avgSent', 'avgConfidence', 'avgRisk'].map((m) => ({
    metric: { avgTech: 'Technical', avgFund: 'Fundamental', avgSent: 'Sentiment', avgConfidence: 'Confidence', avgRisk: 'Risk' }[m],
    ...Object.fromEntries(s.agents.map((a) => [a._id, Math.round(a[m])])),
  }));
  const votes = s.agents.map((a) => ({ agent: a._id.split(' ')[0], BUY: a.buy, SELL: a.sell, HOLD: a.hold, WAIT: a.wait, NO_TRADE: a.noTrade }));

  const columns = [
    { title: 'Time', render: (d) => <span className="small muted">{dateTime(d.createdAt)}</span> },
    { title: 'Trader', render: (d) => <UserCell user={d.user} /> },
    { title: 'Asset', render: (d) => <><div className="bold">{d.symbol}</div><div className="xs dim">{d.market}</div></> },
    { title: 'Consensus', render: (d) => <StatusBadge value={d.consensusDecision} /> },
    { title: 'Votes', render: (d) => <VoteBar d={d} /> },
    { title: 'Confidence', right: true, render: (d) => <span className="mono">{d.overallConfidence}%</span> },
    { title: 'Risk', right: true, render: (d) => <span className={`mono ${d.riskScoreAvg > 60 ? 'neg' : d.riskScoreAvg > 45 ? 'warn' : 'pos'}`}>{d.riskScoreAvg}</span> },
    { title: 'Risk engine', render: (d) => (d.riskApproved ? <Badge tone="green">Approved</Badge> : <Badge tone="red">Rejected</Badge>) },
    { title: 'Outcome', render: (d) => <StatusBadge value={d.actedOn} /> },
    { title: 'Source', render: (d) => <span className="xs dim">{d.liveApi ? 'Live API' : 'Local engine'} · {d.latencyMs}ms</span> },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="AI Council" sub="Multi-agent consensus — Gemini (technicals & sentiment), Claude (fundamentals), ChatGPT (portfolio risk/reward) — filtered by the risk engine." />
      <div className="grid g-4">
        <Stat label="Council decisions" icon={Sparkles} tone="purple" value={num(t.count)} foot={`${num(t.liveApi)} via live Gemini API`} />
        <Stat label="Avg. confidence" icon={Gauge} value={plainPct(t.avgConfidence)} foot="Synthesized overall confidence" />
        <Stat label="Risk engine approval" icon={ShieldCheck} tone="green" value={plainPct((t.riskApproved / Math.max(1, t.count)) * 100)} foot={`${num(t.noTrade)} advised NO TRADE / WAIT`} />
        <Stat label="Avg. latency" icon={Timer} tone="cyan" value={`${Math.round(t.avgLatency || 0)} ms`} foot="End-to-end consensus time" />
      </div>

      <div className="grid g-3">
        {s.agents.map((a, i) => (
          <div key={a._id} className="card card-pad stack" style={{ gap: 10 }}>
            <div className="row">
              <span className="stat-icon" style={{ background: 'var(--surface-3)', color: SERIES[i], width: 36, height: 36 }}><Brain size={18} /></span>
              <div className="grow"><div className="bold">{a._id}</div><div className="xs dim">{a.role}</div></div>
            </div>
            <div className="grid g-3" style={{ gap: 8 }}>
              <div><div className="xs dim">Calls</div><div className="mono bold">{num(a.calls)}</div></div>
              <div><div className="xs dim">Avg conf.</div><div className="mono bold">{Math.round(a.avgConfidence)}%</div></div>
              <div><div className="xs dim">Agreement</div><div className="mono bold">{Math.round((a.agreed / a.calls) * 100)}%</div></div>
            </div>
            <div className="vote-bar">
              {DECISIONS.map((d) => { const v = a[{ BUY: 'buy', SELL: 'sell', HOLD: 'hold', WAIT: 'wait', NO_TRADE: 'noTrade' }[d]]; return <div key={d} title={`${d}: ${v}`} style={{ width: `${(v / a.calls) * 100}%`, background: DECISION_COLORS[d] }} />; })}
            </div>
            <div className="xs dim">Agreement = share of votes matching the final council consensus</div>
          </div>
        ))}
      </div>

      <div className="grid g-2">
        <Card title="Agent score profile" sub="Average sub-scores emitted per agent (0–100)">
          <ResponsiveContainer width="100%" height={280}>
            <RadarChart data={radar} outerRadius="72%">
              <PolarGrid stroke={C.grid} />
              <PolarAngleAxis dataKey="metric" tick={{ fill: 'var(--text-2)', fontSize: 11 }} />
              {s.agents.map((a, i) => <Radar key={a._id} name={a._id} dataKey={a._id} stroke={SERIES[i]} fill={SERIES[i]} fillOpacity={0.12} />)}
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Tooltip content={<ChartTip />} />
            </RadarChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Vote distribution" sub="How each agent votes across all evaluations">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={votes} margin={{ left: 0, right: 8 }}>
              <CartesianGrid stroke={C.grid} vertical={false} />
              <XAxis dataKey="agent" {...axis} />
              <YAxis {...axis} width={34} />
              <Tooltip content={<ChartTip fmt={(v) => v} />} cursor={{ fill: 'var(--surface-2)' }} />
              <Legend wrapperStyle={{ fontSize: 11 }} formatter={label} />
              {DECISIONS.map((d) => <Bar key={d} dataKey={d} name={label(d)} stackId="a" fill={DECISION_COLORS[d]} />)}
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Symbol or rationale…" />
          <Select value={list.filters.consensusDecision} onChange={(v) => list.setFilter('consensusDecision', v)} allLabel="All verdicts" options={DECISIONS} />
          <Select value={list.filters.riskApproved} onChange={(v) => list.setFilter('riskApproved', v)} allLabel="Risk engine: any" options={[['true', 'Approved'], ['false', 'Rejected']]} />
          <Select value={list.filters.actedOn} onChange={(v) => list.setFilter('actedOn', v)} allLabel="All outcomes" options={['EXECUTED', 'OVERRIDDEN', 'IGNORED', 'BLOCKED']} />
          <Select value={list.filters.market} onChange={(v) => list.setFilter('market', v)} allLabel="All markets" options={['INDIA', 'USA']} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} onRowClick={setOpen} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {open && <DecisionModal d={open} onClose={() => setOpen(null)} />}
    </div>
  );
}

function DecisionModal({ d, onClose }) {
  return (
    <Modal title={`${d.symbol} · Council verdict`} onClose={onClose} wide>
      <div className="row wrap" style={{ gap: 8 }}>
        <StatusBadge value={d.consensusDecision} />
        <Badge>{d.overallConfidence}% confidence</Badge>
        <Badge>{d.timeHorizon}</Badge>
        {d.riskApproved ? <Badge tone="green">Risk engine approved</Badge> : <Badge tone="red">Risk engine rejected</Badge>}
        <StatusBadge value={d.riskStatus} />
      </div>
      <p className="pre">{d.synthesizedReason}</p>
      {d.rejectionReason && <div className="banner amber small">{d.rejectionReason}</div>}
      <div className="grid g-3">
        {d.agentBreakdowns.map((a) => (
          <div key={a.agentName} className="card card-pad" style={{ boxShadow: 'none' }}>
            <div className="row between"><span className="bold small">{a.agentName}</span><StatusBadge value={a.decision} /></div>
            <div className="xs dim">{a.agentRole}</div>
            <dl className="kv xs" style={{ marginTop: 10 }}>
              <dt>Confidence</dt><dd className="mono">{a.confidence}%</dd>
              <dt>Technical</dt><dd className="mono">{a.technicalScore}</dd>
              <dt>Fundamental</dt><dd className="mono">{a.fundamentalScore}</dd>
              <dt>Sentiment</dt><dd className="mono">{a.sentimentScore}</dd>
              <dt>Risk</dt><dd className="mono">{a.riskScore}</dd>
            </dl>
            <p className="xs muted" style={{ marginBottom: 0 }}>{a.reasoning}</p>
          </div>
        ))}
      </div>
      <div className="grid g-3">
        {[['Key catalysts', d.keyCatalysts], ['Risk factors', d.riskFactors], ['Invalidating conditions', d.invalidatingConditions]].map(([t, items]) => (
          <div key={t}><div className="small bold" style={{ marginBottom: 6 }}>{t}</div><ul className="small muted" style={{ margin: 0, paddingLeft: 18 }}>{items?.map((x) => <li key={x}>{x}</li>)}</ul></div>
        ))}
      </div>
    </Modal>
  );
}
