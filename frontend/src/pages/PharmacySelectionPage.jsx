import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function PharmacySelectionPage() {
  const { session, selectPharmacy, setDefaultPharmacy } = useAuth();
  const navigate = useNavigate();
  const [selectedId, setSelectedId] = useState(String(session?.selectedPharmacyId ?? ''));
  const [saveAsDefault, setSaveAsDefault] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const options = useMemo(() => session?.availablePharmacies || [], [session?.availablePharmacies]);

  async function submit(event) {
    event.preventDefault();
    if (!selectedId) {
      setError('Please select a pharmacy.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      if (saveAsDefault) {
        await setDefaultPharmacy(Number(selectedId));
      } else {
        await selectPharmacy(Number(selectedId));
      }
      navigate('/', { replace: true });
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-shell">
      <form className="panel login-panel" onSubmit={submit}>
        <p className="eyebrow">Pharmacy context</p>
        <h2>Select Pharmacy</h2>
        <p>Choose the pharmacy you want to work with for this session.</p>
        <label>
          Pharmacy
          <select value={selectedId} onChange={(event) => setSelectedId(event.target.value)} required>
            <option value="">Select pharmacy</option>
            {options.map((pharmacy) => (
              <option key={pharmacy.id} value={pharmacy.id}>{pharmacy.name}</option>
            ))}
          </select>
        </label>
        <label style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <input type="checkbox" checked={saveAsDefault} onChange={(event) => setSaveAsDefault(event.target.checked)} />
          Save as my default pharmacy
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>{loading ? 'Applying...' : 'Continue'}</button>
      </form>
    </section>
  );
}

