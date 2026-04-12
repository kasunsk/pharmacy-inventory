import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { createMedicine, fetchInventory, updateMedicine } from '../api';

const UNIT_TYPES = ['tablet', 'capsule', 'box', 'card', 'bottle', 'sachet', 'tube', 'vial'];
const PAGE_SIZES = [5, 10, 20, 50];

function emptyForm() {
  return {
    name: '',
    batchNumber: '',
    expiryDate: '',
    supplier: '',
    unitType: 'tablet',
    purchasePrice: '',
    sellingPrice: '',
    quantity: ''
  };
}

function toForm(item) {
  return {
    name: item.name || '',
    batchNumber: item.batchNumber || '',
    expiryDate: item.expiryDate || '',
    supplier: item.supplier || '',
    unitType: item.unitType || 'tablet',
    purchasePrice: item.purchasePrice ?? '',
    sellingPrice: item.sellingPrice ?? '',
    quantity: item.quantity ?? ''
  };
}

function toPayload(form) {
  return {
    ...form,
    purchasePrice: Number(form.purchasePrice),
    sellingPrice: Number(form.sellingPrice),
    quantity: Number(form.quantity)
  };
}

function EyeIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z" />
      <circle cx="12" cy="12" r="3" />
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

function MedicineFields({ form, setForm, includeReason, modificationReason, setModificationReason }) {
  return (
    <>
      <div className="form-grid">
        <label>
          Medicine name
          <input
            required
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
          />
        </label>
        <label>
          Batch
          <input
            required
            value={form.batchNumber}
            onChange={(event) => setForm({ ...form, batchNumber: event.target.value })}
          />
        </label>
        <label>
          Expiry
          <input
            required
            type="date"
            value={form.expiryDate}
            onChange={(event) => setForm({ ...form, expiryDate: event.target.value })}
          />
        </label>
        <label>
          Supplier
          <input
            required
            value={form.supplier}
            onChange={(event) => setForm({ ...form, supplier: event.target.value })}
          />
        </label>
        <label>
          Unit
          <select
            value={form.unitType}
            onChange={(event) => setForm({ ...form, unitType: event.target.value })}
          >
            {UNIT_TYPES.map((unit) => (
              <option key={unit} value={unit}>{unit}</option>
            ))}
          </select>
        </label>
        <label>
          Purchase price
          <input
            required
            type="number"
            min="0"
            step="0.01"
            value={form.purchasePrice}
            onChange={(event) => setForm({ ...form, purchasePrice: event.target.value })}
          />
        </label>
        <label>
          Selling price
          <input
            required
            type="number"
            min="0"
            step="0.01"
            value={form.sellingPrice}
            onChange={(event) => setForm({ ...form, sellingPrice: event.target.value })}
          />
        </label>
        <label>
          Quantity
          <input
            required
            type="number"
            min="0"
            value={form.quantity}
            onChange={(event) => setForm({ ...form, quantity: event.target.value })}
          />
        </label>
      </div>
      {includeReason && (
        <label className="full-width-field">
          Modification reason
          <textarea
            required
            rows="3"
            placeholder="Example: Corrected stock count after physical verification"
            value={modificationReason}
            onChange={(event) => setModificationReason(event.target.value)}
          />
        </label>
      )}
    </>
  );
}

export default function InventoryListPage() {
  const { hasRole } = useAuth();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [inventory, setInventory] = useState([]);
  const [filter, setFilter] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [viewingItem, setViewingItem] = useState(null);
  const [form, setForm] = useState(emptyForm());
  const [modificationReason, setModificationReason] = useState('');

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
  }, [page, pageSize]);

  async function loadInventory(nextPage = page, nextSize = pageSize) {
    setLoading(true);
    setError('');
    try {
      const data = await fetchInventory({ page: nextPage, size: nextSize });
      const content = Array.isArray(data) ? data : data.content || [];
      setInventory(content);
      setTotalElements(Array.isArray(data) ? content.length : data.totalElements ?? content.length);
      setTotalPages(Math.max(Array.isArray(data) ? 1 : data.totalPages || 1, 1));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function openAdd() {
    setError('');
    setForm(emptyForm());
    setIsAddOpen(true);
    setEditingItem(null);
    setModificationReason('');
  }

  function openEdit(item) {
    setError('');
    setForm(toForm(item));
    setEditingItem(item);
    setViewingItem(null);
    setIsAddOpen(false);
    setModificationReason('');
  }

  function closeModal() {
    setIsAddOpen(false);
    setEditingItem(null);
    setViewingItem(null);
    setForm(emptyForm());
    setModificationReason('');
  }

  async function handleCreate(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await createMedicine(toPayload(form));
      closeModal();
      setPage(0);
      await loadInventory(0, pageSize);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdate(event) {
    event.preventDefault();
    if (!modificationReason.trim()) {
      setError('Modification reason is required before saving inventory changes.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await updateMedicine(editingItem.id, {
        ...toPayload(form),
        modificationReason: modificationReason.trim()
      });
      closeModal();
      await loadInventory();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, totalElements);
  const modalTitle = editingItem ? `Edit ${editingItem.name}` : 'Add New Inventory';
  const canManageInventory = hasRole('INVENTORY');

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Inventory</h2>
          <p>Search, review, and maintain pharmacy stock with audit-ready change reasons.</p>
        </div>
        <div className="title-actions">
          <button type="button" className="ghost" onClick={loadInventory} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
          {canManageInventory && <button type="button" onClick={openAdd}>Add New</button>}
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="panel table-panel">
        <div className="table-toolbar">
          <label className="search-field">
            Search inventory
            <input
              placeholder="Medicine, batch, or supplier"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
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
                <th>Actions</th>
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
                    <div className="icon-actions">
                      <button type="button" className="icon-btn ghost" onClick={() => setViewingItem(item)} aria-label={`View ${item.name}`} title="View details">
                        <EyeIcon />
                      </button>
                      {canManageInventory && (
                        <button type="button" className="icon-btn ghost" onClick={() => openEdit(item)} aria-label={`Edit ${item.name}`} title="Edit">
                          <PencilIcon />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {!filtered.length && (
                <tr>
                  <td colSpan="10" className="empty-cell">
                    {loading ? 'Loading inventory...' : 'No inventory records found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="pagination-bar">
          <span>
            Showing {start}-{end} of {totalElements}
          </span>
          <div className="pagination-controls">
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.max(value - 1, 0))} disabled={page === 0 || loading}>
              Previous
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.min(value + 1, totalPages - 1))} disabled={page >= totalPages - 1 || loading}>
              Next
            </button>
          </div>
        </div>
      </div>

      {canManageInventory && (isAddOpen || editingItem) && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeModal}>
          <form className="modal-card" onSubmit={editingItem ? handleUpdate : handleCreate} onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">{editingItem ? 'Audit required' : 'Inventory setup'}</p>
                <h3>{modalTitle}</h3>
              </div>
              <button type="button" className="ghost" onClick={closeModal} disabled={saving}>
                Cancel
              </button>
            </div>
            <MedicineFields
              form={form}
              setForm={setForm}
              includeReason={Boolean(editingItem)}
              modificationReason={modificationReason}
              setModificationReason={setModificationReason}
            />
            <div className="modal-actions">
              <button type="button" className="ghost" onClick={closeModal} disabled={saving}>Cancel</button>
              <button type="submit" disabled={saving || (editingItem && !modificationReason.trim())}>
                {saving ? 'Saving...' : editingItem ? 'Save Changes' : 'Save Inventory'}
              </button>
            </div>
          </form>
        </div>
      )}

      {viewingItem && (
        <div className="modal-backdrop" role="presentation" onMouseDown={closeModal}>
          <article className="modal-card compact-modal" onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Inventory details</p>
                <h3>{viewingItem.name}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={closeModal} aria-label="Close details">x</button>
            </div>
            <div className="detail-grid">
              <span><strong>Batch</strong>{viewingItem.batchNumber}</span>
              <span><strong>Expiry</strong>{viewingItem.expiryDate}</span>
              <span><strong>Supplier</strong>{viewingItem.supplier}</span>
              <span><strong>Unit</strong>{viewingItem.unitType || '-'}</span>
              <span><strong>Purchase price</strong>{viewingItem.purchasePrice}</span>
              <span><strong>Selling price</strong>{viewingItem.sellingPrice}</span>
              <span><strong>Profit / unit</strong>{(Number(viewingItem.sellingPrice || 0) - Number(viewingItem.purchasePrice || 0)).toFixed(2)}</span>
              <span><strong>Quantity</strong>{viewingItem.quantity}</span>
            </div>
          </article>
        </div>
      )}
    </section>
  );
}
