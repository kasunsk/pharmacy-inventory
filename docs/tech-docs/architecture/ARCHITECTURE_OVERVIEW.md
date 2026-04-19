# Architecture Overview

This document summarizes the high-level architecture of the pharmacy inventory system.

## Layers

- Frontend: React + Vite UI
- Backend: Spring Boot REST services
- Data: H2/PostgreSQL with Flyway migrations

## Multi-tenant Model

- Tenant-level isolation
- Pharmacy-level context switching
- Role-based authorization for module access

## Integration Points

- Authentication (`/auth/*`)
- Inventory operations (`/inventory/*`)
- Billing and sales (`/sales/*`)
- Documentation (`/docs/tech/*`)

