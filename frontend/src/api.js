const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function authHeaders() {
  const token = localStorage.getItem('token');
  if (!token) {
    return {};
  }
  return { Authorization: `Bearer ${token}` };
}

async function parseApiError(response, fallbackMessage) {
  let message = fallbackMessage;
  try {
    const body = await response.json();
    if (body?.message) {
      message = body.message;
    }
  } catch (_) {
  }
  const error = new Error(message);
  error.status = response.status;
  return error;
}

export async function login(username, password) {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Login failed. Check credentials.');
  }
  return response.json();
}

export async function fetchInventory(options) {
  const isPagedRequest = Boolean(options);
  const { page = 0, size = 10 } = options || {};
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  const response = await fetch(`${API_BASE}/inventory?${params.toString()}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch inventory.');
  }
  const data = await response.json();
  return isPagedRequest ? data : data.content || data;
}

export async function fetchBillingMedicines() {
  const response = await fetch(`${API_BASE}/sales/billing-medicines`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Unable to load medicines for billing.');
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
    throw await parseApiError(response, 'Failed to fetch medicine details.');
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
    throw await parseApiError(response, 'Failed to create inventory record.');
  }
  return response.json();
}

export async function updateMedicine(id, payload) {
  const response = await fetch(`${API_BASE}/inventory/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to update inventory record.');
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
    throw await parseApiError(response, 'Failed to fetch sales summary.');
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
    throw await parseApiError(response, 'Failed to create sale.');
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
  if (filters?.page !== undefined) {
    params.set('page', String(filters.page));
  }
  if (filters?.size !== undefined) {
    params.set('size', String(filters.size));
  }

  const response = await fetch(`${API_BASE}/sales?${params.toString()}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch transactions.');
  }
  const data = await response.json();
  return filters?.page !== undefined || filters?.size !== undefined ? data : data.content || data;
}

export async function fetchTransactionBill(transactionId) {
  const response = await fetch(`${API_BASE}/sales/${encodeURIComponent(transactionId)}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch bill details.');
  }
  return response.json();
}

export async function fetchEmployees(options) {
  const isPagedRequest = Boolean(options);
  const { page = 0, size = 10 } = options || {};
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  const response = await fetch(`${API_BASE}/employees?${params.toString()}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch users.');
  }
  const data = await response.json();
  return isPagedRequest ? data : data.content || data;
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
    throw await parseApiError(response, 'Failed to create user.');
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
    throw await parseApiError(response, 'Failed to update user.');
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
    throw await parseApiError(response, 'Failed to delete user.');
  }
}

export async function fetchProfile() {
  const response = await fetch(`${API_BASE}/profile`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch profile.');
  }
  return response.json();
}

export async function updateProfile(payload) {
  const response = await fetch(`${API_BASE}/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to update profile.');
  }
  return response.json();
}

export async function askAiAssistant(payload) {
  const response = await fetch(`${API_BASE}/ai/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'AI assistant is unavailable right now.');
  }
  return response.json();
}

