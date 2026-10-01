import { createContext, useContext, useEffect, useState } from 'react';
import { api, getToken, setToken } from './api.js';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }) {
  const [admin, setAdmin] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!getToken()) return setReady(true);
    api('/auth/me')
      .then(setAdmin)
      .catch(() => setToken(null))
      .finally(() => setReady(true));
  }, []);

  const login = async (email, password) => {
    const { token, admin: a } = await api('/auth/login', { method: 'POST', body: { email, password } });
    setToken(token);
    setAdmin(a);
  };
  const logout = () => {
    setToken(null);
    setAdmin(null);
  };
  /** SUPER_ADMIN can do everything; other roles get what the server grants them. */
  const can = (...roles) => admin && (admin.role === 'SUPER_ADMIN' || roles.includes(admin.role));

  return <AuthCtx.Provider value={{ admin, ready, login, logout, can }}>{children}</AuthCtx.Provider>;
}
