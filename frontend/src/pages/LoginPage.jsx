import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('admin@default');
  const [password, setPassword] = useState('admin123');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const redirectPath = location.state?.from?.pathname || '/';

  async function onSubmit(event) {
    event.preventDefault();
    if (!username.includes('@')) {
      setError('Please enter username in format: username@tenant');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const nextSession = await login(username, password);
      const isSuperAdmin = Boolean(nextSession?.roles?.includes('SUPER_ADMIN'));
      const requestedAdminPortal = redirectPath.startsWith('/admin-portal') || redirectPath.startsWith('/super-admin');
      const safeRedirectPath = !isSuperAdmin && requestedAdminPortal ? '/' : redirectPath;
      navigate(safeRedirectPath, { replace: true });
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
        <p className="eyebrow">Sri Lanka pharmacy operations</p>
        <h1>Inventory, billing, and transactions in one workspace.</h1>
        <p>
          Keep counter sales moving, track stock confidently, and review daily performance without switching systems.
        </p>
        <div className="auth-highlights" aria-label="System modules">
          <span>Inventory</span>
          <span>Billing</span>
          <span>Transactions</span>
        </div>
      </div>
      <form className="panel login-panel" onSubmit={onSubmit}>
        <p className="eyebrow">Secure access</p>
        <h2>Sign in</h2>
        <p>Use your staff account to continue.</p>
        <label>
          Username
          <input
            required
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            placeholder="username@tenant"
          />
          <small style={{ color: 'var(--color-text-muted, #888)', marginTop: '4px', display: 'block' }}>
            Format: <strong>username@tenant</strong> &nbsp;(e.g. admin@default)
          </small>
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
