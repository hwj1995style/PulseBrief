export type AdminRole = 'VIEWER' | 'EDITOR' | 'ADMIN';

export interface AdminSession {
  token: string;
  expiresAt: string;
  userId: number | null;
  username: string;
  displayName: string;
  role: AdminRole;
  mustChangePassword: boolean;
}

interface ApiResponse<T> {
  code: string;
  message?: string;
  data: T;
}

const SESSION_KEY = 'pulsebrief.admin.session.v1';

export function getStoredAdminSession(): AdminSession | null {
  try {
    const raw = window.sessionStorage.getItem(SESSION_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as AdminSession;
    if (!session.token || !session.expiresAt || new Date(session.expiresAt).getTime() <= Date.now()) {
      clearAdminSession();
      return null;
    }
    return { ...session, mustChangePassword: session.mustChangePassword ?? false };
  } catch {
    clearAdminSession();
    return null;
  }
}

export async function loginAdmin(apiBaseUrl: string, username: string, password: string): Promise<AdminSession> {
  const response = await fetch(`${apiBaseUrl.replace(/\/$/, '')}/api/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const payload = await readPayload<AdminSession>(response);
  if (!response.ok || payload.code !== 'OK') {
    throw new Error(payload.message || '管理员账号或密码错误');
  }
  const session = { ...payload.data, mustChangePassword: payload.data.mustChangePassword ?? false };
  window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export async function changeAdminPassword(
  apiBaseUrl: string,
  token: string,
  currentPassword: string,
  newPassword: string
): Promise<void> {
  const response = await fetch(`${apiBaseUrl.replace(/\/$/, '')}/api/admin/auth/password`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword })
  });
  const payload = await readPayload<boolean>(response);
  if (!response.ok || payload.code !== 'OK') {
    throw new Error(payload.message || '密码修改失败');
  }
  clearAdminSession();
}

export async function logoutAdmin(apiBaseUrl: string, token: string): Promise<void> {
  try {
    await fetch(`${apiBaseUrl.replace(/\/$/, '')}/api/admin/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    });
  } finally {
    clearAdminSession();
  }
}

export function clearAdminSession() {
  window.sessionStorage.removeItem(SESSION_KEY);
}

async function readPayload<T>(response: Response): Promise<ApiResponse<T>> {
  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    return {
      code: 'ERROR',
      message: response.status === 401 ? '管理员账号或密码错误' : `Admin API 请求失败：${response.status}`,
      data: null as T
    };
  }
  return (await response.json()) as ApiResponse<T>;
}
