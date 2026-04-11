import { useEffect, useState } from 'react';
import { createEmployee, deleteEmployee, fetchEmployees, updateEmployee } from '../api';

const ROLE_OPTIONS = ['BILLING', 'TRANSACTIONS', 'INVENTORY', 'ADMIN'];

function emptyForm() {
  return {
    username: '',
    password: '',
    roles: ['BILLING']
  };
}

export default function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(emptyForm());

  useEffect(() => {
    loadUsers();
  }, []);

  async function loadUsers() {
    setLoading(true);
    setError('');
    try {
      const data = await fetchEmployees();
      setUsers(data);
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
      await createEmployee(form);
      setForm(emptyForm());
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

  return (
    <section>
      <h2>User Management</h2>
      <p>Create users and control module access by assigning one or multiple roles.</p>

      {error && <p className="error">{error}</p>}

      <form className="panel" onSubmit={handleCreate}>
        <h3>Create User</h3>
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

        <button type="submit">Create User</button>
      </form>

      <div className="panel table-panel">
        <div className="page-title-row">
          <h3>Users</h3>
          <button type="button" onClick={loadUsers} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Username</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
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
                  <td>{user.enabled ? 'Enabled' : 'Disabled'}</td>
                  <td>
                    <div className="toolbar">
                      <button
                        type="button"
                        className="ghost"
                        onClick={async () => {
                          try {
                            setError('');
                            await toggleEnabled(user);
                          } catch (e) {
                            setError(e.message);
                          }
                        }}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </button>
                      <button
                        type="button"
                        className="ghost"
                        onClick={async () => {
                          try {
                            setError('');
                            await removeUser(user.id);
                          } catch (e) {
                            setError(e.message);
                          }
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

