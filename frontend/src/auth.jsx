import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, tokens } from './api';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export const HOME = { FARMER: '/farmer', CUSTOMER: '/customer', ADMIN: '/admin', DELIVERY_PARTNER: '/delivery' };

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => { const t = tokens.get(); return t ? { id: t.userId, role: t.role, fullName: t.fullName } : null; });
  const login = useCallback((t) => { tokens.set(t); setUser({ id: t.userId, role: t.role, fullName: t.fullName }); }, []);
  const logout = useCallback(async () => {
    const t = tokens.get();
    tokens.set(null); setUser(null);
    if (t?.refreshToken) { try { await api.post('/auth/logout', { refreshToken: t.refreshToken }); } catch { /* token already invalid */ } }
  }, []);
  useEffect(() => { const h = () => setUser(null); window.addEventListener('fd:logout', h); return () => window.removeEventListener('fd:logout', h); }, []);
  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
