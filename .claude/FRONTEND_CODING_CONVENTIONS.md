# FRONTEND_CODING_CONVENTIONS.md — Frontend coding rules

> Conventions and mandatory rules that apply to every addition or modification of **frontend** code (`store-frontend/`).
> For stack and frontend structure, see `ARCHITECTURE.md`. For the backend, see `BACKEND_CODING_CONVENTIONS.md`.

---

## Coding conventions

**TSX components / files**: `PascalCase.tsx` (except Next.js routes: `page.tsx`, `layout.tsx`, etc.)
**Hooks**: `useXxx.ts`
**Utils / lib**: `kebab-case.ts` or `camelCase.ts`
**Types**: `PascalCase`
**Variables / functions**: `camelCase`
**Internal imports**: use the `@/...` alias (e.g. `@/components/ui/button`, `@/lib/utils`)

---

## Strict TypeScript

- **No `any`**. If the type is unknown: `unknown` + narrowing, or `Type<T>` generic. Justify in a comment any unavoidable exception (`// @ts-expect-error: ...`).
- **No unjustified `as`** (type cast). Prefer explicit typing. If `as` is necessary (e.g. after `JSON.parse`), comment why.
- **No accidental nullish**: no `!` after a possibly-null variable without a prior guard. Use `??`, `?.`, or narrowing.
- **Shared DTO types**: in `src/types/` (mapped onto the backend's `<X>Request` / `<X>Response`). Eventually: auto-generate from the backend's OpenAPI (`openapi-typescript`).

---

## Architecture & components

- **Server Components by default**; add `"use client"` only when necessary (React hooks, state, DOM events).
- **shadcn**: generate components via the shadcn CLI in `src/components/ui/`. Don't modify them directly, extend via composition.
- **Business components**: `src/components/forms/`, `src/components/layout/`, etc. — one folder per concern.
- **Minimal component (frontend equivalent of backend rule 22)**: the `*.tsx` component contains **no business logic in the JSX**. No `if (statut === 'PAYEE' && montantTotal > 1000) { ... else if (...) { ... } }` inside a JSX return. Extract into: (a) a custom hook (`useFactureStatus`), (b) a utility function `lib/<domain>/helpers.ts`, or (c) a pure presentation component.
- **Component < 300 lines** as a soft guideline. Beyond that: signal to split into sub-components.

---

## Styles

- **Tailwind 4**: all tokens (colors, radius, etc.) come from `globals.css` (CSS variables) — no `theme.extend`.
- No classic CSS outside `globals.css`. Use only Tailwind classes or the `cn(...)` utility (clsx + tailwind-merge) for conditional composition.

---

## Forms

- **`react-hook-form` + `zod` resolver**. Zod schema in `src/lib/validations/`.
- Client-side validation + server-side revalidation by the backend (never trust client-only Zod).
- Form components in `src/components/forms/<Domain>Form.tsx`.

---

## Data fetching

- **Client-side**: React Query (`useQuery`, `useMutation`) — hooks in `src/hooks/use<Domain>.ts`.
- **Server-side (RSC)**: native `fetch` (wrapped by Next.js) with appropriate cache/revalidate.
- **HTTP**: a single shared axios instance in `src/lib/api/client.ts` that injects the bearer JWT.
- **No `useEffect` for data fetching client-side**: always go through TanStack Query. `useEffect` remains allowed for: DOM subscriptions, timers, non-data side effects.

---

## State management

- **Server state**: React Query (never Zustand for server data).
- **Global client state** (UI, currentTenant, sidebar, …): Zustand 5 — stores in `src/stores/`.
- **Local state**: `useState` / `useReducer` inside the component.
- **Decision hierarchy**: `useState` → React context if shared across 2-3 levels → Zustand if it crosses > 3 levels or multiple pages.

---

## Auth

- Token stored securely (TBD: `httpOnly cookie` via route handler vs `localStorage`).
- Automatic refresh token via axios interceptor on 401.
- Idempotent logout (revoke on the backend, clear on the frontend).
- Session guard on `(dashboard)/*` routes via a dedicated layout.

---

## API errors

- Global error handling via React Query (`onError`) + toast.
- Map backend i18n errors → user messages (keys `entity.notFound`, `validation.error`, …).
- **HTTP 406** (backend convention): treated as "Not Found" UX-wise (the backend returns 406 for `EntityException`, see `GlobalException`).

---

## Internationalization

- For now: UI in English (project is fully English now).
- Later: `next-intl` or equivalent — at least EN, FR later if needed.
- **No hardcoded UI text** once we wire `next-intl`. Convention: keys `<domain>.<context>.<key>` (e.g. `vente.facture.alreadyPaid`).

---

## Accessibility

- `@base-ui/react` components (under shadcn) → accessible by default (focus, ARIA).
- Explicit labels on every input, alt on every image, contrasts respected.

---

## Tests

**Mandatory**: every business function (`domain/`, `application/`, `infrastructure/`) and every business UI component (`presentation/`) **must** have at least one test. See rules 39 and 40.

- **Vitest + Testing Library** (configured, scripts `npm run test`, `test:watch`, `test:coverage`).
- **Coverage threshold**: 80% on statements / branches / functions / lines. The runner fails below.
- **No tests on copied shadcn components** (`src/common/presentation/ui/`) — excluded from coverage.
- **No tests on Next.js routing** (`src/app/**`) — excluded from coverage.
- E2E on critical flows (login, registration, sale, purchase) via Playwright (future).

---

## Cross-cutting rules (inherited from the backend, adapted)

Backend rules 22–37 of `BACKEND_CODING_CONVENTIONS.md` have their frontend equivalent. This section lists the adaptations.

### 22. Minimal components (backend equivalent: minimal controllers)

No business logic in a component's JSX. No business branches (`if (statut === 'PAYEE' && ...)`), no complex transformations (`facture.lignes.filter(...).reduce(...)`) inline in the `return`. Extract into:
- A custom hook (`useFactureStatus`, `useCaisseResume`),
- A utility function (`lib/vente/helpers.ts`),
- A pure presentation sub-component.

### 27. No non-reusable private function in a component (backend equivalent: no private methods in application services)

Any factorable logic must be **exported** in a hook or a `lib/` util. A function internal to a component used in a single place stays tolerated if it's a trivial helper (local formatting, etc.). Otherwise: extract.

### 28. Strategy for dispatch by subtype (direct equivalent)

No `if (type === 'A') ... else if (type === 'B') ...` cascade in JSX or a handler. Use:
```ts
const renderers: Record<MoyenPaiement, () => ReactNode> = {
  CASH: () => <CashIcon />,
  WAVE: () => <WaveIcon />,
  // ...
};
return renderers[paiement.moyen]?.() ?? <DefaultIcon />;
```

### 29. Concise JSDoc on complex hooks and components

- **TSDoc on component / hook**: 1 sentence announcing the responsibility (+ 1 if a particular guarantee).
- **No comments inside the body**. If needed: it's a signal that a better variable/function name is missing.
- **Language**: English for TSDocs, English for identifiers and logs (project consistency).

### 30. Max 3 "root" parameters / props

- **Functions / hooks**: ≤ 3 parameters. Beyond that: group into a typed object (`type <X>Params = { ... }`).
- **Components**: ≤ 3 "root" props. Beyond that: group into a `props` object (except for very common primitives like `className`, `children`, `disabled`).

### 31. Indentation + docs for multi-step functions/components

Every function or component that chains several steps (init, transformation, fetch, render):
- **Indented by blocks**: separate steps with blank lines.
- **Documented**: concise TSDoc (1-3 sentences).
- **No inline comments** — indentation + TSDoc structure the method.

### 32. Explicit names for variables, parameters AND aliases

- Ban single-letter names (`q`, `c`, `m`) and cryptic abbreviations (`dto`, `obj`, `data`).
- **Parameters**: full business name (`searchTerm` instead of `q`, `client` instead of `c`).
- **Local variables**: same rule.
- **Refs / state**: `const userRef = useRef(...)`, not `const r = useRef(...)`.
- **Trivial Stream lambdas**, loop index `i`, and `e` for `Event` or `Exception` in a catch remain tolerated.

### 33. `<X>Filter` type from 2 criteria

As soon as a hook or function takes ≥ 2 filter criteria, create a dedicated type in `src/types/`:
```ts
type CommandeVenteFilter = {
  magasinId: string;
  clientId?: string;
  vendeurId?: string;
  startDate?: string;
  endDate?: string;
  page: number;
  size: number;
};
```
The hook: `useCommandesVente(filter: CommandeVenteFilter)`. The component builds the filter locally.

### 34. Functional methods by default (`.map / .filter / .reduce`)

`for` is allowed only if performance requires it (measured hot path). Otherwise: `array.map`, `.filter`, `.reduce`, `.forEach`, `.find`. Comment a `for` loop if necessary to explain why.

### 35. Extract repeated values into a local const

```ts
// Bad:
return <div>
  <h1>{commande.facture.numero}</h1>
  <span>Amount: {commande.facture.montantTotal}</span>
  <span>Remaining: {commande.facture.montantTotal - commande.facture.montantPaye}</span>
</div>

// Good:
const facture = commande.facture;
const montantRestant = facture.montantTotal - facture.montantPaye;
return <div>
  <h1>{facture.numero}</h1>
  <span>Amount: {facture.montantTotal}</span>
  <span>Remaining: {montantRestant}</span>
</div>
```

### 36. Complex render item → sub-component

Any `.map(item => ...)` body that exceeds 3-5 lines or contains several conditional branches must be extracted into a sub-component:
```tsx
// Good:
{commandes.map(commande => <CommandeRow key={commande.id} commande={commande} />)}
```
The sub-component `CommandeRow` lives in the same file (if private) or in `src/components/<domain>/` (if reused).

### 37. Blank lines around hooks / effects / transformation blocks

```tsx
// Good:
const { data: commandes, isLoading } = useCommandesVente(filter);

const totalMontant = useMemo(
  () => commandes?.content.reduce((acc, c) => acc + c.montantTotal, 0) ?? 0,
  [commandes]
);

useEffect(() => {
  setSidebarTitle('Sales');
}, [setSidebarTitle]);

return ( ... );
```
A blank line before and after each `useQuery`, `useMutation`, `useMemo`, `useCallback`, `useEffect`, or multi-line transformation block.

### 39. Every business function and UI component must be tested

**Mandatory rule**. No function in `domain/`, `application/`, `infrastructure/` and no component in `presentation/` (except `ui/` shadcn) can be delivered without at least one associated test. The Vitest runner enforces a global threshold of **80%** (statements / branches / functions / lines) — any drop below blocks the commit (via `npm run test:coverage`).

- **Business functions**: happy path test + at least one error or fallback case.
- **Business UI components**: minimal render test + critical interaction tests (clicks, submit, open/close).
- **Justified exclusions** (already configured in `vitest.config.ts`): `src/app/**` (Next.js routing), `src/common/presentation/ui/**` (copied shadcn), `src/common/domain/types.ts` (types-only without executable logic), `*.d.ts` files.
- **Defense branches impossible to test in jsdom** (e.g. SSR early return in a browser-only file): prefer **removing the branch** and documenting the contract ("browser-only") rather than using `/* v8 ignore next */`. The ignore is tolerated occasionally but must be justified in a comment.

### 40. Tests in `src/test/`, mirroring the source tree

Tests live in **`src/test/`** (aligned with the backend Maven convention `src/main` ↔ `src/test`), with a **strict mirror** structure of `src/`:

| Source | Test |
|---|---|
| `src/common/infrastructure/api-client.ts` | `src/test/common/infrastructure/api-client.test.ts` |
| `src/common/presentation/shared/DataTable.tsx` | `src/test/common/presentation/shared/DataTable.test.tsx` |
| `src/features/security/domain/rules.ts` | `src/test/features/security/domain/rules.test.ts` |

**No co-location** (`Foo.tsx` + `Foo.test.tsx` side by side is **forbidden**). Configuration: `vitest.config.ts` includes `src/test/**/*.test.{ts,tsx}` and excludes the above-mentioned paths from coverage. Global setup: `src/test/setup.ts`.

### 38. Blank line between methods/properties of the same object or class

Always separate methods (and multi-line properties) of the same object or class with a blank line. Improves readability and visually delimits each responsibility.

```ts
// Bad:
export const authToken = {
  getAccess() {
    return localStorage.getItem('token')
  },
  setAccess(token) {
    localStorage.setItem('token', token)
  },
  clear() {
    localStorage.removeItem('token')
  },
}

// Good:
export const authToken = {
  getAccess() {
    return localStorage.getItem('token')
  },

  setAccess(token) {
    localStorage.setItem('token', token)
  },

  clear() {
    localStorage.removeItem('token')
  },
}
```

Applies to **object literals**, **classes**, **interfaces**, and **types** as soon as they contain at least 2 methods or multi-line properties. Single-line properties (constants, simple types) stay grouped without a blank line between them.

---

## Logs / debug

- **No `console.log` in production**. Temporarily acceptable in dev, remove before committing.
- Caught errors: `console.error` allowed in addition to the user toast (devtools debug).

---

## Commit conventions

Project style: **Conventional Commits in English** (`feat(scope): description`, `fix(scope): ...`, `chore: ...`, `docs: ...`, `refactor: ...`, `test: ...`). Aligned with the backend convention.

```
feat(<scope>): <short summary>

[optional body: what it does, why, technical notes]
```

**Scope**: domain name (`auth`, `vente`, `produit`, `layout`, `forms`, …) in parentheses.

**No `Co-Authored-By: Claude` footer** in commits. No automatic push without explicit request.

**When to commit**: at the end of each validated task (see `TODO.md`).
