import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import {
  AlertTriangle, Banknote, Briefcase, Gauge, Lock, Rocket, Sparkles, Target, TrendingUp, Users, Wallet,
} from 'lucide-react';
import { useApi } from '../lib/hooks.js';
import { C, DECISION_COLORS, MODE_COLORS, axis, shortDate } from '../lib/chart.js';
import { compactInr, dateTime, label, num, plainPct, pnlClass, signedInr } from '../lib/format.js';
import { Card, ChartTip, ErrorBox, PageHeader, Seg, Spinner, Stat, StatusBadge } from '../components/ui.jsx';

export default function Overview() {
  const [days, setDays] = useState('30');
  const { data, loading, error } = useApi(`/stats/overview?days=${days}`);
  if (error) return <ErrorBox error={error} />;
  if (loading && !data) return <Spinner />;
  const k = data.kpis;
  const totalModes = data.modeBreakdown.reduce((a, m) => a + m.count, 0);

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="Platform overview" sub="Real-time health of traders, capital, AI decisions and risk controls.">
        <Seg value={days} onChange={setDays} options={[['7', '7D'], ['30', '30D'], ['90', '90D']]} />
      </PageHeader>

      {data.platform.globalKillSwitch && (
        <div className="banner red"><AlertTriangle size={18} /> Global kill switch is engaged — all trading is halted across the platform. <Link to="/risk" style={{ marginLeft: 'auto', color: 'inherit', textDecoration: 'underline' }}>Manage</Link></div>
      )}

      <div className="grid g-4">
        <Stat label="Traders" icon={Users} value={num(k.totalUsers)} foot={`${k.activeUsers} active · ${k.newUsers} new in ${days}d · ${k.suspendedUsers} suspended`} />
        <Stat label="Capital under management" icon={Wallet} tone="purple" value={compactInr(k.totalCapital)} foot={`${compactInr(k.totalInvested)} deployed · ${compactInr(k.totalCash)} cash`} />
        <Stat label="Realized P&L today" icon={TrendingUp} tone={k.realizedToday >= 0 ? 'green' : 'red'} valueClass={pnlClass(k.realizedToday)} value={signedInr(k.realizedToday)} foot={`Unrealized ${signedInr(k.unrealized)} · ${k.todayTrades} trades`} />
        <Stat label="Lifetime win rate" icon={Target} tone="amber" value={plainPct(k.winRate)} foot={`${num(k.closedTrades)} closed · ${signedInr(k.totalPnl)} net`} />
      </div>

      <div className="grid g-21">
        <Card title="Platform P&L" sub="Daily realized P&L split by paper and live execution, with cumulative curve">
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={data.series} margin={{ left: 0, right: 8, top: 10 }}>
              <defs>
                <linearGradient id="cum" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--teal)" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="var(--teal)" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke={C.grid} vertical={false} />
              <XAxis dataKey="date" tickFormatter={shortDate} {...axis} minTickGap={24} />
              <YAxis tickFormatter={compactInr} {...axis} width={64} />
              <Tooltip content={<ChartTip fmt={(v) => signedInr(v)} />} labelFormatter={shortDate} />
              <Area type="monotone" dataKey="cumulative" name="Cumulative" stroke={C.teal} strokeWidth={2} fill="url(#cum)" />
              <Area type="monotone" dataKey="live" name="Live" stroke={C.purple} strokeWidth={1.5} fill="transparent" />
              <Area type="monotone" dataKey="paper" name="Paper" stroke={C.cyan} strokeWidth={1.5} fill="transparent" strokeDasharray="4 3" />
            </AreaChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Trading modes" sub="Distribution of traders by execution mode">
          <div style={{ position: 'relative' }}>
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie data={data.modeBreakdown} dataKey="count" nameKey="mode" innerRadius={56} outerRadius={80} paddingAngle={2} stroke="none">
                  {data.modeBreakdown.map((m) => <Cell key={m.mode} fill={MODE_COLORS[m.mode]} />)}
                </Pie>
                <Tooltip content={<ChartTip />} />
              </PieChart>
            </ResponsiveContainer>
            <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', pointerEvents: 'none' }}>
              <div style={{ textAlign: 'center' }}><div className="stat-value">{totalModes}</div><div className="xs dim">traders</div></div>
            </div>
          </div>
          <div className="stack" style={{ gap: 8, marginTop: 8 }}>
            {data.modeBreakdown.map((m) => (
              <div key={m.mode} className="row small">
                <span className="dot" style={{ color: MODE_COLORS[m.mode] }} />
                <span className="grow">{label(m.mode)}</span>
                <span className="mono">{m.count}</span>
                <span className="dim mono" style={{ width: 44, textAlign: 'right' }}>{Math.round((m.count / totalModes) * 100)}%</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className="grid g-4">
        <Stat label="Open positions" icon={Briefcase} tone="cyan" value={num(k.openPositions)} foot={`${k.liveTrades} live trades lifetime`} />
        <Stat label="AutoPilot engaged" icon={Rocket} tone="purple" value={num(k.autoPilotOn)} foot={`${k.targetsHit} hit target · ${k.lossLimitsHit} hit loss limit today`} />
        <Stat label="Locked / kill switch" icon={Lock} tone="red" value={`${k.lockedCount} / ${k.killSwitchCount}`} foot="Portfolios currently blocked from trading" />
        <Stat label="Pending fund reviews" icon={Banknote} tone="amber" value={num(k.pendingFunds)} foot={`${compactInr(k.deposits)} in · ${compactInr(k.withdrawals)} out`} />
      </div>

      <div className="grid g-3">
        <Card title="AI Council verdicts" sub={`Avg. trade confidence ${Math.round(k.avgConfidence)}%`} action={<Link to="/ai-council" className="small">Details →</Link>}>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data.decisionBreakdown} layout="vertical" margin={{ left: 10, right: 16 }}>
              <XAxis type="number" hide />
              <YAxis type="category" dataKey="decision" tickFormatter={label} {...axis} width={70} />
              <Tooltip content={<ChartTip />} cursor={{ fill: 'var(--surface-2)' }} />
              <Bar dataKey="count" name="Decisions" radius={[0, 6, 6, 0]} barSize={16}>
                {data.decisionBreakdown.map((d) => <Cell key={d.decision} fill={DECISION_COLORS[d.decision]} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Strategy performance" sub="Net realized P&L by strategy" action={<Link to="/strategies" className="small">Strategy Lab →</Link>}>
          <div className="stack" style={{ gap: 12 }}>
            {data.strategyPerf.map((s) => (
              <div key={s.strategy}>
                <div className="row small between">
                  <span className="ellipsis" style={{ fontWeight: 600 }}>{s.strategy}</span>
                  <span className={`mono ${pnlClass(s.pnl)}`}>{signedInr(s.pnl)}</span>
                </div>
                <div className="row xs dim between"><span>{s.trades} trades</span><span>{s.winRate}% win</span></div>
                <div className="progress" style={{ height: 5, marginTop: 4 }}><div style={{ width: `${s.winRate}%`, background: s.pnl >= 0 ? C.green : C.red }} /></div>
              </div>
            ))}
          </div>
        </Card>
        <Card title="Risk alerts" sub="Latest warnings & critical events" pad={false} action={<Link to="/audit" className="small">Audit log →</Link>}>
          <div style={{ marginTop: 8 }}>
            {data.recentAlerts.map((a) => (
              <div key={a._id} className="list-item">
                <AlertTriangle size={16} color={a.severity === 'CRITICAL' ? 'var(--red)' : 'var(--amber)'} />
                <div className="grow">
                  <div className="small bold ellipsis">{label(a.action)}</div>
                  <div className="xs dim ellipsis">{a.user?.name || a.actorName} · {a.symbol} · {dateTime(a.createdAt)}</div>
                </div>
                <StatusBadge value={a.severity} />
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className="grid g-2">
        <Card title="Trading activity" sub="Closed trades per day and win rate">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={data.series} margin={{ left: 0, right: 8 }}>
              <CartesianGrid stroke={C.grid} vertical={false} />
              <XAxis dataKey="date" tickFormatter={shortDate} {...axis} minTickGap={24} />
              <YAxis {...axis} width={34} />
              <Tooltip content={<ChartTip />} labelFormatter={shortDate} cursor={{ fill: 'var(--surface-2)' }} />
              <Bar dataKey="trades" name="Trades" fill={C.teal} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Engagement" sub={`AI Copilot: ${num(data.copilot.sessions)} sessions · ${num(data.copilot.messages)} messages · ${data.copilot.flagged} flagged`}>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={data.series} margin={{ left: 0, right: 8 }}>
              <CartesianGrid stroke={C.grid} vertical={false} />
              <XAxis dataKey="date" tickFormatter={shortDate} {...axis} minTickGap={24} />
              <YAxis {...axis} width={34} allowDecimals={false} />
              <Tooltip content={<ChartTip />} labelFormatter={shortDate} />
              <Area type="step" dataKey="signups" name="New traders" stroke={C.purple} fill="var(--purple-soft)" />
            </AreaChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid g-3">
        <Card title="Quick links" pad>
          <div className="stack" style={{ gap: 8 }}>
            <Link className="btn" to="/funds?status=PENDING"><Banknote size={16} /> Review {k.pendingFunds} pending fund requests</Link>
            <Link className="btn" to="/strategies"><Sparkles size={16} /> Review {k.pendingStrategies} strategies awaiting live approval</Link>
            <Link className="btn" to="/risk"><Gauge size={16} /> Open risk center</Link>
          </div>
        </Card>
        <Card title="Behavioural adherence" sub="Trades where users overrode the AI Council">
          <div className="stat-value">{plainPct((k.overrides / Math.max(1, k.closedTrades)) * 100)}</div>
          <div className="small muted">{num(k.overrides)} override trades of {num(k.closedTrades)}</div>
        </Card>
        <Card title="Broker connections" sub="Live routing pipeline status">
          <div className="stack" style={{ gap: 8 }}>
            {data.brokerBreakdown.map((b) => (
              <div key={b.status} className="row small">
                <StatusBadge value={b.status} dot />
                <span className="grow" />
                <span className="mono">{b.count}</span>
                <span className="dim mono" style={{ width: 80, textAlign: 'right' }}>{compactInr(b.margin)}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
