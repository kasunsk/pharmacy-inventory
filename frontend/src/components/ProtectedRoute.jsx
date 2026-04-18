import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function ProtectedRoute({ allowedRoles, allowWithoutPharmacyContext = false, children }) {
  const { isAuthenticated, hasAnyRole, session } = useAuth();
  const location = useLocation();
  const isSuperAdminRoute = Array.isArray(allowedRoles) && allowedRoles.includes('SUPER_ADMIN');

  if (!isAuthenticated) {
    return <Navigate to={isSuperAdminRoute ? '/super-admin/login' : '/login'} replace state={{ from: location }} />;
  }

  if (allowedRoles && !hasAnyRole(allowedRoles)) {
    return <Navigate to="/" replace />;
  }

  const requiresPharmacySelection = Boolean(
    !session?.roles?.includes('SUPER_ADMIN')
    && session?.requiresPharmacySelection
    && !allowWithoutPharmacyContext
  );
  if (requiresPharmacySelection) {
    return <Navigate to="/select-pharmacy" replace />;
  }

  return children;
}
