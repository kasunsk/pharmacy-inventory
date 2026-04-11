import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { createMedicine, fetchInventory } from '../api';

const UNIT_TYPES = ['tablet', 'capsule', 'box', 'card', 'bottle', 'sachet', 'tube', 'vial'];

export default function InventoryListPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [inventory, setInventory] = useState([]);
  const [filter, setFilter] = useState('');
  const [createForm, setCreateForm] = useState({
    name: '',
    batchNumber: '',
    expiryDate: '',
    supplier: '',
    unitType: 'tablet',
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
      [item.name, item.batchNumber, item.supplier].some((part) =>
        String(part || '').toLowerCase().includes(value)
      )
    );
  }, [inventory, filter]);

  useEffect(() => {
    loadInventory();
  }, []);

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
        unitType: 'tablet',
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
      <div className="page-title-row">
        <div>
          <h2>Inventory</h2>
          <p>Compact, row-based stock management for quick and accurate updates.</p>
        </div>
        <button type="button" onClick={loadInventory} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <form className="panel" onSubmit={handleCreate}>
        <h3>Add Inventory Row</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Medicine Name</th>
                <th>Batch</th>
                <th>Expiry</th>
                <th>Supplier</th>
                <th>Unit</th>
                <th>Purchase</th>
                <th>Selling</th>
                <th>Quantity</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <input
                    required
                    value={createForm.name}
                    onChange={(event) => setCreateForm({ ...createForm, name: event.target.value })}
                  />
                </td>
                <td>
                  <input
                    required
                    value={createForm.batchNumber}
                    onChange={(event) => setCreateForm({ ...createForm, batchNumber: event.target.value })}
                  />
                </td>
                <td>
                  <input
                    required
                    type="date"
                    value={createForm.expiryDate}
                    onChange={(event) => setCreateForm({ ...createForm, expiryDate: event.target.value })}
                  />
                </td>
                <td>
                  <input
                    required
                    value={createForm.supplier}
                    onChange={(event) => setCreateForm({ ...createForm, supplier: event.target.value })}
                  />
                </td>
                <td>
                  <select
                    value={createForm.unitType}
                    onChange={(event) => setCreateForm({ ...createForm, unitType: event.target.value })}
                  >
                    {UNIT_TYPES.map((unit) => (
                      <option key={unit} value={unit}>{unit}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={createForm.purchasePrice}
                    onChange={(event) => setCreateForm({ ...createForm, purchasePrice: event.target.value })}
                  />
                </td>
                <td>
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    value={createForm.sellingPrice}
                    onChange={(event) => setCreateForm({ ...createForm, sellingPrice: event.target.value })}
                  />
                </td>
                <td>
                  <input
                    required
                    type="number"
                    min="0"
                    value={createForm.quantity}
                    onChange={(event) => setCreateForm({ ...createForm, quantity: event.target.value })}
                  />
                </td>
                <td>
                  <button type="submit" disabled={loading}>
                    {loading ? 'Saving...' : 'Add'}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </form>

      <div className="panel table-panel">
        <div className="toolbar">
          <input
            placeholder="Search by medicine, batch, supplier"
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          />
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Batch</th>
                <th>Expiry</th>
                <th>Supplier</th>
                <th>Unit</th>
                <th>Cost Price</th>
                <th>Selling Price</th>
                <th>Profit / Unit</th>
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
                  <td>{item.unitType || '-'}</td>
                  <td>{item.purchasePrice}</td>
                  <td>{item.sellingPrice}</td>
                  <td>{(Number(item.sellingPrice || 0) - Number(item.purchasePrice || 0)).toFixed(2)}</td>
                  <td>{item.quantity}</td>
                  <td>
                    <Link to={`/inventory/${item.id}`}>View details</Link>
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

