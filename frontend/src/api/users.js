import api from './client';

export const getProfile = () => api.get('/users/me').then((r) => r.data);

export const updateProfile = (payload) => api.put('/users/me', payload).then((r) => r.data);

export const changePassword = (payload) =>
  api.put('/users/me/password', payload).then((r) => r.data);
