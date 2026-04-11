import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import InventoryListPage from './pages/InventoryListPage';
import InventoryDetailPage from './pages/InventoryDetailPage';
import BillingPage from './pages/BillingPage';
import TransactionHistoryPage from './pages/TransactionHistoryPage';
import SalesAnalyticsPage from './pages/SalesAnalyticsPage';
import UserManagementPage from './pages/UserManagementPage';

const ACCESS = {
  BILLING: ['BILLING'],
  INVENTORY: ['INVENTORY'],
  TRANSACTIONS: ['TRANSACTIONS'],
  ADMIN: ['ADMIN']
};

export default function App() {
  const { isAuthenticated, session, logout, hasAnyRole } = useAuth();
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
    <div className="app-shell">
      {isAuthenticated && (
        <header className="top-nav">
          <div>
            <h1>Pharmacy Management System</h1>
            <small>
              Logged in as <strong>{session.username}</strong> ({session.roles.join(', ')})
            </small>
          </div>
          <nav>
            {hasAnyRole(ACCESS.BILLING) && <NavLink to="/billing">Billing</NavLink>}
            {hasAnyRole(ACCESS.INVENTORY) && <NavLink to="/inventory">Inventory</NavLink>}
            {hasAnyRole(ACCESS.TRANSACTIONS) && <NavLink to="/transactions">Transaction History</NavLink>}
            {hasAnyRole(ACCESS.TRANSACTIONS) && <NavLink to="/sales-analytics">Sales Analytics</NavLink>}
            {hasAnyRole(ACCESS.ADMIN) && <NavLink to="/users">Users</NavLink>}
            <button type="button" className="ghost" onClick={logout}>Logout</button>
          </nav>
        </header>
      )}

      <main>
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
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY}>
                <InventoryListPage />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/inventory/:id"
            element={(
              <ProtectedRoute allowedRoles={ACCESS.INVENTORY}>
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
              <ProtectedRoute allowedRoles={ACCESS.TRANSACTIONS}>
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

          <Route path="/sales" element={<Navigate to="/billing" replace />} />
          <Route path="*" element={<Navigate to={defaultPath} replace />} />
        </Routes>
      </main>
    </div>
  );
}
