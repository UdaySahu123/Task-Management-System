import api from './client';

export const register = (payload) => api.post('/auth/register', payload).then((r) => r.data);

export const login = (payload) => api.post('/auth/login', payload).then((r) => r.data);

/**
 * The backend is stateless, so logging out is really a client-side action
 * (dropping the token). We still call the endpoint for completeness; failures
 * are ignored because the local token is cleared regardless.
 */
export const logout = () =>
  api.post('/auth/logout').then((r) => r.data).catch(() => null);

export const me = () => api.get('/auth/me').then((r) => r.data);
