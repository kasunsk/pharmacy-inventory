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

export async function fetchSalesSummary(period) {
  const response = await fetch(`${API_BASE}/sales/summary?period=${encodeURIComponent(period)}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch sales summary.');
  }
  return response.json();
}

export async function createPrescriptionSale(payload) {
  const response = await fetch(`${API_BASE}/sales`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    let message = 'Failed to create sale.';
    try {
      const body = await response.json();
      if (body.message) {
        message = body.message;
      }
    } catch (_) {
    }
    throw new Error(message);
  }
  return response.json();
}

export async function fetchTransactions(filters) {
  const params = new URLSearchParams();
  if (filters?.transactionId) {
    params.set('transactionId', filters.transactionId);
  }
  if (filters?.salesPerson) {
    params.set('salesPerson', filters.salesPerson);
  }
  if (filters?.fromDate) {
    params.set('fromDate', filters.fromDate);
  }
  if (filters?.toDate) {
    params.set('toDate', filters.toDate);
  }

  const response = await fetch(`${API_BASE}/sales?${params.toString()}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch transactions.');
  }
  return response.json();
}

export async function fetchTransactionBill(transactionId) {
  const response = await fetch(`${API_BASE}/sales/${encodeURIComponent(transactionId)}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch bill details.');
  }
  return response.json();
}

export async function fetchEmployees() {
  const response = await fetch(`${API_BASE}/employees`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to fetch users.');
  }
  return response.json();
}

export async function createEmployee(payload) {
  const response = await fetch(`${API_BASE}/employees`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error('Failed to create user.');
  }
  return response.json();
}

export async function updateEmployee(id, payload) {
  const response = await fetch(`${API_BASE}/employees/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new Error('Failed to update user.');
  }
  return response.json();
}

export async function deleteEmployee(id) {
  const response = await fetch(`${API_BASE}/employees/${id}`, {
    method: 'DELETE',
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw new Error('Failed to delete user.');
  }
}

