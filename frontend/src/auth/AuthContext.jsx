import { createContext, useContext, useMemo, useState } from 'react';
import { login as loginRequest } from '../api';

const AuthContext = createContext(null);

function readInitialSession() {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const rolesRaw = localStorage.getItem('roles');
  if (!token || !username || !rolesRaw) {
    return null;
  }

  let roles = [];
  try {
    roles = JSON.parse(rolesRaw);
  } catch (_) {
    roles = [];
  }

  if (!Array.isArray(roles) || roles.length === 0) {
    return null;
  }

  return { token, username, roles };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readInitialSession);

  async function login(username, password) {
    const response = await loginRequest(username, password);
    const next = {
      token: response.token,
      username: response.username,
      roles: response.roles || []
    };

    localStorage.setItem('token', next.token);
    localStorage.setItem('username', next.username);
    localStorage.setItem('roles', JSON.stringify(next.roles));
    setSession(next);
    return next;
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('roles');
    setSession(null);
  }

  function hasAnyRole(requiredRoles) {
    if (!session?.roles?.length) {
      return false;
    }
    if (session.roles.includes('ADMIN')) {
      return true;
    }
    return requiredRoles.some((role) => session.roles.includes(role));
  }

  const value = useMemo(() => {
    return {
      session,
      isAuthenticated: Boolean(session?.token),
      login,
      logout,
      hasAnyRole
    };
  }, [session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}
