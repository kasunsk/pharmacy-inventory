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

### Admin
- Full access
- Manage employees
- View all analytics (sales, cost, profit, inventory)

### Employer (Pharmacist / Staff)
- Handle daily operations
- Create sales and bills
- Manage inventory
- View operational analytics

---

## Authentication and Security
- JWT-based login (`POST /auth/login`)
- RBAC with roles: `ADMIN`, `EMPLOYER`
- Password hashing with BCrypt

Default seed user:
- Username: `admin`
- Password: `admin123`

---

## Core Modules (Current State)

### Inventory Management
- Create, update, delete medicines
- Track medicine name, batch, expiry, supplier, unit type, purchase/selling prices, quantity
- Alerts:
  - Low stock
  - Upcoming expiry

### Billing Workflow
- Create one sale with multiple medicine lines
- Each line captures:
  - Medicine
  - Unit (auto-loaded from inventory)
  - Price per unit
  - Quantity
  - Usage instruction
  - Optional remark
- Auto line total and overall totals
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
- `GET /inventory`
- `GET /inventory/{id}`
- `POST /sales`
- `GET /sales?transactionId=&salesPerson=&fromDate=&toDate=`
- `GET /sales/{transactionId}`
- `GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Detailed API examples are in `docs/api.md`.

---

## Frontend UX Highlights
- Separate Login page with JWT session handling
- Default redirect to Billing after successful login
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
- `SaleItemRequest` now supports per-line `remark`
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
