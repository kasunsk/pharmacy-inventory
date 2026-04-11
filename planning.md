# Planning and Refactoring Strategy
## 1) High-Level Vision
Build a production-ready Pharmacy Management System that is:
- Secure by default (JWT + RBAC)
- Operationally reliable (transaction-safe billing + inventory sync)
- Easy to extend (clear module boundaries and service-layer design)
- Developer-friendly (consistent contracts, docs, and tooling)
## 2) Module Breakdown
- **Auth**
  - Login, token generation, identity propagation
- **User Management**
  - Admin-managed users, multi-role assignment, account enable/disable
- **Billing**
  - Row-based medicine billing with discount logic and usage instructions
- **Inventory**
  - Medicine lifecycle, stock visibility, low-stock/expiry alerts
- **Transactions**
  - Searchable sales history and detailed bill retrieval
- **Analytics**
  - Sales summaries, top-selling medicines, sales-by-user aggregates
- **Files**
  - Prescription/receipt uploads and signed-view URLs
## 3) Current Architecture (Target State)
- **Presentation Layer**
  - Spring REST controllers, React pages/components
- **Business Logic Layer**
  - Service classes (`SalesService`, `InventoryService`, `EmployeeService`, `AuthService`)
- **Data Layer**
  - JPA entities + repositories + DTO contracts
## 4) Deep Analysis Findings (Summary)
### Backend
- **Separation of concerns gaps**
  - Controllers held non-trivial business logic (auth/user operations).
- **RBAC model limitations**
  - Single-role user model blocked granular access control.
- **Error handling quality**
  - Generic error responses lacked context (path/reason/log correlation).
- **Contract drift risk**
  - DTO changes not consistently reflected in docs and API examples.
### Frontend
- **Authorization coupling**
  - Route/menu visibility tightly bound to single-role assumptions.
- **Billing complexity concentration**
  - Dense billing logic in one module, difficult to evolve and test.
- **UX consistency issues**
  - Inconsistent controls and labels across workflow-heavy pages.
## 5) Refactoring Plan (What Changed and Why)
### Refactor Wave A - Access and Identity
- Introduced multi-role user model (`Set<Role>`)
- Added role set in login response (`roles`)
- Updated security authority mapping from all assigned roles
**Why:** Enables scalable permission combinations and cleaner authorization semantics.
### Refactor Wave B - Layering and Modularization
- Added `AuthService` for login orchestration
- Added `EmployeeService` for user-management business rules
- Kept controllers thin (HTTP orchestration only)
**Why:** Reduces controller coupling and makes business logic reusable/testable.
### Refactor Wave C - API and Error Quality
- Improved global exception handling with structured payload (`status`, `error`, `message`, `path`, `timestamp`) and logging
- Standardized endpoint-level role guards per module purpose
**Why:** Better diagnostics, safer operations, and predictable client behavior.
### Refactor Wave D - Billing + Inventory Workflow
- Implemented enhanced row-based billing (`BillingPageV2`)
- Added per-row discounts (`%` / fixed) and standardized usage instructions
- Kept out-of-stock medicines visible but disabled
- Exposed inventory profit visibility in UI
**Why:** Improves pharmacy operator speed, consistency, and data correctness.
## 6) Technical Decisions and Reasoning
- **Decision:** Multi-role RBAC (`ADMIN`, `BILLING`, `TRANSACTIONS`, `INVENTORY`)
  - **Reason:** Matches real pharmacy duties better than a coarse two-role model.
- **Decision:** Keep layered architecture over full hexagonal rewrite for now
  - **Reason:** Lower regression risk while improving maintainability incrementally.
- **Decision:** Use service-layer orchestration for auth and employee workflows
  - **Reason:** Better testability and controller simplification.
- **Decision:** Keep billing persistence compatible with existing sale contract using effective unit price
  - **Reason:** Avoids immediate database/API breaking changes while supporting per-line discounts.
## 7) Feature Roadmap
### Delivered (Current)
- Multi-role RBAC with dynamic frontend navigation
- Admin user management with role assignment
- Enhanced billing workflow and inventory-aware constraints
- Better exception payload and diagnostics
- Expanded docs foundation (`README`, API, Postman, planning)
### Next
- Add dedicated billing line-item discount fields in backend domain (first-class persistence)
- Add optimistic locking on inventory rows to harden concurrent billing
- Add service-level unit tests for new auth/employee services
- Expand frontend form validation utility layer and shared UI table components
- Add CI checks for formatting, test, and API contract consistency
## 8) Known Limitations and Improvement Areas
- Billing per-row discount is currently folded into effective line unit price for persistence compatibility.
- Some modules still use map-based responses (files/AI endpoints) and can be migrated to typed DTOs.
- Integration tests for role matrix scenarios are limited and should be expanded.
- API docs and examples should be versioned and validated against test fixtures.
## 9) Best-Practice Checklist Going Forward
- Keep controllers orchestration-only.
- Put business rules and validations in services.
- Keep DTO contracts explicit and version-aware.
- Add tests for each critical business rule.
- Log actionable context for operational events and failures.
- Update docs in the same PR as contract or workflow changes.
