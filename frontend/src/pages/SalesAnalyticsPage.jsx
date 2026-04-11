import { useEffect, useState } from 'react';
import { fetchSalesSummary } from '../api';

const PERIODS = ['DAY', 'WEEK', 'MONTH', 'YEAR'];

function money(value) {
  return Number(value || 0).toFixed(2);
}

export default function SalesAnalyticsPage() {
  const [period, setPeriod] = useState('DAY');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    load('DAY');
  }, []);

  async function load(nextPeriod) {
    setLoading(true);
    setError('');
    try {
      const data = await fetchSalesSummary(nextPeriod);
      setSummary(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <h2>Sales Analytics</h2>
      <p>Performance insights by time range, top-selling medicines, and sales by user.</p>

      <div className="toolbar">
        <select
          value={period}
          onChange={(event) => {
            const next = event.target.value;
            setPeriod(next);
            load(next);
          }}
        >
          {PERIODS.map((item) => (
            <option key={item} value={item}>{item}</option>
          ))}
        </select>
        <button onClick={() => load(period)} disabled={loading}>
          {loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {summary && (
        <>
          <div className="summary-grid">
            <article className="summary-card">
              <h3>Range</h3>
              <p>{summary.from} to {summary.to}</p>
            </article>
            <article className="summary-card">
              <h3>Sales Count</h3>
              <p>{summary.saleCount}</p>
            </article>
            <article className="summary-card">
              <h3>Total Sales</h3>
              <p className="strong">{money(summary.totalSales)}</p>
            </article>
            <article className="summary-card">
              <h3>Total Cost</h3>
              <p>{money(summary.totalCost)}</p>
            </article>
            <article className="summary-card">
              <h3>Total Profit</h3>
              <p className="strong">{money(summary.totalProfit)}</p>
            </article>
          </div>

          <div className="analytics-split">
            <article className="panel table-panel">
              <h3>Top-selling medicines</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Medicine</th>
                      <th>Quantity sold</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(summary.topSellingMedicines || []).map((row) => (
                      <tr key={row.medicineName}>
                        <td>{row.medicineName}</td>
                        <td>{row.quantitySold}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>

            <article className="panel table-panel">
              <h3>Sales by user</h3>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Sales count</th>
                      <th>Total sales</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(summary.salesByUser || []).map((row) => (
                      <tr key={row.username}>
                        <td>{row.username}</td>
                        <td>{row.saleCount}</td>
                        <td className="strong amount-cell">{money(row.totalSales)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>
          </div>
        </>
      )}
    </section>
  );
}