import axios from 'axios';

const TOKEN_KEY = 'taskflow.token';

/* ---------- token storage ---------- */
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

/* ---------- axios instance ---------- */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Attach the JWT to every outgoing request.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// The auth context registers a handler here so an expired/invalid token logs
// the user out. A callback avoids a circular import between client and context.
let unauthorizedHandler = null;
export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler;
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    return Promise.reject(error);
  },
);

/* ---------- error helpers ---------- */

/** Human-readable message from any API error. */
export const getErrorMessage = (error) => {
  const data = error?.response?.data;

  if (data?.fieldErrors?.length) {
    return data.fieldErrors.map((field) => field.message).join(', ');
  }
  if (data?.message) {
    return data.message;
  }
  if (error?.message === 'Network Error') {
    return 'Cannot reach the server. Is the backend running?';
  }
  return error?.message || 'Something went wrong. Please try again.';
};

/** Maps backend field errors to { fieldName: message } for inline display. */
export const getFieldErrors = (error) => {
  const fieldErrors = error?.response?.data?.fieldErrors;
  if (!Array.isArray(fieldErrors)) {
    return {};
  }
  return fieldErrors.reduce((acc, item) => {
    acc[item.field] = item.message;
    return acc;
  }, {});
};

export default api;
