import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { createMedicine, fetchInventory, fetchInventoryAlertsSummary, updateMedicine } from '../api';

const UNIT_TYPES = ['tablet', 'capsule', 'box', 'card', 'bottle', 'sachet', 'tube', 'vial'];
const PAGE_SIZES = [5, 10, 20, 50];

function buildDefaultUnitDefinition(unit, baseUnit) {
  return {
    unitType: unit,
    parentUnit: unit === baseUnit ? '' : baseUnit,
    unitsPerParent: unit === baseUnit ? '' : '1',
    purchasePrice: '',
    sellingPrice: '',
    quantity: ''
  };
}

function ensureUnitDefinitions(form) {
  const baseUnit = form.baseUnit || form.allowedUnits?.[0] || 'tablet';
  const byUnit = new Map((form.unitDefinitions || []).map((definition) => [definition.unitType, definition]));
  const nextDefinitions = (form.allowedUnits || []).map((unit) => {
    const existing = byUnit.get(unit);
    if (existing) {
      return {
        ...existing,
        parentUnit: unit === baseUnit ? '' : (existing.parentUnit || baseUnit),
        unitsPerParent: unit === baseUnit ? '' : (existing.unitsPerParent || '1')
      };
    }
    return buildDefaultUnitDefinition(unit, baseUnit);
  });
  return {
    ...form,
    baseUnit,
    unitDefinitions: nextDefinitions
  };
}

function emptyForm() {
  return ensureUnitDefinitions({
    name: '',
    batchNumber: '',
    expiryDate: '',
    supplier: '',
    baseUnit: 'tablet',
    allowedUnits: ['tablet'],
    purchasePrice: '',
    sellingPrice: '',
    quantity: '',
    unitDefinitions: [buildDefaultUnitDefinition('tablet', 'tablet')]
  });
}

function toForm(item) {
  const existingUnits = Array.isArray(item.allowedUnits) && item.allowedUnits.length
    ? item.allowedUnits
    : [item.baseUnit || item.unitType || 'tablet'];
  const baseQuantity = Number(item.baseQuantity ?? item.quantity ?? 0);
  const unitDefinitions = Array.isArray(item.unitDefinitions) && item.unitDefinitions.length
    ? item.unitDefinitions.map((definition) => ({
      unitType: definition.unitType,
      parentUnit: definition.parentUnit || '',
      unitsPerParent: definition.unitsPerParent == null ? '' : String(definition.unitsPerParent),
      purchasePrice: definition.purchasePrice ?? '',
      sellingPrice: definition.sellingPrice ?? '',
      quantity: definition.conversionToBase
        ? String(Math.floor(baseQuantity / Number(definition.conversionToBase)))
        : ''
    }))
    : existingUnits.map((unit) => buildDefaultUnitDefinition(unit, item.baseUnit || item.unitType || existingUnits[0]));

  return ensureUnitDefinitions({
    name: item.name || '',
    batchNumber: item.batchNumber || '',
    expiryDate: item.expiryDate || '',
    supplier: item.supplier || '',
    baseUnit: item.baseUnit || item.unitType || existingUnits[0],
    allowedUnits: existingUnits,
    purchasePrice: item.purchasePrice ?? '',
    sellingPrice: item.sellingPrice ?? '',
    quantity: item.baseQuantity ?? item.quantity ?? '',
    unitDefinitions
  });
}

function toPayload(form) {
  const normalized = ensureUnitDefinitions(form);
  const allowedUnits = Array.isArray(normalized.allowedUnits) ? normalized.allowedUnits.filter(Boolean) : [];
  const unitDefinitions = (normalized.unitDefinitions || [])
    .filter((definition) => allowedUnits.includes(definition.unitType))
    .map((definition) => ({
      unitType: definition.unitType,
      parentUnit: definition.unitType === normalized.baseUnit ? null : (definition.parentUnit || normalized.baseUnit),
      unitsPerParent: definition.unitType === normalized.baseUnit ? null : Number(definition.unitsPerParent || 1),
      purchasePrice: Number(definition.purchasePrice || 0),
      sellingPrice: Number(definition.sellingPrice || 0),
      quantity: Number(definition.quantity || 0)
    }));

  const baseDefinition = unitDefinitions.find((definition) => definition.unitType === normalized.baseUnit) || unitDefinitions[0];
  return {
    ...normalized,
    unitType: normalized.baseUnit,
    baseUnit: normalized.baseUnit,
    allowedUnits,
    unitDefinitions,
    purchasePrice: baseDefinition ? baseDefinition.purchasePrice : Number(normalized.purchasePrice || 0),
    sellingPrice: baseDefinition ? baseDefinition.sellingPrice : Number(normalized.sellingPrice || 0),
    quantity: Number(normalized.quantity || 0)
  };
}

function toggleUnitSelection(form, unit) {
  const selected = new Set(form.allowedUnits || []);
  if (selected.has(unit)) {
    selected.delete(unit);
  } else {
    selected.add(unit);
  }

  const nextAllowedUnits = Array.from(selected);
  const nextBase = nextAllowedUnits.includes(form.baseUnit) ? form.baseUnit : (nextAllowedUnits[0] || 'tablet');
  const nextForm = {
    ...form,
    allowedUnits: nextAllowedUnits,
    baseUnit: nextBase
  };
  return ensureUnitDefinitions(nextForm);
}

function updateUnitDefinition(form, unitType, patch) {
  return {
    ...form,
    unitDefinitions: (form.unitDefinitions || []).map((definition) =>
      definition.unitType === unitType
        ? { ...definition, ...patch }
        : definition
    )
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
  const normalizedForm = ensureUnitDefinitions(form);
  const unitOptions = normalizedForm.allowedUnits || [];

  return (
    <>
      <div className="form-grid">
        <label>
          Medicine name
          <input
            required
            value={normalizedForm.name}
            onChange={(event) => setForm({ ...normalizedForm, name: event.target.value })}
          />
        </label>
        <label>
          Batch
          <input
            required
            value={normalizedForm.batchNumber}
            onChange={(event) => setForm({ ...normalizedForm, batchNumber: event.target.value })}
          />
        </label>
        <label>
          Expiry
          <input
            required
            type="date"
            value={normalizedForm.expiryDate}
            onChange={(event) => setForm({ ...normalizedForm, expiryDate: event.target.value })}
          />
        </label>
        <label>
          Supplier
          <input
            required
            value={normalizedForm.supplier}
            onChange={(event) => setForm({ ...normalizedForm, supplier: event.target.value })}
          />
        </label>
        <div className="full-width-field">
          <span>Allowed units</span>
          <div className="role-pills compact">
            {UNIT_TYPES.map((unit) => (
              <label key={unit} className="role-pill">
                <input
                  type="checkbox"
                  checked={(normalizedForm.allowedUnits || []).includes(unit)}
                  onChange={() => setForm(toggleUnitSelection(normalizedForm, unit))}
                />
                {unit}
              </label>
            ))}
          </div>
          <small className="muted">Select one or more units and configure hierarchy and conversion per medicine.</small>
        </div>
        <label>
          Base unit
          <select
            value={normalizedForm.baseUnit}
            onChange={(event) => setForm(ensureUnitDefinitions({ ...normalizedForm, baseUnit: event.target.value }))}
          >
            {unitOptions.map((unit) => (
              <option key={unit} value={unit}>{unit}</option>
            ))}
          </select>
        </label>
        <label>
          Total stock in base units
          <input
            type="number"
            min="0"
            value={normalizedForm.quantity}
            onChange={(event) => setForm({ ...normalizedForm, quantity: event.target.value })}
          />
        </label>
      </div>

      <div className="table-wrap" style={{ marginTop: '14px' }}>
        <table>
          <thead>
            <tr>
              <th>Unit</th>
              <th>Parent unit</th>
              <th>Units / parent</th>
              <th>Cost price</th>
              <th>Selling price</th>
              <th>Configured qty</th>
            </tr>
          </thead>
          <tbody>
            {normalizedForm.unitDefinitions.map((definition) => {
              const isBase = definition.unitType === normalizedForm.baseUnit;
              const selectableParents = unitOptions.filter((unit) => unit !== definition.unitType);
              return (
                <tr key={definition.unitType}>
                  <td>{definition.unitType}</td>
                  <td>
                    {isBase ? (
                      <span className="muted">Base unit</span>
                    ) : (
                      <select
                        value={definition.parentUnit || normalizedForm.baseUnit}
                        onChange={(event) => setForm(updateUnitDefinition(normalizedForm, definition.unitType, { parentUnit: event.target.value }))}
                      >
                        {selectableParents.map((unit) => (
                          <option key={unit} value={unit}>{unit}</option>
                        ))}
                      </select>
                    )}
                  </td>
                  <td>
                    {isBase ? (
                      <input value="1" readOnly />
                    ) : (
                      <input
                        required
                        type="number"
                        min="1"
                        value={definition.unitsPerParent}
                        onChange={(event) => setForm(updateUnitDefinition(normalizedForm, definition.unitType, { unitsPerParent: event.target.value }))}
                      />
                    )}
                  </td>
                  <td>
                    <input
                      required
                      type="number"
                      min="0"
                      step="0.01"
                      value={definition.purchasePrice}
                      onChange={(event) => setForm(updateUnitDefinition(normalizedForm, definition.unitType, { purchasePrice: event.target.value }))}
                    />
                  </td>
                  <td>
                    <input
                      required
                      type="number"
                      min="0"
                      step="0.01"
                      value={definition.sellingPrice}
                      onChange={(event) => setForm(updateUnitDefinition(normalizedForm, definition.unitType, { sellingPrice: event.target.value }))}
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      value={definition.quantity}
                      onChange={(event) => setForm(updateUnitDefinition(normalizedForm, definition.unitType, { quantity: event.target.value }))}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
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
  const { hasAnyRole } = useAuth();
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
  const [alertsSummary, setAlertsSummary] = useState(null);
  const [alertsLoading, setAlertsLoading] = useState(false);
  const [alertsError, setAlertsError] = useState('');

  // Detect whether the edit form has any actual changes vs the original item,
  // including unit hierarchy and conversion configuration.
  const isDirty = useMemo(() => {
    if (!editingItem) return true; // new-item form is always submittable
    const currentForm = ensureUnitDefinitions(form);
    const original = ensureUnitDefinitions(toForm(editingItem));
    const scalarKeys = ['name', 'batchNumber', 'expiryDate', 'supplier', 'quantity', 'baseUnit'];

    // Check scalar fields for changes
    for (const key of scalarKeys) {
      if (String(currentForm[key] ?? '') !== String(original[key] ?? '')) {
        return true;
      }
    }

    // Check allowedUnits for changes - compare as sorted arrays for order-independent comparison
    const currentUnits = Array.isArray(currentForm.allowedUnits) ? currentForm.allowedUnits : [];
    const originalUnits = Array.isArray(original.allowedUnits) ? original.allowedUnits : [];

    // If lengths differ, there's a change
    if (currentUnits.length !== originalUnits.length) {
      return true;
    }

    // Sort and compare for order-independent equality
    const sortedCurrent = [...currentUnits].sort().join(',');
    const sortedOriginal = [...originalUnits].sort().join(',');
    if (sortedCurrent !== sortedOriginal) {
      return true;
    }

    const serializeDefinitions = (definitions) =>
      JSON.stringify(
        (definitions || [])
          .map((definition) => ({
            unitType: definition.unitType,
            parentUnit: definition.parentUnit || '',
            unitsPerParent: String(definition.unitsPerParent || ''),
            purchasePrice: String(definition.purchasePrice || ''),
            sellingPrice: String(definition.sellingPrice || ''),
            quantity: String(definition.quantity || '')
          }))
          .sort((a, b) => a.unitType.localeCompare(b.unitType))
      );

    if (serializeDefinitions(currentForm.unitDefinitions) !== serializeDefinitions(original.unitDefinitions)) {
      return true;
    }

    return false;
  }, [form, editingItem]);

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
    loadAlertsSummary();
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

  async function loadAlertsSummary() {
    setAlertsLoading(true);
    setAlertsError('');
    try {
      const data = await fetchInventoryAlertsSummary();
      setAlertsSummary(data);
    } catch (e) {
      setAlertsError(e.message);
    } finally {
      setAlertsLoading(false);
    }
  }

  async function handleRefresh() {
    await Promise.all([loadInventory(), loadAlertsSummary()]);
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
    if (!form.allowedUnits?.length) {
      setError('Select at least one allowed unit.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      await createMedicine(toPayload(form));
      closeModal();
      setPage(0);
      await Promise.all([loadInventory(0, pageSize), loadAlertsSummary()]);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdate(event) {
    event.preventDefault();
    if (!form.allowedUnits?.length) {
      setError('Select at least one allowed unit.');
      return;
    }
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
      await Promise.all([loadInventory(), loadAlertsSummary()]);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, totalElements);
  const modalTitle = editingItem ? `Edit ${editingItem.name}` : 'Add New Inventory';
  const canManageInventory = hasAnyRole(['INVENTORY']);

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Inventory</h2>
          <p>Search, review, and maintain pharmacy stock with audit-ready change reasons.</p>
        </div>
        <div className="title-actions">
          <button type="button" className="ghost" onClick={handleRefresh} disabled={loading || alertsLoading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
          {canManageInventory && <button type="button" onClick={openAdd}>Add New</button>}
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {alertsError && <p className="error">{alertsError}</p>}

      <div className="summary-grid">
        <article className="summary-card">
          <h3>Low stock medicines</h3>
          <p>{alertsLoading ? '...' : alertsSummary?.lowStockCount ?? '-'}</p>
        </article>
        <article className="summary-card">
          <h3>Expiring soon</h3>
          <p>{alertsLoading ? '...' : alertsSummary?.expiringSoonCount ?? '-'}</p>
        </article>
        <article className="summary-card">
          <h3>Alert window</h3>
          <p>
            {alertsLoading
              ? '...'
              : `Qty <= ${alertsSummary?.lowStockThreshold ?? 10} | ${alertsSummary?.expiryWithinDays ?? 30} days`}
          </p>
        </article>
      </div>

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
                <th>Allowed units</th>
                <th>Cost Price</th>
                <th>Selling Price</th>
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
                  <td>{(item.allowedUnits || [item.unitType]).filter(Boolean).join(', ') || '-'}</td>
                   <td>{item.purchasePrice}</td>
                   <td>{item.sellingPrice}</td>
                   <td>{item.baseQuantity ?? item.quantity}</td>
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
                  <td colSpan="9" className="empty-cell">
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
              <button type="submit" disabled={saving || (editingItem && (!isDirty || !modificationReason.trim()))}>
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
              <span><strong>Allowed units</strong>{(viewingItem.allowedUnits || [viewingItem.unitType]).filter(Boolean).join(', ') || '-'}</span>
              <span><strong>Purchase price</strong>{viewingItem.purchasePrice}</span>
              <span><strong>Selling price</strong>{viewingItem.sellingPrice}</span>
              <span><strong>Profit / unit</strong>{(Number(viewingItem.sellingPrice || 0) - Number(viewingItem.purchasePrice || 0)).toFixed(2)}</span>
              <span><strong>Base stock</strong>{viewingItem.baseQuantity ?? viewingItem.quantity}</span>
            </div>
          </article>
        </div>
      )}
    </section>
  );
}
