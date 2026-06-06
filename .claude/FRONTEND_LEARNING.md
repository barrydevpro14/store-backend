# FRONTEND_LEARNING.md — Learning guide for the current stack

> Transition doc to move from **React + Redux + Middleware + MUI** (familiar stack) to **Next.js + TanStack Query + Zustand + React Hook Form + Zod + shadcn/ui** (project stack).

---

## 🎯 Why this stack

The project stack is not a technical whim: it's the one teams choose when they start a SaaS in 2025/2026. Each piece answers a specific problem that Redux + MUI solved with a lot of boilerplate:

| Problem | Redux + MUI answer (verbose) | Current stack answer (concise) |
|---|---|---|
| Fetch API data with cache/loading/error | Redux + Thunk + 3 files (actions, reducer, slice) | `useQuery(...)` one line |
| Local client state (modal open, user logged in) | Redux store + slice + dispatch | `useStore()` Zustand, 10 lines total |
| Validated form | Formik + Yup | React Hook Form + Zod (perf, typesafe) |
| Accessible UI component | MUI (heavy, opinionated) | shadcn/ui (copied into the project, customizable) |
| Routing + SEO | React Router (CSR only) | Next.js App Router (SSR + CSR as you wish) |

**Expected investment**: ~2 weeks to master the 4 pillars. You'll be **more productive** than with Redux + MUI once the curve is behind you.

---

## 1️⃣ Zustand — Client state (easiest, 1h)

**Concept**: a global store accessible from any component via a hook, without a `Provider`, without actions, without reducers. You just make an object with your state and your functions.

### Example: auth store

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

### Use in a component

```tsx
function Header() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  return <button onClick={logout}>Sign out ({user?.username})</button>
}
```

**Redux → Zustand equivalences**:
| Redux | Zustand |
|---|---|
| `createSlice` + `actions` + `reducer` | `create((set) => ({ ... }))` |
| `dispatch(action(payload))` | direct function call on the store |
| `useSelector(state => state.auth.user)` | `useAuthStore((s) => s.user)` |
| `Provider` around the app | nothing, the hook works everywhere |
| 3 files per feature | 1 file |

**Golden rule**: use Zustand **only** for client state (auth, UI, preferences). Data coming from the API → TanStack Query.

**Docs**: https://docs.pmnd.rs/zustand

---

## 2️⃣ TanStack Query — Server state (1 day)

**Concept**: you no longer store API data in Redux. You *query* it on demand. TanStack Query handles, for you: cache, deduplication, background refetch, invalidation after mutation, retry on error, etc.

### Read (`useQuery`)

```tsx
// Fetch the list of employees
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

### Write (`useMutation`)

```tsx
function CreateEmployeeForm() {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (payload) => api.post('/api/v1/employees', payload),
    onSuccess: () => {
      // Invalidates the cache → the list auto-reloads
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      toast.success('Employee created')
    },
    onError: (err) => toast.error(err.message),
  })

  return <form onSubmit={(data) => mutation.mutate(data)}>...</form>
}
```

### Why it's better than Redux + Thunk

- **Automatic cache**: if 3 components ask for `['employees']`, only 1 HTTP call.
- **Background refetch**: when you come back to the tab, TanStack silently revalidates.
- **Optimistic update**: mutation visible before the server response, rollback on error.
- **No manual loading/error spinner**: `isLoading`, `error`, `isFetching` exposed for free.
- **Pagination/infinite scroll**: `useInfiniteQuery`.
- **Typesafe with TypeScript** with no effort.

### Project pattern: one axios client + JWT interceptor

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

**Docs**: https://tanstack.com/query/latest/docs/framework/react/overview

---

## 3️⃣ React Hook Form + Zod — Forms (1 day)

**Concept**: a Zod schema = source of truth (generated TS types + runtime validation). RHF manages the form state without re-rendering on every keystroke (perf).

### Complete example

```tsx
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

// 1. Zod schema (= backend rules + TS types in one shot)
const employeeSchema = z.object({
  nom: z.string().min(1, 'Last name is required'),
  prenom: z.string().min(1, 'First name is required'),
  email: z.string().email('Invalid email'),
  role: z.enum(['MANAGER', 'VENDEUR']),
})
type EmployeeForm = z.infer<typeof employeeSchema> // automatic types

// 2. Component
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
      <button type="submit">Create</button>
    </form>
  )
}
```

**Benefits vs Formik + Yup**:
- No `<Field>`, `<FieldArray>`: just `register` or `<Controller>` for custom components.
- 10× faster re-renders (RHF uses refs).
- Automatic TypeScript inference from the Zod schema.
- Zod schemas reusable elsewhere (server-side validation if you go Next.js full-stack, API type derivation, etc.).

**Docs**: https://react-hook-form.com + https://zod.dev

---

## 4️⃣ Next.js App Router — Routing + SSR (1 week, the real curve)

**Concept**: a file-system based router (`app/dashboard/page.tsx` = `/dashboard`). Distinction between **Server Components** (rendered on the server) and **Client Components** (`'use client'`, rendered in the browser).

### Basic structure

```
src/app/
  layout.tsx              # root layout, persists across navigation
  page.tsx                # public homepage /
  (auth)/                 # group with no URL prefix
    login/page.tsx        # → /login
    register/page.tsx     # → /register
  (dashboard)/
    layout.tsx            # shared dashboard layout (sidebar, header)
    employees/
      page.tsx            # → /employees (list)
      [id]/page.tsx       # → /employees/abc-123 (dynamic detail)
      new/page.tsx        # → /employees/new
```

### Server Component vs Client Component

```tsx
// Server Component (default) — no hooks, can fetch directly
// app/(dashboard)/employees/page.tsx
export default async function EmployeesPage() {
  // This call happens SERVER-SIDE (no visible loading on the client)
  const res = await fetch('http://api/employees', { cache: 'no-store' })
  const data = await res.json()
  return <EmployeesList initialData={data} />
}
```

```tsx
// Client Component — hooks, events, state
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

### Rules to remember

1. **By default, everything is a Server Component**. Add `'use client'` at the top of the file only if you need hooks, events, or state.
2. **Server Components cannot**: `useState`, `useEffect`, DOM events (`onClick`).
3. **Client Components cannot**: direct `async/await` (use `useQuery`).
4. **You can import a Client Component from a Server Component**, never the opposite as a direct parent.
5. **Navigation**: `<Link href="/employees/123">` or `router.push()` via `useRouter` (from `next/navigation`, not `next/router`).

### Session guard (auth required)

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

**Docs**: https://nextjs.org/docs/app

---

## ⭐ Bonus: shadcn/ui

**Not a library**: a catalog of accessible components (Radix Primitives + Tailwind) that you **copy** into your project via the CLI:

```bash
npx shadcn@latest add button
# Creates src/components/ui/button.tsx, edit as you like
```

You own the code → tweaking colors / radius / animations = editing a file in your repo.

Advantages vs MUI:
- ~10× smaller final bundle (you ship only what you use).
- Trivial customization (no `sx`, theme overrides, etc.).
- You see the code, you debug the code.
- Tailwind = design system consistency.

Downside: more files in your repo. That's intentional (ownership).

**Docs**: https://ui.shadcn.com

---

## 🗺️ Learning roadmap (2 weeks)

### Week 1 — Isolated concepts

| Day | Topic | Deliverables |
|---|---|---|
| Mon | Zustand | Redo the project's auth store, add localStorage persistence |
| Tue | TanStack Query (read) | `useQuery` on 3 endpoints (employees, magasins, products) |
| Wed | TanStack Query (mutation) | `useMutation` + `invalidateQueries` for create/update/delete employee |
| Thu | RHF + Zod | Employee creation form with full validation |
| Fri | shadcn/ui | Copy 5 components (button, input, dialog, table, toast), assemble the employees list page |

### Week 2 — Next.js + integration

| Day | Topic | Deliverables |
|---|---|---|
| Mon-Tue | Next.js App Router, `layout.tsx`/`page.tsx` files | Pages `/login`, `/register`, `/dashboard` with session guard |
| Wed | Server vs Client Components | For each page, decide if it can be Server (public product catalog) or Client (dashboard with interactions) |
| Thu | Full-flow integration | Full end-to-end employees CRUD (list, create, edit, deactivate) |
| Fri | Polish | Global error toasts, loading skeletons, 401 handling (auto logout + login redirect) |

---

## ⚠️ Anti-patterns to unlearn (Redux/MUI reflexes to forget)

1. **❌ Don't put API data in Zustand**. Zustand = client state. TanStack Query = server state. If you want to refetch a list, it's `queryClient.invalidateQueries`, not a `dispatch`.

2. **❌ Don't create a "userSlice + employeesSlice + magasinsSlice"** like in Redux. You don't need slices: `useQuery(['employees'])` is enough, the TanStack Query cache IS your server store.

3. **❌ Don't write actions/thunks**. `useMutation` replaces all that in 3 lines.

4. **❌ Don't use `useEffect` to fetch data**. If you write `useEffect(() => fetch(...), [])` instead of a `useQuery`, you're back in Redux hell.

5. **❌ Don't look for a "Provider"**. Zustand doesn't need one. TanStack Query needs ONE, to put in `app/layout.tsx`.

6. **❌ Don't style with inline `style={{...}}` or `sx={{...}}`**. Use Tailwind classes (`className="px-4 py-2 rounded bg-primary"`). You can abstract into a shadcn component if it repeats.

7. **❌ Don't create a BaseDialog/BaseButton/BaseInput**. shadcn gives you `<Button>`, `<Dialog>`, `<Input>` already accessible. Modify the `components/ui/button.tsx` file if needed.

8. **❌ Don't make "container components" vs "presentational components"**. Obsolete pattern since hooks. A client component can be both.

---

## 📚 Priority resources

1. **TanStack Query**: official tutorial + TkDodo's video (https://tkdodo.eu/blog/practical-react-query) → essential.
2. **Next.js App Router**: official tutorial `nextjs.org/learn/dashboard-app` (60-90 min, complete project).
3. **Zustand**: the docs fit in 10 minutes.
4. **RHF + Zod**: official RHF tutorial + "with schema validation" section.
5. **shadcn/ui**: the docs are the component code itself, nothing else needed.

---

## 🚦 When you're comfortable

Once these 4 pillars are mastered, you'll have what you need to ship **the entire store-frontend UI** without frustration. The patterns become second nature:
- New page = new `page.tsx` file
- New data = new `useQuery`
- New action = new `useMutation`
- New form = new Zod schema + `useForm`
- New global UI state = new Zustand store (or addition to an existing one)

No boilerplate, no ceremony. And you keep the door open to evolve (RSC, server actions, streaming, etc.) along the way.
