# AGENT.md
# Sri Lanka Pharmacy Management System – AI Agent Guide

## Overview
This project is a Pharmacy Management System designed for Sri Lanka.  
It supports inventory management, sales tracking, receipt generation, prescription uploads, and AI-powered insights.

---

## User Roles

### Admin
- Full access to the system
- Manage employees (create/update/delete)
- View all analytics (sales, profit, inventory)
- Configure system settings

### Employer (Pharmacist / Staff)
- Manage daily operations
- Handle sales and receipts
- Upload prescriptions
- View limited analytics (daily sales, stock)

---

## Authentication
- Use JWT-based authentication
- Implement Role-Based Access Control (RBAC)
- Passwords must be hashed (e.g., BCrypt)

---

## Core Modules

### Inventory Management
- Add, update, delete medicines
- Track:
    - Name
    - Batch number
    - Expiry date
    - Supplier
    - Purchase price
    - Selling price
    - Quantity
- Features:
    - Low stock alerts
    - Expiry alerts

---

### Sales Management
- Create sales transactions
- Select medicines from inventory
- Auto-calculate:
    - Total price
    - Discounts
- Deduct stock automatically

---

### Receipt Management
- Generate digital receipts
- Include:
    - Date & time
    - Items
    - Quantity & price
    - Total amount
- Export as PDF / print-ready format

---

### Prescription Upload
- Upload image or PDF
- Link to a sale
- Store securely
- Optional: OCR (future enhancement)

---

## AI Assistant Integration

### Purpose
Provide a natural language interface to query pharmacy data.

### Example Queries
- "What medicines are low in stock?"
- "What is today's total sales?"
- "Show daily profit"
- "Do we have Paracetamol?"
- "Which medicine is selling the most?"

---

## OpenRouter Configuration

API key is stored in `.env` file:
