import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function SuperAdminLoginPage() {
  const { login, isAuthenticated, hasAnyRole } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('super_admin');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated) {
    // If already logged in as SUPER_ADMIN, go to tenants; otherwise to root
    return <Navigate to={hasAnyRole(['SUPER_ADMIN']) ? '/admin-portal/tenants' : '/'} replace />;
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await login(username.trim(), password);
      navigate('/admin-portal/tenants', { replace: true });
    } catch (e) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-shell">
      <div className="auth-intro">
        <span className="brand-mark large" aria-hidden="true">Rx</span>
        <p className="eyebrow">Global administration</p>
        <h1>Super Admin Portal</h1>
        <p>
          Manage pharmacy tenants, create organizations, and assign users across the entire deployment.
        </p>
        <div className="auth-highlights" aria-label="Admin modules">
          <span>Tenant Management</span>
          <span>User Assignment</span>
          <span>Global Access</span>
        </div>
      </div>
      <form className="panel login-panel" onSubmit={onSubmit}>
        <p className="eyebrow">Restricted access</p>
        <h2>Super Admin Sign in</h2>
        <p>This portal is for system administrators only.</p>
        <label>
          Username
          <input
            required
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            placeholder="super_admin"
          />
        </label>
        <label>
          Password
          <input
            required
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </section>
  );
}

