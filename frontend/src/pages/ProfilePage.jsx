import { useEffect, useState } from 'react';
import { fetchProfile, updateProfile } from '../api';

const PROFILE_FIELDS = [
  ['firstName', 'First name', 'text'],
  ['lastName', 'Last name', 'text'],
  ['phoneNumber', 'Phone', 'tel'],
  ['email', 'Email', 'email'],
  ['address', 'Address', 'text'],
  ['birthdate', 'Birthdate', 'date'],
  ['gender', 'Gender', 'text']
];

function emptyProfile() {
  return {
    firstName: '',
    lastName: '',
    phoneNumber: '',
    email: '',
    address: '',
    birthdate: '',
    gender: '',
    password: ''
  };
}

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(emptyProfile());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    setError('');
    try {
      const data = await fetchProfile();
      setProfile(data);
      setForm({
        ...emptyProfile(),
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        phoneNumber: data.phoneNumber || '',
        email: data.email || '',
        address: data.address || '',
        birthdate: data.birthdate || '',
        gender: data.gender || ''
      });
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = { ...form };
      if (!payload.password.trim()) {
        delete payload.password;
      }
      const data = await updateProfile(payload);
      setProfile(data);
      setForm({ ...form, password: '' });
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Profile</h2>
          <p>Manage your personal details and reset your password.</p>
        </div>
        <button type="button" className="ghost" onClick={loadProfile} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="panel">
        {profile && (
          <div className="detail-grid">
            <span><strong>Username</strong>{profile.username}</span>
            <span><strong>Roles</strong>{(profile.roles || []).join(' / ')}</span>
            <span><strong>Status</strong>{profile.enabled ? 'Enabled' : 'Disabled'}</span>
          </div>
        )}
      </div>

      <form className="panel" onSubmit={handleSave}>
        <h3>Edit Details</h3>
        <div className="form-grid">
          {PROFILE_FIELDS.map(([key, label, type]) => (
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
            New password
            <input
              type="password"
              placeholder="Leave blank to keep current password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
            />
          </label>
        </div>
        <div className="modal-actions">
          <button type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </div>
      </form>
    </section>
  );
}
