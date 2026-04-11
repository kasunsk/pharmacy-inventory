import { useEffect, useState } from 'react';
import { fetchTransactionBill, fetchTransactions } from '../api';

function money(value) {
  return Number(value || 0).toFixed(2);
}

export default function TransactionHistoryPage() {
  const [filters, setFilters] = useState({
    transactionId: '',
    salesPerson: '',
    fromDate: '',
    toDate: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [selectedBill, setSelectedBill] = useState(null);

  useEffect(() => {
    loadTransactions();
  }, []);

  async function loadTransactions() {
    setLoading(true);
    setError('');
    try {
      const data = await fetchTransactions(filters);
      setTransactions(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function openDetails(transactionId) {
    try {
      const data = await fetchTransactionBill(transactionId);
      setSelectedBill(data);
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <section>
      <h2>Transaction History</h2>
      <p>Search and review completed transactions with full medicine-level details.</p>

      {error && <p className="error">{error}</p>}

      <div className="panel">
        <div className="filters-grid">
          <label>
            Transaction ID
            <input
              placeholder="TXN-..."
              value={filters.transactionId}
              onChange={(event) => setFilters({ ...filters, transactionId: event.target.value })}
            />
          </label>
          <label>
            Sales person
            <input
              placeholder="username"
              value={filters.salesPerson}
              onChange={(event) => setFilters({ ...filters, salesPerson: event.target.value })}
            />
          </label>
          <label>
            From date
            <input
              type="date"
              value={filters.fromDate}
              onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })}
            />
          </label>
          <label>
            To date
            <input
              type="date"
              value={filters.toDate}
              onChange={(event) => setFilters({ ...filters, toDate: event.target.value })}
            />
          </label>
        </div>
        <div className="toolbar">
          <button type="button" onClick={loadTransactions} disabled={loading}>
            {loading ? 'Loading...' : 'Apply Filters'}
          </button>
          <button
            type="button"
            className="ghost"
            onClick={() => {
              setFilters({ transactionId: '', salesPerson: '', fromDate: '', toDate: '' });
              setSelectedBill(null);
            }}
          >
            Clear
          </button>
        </div>
      </div>

      <div className="panel table-panel">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Transaction ID</th>
                <th>Date &amp; time</th>
                <th>Sales person</th>
                <th>Medicines</th>
                <th>Total amount</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((item) => (
                <tr key={item.transactionId}>
                  <td>{item.transactionId}</td>
                  <td>{new Date(item.dateTime).toLocaleString()}</td>
                  <td>{item.salesPerson}</td>
                  <td>{(item.medicines || []).join(', ')}</td>
                  <td className="strong amount-cell">{money(item.totalAmount)}</td>
                  <td>
                    <button type="button" className="ghost" onClick={() => openDetails(item.transactionId)}>
                      View details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {selectedBill && (
        <article className="panel">
          <h3>Transaction Details - {selectedBill.transactionId}</h3>
          <p>
            <strong>Date:</strong> {new Date(selectedBill.dateTime).toLocaleString()} | <strong>Sales person:</strong>{' '}
            {selectedBill.salesPerson}
          </p>
          <p>
            <strong>Customer:</strong> {selectedBill.customerName || 'Walk-in'} | <strong>Phone:</strong>{' '}
            {selectedBill.customerPhone || '-'}
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
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                {selectedBill.items.map((entry, idx) => (
                  <tr key={idx}>
                    <td>{entry.medicineName}</td>
                    <td>{entry.quantity}</td>
                    <td>{entry.unitType}</td>
                    <td>{entry.dosageInstruction === 'CUSTOM' ? entry.customDosageInstruction : entry.dosageInstruction}</td>
                    <td>{entry.remark || '-'}</td>
                    <td>{money(entry.pricePerUnit)}</td>
                    <td>{money(entry.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="bill-totals">
            <span>Total before discount: {money(selectedBill.totalBeforeDiscount)}</span>
            <span>Discount: {money(selectedBill.discountAmount)}</span>
            <span className="strong">Total amount: {money(selectedBill.totalAmount)}</span>
          </div>
        </article>
      )}
    </section>
  );
}

