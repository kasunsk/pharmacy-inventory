# Planning and Refactoring Strategy

## 1) High-Level Vision
Build a tenant-aware Pharmacy Management System that is:
- Secure by default (JWT + RBAC + tenant isolation)
- Operationally reliable (billing/inventory consistency and clear errors)
- Easy to scale to multiple pharmacy organizations (tenant lifecycle + feature flags)
- Maintainable (service-layer rules, thin controllers, synced docs)

## 2) Current Functional Scope
- **Authentication & Authorization**
  - `username@tenant` login for tenant users
  - dedicated `super_admin` login for admin portal
  - multi-role RBAC (`SUPER_ADMIN`, `ADMIN`, `BILLING`, `TRANSACTIONS`, `INVENTORY`)
- **Tenant Administration**
  - create tenant + auto-create tenant admin
  - enable/disable tenant
  - per-tenant feature toggles (billing, transactions, inventory, analytics, AI)
  - tenant audit log endpoints
- **Pharmacy Operations**
  - inventory CRUD + alerts
  - billing + transaction history + analytics
  - user management per tenant

## 3) Architecture State
- **Presentation Layer**: Spring REST controllers + React pages
- **Business Layer**: `AuthService`, `TenantService`, `InventoryService`, `SalesService`, `EmployeeService`
- **Data Layer**: JPA entities/repositories with tenant-aware queries
- **Security Model**: JWT contains username + optional tenantId (null for super admin)

## 4) Major Changes Delivered

### Wave A - Identity and Access
- Replaced legacy login with `username@tenant`
- Added `SUPER_ADMIN` role and separate super admin login page
- Added null-tenant principal handling for super admin token flow

### Wave B - Tenant Platform
- Added tenant status and module feature flags
- Added admin portal routes: `/admin-portal/tenants` (with compatibility alias)
- Added tenant config/status update APIs
- Added tenant audit entity + endpoints (`/admin-portal/tenants/audits`)

### Wave C - Frontend UX and Guarding
- Added separate staff/super-admin login experience
- Added feature-based navigation and module gating
- Refactored tenant management to icon actions + modal config
- Removed tenant user assignment section from admin portal UI

### Wave D - Reliability Fixes
- Fixed inventory 500 caused by lazy tenant proxy serialization (`Medicine.tenant` ignored in JSON)
- Added structured bad-request handling for duplicate batch constraints
- Added stricter gender input contracts (`MALE`/`FEMALE`) for user creation and tenant admin creation

## 5) Recent Incident Notes
- **Issue:** `POST /inventory` returned 500 (`ByteBuddyInterceptor` serialization failure)
- **Root Cause:** Jackson attempted to serialize lazy `tenant` proxy from `Medicine`
- **Fix:** Annotated `Medicine.tenant`/getter with ignore for JSON serialization
- **Validation:** live login + create inventory + list inventory succeeded after patch

## 6) Roadmap (Next)
- Add dedicated audit dashboard (filters, trend charts, usage insights)
- Expand automated integration tests for tenant role matrix and inventory flows
- Add optimistic locking for high-concurrency inventory updates
- Move remaining map-based endpoints to typed response DTOs

## 7) Known Limitations
- Audit trail dashboard UI is still basic table-first; monitoring visuals are pending
- Some backward-compatibility code paths remain for legacy tenant configurations
- API docs and Postman examples still need periodic contract sync checks

## 8) Delivery Discipline
- Keep tenant and role checks centralized in services/guards
- Keep controllers orchestration-only
- Validate DTO contracts at boundary (server-side) even if frontend validates
- Ship docs updates in same change set as API/UX contract updates
