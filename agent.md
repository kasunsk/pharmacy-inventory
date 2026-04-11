# AGENT.md
# Sri Lanka Pharmacy Management System - AI Agent Guide

## Overview
This project is a Pharmacy Management System for Sri Lanka.

Current implementation is a split monorepo:
- `backend/`: Spring Boot (Gradle, Java 17)
- `frontend/`: React + Vite

It supports inventory, prescription-based sales, billing, transaction history, file upload, and analytics.

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
- Track medicine name, batch, expiry, supplier, purchase/selling prices, quantity
- Alerts:
  - Low stock
  - Upcoming expiry

### Prescription-Based Sales and Billing
- Create one sale with multiple medicine lines
- Each line captures:
  - Medicine
  - Quantity
  - Unit type (tablets, capsules, bottle, etc.)
  - Price per unit
- Auto line total and overall totals
- Optional customer details (name, phone)
- Stock validation before checkout
- Automatic inventory deduction after successful sale
- Prevent sale on insufficient stock with clear error message

### Transaction Records and Retrieval
- Every sale is saved with unique `transactionId`
- Transaction includes date/time, items, and totals
- History retrieval:
  - Search by transaction ID
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

### Files (Receipt/Prescription)
- Upload and store receipt/prescription files linked to sales
- View via signed URL endpoint

---

## API Highlights
- `POST /auth/login`
- `GET /inventory`
- `GET /inventory/{id}`
- `POST /sales`
- `GET /sales?transactionId=&fromDate=&toDate=`
- `GET /sales/{transactionId}`
- `GET /sales/summary?period=DAY|WEEK|MONTH|YEAR`

Detailed API examples are in `docs/api.md`.

---

## Frontend UX Highlights
- Inventory page with create/search/view flow
- Prescription Sales page with fast multi-line billing
- Immediate bill display after sale
- Transaction history and bill reopening
- Print-ready bill from browser print
- Sales Analytics dashboard for day/week/month/year

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
