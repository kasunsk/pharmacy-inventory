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

function emptyLine() {
  return {
    medicineId: '',
    quantity: 1,
    unitType: 'tablets',
    pricePerUnit: ''
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

    if (patch.medicineId) {
      const selected = inventoryById.get(String(patch.medicineId));
      if (selected && (next[index].pricePerUnit === '' || next[index].pricePerUnit == null)) {
        next[index].pricePerUnit = selected.sellingPrice;
      }
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

  const estimatedTotal = lines.reduce((acc, line) => {
    const qty = Number(line.quantity || 0);
    const unit = Number(line.pricePerUnit || 0);
    return acc + qty * unit;
  }, 0);

  async function submitSale(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      const payload = {
        customerName: customerName || null,
        customerPhone: customerPhone || null,
        discountAmount: Number(discountAmount || 0),
        items: lines.map((line) => {
          const selected = inventoryById.get(String(line.medicineId));
          return {
            medicineId: Number(line.medicineId),
            medicineName: selected?.name || '',
            quantity: Number(line.quantity),
            unitType: line.unitType,
            pricePerUnit: Number(line.pricePerUnit)
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

        <div className="line-items">
          {lines.map((line, index) => (
            <div key={index} className="line-item-row">
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

              <input
                type="number"
                min="1"
                required
                value={line.quantity}
                onChange={(e) => updateLine(index, { quantity: e.target.value })}
                placeholder="Qty"
              />

              <input
                required
                value={line.unitType}
                onChange={(e) => updateLine(index, { unitType: e.target.value })}
                placeholder="Unit type"
              />

              <input
                type="number"
                min="0"
                step="0.01"
                required
                value={line.pricePerUnit}
                onChange={(e) => updateLine(index, { pricePerUnit: e.target.value })}
                placeholder="Price/unit"
              />

              <button type="button" onClick={() => removeLine(index)}>
                Remove
              </button>
            </div>
          ))}
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
          <strong>Estimated: {money(estimatedTotal)}</strong>
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
                  <td>{new Date(txn.dateTime).toLocaleString()}</td>
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

