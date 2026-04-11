import { Link, Route, Routes } from 'react-router-dom';
import InventoryListPage from './pages/InventoryListPage';
import InventoryDetailPage from './pages/InventoryDetailPage';

export default function App() {
  return (
    <div className="app-shell">
      <header>
        <h1>Pharmacy Inventory</h1>
        <nav>
          <Link to="/inventory">Inventory</Link>
        </nav>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<InventoryListPage />} />
          <Route path="/inventory" element={<InventoryListPage />} />
          <Route path="/inventory/:id" element={<InventoryDetailPage />} />
        </Routes>
      </main>
    </div>
  );
}

