# CONVENTION_CODAGE_FRONTEND.md — Règles de codage du frontend

> Conventions et règles obligatoires applicables sur chaque ajout/modification de code **frontend** (`store-frontend/`).
> Pour la stack et la structure du frontend, voir `ARCHITECTURE.md`. Pour le backend, voir `CONVENTION_CODAGE_BACKEND.md`.

---

## Conventions de code

**Composants / fichiers TSX** : `PascalCase.tsx` (sauf routes Next.js : `page.tsx`, `layout.tsx`, etc.)
**Hooks** : `useXxx.ts`
**Utils / lib** : `kebab-case.ts` ou `camelCase.ts`
**Types** : `PascalCase`
**Variables / fonctions** : `camelCase`
**Imports internes** : utiliser l'alias `@/...` (ex. `@/components/ui/button`, `@/lib/utils`)

---

## Règles spécifiques

### Architecture & composants

- **Server Components par défaut** ; ajouter `"use client"` uniquement quand c'est nécessaire (hooks React, état, événements DOM).
- **shadcn** : générer les composants via la CLI shadcn dans `src/components/ui/`. Ne pas les modifier directement, étendre via composition.
- **Composants métier** : `src/components/forms/`, `src/components/layout/`, etc. — un dossier par préoccupation.

### Styles

- **Tailwind 4** : tous les tokens (couleurs, radius, etc.) viennent de `globals.css` (CSS variables) — pas de `theme.extend`.
- Pas de CSS classique en dehors de `globals.css`. Utiliser uniquement les classes Tailwind ou les utilitaires `cn(...)` (clsx + tailwind-merge) pour la composition conditionnelle.

### Forms

- **`react-hook-form` + résolveur `zod`**. Schéma Zod dans `src/lib/validations/`.
- Validation côté client + revalidation côté serveur via le backend (jamais faire confiance au seul Zod côté client).
- Composants de formulaire dans `src/components/forms/<Domaine>Form.tsx`.

### Data fetching

- **Côté client** : React Query (`useQuery`, `useMutation`) — hooks dans `src/hooks/use<Domaine>.ts`.
- **Côté serveur (RSC)** : `fetch` natif (Next.js wrappé) avec cache/revalidate appropriés.
- **HTTP** : une seule instance axios partagée dans `src/lib/api/client.ts` qui injecte le bearer JWT.

### State management

- **Server state** : React Query (jamais Zustand pour des données serveur).
- **Client state global** (UI, currentTenant, sidebar, …) : Zustand 5 — stores dans `src/stores/`.
- **Local state** : `useState` / `useReducer` à l'intérieur du composant.

### Auth

- Token stocké de façon sécurisée (à arbitrer : `httpOnly cookie` via route handler vs `localStorage`).
- Refresh token automatique via intercepteur axios sur 401.
- Logout idempotent (révoque côté backend, clear côté frontend).
- Garde de session sur les routes `(dashboard)/*` via layout dédié.

### Types DTO

- Types partagés avec le backend dans `src/types/` (mappés sur les `<X>Request` / `<X>Response` du backend).
- À terme : générer automatiquement depuis l'OpenAPI du backend (`openapi-typescript`).

### Erreurs API

- Gestion globale des erreurs via React Query (`onError`) + toast.
- Mapping des erreurs i18n du backend → messages utilisateur (clés `entity.notFound`, `validation.error`, …).

### Internationalisation

- Pour l'instant : interface en français.
- Plus tard : `next-intl` ou équivalent — au minimum FR, EN ensuite.

### Accessibilité

- Composants `@base-ui/react` (sous shadcn) → accessibles par défaut (focus, ARIA).
- Labels explicites sur tous les inputs, alt sur toutes les images, contrastes respectés.

---

## Tests

- **Vitest + Testing Library** (à mettre en place — nice-to-have actuellement).
- Tests par composant métier (forms, layouts complexes), pas par composant UI shadcn.
- E2E sur les flux critiques (login, inscription, vente, achat) via Playwright (futur).

---

## Conventions de commits

Style projet : titre direct (pas de préfixe `feat:`/`fix:` etc), description du « pourquoi » dans le corps.

```
<résumé court fonctionnel>

[corps optionnel : ce que ça fait, pourquoi, notes techniques]
```

**Quand committer** : à chaque fin de tâche validée (cf. `TODO.md`).

**Jamais** `Co-Authored-By: Claude` dans les commits. Pas de push automatique sans demande explicite.
