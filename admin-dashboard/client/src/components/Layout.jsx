import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Activity, AreaChart, Banknote, Bot, BookOpen, Briefcase, CandlestickChart, FileText, FlaskConical,
  LayoutDashboard, LogOut, Menu, Moon, Plug, Rocket, ScrollText, Settings, Shield, Sparkles, Sun, Timer, Users,
} from 'lucide-react';
import { useAuth } from '../lib/auth.jsx';
import { useApi } from '../lib/hooks.js';
import { initials, label } from '../lib/format.js';

const NAV = [
  [null, [['/', 'Overview', LayoutDashboard]]],
  ['Operations', [
    ['/traders', 'Traders', Users],
    ['/risk', 'Risk Center', Shield],
    ['/autopilot', 'AutoPilot', Rocket],
    ['/positions', 'Open Positions', Briefcase],
    ['/trades', 'Trades & Journal', BookOpen],
    ['/funds', 'Funds', Banknote, 'pendingFunds'],
  ]],
  ['Intelligence', [
    ['/ai-council', 'AI Council', Sparkles],
    ['/copilot', 'AI Copilot', Bot],
    ['/markets', 'Markets & Scanner', CandlestickChart],
  ]],
  ['Strategy', [
    ['/strategies', 'Strategy Lab', FlaskConical, 'pendingStrategies'],
    ['/backtests', 'Backtests', Timer],
  ]],
  ['Platform', [
    ['/brokers', 'Broker Connections', Plug],
    ['/reports', 'Daily Report', FileText],
    ['/audit', 'Audit Log', ScrollText],
    ['/settings', 'Settings', Settings],
  ]],
];

const getTheme = () => {
  try { return localStorage.getItem('tp_theme') || 'dark'; } catch { return 'dark'; }
};

export default function Layout() {
  const { admin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [theme, setTheme] = useState(getTheme);
  const loc = useLocation();
  const { data } = useApi('/stats/overview?days=7', [loc.pathname]);
  const k = data?.kpis || {};
  const platform = data?.platform || {};

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try { localStorage.setItem('tp_theme', theme); } catch { /* ignore */ }
  }, [theme]);
  useEffect(() => setOpen(false), [loc.pathname]);

  return (
    <div className="app">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-logo"><AreaChart size={18} strokeWidth={2.6} /></div>
          <div>
            <div className="brand-name">AI TradePilot</div>
            <div className="brand-sub">Admin Console</div>
          </div>
        </div>
        {NAV.map(([group, items]) => (
          <div key={group || 'root'}>
            {group && <div className="nav-group">{group}</div>}
            {items.map(([to, text, Icon, countKey]) => (
              <NavLink key={to} to={to} end={to === '/'} className="nav-link">
                <Icon size={17} />
                {text}
                {countKey && k[countKey] > 0 && <span className="count">{k[countKey]}</span>}
              </NavLink>
            ))}
          </div>
        ))}
        <div style={{ marginTop: 'auto', paddingTop: 16 }}>
          <div className="card" style={{ padding: 12, display: 'flex', gap: 10, alignItems: 'center' }}>
            <span className="avatar">{initials(admin?.name)}</span>
            <div className="grow">
              <div className="bold ellipsis">{admin?.name}</div>
              <div className="xs dim">{label(admin?.role || '')}</div>
            </div>
            <button className="btn ghost icon sm" onClick={logout} title="Sign out" aria-label="Sign out"><LogOut size={16} /></button>
          </div>
        </div>
      </aside>
      {open && <div className="overlay" style={{ zIndex: 30 }} onClick={() => setOpen(false)} />}

      <div className="main">
        <header className="topbar">
          <button className="btn ghost icon menu-btn" onClick={() => setOpen(true)} aria-label="Open menu"><Menu size={20} /></button>
          <div className="row grow" style={{ gap: 8, overflow: 'hidden' }}>
            {platform.globalKillSwitch ? (
              <span className="badge red"><span className="dot pulse" /> GLOBAL KILL SWITCH ENGAGED</span>
            ) : (
              <span className="badge green"><span className="dot pulse" /> Trading systems operational</span>
            )}
            {platform.maintenanceMode && <span className="badge amber">Maintenance mode</span>}
            {platform.liveTradingEnabled === false && <span className="badge amber">Live trading disabled</span>}
            {k.killSwitchCount > 0 && <span className="badge red">{k.killSwitchCount} kill switches</span>}
          </div>
          <span className="small muted row" style={{ gap: 6 }}><Activity size={14} /> {k.todayTrades ?? '–'} trades today</span>
          <button className="btn ghost icon" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} aria-label="Toggle theme">
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
