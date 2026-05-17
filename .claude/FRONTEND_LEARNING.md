# FRONTEND_LEARNING.md — Guide d'apprentissage du stack actuel

> Document de transition pour passer de **React+Redux+Middleware+MUI** (stack connu) vers **Next.js + TanStack Query + Zustand + React Hook Form + Zod + shadcn/ui** (stack du projet).

---

## 🎯 Pourquoi ce stack

Le stack du projet n'est pas un caprice technique : c'est celui que choisissent les équipes qui démarrent un SaaS en 2025/2026. Chaque pièce répond à un problème précis que Redux+MUI résolvait avec beaucoup de boilerplate :

| Problème | Réponse Redux+MUI (verbeux) | Réponse stack actuel (concis) |
|---|---|---|
| Récupérer des données API avec cache/loading/error | Redux + Thunk + 3 fichiers (actions, reducer, slice) | `useQuery(...)` une ligne |
| State client local (modale ouverte, user connecté) | Redux store + slice + dispatch | `useStore()` Zustand, 10 lignes au total |
| Formulaire validé | Formik + Yup | React Hook Form + Zod (perfs, typesafe) |
| Composant UI accessible | MUI (lourd, opinionné) | shadcn/ui (copié dans le projet, customizable) |
| Routing + SEO | React Router (CSR seul) | Next.js App Router (SSR + CSR au choix) |

**Investissement attendu** : ~2 semaines pour maîtriser les 4 piliers. Tu vas être **plus productif** qu'en Redux+MUI une fois la courbe passée.

---

## 1️⃣ Zustand — State client (le plus facile, 1h)

**Concept** : un store global accessible depuis n'importe quel composant via un hook, sans `Provider`, sans actions, sans reducers. Tu fais juste un objet avec ton state et tes fonctions.

### Exemple : store d'authentification

```ts
// src/lib/stores/auth-store.ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

type AuthState = {
  accessToken: string | null
  user: { id: string; username: string; role: string } | null
  setAuth: (token: string, user: AuthState['user']) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      setAuth: (accessToken, user) => set({ accessToken, user }),
      logout: () => set({ accessToken: null, user: null }),
    }),
    { name: 'auth-storage' } // localStorage
  )
)
```

### Utilisation dans un composant

```tsx
function Header() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  return <button onClick={logout}>Déconnexion ({user?.username})</button>
}
```

**Équivalences Redux → Zustand** :
| Redux | Zustand |
|---|---|
| `createSlice` + `actions` + `reducer` | `create((set) => ({ ... }))` |
| `dispatch(action(payload))` | appel direct de la fonction du store |
| `useSelector(state => state.auth.user)` | `useAuthStore((s) => s.user)` |
| `Provider` autour de l'app | rien, le hook marche partout |
| 3 fichiers par feature | 1 fichier |

**Règle d'or** : utilise Zustand **uniquement** pour du state client (auth, UI, préférences). Les données qui viennent de l'API → TanStack Query.

**Doc** : https://docs.pmnd.rs/zustand

---

## 2️⃣ TanStack Query — State serveur (1 jour)

**Concept** : tu ne stockes plus les données API dans Redux. Tu les *queries* à la demande. TanStack Query gère pour toi : cache, deduplication, refetch en background, invalidation après mutation, retry sur erreur, etc.

### Lecture (`useQuery`)

```tsx
// Récupérer la liste des employés
function EmployeesList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['employees', { actif: true }],
    queryFn: () => api.get('/api/v1/employees?actif=true').then(r => r.data),
  })

  if (isLoading) return <Skeleton />
  if (error) return <ErrorBanner error={error} />
  return data.content.map(e => <EmployeeCard key={e.id} employee={e} />)
}
```

### Écriture (`useMutation`)

```tsx
function CreateEmployeeForm() {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (payload) => api.post('/api/v1/employees', payload),
    onSuccess: () => {
      // Invalide le cache → la liste se recharge auto
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      toast.success('Employé créé')
    },
    onError: (err) => toast.error(err.message),
  })

  return <form onSubmit={(data) => mutation.mutate(data)}>...</form>
}
```

### Pourquoi c'est mieux que Redux + Thunk

- **Cache automatique** : si 3 composants demandent `['employees']`, 1 seul appel HTTP
- **Refetch en background** : revenant sur l'onglet, TanStack revalide silencieusement
- **Optimistic update** : mutation visible avant la réponse serveur, rollback si erreur
- **Pas de loading/error spinner manuel** : `isLoading`, `error`, `isFetching` exposés gratos
- **Pagination/infinite scroll** : `useInfiniteQuery`
- **Typesafe avec TypeScript** sans effort

### Pattern projet : un client axios + intercepteur JWT

```ts
// src/lib/api/client.ts
import axios from 'axios'
import { useAuthStore } from '../stores/auth-store'

export const api = axios.create({ baseURL: process.env.NEXT_PUBLIC_API_URL })

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401) useAuthStore.getState().logout()
    return Promise.reject(error)
  }
)
```

**Doc** : https://tanstack.com/query/latest/docs/framework/react/overview

---

## 3️⃣ React Hook Form + Zod — Forms (1 jour)

**Concept** : un schema Zod = source de vérité (types TS générés + validation runtime). RHF gère le state du formulaire sans re-render à chaque keystroke (perf).

### Exemple complet

```tsx
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

// 1. Schéma Zod (= règles backend + types TS d'un seul coup)
const employeeSchema = z.object({
  nom: z.string().min(1, 'Nom obligatoire'),
  prenom: z.string().min(1, 'Prénom obligatoire'),
  email: z.string().email('Email invalide'),
  role: z.enum(['MANAGER', 'VENDEUR']),
})
type EmployeeForm = z.infer<typeof employeeSchema> // types auto

// 2. Composant
function NewEmployeeForm() {
  const { register, handleSubmit, formState: { errors } } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema),
  })

  const mutation = useMutation({
    mutationFn: (data: EmployeeForm) => api.post('/api/v1/employees', data),
  })

  return (
    <form onSubmit={handleSubmit(mutation.mutate)}>
      <input {...register('nom')} />
      {errors.nom && <span>{errors.nom.message}</span>}
      <input {...register('email')} />
      {errors.email && <span>{errors.email.message}</span>}
      <button type="submit">Créer</button>
    </form>
  )
}
```

**Bénéfices vs Formik+Yup** :
- Pas de `<Field>`, `<FieldArray>` : juste `register` ou `<Controller>` pour composants custom
- 10× plus rapide en re-render (RHF utilise des refs)
- Inférence TypeScript automatique depuis le schema Zod
- Zod schemas réutilisables ailleurs (validation côté serveur si tu fais du Next.js full-stack, dérivation de types API, etc.)

**Doc** : https://react-hook-form.com + https://zod.dev

---

## 4️⃣ Next.js App Router — Routing + SSR (1 semaine, la vraie courbe)

**Concept** : un router file-system based (`app/dashboard/page.tsx` = `/dashboard`). Distinction **Server Components** (rendus sur serveur) vs **Client Components** (`'use client'`, rendus dans le navigateur).

### Structure de base

```
src/app/
  layout.tsx              # layout racine, persiste entre navigations
  page.tsx                # homepage publique /
  (auth)/                 # groupe sans préfixe URL
    login/page.tsx        # → /login
    register/page.tsx     # → /register
  (dashboard)/
    layout.tsx            # layout commun dashboard (sidebar, header)
    employees/
      page.tsx            # → /employees (liste)
      [id]/page.tsx       # → /employees/abc-123 (détail dynamique)
      new/page.tsx        # → /employees/new
```

### Server Component vs Client Component

```tsx
// Server Component (par défaut) — pas de hooks, peut fetch direct
// app/(dashboard)/employees/page.tsx
export default async function EmployeesPage() {
  // Cet appel se fait CÔTÉ SERVEUR (pas de loading visible client)
  const res = await fetch('http://api/employees', { cache: 'no-store' })
  const data = await res.json()
  return <EmployeesList initialData={data} />
}
```

```tsx
// Client Component — hooks, événements, state
// app/(dashboard)/employees/_components/EmployeesList.tsx
'use client'
import { useQuery } from '@tanstack/react-query'

export function EmployeesList({ initialData }) {
  const { data } = useQuery({
    queryKey: ['employees'],
    queryFn: () => api.get('/employees').then(r => r.data),
    initialData,
  })
  return data.map(e => <EmployeeCard key={e.id} {...e} />)
}
```

### Règles à retenir

1. **Par défaut, tout est Server Component**. Ajoute `'use client'` en haut du fichier seulement si tu as besoin de hooks, événements ou state.
2. **Server Components ne peuvent pas** : `useState`, `useEffect`, événements DOM (`onClick`).
3. **Client Components ne peuvent pas** : `async/await` directement (utilise `useQuery`).
4. **Tu peux importer un Client Component depuis un Server Component**, jamais l'inverse en tant que parent direct.
5. **Navigation** : `<Link href="/employees/123">` ou `router.push()` via `useRouter` (depuis `next/navigation`, pas `next/router`).

### Garde de session (auth required)

```tsx
// src/app/(dashboard)/layout.tsx
'use client'
import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/lib/stores/auth-store'

export default function DashboardLayout({ children }) {
  const token = useAuthStore((s) => s.accessToken)
  const router = useRouter()

  useEffect(() => {
    if (!token) router.push('/login')
  }, [token, router])

  if (!token) return null
  return <div><Sidebar /><main>{children}</main></div>
}
```

**Doc** : https://nextjs.org/docs/app

---

## ⭐ Bonus : shadcn/ui

**Pas une lib** : un catalogue de composants accessibles (Radix Primitives + Tailwind) que tu **copies** dans ton projet via la CLI :

```bash
npx shadcn@latest add button
# Crée src/components/ui/button.tsx, à toi de le modifier
```

Tu possèdes le code → modifier les couleurs/radius/animations = éditer un fichier dans ton repo.

Avantages vs MUI :
- Bundle final ~10× plus léger (tu n'as que ce que tu utilises)
- Customization triviale (pas de `sx`, theme override, etc.)
- Tu vois le code, tu débugues le code
- Tailwind = consistance design system

Inconvénient : tu as plus de fichiers dans ton repo. C'est voulu (ownership).

**Doc** : https://ui.shadcn.com

---

## 🗺️ Roadmap d'apprentissage (2 semaines)

### Semaine 1 — Concepts isolés

| Jour | Sujet | Livrables |
|---|---|---|
| Lun | Zustand | Refais l'auth-store du projet, ajoute une persistance localStorage |
| Mar | TanStack Query (lecture) | `useQuery` sur 3 endpoints (employees, magasins, products) |
| Mer | TanStack Query (mutation) | `useMutation` + `invalidateQueries` pour create/update/delete employé |
| Jeu | RHF + Zod | Formulaire de création employé avec validation complète |
| Ven | shadcn/ui | Copie 5 composants (button, input, dialog, table, toast), assemble la page liste employés |

### Semaine 2 — Next.js + intégration

| Jour | Sujet | Livrables |
|---|---|---|
| Lun-Mar | Next.js App Router, fichiers `layout.tsx`/`page.tsx` | Pages `/login`, `/register`, `/dashboard` avec garde de session |
| Mer | Server vs Client Components | Identifie pour chaque page si elle peut être Server (catalogue produits public) ou Client (dashboard avec interactions) |
| Jeu | Intégration full-flow | CRUD employés complet bout-en-bout (liste, création, édition, désactivation) |
| Ven | Polish | Toasts d'erreur globaux, loading skeletons, gestion 401 (logout auto + redirect login) |

---

## ⚠️ Anti-patterns à désapprendre (réflexes Redux/MUI à oublier)

1. **❌ Ne pas mettre les données API dans Zustand**. Zustand = state client. TanStack Query = state serveur. Si tu veux refetcher une liste, c'est `queryClient.invalidateQueries`, pas un `dispatch`.

2. **❌ Ne pas créer un "userSlice + employeesSlice + magasinsSlice"** comme en Redux. Tu n'as pas besoin de slices : `useQuery(['employees'])` suffit, le cache TanStack Query EST ton store serveur.

3. **❌ Ne pas écrire d'actions/thunks**. `useMutation` remplace tout ça en 3 lignes.

4. **❌ Ne pas utiliser `useEffect` pour fetch des données**. Si tu écris `useEffect(() => fetch(...), [])` au lieu d'un `useQuery`, tu reviens dans la galère Redux.

5. **❌ Ne pas chercher un "Provider"**. Zustand n'en a pas besoin. TanStack Query en a UN seul, à mettre dans `app/layout.tsx`.

6. **❌ Ne pas styler avec inline `style={{...}}` ou `sx={{...}}`**. Utilise les classes Tailwind (`className="px-4 py-2 rounded bg-primary"`). Tu peux abstraire dans un composant shadcn si répétitif.

7. **❌ Ne pas créer un BaseDialog/BaseButton/BaseInput**. shadcn te donne `<Button>`, `<Dialog>`, `<Input>` déjà accessibles. Tu modifies le fichier `components/ui/button.tsx` si besoin.

8. **❌ Ne pas faire de "container component" vs "presentational component"**. Pattern obsolète depuis les hooks. Un composant client peut être les deux.

---

## 📚 Ressources prioritaires

1. **TanStack Query** : tutorial officiel + vidéo de TkDodo (https://tkdodo.eu/blog/practical-react-query) → indispensable
2. **Next.js App Router** : tutorial officiel `nextjs.org/learn/dashboard-app` (60-90 min, projet complet)
3. **Zustand** : la doc tient en 10 minutes
4. **RHF + Zod** : tutorial RHF officiel + section "with schema validation"
5. **shadcn/ui** : la doc est le code des composants, pas besoin d'autre chose

---

## 🚦 Quand tu seras à l'aise

Une fois ces 4 piliers maîtrisés, tu auras les outils pour livrer **toute l'UI du projet store-frontend** sans frustration. Les patterns deviennent naturels :
- Nouvelle page = nouveau fichier `page.tsx`
- Nouvelle donnée = nouveau `useQuery`
- Nouvelle action = nouveau `useMutation`
- Nouveau formulaire = nouveau schema Zod + `useForm`
- Nouveau state global UI = nouveau store Zustand (ou ajout dans un existant)

Pas de boilerplate, pas de cérémonie. Et tu gardes la porte ouverte pour évoluer (RSC, server actions, streaming, etc.) au fur et à mesure.
