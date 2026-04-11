# API Contract (Inventory View)

## Authentication

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
  "role": "ADMIN"
}
```

## List Inventory

`GET /inventory`

Headers:

- `Authorization: Bearer <jwt>`

Response (sample):

```json
[
  {
    "id": 1,
    "name": "Paracetamol",
    "batchNumber": "B001",
    "expiryDate": "2026-10-31",
    "supplier": "ABC Pharma",
    "purchasePrice": 20.00,
    "sellingPrice": 30.00,
    "quantity": 100
  }
]
```

## Inventory Detail

`GET /inventory/{id}`

Headers:

- `Authorization: Bearer <jwt>`

Response (sample):

```json
{
  "id": 1,
  "name": "Paracetamol",
  "batchNumber": "B001",
  "expiryDate": "2026-10-31",
  "supplier": "ABC Pharma",
  "purchasePrice": 20.00,
  "sellingPrice": 30.00,
  "quantity": 100
}
```

## Sales Summary (Day/Week/Month/Year)

`GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Headers:

- `Authorization: Bearer <jwt>`

Response (sample):

```json
{
  "period": "MONTH",
  "from": "2026-04-01",
  "to": "2026-04-30",
  "saleCount": 14,
  "totalSales": 25400.00,
  "totalCost": 17320.00,
  "totalProfit": 8080.00
}
```

## Create Prescription Sale

`POST /sales`

Headers:

- `Authorization: Bearer <jwt>`

Request (sample):

```json
{
  "customerName": "Nimal Perera",
  "customerPhone": "0771234567",
  "discountAmount": 50.00,
  "items": [
    {
      "medicineId": 1,
      "medicineName": "Paracetamol 500mg",
      "quantity": 10,
      "unitType": "tablets",
      "pricePerUnit": 25.00
    }
  ]
}
```

Response includes full bill details with transaction ID and line totals.

## Transaction History / Search

`GET /sales?transactionId=&fromDate=&toDate=`

Headers:

- `Authorization: Bearer <jwt>`

Query params are optional and support date filtering (`YYYY-MM-DD`) and transaction ID search.

## Get Bill by Transaction ID

`GET /sales/{transactionId}`

Headers:

- `Authorization: Bearer <jwt>`

