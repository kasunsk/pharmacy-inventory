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
  - dedicated `super_admin` login for admin portal (`/super-admin/login`)
  - multi-role RBAC (`SUPER_ADMIN`, `ADMIN`, `BILLING`, `TRANSACTIONS`, `INVENTORY`, `ANALYTICS`)
- **Tenant Administration**
  - create tenant + auto-create tenant admin
  - enable/disable tenant
  - per-tenant feature toggles (billing, transactions, inventory, analytics, AI)
  - tenant audit log endpoints
  - audit trail dashboard with filters, search, summary cards
- **Pharmacy Operations**
  - inventory CRUD + alerts (role-gated: INVENTORY role required for write)
  - billing + transaction history + analytics
  - user management per tenant
  - AI Assistant (per-tenant toggle)

## 3) Architecture State
- **Presentation Layer**: Spring REST controllers + React pages
- **Business Layer**: `AuthService`, `TenantService`, `InventoryService`, `SalesService`, `EmployeeService`
- **Data Layer**: JPA entities/repositories with tenant-aware queries
- **Security Model**: JWT contains username + optional tenantId (null for super admin)
- **Admin Portal**: Separate UI layout at `/admin-portal` for Super Admin operations

## 4) Major Changes Delivered

### Wave A - Identity and Access
- Replaced legacy login with `username@tenant` format
- Added `SUPER_ADMIN` role and separate super admin login page (`/super-admin/login`)
- Added null-tenant principal handling for super admin token flow
- Tenant users redirected to main app; super admin redirected to admin portal only

### Wave B - Tenant Platform
- Added tenant status and module feature flags
- Added admin portal routes: `/admin-portal/tenants`
- Added tenant config/status update APIs
- Added tenant audit entity + endpoints (`/admin-portal/tenants/audits`)
- Tenant management completely separated from main pharmacy operation tabs

### Wave C - Frontend UX and Guarding
- Added separate staff/super-admin login experience
- Added feature-based navigation and module gating
- Refactored tenant management to icon actions (⚙️ Configure, 🛑 Disable) + modal config
- Modal-based quick configuration (no page navigation)
- Removed tenant user assignment section from admin portal UI
- Super Admin can view users of each tenant (read-only)

### Wave D - Reliability Fixes
- Fixed inventory 500 caused by lazy tenant proxy serialization (`Medicine.tenant` ignored in JSON)
- Added structured bad-request handling for duplicate batch constraints
- Fixed cross-tenant redirect issue: tenant users no longer redirected to admin portal
- Fixed 403 errors for inventory, transactions, analytics, users for tenant users
- Empty state shown (no error) when no data exists for a tenant

### Wave E - UX Polish & Data Integrity
- Gender input changed from free-text to `MALE`/`FEMALE` dropdown for:
  - Tenant user creation
  - Tenant admin creation (admin portal)
- Profile update success feedback (toast/snackbar: "Profile updated successfully")
- Removed "Profit / Unit" column from inventory table
- User roles/privileges hidden from profile and general UI; visible only in User Management table
- Auto-scroll to bottom on new AI chat messages
- Inventory action buttons (Add, Edit, Delete) hidden/disabled for non-INVENTORY roles

### Wave F - Audit Trail & Admin Portal Enhancements
- Dedicated "Audit Trail" tab in admin portal
- Tracks:
  - User login history
  - Inventory CREATE / UPDATE / DELETE (with before/after values)
  - Module feature usage
  - AI Assistant usage (per tenant, per user)
- Audit record fields: `user_id`, `tenant_id`, `action_type`, `module`, `timestamp`, `metadata` (JSON)
- Audit dashboard includes filters (date, tenant, user, module), search, summary cards
- Audit trail monitoring charts for activity trends

### Wave G - Flyway Migration Refactor (Production-Grade Initialization)
- Removed runtime hardcoded seeding from application startup path (`DataInitializer` no longer performs bootstrap writes)
- Added Flyway schema migration pipeline:
  - `db/migration/V1__create_schema.sql` -> creates core schema objects
  - `db/migration/V2__seed_super_admin.sql` -> idempotent base super-admin seed
- Added optional demo seed migration:
  - `db/demo/V3__seed_demo_tenant_data.sql` -> DEMO tenant/pharmacies/admin/inventory
- Added property-driven migration location control (`app.demo-data.enabled`) via Flyway customizer
- Switched JPA `ddl-auto` to `none` so schema lifecycle is owned by Flyway only
- Validation performed with integration tests and startup test for both default and demo-disabled mode

## 5) Recent Incident Notes
- **Issue:** `POST /inventory` returned 500 (`ByteBuddyInterceptor` serialization failure)
- **Root Cause:** Jackson attempted to serialize lazy `tenant` proxy from `Medicine`
- **Fix:** Annotated `Medicine.tenant`/getter with `@JsonIgnore` for JSON serialization
- **Validation:** live login + create inventory + list inventory succeeded after patch

- **Issue:** Tenant users redirected to admin portal after login
- **Root Cause:** Frontend routing did not differentiate `SUPER_ADMIN` from tenant admin
- **Fix:** Route guard checks role; only `SUPER_ADMIN` redirected to `/admin-portal`

- **Issue:** 403 errors on inventory/transactions/users for tenant users
- **Root Cause:** Tenant context not correctly extracted from JWT in some filter paths
- **Fix:** Ensured tenant extraction from token applied consistently across all secured endpoints

## 6) Roadmap (Next)
- Add optimistic locking for high-concurrency inventory updates
- Move remaining map-based endpoints to typed response DTOs
- Expand automated integration tests for tenant role matrix and inventory flows
- Add export (CSV/PDF) for audit trail and analytics reports
- Add tenant-level branding (logo, name in header)
- Add Flyway migration test coverage for demo flag matrix (`true/false`) and migration idempotency checks
- Periodic Postman collection sync with current API contracts

## 7) Known Limitations
- Audit trend charts are basic; advanced time-series visualizations are pending
- Some backward-compatibility code paths remain for legacy tenant configurations
- Postman collection and API docs may need updates after recent contract changes
- Legacy environments initialized outside Flyway may require one-time baseline verification before production rollout

## 8) Delivery Discipline
- Keep tenant and role checks centralized in services/guards
- Keep controllers orchestration-only
- Validate DTO contracts at boundary (server-side) even if frontend validates
- Ship docs updates in same change set as API/UX contract updates
- Never expose profit data or internal role metadata outside authorized screens
