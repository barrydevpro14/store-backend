# FRONTEND_ARCHITECTURE.md — Frontend SaaS architecture

> Target architecture for `store-frontend` (Next.js + TanStack Query + Zustand + RHF/Zod + shadcn/ui).
>
> **Direct inspiration: the backend's DDD Spring Boot architecture**. Each backend module (`org.store.users`, `org.store.vente`, etc.) has its frontend counterpart, with the same 4 layers `domain` / `application` / `infrastructure` / `presentation`. What doesn't apply on the frontend (JPA persistence, Spring services) is replaced by its equivalent (TanStack Query cache, React hooks).

---

## 🎯 Guiding principles

1. **Mirror the backend** — One backend module = one frontend module. Same name, same scope, same domain language. `org.store.users` on the backend ↔ `features/users/` on the frontend.
2. **4 DDD layers** — `domain` (pure model, types, schemas, rules), `application` (orchestration, use cases), `infrastructure` (API clients, browser persistence), `presentation` (UI components, pages).
3. **Pure domain, React-independent** — Types, Zod schemas, business rules are pure TypeScript, testable without a renderer, reusable in a potential mobile/CLI app.
4. **Application orchestrates, doesn't hold data** — `useQuery`/`useMutation` hooks orchestrate calls and cache. Like `<X>ServiceImpl` on the backend.
5. **Infrastructure = external adapters** — Axios client, API error parsing, localStorage. Like `<X>JpaRepository` on the backend.
6. **Presentation decoupled from the domain** — A page imports hooks/components from `presentation/`, never directly from `domain` or `infrastructure`.

---

## 📁 Project tree

```
store-frontend/
├── public/
├── src/
│   ├── app/                          # Next.js App Router (file-system routing only)
│   │   ├── layout.tsx                # Root layout (Providers, fonts)
│   │   ├── page.tsx                  # Public landing
│   │   ├── error.tsx
│   │   ├── not-found.tsx
│   │   │
│   │   ├── (auth)/                   # Public route group
│   │   │   ├── layout.tsx
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   └── forgot-password/page.tsx
│   │   │
│   │   ├── (dashboard)/              # Authenticated route group
│   │   │   ├── layout.tsx            # Sidebar + Header + JWT guard
│   │   │   ├── page.tsx              # Dashboard home
│   │   │   ├── employees/
│   │   │   │   ├── page.tsx          # ↳ delegates to features/users/presentation/pages/EmployeesListPage
│   │   │   │   ├── new/page.tsx
│   │   │   │   └── [id]/page.tsx
│   │   │   ├── magasins/...
│   │   │   ├── produits/...
│   │   │   ├── ventes/...
│   │   │   ├── stocks/...
│   │   │   ├── inventaires/...
│   │   │   ├── achats/...
│   │   │   ├── caisse/...
│   │   │   └── settings/...
│   │   │
│   │   └── (admin)/                  # ADMIN_ACCESS-only route group
│   │       ├── layout.tsx
│   │       ├── entreprises/page.tsx
│   │       └── plans/page.tsx
│   │
│   ├── features/                     # ⭐ Business modules (backend mirror, DDD structure)
│   │   ├── security/                 # ↔ org.store.security
│   │   │   ├── domain/
│   │   │   │   ├── enums.ts          # PermissionCode (mirror of the backend enum)
│   │   │   │   ├── types.ts          # UserPrincipal, Account, Role
│   │   │   │   ├── schemas.ts        # LoginSchema, AccountSchema, ChangePasswordSchema
│   │   │   │   └── rules.ts          # hasPermission(perm), isAdmin(user), isProprietaire(user)
│   │   │   ├── application/
│   │   │   │   ├── auth-store.ts     # Zustand: auth state (≈ session domain)
│   │   │   │   ├── use-auth.ts       # useLogin, useLogout, useRefresh, useChangePassword
│   │   │   │   └── use-permission.ts # hook derived from auth-store
│   │   │   ├── infrastructure/
│   │   │   │   ├── api.ts            # login/logout/refresh axios
│   │   │   │   └── jwt-storage.ts    # persist tokens (localStorage)
│   │   │   └── presentation/
│   │   │       ├── components/
│   │   │       │   ├── LoginForm.tsx
│   │   │       │   ├── RegisterForm.tsx
│   │   │       │   ├── ChangePasswordDialog.tsx
│   │   │       │   └── PermissionGuard.tsx
│   │   │       └── pages/
│   │   │           ├── LoginPage.tsx
│   │   │           └── RegisterPage.tsx
│   │   │
│   │   ├── users/                    # ↔ org.store.users
│   │   │   ├── domain/
│   │   │   │   ├── types.ts          # Employe, Proprietaire, Utilisateur
│   │   │   │   ├── schemas.ts        # EmployeSchema, EmployeCreateSchema, EmployeFilterSchema, ...
│   │   │   │   ├── enums.ts          # EmployeRole (mirror)
│   │   │   │   └── rules.ts          # canManagerEditEmploye(currentUser, employe), etc.
│   │   │   ├── application/
│   │   │   │   └── use-employes.ts   # useEmployes, useEmploye, useCreateEmploye, useUpdate, useDeactivate, useActivate, useResetPassword
│   │   │   ├── infrastructure/
│   │   │   │   └── api.ts            # employesApi.list/getById/create/update/...
│   │   │   └── presentation/
│   │   │       ├── components/
│   │   │       │   ├── EmployesTable.tsx
│   │   │       │   ├── EmployeForm.tsx
│   │   │       │   ├── EmployeFiltersBar.tsx
│   │   │       │   ├── ResetPasswordDialog.tsx
│   │   │       │   └── UserProfileForm.tsx
│   │   │       └── pages/
│   │   │           ├── EmployesListPage.tsx
│   │   │           ├── NewEmployePage.tsx
│   │   │           ├── EmployeDetailPage.tsx
│   │   │           └── UserProfilePage.tsx
│   │   │
│   │   ├── magasin/                  # ↔ org.store.magasin
│   │   ├── entreprise/               # ↔ org.store.entreprise
│   │   ├── produit/                  # ↔ org.store.produit (Product, CategoryProduct, Quality, ProductFournisseur)
│   │   ├── achat/                    # ↔ org.store.achat
│   │   ├── vente/                    # ↔ org.store.vente (CommandeVente, FactureClient, PaiementVente, Client, Caisse)
│   │   ├── stock/                    # ↔ org.store.stock
│   │   ├── depense/                  # ↔ org.store.depense
│   │   ├── inventaire/               # ↔ org.store.inventaire
│   │   └── abonnement/               # ↔ org.store.abonnement
│   │
│   ├── common/                       # ↔ org.store.common (cross-cutting utilities)
│   │   ├── domain/
│   │   │   ├── types.ts              # PageResponse<T>, ErrorResponse, ImageDownloadResponse
│   │   │   ├── enums.ts              # MoyenPaiement (cross-cutting mirror)
│   │   │   └── schemas.ts            # Cross-cutting Zod schemas (DatePattern, PhoneNumber, ...)
│   │   ├── application/
│   │   │   ├── use-debounce.ts
│   │   │   ├── use-pagination.ts
│   │   │   └── use-toast.ts
│   │   ├── infrastructure/
│   │   │   ├── api-client.ts         # axios instance + interceptors (REST-client equivalent)
│   │   │   ├── api-errors.ts         # backend error parsing (i18n key)
│   │   │   ├── env.ts                # typesafe env-var parsing via Zod
│   │   │   └── query-client.ts       # TanStack Query config
│   │   └── presentation/
│   │       ├── ui/                   # shadcn/ui copies (button, input, dialog, table, ...)
│   │       ├── layout/
│   │       │   ├── Sidebar.tsx
│   │       │   ├── Header.tsx
│   │       │   ├── UserMenu.tsx
│   │       │   └── ThemeToggle.tsx
│   │       └── shared/
│   │           ├── DataTable.tsx
│   │           ├── ConfirmDialog.tsx
│   │           ├── EmptyState.tsx
│   │           ├── ErrorBanner.tsx
│   │           ├── PageHeader.tsx
│   │           ├── FormField.tsx
│   │           └── PhoneInput.tsx
│   │
│   └── providers/                    # React Provider wrappers (backend @Configuration equivalent)
│       ├── QueryProvider.tsx         # TanStack QueryClientProvider
│       ├── ThemeProvider.tsx
│       └── ToastProvider.tsx
│
├── tests/                            # Cross-feature integration tests (Playwright E2E)
├── tsconfig.json
├── tailwind.config.ts
├── next.config.ts
├── package.json
├── .env.example
└── components.json                   # shadcn/ui config
```

---

## 🧩 The 4 layers — details by responsibility

### 1️⃣ `domain/` — Pure business model

**Role**: represent business concepts in pure TypeScript, without React/axios/router dependency. This is the counterpart of `org.store.<module>.domain/` on the backend (entities, value objects, enums, pure business services).

**Content**:
- `types.ts` — TS types representing the business aggregates (`Employe`, `CommandeVente`, etc.). Derived from Zod via `z.infer`.
- `schemas.ts` — Zod schemas, **source of truth** for runtime validation + TS types. Mirror of `@NotBlank/@Email/@Min` from Java records.
- `enums.ts` — TS enums mirroring the Java enums (`PermissionCode`, `EmployeRole`, `InventaireStatut`, `StatutFacture`, etc.).
- `rules.ts` — pure reusable business-rule functions (`canManagerActOn(employe)`, `computeBenefice(...)`). Equivalent of pure static methods on a backend domain service.

**Strict rule**: 0 React imports, 0 axios imports, 0 Next.js imports. Pure TS only. Testable with `vitest` without a renderer.

**Why**: the domain layer stays portable. If we ship React Native or a Node.js CLI tomorrow, we reuse it as-is.

---

### 2️⃣ `application/` — Orchestration & use cases

**Role**: orchestrate business operations by composing `infrastructure` + `domain`. Frontend counterpart of `org.store.<module>.application.service.*ServiceImpl`.

**Content**:
- TanStack Query hooks (`useEmployes`, `useCreateEmploye`, etc.). Functional equivalent of `EmployeServiceImpl.create()`.
- Zustand stores (`auth-store.ts`, `ui-store.ts`). Equivalent of an application service that maintains a session or scope state.
- Reusable logic hooks (`useCurrentUser`, `usePermission`, `useCurrentMagasin`). Equivalent of `ICurrentUserService`.

**Rule**: **never** contains direct axios calls or JSX. Receives the API from `infrastructure/api.ts`, applies transformations, manages the TanStack Query cache, exposes hooks the `presentation` layer consumes.

**Backend ↔ Frontend mapping**:
| Backend (Java) | Frontend (TS) |
|---|---|
| `EmployeServiceImpl.create(req)` | `useCreateEmploye()` (TanStack mutation) |
| `EmployeServiceImpl.findAllByCurrentEntreprise(filter)` | `useEmployes(filter)` (TanStack query) |
| `ICurrentUserService.getCurrent()` | `useAuthStore((s) => s.user)` |
| `IPermissionsService.findAllByRoleId(...)` | derived from `auth-store.user.permissions` |
| `@Transactional` orchestration | TanStack mutation with `onSuccess: () => invalidateQueries(...)` |

---

### 3️⃣ `infrastructure/` — External adapters

**Role**: everything that talks to an external world (network, browser, storage). Frontend counterpart of `org.store.<module>.infrastructure.repository.*JpaRepository`.

**Content**:
- `api.ts` per feature — pure axios functions (`employesApi.list`, `.create`, etc.). Equivalent of `*JpaRepository`.
- LocalStorage / sessionStorage / cookie adapters if needed (`jwt-storage.ts`).
- Specific mappers if the API response shape differs from the domain type (rare, but possible).

**Rule**: no `useQuery` here (that's `application/`'s role). No JSX. Just functions that take an input and return a typed `Promise<Response>`.

**Why separate**: if we swap axios for fetch, or add an offline cache (Service Worker), we change `infrastructure` without touching the rest.

---

### 4️⃣ `presentation/` — UI

**Role**: visual components + pages. Frontend counterpart of `org.store.<module>.presentation.*Controller`.

**Content**:
- `components/` — feature-specific React components (`EmployeForm`, `EmployesTable`, `ResetPasswordDialog`). They consume `application/` hooks.
- `pages/` — "full view" components that assemble several `components`. They are imported from `app/.../page.tsx`.

**Golden rule**: `presentation/` components may import from `application/` (hooks) and `domain/` (types). **Never** from `infrastructure/` directly (the separation is sacred).

**Why**:
- `app/.../page.tsx` stays minimal and tied to Next.js routing.
- If we migrate to Remix, Vite, or TanStack Router tomorrow, we move only `app/` and all `presentation/` stays intact.

---

## 🔍 Full example — feature `users/` (employees)

### `features/users/domain/enums.ts`

```ts
// Backend mirror: org.store.security.application.enums.PermissionCode
export const PermissionCode = {
  EMPLOYE_ACCESS: 'EMPLOYE_ACCESS',
  EMPLOYE_CREATE: 'EMPLOYE_CREATE',
  EMPLOYE_READ: 'EMPLOYE_READ',
  EMPLOYE_UPDATE: 'EMPLOYE_UPDATE',
  EMPLOYE_DELETE: 'EMPLOYE_DELETE',
  EMPLOYE_RESET_PASSWORD: 'EMPLOYE_RESET_PASSWORD',
  PROPRIETAIRE_ACCESS: 'PROPRIETAIRE_ACCESS',
  ADMIN_ACCESS: 'ADMIN_ACCESS',
} as const

export type PermissionCode = (typeof PermissionCode)[keyof typeof PermissionCode]
```

### `features/users/domain/schemas.ts`

```ts
import { z } from 'zod'

export const employeSchema = z.object({
  id: z.string().uuid(),
  nom: z.string(),
  prenom: z.string(),
  email: z.string().email(),
  telephone: z.string(),
  adresse: z.string().nullable(),
  username: z.string(),
  role: z.string(),
  magasinId: z.string().uuid(),
  actif: z.boolean(),
})
export type Employe = z.infer<typeof employeSchema>

export const employeCreateSchema = z.object({
  account: z.object({
    username: z.string().min(3).max(50),
    password: z.string().min(8).max(100),
  }),
  utilisateur: z.object({
    nom: z.string().min(1),
    prenom: z.string().min(1),
    email: z.string().email(),
    telephone: z.string().min(8),
    adresse: z.string().optional(),
  }),
  role: z.string().min(1),
  magasinId: z.string().uuid(),
})
export type EmployeCreate = z.infer<typeof employeCreateSchema>

export const employeFilterSchema = z.object({
  nom: z.string().optional(),
  prenom: z.string().optional(),
  role: z.string().optional(),
  magasinId: z.string().uuid().optional(),
  actif: z.boolean().optional(),
  page: z.number().int().min(0).default(0),
  size: z.number().int().min(1).max(100).default(10),
})
export type EmployeFilter = z.infer<typeof employeFilterSchema>
```

### `features/users/domain/rules.ts`

```ts
import type { UserPrincipal } from '@/features/security/domain/types'
import type { Employe } from './schemas'
import { PermissionCode } from './enums'

// Pure rule: a MANAGER can only act on employees of their store
// Mirror of EmployeServiceImpl.ensureAccessibleByManager on the backend.
export function canManagerActOn(currentUser: UserPrincipal, employe: Employe): boolean {
  if (currentUser.permissions.includes(PermissionCode.PROPRIETAIRE_ACCESS)) return true
  if (currentUser.permissions.includes(PermissionCode.ADMIN_ACCESS)) return true
  return currentUser.magasinId === employe.magasinId
}
```

### `features/users/infrastructure/api.ts`

```ts
import { apiClient } from '@/common/infrastructure/api-client'
import type { PageResponse } from '@/common/domain/types'
import type { Employe, EmployeCreate, EmployeFilter, EmployeUpdate, ResetPasswordRequest } from '../domain/schemas'

export const employesApi = {
  list: (filter: EmployeFilter) =>
    apiClient.get<PageResponse<Employe>>('/api/v1/employees', { params: filter }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<Employe>(`/api/v1/employees/${id}`).then((r) => r.data),

  create: (data: EmployeCreate) =>
    apiClient.post<Employe>('/api/v1/employees', data).then((r) => r.data),

  update: (id: string, data: EmployeUpdate) =>
    apiClient.put<Employe>(`/api/v1/employees/${id}`, data).then((r) => r.data),

  deactivate: (id: string) => apiClient.delete(`/api/v1/employees/${id}`),
  activate: (id: string) => apiClient.patch(`/api/v1/employees/${id}/activate`),
  resetPassword: (id: string, payload: ResetPasswordRequest) =>
    apiClient.post(`/api/v1/employees/${id}/reset-password`, payload),
}
```

### `features/users/application/use-employes.ts`

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { employesApi } from '../infrastructure/api'
import type { EmployeCreate, EmployeFilter, EmployeUpdate } from '../domain/schemas'

const KEY = ['employes']

export function useEmployes(filter: EmployeFilter) {
  return useQuery({ queryKey: [...KEY, filter], queryFn: () => employesApi.list(filter) })
}

export function useEmploye(id: string) {
  return useQuery({ queryKey: [...KEY, id], queryFn: () => employesApi.getById(id), enabled: !!id })
}

export function useCreateEmploye() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: EmployeCreate) => employesApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useDeactivateEmploye() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => employesApi.deactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}
// ... etc
```

### `features/users/presentation/components/EmployesTable.tsx`

```tsx
'use client'
import { useEmployes } from '../../application/use-employes'
import { DataTable } from '@/common/presentation/shared/DataTable'
import type { EmployeFilter } from '../../domain/schemas'

export function EmployesTable({ filter }: { filter: EmployeFilter }) {
  const { data, isLoading } = useEmployes(filter)
  return <DataTable data={data?.content ?? []} columns={columns} loading={isLoading} />
}
```

### `features/users/presentation/pages/EmployesListPage.tsx`

```tsx
'use client'
import { useState } from 'react'
import { EmployesTable } from '../components/EmployesTable'
import { EmployeFiltersBar } from '../components/EmployeFiltersBar'
import { PageHeader } from '@/common/presentation/shared/PageHeader'

export function EmployesListPage() {
  const [filter, setFilter] = useState({ page: 0, size: 10 })
  return (
    <>
      <PageHeader title="Employees" action={<NewEmployeButton />} />
      <EmployeFiltersBar value={filter} onChange={setFilter} />
      <EmployesTable filter={filter} />
    </>
  )
}
```

### `app/(dashboard)/employees/page.tsx`

```tsx
import { EmployesListPage } from '@/features/users/presentation/pages/EmployesListPage'
export default EmployesListPage
```

> The Next.js page is a **pure re-export**. All logic lives in `features/`. It's the equivalent of a minimal `EmployeController` that delegates to `IEmployeService`.

---

## 🔁 Module-to-module mapping with the backend

| Backend module (`org.store.<x>`) | Frontend module (`features/<x>/`) | Specifics |
|---|---|---|
| `security` | `security/` | Zustand auth store in `application/`. PermissionGuard component in `presentation/`. |
| `users` | `users/` | Includes Employe + Proprietaire + UserProfile. |
| `magasin` | `magasin/` | + store selector for multi-store owner (`MagasinContextStore`). |
| `entreprise` | `entreprise/` | Distinction between route `/me` (PROPRIETAIRE) and `/admin/entreprises` (ADMIN). |
| `produit` | `produit/` | Sub-modules `category`, `quality`, `product-fournisseur` grouped under the same feature (strong cohesion). |
| `achat` | `achat/` | Suppliers + orders + invoices + purchase payments. |
| `vente` | `vente/` | Client + orders + invoices + payments + cash register (top products, daily summary). |
| `stock` | `stock/` | Entries, exits, adjustments, movements, valuation, thresholds, expiring lots, margins. |
| `depense` | `depense/` | Categories + expenses. |
| `inventaire` | `inventaire/` | Physical inventory + accounting report. |
| `abonnement` | `abonnement/` | Plans, types, coupons, promotions, payments (coming, see TODO). |
| `common` | `common/` | Cross-cutting helpers, shared types (`PageResponse`, etc.), UI components. |

**Golden rule**: if a module doesn't exist on the backend, it doesn't exist on the frontend. If we create a backend module, we create the mirror frontend module.

---

## 🌐 Common layer `common/` (detailed)

### `common/infrastructure/api-client.ts`

```ts
import axios, { AxiosError } from 'axios'
import { env } from './env'
import { useAuthStore } from '@/features/security/application/auth-store'
import { parseApiError } from './api-errors'

export const apiClient = axios.create({ baseURL: env.NEXT_PUBLIC_API_URL })

// JWT injection (frontend equivalent of the backend JwtAuthenticationFilter)
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 refresh + i18n-friendly error parsing
apiClient.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    // ... refresh + retry logic ...
    return Promise.reject(parseApiError(error))
  }
)
```

### `common/infrastructure/api-errors.ts`

```ts
// Parses Spring backend error responses (GlobalException → ErrorResponse)
// to expose an i18n key + status to the rest of the frontend
export type ApiError = { messageKey: string; status: number; args?: any[] }

export function parseApiError(error: AxiosError): ApiError {
  const data = error.response?.data as any
  return {
    messageKey: data?.errors?.[0]?.message ?? 'errors.unknown',
    status: error.response?.status ?? 0,
    args: data?.errors?.[0]?.args,
  }
}
```

### `common/domain/types.ts`

```ts
// Mirror of Spring Data paginated responses (PageImpl)
export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // current page
  size: number
  first: boolean
  last: boolean
}

// Mirror of org.store.common.dto.ImageDownloadResponse on the backend
export type ImageDownloadResponse = { content: ArrayBuffer; contentType: string }
```

---

## 🔐 Authentication & permissions (DDD-aligned)

### `features/security/domain/types.ts`

```ts
// Mirror of org.store.security.application.dto.UserPrincipal
export type UserPrincipal = {
  accountId: string
  userId: string
  entrepriseId: string
  magasinId: string | null
  username: string
  role: string
  permissions: string[]
}
```

### `features/security/application/auth-store.ts`

```ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserPrincipal } from '../domain/types'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  user: UserPrincipal | null
  setAuth: (tokens: { access: string; refresh: string }, user: UserPrincipal) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setAuth: (tokens, user) => set({ accessToken: tokens.access, refreshToken: tokens.refresh, user }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'store-auth' }
  )
)
```

### `features/security/application/use-permission.ts`

```ts
import { useAuthStore } from './auth-store'
import type { PermissionCode } from '../domain/enums'

export function usePermission(...required: PermissionCode[]) {
  const permissions = useAuthStore((s) => s.user?.permissions ?? [])
  return required.every((p) => permissions.includes(p))
}
```

### `features/security/presentation/components/PermissionGuard.tsx`

```tsx
'use client'
import type { ReactNode } from 'react'
import type { PermissionCode } from '../../domain/enums'
import { usePermission } from '../../application/use-permission'

export function PermissionGuard({ required, children, fallback = null }: { required: PermissionCode[]; children: ReactNode; fallback?: ReactNode }) {
  return usePermission(...required) ? <>{children}</> : <>{fallback}</>
}
```

**Usage**:
```tsx
<PermissionGuard required={[PermissionCode.EMPLOYE_DELETE]}>
  <Button onClick={onDeactivate}>Deactivate</Button>
</PermissionGuard>
```

---

## 🎨 UI conventions

1. **Tailwind only** — no inline `style={{}}`. Use `cn()` to compose (`cn('px-4 py-2', isActive && 'bg-primary')`).
2. **shadcn/ui copied into `common/presentation/ui/`** — modifiable directly, owned by the project.
3. **Semantic colors** in `tailwind.config.ts`: `primary`, `secondary`, `destructive`, `muted`, `accent`. Never `bg-red-500`.
4. **Dark mode** via shadcn CSS variables (`[data-theme="dark"]`).
5. **Mobile-first**: responsive classes (`md:`, `lg:`) added on top of the base.
6. **Logos / images**: `next/image` mandatory (auto optimizations). Relative backend URL → prefix with `apiClient.defaults.baseURL` (utility helper).

---

## 📊 Cross-cutting components (`common/presentation/shared/`)

| Component | Role |
|---|---|
| `DataTable<T>` | @tanstack/react-table wrapper + shadcn `<Table>`. Handles sort / pagination / loading / empty / actions. |
| `PageHeader` | Title + breadcrumbs + actions slot (CTA buttons). |
| `ConfirmDialog` | Confirmation dialog for destructive actions. |
| `EmptyState` | Standardized "no data" display. |
| `ErrorBanner` | Standardized error display (with i18n `messageKey`). |
| `FormField` | RHF wrapper + Label + Input + ErrorMessage. |
| `PhoneInput` | Phone input with Phone validation (mirror of backend validation). |

---

## 🧪 Tests (aligned with the backend test strategy)

| Level | Tool | Target | Backend equivalent |
|---|---|---|---|
| **Pure domain** | Vitest | `schemas` (Zod validation), `rules.ts`, `utils.ts` helpers | Pure JUnit tests without `@SpringBootTest` |
| **Application** | Vitest + React Testing Library | `useEmployes`/`useCreateEmploye` hooks (with mocked QueryClient) | `*ServiceImplTest` with mocks |
| **Presentation** | React Testing Library | Critical components (forms, guards, tables) | `*ControllerTest` with MockMvc |
| **E2E** | Playwright | 5-10 end-to-end scenarios (login, create employee, full sale) | No backend equivalent (separate Spring integration tests) |

**Rule**: pure `domain/` tests at 80%+. Tests on `application/` for complex hooks. Targeted `presentation/` tests (forms + guards). No obsessive TDD — test what can break.

---

## ⚙️ Tooling

| Tool | Role | Backend equivalent |
|---|---|---|
| TypeScript strict | Type safety | Java type system |
| ESLint + Prettier | Linting + format | (nothing imposed) |
| Husky + lint-staged | Pre-commit hook | (to put in place backend-side too) |
| Vitest | Unit tests | JUnit 5 |
| Playwright | E2E | (coming) |
| commitlint | Conventional commits (English) | already enforced backend-side |

---

## 🔄 Typical workflow for a new feature

1. **Domain first** — Create `features/<f>/domain/schemas.ts` (Zod), `types.ts`, `enums.ts`, `rules.ts` if any business rules
2. **Infrastructure** — Create `features/<f>/infrastructure/api.ts` (pure axios functions)
3. **Application** — Create `features/<f>/application/use-<f>.ts` (TanStack Query hooks)
4. **Presentation** — Create components in `features/<f>/presentation/components/`, then pages in `presentation/pages/`
5. **Routing** — Re-export the page from `app/(dashboard)/<f>/page.tsx`
6. **Tests** — Test `domain/` (Vitest) and critical components (Testing Library)

**Golden rule**: `app/.../page.tsx` NEVER contains logic. Only `import` + `export default`.

---

## ⚡ Performance — levers to know

1. **Server Components by default** — anything non-interactive stays server-side (bundle savings).
2. **`next/image`** for all images (modern formats, lazy loading).
3. **`next/font`** for fonts (self-hosted, no FOUT).
4. **TanStack Query `staleTime`** — 30s default, 5min for near-static data (subscription plans).
5. **Skeleton loaders** rather than spinners.
6. **`useDebounce` ~300ms** on search filters.
7. **Auto code splitting by Next.js** — each page = its own chunk.

---

## 🌍 Internationalization (future, backend mirror)

- Backend has `messages.properties` (FR) + `messages_en.properties` (EN). The frontend must follow the **same key map**.
- The `messageKey` returned by backend errors (`employe.notFound`, `account.currentPassword.invalid`) is resolved on the frontend via `next-intl` or equivalent.
- To start: **English hardcoded** is fine. i18n will come when the FR market becomes a priority.

---

## 🛑 Anti-patterns to avoid

1. **Mixing layers** — A `presentation/` component that calls `apiClient.get()` directly → bypasses `infrastructure/api.ts`. Mandatory refactor.
2. **Business logic in `presentation/`** — If you write `if (user.role === 'MANAGER' && ...)` in a component, move it into `domain/rules.ts`.
3. **Shared components anywhere but in `common/`** — If `presentation/components/EmployeCard` is reused in `vente/`, it moves into `common/presentation/shared/`.
4. **Duplicate types** — Always `z.infer<typeof schema>`. Never manual redeclaration.
5. **API data in Zustand** — TanStack Query for server state. Zustand for client state only.
6. **`useEffect` for fetch** — Always `useQuery`.
7. **Hardcoded color** — Always via the Tailwind theme (`bg-primary`, not `bg-blue-500`).
8. **Useless wrapper UI component** — If `<MyButton>` only wraps shadcn's `<Button>` without adding anything, delete it.

---

## 📋 `store-frontend` bootstrap checklist

**Phase 0 — Setup (1 day)**
- [ ] `npx create-next-app@latest store-frontend --typescript --tailwind --app`
- [ ] Install shadcn/ui (`npx shadcn@latest init`) + base components (button, input, dialog, table, toast, dropdown-menu, badge, form, select, popover)
- [ ] Install TanStack Query, Zustand, axios, RHF, @hookform/resolvers, zod, @tanstack/react-table
- [ ] Create the `src/{app,features,common,providers}/` structure
- [ ] Configure `tailwind.config.ts` (semantic colors + dark mode)
- [ ] Set up ESLint + Prettier + Husky + lint-staged + commitlint

**Phase 1 — Foundations (3 days)**
- [ ] `common/infrastructure/{api-client,api-errors,env,query-client}.ts`
- [ ] `common/domain/types.ts` (PageResponse, etc.)
- [ ] `common/presentation/ui/` (shadcn copies)
- [ ] `common/presentation/shared/` (DataTable, PageHeader, ConfirmDialog, FormField, etc.)
- [ ] `features/security/` complete (4 layers)
- [ ] `app/(auth)/{login,register}/page.tsx` (re-exports)
- [ ] `app/(dashboard)/layout.tsx` with JWT guard

**Phase 2 — First complete feature (2 days)**
- [ ] `features/users/` complete (4 layers): Employe + UserProfile
- [ ] Pages `app/(dashboard)/employees/{page,new/page,[id]/page}.tsx`
- [ ] Pages `app/(dashboard)/settings/{profile,security}/page.tsx`

**Phase 3 — Feature iterations (rhythm: 1 feature per day, suggested order)**
- [ ] `magasin/` (CRUD admin + logo)
- [ ] `entreprise/` (owner profile + logo + admin)
- [ ] `produit/` (CRUD + image gallery)
- [ ] `stock/` (entries, exits, adjustments, valuation)
- [ ] `achat/` (orders, invoices, payments)
- [ ] `vente/` (orders, invoices, payments, cash register, top-products)
- [ ] `depense/` (categories + expenses)
- [ ] `inventaire/` (physical + report)
- [ ] `abonnement/` (subscription + public plans)

**Phase 4 — Polish**
- [ ] Consistent skeleton loaders
- [ ] Destructive confirmations (ConfirmDialog everywhere)
- [ ] Playwright tests on 5 critical flows
- [ ] Lighthouse optimizations (LCP < 2.5s)
- [ ] README + CONTRIBUTING.md

---

## 🎓 How this architecture evolves

- **Next.js Server Actions**: possible addition in `features/<f>/application/server-actions.ts` (Next.js 14+), without breaking the DDD structure.
- **Monorepo (front + back together)**: move `store-frontend` to `apps/frontend/`, package shared domain types in `packages/domain-types/` (reusable for React Native if needed).
- **React Native mobile app**: `features/<f>/{domain,application,infrastructure}/` reusable as-is. Only `presentation/` changes (React Native primitives instead of shadcn).
- **Node.js CLI (admin script)**: `features/<f>/{domain,infrastructure}/` reusable. No `application/` (React hooks) nor `presentation/` needed.

**The DDD structure stands the test of time**. That's exactly why we use it on the backend, and that's why it applies on the frontend too.
