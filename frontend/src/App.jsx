import { useEffect, useRef, useState } from 'react';
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { fetchCurrentPharmacyLogoUrl, fetchCurrentTenantLogoUrl } from './api';
import AiAssistantPanel from './components/AiAssistantPanel';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import SuperAdminLoginPage from './pages/SuperAdminLoginPage';
import InventoryListPage from './pages/InventoryListPage';
import InventoryDetailPage from './pages/InventoryDetailPage';
import BillingPage from './pages/BillingPage';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import SalesAnalyticsPage from './pages/SalesAnalyticsPage';
import TenantManagementPage from './pages/TenantManagementPage';
import UserManagementPage from './pages/UserManagementPage';
import ProfilePage from './pages/ProfilePage';
import PharmacySelectionPage from './pages/PharmacySelectionPage';

const ACCESS = {
  BILLING: ['BILLING'],
  INVENTORY: ['INVENTORY'],
  INVENTORY_VIEW: ['INVENTORY'],  // ADMIN bypasses via hasAnyRole; BILLING/TRANSACTIONS must NOT see this tab
  TRANSACTIONS: ['TRANSACTIONS'],
  ADMIN: ['ADMIN'],
  ANALYTICS: ['ADMIN'],           // backend /sales/summary requires ADMIN only
  AI: ['BILLING', 'INVENTORY', 'TRANSACTIONS', 'ADMIN'],
  SUPER_ADMIN: ['SUPER_ADMIN']
};

export default function App() {
  const { isAuthenticated, session, logout, hasAnyRole, hasFeature, selectPharmacy } = useAuth();
  const location = useLocation();
  const [isAiOpen, setIsAiOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [tenantLogoUrl, setTenantLogoUrl] = useState('');
  const [pharmacyLogoUrl, setPharmacyLogoUrl] = useState('');
  const profileRef = useRef(null);

  const isAdminPortalRoute = location.pathname.startsWith('/admin-portal');
  const isSuperAdmin = hasAnyRole(ACCESS.SUPER_ADMIN);

  const canAccessBilling = hasAnyRole(ACCESS.BILLING) && hasFeature('billingEnabled');
  const canAccessInventory = hasAnyRole(ACCESS.INVENTORY_VIEW) && hasFeature('inventoryEnabled');
  const canAccessTransactions = hasAnyRole(ACCESS.TRANSACTIONS) && hasFeature('transactionsEnabled');
  const canAccessAnalytics = hasAnyRole(ACCESS.ANALYTICS) && hasFeature('analyticsEnabled');
  const canAccessAi = hasAnyRole(ACCESS.AI) && hasFeature('aiAssistantEnabled');
  const needsPharmacySelection = Boolean(isAuthenticated && !isSuperAdmin && session?.requiresPharmacySelection);
  const pharmacyContextKey = String(session?.selectedPharmacyId ?? 'none');

  useEffect(() => {
    let active = true;
    let nextTenantLogoUrl = '';
    let nextPharmacyLogoUrl = '';

    async function loadLogos() {
      if (!isAuthenticated || isSuperAdmin) {
        if (active) {
          setTenantLogoUrl('');
          setPharmacyLogoUrl('');
        }
        return;
      }

      if (session?.tenantHasLogo) {
        try {
          nextTenantLogoUrl = await fetchCurrentTenantLogoUrl();
        } catch (_) {
          nextTenantLogoUrl = '';
        }
      }

      const selectedPharmacy = (session?.availablePharmacies || []).find((item) => item.id === session?.selectedPharmacyId);
      if (selectedPharmacy?.hasLogo && session?.selectedPharmacyId) {
        try {
          nextPharmacyLogoUrl = await fetchCurrentPharmacyLogoUrl();
        } catch (_) {
          nextPharmacyLogoUrl = '';
        }
      }

      if (active) {
        setTenantLogoUrl(nextTenantLogoUrl);
        setPharmacyLogoUrl(nextPharmacyLogoUrl);
      }
    }

    loadLogos();
    return () => {
      active = false;
      if (nextTenantLogoUrl) {
        URL.revokeObjectURL(nextTenantLogoUrl);
      }
      if (nextPharmacyLogoUrl) {
        URL.revokeObjectURL(nextPharmacyLogoUrl);
      }
    };
  }, [isAuthenticated, isSuperAdmin, session?.tenantHasLogo, session?.selectedPharmacyId, session?.availablePharmacies]);

  useEffect(() => {
    if (!isProfileOpen) return;
    function handleOutsideClick(e) {
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setIsProfileOpen(false);
      }
    }
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [isProfileOpen]);

  const defaultPath = !isAuthenticated
    ? '/login'
    : needsPharmacySelection
      ? '/select-pharmacy'
    : isSuperAdmin
      ? '/admin-portal/tenants'
      : canAccessBilling
        ? '/billing'
        : canAccessTransactions
          ? '/transactions'
          : canAccessInventory
            ? '/inventory'
            : hasAnyRole(ACCESS.ADMIN)
              ? '/users'
              : '/login';

  function renderProfileMenu() {
    return (
      <div className="profile-menu-wrap" ref={profileRef}>
        <button
          type="button"
          className="profile-icon-btn"
          onClick={() => setIsProfileOpen((v) => !v)}
          aria-expanded={isProfileOpen}
          aria-label="User profile menu"
        >
          <span className="profile-avatar-badge">{session.username.charAt(0).toUpperCase()}</span>
        </button>
        {isProfileOpen && (
          <div className="profile-dropdown" role="menu">
            <div className="profile-dropdown-header">
              <span className="profile-avatar-badge large">{session.username.charAt(0).toUpperCase()}</span>
              <div>
                <strong>{session.username}</strong>
                {!isSuperAdmin && <small>{session?.selectedPharmacyName || 'No pharmacy selected'}</small>}
              </div>
            </div>
            <div className="profile-dropdown-divider" />
            <NavLink
              to="/profile"
              className="profile-dropdown-item"
              onClick={() => setIsProfileOpen(false)}
              role="menuitem"
            >
              My Profile
            </NavLink>
            <button
              type="button"
              className="profile-dropdown-item ghost danger-action"
              role="menuitem"
              onClick={() => { setIsProfileOpen(false); logout(); }}
            >
              Sign Out
            </button>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={`app-shell ${isAiOpen ? 'ai-open' : ''}`}>
      {isAuthenticated && !isAdminPortalRoute && !isSuperAdmin && (
        <header className="top-nav">
          <div className="brand-lockup">
            {pharmacyLogoUrl || tenantLogoUrl ? (
              <img className="brand-logo" src={pharmacyLogoUrl || tenantLogoUrl} alt="Pharmacy logo" />
            ) : (
              <span className="brand-mark" aria-hidden="true">Rx</span>
            )}
            <div>
              <h1>{session?.selectedPharmacyName || 'Select pharmacy'}</h1>
              <small>{session.username}</small>
            </div>
          </div>
          <nav aria-label="Main navigation">
            {canAccessBilling && <NavLink to="/billing">Billing</NavLink>}
            {canAccessInventory && <NavLink to="/inventory">Inventory</NavLink>}
            {canAccessTransactions && <NavLink to="/transactions">Transactions</NavLink>}
            {canAccessAnalytics && <NavLink to="/sales-analytics">Analytics</NavLink>}
            {hasAnyRole(ACCESS.ADMIN) && <NavLink to="/users">Users</NavLink>}
            <div className="top-nav-right">
              <select
                value={session?.selectedPharmacyId ?? ''}
                onChange={async (event) => {
                  const value = event.target.value;
                  if (!value) return;
                  await selectPharmacy(Number(value));
                }}
                title="Active pharmacy"
              >
                {!session?.selectedPharmacyId && <option value="">Select pharmacy</option>}
                {(session?.availablePharmacies || []).map((pharmacy) => (
                  <option key={pharmacy.id} value={pharmacy.id}>{pharmacy.name}</option>
                ))}
              </select>
              {renderProfileMenu()}
            </div>
          </nav>
        </header>
      )}

      {isAuthenticated && isAdminPortalRoute && isSuperAdmin && (
        <header className="top-nav">
          <div className="brand-lockup">
            <span className="brand-mark" aria-hidden="true">Rx</span>
            <div>
              <h1>Admin Portal</h1>
              <small>Super Admin: {session.username}</small>
            </div>
          </div>
          <nav aria-label="Admin portal navigation">
            <NavLink to="/admin-portal/tenants">Tenants</NavLink>
            <div className="top-nav-right">
              {renderProfileMenu()}
            </div>
          </nav>
        </header>
      )}

      {isAuthenticated && canAccessAi && (
        <AiAssistantPanel isOpen={isAiOpen} onToggle={() => setIsAiOpen((value) => !value)} onClose={() => setIsAiOpen(false)} />
      )}

      <main className={isAuthenticated ? 'page-content' : 'auth-content'}>
        <Routes>
          <Route path="/" element={<Navigate to={defaultPath} replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/super-admin/login" element={<SuperAdminLoginPage />} />
          <Route
            path="/select-pharmacy"
            element={(
              <ProtectedRoute allowWithoutPharmacyContext>
                {isSuperAdmin ? <Navigate to={defaultPath} replace /> : <PharmacySelectionPage />}
              </ProtectedRoute>
            )}
          />

          <Route
            path="/billing"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.BILLING}>
                {canAccessBilling ? <BillingPage key={`billing-${pharmacyContextKey}`} /> : <Navigate to={defaultPath} replace />}
              </ProtectedRoute>
            )}
          />
          <Route
            path="/inventory"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY_VIEW}>
                {canAccessInventory ? <InventoryListPage key={`inventory-${pharmacyContextKey}`} /> : <Navigate to={defaultPath} replace />}
              </ProtectedRoute>
            )}
          />
          <Route
            path="/inventory/:id"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY_VIEW}>
                {canAccessInventory ? <InventoryDetailPage key={`inventory-detail-${pharmacyContextKey}`} /> : <Navigate to={defaultPath} replace />}
              </ProtectedRoute>
            )}
          />
          <Route
            path="/transactions"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.TRANSACTIONS}>
                {canAccessTransactions ? <TransactionHistoryPage key={`transactions-${pharmacyContextKey}`} /> : <Navigate to={defaultPath} replace />}
              </ProtectedRoute>
            )}
          />
          <Route
            path="/sales-analytics"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.ANALYTICS}>
                {canAccessAnalytics ? <SalesAnalyticsPage key={`analytics-${pharmacyContextKey}`} /> : <Navigate to={defaultPath} replace />}
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin-portal/tenants"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.SUPER_ADMIN}>
                <TenantManagementPage />
              </ProtectedRoute>
            )}
          />
          <Route path="/tenants" element={<Navigate to="/admin-portal/tenants" replace />} />
          <Route path="/super-admin/tenants" element={<Navigate to="/admin-portal/tenants" replace />} />
          <Route
            path="/users"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.ADMIN}>
                <UserManagementPage />
              </ProtectedRoute>
            )}
          />
          <Route path="/ai-assistant" element={<Navigate to={defaultPath} replace />} />
          <Route
            path="/profile"
            element={(
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            )}
          />

          <Route path="/sales" element={<Navigate to="/billing" replace />} />
          <Route path="*" element={<Navigate to={defaultPath} replace />} />
        </Routes>
      </main>
    </div>
  );
}
