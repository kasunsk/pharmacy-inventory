const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function emptyPage() {
  return {
    content: [],
    totalElements: 0,
    totalPages: 1,
    number: 0,
    size: 10
  };
}

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

export async function selectPharmacy(pharmacyId) {
  const response = await fetch(`${API_BASE}/auth/pharmacy/select`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ pharmacyId })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to select pharmacy.');
  }
  return response.json();
}

export async function setDefaultPharmacy(pharmacyId) {
  const response = await fetch(`${API_BASE}/auth/pharmacy/default`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ pharmacyId })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to set default pharmacy.');
  }
  return response.json();
}

export async function fetchMyPharmacies() {
  const response = await fetch(`${API_BASE}/pharmacies/my`, {
    headers: { ...authHeaders() }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to load pharmacies.');
  }
  return response.json();
}

export async function fetchTenants() {
  const response = await fetch(`${API_BASE}/admin-portal/tenants`, {
    headers: { ...authHeaders() }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch tenants.');
  }
  return response.json();
}

export async function createTenant(payload) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to create tenant.');
  }
  return response.json();
}

export async function fetchTenantUsers() {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/users`, {
    headers: { ...authHeaders() }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch tenant users.');
  }
  return response.json();
}

export async function assignUserToTenant(userId, tenantId) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/users/${userId}/assignment`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ tenantId })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to assign user to tenant.');
  }
  return response.json();
}

export async function updateTenantStatus(tenantId, enabled) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ enabled })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to update tenant status.');
  }
  return response.json();
}

export async function updateTenantConfig(tenantId, config) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/config`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(config)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to update tenant configuration.');
  }
  return response.json();
}

export async function fetchTenantAudits(limit = 50) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/audits?limit=${encodeURIComponent(limit)}`, {
    headers: { ...authHeaders() }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch tenant audit logs.');
  }
  return response.json();
}

export async function fetchTenantPharmacies(tenantId, enabledOnly = false) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/pharmacies?enabledOnly=${encodeURIComponent(enabledOnly)}`, {
    headers: { ...authHeaders() }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch tenant pharmacies.');
  }
  return response.json();
}

export async function createTenantPharmacy(tenantId, payload) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/pharmacies`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to create pharmacy.');
  }
  return response.json();
}

export async function updateTenantPharmacyStatus(tenantId, pharmacyId, enabled) {
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/pharmacies/${pharmacyId}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ enabled })
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to update pharmacy status.');
  }
  return response.json();
}

export async function uploadTenantLogo(tenantId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/logo`, {
    method: 'POST',
    headers: { ...authHeaders() },
    body: formData
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to upload tenant logo.');
  }
}

export async function uploadPharmacyLogo(tenantId, pharmacyId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${API_BASE}/admin-portal/tenants/${tenantId}/pharmacies/${pharmacyId}/logo`, {
    method: 'POST',
    headers: { ...authHeaders() },
    body: formData
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to upload pharmacy logo.');
  }
  return response.json();
}

async function fetchLogoAsObjectUrl(url) {
  const response = await fetch(url, { headers: { ...authHeaders() } });
  if (!response.ok) {
    throw await parseApiError(response, 'Logo is unavailable.');
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}

export async function fetchTenantLogoUrl(tenantId) {
  return fetchLogoAsObjectUrl(`${API_BASE}/admin-portal/tenants/${tenantId}/logo`);
}

export async function fetchPharmacyLogoUrl(tenantId, pharmacyId) {
  return fetchLogoAsObjectUrl(`${API_BASE}/admin-portal/tenants/${tenantId}/pharmacies/${pharmacyId}/logo`);
}

export async function fetchCurrentTenantLogoUrl() {
  return fetchLogoAsObjectUrl(`${API_BASE}/branding/tenant/logo`);
}

export async function fetchCurrentPharmacyLogoUrl() {
  return fetchLogoAsObjectUrl(`${API_BASE}/branding/pharmacy/logo`);
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
  if (response.status === 404 || response.status === 204) {
    return isPagedRequest ? emptyPage() : [];
  }
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch inventory.');
  }
  const data = await response.json();
  return isPagedRequest ? data : data.content || data;
}

export async function fetchInventoryAlertsSummary(options = {}) {
  const lowStockThreshold = options.lowStockThreshold ?? 10;
  const expiryDays = options.expiryDays ?? 30;
  const params = new URLSearchParams({
    lowStockThreshold: String(lowStockThreshold),
    expiryDays: String(expiryDays)
  });
  const response = await fetch(`${API_BASE}/inventory/alerts/summary?${params.toString()}`, {
    headers: {
      ...authHeaders()
    }
  });
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to fetch inventory alerts summary.');
  }
  return response.json();
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
  if (response.status === 404 || response.status === 204) {
    return {
      from: '-',
      to: '-',
      saleCount: 0,
      totalSales: 0,
      totalCost: 0,
      totalProfit: 0,
      topSellingMedicines: [],
      salesByUser: []
    };
  }
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
  if (response.status === 404 || response.status === 204) {
    return filters?.page !== undefined || filters?.size !== undefined ? emptyPage() : [];
  }
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
  if (response.status === 404 || response.status === 204) {
    return isPagedRequest ? emptyPage() : [];
  }
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

export async function fetchDocsCatalog() {
  const response = await fetch(`${API_BASE}/docs/tech/list`);
  if (!response.ok) {
    throw await parseApiError(response, 'Failed to load documentation catalog.');
  }
  return response.json();
}

export async function fetchDocContent(path, options = {}) {
  const params = new URLSearchParams();
  if (options.domain) {
    params.set('domain', options.domain);
  }
  params.set('format', options.format || 'text');
  const response = await fetch(`${API_BASE}/docs/tech/${encodeURI(path)}?${params.toString()}`);
  if (!response.ok) {
    throw await parseApiError(response, `Failed to load document: ${path}`);
  }
  if ((options.format || 'text') === 'json') {
    return response.json();
  }
  return response.text();
}

export async function searchDocs(keyword, options = {}) {
  const params = new URLSearchParams({ keyword });
  if (options.domain) {
    params.set('domain', options.domain);
  }
  if (options.category) {
    params.set('category', options.category);
  }
  const response = await fetch(`${API_BASE}/docs/tech/search?${params.toString()}`);
  if (!response.ok) {
    throw await parseApiError(response, 'Documentation search failed.');
  }
  return response.json();
}

