# API Documentation

Base URL: `http://localhost:8080`

## Authentication

### Login

`POST /auth/login`

Request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response:

```json
{
  "token": "<jwt>",
  "username": "admin",
  "roles": ["ADMIN"]
}
```

## Users (Admin)

All `/employees` endpoints require `ROLE_ADMIN`.

### List Users

`GET /employees`

Response:

```json
[
  {
    "id": 1,
    "username": "admin",
    "roles": ["ADMIN"],
    "enabled": true
  }
]
```

### Create User

`POST /employees`

```json
{
  "username": "cashier1",
  "password": "pass123",
  "roles": ["BILLING", "TRANSACTIONS"]
}
```

### Update User

`PUT /employees/{id}`

```json
{
  "roles": ["INVENTORY"],
  "enabled": true
}
```

### Delete User

`DELETE /employees/{id}`

## Inventory

Requires role: `ADMIN` or `INVENTORY` (delete is `ADMIN` only).

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

### Update Medicine

`PUT /inventory/{id}`

### Delete Medicine

`DELETE /inventory/{id}`

### Low Stock Alert

`GET /inventory/alerts/low-stock?threshold=10`

### Expiry Alert

`GET /inventory/alerts/expiry?days=30`

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
