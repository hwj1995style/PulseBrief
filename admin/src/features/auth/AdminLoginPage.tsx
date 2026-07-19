import { ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { loginAdmin, type AdminSession } from '../../shared/api/adminAuth';

interface AdminLoginPageProps {
  apiBaseUrl: string;
  onLogin: (session: AdminSession) => void;
}

export function AdminLoginPage({ apiBaseUrl, onLogin }: AdminLoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage('');
    try {
      onLogin(await loginAdmin(apiBaseUrl, username, password));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '登录失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="admin-login-shell">
      <form className="admin-login-card" onSubmit={submit}>
        <div className="admin-login-brand">
          <span className="brand-mark">PB</span>
          <div>
            <strong>PulseBrief Admin</strong>
            <small>安全运营控制台</small>
          </div>
        </div>
        <div className="admin-login-heading">
          <ShieldCheck size={28} />
          <div>
            <h1>管理员登录</h1>
            <p>使用已分配的账号登录，操作将记录到审计日志。</p>
          </div>
        </div>
        <label className="field-label">
          管理员账号
          <input
            autoComplete="username"
            autoFocus
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </label>
        <label className="field-label">
          密码
          <input
            autoComplete="current-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {errorMessage ? <p className="inline-error">{errorMessage}</p> : null}
        <button className="primary-action" disabled={submitting} type="submit">
          {submitting ? '登录中...' : '登录'}
        </button>
      </form>
    </main>
  );
}
