import { changeAdminPassword, clearAdminSession, getStoredAdminSession, loginAdmin, logoutAdmin } from './adminAuth';

describe('adminAuth', () => {
  beforeEach(() => {
    clearAdminSession();
    vi.restoreAllMocks();
  });

  it('stores a successful opaque admin session for the current browser session', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({
        code: 'OK',
        data: {
          token: 'opaque-session-token',
          expiresAt: '2999-12-31T23:59:59',
          userId: 7,
          username: 'editor',
          displayName: 'Editor',
          role: 'EDITOR'
        }
      })
    } as Response);

    const session = await loginAdmin('http://localhost:8080', 'editor', 'secret');

    expect(session.role).toBe('EDITOR');
    expect(getStoredAdminSession()?.token).toBe('opaque-session-token');
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/auth/login',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('revokes and clears the browser session on logout', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({
          code: 'OK',
          data: {
            token: 'opaque-session-token',
            expiresAt: '2999-12-31T23:59:59',
            userId: 1,
            username: 'admin',
            displayName: 'Admin',
            role: 'ADMIN'
          }
        })
      } as Response)
      .mockResolvedValueOnce({ ok: true, status: 200 } as Response);
    await loginAdmin('http://localhost:8080', 'admin', 'secret');

    await logoutAdmin('http://localhost:8080', 'opaque-session-token');

    expect(getStoredAdminSession()).toBeNull();
  });

  it('changes the password and clears every local session', async () => {
    window.sessionStorage.setItem('pulsebrief.admin.session.v1', JSON.stringify({
      token: 'opaque-session-token', expiresAt: '2999-12-31T23:59:59', role: 'ADMIN'
    }));
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ code: 'OK', data: true })
    } as Response);

    await changeAdminPassword('http://localhost:8080', 'opaque-session-token', 'old-password', 'new-password');

    expect(getStoredAdminSession()).toBeNull();
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/admin/auth/password',
      expect.objectContaining({ method: 'POST' })
    );
  });
});
