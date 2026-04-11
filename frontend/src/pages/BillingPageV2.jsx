import { useEffect, useMemo, useState } from 'react';
import { createPrescriptionSale, fetchBillingMedicines } from '../api';

const USAGE_OPTIONS = [
  { value: 'ORAL_ONCE_DAILY_AFTER_MEALS', label: 'Oral - Take 1 tablet once daily after meals' },
  { value: 'ORAL_TWICE_DAILY_AFTER_FOOD', label: 'Oral - Take 1 tablet twice daily (morning and evening) after food' },
  { value: 'ORAL_THREE_TIMES_BEFORE_MEALS', label: 'Oral - Take 1 tablet three times daily before meals' },
  { value: 'TOPICAL_TWICE_DAILY', label: 'Topical - Apply externally twice daily' },
  { value: 'INJECTION_AS_DIRECTED', label: 'Injection - Take as directed by physician' },
  { value: 'CUSTOM', label: 'Custom instruction' }
];

function money(value) {
  return Number(value || 0).toFixed(2);
}

function round2(value) {
  return Math.round(Number(value || 0) * 100) / 100;
}

function emptyRow() {
  return {
    medicineId: '',
    quantity: 1,
    unitType: '',
    sellingPrice: 0,
    discountType: 'PERCENT',
    discountValue: 0,
    usageInstruction: USAGE_OPTIONS[0].value,
    customUsageInstruction: '',
    remark: ''
  };
}

export default function BillingPageV2() {
  const [inventory, setInventory] = useState([]);
  const [rows, setRows] = useState([emptyRow()]);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [latestBill, setLatestBill] = useState(null);

  const inventoryById = useMemo(() => new Map(inventory.map((item) => [String(item.id), item])), [inventory]);

  const calculatedRows = useMemo(() => {
    return rows.map((row) => {
      const quantity = Number(row.quantity || 0);
      const sellingPrice = Number(row.sellingPrice || 0);
      const subTotal = round2(quantity * sellingPrice);
      const rawDiscount = Number(row.discountValue || 0);
      const discountAmount = row.discountType === 'PERCENT'
        ? round2(subTotal * (rawDiscount / 100))
        : round2(rawDiscount);
      const clampedDiscount = Math.min(Math.max(discountAmount, 0), subTotal);
      const total = round2(subTotal - clampedDiscount);
      return {
        ...row,
        quantity,
        sellingPrice,
        subTotal,
        discountAmount: clampedDiscount,
        total,
        effectiveUnitPrice: quantity > 0 ? round2(total / quantity) : 0
      };
    });
  }, [rows]);

  const totals = useMemo(() => {
    return calculatedRows.reduce(
      (acc, row) => {
        acc.subTotal += row.subTotal;
        acc.discount += row.discountAmount;
        acc.payable += row.total;
        return acc;
      },
      { subTotal: 0, discount: 0, payable: 0 }
    );
  }, [calculatedRows]);

  useEffect(() => {
    loadBillingMedicines();
  }, []);

  async function loadBillingMedicines() {
    setLoading(true);
    setError('');
    try {
      setInventory(await fetchBillingMedicines());
    } catch (e) {
      // Keep billing UX clean for limited users and avoid exposing module-level technical errors.
      if (e.status === 401 || e.status === 403) {
        setInventory([]);
        return;
      }
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function updateRow(index, patch) {
    const next = [...rows];
    next[index] = { ...next[index], ...patch };

    if (patch.medicineId !== undefined) {
      const selected = inventoryById.get(String(next[index].medicineId));
      next[index].unitType = selected?.unitType || '';
      next[index].sellingPrice = Number(selected?.sellingPrice || 0);
    }

    if (patch.usageInstruction && patch.usageInstruction !== 'CUSTOM') {
      next[index].customUsageInstruction = '';
    }

    setRows(next);
  }

  function addRow() {
    setRows([...rows, emptyRow()]);
  }

  function removeRow(index) {
    if (rows.length === 1) {
      return;
    }
    setRows(rows.filter((_, idx) => idx !== index));
  }

  function validate() {
    calculatedRows.forEach((row, index) => {
      const selected = inventoryById.get(String(row.medicineId));
      const label = `Row ${index + 1}`;
      if (!row.medicineId) {
        throw new Error(`${label}: medicine is required.`);
      }
      if (selected && Number(selected.quantity) <= 0) {
        throw new Error(`${label}: selected medicine is out of stock.`);
      }
      if (row.quantity <= 0) {
        throw new Error(`${label}: quantity must be greater than zero.`);
      }
      if (selected && row.quantity > Number(selected.quantity)) {
        throw new Error(`${label}: quantity exceeds available stock (${selected.quantity}).`);
      }
      if (row.discountType === 'PERCENT' && Number(row.discountValue) > 100) {
        throw new Error(`${label}: discount percentage must be 0-100.`);
      }
      if (row.discountValue < 0) {
        throw new Error(`${label}: discount cannot be negative.`);
      }
      if (row.usageInstruction === 'CUSTOM' && !row.customUsageInstruction.trim()) {
        throw new Error(`${label}: custom instruction is required.`);
      }
    });
  }

  function mapUsage(row) {
    if (row.usageInstruction === 'CUSTOM') {
      return {
        dosageInstruction: 'CUSTOM',
        customDosageInstruction: row.customUsageInstruction.trim()
      };
    }
    const option = USAGE_OPTIONS.find((item) => item.value === row.usageInstruction);
    return {
      dosageInstruction: option ? option.label : row.usageInstruction,
      customDosageInstruction: null
    };
  }

  async function submitSale(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      validate();
      const payload = {
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        discountAmount: 0,
        items: calculatedRows.map((row) => {
          const selected = inventoryById.get(String(row.medicineId));
          const usage = mapUsage(row);
          return {
            medicineId: Number(row.medicineId),
            medicineName: selected?.name || '',
            quantity: row.quantity,
            unitType: row.unitType,
            pricePerUnit: row.effectiveUnitPrice,
            allowPriceOverride: true,
            dosageInstruction: usage.dosageInstruction,
            customDosageInstruction: usage.customDosageInstruction,
            remark: row.remark?.trim() || null
          };
        })
      };
      const bill = await createPrescriptionSale(payload);
      setLatestBill(bill);
      setRows([emptyRow()]);
      setCustomerName('');
      setCustomerPhone('');
      await loadBillingMedicines();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <div className="page-title-row">
        <h2>Billing</h2>
        <button type="button" onClick={loadBillingMedicines} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh Inventory'}
        </button>
      </div>

      {!loading && inventory.length === 0 && (
        <p className="muted">No medicines are available for billing right now.</p>
      )}

      {error && <p className="error">{error}</p>}

      <form className="panel" onSubmit={submitSale}>
        <div className="form-grid">
          <label>
            Customer name (optional)
            <input value={customerName} onChange={(event) => setCustomerName(event.target.value)} />
          </label>
          <label>
            Customer phone (optional)
            <input value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} />
          </label>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Unit</th>
                <th>Selling price / unit</th>
                <th>Quantity</th>
                <th>Discount</th>
                <th>Total price</th>
                <th>Usage instructions</th>
                <th>Remark</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {calculatedRows.map((row, index) => (
                <tr key={index}>
                  <td>
                    <select
                      required
                      value={row.medicineId}
                      onChange={(event) => updateRow(index, { medicineId: event.target.value })}
                    >
                      <option value="">Select medicine</option>
                      {inventory.map((item) => {
                        const out = Number(item.quantity) <= 0;
                        return (
                          <option key={item.id} value={item.id} disabled={out}>
                            {item.name} (stock: {item.quantity}){out ? ' - Out of stock' : ''}
                          </option>
                        );
                      })}
                    </select>
                  </td>
                  <td><input value={row.unitType} readOnly /></td>
                  <td><input value={money(row.sellingPrice)} readOnly /></td>
                  <td>
                    <input
                      type="number"
                      min="1"
                      value={row.quantity}
                      onChange={(event) => updateRow(index, { quantity: event.target.value })}
                    />
                  </td>
                  <td>
                    <div className="discount-control">
                      <select
                        value={row.discountType}
                        onChange={(event) => updateRow(index, { discountType: event.target.value })}
                      >
                        <option value="PERCENT">%</option>
                        <option value="FIXED">LKR</option>
                      </select>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={row.discountValue}
                        onChange={(event) => updateRow(index, { discountValue: event.target.value })}
                      />
                    </div>
                  </td>
                  <td className="strong amount-cell">{money(row.total)}</td>
                  <td>
                    <select
                      value={row.usageInstruction}
                      onChange={(event) => updateRow(index, { usageInstruction: event.target.value })}
                    >
                      {USAGE_OPTIONS.map((item) => (
                        <option key={item.value} value={item.value}>{item.label}</option>
                      ))}
                    </select>
                    {row.usageInstruction === 'CUSTOM' && (
                      <input
                        required
                        placeholder="Type custom instruction"
                        value={row.customUsageInstruction}
                        onChange={(event) => updateRow(index, { customUsageInstruction: event.target.value })}
                      />
                    )}
                  </td>
                  <td>
                    <input
                      placeholder="Optional"
                      value={row.remark}
                      onChange={(event) => updateRow(index, { remark: event.target.value })}
                    />
                  </td>
                  <td>
                    <button type="button" className="ghost" onClick={() => removeRow(index)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="billing-footer">
          <button type="button" className="plus-btn" onClick={addRow} title="Add medicine row">+</button>
          <div className="totals-box">
            <span>Subtotal: {money(totals.subTotal)}</span>
            <span>Discount: {money(totals.discount)}</span>
            <span className="strong">Final payable: {money(totals.payable)}</span>
          </div>
          <button type="submit" disabled={saving || loading || inventory.length === 0}>
            {saving ? 'Completing billing...' : 'Complete Billing'}
          </button>
        </div>
      </form>

      {latestBill && (
        <article className="panel">
          <h3>Latest Bill - {latestBill.transactionId}</h3>
          <p>
            <strong>Date:</strong> {new Date(latestBill.dateTime).toLocaleString()} | <strong>Sales person:</strong> {latestBill.salesPerson}
          </p>
          <p>
            <strong>Customer:</strong> {latestBill.customerName || 'Walk-in'} | <strong>Phone:</strong> {latestBill.customerPhone || '-'}
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Medicine</th>
                  <th>Qty</th>
                  <th>Unit</th>
                  <th>Usage</th>
                  <th>Remark</th>
                  <th>Price/Unit</th>
                  <th>Line Total</th>
                </tr>
              </thead>
              <tbody>
                {latestBill.items.map((item, index) => (
                  <tr key={index}>
                    <td>{item.medicineName}</td>
                    <td>{item.quantity}</td>
                    <td>{item.unitType}</td>
                    <td>{item.dosageInstruction === 'CUSTOM' ? item.customDosageInstruction : item.dosageInstruction}</td>
                    <td>{item.remark || '-'}</td>
                    <td>{money(item.pricePerUnit)}</td>
                    <td>{money(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="bill-totals">
            <span>Total before discount: {money(latestBill.totalBeforeDiscount)}</span>
            <span>Discount: {money(latestBill.discountAmount)}</span>
            <span className="strong">Total amount: {money(latestBill.totalAmount)}</span>
          </div>
        </article>
      )}
    </section>
  );
}

