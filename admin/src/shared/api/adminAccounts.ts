import { adminApiConfig } from './adminApi';
import { getStoredAdminSession, type AdminRole } from './adminAuth';

export type AdminUserStatus = 'ACTIVE' | 'DISABLED';

export interface AdminAccount {
  id: number;
  username: string;
  displayName: string;
  role: AdminRole;
  status: AdminUserStatus;
  mustChangePassword: boolean;
  passwordChangedAt: string | null;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface AdminAccountCreateInput {
  username: string;
  displayName: string;
  role: AdminRole;
  temporaryPassword: string;
}

export interface AdminAccountUpdateInput {
  displayName: string;
  role: AdminRole;
  status: AdminUserStatus;
}

const mockAccounts: AdminAccount[] = [
  {
    id: 1,
    username: 'mock-admin',
    displayName: 'Mock Admin',
    role: 'ADMIN',
    status: 'ACTIVE',
    mustChangePassword: false,
    passwordChangedAt: '2026-07-19T09:00:00',
    lastLoginAt: '2026-07-19T15:30:00',
    createdAt: '2026-07-19T09:00:00'
  }
];

export async function listAdminAccounts(): Promise<AdminAccount[]> {
  if (adminApiConfig.mode === 'mock') return mockAccounts.map((account) => ({ ...account }));
  return request<AdminAccount[]>('/api/admin/users');
}

export async function createAdminAccount(input: AdminAccountCreateInput): Promise<AdminAccount> {
  if (adminApiConfig.mode === 'mock') {
    const account: AdminAccount = {
      id: Math.max(...mockAccounts.map((item) => item.id), 0) + 1,
      username: input.username,
      displayName: input.displayName,
      role: input.role,
      status: 'ACTIVE',
      mustChangePassword: true,
      passwordChangedAt: new Date().toISOString(),
      lastLoginAt: null,
      createdAt: new Date().toISOString()
    };
    mockAccounts.push(account);
    return { ...account };
  }
  return request<AdminAccount>('/api/admin/users', 'POST', input);
}

export async function updateAdminAccount(id: number, input: AdminAccountUpdateInput): Promise<AdminAccount> {
  if (adminApiConfig.mode === 'mock') {
    const index = mockAccounts.findIndex((account) => account.id === id);
    mockAccounts[index] = { ...mockAccounts[index], ...input };
    return { ...mockAccounts[index] };
  }
  return request<AdminAccount>(`/api/admin/users/${id}`, 'PUT', input);
}

export async function resetAdminAccountPassword(id: number, temporaryPassword: string): Promise<AdminAccount> {
  if (adminApiConfig.mode === 'mock') {
    const account = mockAccounts.find((item) => item.id === id)!;
    account.mustChangePassword = true;
    account.passwordChangedAt = new Date().toISOString();
    return { ...account };
  }
  return request<AdminAccount>(`/api/admin/users/${id}/password-reset`, 'POST', { temporaryPassword });
}

async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const session = getStoredAdminSession();
  const response = await fetch(`${adminApiConfig.apiBaseUrl!.replace(/\/$/, '')}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${session?.token ?? ''}`,
      ...(body ? { 'Content-Type': 'application/json' } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const payload = (await response.json()) as { code: string; message?: string; data: T };
  if (!response.ok || payload.code !== 'OK') throw new Error(payload.message || `Admin API 请求失败：${response.status}`);
  return payload.data;
}
