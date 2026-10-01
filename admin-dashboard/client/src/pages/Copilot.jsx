import { useState } from 'react';
import { Flag, MessageSquare, Star } from 'lucide-react';
import { api } from '../lib/api.js';
import { useApi, useList } from '../lib/hooks.js';
import { useAuth } from '../lib/auth.jsx';
import { dateTime, label, num } from '../lib/format.js';
import { Badge, Confirm, DataTable, Pager, PageHeader, SearchInput, Select, Stat, UserCell, useAction } from '../components/ui.jsx';

const TOPICS = ['MARKET_ANALYSIS', 'RISK', 'STRATEGY', 'PORTFOLIO', 'EDUCATION', 'OTHER'];

export default function Copilot() {
  const { can } = useAuth();
  const run = useAction();
  const list = useList('copilot-sessions', { topic: 'ALL', flagged: 'ALL', model: 'ALL' }, { limit: 20 });
  const stats = useApi('/stats/overview?days=30');
  const settings = useApi('/settings');
  const [flagging, setFlagging] = useState(null);
  const c = stats.data?.copilot || {};

  const columns = [
    { title: 'Session', render: (s) => <><div className="bold">{s.title}</div><div className="xs dim ellipsis" style={{ maxWidth: 320 }}>{s.lastMessagePreview}</div></> },
    { title: 'Trader', render: (s) => <UserCell user={s.user} /> },
    { title: 'Topic', render: (s) => <Badge tone="cyan">{label(s.topic)}</Badge> },
    { title: 'Model', render: (s) => <span className="mono xs">{s.model}</span> },
    { title: 'Messages', right: true, render: (s) => <span className="mono">{s.messageCount}</span> },
    { title: 'Tokens', right: true, render: (s) => <span className="mono">{num(s.tokensUsed)}</span> },
    { title: 'Rating', render: (s) => <span className="row" style={{ gap: 2 }}>{Array.from({ length: 5 }, (_, i) => <Star key={i} size={12} fill={i < s.rating ? 'var(--gold)' : 'none'} color={i < s.rating ? 'var(--gold)' : 'var(--text-3)'} />)}</span> },
    { title: 'Moderation', render: (s) => (s.flagged ? <span title={s.flagReason}><Badge tone="red">Flagged</Badge></span> : <span className="dim small">Clean</span>) },
    { title: 'Started', render: (s) => <span className="small muted">{dateTime(s.createdAt)}</span> },
    {
      title: '',
      render: (s) => can('SUPPORT', 'RISK_OFFICER') && (
        s.flagged
          ? <button className="btn sm" onClick={() => run(() => api(`/copilot-sessions/${s._id}/flag`, { method: 'POST', body: { flagged: false } }), 'Flag cleared').then(list.reload)}>Clear flag</button>
          : <button className="btn sm icon ghost" title="Flag session" onClick={() => setFlagging(s)}><Flag size={14} /></button>
      ),
    },
  ];

  return (
    <div className="stack" style={{ gap: 20 }}>
      <PageHeader title="AI Copilot" sub="Gemini-powered chat assistant usage, token consumption and conversation moderation." />
      <div className="grid g-4">
        <Stat label="Sessions" icon={MessageSquare} tone="cyan" value={num(c.sessions)} foot={`Model: ${settings.data?.copilot?.model || '—'}`} />
        <Stat label="Messages" value={num(c.messages)} foot={`Daily limit ${settings.data?.copilot?.dailyMessageLimit ?? '—'} / trader`} />
        <Stat label="Tokens consumed" tone="purple" value={num(c.tokens)} foot="Across all sessions" />
        <Stat label="Flagged sessions" icon={Flag} tone="red" value={num(c.flagged)} foot="Need compliance review" />
      </div>
      <div className="card">
        <div className="toolbar">
          <SearchInput value={list.q} onChange={list.setQ} placeholder="Search session title…" />
          <Select value={list.filters.topic} onChange={(v) => list.setFilter('topic', v)} allLabel="All topics" options={TOPICS} />
          <Select value={list.filters.model} onChange={(v) => list.setFilter('model', v)} allLabel="All models" options={[['gemini-2.5-flash', 'gemini-2.5-flash'], ['gemini-2.5-pro', 'gemini-2.5-pro']]} />
          <Select value={list.filters.flagged} onChange={(v) => list.setFilter('flagged', v)} allLabel="Flagged & clean" options={[['true', 'Flagged'], ['false', 'Clean']]} />
        </div>
        <DataTable columns={columns} rows={list.data?.items} loading={list.loading} />
        <Pager data={list.data} page={list.page} setPage={list.setPage} />
      </div>
      {flagging && (
        <Confirm title="Flag Copilot session" withReason confirmLabel="Flag"
          message={`“${flagging.title}” by ${flagging.user?.name} will be marked for compliance review.`}
          onConfirm={(reason) => run(() => api(`/copilot-sessions/${flagging._id}/flag`, { method: 'POST', body: { flagged: true, reason } }), 'Session flagged').then(list.reload)}
          onClose={() => setFlagging(null)} />
      )}
    </div>
  );
}
