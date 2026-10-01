import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AreaChart, Lock, ShieldCheck, Sparkles, Zap } from 'lucide-react';
import { useAuth } from '../lib/auth.jsx';
import { Field } from '../components/ui.jsx';

export default function Login() {
  const { admin, login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  if (admin) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email, password);
      nav('/');
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <div className="login-hero">
        <div className="brand" style={{ padding: 0 }}>
          <div className="brand-logo"><AreaChart size={18} strokeWidth={2.6} /></div>
          <div>
            <div className="brand-name">AI TradePilot</div>
            <div className="brand-sub">Admin Console</div>
          </div>
        </div>
        <div>
          <h1>Oversee every trader, trade and AI decision from one desk.</h1>
          <p style={{ color: '#94a3b8', maxWidth: 480, fontSize: 15 }}>
            Monitor the multi-agent AI Council, enforce deterministic risk limits, manage broker connections and
            intervene instantly with kill switches.
          </p>
          <div className="stack" style={{ marginTop: 28, gap: 12 }}>
            {[
              [ShieldCheck, 'Deterministic risk engine controls & global kill switch'],
              [Sparkles, 'Gemini · Claude · ChatGPT council analytics'],
              [Zap, 'AutoPilot, paper & live execution oversight'],
            ].map(([Icon, t]) => (
              <div key={t} className="row" style={{ color: '#cbd5e1' }}><Icon size={18} color="#00F5D4" /> {t}</div>
            ))}
          </div>
        </div>
        <div className="xs" style={{ color: '#64748b' }}>All admin actions are recorded in the immutable audit trail.</div>
      </div>
      <div className="login-panel">
        <form className="card card-pad login-card stack" onSubmit={submit}>
          <div>
            <div className="stat-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)', width: 40, height: 40, borderRadius: 12 }}><Lock size={18} /></div>
            <h2 style={{ margin: '14px 0 4px' }}>Sign in</h2>
            <p className="muted" style={{ margin: 0 }}>Use your admin credentials.</p>
          </div>
          {error && <div className="banner red">{error}</div>}
          <Field label="Email">
            <input className="input" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </Field>
          <Field label="Password">
            <input className="input" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>
          <button className="btn primary" style={{ height: 42 }} disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
          <p className="xs dim" style={{ margin: 0 }}>Demo seed: admin@tradepilot.ai / Admin@123 (see server/.env.example)</p>
        </form>
      </div>
    </div>
  );
}
