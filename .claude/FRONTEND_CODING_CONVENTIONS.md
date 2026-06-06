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

**Mandatory rule.** Every variable, parameter, function, callback arg,
and destructured field must carry a name that describes **what it is**,
not just its type or "the result of the previous line". Reviewers should
not need surrounding context to guess the role.

- **Banned**: single-letter names (`q`, `c`, `m`, `u`), generic
  abbreviations (`dto`, `obj`, `data`, `res`, `tmp`, `val`, `cfg`,
  `evt`), and "result of an operation" placeholders (`ok`, `result`,
  `output`, `value` on its own). Use the business meaning:
  `isStepValid`, `apiResponse`, `nextLocale`, `submittedValues`.
- **Setter callback args** (`setState((prev) => …)`) — name them after
  the value, not generic `prev` / `current`: `setStepIndex((previousIndex) => previousIndex + 1)`.
- **Parameters**: full business name (`searchTerm` instead of `q`,
  `client` instead of `c`, `apiError` instead of `e`).
- **Local variables**: same rule. `const ok = await trigger(...)` →
  `const isStepValid = await trigger(...)`.
- **Refs / state**: `const userRef = useRef(...)`, not `const r = useRef(...)`.
- **Tolerated exceptions** (limited, library-idiom rationale):
  - trivial inline stream lambdas (`items.map(item => item.id)`)
  - loop index `i`
  - `error` for the caught error in a `catch (error)` block
  - `t` for the next-intl translator returned by `useTranslations(...)`
    / `getTranslations(...)` — canonical name across the i18n
    ecosystem, kept for grep-affinity with library docs
  - `state` as the **selector argument** of Zustand stores
    (`useStore((state) => state.x)`) — canonical Zustand idiom. The
    *binding* on the consumer side still gets a business name
    (`const currentUser = useAuthStore((state) => state.user)`).

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
- **Justified exclusions** (already configured in `vitest.config.ts`): `src/app/**` (Next.js routing), `src/common/presentation/ui/**` (copied shadcn), `src/**/dtos/**` (DTO files are type-only with no executable logic — see rule 41), `*.d.ts` files.
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

### 41. DTOs in a `dtos/` package, one file per DTO

Every DTO (`<X>Request`, `<X>Response`, `<X>Summary`, `<X>Filter`, plus the principal types of the domain like `UserPrincipal`, `PageResponse`, …) lives under a `dtos/` folder, **one type per file**, in the relevant module's `domain/` layer.

| Source | Path |
|---|---|
| `LoginRequest` | `src/features/security/domain/dtos/login-request.ts` |
| `AuthResponse` | `src/features/security/domain/dtos/auth-response.ts` |
| `PublicPlan` | `src/features/abonnement/domain/dtos/public-plan.ts` |
| `PageResponse<T>` | `src/common/domain/dtos/page-response.ts` |

- **Aligned with the backend** convention `org.store.<module>.application.dto.<X>Request` — one file per record/DTO, one class per file.
- **File naming**: kebab-case mirror of the type name (`UserPrincipal` → `user-principal.ts`, `RegisterPropertyRequest` → `register-property-request.ts`).
- **Composite DTOs** (e.g. `RegisterPropertyRequest` aggregates `AccountRequest` + `UtilisateurRequest` + `EntrepriseRequest` + `MagasinRequest`) import the sub-DTOs via `import type { ... } from './<sub-dto>'`.
- **No `index.ts` barrel**: each consumer imports the explicit DTO file it needs. Keeps refactor diffs minimal and IDE jumps direct.
- **`domain/types.ts` is forbidden** as a catch-all for several DTOs. Concise helper enums or branded types that are NOT proper DTOs (e.g. local utility unions) may still live in `domain/` but outside `dtos/`.
- **No runtime code** inside a DTO file — `type` (or `interface`) only. Zod schemas associated with a DTO live next to the form that uses them (`presentation/<X>Form.tsx`) or in `domain/schemas/` if shared.

### 42. No inline arrow handlers in JSX — extract into named functions

Any `(args) => { ... }` (especially multi-line, but also one-liners with body logic) passed as a JSX prop must be declared as a **named function above the `return`** of the component, then referenced by name.

❌ Bad:
```tsx
<ResetPasswordDialog
  open={resetOpen}
  onOpenChange={(open) => {
    setResetOpen(open)
    if (!open) setResetTarget(undefined)
  }}
  target={resetTarget}
/>
```

✅ Good:
```tsx
function handleResetOpenChange(open: boolean) {
  setResetOpen(open)
  if (!open) setResetTarget(undefined)
}

return (
  <ResetPasswordDialog
    open={resetOpen}
    onOpenChange={handleResetOpenChange}
    target={resetTarget}
  />
)
```

**Why** — inline lambdas in JSX:
- get re-created on every render (memoization useless),
- hurt readability: at the call-site you only see `(args) => { ... }`, no name documents intent,
- block `useCallback`-able patterns and reduce-as-a-block diffs,
- obscure what the handler does when the prop is `onSomething` — the function name is the documentation.

**Tolerated narrow exceptions** (don't over-extract):
- Trivial one-line setter calls inside `.map(...)` callbacks where you need to bind a row to the handler:
  `<DropdownMenuItem onClick={() => onSelect(row)}>…</DropdownMenuItem>`.
  Even here, a curried handler factory or a row-aware sub-handler outside JSX is preferred when possible.
- `<Controller render={({ field }) => <Component ... />}>` — RHF's API forces the lambda. Keep the lambda short and pass the field's handlers through cleanly.

Applies to all React/Next/TSX code in this repo. Reinforces rule 32 (explicit names).

### 43. Never render technical IDs in user-facing UI

Never display UUIDs, DB ints, or any raw primary key in user-facing UI — table cells, badges, breadcrumbs, dialog descriptions, toast messages. Always resolve the ID to a human label first (`nom`, `libelle`, `username`, …).

❌ Bad:
```tsx
<TableCell>{employe.magasinId}</TableCell>
toast.error(`Magasin ${magasinId} introuvable`)
```

✅ Good:
```tsx
<TableCell>{magasinNames.get(employe.magasinId) ?? '—'}</TableCell>
toast.error(t('magasin.notFound')) // i18n message must not interpolate the id
```

**Why:** UUIDs are noise to a merchant. They erode trust ("system is showing me database internals") and make support harder (no one reads `1e35bbba-…` aloud).

**Concrete patterns already in repo:**
- `EmployeTable` resolves `magasinId → nom` via a `Map<string, string>` built from `useMagasinList`. The id stays internal.
- `MagasinTable` shows `magasin.nom`, never `magasin.id`.

**Acceptable uses** (id doesn't reach text content):
- `key={item.id}` on a React list.
- `value={item.id}` on a `<SelectItem>`.
- `href="/dashboard/magasins/${id}"` — URL bar is not user-facing text content.
- Logs / dev console.

**If you can't resolve to a name** (no query handy), prefer a generic phrasing ("le magasin sélectionné", "cet employé") over leaking the id.

The mirror rule lives in `BACKEND_CODING_CONVENTIONS.md` — backend i18n messages that take a `{0}` must not be called with an id; pass the entity name, or drop the placeholder and rephrase.

---

### 44. Always controlled `<Select>` — never the `value || undefined` pattern

The shared `Select` (`common/presentation/ui/select.tsx`) wraps base-ui's `SelectPrimitive.Root` and **coerces `undefined` to `''`** when no `defaultValue` is provided. Forms get controlled mode for free.

**Do**: pass the raw form-state value, even if empty:

```tsx
<Select value={field.value} onValueChange={(value) => field.onChange(value ?? '')}>
```

**Don't**: short-circuit to `undefined` — the wrapper protects you, but the pattern is misleading and harder to read:

```tsx
// ❌ misleading, even though the wrapper still keeps it controlled
<Select value={field.value || undefined} ...>
```

**Reason**: the `state || undefined` pattern flipped between uncontrolled (first render, empty state) and controlled (after first selection), triggering the base-ui warning *"A component is changing the uncontrolled value state of Select to be controlled"*. The wrapper neutralizes the bug globally, but the explicit form-state value reads better and is the only style we use in this codebase.

If you really need an uncontrolled `<Select>`, pass `defaultValue` explicitly — the wrapper detects it and skips the coercion.

---

### 45. Code smells are mandatory to fix — always extract into a shared external function

**Any duplicated logic** — flagged by the IDE, spotted in review, or noticed while writing — **must be extracted into a shared external function. No exception.** No "I'll dedupe later", no "it only repeats twice so it's fine".

**What counts as duplication:**
- Same statement sequence in two functions (even across files / pages).
- Identical `onSuccess` / `onError` boilerplate across TanStack mutation handlers — extract via `runMutationWithToast` (`common/application/mutation-toast.ts`) or a similar wrapper.
- Repeated mapping / transformation / formatting helpers in two components — promote to `common/application/<name>.ts`.
- Copy-pasted guard / validation / permission-check blocks — promote to a hook (`use<Name>`) or a pure function (`common/application/<name>.ts`).
- Repeated JSX shape spanning more than a handful of lines — extract a sub-component (cf. rule 36).

**Where the extracted function lives:**
- Single feature → `features/<feature>/application/<name>.ts` (hook) or `features/<feature>/domain/<name>.ts` (pure).
- Cross-feature, side-effecting → `common/application/<name>.ts`.
- Cross-feature, pure / converters → `common/domain/<name>.ts` or `common/infrastructure/<name>.ts`.

**Naming:** explicit business name (rule 32). Never `helper`, `util`, `doStuff`. The function name describes *what the duplication did*, not where it came from. (Example: `runMutationWithToast`, `toastApiError`, `navGuard.canSee`.)

**Tests:** the new function gets its own unit test (rule 39 — every business function is tested). Call sites stop testing the extracted logic; they only assert the wiring.

**Reason:** duplication is the #1 source of bugs that only surface after a partial fix lands on one copy. Extraction makes the next change land in one place.

**Mirror rule backend:** `BACKEND_CODING_CONVENTIONS.md` rule 39 — same meta-rule, applied to Java services.

---

### 46. One JSX component per file — no nested component definitions

**Every component that returns JSX lives in its own file.** No exception. The file name matches the component name in PascalCase (`PaiementAbonnementTable.tsx`, `RejectPaiementDialog.tsx`, …).

**Forbidden:**
- Defining a second component (or a sub-component) inside the same file as the main export — even when "it's only used here".
- Inline `function SubRow(...) { return <tr>...</tr> }` declared above or below the main `function Page()` in the same file.
- Anonymous inline component expressions assigned to a variable (`const Row = (props) => <tr>...</tr>`) used as a JSX element.

**What counts as a component:** any function (or arrow function) that returns JSX and is used as `<Name />`. Pure render helpers that return primitive data (a string, a number) are not components and stay inline.

**Where to put the extracted file:**
- Used by exactly one parent → same folder as the parent, sibling file. Example: `PaiementAbonnementTable.tsx` next to `PaiementAbonnementPage.tsx`.
- Used by two or more parents in the same feature → `features/<feature>/presentation/<Name>.tsx`.
- Cross-feature → `common/presentation/shared/<Name>.tsx`.

**Naming:** business name first, role suffix when useful (`...Table`, `...Form`, `...Dialog`, `...Cell`, `...Filters`). Never `Wrapper`, `Inner`, `Sub`, `Helper`.

**Tests:** the extracted component gets its own test file (rule 39 + rule 40), placed under `src/test/` mirroring the source tree.

**Reason:** one component per file keeps imports explicit, makes the dependency graph readable, lets each component be tested in isolation, and prevents the "300-line file with 4 hidden components" anti-pattern that no one can refactor without breaking three call sites at once.

**Mirror rule backend:** `BACKEND_CODING_CONVENTIONS.md` rule 18 (one public class per file) — same intent, applied to React components.

---

### 47. Tables never auto-fetch on mount — explicit Search trigger required

**On page mount, every list / table screen displays an empty Search-prompt state. No data is fetched until the user explicitly triggers a search.** No exception — including small admin tables.

**Forbidden:**
- Calling `useXPage(filter)` directly with the initial filter (auto-fires on mount).
- Debounced "search-as-you-type" that fires HTTP per keystroke as the only trigger.
- Loading the full list "just in case" the user wants to browse.

**Required pattern:**
- Use the shared hook `useDeferredSearch<F>(initialDraft)` from `common/application/use-deferred-search.ts`. It exposes `draft`, `setDraft`, `active`, `isReady`, `search`, `reset`.
- The filters bar binds inputs to `draft`. A dedicated **"Rechercher" Button** at the end of the bar calls `search()` (= snapshot draft → active).
- The page hook receives the active filter + `enabled: isReady`. The query stays idle until the first `search()`.
- Render `<SearchPromptState />` (`common/presentation/shared/SearchPromptState.tsx`) when `!isReady` — Search icon + "Filtrez puis cliquez sur Rechercher" + primary CTA wired to `search()`.
- **Pagination clicks** internally call `setActiveFilter({ ...active, page: N })` — they don't go through `search()` (the user has already opted in to a search).
- **After a create / update / delete / activate mutation:** call `search()` in `onSuccess` if `!isReady`, otherwise the invalidate handles it.
- **Reset filter:** `reset()` clears draft + sets `active = null` (back to empty state).

**i18n keys:** every screen reuses the shared `common.searchPrompt.{title, description, cta}` namespace — no per-feature duplication.

**Reason:** prevents expensive default queries on admin-heavy datasets (entreprises, paiements, audit logs), lets the user formulate the filter before fetching, removes the stale-rows flash on slow connections, and makes RBAC failures (403 on a hidden list) invisible to users who weren't going to search anyway.

**Mirror rule backend:** none — pure frontend UX rule.

---

### 48. CRUD list filters always expose a created-date range

Every CRUD list page's filter UI must include the shared `<DateRangeFilter />` component bound to `createdStartDate` + `createdEndDate` (ISO `yyyy-MM-dd` strings). The backend honors these as a filter on `entity.createdAt` and orders the result by `createdAt DESC`.

**Forbidden:**
- Building a list page filter UI without a date range when the corresponding backend filter DTO carries `createdStartDate` / `createdEndDate`.
- Hand-rolling two `<Input type="date" />` per page — use the shared component (rule 45).

**Required pattern:**
- The page-level filter DTO carries `createdStartDate?: string` + `createdEndDate?: string`.
- The `initialFilter` const sets both to `undefined`.
- The `<X>Filters />` component receives `createdStartDate` + `createdEndDate` + matching `onChange` callbacks, and renders `<DateRangeFilter />` inside the same filter bar.
- The API adapter forwards both values as query params, skipping blank / undefined entries.
- i18n labels live in the shared `common.dateRange.{from, to}` namespace.

**Mirror rule backend:** `BACKEND_CODING_CONVENTIONS.md` rule 40 — defines the backend contract these UI fields target.

### 49. Business-semantic filters live in the backend, never in `.filter()`

Whenever a list/grid needs filtering based on business semantics (exclude trial plan from the OWNER subscribe page, hide soft-deleted rows from non-admins, restrict by tenant, drop archived records, …) the filter MUST be implemented on the backend via JPQL — never as a client-side `array.filter(...)` over a generic fetch.

**Forbidden:**
```ts
// SubscribePage.tsx — exposes the trial plan over the wire, then hides it client-side
const catalog = await catalogApi.findPublicCatalog()
const paidPlans = catalog.plans.filter((plan) => plan.prix > 0)
```

**Required:**
```ts
// SubscribePage.tsx — backend already filtered via JPQL
const catalog = await catalogApi.findSubscribableCatalog()
const paidPlans = catalog.plans
```

**How to apply:**
- Add a dedicated repository method in the backend (e.g. `findSubscribableResponses()`) with its own JPQL.
- Expose a dedicated endpoint when the filter is permission-bound (`/catalog/subscribable` gated `SUBSCRIPTION_CREATE` is distinct from `/catalog/public` permitAll).
- Frontend: extend the repository port + adapter (`catalog-api.ts`) with the new method, add a matching `use<X>` hook, and call it from the page. No `.filter(...)` over the generic catalog.
- Cosmetic / UI-only filters (sort direction toggles, "show only my drafts" personal preferences not tied to permissions) stay on the client — the rule targets business invariants, not user comfort filters.

**Mirror rule backend:** `BACKEND_CODING_CONVENTIONS.md` rule 41 — defines the JPQL contract these UI calls target.

### 50. Primary CTA goes on the right of the row

In any horizontal action row — filter bars, dialog footers, form button strips — the **primary** action (Submit, Search, Save, Create, Confirm…) sits at the **right** edge, and any secondary action (Reset, Cancel, Discard, Back…) sits to its **left**.

**Required pattern (filter bar):**
```tsx
<div className="flex flex-col gap-2 sm:flex-row sm:items-center">
  <Select>…</Select>
  <Button type="button" variant="ghost" onClick={onReset} className="sm:ml-auto">
    <X aria-hidden="true" className="size-4" />
    {t('reset')}
  </Button>
  <Button type="button" onClick={onSearch}>
    <Search aria-hidden="true" className="size-4" />
    {tCommon('cta')}
  </Button>
</div>
```

The `sm:ml-auto` on the **first** action button pushes the entire trailing group to the right (`flex` collapses the gap before it). Both buttons stay together; any preceding controls (selects, inputs, date pickers) keep their natural left alignment.

**Why:** Western reading order = top-left to bottom-right, so the eye lands last on the primary action. Putting Search before Reset on the left makes the user scan back-and-forth and ambiguates which action commits the form. This convention also matches every shadcn/Material/Apple-HIG dialog footer.

**Forbidden:** `[Search][Reset]` on the left, `[Confirm][Cancel]` ordering, primary buttons floating mid-row.

**Scope:** every `*Filters.tsx`, every dialog footer (`<DialogFooter>` already right-aligns), every form button strip. Filter row sweep landed in the same commit that introduced this rule.

---

### 51. Required form fields are marked with a trailing `*`

Every input whose form schema requires a non-empty value MUST carry a red asterisk after its label. Optional fields stay unmarked — readers learn the convention once and can scan a form in seconds.

**Implementation:** the shared `<Label>` primitive (`src/common/presentation/ui/label.tsx`) accepts a `required` boolean prop that renders the trailing `<span aria-hidden="true">*</span>` for you. Always pair it with `aria-required` (or the native `required` attribute) on the input element so screen readers still announce the field as mandatory — the `*` itself is decorative.

```tsx
<Label required>{t('fields.fournisseur')}</Label>
<Select required aria-required …>…</Select>

<Label>{t('fields.numeroLot')}</Label>     // optional → no *
<Input type="text" …>
```

**Forbidden:** `(facultatif)` / `(optional)` suffixes baked into the label text, parenthetical reminders, or asterisks added manually as part of the i18n string — the marker is structural, not editorial. Drop existing `(facultatif)` strings as you sweep each form.

**Scope:** every dialog form and every page-level form. Applied form-by-form as the area is touched, same rollout pattern as rule 50.

### 52. One hook per file — no co-located hooks

**Every `useXxx` hook lives in its own dedicated file.** No exception.

**Forbidden:**
- Defining two or more hooks in the same `.ts` / `.tsx` file.
- Inlining a `useQuery` / `useMutation` body directly inside a component or page file.
- "Helper hook" files that bundle several `useXxx` exports together.

**Required pattern:**
- One file → one exported hook. File name: `use<Domain><Action>.ts` (camelCase, no PascalCase).
- Feature-scoped hooks live under `features/<feature>/application/use<X>.ts`.
- Cross-feature hooks live under `common/application/use<X>.ts`.
- The hook file exports exactly one function — the hook itself. No side-exported types (put those in `domain/dtos/`), no utility functions (promote to `common/tools/`).

**Examples:**
```
features/vente/application/useCaisseResumeToday.ts   ← one hook
features/achat/application/usePendingPurchasesCount.ts  ← one hook
features/vente/application/useUnpaidInvoicesCount.ts    ← one hook
```

**Why:** co-located hooks create invisible coupling — renaming one file silently removes the other. One-hook-per-file keeps the import graph flat, lets each hook be tested in isolation (rule 39), and mirrors the one-component-per-file constraint (rule 46).

**Mirror rule:** rule 46 (one JSX component per file) — same intent applied to hooks.

---

### 53. Types and constants in their own files — no inline definitions in component files

**Every TypeScript type / interface used as component props, and every constant array / object used by multiple files, lives in its own dedicated `.ts` file.**

**Forbidden:**
- Defining `type XxxProps = { ... }` inside the same `.tsx` file as the component.
- Defining a constant array (e.g. `PERIODS`, `COLUMNS`) inside a component or page file.
- Defining an `enum` inside a component file.

**Required pattern:**
```
// ✓ — types file
// kpi-card-props.ts
export type KpiCardVariant = 'default' | 'success' | 'warning' | 'info'
export type KpiCardProps = { label: string; value: string | number; icon: ReactNode; variant?: KpiCardVariant }

// ✓ — component file imports the type
// KpiCard.tsx
import type { KpiCardProps } from './kpi-card-props'
export function KpiCard({ label, value, icon, variant = 'default' }: KpiCardProps) { ... }

// ✗ — forbidden: type defined inside component file
// KpiCard.tsx
type KpiCardProps = { ... }   ← extract to kpi-card-props.ts
export function KpiCard(...) { ... }
```

**Naming convention:**
- Prop type file: `kebab-case-props.ts` (e.g. `kpi-card-props.ts`, `period-selector-props.ts`)
- Shared constant file: `kebab-case-constants.ts` or collocated with its type file
- Enum file: `kebab-case.ts` (e.g. `audit-action.ts`)

**Scope:**
- Component-specific prop types → same directory as the component
- Domain types (DTOs, API responses) → `features/<feature>/domain/dtos/<name>.ts`
- Shared utility types → `common/domain/dtos/<name>.ts`

**Why:** inline types bind the type definition to the implementation file. Extracting them makes types importable independently, prevents circular imports, and enables testing type contracts in isolation.

**Mirror rules:** rule 41 (DTOs in `dtos/`, one per file), rule 46 (one component per file), rule 52 (one hook per file).

### 54. All form/dialog selectors must use `<Combobox>` — never `<Select>`

**Any dropdown that lets the user pick one item from a list inside a form or dialog MUST use the shared `<Combobox>` component** (`common/presentation/ui/combobox.tsx`). The plain `<Select>` is allowed only in **filter bars** (where the list is short and fixed, e.g. a 3-value status filter) and in the system-level page-size selector in `<Pagination>`.

**Why:** `<Select>` lists do not have a search input — any list longer than ~10 items forces the user to scroll through everything. `<Combobox>` adds a search input automatically and is API-compatible for single-value picks.

**Forbidden in forms:**
```tsx
// ❌ 40+ countries, 10+ categories, 5+ roles — all unscrollable without search
<Select value={field.value} onValueChange={field.onChange}>
  <SelectTrigger>…</SelectTrigger>
  <SelectContent>
    {items.map(item => <SelectItem key={item.id} value={item.id}>{item.label}</SelectItem>)}
  </SelectContent>
</Select>
```

**Required in forms:**
```tsx
// ✓ — same list, now searchable
<Combobox
  items={items.map(item => ({ value: item.id, label: item.label }))}
  value={field.value}
  onValueChange={(v) => field.onChange(v ?? '')}
  placeholder={t('fields.placeholder')}
  ariaLabel={t('fields.label')}
  emptyLabel={t('fields.empty')}
/>
```

**Special case — custom trigger display:** when the trigger must show compact text that differs from the full label (e.g. the `PhoneField` country selector shows `🇸🇳 +221` while the dropdown shows `🇸🇳 Sénégal +221`), build a `CountryCombobox`-style wrapper using Base UI's `Combobox.*` primitives directly — same pattern as `common/presentation/ui/combobox.tsx` but with a custom trigger render.

**Scope:** every `<form>`, every `<Dialog>` that contains a form, every `<Sheet>` form. Filter sidebars / filter rows are exempt when the list ≤ 5 fixed values (status selectors, boolean filters).

**Applied:** ClientForm magasinId, EmployeForm role + magasinId, DepenseForm categoryId + modePaiement, SubscriptionTypeForm reductionType, PromotionForm + CouponForm planId + reductionType, CancelAchatDialog motif, CancelVenteDialog motif, ProductForm categoryProductId.

---

### 55. Authenticated images must be fetched via `apiClient` as blob URLs

Images served by authenticated API endpoints (e.g. `GET /api/v1/products/{id}/image`) cannot be loaded via a plain `<img src="...">` because the browser does not attach the `Authorization: Bearer` header to image requests.

**Pattern:** fetch via `apiClient.get<ArrayBuffer>(url, { responseType: 'arraybuffer' })` inside a `useEffect` or `useQuery`, build a `URL.createObjectURL(blob)`, pass the result to `<img src={blobUrl}>`.

```ts
// useBlobUrl.ts (or inline in a component)
useEffect(() => {
  if (!url) return
  let cancelled = false
  apiClient.get<ArrayBuffer>(url, { responseType: 'arraybuffer' }).then(response => {
    if (cancelled) return
    const ct = (response.headers['content-type'] as string) ?? 'image/jpeg'
    setBlobUrl(URL.createObjectURL(new Blob([response.data], { type: ct })))
  }).catch(() => {})
  return () => { cancelled = true }
}, [url])
```

React Query variant (preferred for shared images): `useQuery({ queryKey: [...], queryFn: async () => { ... URL.createObjectURL(...) }, enabled: hasImage, staleTime: 5 * 60_000 })`.

**Clean up**: the blob URL is managed by the browser GC on tab close — short-lived blobs (carousel/lightbox slides) are acceptable inline. Long-lived cached blobs (profile photo, product main image) should use `gcTime` in React Query to let TanStack clean up the cache entry.

**Scope:** profile photo (`useProfilePhotoBlob`), magasin logo (`useMagasinLogoBlob`), product main image (`useProductMainImage`), gallery thumbnails (`useGalleryBlobUrl` in `ProductImageSection`), lightbox slides (`useBlobUrl` in `ImageLightbox`). Any future authenticated image endpoint follows the same pattern.

---

### 56. Currency belongs in the column header, not in every cell value

In any table (DataTable or raw `<table>`) that contains monetary amounts, **the currency symbol or code must appear once in the column header — never repeated on every cell value**.

**Forbidden:**
```tsx
// ❌ "1 500 000 F CFA" repeated on every row
header: t('montantTotal'),
cell: ({ row }) => format.number(row.original.montantTotal, { style: 'currency', currency })
```

**Required:**
```tsx
// ✓ header carries the label; cells show plain numbers
header: `${t('montantTotal')} (${currencyLabel})`,
cell: ({ row }) => <span className="tabular-nums">{format.number(row.original.montantTotal)}</span>
```

**How to build `currencyLabel`:** use `Intl.NumberFormat.formatToParts` with the app locale to extract the currency symbol. This resolves `XOF → F CFA`, `EUR → €`, etc. Add it as a `useMemo` in the component:

```ts
const locale = useLocale()
const currency = useCurrency()
const currencyLabel = useMemo(() => {
  try {
    const parts = new Intl.NumberFormat(locale, { style: 'currency', currency, currencyDisplay: 'symbol' }).formatToParts(0)
    return parts.find((p) => p.type === 'currency')?.value ?? currency
  } catch { return currency }
}, [locale, currency])
```

Add `currencyLabel` to the `useMemo` deps array for the columns definition.

**Scope:** every `<th>` / `header:` that corresponds to a monetary column. Non-table inline uses (summary sentences, KPI cards, dialog totals) may keep `{ style: 'currency', currency }` formatting — this rule applies to **table columns only**.

**Why:** repeating "F CFA" on every row is visual noise and wastes horizontal space. The header communicates the unit once; rows stay clean and scannable.

**Applied:** CommandeAchatTable (totalHt), CommandeVenteTable (montantTotal, montantPaye), AchatDetailsContent (prixAchat, prixVente, montant), VenteDetailsContent (prixUnitaire, montantTotal), StockTable (prixMoyen, valeur), AchatLineTable (prixAchat, prixVente, subtotal), DepenseTable (montant), PaiementAbonnementTable (montant), ExpiringLotsTable (prixAchat), PlanAdminTable (prix), OwnerPaiementTable (montant), InventaireDetailsContent (prixUnitaire, total).

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
