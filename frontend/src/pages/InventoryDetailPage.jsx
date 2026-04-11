import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchMedicine } from '../api';

export default function InventoryDetailPage() {
  const { id } = useParams();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    load();
  }, [id]);

  async function load() {
    try {
      const data = await fetchMedicine(id);
      setItem(data);
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }

  if (error) {
    return (
      <section>
        <p className="error">{error}</p>
        <Link to="/inventory">Back to inventory</Link>
      </section>
    );
  }

  if (!item) {
    return <p>Loading details...</p>;
  }

  return (
    <section className="panel">
      <h2>{item.name}</h2>
      <p><strong>Batch:</strong> {item.batchNumber}</p>
      <p><strong>Expiry:</strong> {item.expiryDate}</p>
      <p><strong>Supplier:</strong> {item.supplier}</p>
      <p><strong>Unit:</strong> {item.unitType}</p>
      <p><strong>Purchase Price:</strong> {item.purchasePrice}</p>
      <p><strong>Selling Price:</strong> {item.sellingPrice}</p>
      <p><strong>Profit / Unit:</strong> {(Number(item.sellingPrice || 0) - Number(item.purchasePrice || 0)).toFixed(2)}</p>
      <p><strong>Quantity:</strong> {item.quantity}</p>
      <Link to="/inventory">Back to inventory</Link>
    </section>
  );
}

