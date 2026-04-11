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

