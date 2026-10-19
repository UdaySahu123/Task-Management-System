import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth';
import { clearToken, getToken, setToken, setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an <AuthProvider>');
  }
  return context;
}

/**
 * Holds the authenticated user and the JWT.
 *
 * On first load, if a token is present we call GET /api/auth/me to confirm it
 * is still valid. An invalid/expired token is discarded automatically.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);

  // Let the axios layer notify us when the token stops being accepted.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearToken();
      setUser(null);
    });
  }, []);

  // Validate a stored token once, on startup.
  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      if (!getToken()) {
        if (active) setInitializing(false);
        return;
      }
      try {
        const profile = await authApi.me();
        if (active) setUser(profile);
      } catch {
        clearToken();
      } finally {
        if (active) setInitializing(false);
      }
    };

    bootstrap();
    return () => {
      active = false;
    };
  }, []);

  const value = useMemo(
    () => ({
      user,
      setUser,
      initializing,
      isAuthenticated: Boolean(user),

      login: async (credentials) => {
        const data = await authApi.login(credentials);
        setToken(data.token);
        setUser(data.user);
        return data.user;
      },

      register: async (payload) => {
        const data = await authApi.register(payload);
        setToken(data.token);
        setUser(data.user);
        return data.user;
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch {
          // The local token is cleared regardless of the network result.
        } finally {
          clearToken();
          setUser(null);
        }
      },
    }),
    [user, initializing],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
