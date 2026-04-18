# Planning and Refactoring Strategy

## 1) High-Level Vision
Build a tenant-aware Pharmacy Management System that is:
- Secure by default (JWT + RBAC + tenant isolation)
- Operationally reliable (billing/inventory consistency and clear errors)
- Easy to scale to multiple pharmacy organizations (tenant lifecycle + feature flags)
- Maintainable (service-layer rules, thin controllers, synced docs)

---

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
  - user management per tenant (`/employees`)
  - AI Assistant (per-tenant toggle)

---

## 3) Architecture State
- **Presentation Layer**: Spring REST controllers + React (Vite) pages
- **Business Layer**: `AuthService`, `TenantService`, `InventoryService`, `SalesService`, `EmployeeService`, `AiAssistantService`
- **Data Layer**: JPA entities/repositories with tenant-aware queries; schema managed entirely by Flyway
- **Security Model**: JWT contains username + optional tenantId (null for super admin); pharmacy selected via `X-Pharmacy-Id` header
- **Admin Portal**: Separate UI layout at `/admin-portal` for Super Admin operations
- **Runtime**: Backend on port `8080` (Spring Boot), Frontend on port `5173` (Vite)

---

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
- Gender input changed from free-text to `MALE`/`FEMALE` dropdown for tenant user and tenant admin creation
- Profile update success feedback (toast/snackbar: "Profile updated successfully")
- Removed "Profit / Unit" column from inventory table
- User roles/privileges hidden from profile and general UI; visible only in User Management table
- Auto-scroll to bottom on new AI chat messages
- Inventory action buttons (Add, Edit, Delete) hidden/disabled for non-INVENTORY roles

### Wave F - Audit Trail & Admin Portal Enhancements
- Dedicated "Audit Trail" tab in admin portal
- Tracks: user login history, inventory CREATE/UPDATE/DELETE (with before/after values), module feature usage, AI Assistant usage (per tenant, per user)
- Audit record fields: `user_id`, `tenant_id`, `action_type`, `module`, `timestamp`, `metadata` (JSON)
- Audit dashboard includes filters (date, tenant, user, module), search, summary cards
- Audit trail monitoring charts for activity trends

### Wave G - Flyway Migration Refactor (Production-Grade Initialization)
- Removed runtime hardcoded seeding (`DataInitializer` no longer performs bootstrap writes)
- Flyway migration pipeline:
  - `V1__create_schema.sql` → creates core schema objects
  - `V2__seed_super_admin.sql` → idempotent base super-admin seed
  - `V3__seed_demo_tenant_data.sql` → DEMO tenant/pharmacies/admin/inventory (optional)
  - `V4__medicine_allowed_units.sql` → allowed units column and seed data
- Property-driven migration location control (`app.demo-data.enabled`) via Flyway customizer
- JPA `ddl-auto` set to `none`; schema lifecycle fully owned by Flyway
- Validated with integration tests for both demo-enabled and demo-disabled startup modes

### Wave H - Billing and Pharmacy Context Alignment
- Header title reflects currently selected pharmacy name across tenant-user routes
- Billing medicine search dropdown: on focus shows top 5, on typing expands to all matches
- Discount model consolidated to bill-level `discountAmount` only (no line-level discount)
- Inventory-configured `allowedUnits` drive billing unit dropdown options
- Backend sales validation enforces requested unit against each medicine `allowedUnits`
- Usage instructions persisted per sale line; visible in transaction details only

### Wave I - Full Stack Smoke Verification (April 2026)
- End-to-end smoke test run against live backend (H2 in-memory, demo seed)
- All core API endpoints confirmed passing:

| Endpoint | Result |
|---|---|
| `POST /auth/login` | ✅ |
| `GET /inventory` | ✅ |
| `GET /inventory/{id}` | ✅ |
| `GET /inventory/alerts/low-stock` | ✅ |
| `GET /inventory/alerts/expiry` | ✅ |
| `GET /inventory/alerts/summary` | ✅ |
| `GET /sales/billing-medicines` | ✅ |
| `POST /sales` | ✅ |
| `GET /sales` | ✅ |
| `GET /sales/summary?period=WEEK` | ✅ |
| `GET /employees` | ✅ |

- `POST /sales` previously listed as a known blocker — confirmed fully working end-to-end
- Backend (port 8080) and frontend (port 5173) both confirmed running clean

---

## 5) Incident Log

| # | Issue | Root Cause | Fix | Status |
|---|---|---|---|---|
| 1 | `POST /inventory` → 500 (`ByteBuddyInterceptor`) | Jackson serialized lazy `tenant` proxy on `Medicine` | `@JsonIgnore` on `Medicine.tenant` | ✅ Resolved |
| 2 | Tenant users redirected to admin portal | Frontend route guard didn't differentiate `SUPER_ADMIN` | Role-checked redirect; only `SUPER_ADMIN` goes to `/admin-portal` | ✅ Resolved |
| 3 | 403 on inventory/transactions/users for tenant users | Tenant context not extracted from JWT in some filter paths | Consistent tenant extraction across all secured endpoints | ✅ Resolved |
| 4 | `POST /sales` suspected 500 | Stale log entry — no actual failure reproduced | Smoke test confirmed working (April 2026) | ✅ Resolved |

---

## 6) Roadmap (Next)
- Add optimistic locking for high-concurrency inventory updates
- Move remaining map-based endpoints to typed response DTOs
- Expand automated integration tests for tenant role matrix and full billing flows
- Add export (CSV/PDF) for audit trail and analytics reports
- Add tenant-level branding (logo, name in header)
- Add Flyway migration test coverage for demo flag matrix (`true/false`) and idempotency checks
- Periodic Postman collection sync with current API contracts
- Add `GET /sales/{transactionId}` to frontend transaction history reopen flow (verify wiring)
- Consider pagination on `GET /inventory/alerts/low-stock` and `expiry` for large datasets

---

## 7) Known Limitations
- Audit trend charts are basic; advanced time-series visualizations are pending
- Some backward-compatibility code paths remain for legacy tenant configurations
- Postman collection and API docs may need a sweep after Wave H contract changes
- Legacy environments initialized outside Flyway may require one-time baseline verification before production rollout
- H2 in-memory database used in development; production migration to PostgreSQL/MySQL not yet scripted

---

## 8) Delivery Discipline
- Keep tenant and role checks centralized in services/guards
- Keep controllers orchestration-only
- Validate DTO contracts at boundary (server-side) even if frontend validates
- Ship docs updates in same change set as API/UX contract updates
- Never expose profit data or internal role metadata outside authorized screens
