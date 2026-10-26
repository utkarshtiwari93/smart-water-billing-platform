import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { get, post, setUnauthorizedHandler, tokenStore } from './api.js';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(!!tokenStore.get());

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const loadMe = useCallback(async () => {
    const me = await get('/api/v1/users/me');
    setUser(me);
    return me;
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    if (tokenStore.get()) {
      loadMe().catch(logout).finally(() => setLoading(false));
    }
  }, [logout, loadMe]);

  const acceptToken = useCallback(async (res) => {
    tokenStore.set(res.token);
    return loadMe();
  }, [loadMe]);

  const login = useCallback(async (email, password) => {
    const res = await post('/api/v1/auth/login', { email, password });
    return acceptToken(res);
  }, [acceptToken]);

  const loginWithGoogle = useCallback(async (idToken, apartmentId) => {
    const body = { idToken };
    if (apartmentId) body.apartmentId = Number(apartmentId);
    const res = await post('/api/v1/auth/google', body);
    return acceptToken(res);
  }, [acceptToken]);

  const value = useMemo(
    () => ({ user, loading, login, loginWithGoogle, logout, refresh: loadMe, isAdmin: user?.role === 'ADMIN' }),
    [user, loading, login, loginWithGoogle, logout, loadMe]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
