import { Bell, LogOut, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { HashRouter, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { navigationItems } from './app/navigation';
import { CandidateReviewPage } from './features/candidates/CandidateReviewPage';
import { DigestManagementPage } from './features/digests/DigestManagementPage';
import { IngestionMonitorPage } from './features/ingestion/IngestionMonitorPage';
import { AdminLoginPage } from './features/auth/AdminLoginPage';
import { adminApiConfig } from './shared/api/adminApi';
import { getStoredAdminSession, logoutAdmin, type AdminSession } from './shared/api/adminAuth';
import './styles.css';

function App() {
  const [session, setSession] = useState<AdminSession | null>(() =>
    adminApiConfig.mode === 'mock'
      ? {
          token: 'mock',
          expiresAt: '2999-12-31T23:59:59',
          userId: null,
          username: 'mock-admin',
          displayName: 'Mock Admin',
          role: 'ADMIN'
        }
      : getStoredAdminSession()
  );

  if (adminApiConfig.mode === 'api' && !session) {
    return (
      <HashRouter>
        <AdminLoginPage apiBaseUrl={adminApiConfig.apiBaseUrl!} onLogin={setSession} />
      </HashRouter>
    );
  }

  async function handleLogout() {
    if (adminApiConfig.mode === 'api' && session) {
      await logoutAdmin(adminApiConfig.apiBaseUrl!, session.token);
      setSession(null);
    }
  }

  return (
    <HashRouter>
      <div className="admin-shell">
        <aside className="sidebar">
          <div className="brand">
            <span className="brand-mark">PB</span>
            <div>
              <strong>PulseBrief</strong>
              <small>Admin</small>
            </div>
          </div>

          <nav aria-label="后台导航" className="sidebar-nav">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}
                  key={item.key}
                  to={`/${item.key}`}
                >
                  <Icon size={18} />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </aside>

        <div className="app-region">
          <header className="topbar">
            <div>
              <strong>运营控制台</strong>
              <span>真实资讯采集 · 人工审核 · 用户端发布</span>
            </div>
            <div className="topbar-actions">
              <span className="env-pill">
                <ShieldCheck size={16} />
                {session?.displayName} · {session?.role}
              </span>
              <button className="icon-button" aria-label="通知">
                <Bell size={18} />
              </button>
              {adminApiConfig.mode === 'api' ? (
                <button className="icon-button" aria-label="退出登录" onClick={handleLogout} type="button">
                  <LogOut size={18} />
                </button>
              ) : null}
            </div>
          </header>

          <Routes>
            <Route path="/" element={<Navigate replace to="/candidates" />} />
            <Route path="/candidates" element={<CandidateReviewPage />} />
            <Route path="/dashboard" element={<PlaceholderPage title="仪表盘" />} />
            <Route path="/ingestion" element={<IngestionMonitorPage />} />
            <Route path="/articles" element={<PlaceholderPage title="文章管理" />} />
            <Route path="/categories" element={<PlaceholderPage title="分类管理" />} />
            <Route path="/digests" element={<DigestManagementPage />} />
          </Routes>
        </div>
      </div>
    </HashRouter>
  );
}

function PlaceholderPage({ title }: { title: string }) {
  return (
    <main className="workspace placeholder">
      <section className="empty-panel">
        <h1>{title}</h1>
        <p>该模块将在候选审核闭环稳定后接入。</p>
      </section>
    </main>
  );
}

export default App;
