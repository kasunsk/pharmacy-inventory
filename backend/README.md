# Backend Module (Spring Boot + Gradle)

This module contains the pharmacy backend API with JWT auth, RBAC, inventory, sales, file uploads, and AI query routing.

## Build and Run

```powershell
Set-Location "C:\Users\kasun\OneDrive\Desktop\Projects\pharmacy-inventory\backend"
gradle bootRun
```

## Run Tests

```powershell
Set-Location "C:\Users\kasun\OneDrive\Desktop\Projects\pharmacy-inventory\backend"
gradle test
```

## Key Endpoints for Inventory Viewing

- `POST /auth/login`
- `GET /inventory`
- `GET /inventory/{id}`
- `POST /sales` (create prescription sale + bill response)
- `GET /sales?transactionId=&fromDate=&toDate=` (history/search)
- `GET /sales/{transactionId}` (full bill retrieval)
- `GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Use `Authorization: Bearer <token>` for protected endpoints.

## Seed Data

Schema and seed data are managed by Flyway migrations:

- `backend/src/main/resources/db/migration` (always runs)
- `backend/src/main/resources/db/demo` (runs only when demo flag is enabled)

Demo data flag:

- `app.demo-data.enabled=true` -> inserts DEMO tenant, demo pharmacies, `admin@demo`, and sample inventory
- `app.demo-data.enabled=false` -> skips demo inserts

On startup, Flyway ensures:

- schema is created first
- base system seed (super admin) is applied idempotently
- demo seed runs only if enabled and remains idempotent

