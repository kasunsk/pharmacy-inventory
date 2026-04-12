import { useEffect, useMemo, useState } from 'react';
import {
  assignUserToTenant,
  createTenant,
  fetchTenants,
  fetchTenantUsers,
  fetchTenantAudits,
  updateTenantConfig,
  updateTenantStatus
} from '../api';

function emptyTenantForm() {
  return {
    code: '',
    name: '',
    adminUsername: '',
    adminPassword: ''
  };
}

function toConfigPayload(tenant) {
  return {
    billingEnabled: Boolean(tenant.billingEnabled),
    transactionsEnabled: Boolean(tenant.transactionsEnabled),
    inventoryEnabled: Boolean(tenant.inventoryEnabled),
    analyticsEnabled: Boolean(tenant.analyticsEnabled),
    aiAssistantEnabled: Boolean(tenant.aiAssistantEnabled)
  };
}

export default function TenantManagementPage() {
  const [tenants, setTenants] = useState([]);
  const [users, setUsers] = useState([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState(emptyTenantForm());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [assignments, setAssignments] = useState({});
  const [editingTenant, setEditingTenant] = useState(null);
  const [configForm, setConfigForm] = useState(null);
  const [audits, setAudits] = useState([]);

  useEffect(() => {
    loadData();
  }, []);

  const visibleTenants = useMemo(() => {
    const text = query.trim().toLowerCase();
    return [...tenants]
      .filter((tenant) => {
        if (!text) return true;
        return tenant.name.toLowerCase().includes(text) || tenant.code.toLowerCase().includes(text);
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [tenants, query]);

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [tenantData, userData, auditData] = await Promise.all([fetchTenants(), fetchTenantUsers(), fetchTenantAudits(80)]);
      setTenants(tenantData);
      setUsers(userData);
      setAudits(auditData);
      setAssignments(Object.fromEntries(userData.map((user) => [user.userId, user.tenantId ?? ''])));
    } catch (e) {
      setError(e.message || 'Failed to load tenant data.');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateTenant(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await createTenant({
        code: form.code.trim().toUpperCase(),
        name: form.name.trim(),
        adminUsername: form.adminUsername.trim(),
        adminPassword: form.adminPassword
      });
      setForm(emptyTenantForm());
      setSuccess('Tenant and tenant admin user created successfully.');
      await loadData();
    } catch (e) {
      setError(e.message || 'Failed to create tenant.');
    } finally {
      setSaving(false);
    }
  }

  async function handleAssign(user) {
    const tenantId = assignments[user.userId];
    if (!tenantId) {
      setError('Select a tenant before assigning the user.');
      return;
    }

    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await assignUserToTenant(user.userId, Number(tenantId));
      setSuccess(`Assigned ${user.username} successfully.`);
      await loadData();
    } catch (e) {
      setError(e.message || 'Failed to assign user to tenant.');
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleStatus(tenant) {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await updateTenantStatus(tenant.id, !tenant.enabled);
      setSuccess(`Tenant ${tenant.code} ${tenant.enabled ? 'disabled' : 'enabled'} successfully.`);
      await loadData();
    } catch (e) {
      setError(e.message || 'Failed to update tenant status.');
    } finally {
      setSaving(false);
    }
  }

  function openConfigModal(tenant) {
    setEditingTenant(tenant);
    setConfigForm(toConfigPayload(tenant));
  }

  async function handleSaveConfig(event) {
    event.preventDefault();
    if (!editingTenant || !configForm) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await updateTenantConfig(editingTenant.id, configForm);
      setSuccess(`Configuration updated for ${editingTenant.code}.`);
      setEditingTenant(null);
      setConfigForm(null);
      await loadData();
    } catch (e) {
      setError(e.message || 'Failed to update tenant configuration.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Admin Portal - Tenant Control Center</h2>
          <p>Manage tenant lifecycle, module configuration, and user assignment.</p>
        </div>
        <button type="button" className="ghost" onClick={loadData} disabled={loading || saving}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {success && <p className="success-banner">{success}</p>}

      <form className="panel" onSubmit={handleCreateTenant}>
        <h3>Create Tenant</h3>
        <div className="form-grid">
          <label>
            Tenant code
            <input
              required
              placeholder="DEFAULT"
              value={form.code}
              onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase() })}
            />
          </label>
          <label>
            Tenant name
            <input
              required
              placeholder="Sunrise Pharmacy"
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </label>
          <label>
            Tenant admin username
            <input
              required
              placeholder="admin"
              value={form.adminUsername}
              onChange={(event) => setForm({ ...form, adminUsername: event.target.value })}
            />
          </label>
          <label>
            Tenant admin password
            <input
              required
              type="password"
              placeholder="Minimum 6 characters"
              value={form.adminPassword}
              onChange={(event) => setForm({ ...form, adminPassword: event.target.value })}
            />
          </label>
        </div>
        <div className="modal-actions">
          <button
            type="submit"
            disabled={saving || !form.code.trim() || !form.name.trim() || !form.adminUsername.trim() || !form.adminPassword.trim()}
          >
            {saving ? 'Saving...' : 'Create Tenant'}
          </button>
        </div>
      </form>

      <div className="panel table-panel">
        <div className="page-title-row">
          <div>
            <h3>Tenants</h3>
            <p>Enable/disable tenants and configure available modules.</p>
          </div>
          <input
            placeholder="Filter by name or code"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            style={{ maxWidth: 260 }}
          />
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Status</th>
                <th>Modules</th>
                <th>AI</th>
                <th>Users</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleTenants.map((tenant) => (
                <tr key={tenant.id}>
                  <td>{tenant.code}</td>
                  <td>{tenant.name}</td>
                  <td>{tenant.enabled ? 'Enabled' : 'Disabled'}</td>
                  <td>
                    {[tenant.billingEnabled && 'Billing', tenant.transactionsEnabled && 'Transactions', tenant.inventoryEnabled && 'Inventory', tenant.analyticsEnabled && 'Analytics']
                      .filter(Boolean)
                      .join(', ') || '-'}
                  </td>
                  <td>{tenant.aiAssistantEnabled ? 'Enabled' : 'Disabled'}</td>
                  <td>{tenant.userCount}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button type="button" className="ghost" onClick={() => openConfigModal(tenant)} disabled={saving}>
                        Configure
                      </button>
                      <button type="button" className="ghost" onClick={() => handleToggleStatus(tenant)} disabled={saving}>
                        {tenant.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {!visibleTenants.length && (
                <tr>
                  <td colSpan="7" className="empty-cell">
                    {loading ? 'Loading tenants...' : 'No tenants found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="panel table-panel">
        <div className="page-title-row">
          <div>
            <h3>User Assignment</h3>
            <p>Assign each staff account to the right tenant.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Status</th>
                <th>Current Tenant</th>
                <th>Assign To</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.userId}>
                  <td>{user.username}</td>
                  <td>{user.enabled ? 'Enabled' : 'Disabled'}</td>
                  <td>{user.tenantName || '-'}</td>
                  <td>
                    <select
                      value={assignments[user.userId] ?? ''}
                      onChange={(event) => setAssignments((prev) => ({ ...prev, [user.userId]: event.target.value }))}
                    >
                      <option value="">Select tenant</option>
                      {visibleTenants.map((tenant) => (
                        <option key={tenant.id} value={tenant.id}>{tenant.name} ({tenant.code})</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="ghost"
                      onClick={() => handleAssign(user)}
                      disabled={saving || !assignments[user.userId]}
                    >
                      Assign
                    </button>
                  </td>
                </tr>
              ))}
              {!users.length && (
                <tr>
                  <td colSpan="5" className="empty-cell">
                    {loading ? 'Loading users...' : 'No users found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {editingTenant && configForm && (
        <div className="panel" style={{ border: '2px solid #b2d8c8' }}>
          <h3>Edit Tenant Configuration: {editingTenant.name}</h3>
          <form onSubmit={handleSaveConfig}>
            <div className="form-grid">
              <label><input type="checkbox" checked={configForm.billingEnabled} onChange={(e) => setConfigForm({ ...configForm, billingEnabled: e.target.checked })} /> Billing</label>
              <label><input type="checkbox" checked={configForm.transactionsEnabled} onChange={(e) => setConfigForm({ ...configForm, transactionsEnabled: e.target.checked })} /> Transactions</label>
              <label><input type="checkbox" checked={configForm.inventoryEnabled} onChange={(e) => setConfigForm({ ...configForm, inventoryEnabled: e.target.checked })} /> Inventory</label>
              <label><input type="checkbox" checked={configForm.analyticsEnabled} onChange={(e) => setConfigForm({ ...configForm, analyticsEnabled: e.target.checked })} /> Analytics</label>
              <label><input type="checkbox" checked={configForm.aiAssistantEnabled} onChange={(e) => setConfigForm({ ...configForm, aiAssistantEnabled: e.target.checked })} /> AI Assistant</label>
            </div>
            <div className="modal-actions">
              <button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save Configuration'}</button>
              <button type="button" className="ghost" onClick={() => { setEditingTenant(null); setConfigForm(null); }} disabled={saving}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div className="panel table-panel">
        <div className="page-title-row">
          <div>
            <h3>Audit Trail</h3>
            <p>Recent tenant administration actions performed by super admins.</p>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Action</th>
                <th>Tenant</th>
                <th>Performed By</th>
              </tr>
            </thead>
            <tbody>
              {audits.map((audit, index) => (
                <tr key={`${audit.tenantId}-${audit.createdAt}-${index}`}>
                  <td>{audit.createdAt ? new Date(audit.createdAt).toLocaleString() : '-'}</td>
                  <td>{audit.action}</td>
                  <td>{audit.tenantName} ({audit.tenantCode})</td>
                  <td>{audit.performedBy}</td>
                </tr>
              ))}
              {!audits.length && (
                <tr>
                  <td colSpan="4" className="empty-cell">
                    {loading ? 'Loading audit logs...' : 'No tenant audit logs yet.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
