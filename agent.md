# AGENT.md
# Sri Lanka Pharmacy Management System - AI Agent Guide

## Overview
This project is a Pharmacy Management System for Sri Lanka.

Current implementation is a split monorepo:
- `backend/`: Spring Boot (Gradle, Java 17)
- `frontend/`: React + Vite

It supports inventory, billing, transaction history, file upload, and analytics.

---

## User Roles

### SUPER_ADMIN
- Global system access
- Manage tenants in Admin Portal
- Enable/disable tenant modules and tenant status

### ADMIN (Tenant)
- Full access within own tenant
- Manage tenant users
- View tenant analytics and operations

### BILLING / TRANSACTIONS / INVENTORY
- Module-scoped access within own tenant

---

## Authentication and Security
- JWT-based login (`POST /auth/login`)
- Tenant staff login format: `username@tenant`
- Super admin login: `super_admin`
- RBAC with roles: `SUPER_ADMIN`, `ADMIN`, `BILLING`, `TRANSACTIONS`, `INVENTORY`
- Password hashing with BCrypt

Default seed user:
- Tenant admin: `admin@demo` / `admin@123`
- Super admin: `super_admin` / `admin@123`

---

## Core Modules (Current State)

### Admin Portal (Super Admin)
- Route: `/admin-portal/tenants`
- Tenant lifecycle management (create, enable/disable)
- Tenant module configuration (billing/transactions/inventory/analytics/AI)
- Tenant audit listing

### Inventory Management
- Create, update, delete medicines
- Track medicine name, batch, expiry, supplier, unit type, allowed units, purchase/selling prices, quantity
- Alerts:
  - Low stock
  - Upcoming expiry
- Duplicate batch protection per tenant and pharmacy (`tenant_id + pharmacy_id + batch_number`)

### Billing Workflow
- Create one sale with multiple medicine lines
- Each line captures:
  - Medicine
  - Unit (must match inventory-configured allowed units)
  - Price per unit
  - Quantity
  - Usage instruction
  - Optional remark
- Auto line totals and bill-level totals
- Discount is applied at bill level (`discountAmount`), not per line
- Optional customer details (name, phone)
- Stock validation before checkout
- Automatic inventory deduction after successful sale
- Prevent sale on insufficient stock with clear error message

### Transaction Records and Retrieval
- Every sale is saved with unique `transactionId`
- Transaction includes date/time, sales person, item list, and totals
- History retrieval:
  - Search by transaction ID
  - Filter by sales person
  - Filter by date range
- Open a past transaction and view full bill
- Usage instructions are visible in transaction details and not shown in billing preview table

### Sales Analytics
- Summary endpoint supports period views:
  - `DAY`, `WEEK`, `MONTH`, `YEAR`
- Metrics:
  - Total sales
  - Total cost
  - Total profit
  - Sale count
- Breakdowns:
  - Top-selling medicines
  - Sales by user

### Files (Receipt/Prescription)
- Upload and store receipt/prescription files linked to sales
- View via signed URL endpoint

---

## API Highlights
- `POST /auth/login`
- `GET /admin-portal/tenants`
- `POST /admin-portal/tenants`
- `PUT /admin-portal/tenants/{tenantId}/status`
- `PUT /admin-portal/tenants/{tenantId}/config`
- `GET /admin-portal/tenants/audits`
- `GET /inventory`
- `GET /inventory/{id}`
- `POST /sales`
- `GET /sales?transactionId=&fromDate=&toDate=`
- `GET /sales/{transactionId}`
- `GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Detailed API examples are in `docs/api.md`.

---

## Frontend UX Highlights
- Separate staff login and super-admin login pages
- Super admin routed to admin portal, tenant users routed to operations app
- Role-protected navigation tabs
  - Billing
  - Inventory
  - Transaction History
  - Sales Analytics
- Inventory page with compact row-based create/search/view flow
- Billing page with fast row-based multi-line entry
- Immediate bill display after sale
- Dedicated Transaction History page with filters and bill reopening
- Print-ready bill from browser print
- Sales Analytics dashboard for day/week/month/year with user and medicine breakdowns

---

## Backend Contract Notes (Recent)
- Login response includes tenant identity and per-module feature flags
- Tenant admin creation requires `adminGender` = `MALE` or `FEMALE`
- Employee creation requires `gender` = `MALE` or `FEMALE`
- `Medicine.tenant` is excluded from JSON serialization to prevent lazy-proxy response failures
- `SaleItemRequest` now supports per-line `remark`
- `CreateSaleRequest.discountAmount` is bill-level discount
- Sale item `unitType` must be one of the medicine `allowedUnits`
- `SaleBillItemResponse` now returns `remark`
- `SaleBillResponse` now returns `salesPerson`
- `SaleTransactionSummaryResponse` now returns `salesPerson` and `medicines`
- `SalesSummaryResponse` now returns `topSellingMedicines` and `salesByUser`
- `Medicine` now includes `unitType` for inventory and billing auto-load consistency

---

## AI Assistant Integration
Purpose: natural language queries over pharmacy operations.

Supported intent examples:
- Low stock medicines
- Today's sales
- Top-selling medicine
- Availability checks by medicine name

---

## Environment Configuration
- OpenRouter key stored in `.env`:
  - `OPENROUTER_API_KEY=...`

---

## Future Enhancements
- OCR for prescriptions
- PDF-rendered bills/receipts
- Richer dashboards and trend charts
- Stronger production file storage strategy
