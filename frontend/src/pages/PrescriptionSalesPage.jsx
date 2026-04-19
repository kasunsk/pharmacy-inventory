import { useEffect, useMemo, useState } from 'react';
import {
  createPrescriptionSale,
  fetchInventory,
  fetchTransactionBill,
  fetchTransactions
} from '../api';

function money(value) {
  return Number(value || 0).toFixed(2);
}

const UNIT_TYPES = [
  'tablet',
  'card',
  'box',
  'capsule',
  '50 ml bottle',
  '100 ml bottle',
  'syrup bottle',
  'tube',
  'vial'
];

const DOSAGE_OPTIONS = [
  '1 tablet per day (morning)',
  '1 tablet per day (evening after food)',
  '2 tablets per day (morning and evening after food)',
  '3 times a day (after meals)',
  'Apply externally twice a day',
  'Use as needed',
  'CUSTOM'
];

function emptyLine() {
  return {
    medicineId: '',
    quantity: 1,
    unitType: 'tablet',
    pricePerUnit: '',
    allowPriceOverride: false,
    dosageInstruction: DOSAGE_OPTIONS[0],
    customDosageInstruction: ''
  };
}

export default function PrescriptionSalesPage() {
  const [inventory, setInventory] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [discountAmount, setDiscountAmount] = useState('0');
  const [lines, setLines] = useState([emptyLine()]);

  const [latestBill, setLatestBill] = useState(null);
  const [history, setHistory] = useState([]);
  const [historyFilters, setHistoryFilters] = useState({
    transactionId: '',
    fromDate: '',
    toDate: ''
  });

  const inventoryById = useMemo(() => {
    return new Map(inventory.map((item) => [String(item.id), item]));
  }, [inventory]);

  const normalizedLines = useMemo(() => {
    return lines.map((line) => {
      const quantity = Number(line.quantity || 0);
      const pricePerUnit = Number(line.pricePerUnit || 0);
      return {
        ...line,
        quantity,
        pricePerUnit,
        lineTotal: quantity * pricePerUnit
      };
    });
  }, [lines]);

  useEffect(() => {
    loadInventory();
    loadHistory();
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

  async function loadHistory() {
    try {
      const data = await fetchTransactions(historyFilters);
      setHistory(data);
    } catch (e) {
      setError(e.message);
    }
  }

  function updateLine(index, patch) {
    const next = [...lines];
    next[index] = { ...next[index], ...patch };

    if (patch.medicineId !== undefined) {
      const selected = inventoryById.get(String(next[index].medicineId));
      if (selected) {
        next[index].pricePerUnit = selected.sellingPrice;
      } else {
        next[index].pricePerUnit = '';
      }
    }

    if (patch.allowPriceOverride === false) {
      const selected = inventoryById.get(String(next[index].medicineId));
      if (selected) {
        next[index].pricePerUnit = selected.sellingPrice;
      }
    }

    if (patch.dosageInstruction && patch.dosageInstruction !== 'CUSTOM') {
      next[index].customDosageInstruction = '';
    }

    setLines(next);
  }

  function addLine() {
    setLines([...lines, emptyLine()]);
  }

  function removeLine(index) {
    if (lines.length === 1) {
      return;
    }
    setLines(lines.filter((_, i) => i !== index));
  }

  const estimatedTotal = normalizedLines.reduce((acc, line) => {
    return acc + line.lineTotal;
  }, 0);

  const discountValue = Number(discountAmount || 0);
  const finalTotal = Math.max(estimatedTotal - discountValue, 0);

  function validateBeforeSubmit() {
    for (let i = 0; i < lines.length; i += 1) {
      const line = lines[i];
      if (!line.medicineId) {
        throw new Error(`Line ${i + 1}: medicine is required.`);
      }
      if (Number(line.quantity) <= 0) {
        throw new Error(`Line ${i + 1}: quantity must be greater than zero.`);
      }
      if (!line.unitType) {
        throw new Error(`Line ${i + 1}: unit type is required.`);
      }
      if (line.allowPriceOverride && Number(line.pricePerUnit) < 0) {
        throw new Error(`Line ${i + 1}: price per unit cannot be negative.`);
      }
      if (!line.dosageInstruction) {
        throw new Error(`Line ${i + 1}: dosage instruction is required.`);
      }
      if (line.dosageInstruction === 'CUSTOM' && !line.customDosageInstruction.trim()) {
        throw new Error(`Line ${i + 1}: custom dosage instruction is required.`);
      }
    }
    if (discountValue < 0) {
      throw new Error('Discount cannot be negative.');
    }
    if (discountValue > estimatedTotal) {
      throw new Error('Discount cannot exceed subtotal.');
    }
  }

  async function submitSale(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      validateBeforeSubmit();
      const payload = {
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        discountAmount: discountValue,
        items: lines.map((line) => {
          const selected = inventoryById.get(String(line.medicineId));
          return {
            medicineId: Number(line.medicineId),
            medicineName: selected?.name || '',
            quantity: Number(line.quantity),
            unitType: line.unitType,
            pricePerUnit: Number(line.pricePerUnit),
            allowPriceOverride: Boolean(line.allowPriceOverride),
            dosageInstruction: line.dosageInstruction,
            customDosageInstruction: line.dosageInstruction === 'CUSTOM'
              ? line.customDosageInstruction.trim()
              : null
          };
        })
      };

      const bill = await createPrescriptionSale(payload);
      setLatestBill(bill);
      setLines([emptyLine()]);
      setCustomerName('');
      setCustomerPhone('');
      setDiscountAmount('0');
      await loadInventory();
      await loadHistory();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  async function openBill(transactionId) {
    try {
      const bill = await fetchTransactionBill(transactionId);
      setLatestBill(bill);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <section>
      <h2>Prescription Sales</h2>
      <p>Fast billing for multiple medicines with automatic stock deduction.</p>

      {error && <p className="error">{error}</p>}

      <form className="card sales-form" onSubmit={submitSale}>
        <h3>New Sale</h3>
        <label>
          Customer Name (optional)
          <input value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
        </label>
        <label>
          Customer Phone (optional)
          <input value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} />
        </label>

        <div className="table-wrap entry-table-wrap">
          <table className="entry-table">
            <thead>
              <tr>
                <th>Medicine name</th>
                <th>Unit type</th>
                <th>Quantity</th>
                <th>Price / unit</th>
                <th>Total / item</th>
                <th>Dosage instruction</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {normalizedLines.map((line, index) => {
                const selected = inventoryById.get(String(line.medicineId));
                return (
                  <tr key={index}>
                    <td>
                      <select
                        required
                        value={line.medicineId}
                        onChange={(e) => updateLine(index, { medicineId: e.target.value })}
                      >
                        <option value="">Select medicine</option>
                        {inventory.map((item) => (
                          <option key={item.id} value={item.id}>
                            {item.name} (stock: {item.quantity})
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        required
                        value={line.unitType}
                        onChange={(e) => updateLine(index, { unitType: e.target.value })}
                      >
                        {UNIT_TYPES.map((unit) => (
                          <option key={unit} value={unit}>{unit}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        min="1"
                        required
                        value={line.quantity}
                        onChange={(e) => updateLine(index, { quantity: e.target.value })}
                      />
                    </td>
                    <td>
                      <div className="price-cell">
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          required
                          value={line.pricePerUnit}
                          disabled={!line.allowPriceOverride}
                          onChange={(e) => updateLine(index, { pricePerUnit: e.target.value })}
                        />
                        <label className="inline-check">
                          <input
                            type="checkbox"
                            checked={Boolean(line.allowPriceOverride)}
                            onChange={(e) => updateLine(index, { allowPriceOverride: e.target.checked })}
                          />
                          Allow override
                        </label>
                        {!line.allowPriceOverride && selected && (
                          <small>Auto from inventory: {money(selected.sellingPrice)}</small>
                        )}
                      </div>
                    </td>
                    <td className="item-total">{money(line.lineTotal)}</td>
                    <td>
                      <select
                        value={line.dosageInstruction}
                        onChange={(e) => updateLine(index, { dosageInstruction: e.target.value })}
                      >
                        {DOSAGE_OPTIONS.map((item) => (
                          <option key={item} value={item}>{item}</option>
                        ))}
                      </select>
                      {line.dosageInstruction === 'CUSTOM' && (
                        <input
                          required
                          placeholder="Enter custom dosage"
                          value={line.customDosageInstruction}
                          onChange={(e) => updateLine(index, { customDosageInstruction: e.target.value })}
                        />
                      )}
                    </td>
                    <td>
                      <button type="button" onClick={() => removeLine(index)}>
                        Remove
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="toolbar">
          <button type="button" onClick={addLine}>Add Medicine</button>
          <label>
            Discount
            <input
              type="number"
              min="0"
              step="0.01"
              value={discountAmount}
              onChange={(e) => setDiscountAmount(e.target.value)}
            />
          </label>
          <strong>Subtotal: {money(estimatedTotal)}</strong>
          <strong>Final Total: {money(finalTotal)}</strong>
        </div>

        <button type="submit" disabled={saving || loading}>
          {saving ? 'Completing Sale...' : 'Complete Sale'}
        </button>
      </form>

      {latestBill && (
        <div className="card bill-card">
          <h3>Bill: {latestBill.transactionId}</h3>
          <p>Date: {new Date(latestBill.dateTime).toLocaleString()}</p>
          <p>Customer: {latestBill.customerName || 'Walk-in'}</p>
          <p>Phone: {latestBill.customerPhone || '-'}</p>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Medicine</th>
                  <th>Qty</th>
                  <th>Unit</th>
                  <th>Dosage</th>
                  <th>Price/Unit</th>
                  <th>Line Total</th>
                </tr>
              </thead>
              <tbody>
                {latestBill.items.map((item, idx) => (
                  <tr key={idx}>
                    <td>{item.medicineName}</td>
                    <td>{item.quantity}</td>
                    <td>{item.unitType}</td>
                    <td>
                      {item.dosageInstruction === 'CUSTOM'
                        ? item.customDosageInstruction
                        : item.dosageInstruction}
                    </td>
                    <td>{money(item.pricePerUnit)}</td>
                    <td>{money(item.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p>Total before discount: {money(latestBill.totalBeforeDiscount)}</p>
          <p>Discount: {money(latestBill.discountAmount)}</p>
          <h4>Total amount: {money(latestBill.totalAmount)}</h4>

          <button type="button" onClick={() => window.print()}>Print / Download Bill</button>
        </div>
      )}

      <div className="card history-card">
        <h3>Transaction History</h3>
        <div className="toolbar">
          <input
            placeholder="Transaction ID"
            value={historyFilters.transactionId}
            onChange={(e) => setHistoryFilters({ ...historyFilters, transactionId: e.target.value })}
          />
          <input
            type="date"
            value={historyFilters.fromDate}
            onChange={(e) => setHistoryFilters({ ...historyFilters, fromDate: e.target.value })}
          />
          <input
            type="date"
            value={historyFilters.toDate}
            onChange={(e) => setHistoryFilters({ ...historyFilters, toDate: e.target.value })}
          />
          <button type="button" onClick={loadHistory}>Search</button>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Transaction</th>
                <th>Date</th>
                <th>Customer</th>
                <th>Items</th>
                <th>Total</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {history.map((txn) => (
                <tr key={txn.transactionId}>
                  <td>{txn.transactionId}</td>
                  <td>{formatDateTime(txn.dateTime)}</td>
                  <td>{txn.customerName || 'Walk-in'}</td>
                  <td>{txn.itemCount}</td>
                  <td>{money(txn.totalAmount)}</td>
                  <td>
                    <button type="button" onClick={() => openBill(txn.transactionId)}>Open Bill</button>
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

