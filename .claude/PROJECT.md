# PROJECT.md — Project specs

---

## 1. Overall vision

**Project name** → STORE

**In one sentence** → Multi-tenant SaaS platform to manage a spare-parts store (sales, purchases, stock, accounting). Monorepo with **`store/`** (Spring Boot backend) and **`store-frontend/`** (Next.js frontend).

**What problem it solves, and for whom?**
→ Digitize the full management of a spare-parts store. Designed as an ERP **SaaS multi-store, multi-user**, sold by subscription to store owners.

---

## 2. Main goal — business modules

1. Authentication & Security (JWT, RBAC)
2. Company management (root tenant)
3. Store management (sub-tenant: one owner can have multiple stores)
4. Users (Owner, Employee)
5. Products (catalog, categories, quality, linked suppliers)
6. Stock (FIFO entries/exits, movements, thresholds)
7. Purchases (orders, invoices, supplier payments)
8. Sales (orders, invoices, client payments)
9. Expenses
10. Payments (purchase / sale / subscription)
11. SaaS subscriptions (plans, types, coupons, promotions)
12. Notifications (alerts, due dates, templates)
13. Documents & images (`PieceJointe`)
14. Dashboard & reporting (planned)
15. System settings
16. Audit & history (`AuditableEntity`: created/updated by/at)
17. Multi-tenant & permissions
18. API & integrations (REST + OpenAPI)

---

## 3. Users

**Who uses it** → Store owners (SaaS customers) and their employees (sellers).

**Roles**:
- `Proprietaire` (Owner) — owns a `Entreprise` (Company), manages stores, employees, and their subscription.
- `Employe` (Employee) — assigned to a store, handles sales/purchases/stock per permission.
- (Future) SaaS Admin — platform management on the vendor side.

Fine-grained permissions via `Role` ↔ `Permissions` (N‑N).

---

## 4. Features

**Must-have (MVP)**:
- Owner registration + company creation + first store
- JWT authentication + refresh token
- CRUD for products / categories / quality
- FIFO stock management (entries via purchases, exits via sales)
- Purchase cycle: order → invoice → payment
- Sale cycle: order → invoice → payment
- SaaS subscription with free trial (`PlanAbonnement.trial`, `Entreprise.trialUsed`)

**Nice-to-have**:
- Email/SMS notifications on unpaid due dates
- Coupons and promotions on subscriptions
- Physical inventories with discrepancies
- Dashboard and reports

**Out of scope**:
- Physical POS / cash register
- Advanced analytical accounting
- Native mobile (the web frontend must stay responsive)

---

## 5. Tech stack

### Backend (`store/`)
- **Language** → Java 21
- **Framework** → Spring Boot 4.0.6 (web, data-jpa, security, validation, actuator)
- **Database** → PostgreSQL (Hibernate, `ddl-auto: update` in dev)
- **Auth** → Spring Security + JWT (`io.jsonwebtoken:jjwt 0.11.5`)
- **API docs** → springdoc-openapi 2.8.1 (Swagger UI)
- **Boilerplate** → Lombok (`@Getter` / `@Setter` on entities, never `@Data`)
- **Tests** → JUnit 5 + Mockito

### Frontend (`store-frontend/`)
- **Framework** → Next.js 16.2.6 (App Router, React Server Components)
  ⚠️ Next.js 16 has **breaking changes** vs earlier versions — see `store-frontend/AGENTS.md` and `node_modules/next/dist/docs/` before writing code.
- **UI lib** → React 19.2.4 + TypeScript 5
- **Styles** → Tailwind CSS 4 (CSS-variables config in `src/app/globals.css`, no `tailwind.config.ts` anymore)
- **Components** → shadcn/ui (style `base-nova`, baseColor `neutral`) + `@base-ui/react` 1.4.1 (headless primitives, replaces Radix)
- **Icons** → lucide-react
- **Forms** → react-hook-form + zod + `@hookform/resolvers`
- **Client state** → zustand 5
- **Data fetching** → TanStack React Query + axios
- **Lint** → ESLint (`eslint-config-next`)

### Deployment
TBD (likely Docker for backend, Vercel or Docker for frontend).

---

## 6. Constraints

**Technical**:
- Strict multi-tenancy: every request must be scoped to the current user's `Entreprise` / `Magasin`.
- Mandatory audit on every business entity (`AuditableEntity` inheritance).
- IDs as `UUID` (no `Long auto-increment`).

**Sensitive data**:
- Passwords (hashed in `Account.password`)
- Tokens (JWT + `RefreshToken`)
- Financial data (`BigDecimal precision=19 scale=2`)

---

## 7. Top priority

If only one thing must ship: **owner registration flow with company creation + first store + trial subscription activation**, followed by JWT authentication (backend) + **a registration / login screen on the frontend consuming these endpoints**. Everything else chains behind this flow.
