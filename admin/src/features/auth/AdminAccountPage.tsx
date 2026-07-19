import { RefreshCw, ShieldPlus, UserCog } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { createAdminAccount, listAdminAccounts, resetAdminAccountPassword, updateAdminAccount, type AdminAccount, type AdminAccountUpdateInput } from '../../shared/api/adminAccounts';
import type { AdminRole } from '../../shared/api/adminAuth';

export function AdminAccountPage() {
  const [accounts, setAccounts] = useState<AdminAccount[]>([]);
  const [drafts, setDrafts] = useState<Record<number, AdminAccountUpdateInput>>({});
  const [resetPasswords, setResetPasswords] = useState<Record<number, string>>({});
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState<AdminRole>('VIEWER');
  const [temporaryPassword, setTemporaryPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { void reload(); }, []);
  async function reload() {
    try {
      const result = await listAdminAccounts();
      setAccounts(result);
      setDrafts(Object.fromEntries(result.map((account) => [account.id, { displayName: account.displayName, role: account.role, status: account.status }])));
    } catch (reason) { setError(messageOf(reason)); }
  }
  async function create(event: FormEvent) {
    event.preventDefault(); setError(null);
    try {
      await createAdminAccount({ username, displayName, role, temporaryPassword });
      setUsername(''); setDisplayName(''); setRole('VIEWER'); setTemporaryPassword('');
      setMessage('管理员已创建，首次登录必须修改临时密码'); await reload();
    } catch (reason) { setError(messageOf(reason)); }
  }
  async function save(account: AdminAccount) {
    setError(null);
    try { await updateAdminAccount(account.id, drafts[account.id]); setMessage(`已更新 ${account.username}`); await reload(); }
    catch (reason) { setError(messageOf(reason)); }
  }
  async function resetPassword(account: AdminAccount) {
    setError(null);
    try {
      await resetAdminAccountPassword(account.id, resetPasswords[account.id] ?? '');
      setResetPasswords((current) => ({ ...current, [account.id]: '' }));
      setMessage(`已重置 ${account.username} 的密码，并撤销其全部会话`); await reload();
    } catch (reason) { setError(messageOf(reason)); }
  }
  return <main className="workspace account-workspace">
    <section className="panel account-panel">
      <div className="page-heading"><div><span className="eyebrow">Security</span><h1>管理员账号</h1><p>管理角色、状态、临时密码和登录生命周期。</p></div></div>
      {message ? <p className="success-message" role="status">{message}</p> : null}
      {error ? <p className="form-error" role="alert">{error}</p> : null}
      <div className="account-list">{accounts.map((account) => {
        const draft = drafts[account.id] ?? account;
        return <article className="account-card" key={account.id}>
          <div className="account-card-title"><UserCog size={20}/><div><strong>{account.username}</strong><small>{account.mustChangePassword ? '等待首次改密' : `最近登录：${formatTime(account.lastLoginAt)}`}</small></div><span className={`status-badge ${account.status === 'ACTIVE' ? 'success' : ''}`}>{account.status}</span></div>
          <div className="account-fields">
            <label>显示名称<input aria-label={`${account.username} 显示名称`} value={draft.displayName} onChange={(e) => setDrafts((d) => ({ ...d, [account.id]: { ...draft, displayName: e.target.value } }))}/></label>
            <label>角色<select aria-label={`${account.username} 角色`} value={draft.role} onChange={(e) => setDrafts((d) => ({ ...d, [account.id]: { ...draft, role: e.target.value as AdminRole } }))}><option value="VIEWER">VIEWER</option><option value="EDITOR">EDITOR</option><option value="ADMIN">ADMIN</option></select></label>
            <label>状态<select aria-label={`${account.username} 状态`} value={draft.status} onChange={(e) => setDrafts((d) => ({ ...d, [account.id]: { ...draft, status: e.target.value as 'ACTIVE' | 'DISABLED' } }))}><option value="ACTIVE">ACTIVE</option><option value="DISABLED">DISABLED</option></select></label>
          </div>
          <div className="account-actions"><button className="secondary-button" onClick={() => void save(account)} type="button">保存账号</button><input aria-label={`${account.username} 临时密码`} maxLength={128} minLength={12} placeholder="新的临时密码" type="password" value={resetPasswords[account.id] ?? ''} onChange={(e) => setResetPasswords((p) => ({ ...p, [account.id]: e.target.value }))}/><button className="ghost-button" disabled={(resetPasswords[account.id]?.length ?? 0) < 12} onClick={() => void resetPassword(account)} type="button"><RefreshCw size={15}/>重置密码</button></div>
        </article>;
      })}</div>
    </section>
    <aside className="detail-panel account-create-panel" aria-label="创建管理员">
      <div className="section-title-row"><div><span className="eyebrow">New account</span><h2>创建管理员</h2></div><ShieldPlus size={24}/></div>
      <form className="account-create-form" onSubmit={create}>
        <label>用户名<input aria-label="新管理员用户名" pattern="[a-zA-Z0-9._-]{3,64}" value={username} onChange={(e) => setUsername(e.target.value)} required/></label>
        <label>显示名称<input aria-label="新管理员显示名称" maxLength={128} value={displayName} onChange={(e) => setDisplayName(e.target.value)} required/></label>
        <label>角色<select aria-label="新管理员角色" value={role} onChange={(e) => setRole(e.target.value as AdminRole)}><option value="VIEWER">VIEWER</option><option value="EDITOR">EDITOR</option><option value="ADMIN">ADMIN</option></select></label>
        <label>临时密码<input aria-label="新管理员临时密码" type="password" minLength={12} maxLength={128} value={temporaryPassword} onChange={(e) => setTemporaryPassword(e.target.value)} required/></label>
        <small>临时密码不得包含用户名；首次登录后必须立即修改。</small><button className="primary-button" type="submit">创建管理员</button>
      </form>
    </aside>
  </main>;
}

function messageOf(reason: unknown) { return reason instanceof Error ? reason.message : '操作失败'; }
function formatTime(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '从未登录'; }
