import { useEffect, useMemo, useState } from 'react';
import {
  createTenantPharmacy,
  createTenant,
  fetchPharmacyLogoUrl,
  fetchTenantLogoUrl,
  fetchTenantPharmacies,
  fetchTenants,
  fetchTenantAudits,
  updateTenantPharmacyStatus,
  updateTenantConfig,
  updateTenantStatus,
  uploadPharmacyLogo,
  uploadTenantLogo
} from '../api';

function emptyTenantForm() {
  return {
    code: '',
    name: '',
    adminUsername: '',
    adminFirstName: '',
    adminLastName: '',
    adminEmail: '',
    adminPassword: '',
    adminGender: ''
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

function emptyPharmacyForm() {
  return {
    name: ''
  };
}

function GearIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="2" />
      <path d="M12 6V3M12 21v-3M18 12h3M3 12h3M17.66 6.34l2.12-2.12M4.22 19.78l2.12-2.12M17.66 17.66l2.12 2.12M4.22 4.22l2.12 2.12" />
    </svg>
  );
}

function ToggleIcon({ enabled }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      {enabled ? (
        <>
          <rect x="1" y="5" width="22" height="14" rx="7" ry="7" fill="none" stroke="currentColor" strokeWidth="2" />
          <circle cx="18" cy="12" r="5" />
        </>
      ) : (
        <>
          <rect x="1" y="5" width="22" height="14" rx="7" ry="7" fill="none" stroke="currentColor" strokeWidth="2" />
          <circle cx="6" cy="12" r="5" />
        </>
      )}
    </svg>
  );
}

function StoreIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 7h18l-1.5 4.5H4.5z" />
      <path d="M5 11v8h14v-8" />
      <path d="M9 19v-5h6v5" />
    </svg>
  );
}

export default function TenantManagementPage() {
  const [tenants, setTenants] = useState([]);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState(emptyTenantForm());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [editingTenant, setEditingTenant] = useState(null);
  const [configForm, setConfigForm] = useState(null);
  const [audits, setAudits] = useState([]);
  const [pharmacyTenant, setPharmacyTenant] = useState(null);
  const [pharmacyForm, setPharmacyForm] = useState(emptyPharmacyForm());
  const [tenantPharmacies, setTenantPharmacies] = useState([]);
  const [createTenantLogoFile, setCreateTenantLogoFile] = useState(null);
  const [tenantLogoPreview, setTenantLogoPreview] = useState('');
  const [pharmacyLogoPreviews, setPharmacyLogoPreviews] = useState({});

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    return () => {
      if (tenantLogoPreview) {
        URL.revokeObjectURL(tenantLogoPreview);
      }
      Object.values(pharmacyLogoPreviews).forEach((value) => {
        if (value) {
          URL.revokeObjectURL(value);
        }
      });
    };
  }, [tenantLogoPreview, pharmacyLogoPreviews]);

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
      const [tenantData, auditData] = await Promise.all([fetchTenants(), fetchTenantAudits(80)]);
      setTenants(tenantData);
      setAudits(auditData);
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
      const createdTenant = await createTenant({
        code: form.code.trim().toUpperCase(),
        name: form.name.trim(),
        adminUsername: form.adminUsername.trim(),
        adminFirstName: form.adminFirstName.trim(),
        adminLastName: form.adminLastName.trim(),
        adminEmail: form.adminEmail.trim(),
        adminPassword: form.adminPassword,
        adminGender: form.adminGender
      });

      if (createTenantLogoFile && createdTenant?.id) {
        await uploadTenantLogo(createdTenant.id, createTenantLogoFile);
      }

      setForm(emptyTenantForm());
      setCreateTenantLogoFile(null);
      setSuccess(createTenantLogoFile
        ? 'Tenant, admin user, and tenant logo created successfully.'
        : 'Tenant and tenant admin user created successfully.');
      await loadData();
    } catch (e) {
      setError(e.message || 'Failed to create tenant.');
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

  async function openPharmacyModal(tenant) {
    setPharmacyTenant(tenant);
    setPharmacyForm(emptyPharmacyForm());
    setTenantPharmacies([]);
    setPharmacyLogoPreviews({});
    setTenantLogoPreview('');
    setLoading(true);
    setError('');
    try {
      const pharmacies = await fetchTenantPharmacies(tenant.id, false);
      setTenantPharmacies(pharmacies);

      if (tenant.hasLogo) {
        try {
          const tenantLogo = await fetchTenantLogoUrl(tenant.id);
          setTenantLogoPreview(tenantLogo);
        } catch (_) {
          setTenantLogoPreview('');
        }
      }

      const previews = {};
      for (const pharmacy of pharmacies) {
        if (!pharmacy.hasLogo) {
          continue;
        }
        try {
          previews[pharmacy.id] = await fetchPharmacyLogoUrl(tenant.id, pharmacy.id);
        } catch (_) {
          previews[pharmacy.id] = '';
        }
      }
      setPharmacyLogoPreviews(previews);
    } catch (e) {
      setError(e.message || 'Failed to load tenant pharmacies.');
    } finally {
      setLoading(false);
    }
  }

  function closePharmacyModal() {
    setPharmacyTenant(null);
    setPharmacyForm(emptyPharmacyForm());
    setTenantPharmacies([]);
    if (tenantLogoPreview) {
      URL.revokeObjectURL(tenantLogoPreview);
    }
    Object.values(pharmacyLogoPreviews).forEach((value) => {
      if (value) {
        URL.revokeObjectURL(value);
      }
    });
    setTenantLogoPreview('');
    setPharmacyLogoPreviews({});
  }

  async function refreshPharmacies(tenantId) {
    const pharmacies = await fetchTenantPharmacies(tenantId, false);
    setTenantPharmacies(pharmacies);
    return pharmacies;
  }

  async function handleCreatePharmacy(event) {
    event.preventDefault();
    if (!pharmacyTenant) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await createTenantPharmacy(pharmacyTenant.id, {
        name: pharmacyForm.name.trim()
      });
      setPharmacyForm(emptyPharmacyForm());
      await refreshPharmacies(pharmacyTenant.id);
      await loadData();
      setSuccess('Pharmacy created successfully.');
    } catch (e) {
      setError(e.message || 'Failed to create pharmacy.');
    } finally {
      setSaving(false);
    }
  }

  async function handleTogglePharmacyStatus(pharmacy) {
    if (!pharmacyTenant) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await updateTenantPharmacyStatus(pharmacyTenant.id, pharmacy.id, !pharmacy.enabled);
      await refreshPharmacies(pharmacyTenant.id);
      setSuccess(`Pharmacy ${pharmacy.code} ${pharmacy.enabled ? 'disabled' : 'enabled'} successfully.`);
    } catch (e) {
      setError(e.message || 'Failed to update pharmacy status.');
    } finally {
      setSaving(false);
    }
  }

  async function handleUploadTenantLogo(event) {
    if (!pharmacyTenant) return;
    const file = event.target.files?.[0];
    if (!file) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await uploadTenantLogo(pharmacyTenant.id, file);
      if (tenantLogoPreview) {
        URL.revokeObjectURL(tenantLogoPreview);
      }
      const next = await fetchTenantLogoUrl(pharmacyTenant.id);
      setTenantLogoPreview(next);
      await loadData();
      setSuccess('Tenant logo uploaded successfully.');
    } catch (e) {
      setError(e.message || 'Failed to upload tenant logo.');
    } finally {
      setSaving(false);
      event.target.value = '';
    }
  }

  async function handleUploadPharmacyLogo(pharmacy, event) {
    if (!pharmacyTenant) return;
    const file = event.target.files?.[0];
    if (!file) return;
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await uploadPharmacyLogo(pharmacyTenant.id, pharmacy.id, file);
      const existing = pharmacyLogoPreviews[pharmacy.id];
      if (existing) {
        URL.revokeObjectURL(existing);
      }
      const logoUrl = await fetchPharmacyLogoUrl(pharmacyTenant.id, pharmacy.id);
      setPharmacyLogoPreviews((prev) => ({ ...prev, [pharmacy.id]: logoUrl }));
      await refreshPharmacies(pharmacyTenant.id);
      setSuccess(`Logo updated for ${pharmacy.name}.`);
    } catch (e) {
      setError(e.message || 'Failed to upload pharmacy logo.');
    } finally {
      setSaving(false);
      event.target.value = '';
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
            Tenant admin first name
            <input
              required
              placeholder="Nimal"
              value={form.adminFirstName}
              onChange={(event) => setForm({ ...form, adminFirstName: event.target.value })}
            />
          </label>
          <label>
            Tenant admin last name
            <input
              required
              placeholder="Perera"
              value={form.adminLastName}
              onChange={(event) => setForm({ ...form, adminLastName: event.target.value })}
            />
          </label>
          <label>
            Tenant admin email
            <input
              required
              type="email"
              placeholder="admin@example.com"
              value={form.adminEmail}
              onChange={(event) => setForm({ ...form, adminEmail: event.target.value })}
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
          <label>
            Tenant admin gender
            <select
              required
              value={form.adminGender}
              onChange={(event) => setForm({ ...form, adminGender: event.target.value })}
            >
              <option value="">Select gender</option>
              <option value="MALE">MALE</option>
              <option value="FEMALE">FEMALE</option>
            </select>
          </label>
          <label>
            Tenant logo (optional)
            <input
              type="file"
              accept="image/*"
              onChange={(event) => setCreateTenantLogoFile(event.target.files?.[0] || null)}
            />
          </label>
        </div>
        <div className="modal-actions">
          <button
            type="submit"
            disabled={saving
              || !form.code.trim()
              || !form.name.trim()
              || !form.adminUsername.trim()
              || !form.adminFirstName.trim()
              || !form.adminLastName.trim()
              || !form.adminEmail.trim()
              || !form.adminPassword.trim()
              || !form.adminGender}
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
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <button
                        type="button"
                        className="icon-btn ghost"
                        onClick={() => openConfigModal(tenant)}
                        disabled={saving}
                        title="Configure Tenant"
                        aria-label={`Configure ${tenant.name}`}
                      >
                        <GearIcon />
                      </button>
                      <button
                        type="button"
                        className="icon-btn ghost"
                        onClick={() => openPharmacyModal(tenant)}
                        disabled={saving}
                        title="Manage Pharmacies"
                        aria-label={`Manage pharmacies for ${tenant.name}`}
                      >
                        <StoreIcon />
                      </button>
                      <button
                        type="button"
                        className="icon-btn ghost"
                        onClick={() => handleToggleStatus(tenant)}
                        disabled={saving}
                        title={tenant.enabled ? 'Disable Tenant' : 'Enable Tenant'}
                        aria-label={tenant.enabled ? `Disable ${tenant.name}` : `Enable ${tenant.name}`}
                      >
                        <ToggleIcon enabled={tenant.enabled} />
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

      {pharmacyTenant && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closePharmacyModal}>
          <div className="modal-card" onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Tenant Pharmacies</p>
                <h3>{pharmacyTenant.name}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={closePharmacyModal} aria-label="Close pharmacy modal">x</button>
            </div>

            <div className="panel" style={{ marginBottom: 16 }}>
              <h4>Tenant Logo</h4>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
                {tenantLogoPreview ? (
                  <img src={tenantLogoPreview} alt="Tenant logo" className="brand-logo" />
                ) : (
                  <span className="brand-mark" aria-hidden="true">Rx</span>
                )}
                <label className="ghost" style={{ display: 'inline-block' }}>
                  Upload Tenant Logo
                  <input type="file" accept="image/*" onChange={handleUploadTenantLogo} disabled={saving} />
                </label>
              </div>
            </div>

            <form className="panel" onSubmit={handleCreatePharmacy}>
              <h4>Create Pharmacy</h4>
              <div className="form-grid">
                <label>
                  Pharmacy name
                  <input
                    required
                    value={pharmacyForm.name}
                    onChange={(event) => setPharmacyForm((prev) => ({ ...prev, name: event.target.value }))}
                    placeholder="Borella Branch"
                  />
                </label>
              </div>
              <div className="modal-actions">
                <button type="submit" disabled={saving || !pharmacyForm.name.trim()}>
                  {saving ? 'Saving...' : 'Create Pharmacy'}
                </button>
              </div>
            </form>

            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Status</th>
                    <th>Logo</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {tenantPharmacies.map((pharmacy) => (
                    <tr key={pharmacy.id}>
                      <td>{pharmacy.code}</td>
                      <td>{pharmacy.name}</td>
                      <td>{pharmacy.enabled ? 'Enabled' : 'Disabled'}</td>
                      <td>
                        {pharmacyLogoPreviews[pharmacy.id]
                          ? <img src={pharmacyLogoPreviews[pharmacy.id]} alt={`${pharmacy.name} logo`} className="brand-logo" />
                          : '-'}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                          <button
                            type="button"
                            className="icon-btn ghost"
                            onClick={() => handleTogglePharmacyStatus(pharmacy)}
                            disabled={saving}
                            title={pharmacy.enabled ? 'Disable Pharmacy' : 'Enable Pharmacy'}
                            aria-label={pharmacy.enabled ? `Disable ${pharmacy.name}` : `Enable ${pharmacy.name}`}
                          >
                            <ToggleIcon enabled={pharmacy.enabled} />
                          </button>
                          <label className="ghost" style={{ display: 'inline-block' }}>
                            Upload Logo
                            <input
                              type="file"
                              accept="image/*"
                              onChange={(event) => handleUploadPharmacyLogo(pharmacy, event)}
                              disabled={saving}
                            />
                          </label>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!tenantPharmacies.length && (
                    <tr>
                      <td colSpan="5" className="empty-cell">No pharmacies found for this tenant.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {editingTenant && configForm && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => { setEditingTenant(null); setConfigForm(null); }}>
          <form className="modal-card" onSubmit={handleSaveConfig} onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Module Configuration</p>
                <h3>{editingTenant.name}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={() => { setEditingTenant(null); setConfigForm(null); }} aria-label="Close modal">x</button>
            </div>
            <div className="form-grid">
              <label>
                <input
                  type="checkbox"
                  checked={configForm.billingEnabled}
                  onChange={(e) => setConfigForm({ ...configForm, billingEnabled: e.target.checked })}
                />
                Billing Module
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={configForm.transactionsEnabled}
                  onChange={(e) => setConfigForm({ ...configForm, transactionsEnabled: e.target.checked })}
                />
                Transactions Module
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={configForm.inventoryEnabled}
                  onChange={(e) => setConfigForm({ ...configForm, inventoryEnabled: e.target.checked })}
                />
                Inventory Module
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={configForm.analyticsEnabled}
                  onChange={(e) => setConfigForm({ ...configForm, analyticsEnabled: e.target.checked })}
                />
                Analytics Module
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={configForm.aiAssistantEnabled}
                  onChange={(e) => setConfigForm({ ...configForm, aiAssistantEnabled: e.target.checked })}
                />
                AI Assistant
              </label>
            </div>
            <div className="modal-actions">
              <button type="button" className="ghost" onClick={() => { setEditingTenant(null); setConfigForm(null); }} disabled={saving}>Cancel</button>
              <button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save Configuration'}</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
