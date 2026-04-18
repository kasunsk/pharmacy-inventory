import { createContext, useContext, useMemo, useState } from 'react';
import { login as loginRequest, selectPharmacy as selectPharmacyRequest, setDefaultPharmacy as setDefaultPharmacyRequest } from '../api';

const AuthContext = createContext(null);

function readInitialSession() {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const rolesRaw = localStorage.getItem('roles');
  const tenantIdRaw = localStorage.getItem('tenantId');
  const tenantCode = localStorage.getItem('tenantCode');
  const tenantName = localStorage.getItem('tenantName');
  const tenantHasLogoRaw = localStorage.getItem('tenantHasLogo');
  const selectedPharmacyIdRaw = localStorage.getItem('selectedPharmacyId');
  const selectedPharmacyName = localStorage.getItem('selectedPharmacyName');
  const availablePharmaciesRaw = localStorage.getItem('availablePharmacies');
  const requiresPharmacySelectionRaw = localStorage.getItem('requiresPharmacySelection');
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

  let availablePharmacies = [];
  try {
    availablePharmacies = availablePharmaciesRaw ? JSON.parse(availablePharmaciesRaw) : [];
  } catch (_) {
    availablePharmacies = [];
  }

  return {
    token,
    username,
    roles,
    tenantId: tenantIdRaw ? Number(tenantIdRaw) : null,
    tenantCode: tenantCode || null,
    tenantName: tenantName || null,
    tenantHasLogo: tenantHasLogoRaw === 'true',
    selectedPharmacyId: selectedPharmacyIdRaw ? Number(selectedPharmacyIdRaw) : null,
    selectedPharmacyName: selectedPharmacyName || null,
    availablePharmacies,
    requiresPharmacySelection: requiresPharmacySelectionRaw === 'true',
    tenantFeatures
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readInitialSession);

  async function login(username, password) {
    const response = await loginRequest(username, password);
    return applySessionResponse(response);
  }

  function applySessionResponse(response) {
    const next = {
      token: response.token,
      username: response.username,
      roles: response.roles || [],
      tenantId: response.tenantId ?? null,
      tenantCode: response.tenantCode ?? null,
      tenantName: response.tenantName ?? null,
      tenantHasLogo: Boolean(response.tenantHasLogo),
      selectedPharmacyId: response.selectedPharmacyId ?? null,
      selectedPharmacyName: response.selectedPharmacyName ?? null,
      availablePharmacies: response.availablePharmacies || [],
      requiresPharmacySelection: Boolean(response.requiresPharmacySelection),
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
    localStorage.setItem('tenantHasLogo', String(next.tenantHasLogo));
    if (next.tenantFeatures) {
      localStorage.setItem('tenantFeatures', JSON.stringify(next.tenantFeatures));
    } else {
      localStorage.removeItem('tenantFeatures');
    }
    if (next.selectedPharmacyId !== null) {
      localStorage.setItem('selectedPharmacyId', String(next.selectedPharmacyId));
    } else {
      localStorage.removeItem('selectedPharmacyId');
    }
    if (next.selectedPharmacyName) {
      localStorage.setItem('selectedPharmacyName', next.selectedPharmacyName);
    } else {
      localStorage.removeItem('selectedPharmacyName');
    }
    localStorage.setItem('availablePharmacies', JSON.stringify(next.availablePharmacies));
    localStorage.setItem('requiresPharmacySelection', String(next.requiresPharmacySelection));
    setSession(next);
    return next;
  }

  async function selectPharmacy(pharmacyId) {
    const response = await selectPharmacyRequest(pharmacyId);
    return applySessionResponse(response);
  }

  async function setDefaultPharmacy(pharmacyId) {
    const response = await setDefaultPharmacyRequest(pharmacyId);
    return applySessionResponse(response);
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('roles');
    localStorage.removeItem('tenantId');
    localStorage.removeItem('tenantCode');
    localStorage.removeItem('tenantName');
    localStorage.removeItem('tenantHasLogo');
    localStorage.removeItem('tenantFeatures');
    localStorage.removeItem('selectedPharmacyId');
    localStorage.removeItem('selectedPharmacyName');
    localStorage.removeItem('availablePharmacies');
    localStorage.removeItem('requiresPharmacySelection');
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
        hasFeature,
        selectPharmacy,
        setDefaultPharmacy
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
