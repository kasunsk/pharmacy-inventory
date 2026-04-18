# API Documentation

Base URL: `http://localhost:8080`

## Authentication

### Login

`POST /auth/login`

Tenant user request:

```json
{
  "username": "admin@demo",
  "password": "admin@123"
}
```

Super admin request:

```json
{
  "username": "super_admin",
  "password": "admin@123"
}
```

Response:

```json
{
  "token": "<jwt>",
  "username": "admin",
  "roles": ["ADMIN"],
  "tenantId": 1,
  "tenantCode": "DEMO",
  "tenantName": "DEMO",
  "tenantHasLogo": true,
  "selectedPharmacyId": 10,
  "selectedPharmacyName": "Demo Pharmacy P/L Badulla",
  "requiresPharmacySelection": false,
  "availablePharmacies": [
    { "id": 10, "code": "MAIN", "name": "Demo Pharmacy P/L Badulla", "enabled": true, "hasLogo": true },
    { "id": 11, "code": "HALIELA", "name": "Demo HaliEla Medicine", "enabled": true, "hasLogo": false }
  ],
  "billingEnabled": true,
  "transactionsEnabled": true,
  "inventoryEnabled": true,
  "analyticsEnabled": true,
  "aiAssistantEnabled": true
}
```

For `SUPER_ADMIN`, tenant fields are `null` and feature flags are `false`.

### Select Active Pharmacy

`POST /auth/pharmacy/select`

```json
{
  "pharmacyId": 10
}
```

Returns a refreshed login payload with updated JWT and selected pharmacy.

### Set My Default Pharmacy

`POST /auth/pharmacy/default`

```json
{
  "pharmacyId": 10
}
```

## Pharmacy Context

### List Pharmacies For Current User

`GET /pharmacies/my`

Returns pharmacies accessible to the signed-in tenant user.

## Admin Portal (SUPER_ADMIN)

All endpoints below require `ROLE_SUPER_ADMIN`.

### List Tenants

`GET /admin-portal/tenants`

### Create Tenant

`POST /admin-portal/tenants`

```json
{
  "code": "NEW01",
  "name": "New Pharmacy",
  "adminUsername": "admin",
  "adminFirstName": "Nimal",
  "adminLastName": "Perera",
  "adminEmail": "admin@newpharmacy.lk",
  "adminPassword": "admin123",
  "adminGender": "MALE"
}
```

`adminGender` must be `MALE` or `FEMALE`. `adminFirstName`, `adminLastName`, and `adminEmail` are required.

### Update Tenant Status

`PUT /admin-portal/tenants/{tenantId}/status`

```json
{
  "enabled": false
}
```

### Update Tenant Feature Config

`PUT /admin-portal/tenants/{tenantId}/config`

```json
{
  "billingEnabled": true,
  "transactionsEnabled": true,
  "inventoryEnabled": true,
  "analyticsEnabled": true,
  "aiAssistantEnabled": false
}
```

### Tenant Audits

`GET /admin-portal/tenants/audits?limit=50`

Compatibility alias for admin portal endpoints is also available under `/super-admin/tenants`.

### List Tenant Pharmacies

`GET /admin-portal/tenants/{tenantId}/pharmacies?enabledOnly=false`

### Create Tenant Pharmacy

`POST /admin-portal/tenants/{tenantId}/pharmacies`

```json
{
  "name": "Borella Branch"
}
```

Pharmacy code is generated automatically by the system (for example, `PH001`, `PH002`, ...).

### Update Pharmacy Status

`PUT /admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/status`

```json
{
  "enabled": true
}
```

### Upload Tenant Logo

`POST /admin-portal/tenants/{tenantId}/logo` (multipart form-data, `file`)

### View Tenant Logo

`GET /admin-portal/tenants/{tenantId}/logo`

### Upload Pharmacy Logo

`POST /admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/logo` (multipart form-data, `file`)

### View Pharmacy Logo

`GET /admin-portal/tenants/{tenantId}/pharmacies/{pharmacyId}/logo`

### Current Tenant Logo (Authenticated Tenant User)

`GET /branding/tenant/logo`

### Current Pharmacy Logo (Authenticated Tenant User)

`GET /branding/pharmacy/logo`

## Users (Admin)

All `/employees` endpoints require `ROLE_ADMIN`.

### List Users

`GET /employees`

Response:

```json
{
  "content": [
    {
      "id": 1,
      "username": "admin",
      "roles": ["ADMIN"],
      "enabled": true,
      "gender": "MALE"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### Create User

`POST /employees`

```json
{
  "username": "cashier1",
  "password": "pass123",
  "roles": ["BILLING", "TRANSACTIONS"],
  "pharmacyIds": [10, 11],
  "defaultPharmacyId": 10,
  "gender": "FEMALE"
}
```

`gender` must be `MALE` or `FEMALE`.

### Update User

`PUT /employees/{id}`

```json
{
  "roles": ["INVENTORY"],
  "pharmacyIds": [10],
  "defaultPharmacyId": 10,
  "enabled": true
}
```

### Delete User

`DELETE /employees/{id}`

## Inventory

Requires role:
- list/read: `ADMIN`, `INVENTORY`, `BILLING`, or `TRANSACTIONS`
- create/update/delete: `ADMIN` or `INVENTORY`

Also requires tenant inventory feature to be enabled.

### List Inventory

`GET /inventory`

### Get Medicine

`GET /inventory/{id}`

### Create Medicine

`POST /inventory`

```json
{
  "name": "Paracetamol 500mg",
  "batchNumber": "SL-PARA-101",
  "expiryDate": "2027-01-31",
  "supplier": "Hemas Pharma",
  "unitType": "tablet",
  "purchasePrice": 18.0,
  "sellingPrice": 25.0,
  "quantity": 150
}
```

Notes:
- Batch number must be unique per tenant and pharmacy (`tenant_id + pharmacy_id + batch_number`).
- Duplicate batch returns a `400` error with a clear business message.

### Update Medicine

`PUT /inventory/{id}`

### Delete Medicine

`DELETE /inventory/{id}`

### Low Stock Alert

`GET /inventory/alerts/low-stock?threshold=10`

### Expiry Alert

`GET /inventory/alerts/expiry?days=30`

### Alerts Summary

`GET /inventory/alerts/summary?lowStockThreshold=10&expiryDays=30`

Response:

```json
{
  "lowStockCount": 4,
  "expiringSoonCount": 3,
  "lowStockThreshold": 10,
  "expiryWithinDays": 30,
  "expiryCutoffDate": "2026-05-18"
}
```

## Billing and Transactions

### Create Bill (Sale)

`POST /sales`

Requires role: `ADMIN` or `BILLING`

```json
{
  "customerName": "Nimal Perera",
  "customerPhone": "0771234567",
  "discountAmount": 0,
  "items": [
    {
      "medicineId": 1,
      "medicineName": "Paracetamol 500mg",
      "quantity": 2,
      "unitType": "tablet",
      "pricePerUnit": 24.5,
      "allowPriceOverride": true,
      "dosageInstruction": "Oral - Take 1 tablet once daily after meals",
      "customDosageInstruction": null,
      "remark": "After dinner"
    }
  ]
}
```

### Transaction History

`GET /sales?transactionId=&salesPerson=&fromDate=&toDate=`

Requires role: `ADMIN` or `TRANSACTIONS`

### Bill by Transaction ID

`GET /sales/{transactionId}`

Requires role: `ADMIN` or `TRANSACTIONS`

### Sales Summary

`GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Requires role: `ADMIN` or `TRANSACTIONS`

Response includes:
- `saleCount`
- `totalSales`
- `totalCost`
- `totalProfit`
- `topSellingMedicines`
- `salesByUser`

## Files

Requires role: `ADMIN`, `BILLING`, or `TRANSACTIONS`.

- `POST /files/sales/{saleId}/receipt`
- `POST /files/sales/{saleId}/prescription`
- `GET /files/{fileId}/signed-url`
- `GET /files/view/{fileId}?token=...` (public signed view)

## AI Query

Requires role: `ADMIN` or `TRANSACTIONS`.

`POST /ai/query`

## AI Assistant Chat

Requires role: `ADMIN`, `BILLING`, `INVENTORY`, or `TRANSACTIONS`.

`POST /ai/chat`

```json
{
  "query": "Show profit for last 7 days",
  "sessionId": "default-session",
  "history": [
    { "role": "user", "content": "Do we have Paracetamol?" },
    { "role": "assistant", "content": "Yes, stock is available." }
  ]
}
```

Response (sample):

```json
{
  "intent": "profit_analysis",
  "answer": "Profit summary for the period is ...",
  "quickActions": ["Show profit for last 7 days", "Start a new billing"],
  "data": {
    "from": "2026-04-05",
    "to": "2026-04-11",
    "saleCount": 12,
    "revenue": 25100.0,
    "cost": 17920.0,
    "netProfit": 7180.0
  }
}
```

## Health

`GET /health`

## Error Payload Format

```json
{
  "timestamp": "2026-04-11T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "quantity must be greater than 0",
  "path": "/sales"
}
```

Common examples:
- `400` duplicate inventory batch: `"Duplicate entry: batch number already exists for this tenant"`
- `403` role/feature denied: `"Access denied"` or module-disabled message

