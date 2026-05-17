# FRONTEND_ARCHITECTURE.md — Architecture du frontend SaaS

> Architecture cible pour `store-frontend` (Next.js + TanStack Query + Zustand + RHF/Zod + shadcn/ui).
>
> **Inspiration directe : l'architecture DDD du backend Spring Boot**. Chaque module backend (`org.store.users`, `org.store.vente`, etc.) trouve son écho côté frontend, avec les mêmes 4 couches `domain` / `application` / `infrastructure` / `presentation`. Ce qui n'a pas de sens côté front (persistance JPA, services Spring) est remplacé par son équivalent (cache TanStack Query, hooks React).

---

## 🎯 Principes directeurs

1. **Mirror du backend** — Un module backend = un module frontend. Même nom, même périmètre, même langage métier. `org.store.users` côté backend ↔ `features/users/` côté frontend.
2. **4 couches DDD** — `domain` (modèle pur, types, schemas, règles), `application` (orchestration, use cases), `infrastructure` (clients API, persistance navigateur), `presentation` (composants UI, pages).
3. **Domain pur, indépendant de React** — Les types, schemas Zod, règles métier sont du TypeScript pur, testables sans render, réutilisables dans une éventuelle app mobile/CLI.
4. **Application orchestre, ne contient pas la donnée** — Les hooks `useQuery`/`useMutation` orchestrent les appels et la cache. Comme `<X>ServiceImpl` côté backend.
5. **Infrastructure = adaptateurs externes** — Client axios, parsing erreurs API, localStorage. Comme les `<X>JpaRepository` côté backend.
6. **Presentation découplée du domain** — Une page importe des hooks/composants `presentation/`, jamais directement le `domain` ou l'`infrastructure`.

---

## 📁 Arborescence projet

```
store-frontend/
├── public/
├── src/
│   ├── app/                          # Next.js App Router (routing file-system uniquement)
│   │   ├── layout.tsx                # Layout racine (Providers, fonts)
│   │   ├── page.tsx                  # Landing publique
│   │   ├── error.tsx
│   │   ├── not-found.tsx
│   │   │
│   │   ├── (auth)/                   # Group routes publiques
│   │   │   ├── layout.tsx
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   └── forgot-password/page.tsx
│   │   │
│   │   ├── (dashboard)/              # Group routes authentifiées
│   │   │   ├── layout.tsx            # Sidebar + Header + garde JWT
│   │   │   ├── page.tsx              # Accueil dashboard
│   │   │   ├── employees/
│   │   │   │   ├── page.tsx          # ↳ délègue à features/users/presentation/pages/EmployeesListPage
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
│   │   └── (admin)/                  # Group routes ADMIN_ACCESS only
│   │       ├── layout.tsx
│   │       ├── entreprises/page.tsx
│   │       └── plans/page.tsx
│   │
│   ├── features/                     # ⭐ Modules métier (miroir backend, structure DDD)
│   │   ├── security/                 # ↔ org.store.security
│   │   │   ├── domain/
│   │   │   │   ├── enums.ts          # PermissionCode (miroir enum backend)
│   │   │   │   ├── types.ts          # UserPrincipal, Account, Role
│   │   │   │   ├── schemas.ts        # LoginSchema, AccountSchema, ChangePasswordSchema
│   │   │   │   └── rules.ts          # hasPermission(perm), isAdmin(user), isProprietaire(user)
│   │   │   ├── application/
│   │   │   │   ├── auth-store.ts     # Zustand : auth state (≈ session domain)
│   │   │   │   ├── use-auth.ts       # useLogin, useLogout, useRefresh, useChangePassword
│   │   │   │   └── use-permission.ts # hook dérivé de auth-store
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
│   │   │   │   ├── enums.ts          # EmployeRole (miroir)
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
│   ├── common/                       # ↔ org.store.common (utilitaires transverses)
│   │   ├── domain/
│   │   │   ├── types.ts              # PageResponse<T>, ErrorResponse, ImageDownloadResponse
│   │   │   ├── enums.ts              # MoyenPaiement (miroir transverse)
│   │   │   └── schemas.ts            # schemas Zod transverses (DatePattern, PhoneNumber, ...)
│   │   ├── application/
│   │   │   ├── use-debounce.ts
│   │   │   ├── use-pagination.ts
│   │   │   └── use-toast.ts
│   │   ├── infrastructure/
│   │   │   ├── api-client.ts         # axios instance + intercepteurs (équivalent du client REST)
│   │   │   ├── api-errors.ts         # parsing erreurs backend (i18n key)
│   │   │   ├── env.ts                # parsing env vars typesafe via Zod
│   │   │   └── query-client.ts       # config TanStack Query
│   │   └── presentation/
│   │       ├── ui/                   # shadcn/ui copiés (button, input, dialog, table, ...)
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
│   └── providers/                    # Wrappers React Provider (équivalent @Configuration backend)
│       ├── QueryProvider.tsx         # TanStack QueryClientProvider
│       ├── ThemeProvider.tsx
│       └── ToastProvider.tsx
│
├── tests/                            # Tests d'intégration cross-features (Playwright E2E)
├── tsconfig.json
├── tailwind.config.ts
├── next.config.ts
├── package.json
├── .env.example
└── components.json                   # config shadcn/ui
```

---

## 🧩 Les 4 couches — détail par responsabilité

### 1️⃣ `domain/` — Modèle métier pur

**Rôle** : représenter les concepts métier en TypeScript pur, sans dépendance React/axios/router. C'est le pendant de `org.store.<module>.domain/` côté backend (entités, value objects, enums, services métier purs).

**Contenu** :
- `types.ts` — types TS représentant les agrégats métier (`Employe`, `CommandeVente`, etc.). Dérivés de Zod via `z.infer`.
- `schemas.ts` — Zod schemas, **source de vérité** pour validation runtime + types TS. Miroir des `@NotBlank/@Email/@Min` des records Java.
- `enums.ts` — TS enums miroirs des enums Java (`PermissionCode`, `EmployeRole`, `InventaireStatut`, `StatutFacture`, etc.).
- `rules.ts` — fonctions pures de règles métier réutilisables (`canManagerActOn(employe)`, `computeBenefice(...)`). Équivalent des méthodes statiques pures d'un domain service backend.

**Règle stricte** : 0 import React, 0 import axios, 0 import Next.js. Que du TS pur. Testable avec `vitest` sans renderer.

**Pourquoi** : la domain layer reste portable. Si demain on fait React Native ou un CLI Node.js, on réutilise tel quel.

---

### 2️⃣ `application/` — Orchestration & use cases

**Rôle** : orchestrer les opérations métier en composant `infrastructure` + `domain`. Pendant frontend de `org.store.<module>.application.service.*ServiceImpl`.

**Contenu** :
- Hooks TanStack Query (`useEmployes`, `useCreateEmploye`, etc.). Équivalent fonctionnel d'un `EmployeServiceImpl.create()`.
- Stores Zustand (`auth-store.ts`, `ui-store.ts`). Équivalent d'un service applicatif qui maintient un état de session ou de scope.
- Hooks de logique réutilisable (`useCurrentUser`, `usePermission`, `useCurrentMagasin`). Équivalent d'un `ICurrentUserService`.

**Règle** : ne contient **jamais** d'appel axios direct ni de JSX. Reçoit l'API depuis `infrastructure/api.ts`, applique des transformations, gère le cache TanStack Query, expose des hooks que la couche `presentation` consomme.

**Mapping backend ↔ frontend** :
| Backend (Java) | Frontend (TS) |
|---|---|
| `EmployeServiceImpl.create(req)` | `useCreateEmploye()` (mutation TanStack) |
| `EmployeServiceImpl.findAllByCurrentEntreprise(filter)` | `useEmployes(filter)` (query TanStack) |
| `ICurrentUserService.getCurrent()` | `useAuthStore((s) => s.user)` |
| `IPermissionsService.findAllByRoleId(...)` | dérivé de `auth-store.user.permissions` |
| `@Transactional` orchestration | mutation TanStack avec `onSuccess: () => invalidateQueries(...)` |

---

### 3️⃣ `infrastructure/` — Adaptateurs externes

**Rôle** : tout ce qui parle à un monde extérieur (réseau, navigateur, stockage). Pendant frontend de `org.store.<module>.infrastructure.repository.*JpaRepository`.

**Contenu** :
- `api.ts` par feature — fonctions axios pures (`employesApi.list`, `.create`, etc.). Équivalent du `*JpaRepository`.
- Adaptateurs LocalStorage / sessionStorage / cookies si besoin (`jwt-storage.ts`).
- Mappers spécifiques si la response API a une forme différente du type domain (rare, mais possible).

**Règle** : pas de `useQuery` ici (c'est le rôle de `application/`). Pas de JSX. Que des fonctions qui prennent un input et retournent une `Promise<Response>` typée.

**Pourquoi séparer** : si on change axios pour fetch, ou si on ajoute un cache offline (Service Worker), on modifie `infrastructure` sans toucher au reste.

---

### 4️⃣ `presentation/` — UI

**Rôle** : composants visuels + pages. Pendant frontend de `org.store.<module>.presentation.*Controller`.

**Contenu** :
- `components/` — composants React feature-spécifiques (`EmployeForm`, `EmployesTable`, `ResetPasswordDialog`). Ils consomment des hooks de `application/`.
- `pages/` — composants "vue complète" qui assemblent plusieurs `components`. Ils sont importés depuis `app/.../page.tsx`.

**Règle d'or** : les composants `presentation/` peuvent importer depuis `application/` (hooks) et `domain/` (types). **Jamais** depuis `infrastructure/` directement (la séparation est sacrée).

**Pourquoi** :
- `app/.../page.tsx` reste minimal et lié au routing Next.js
- Si on migre vers Remix, Vite, ou Tanstack Router demain, on déplace juste `app/` et tout `presentation/` reste intact

---

## 🔍 Exemple complet — feature `users/` (employés)

### `features/users/domain/enums.ts`

```ts
// Miroir backend : org.store.security.application.enums.PermissionCode
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

// Règle pure : un MANAGER ne peut agir que sur les employés de son magasin
// Miroir de EmployeServiceImpl.ensureAccessibleByManager côté backend.
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
      <PageHeader title="Employés" action={<NewEmployeButton />} />
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

> La page Next.js est un **re-export pur**. Toute la logique vit dans `features/`. C'est l'équivalent d'un `EmployeController` minimal qui délègue à `IEmployeService`.

---

## 🔁 Mapping module-à-module avec le backend

| Module backend (`org.store.<x>`) | Module frontend (`features/<x>/`) | Particularités |
|---|---|---|
| `security` | `security/` | Auth store Zustand dans `application/`. PermissionGuard composant `presentation/`. |
| `users` | `users/` | Inclut Employe + Proprietaire + UserProfile. |
| `magasin` | `magasin/` | + sélecteur magasin pour propriétaire multi-magasins (`MagasinContextStore`). |
| `entreprise` | `entreprise/` | Distinction route `/me` (PROPRIETAIRE) vs `/admin/entreprises` (ADMIN). |
| `produit` | `produit/` | Sous-modules `category`, `quality`, `product-fournisseur` regroupés dans le même feature (cohésion forte). |
| `achat` | `achat/` | Fournisseurs + commandes + factures + paiements achat. |
| `vente` | `vente/` | Client + commandes + factures + paiements + caisse (top produits, résumé journalier). |
| `stock` | `stock/` | Entrées, sorties, ajustements, mouvements, valorisation, seuils, lots expirants, marges. |
| `depense` | `depense/` | Catégories + dépenses. |
| `inventaire` | `inventaire/` | Inventaire physique + rapport comptable. |
| `abonnement` | `abonnement/` | Plans, types, coupons, promotions, paiements (à venir, voir TODO). |
| `common` | `common/` | Helpers transverses, types partagés (`PageResponse`, etc.), composants UI. |

**Règle d'or** : si un module n'existe pas côté backend, il n'existe pas côté frontend. Si on crée un module backend, on crée le module frontend miroir.

---

## 🌐 Couche commune `common/` (détaillée)

### `common/infrastructure/api-client.ts`

```ts
import axios, { AxiosError } from 'axios'
import { env } from './env'
import { useAuthStore } from '@/features/security/application/auth-store'
import { parseApiError } from './api-errors'

export const apiClient = axios.create({ baseURL: env.NEXT_PUBLIC_API_URL })

// Injection JWT (équivalent côté front du JwtAuthenticationFilter backend)
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Refresh 401 + parsing erreurs i18n-friendly
apiClient.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    // ... logique refresh + retry ...
    return Promise.reject(parseApiError(error))
  }
)
```

### `common/infrastructure/api-errors.ts`

```ts
// Parse les réponses d'erreur du backend Spring (GlobalException → ErrorResponse)
// pour exposer une clé i18n + status au reste du frontend
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
// Miroir des réponses Spring Data paginées (PageImpl)
export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number   // page courante
  size: number
  first: boolean
  last: boolean
}

// Miroir de org.store.common.dto.ImageDownloadResponse côté backend
export type ImageDownloadResponse = { content: ArrayBuffer; contentType: string }
```

---

## 🔐 Authentification & permissions (DDD-aligned)

### `features/security/domain/types.ts`

```ts
// Miroir de org.store.security.application.dto.UserPrincipal
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

**Usage** :
```tsx
<PermissionGuard required={[PermissionCode.EMPLOYE_DELETE]}>
  <Button onClick={onDeactivate}>Désactiver</Button>
</PermissionGuard>
```

---

## 🎨 Conventions UI

1. **Tailwind only** — pas de `style={{}}` inline. `cn()` pour composer (`cn('px-4 py-2', isActive && 'bg-primary')`).
2. **shadcn/ui copié dans `common/presentation/ui/`** — modifiable directement, possédé par le projet.
3. **Couleurs sémantiques** dans `tailwind.config.ts` : `primary`, `secondary`, `destructive`, `muted`, `accent`. Jamais `bg-red-500`.
4. **Dark mode** via CSS variables shadcn (`[data-theme="dark"]`).
5. **Mobile-first** : classes responsives (`md:`, `lg:`) ajoutées par-dessus le base.
6. **Logos / images** : `next/image` obligatoire (optimisations auto). URL backend relative → ajouter `apiClient.defaults.baseURL` comme préfixe (helper utilitaire).

---

## 📊 Composants transverses (`common/presentation/shared/`)

| Composant | Rôle |
|---|---|
| `DataTable<T>` | Wrapper @tanstack/react-table + shadcn `<Table>`. Gère tri/pagination/loading/empty/actions. |
| `PageHeader` | Titre + breadcrumbs + slot actions (boutons CTA). |
| `ConfirmDialog` | Dialogue de confirmation pour actions destructives. |
| `EmptyState` | Affichage standardisé "aucune donnée". |
| `ErrorBanner` | Affichage standardisé d'une erreur (avec `messageKey` i18n). |
| `FormField` | Wrapper RHF + Label + Input + ErrorMessage. |
| `PhoneInput` | Input téléphone avec validation Phone (miroir validation backend). |

---

## 🧪 Tests (alignés sur la stratégie backend)

| Niveau | Tool | Cible | Équivalent backend |
|---|---|---|---|
| **Domain pur** | Vitest | `schemas` (validation Zod), `rules.ts`, helpers `utils.ts` | tests JUnit purs sans `@SpringBootTest` |
| **Application** | Vitest + React Testing Library | hooks `useEmployes`/`useCreateEmploye` (avec QueryClient mock) | tests `*ServiceImplTest` avec mocks |
| **Presentation** | React Testing Library | composants critiques (forms, guards, tables) | tests `*ControllerTest` avec MockMvc |
| **E2E** | Playwright | 5-10 scénarios bout-en-bout (login, création employé, vente complète) | pas d'équivalent backend (tests d'intégration Spring séparés) |

**Règle** : tests unitaires sur la `domain/` à 80%+. Tests sur l'`application/` pour les hooks complexes. Tests `presentation/` ciblés (formulaires + guards). Pas de TDD obsessionnel — tester ce qui peut casser.

---

## ⚙️ Outillage

| Tool | Rôle | Équivalent backend |
|---|---|---|
| TypeScript strict | Type safety | Java type system |
| ESLint + Prettier | Linting + format | (rien d'équivalent imposé) |
| Husky + lint-staged | Pre-commit hook | (à mettre en place côté backend aussi) |
| Vitest | Tests unitaires | JUnit 5 |
| Playwright | E2E | (à venir) |
| commitlint | Conventional commits FR | déjà appliqué côté backend |

---

## 🔄 Workflow type pour une nouvelle feature

1. **Domain first** — Créer `features/<f>/domain/schemas.ts` (Zod), `types.ts`, `enums.ts`, `rules.ts` si règles métier
2. **Infrastructure** — Créer `features/<f>/infrastructure/api.ts` (fonctions axios pures)
3. **Application** — Créer `features/<f>/application/use-<f>.ts` (hooks TanStack Query)
4. **Presentation** — Créer composants dans `features/<f>/presentation/components/`, puis pages dans `presentation/pages/`
5. **Routing** — Re-exporter la page depuis `app/(dashboard)/<f>/page.tsx`
6. **Tests** — Tester `domain/` (Vitest) et composants critiques (Testing Library)

**Règle d'or** : `app/.../page.tsx` ne contient JAMAIS de logique. Que des `import` + `export default`.

---

## ⚡ Performance — leviers à connaître

1. **Server Components par défaut** — Tout ce qui n'est pas interactif reste serveur (économie bundle).
2. **`next/image`** pour toutes les images (formats modernes, lazy loading).
3. **`next/font`** pour les fonts (autohébergées, pas de FOUT).
4. **TanStack Query `staleTime`** — 30s par défaut, 5min pour données quasi-statiques (plans abonnement).
5. **Skeleton loaders** plutôt que spinners.
6. **`useDebounce` ~300ms** sur les filtres de recherche.
7. **Code splitting auto Next.js** — chaque page = chunk dédié.

---

## 🌍 Internationalisation (futur, miroir backend)

- Backend a `messages.properties` (FR) + `messages_en.properties` (EN). Frontend doit suivre la **même map de clés**.
- `messageKey` retournée par les erreurs backend (`employe.notFound`, `account.currentPassword.invalid`) est résolue côté front via `next-intl` ou équivalent.
- Pour démarrer : **français en dur** suffit. L'i18n viendra quand le marché EN sera prioritaire.

---

## 🛑 Anti-patterns à éviter

1. **Mélanger les couches** — Un composant `presentation/` qui appelle `apiClient.get()` directement → contourne `infrastructure/api.ts`. Refactor obligatoire.
2. **Logique métier dans `presentation/`** — Si tu écris un `if (user.role === 'MANAGER' && ...)` dans un composant, déporte dans `domain/rules.ts`.
3. **Composants partagés ailleurs que dans `common/`** — Si `presentation/components/EmployeCard` est réutilisé dans `vente/`, il monte dans `common/presentation/shared/`.
4. **Types dupliqués** — Toujours `z.infer<typeof schema>`. Jamais redéclaration manuelle.
5. **Données API dans Zustand** — TanStack Query pour le state serveur. Zustand pour le state client uniquement.
6. **`useEffect` pour fetch** — Toujours `useQuery`.
7. **Couleur en dur** — Toujours via Tailwind theme (`bg-primary`, pas `bg-blue-500`).
8. **Composant UI wrapper inutile** — Si `<MyButton>` ne fait que wrapper `<Button>` shadcn sans ajout, supprimer.

---

## 📋 Checklist de démarrage `store-frontend`

**Phase 0 — Setup (1 jour)**
- [ ] `npx create-next-app@latest store-frontend --typescript --tailwind --app`
- [ ] Installer shadcn/ui (`npx shadcn@latest init`) + composants de base (button, input, dialog, table, toast, dropdown-menu, badge, form, select, popover)
- [ ] Installer TanStack Query, Zustand, axios, RHF, @hookform/resolvers, zod, @tanstack/react-table
- [ ] Créer la structure `src/{app,features,common,providers}/`
- [ ] Configurer `tailwind.config.ts` (couleurs sémantiques + dark mode)
- [ ] Setup ESLint + Prettier + Husky + lint-staged + commitlint

**Phase 1 — Fondations (3 jours)**
- [ ] `common/infrastructure/{api-client,api-errors,env,query-client}.ts`
- [ ] `common/domain/types.ts` (PageResponse, etc.)
- [ ] `common/presentation/ui/` (shadcn copiés)
- [ ] `common/presentation/shared/` (DataTable, PageHeader, ConfirmDialog, FormField, etc.)
- [ ] `features/security/` complet (4 couches)
- [ ] `app/(auth)/{login,register}/page.tsx` (re-exports)
- [ ] `app/(dashboard)/layout.tsx` avec garde JWT

**Phase 2 — Première feature complète (2 jours)**
- [ ] `features/users/` complet (4 couches) : Employe + UserProfile
- [ ] Pages `app/(dashboard)/employees/{page,new/page,[id]/page}.tsx`
- [ ] Pages `app/(dashboard)/settings/{profile,security}/page.tsx`

**Phase 3 — Itérations features (rythme : 1 feature par jour, ordre conseillé)**
- [ ] `magasin/` (CRUD admin + logo)
- [ ] `entreprise/` (profil propriétaire + logo + admin)
- [ ] `produit/` (CRUD + galerie images)
- [ ] `stock/` (entrées, sorties, ajustements, valorisation)
- [ ] `achat/` (commandes, factures, paiements)
- [ ] `vente/` (commandes, factures, paiements, caisse, top-produits)
- [ ] `depense/` (catégories + dépenses)
- [ ] `inventaire/` (physique + rapport)
- [ ] `abonnement/` (souscription + plans publics)

**Phase 4 — Polish**
- [ ] Skeleton loaders cohérents
- [ ] Confirmations destructives (ConfirmDialog partout)
- [ ] Tests Playwright sur 5 flows critiques
- [ ] Optimisations Lighthouse (LCP < 2.5s)
- [ ] README + CONTRIBUTING.md

---

## 🎓 Comment cette architecture évolue

- **Server Actions Next.js** : ajout possible dans `features/<f>/application/server-actions.ts` (Next.js 14+), sans casser la structure DDD.
- **Monorepo (front + back ensemble)** : déplacer `store-frontend` dans `apps/frontend/`, packager les types domain partagés dans `packages/domain-types/` (réutilisable React Native si nécessaire).
- **App mobile React Native** : `features/<f>/{domain,application,infrastructure}/` réutilisables tels quels. Seul `presentation/` change (React Native primitives au lieu de shadcn).
- **CLI Node.js (script admin)** : `features/<f>/{domain,infrastructure}/` réutilisables. Pas besoin d'`application/` (hooks React) ni de `presentation/`.

**La structure DDD résiste au temps**. C'est exactement pour ça qu'on l'utilise côté backend, et c'est pareil côté front.
