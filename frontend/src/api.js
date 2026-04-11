const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function authHeaders() {
  const token = localStorage.getItem('token');
  if (!token) {
    return {};
  }
  return { Authorization: `Bearer ${token}` };
}

export async function login(username, password) {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) {
    throw new Error('Login failed. Check credentials.');
  }
  return response.json();
}

export async function fetchInventory() {
  const response = await fetch(`${API_BASE}/inventory`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch inventory.');
  }
  return response.json();
}

export async function fetchMedicine(id) {
  const response = await fetch(`${API_BASE}/inventory/${id}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch medicine details.');
  }
  return response.json();
}

export async function createMedicine(payload) {
  const response = await fetch(`${API_BASE}/inventory`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error('Failed to create inventory record.');
  }
  return response.json();
}

