import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { createMedicine, fetchInventory, login } from '../api';

export default function InventoryListPage() {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [inventory, setInventory] = useState([]);
  const [filter, setFilter] = useState('');
  const [createForm, setCreateForm] = useState({
    name: '',
    batchNumber: '',
    expiryDate: '',
    supplier: '',
    purchasePrice: '',
    sellingPrice: '',
    quantity: ''
  });

  const filtered = useMemo(() => {
    const value = filter.trim().toLowerCase();
    if (!value) {
      return inventory;
    }
    return inventory.filter((item) =>
      [item.name, item.batchNumber, item.supplier].some((v) =>
        String(v || '').toLowerCase().includes(value)
      )
    );
  }, [inventory, filter]);

  useEffect(() => {
    if (localStorage.getItem('token')) {
      loadInventory();
    }
  }, []);

  async function handleLogin(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await login(username, password);
      localStorage.setItem('token', data.token);
      await loadInventory();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadInventory() {
    setLoading(true);
    setError('');
    try {
      const data = await fetchInventory();
      setInventory(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      await createMedicine({
        ...createForm,
        purchasePrice: Number(createForm.purchasePrice),
        sellingPrice: Number(createForm.sellingPrice),
        quantity: Number(createForm.quantity)
      });
      setCreateForm({
        name: '',
        batchNumber: '',
        expiryDate: '',
        supplier: '',
        purchasePrice: '',
        sellingPrice: '',
        quantity: ''
      });
      await loadInventory();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <h2>Inventory</h2>

      {!localStorage.getItem('token') && (
        <form className="card" onSubmit={handleLogin}>
          <h3>Login</h3>
          <label>
            Username
            <input value={username} onChange={(e) => setUsername(e.target.value)} />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <button type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      )}

      {localStorage.getItem('token') && (
        <>
          <div className="toolbar">
            <input
              placeholder="Search by name, batch, supplier"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
            <button onClick={loadInventory} disabled={loading}>
              Refresh
            </button>
            <button
              onClick={() => {
                localStorage.removeItem('token');
                setInventory([]);
              }}
            >
              Logout
            </button>
          </div>

          <form className="card create-card" onSubmit={handleCreate}>
            <h3>Add Inventory Record</h3>
            <label>
              Medicine Name
              <input
                required
                value={createForm.name}
                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              />
            </label>
            <label>
              Batch Number
              <input
                required
                value={createForm.batchNumber}
                onChange={(e) => setCreateForm({ ...createForm, batchNumber: e.target.value })}
              />
            </label>
            <label>
              Expiry Date
              <input
                required
                type="date"
                value={createForm.expiryDate}
                onChange={(e) => setCreateForm({ ...createForm, expiryDate: e.target.value })}
              />
            </label>
            <label>
              Supplier
              <input
                required
                value={createForm.supplier}
                onChange={(e) => setCreateForm({ ...createForm, supplier: e.target.value })}
              />
            </label>
            <label>
              Purchase Price
              <input
                required
                type="number"
                step="0.01"
                min="0"
                value={createForm.purchasePrice}
                onChange={(e) => setCreateForm({ ...createForm, purchasePrice: e.target.value })}
              />
            </label>
            <label>
              Selling Price
              <input
                required
                type="number"
                step="0.01"
                min="0"
                value={createForm.sellingPrice}
                onChange={(e) => setCreateForm({ ...createForm, sellingPrice: e.target.value })}
              />
            </label>
            <label>
              Quantity
              <input
                required
                type="number"
                min="0"
                value={createForm.quantity}
                onChange={(e) => setCreateForm({ ...createForm, quantity: e.target.value })}
              />
            </label>
            <button type="submit" disabled={loading}>
              {loading ? 'Saving...' : 'Add Medicine'}
            </button>
          </form>
        </>
      )}

      {error && <p className="error">{error}</p>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Batch</th>
              <th>Expiry</th>
              <th>Supplier</th>
              <th>Selling Price</th>
              <th>Qty</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td>
                <td>{item.batchNumber}</td>
                <td>{item.expiryDate}</td>
                <td>{item.supplier}</td>
                <td>{item.sellingPrice}</td>
                <td>{item.quantity}</td>
                <td>
                  <Link to={`/inventory/${item.id}`}>View details</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

