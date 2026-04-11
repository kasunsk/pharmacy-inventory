import { Link, Route, Routes } from 'react-router-dom';
import InventoryListPage from './pages/InventoryListPage';
import InventoryDetailPage from './pages/InventoryDetailPage';
import PrescriptionSalesPage from './pages/PrescriptionSalesPage';
import SalesAnalyticsPage from './pages/SalesAnalyticsPage';

export default function App() {
  return (
    <div className="app-shell">
      <header>
        <h1>Pharmacy Inventory</h1>
        <nav>
          <Link to="/inventory">Inventory</Link>
          <Link to="/sales">Prescription Sales</Link>
          <Link to="/sales-analytics">Sales Analytics</Link>
        </nav>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<InventoryListPage />} />
          <Route path="/inventory" element={<InventoryListPage />} />
          <Route path="/inventory/:id" element={<InventoryDetailPage />} />
          <Route path="/sales" element={<PrescriptionSalesPage />} />
          <Route path="/sales-analytics" element={<SalesAnalyticsPage />} />
        </Routes>
      </main>
    </div>
  );
}

