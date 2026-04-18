import { useEffect, useState } from 'react';
import { fetchTransactionBill, fetchTransactions } from '../api';

const PAGE_SIZES = [5, 10, 20, 50];

function money(value) {
  return Number(value || 0).toFixed(2);
}

function EyeIcon() {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export default function TransactionHistoryPage() {
  const [filters, setFilters] = useState({
    transactionId: '',
    fromDate: '',
    toDate: ''
  });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [selectedBill, setSelectedBill] = useState(null);

  useEffect(() => {
    loadTransactions();
  }, [page, pageSize]);

  async function loadTransactions(nextPage = page, nextSize = pageSize, nextFilters = filters) {
    setLoading(true);
    setError('');
    try {
      const data = await fetchTransactions({ ...nextFilters, page: nextPage, size: nextSize });
      const content = Array.isArray(data) ? data : data.content || [];
      setTransactions(content);
      setTotalElements(Array.isArray(data) ? content.length : data.totalElements ?? content.length);
      setTotalPages(Math.max(Array.isArray(data) ? 1 : data.totalPages || 1, 1));
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

  function applyFilters() {
    setPage(0);
    loadTransactions(0, pageSize);
  }

  function clearFilters() {
    const emptyFilters = { transactionId: '', fromDate: '', toDate: '' };
    setFilters(emptyFilters);
    setSelectedBill(null);
    setPage(0);
    loadTransactions(0, pageSize, emptyFilters);
  }

  const start = totalElements === 0 ? 0 : page * pageSize + 1;
  const end = Math.min((page + 1) * pageSize, totalElements);

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
          <button type="button" onClick={applyFilters} disabled={loading}>
            {loading ? 'Loading...' : 'Apply Filters'}
          </button>
          <button type="button" className="ghost" onClick={clearFilters}>Clear</button>
        </div>
      </div>

      <div className="panel table-panel">
        <div className="table-toolbar">
          <span className="muted">Showing {start}-{end} of {totalElements}</span>
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
                    <button type="button" className="icon-btn ghost" onClick={() => openDetails(item.transactionId)} aria-label={`View ${item.transactionId}`} title="View details">
                      <EyeIcon />
                    </button>
                  </td>
                </tr>
              ))}
              {!transactions.length && (
                <tr>
                  <td colSpan="6" className="empty-cell">
                    {loading ? 'Loading transactions...' : 'No transactions found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="pagination-bar">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="pagination-controls">
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.max(value - 1, 0))} disabled={page === 0 || loading}>
              Previous
            </button>
            <button type="button" className="ghost" onClick={() => setPage((value) => Math.min(value + 1, totalPages - 1))} disabled={page >= totalPages - 1 || loading}>
              Next
            </button>
          </div>
        </div>
      </div>

      {selectedBill && (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => setSelectedBill(null)}>
          <article className="modal-card" onMouseDown={(event) => event.stopPropagation()}>
            <div className="page-title-row">
              <div>
                <p className="eyebrow">Transaction details</p>
                <h3>{selectedBill.transactionId}</h3>
              </div>
              <button type="button" className="ghost icon-btn" onClick={() => setSelectedBill(null)} aria-label="Close details">x</button>
            </div>
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
        </div>
      )}
    </section>
  );
}
