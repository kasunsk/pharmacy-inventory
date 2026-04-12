import { createContext, useContext, useMemo, useState } from 'react';
import { login as loginRequest } from '../api';

const AuthContext = createContext(null);

function readInitialSession() {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const rolesRaw = localStorage.getItem('roles');
  const tenantIdRaw = localStorage.getItem('tenantId');
  const tenantCode = localStorage.getItem('tenantCode');
  const tenantName = localStorage.getItem('tenantName');
  const tenantFeaturesRaw = localStorage.getItem('tenantFeatures');
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

  let tenantFeatures = null;
  try {
    tenantFeatures = tenantFeaturesRaw ? JSON.parse(tenantFeaturesRaw) : null;
  } catch (_) {
    tenantFeatures = null;
  }

  return {
    token,
    username,
    roles,
    tenantId: tenantIdRaw ? Number(tenantIdRaw) : null,
    tenantCode: tenantCode || null,
    tenantName: tenantName || null,
    tenantFeatures
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readInitialSession);

  async function login(username, password) {
    const response = await loginRequest(username, password);
    const next = {
      token: response.token,
      username: response.username,
      roles: response.roles || [],
      tenantId: response.tenantId ?? null,
      tenantCode: response.tenantCode ?? null,
      tenantName: response.tenantName ?? null,
      tenantFeatures: response.tenantId
        ? {
          billingEnabled: Boolean(response.billingEnabled),
          transactionsEnabled: Boolean(response.transactionsEnabled),
          inventoryEnabled: Boolean(response.inventoryEnabled),
          analyticsEnabled: Boolean(response.analyticsEnabled),
          aiAssistantEnabled: Boolean(response.aiAssistantEnabled)
        }
        : null
    };

    localStorage.setItem('token', next.token);
    localStorage.setItem('username', next.username);
    localStorage.setItem('roles', JSON.stringify(next.roles));
    if (next.tenantId !== null) {
      localStorage.setItem('tenantId', String(next.tenantId));
    } else {
      localStorage.removeItem('tenantId');
    }
    if (next.tenantCode) {
      localStorage.setItem('tenantCode', next.tenantCode);
    } else {
      localStorage.removeItem('tenantCode');
    }
    if (next.tenantName) {
      localStorage.setItem('tenantName', next.tenantName);
    } else {
      localStorage.removeItem('tenantName');
    }
    if (next.tenantFeatures) {
      localStorage.setItem('tenantFeatures', JSON.stringify(next.tenantFeatures));
    } else {
      localStorage.removeItem('tenantFeatures');
    }
    setSession(next);
    return next;
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('roles');
    localStorage.removeItem('tenantId');
    localStorage.removeItem('tenantCode');
    localStorage.removeItem('tenantName');
    localStorage.removeItem('tenantFeatures');
    setSession(null);
  }

  function hasAnyRole(requiredRoles) {
    if (!session?.roles?.length) {
      return false;
    }
    const needsSuperAdmin = requiredRoles.includes('SUPER_ADMIN');
    if (needsSuperAdmin) {
      return session.roles.includes('SUPER_ADMIN');
    }
    // Tenant ADMIN bypasses tenant role checks, but never SUPER_ADMIN-only checks.
    if (session.roles.includes('ADMIN')) {
      return true;
    }
    return requiredRoles.some((role) => session.roles.includes(role));
  }

  function hasRole(role) {
    return Boolean(session?.roles?.includes(role));
  }

  function hasFeature(featureName) {
    if (session?.roles?.includes('SUPER_ADMIN')) {
      return false;
    }
    return Boolean(session?.tenantFeatures?.[featureName]);
  }

  const value = useMemo(() => {
    return {
      session,
      isAuthenticated: Boolean(session?.token),
      login,
      logout,
      hasAnyRole,
      hasRole,
      hasFeature
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
