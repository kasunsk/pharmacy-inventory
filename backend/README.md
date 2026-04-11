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

Use `Authorization: Bearer <token>` for protected endpoints.

## Seed Data

On startup, the backend seeds:

- default admin user (`admin` / `admin123`)
- 5 pharmacy-related medicine inventory records (if inventory is empty)

