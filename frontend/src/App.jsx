import { useEffect, useRef, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import AiAssistantPanel from './components/AiAssistantPanel';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import InventoryListPage from './pages/InventoryListPage';
import InventoryDetailPage from './pages/InventoryDetailPage';
import BillingPage from './pages/BillingPage';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import SalesAnalyticsPage from './pages/SalesAnalyticsPage';
import UserManagementPage from './pages/UserManagementPage';
import ProfilePage from './pages/ProfilePage';

const ACCESS = {
  BILLING: ['BILLING'],
  INVENTORY: ['INVENTORY'],
  INVENTORY_VIEW: ['INVENTORY', 'BILLING', 'TRANSACTIONS'],
  TRANSACTIONS: ['TRANSACTIONS'],
  ADMIN: ['ADMIN'],
  ANALYTICS: ['ADMIN'],
  AI: ['BILLING', 'INVENTORY', 'TRANSACTIONS', 'ADMIN']
};

export default function App() {
  const { isAuthenticated, session, logout, hasAnyRole } = useAuth();
  const [isAiOpen, setIsAiOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileRef = useRef(null);

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
    : hasAnyRole(ACCESS.BILLING)
      ? '/billing'
      : hasAnyRole(ACCESS.TRANSACTIONS)
        ? '/transactions'
        : hasAnyRole(ACCESS.INVENTORY)
          ? '/inventory'
          : hasAnyRole(ACCESS.ADMIN)
            ? '/users'
            : '/login';

  return (
    <div className={`app-shell ${isAiOpen ? 'ai-open' : ''}`}>
      {isAuthenticated && (
        <header className="top-nav">
          <div className="brand-lockup">
            <span className="brand-mark" aria-hidden="true">Rx</span>
            <div>
              <h1>Pharmacy Management System</h1>
              <small>
                {session.username} <span>{session.roles.join(' / ')}</span>
              </small>
            </div>
          </div>
          <nav aria-label="Main navigation">
            {hasAnyRole(ACCESS.BILLING) && <NavLink to="/billing">Billing</NavLink>}
            {hasAnyRole(ACCESS.INVENTORY_VIEW) && <NavLink to="/inventory">Inventory</NavLink>}
            {hasAnyRole(ACCESS.TRANSACTIONS) && <NavLink to="/transactions">Transactions</NavLink>}
            {hasAnyRole(ACCESS.ANALYTICS) && <NavLink to="/sales-analytics">Analytics</NavLink>}
            {hasAnyRole(ACCESS.ADMIN) && <NavLink to="/users">Users</NavLink>}

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
                      <small>{session.roles.join(' / ')}</small>
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
          </nav>
        </header>
      )}

      {isAuthenticated && hasAnyRole(ACCESS.AI) && (
        <AiAssistantPanel isOpen={isAiOpen} onToggle={() => setIsAiOpen((value) => !value)} onClose={() => setIsAiOpen(false)} />
      )}

      <main className={isAuthenticated ? 'page-content' : 'auth-content'}>
        <Routes>
          <Route path="/" element={<Navigate to={defaultPath} replace />} />
          <Route path="/login" element={<LoginPage />} />

          <Route
            path="/billing"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.BILLING}>
                <BillingPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/inventory"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY_VIEW}>
                <InventoryListPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/inventory/:id"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY_VIEW}>
                <InventoryDetailPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/transactions"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.TRANSACTIONS}>
                <TransactionHistoryPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/sales-analytics"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.ANALYTICS}>
                <SalesAnalyticsPage />
              </ProtectedRoute>
            )}
          />
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
