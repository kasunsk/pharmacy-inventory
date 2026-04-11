# Pharmacy Inventory Monorepo

This project is now split into two separate modules:

- `backend`: Spring Boot API with Gradle
- `frontend`: React app with Vite

## Project Structure

- `backend/` - Java 17, Spring Boot, JWT/RBAC, inventory/sales/files/AI APIs
- `frontend/` - React inventory dashboard with list + detail pages

## Inventory View Flow

1. User signs in from frontend (`admin` / `admin123` by default).
2. Frontend calls `GET /inventory` and displays all medicines.
3. User opens item details via `GET /inventory/{id}`.

## Run Backend

```powershell
Set-Location "C:\Users\kasun\OneDrive\Desktop\Projects\pharmacy-inventory\backend"
gradle bootRun
```

## Run Frontend

```powershell
Set-Location "C:\Users\kasun\OneDrive\Desktop\Projects\pharmacy-inventory\frontend"
npm install
npm run dev
```

See module-specific docs in `backend/README.md` and `frontend/README.md`.

