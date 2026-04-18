import { useEffect, useState } from 'react';
import { createEmployee, deleteEmployee, fetchEmployees, fetchMyPharmacies, updateEmployee } from '../api';

const ROLE_OPTIONS = ['BILLING', 'TRANSACTIONS', 'INVENTORY', 'ADMIN'];
const GENDER_OPTIONS = ['MALE', 'FEMALE'];
const PAGE_SIZES = [5, 10, 20, 50];

function emptyForm() {
  return {
    username: '',
    password: '',
    roles: ['BILLING'],
    pharmacyIds: [],
    defaultPharmacyId: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    email: '',
    address: '',
    birthdate: '',
    gender: ''
  };
}

function StatusIcon({ enabled }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      {enabled ? (
        <>
          <circle cx="12" cy="12" r="9" />
          <path d="M15 9 9 15" />
          <path d="m9 9 6 6" />
        </>
      ) : (
        <>
          <circle cx="12" cy="12" r="9" />
          <path d="m8 12 2.5 2.5L16 9" />
        </>
      )}
    </svg>
  );
}

function KeyIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="8" cy="12" r="3" />
      <path d="M11 12h9" />
      <path d="M17 12v3" />
      <path d="M14 12v2" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 7h16" />
      <path d="M10 11v6" />
      <path d="M14 11v6" />
      <path d="M6 7l1 13h10l1-13" />
      <path d="M9 7V4h6v3" />
    </svg>
  );
}

function PencilIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 16.5V20h3.5L18.1 9.4l-3.5-3.5L4 16.5z" />
      <path d="m16 4.5 3.5 3.5" />
    </svg>
  );
}

export default function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(emptyForm());
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [resetUser, setResetUser] = useState(null);
  const [resetPassword, setResetPassword] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [pharmacyOptions, setPharmacyOptions] = useState([]);

  useEffect(() => {
    loadUsers();
  }, [page, pageSize]);

  useEffect(() => {
    loadPharmacies();
  }, []);

  async function loadPharmacies() {
    try {
      const data = await fetchMyPharmacies();
      setPharmacyOptions(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(e.message);
    }
  }

  async function loadUsers(nextPage = page, nextSize = pageSize) {
    setLoading(true);
    setError('');
    try {
      const data = await fetchEmployees({ page: nextPage, size: nextSize });
      const content = Array.isArray(data) ? data : data.content || [];
      setUsers(content);
      setTotalElements(Array.isArray(data) ? content.length : data.totalElements ?? content.length);
      setTotalPages(Math.max(Array.isArray(data) ? 1 : data.totalPages || 1, 1));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(event) {
    event.preventDefault();
    setError('');
    try {
      const payload = {
        ...form,
        defaultPharmacyId: form.defaultPharmacyId ? Number(form.defaultPharmacyId) : null,
        pharmacyIds: (form.pharmacyIds || []).map(Number)
      };
      await createEmployee(payload);
      setForm(emptyForm());
      setIsCreateOpen(false);
      setPage(0);
      await loadUsers(0, pageSize);
    } catch (e) {
      setError(e.message);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    if (!resetPassword.trim()) {
      setError('New password is required.');
      return;
    }
    setError('');
    try {
      await updateEmployee(resetUser.id, { password: resetPassword.trim() });
      setResetUser(null);
      setResetPassword('');
      await loadUsers();
    } catch (e) {
      setError(e.message);
    }
  }

  function toggleRole(currentRoles, role) {
    if (currentRoles.includes(role)) {
      const next = currentRoles.filter((item) => item !== role);
      return next.length ? next : currentRoles;
    }
    return [...currentRoles, role];
  }

  async function updateRoles(user, role) {
    const roles = toggleRole(user.roles || [], role);
    await updateEmployee(user.id, { roles });
    await loadUsers();
  }

  async function toggleEnabled(user) {
    await updateEmployee(user.id, { enabled: !user.enabled });
    await loadUsers();
  }

  async function removeUser(id) {
    await deleteEmployee(id);
    await loadUsers();
  }

  function closeCreate() {
    setForm(emptyForm());
    setIsCreateOpen(false);
  }

  function openEdit(user) {
    setEditingUser(user);
    const userPharmacyIds = user.pharmacyIds || [];
    const resolvedDefault = user.defaultPharmacyId ?? userPharmacyIds[0] ?? '';
    setForm({
      ...emptyForm(),
      username: user.username || '',
      password: '',
      roles: user.roles || [],
      pharmacyIds: userPharmacyIds,
      defaultPharmacyId: resolvedDefault,
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      phoneNumber: user.phoneNumber || '',
      email: user.email || '',
      address: user.address || '',
      birthdate: user.birthdate || '',
      gender: user.gender || ''
    });
  }

  function closeEdit() {
    setEditingUser(null);
    setForm(emptyForm());
  }

  async function handleEdit(event) {
    event.preventDefault();
    setError('');
    try {
      const payload = {
        ...form,
        defaultPharmacyId: form.defaultPharmacyId ? Number(form.defaultPharmacyId) : null,
        pharmacyIds: (form.pharmacyIds || []).map(Number)
      };
      if (!payload.password.trim()) {
        delete payload.password;
      }
      await updateEmployee(editingUser.id, payload);
      closeEdit();
      await loadUsers();
    } catch (e) {
      setError(e.message);
    }
  }

  const profileFields = [
    ['firstName', 'First name', 'text'],
    ['lastName', 'Last name', 'text'],
    ['phoneNumber', 'Phone number', 'tel'],
    ['email', 'Email', 'email'],
    ['address', 'Address', 'text'],
    ['birthdate', 'Birthdate', 'date']
  ];
  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, totalElements);
  const selectedIsAdmin = form.roles.includes('ADMIN');

  function togglePharmacySelection(pharmacyId) {
    const exists = form.pharmacyIds.includes(pharmacyId);
    const next = exists
      ? form.pharmacyIds.filter((id) => id !== pharmacyId)
      : [...form.pharmacyIds, pharmacyId];
    const defaultPharmacyId = next.includes(Number(form.defaultPharmacyId))
      ? form.defaultPharmacyId
      : next[0] ?? '';
    setForm({ ...form, pharmacyIds: next, defaultPharmacyId });
  }

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>User Management</h2>
          <p>Create users and control module access by assigning one or multiple roles.</p>
        </div>
        <button type="button" onClick={() => setIsCreateOpen(true)}>Add New User</button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="panel table-panel">
        <div className="page-title-row">
          <div>
            <h3>Users</h3>
            <span className="muted">Showing {start}-{end} of {totalElements}</span>
          </div>
          <div className="title-actions">
            <label className="page-size-control">
              Page size
              <select
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value));
                  setPage(0);
                }}
              >
                {PAGE_SIZES.map((size) => (
                  <option key={size} value={size}>{size}</option>
                ))}
              </select>
            </label>
            <button type="button" onClick={() => loadUsers()} disabled={loading}>
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Name</th>
                <th>Contact</th>
                <th>Roles</th>
                <th>Pharmacies</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{[user.firstName, user.lastName].filter(Boolean).join(' ') || '-'}</td>
                  <td>
                    {user.email || '-'}
                    {user.phoneNumber && <span className="muted">{user.phoneNumber}</span>}
                  </td>
                  <td>
                    <div className="role-pills compact">
                      {ROLE_OPTIONS.map((role) => (
                        <label key={role} className="role-pill">
                          <input
                            type="checkbox"
                            checked={(user.roles || []).includes(role)}
                            onChange={async () => {
                              try {
                                setError('');
                                await updateRoles(user, role);
                              } catch (e) {
                                setError(e.message);
                              }
                            }}
                          />
                          {role}
                        </label>
                      ))}
                    </div>
                  </td>
                  <td>
                    {user.roles?.includes('ADMIN')
                      ? 'All pharmacies'
                      : (user.pharmacyIds || []).length
                        ? (user.pharmacyIds || []).map((id) => pharmacyOptions.find((item) => item.id === id)?.name || `#${id}`).join(', ')
                        : '-'}
                  </td>
                  <td>{user.enabled ? 'Enabled' : 'Disabled'}</td>
                  <td>
                    <div className="icon-actions">
                      <button
                        type="button"
                        className="icon-btn ghost"
                        title="Edit details"
                        aria-label={`Edit ${user.username}`}
                        onClick={() => openEdit(user)}
                      >
                        <PencilIcon />
                      </button>
                      <button
                        type="button"
                        className="icon-btn ghost"
                        title={user.enabled ? 'Disable' : 'Enable'}
                        aria-label={user.enabled ? `Disable ${user.username}` : `Enable ${user.username}`}
                        onClick={async () => {
                          try {
                            setError('');
                            await toggleEnabled(user);
                          } catch (e) {
                            setError(e.message);
                          }
                        }}
                      >
                        <StatusIcon enabled={user.enabled} />
                      </button>
                      <button
                        type="button"
                        className="icon-btn ghost"
                        title="Reset password"
                        aria-label={`Reset password for ${user.username}`}
                        onClick={() => {
                          setResetUser(user);
                          setResetPassword('');
                        }}
                      >
                        <KeyIcon />
                      </button>
                      <button
                        type="button"
                        className="icon-btn ghost danger-action"
                        title="Delete"
                        aria-label={`Delete ${user.username}`}
                        onClick={async () => {
                          try {
                            setError('');
                            await removeUser(user.id);
                          } catch (e) {
                            setError(e.message);
                          }
                        }}
                      >
                        <TrashIcon />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {!users.length && (
                <tr>
                  <td colSpan="7" className="empty-cell">
                    {loading ? 'Loading users...' : 'No users found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="pagination-bar">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="pagination-controls">
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.max(value - 1, 0))} disabled={page === 0 || loading}>
              Previous
            </button>
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.min(value + 1, totalPages - 1))} disabled={page >= totalPages - 1 || loading}>
              Next
            </button>
          </div>
        </div>
      </div>

      {isCreateOpen && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeCreate}>
          <form className="modal-card" onSubmit={handleCreate} onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Admin access</p>
                <h3>Create User</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={closeCreate} aria-label="Close create user">x</button>
            </div>
            <div className="form-grid">
              <label>
                Username
                <input
                  required
                  value={form.username}
                  onChange={(event) => setForm({ ...form, username: event.target.value })}
                />
              </label>
              <label>
                Password
                <input
                  required
                  type="password"
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                />
              </label>
              {profileFields.map(([key, label, type]) => (
                <label key={key}>
                  {label}
                  <input
                    type={type}
                    value={form[key]}
                    onChange={(event) => setForm({ ...form, [key]: event.target.value })}
                  />
                </label>
              ))}
              <label>
                Gender
                <select
                  required
                  value={form.gender}
                  onChange={(event) => setForm({ ...form, gender: event.target.value })}
                >
                  <option value="">Select gender</option>
                  {GENDER_OPTIONS.map((value) => (
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </label>
            </div>

            <div className="panel" style={{ marginTop: '16px' }}>
              <h4>Pharmacy Assignment</h4>
              {selectedIsAdmin ? (
                <p className="muted">ADMIN users automatically access all pharmacies in the tenant.</p>
              ) : (
                <>
                  <div className="role-pills compact" style={{ marginBottom: '10px' }}>
                    {pharmacyOptions.map((pharmacy) => (
                      <label key={pharmacy.id} className="role-pill">
                        <input
                          type="checkbox"
                          checked={form.pharmacyIds.includes(pharmacy.id)}
                          onChange={() => togglePharmacySelection(pharmacy.id)}
                        />
                        {pharmacy.name}
                      </label>
                    ))}
                  </div>
                  <label>
                    Default pharmacy
                    <select
                      value={form.defaultPharmacyId}
                      onChange={(event) => setForm({ ...form, defaultPharmacyId: event.target.value })}
                      required={form.pharmacyIds.length > 0}
                    >
                      <option value="">Select default pharmacy</option>
                      {form.pharmacyIds.map((pharmacyId) => {
                        const pharmacy = pharmacyOptions.find((item) => item.id === pharmacyId);
                        return (
                          <option key={pharmacyId} value={pharmacyId}>{pharmacy?.name || `#${pharmacyId}`}</option>
                        );
                      })}
                    </select>
                  </label>
                </>
              )}
            </div>

            <div className="role-pills">
              {ROLE_OPTIONS.map((role) => (
                <label key={role} className="role-pill">
                  <input
                    type="checkbox"
                    checked={form.roles.includes(role)}
                    onChange={() => setForm({ ...form, roles: toggleRole(form.roles, role) })}
                  />
                  {role}
                </label>
              ))}
            </div>

            <div className="modal-actions">
              <button type="button" className="ghost" onClick={closeCreate}>Cancel</button>
              <button type="submit">Create User</button>
            </div>
          </form>
        </div>
      )}

      {resetUser && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setResetUser(null)}>
          <form className="modal-card compact-modal" onSubmit={handleResetPassword} onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Password reset</p>
                <h3>{resetUser.username}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={() => setResetUser(null)} aria-label="Close reset password">x</button>
            </div>
            <label>
              New password
              <input
                required
                type="password"
                value={resetPassword}
                onChange={(event) => setResetPassword(event.target.value)}
              />
            </label>
            <div className="modal-actions">
              <button type="button" className="ghost" onClick={() => setResetUser(null)}>Cancel</button>
              <button type="submit">Reset Password</button>
            </div>
          </form>
        </div>
      )}

      {editingUser && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeEdit}>
          <form className="modal-card" onSubmit={handleEdit} onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">User details</p>
                <h3>Edit {editingUser.username}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={closeEdit} aria-label="Close edit user">x</button>
            </div>
            <div className="form-grid">
              <label>
                Username
                <input
                  required
                  value={form.username}
                  onChange={(event) => setForm({ ...form, username: event.target.value })}
                />
              </label>
              <label>
                New password
                <input
                  type="password"
                  placeholder="Leave blank to keep current password"
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                />
              </label>
              {profileFields.map(([key, label, type]) => (
                <label key={key}>
                  {label}
                  <input
                    type={type}
                    value={form[key]}
                    onChange={(event) => setForm({ ...form, [key]: event.target.value })}
                  />
                </label>
              ))}
              <label>
                Gender
                <select
                  required
                  value={form.gender}
                  onChange={(event) => setForm({ ...form, gender: event.target.value })}
                >
                  <option value="">Select gender</option>
                  {GENDER_OPTIONS.map((value) => (
                    <option key={value} value={value}>{value}</option>
                  ))}
                </select>
              </label>
            </div>
            <div className="panel" style={{ marginTop: '16px' }}>
              <h4>Pharmacy Assignment</h4>
              {selectedIsAdmin ? (
                <p className="muted">ADMIN users automatically access all pharmacies in the tenant.</p>
              ) : (
                <>
                  <div className="role-pills compact" style={{ marginBottom: '10px' }}>
                    {pharmacyOptions.map((pharmacy) => (
                      <label key={pharmacy.id} className="role-pill">
                        <input
                          type="checkbox"
                          checked={form.pharmacyIds.includes(pharmacy.id)}
                          onChange={() => togglePharmacySelection(pharmacy.id)}
                        />
                        {pharmacy.name}
                      </label>
                    ))}
                  </div>
                  <label>
                    Default pharmacy
                    <select
                      value={form.defaultPharmacyId}
                      onChange={(event) => setForm({ ...form, defaultPharmacyId: event.target.value })}
                      required={form.pharmacyIds.length > 0}
                    >
                      <option value="">Select default pharmacy</option>
                      {form.pharmacyIds.map((pharmacyId) => {
                        const pharmacy = pharmacyOptions.find((item) => item.id === pharmacyId);
                        return (
                          <option key={pharmacyId} value={pharmacyId}>{pharmacy?.name || `#${pharmacyId}`}</option>
                        );
                      })}
                    </select>
                  </label>
                </>
              )}
            </div>
            <div className="role-pills">
              {ROLE_OPTIONS.map((role) => (
                <label key={role} className="role-pill">
                  <input
                    type="checkbox"
                    checked={form.roles.includes(role)}
                    onChange={() => setForm({ ...form, roles: toggleRole(form.roles, role) })}
                  />
                  {role}
                </label>
              ))}
            </div>
            <div className="modal-actions">
              <button type="button" className="ghost" onClick={closeEdit}>Cancel</button>
              <button type="submit">Save User</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
