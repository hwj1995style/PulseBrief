import { KeyRound } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { changeAdminPassword, type AdminSession } from '../../shared/api/adminAuth';

interface Props {
  apiBaseUrl: string;
  session: AdminSession;
  required?: boolean;
  onComplete: () => void;
  onCancel?: () => void;
}

export function AdminPasswordChangePage({ apiBaseUrl, session, required = false, onComplete, onCancel }: Props) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      setError('两次输入的新密码不一致');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await changeAdminPassword(apiBaseUrl, session.token, currentPassword, newPassword);
      onComplete();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '密码修改失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="admin-login-shell">
      <form className="admin-login-card" onSubmit={submit}>
        <div className="admin-login-heading">
          <KeyRound size={30} />
          <div>
            <h1>{required ? '首次登录必须修改密码' : '修改管理员密码'}</h1>
            <p>修改成功后，所有会话将失效，需要重新登录。</p>
          </div>
        </div>
        <label>当前密码<input aria-label="当前密码" type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required /></label>
        <label>新密码<input aria-label="新密码" type="password" minLength={12} maxLength={128} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required /></label>
        <label>确认新密码<input aria-label="确认新密码" type="password" minLength={12} maxLength={128} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <button className="primary-button" disabled={submitting} type="submit">{submitting ? '正在修改…' : '修改密码并重新登录'}</button>
        {!required && onCancel ? <button className="secondary-button" onClick={onCancel} type="button">取消</button> : null}
      </form>
    </div>
  );
}
