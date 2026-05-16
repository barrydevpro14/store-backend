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

## TypeScript strict

- **Pas d'`any`**. Si on ne sait pas le type : `unknown` + narrowing, ou `Type<T>` générique. Justifier en commentaire toute exception inévitable (`// @ts-expect-error: ...`).
- **Pas de `as` non motivé** (cast type). Préférer le typage explicite. Si `as` nécessaire (ex. après `JSON.parse`), commenter pourquoi.
- **Pas de nullish accidentel** : pas de `!` après une variable potentiellement nulle sans guard préalable. Utiliser `??`, `?.`, ou narrowing.
- **Types DTO partagés** : dans `src/types/` (mappés sur `<X>Request` / `<X>Response` du backend). À terme : générer auto depuis l'OpenAPI du backend (`openapi-typescript`).

---

## Architecture & composants

- **Server Components par défaut** ; ajouter `"use client"` uniquement quand c'est nécessaire (hooks React, état, événements DOM).
- **shadcn** : générer les composants via la CLI shadcn dans `src/components/ui/`. Ne pas les modifier directement, étendre via composition.
- **Composants métier** : `src/components/forms/`, `src/components/layout/`, etc. — un dossier par préoccupation.
- **Composant minimal (pendant frontend de la règle 22 backend)** : le composant `*.tsx` ne contient **pas de logique métier dans le JSX**. Pas de `if (statut === 'PAYEE' && montantTotal > 1000) { ... else if (...) { ... } }` à l'intérieur d'un return JSX. Extraire dans : (a) un hook personnalisé (`useFactureStatus`), (b) une fonction utilitaire `lib/<domain>/helpers.ts`, ou (c) un composant de présentation pur.
- **Composant < 300 lignes** indicatif. Au-delà : signal de découpage en sous-composants.

---

## Styles

- **Tailwind 4** : tous les tokens (couleurs, radius, etc.) viennent de `globals.css` (CSS variables) — pas de `theme.extend`.
- Pas de CSS classique en dehors de `globals.css`. Utiliser uniquement les classes Tailwind ou les utilitaires `cn(...)` (clsx + tailwind-merge) pour la composition conditionnelle.

---

## Forms

- **`react-hook-form` + résolveur `zod`**. Schéma Zod dans `src/lib/validations/`.
- Validation côté client + revalidation côté serveur via le backend (jamais faire confiance au seul Zod côté client).
- Composants de formulaire dans `src/components/forms/<Domaine>Form.tsx`.

---

## Data fetching

- **Côté client** : React Query (`useQuery`, `useMutation`) — hooks dans `src/hooks/use<Domaine>.ts`.
- **Côté serveur (RSC)** : `fetch` natif (Next.js wrappé) avec cache/revalidate appropriés.
- **HTTP** : une seule instance axios partagée dans `src/lib/api/client.ts` qui injecte le bearer JWT.
- **Pas de `useEffect` pour data fetching côté client** : toujours via TanStack Query. `useEffect` reste autorisé pour : abonnements DOM, timers, effets secondaires non-data.

---

## State management

- **Server state** : React Query (jamais Zustand pour des données serveur).
- **Client state global** (UI, currentTenant, sidebar, …) : Zustand 5 — stores dans `src/stores/`.
- **Local state** : `useState` / `useReducer` à l'intérieur du composant.
- **Hiérarchie de décision** : `useState` → contexte React si partagé sur 2-3 niveaux → Zustand si traversant > 3 niveaux ou multi-pages.

---

## Auth

- Token stocké de façon sécurisée (à arbitrer : `httpOnly cookie` via route handler vs `localStorage`).
- Refresh token automatique via intercepteur axios sur 401.
- Logout idempotent (révoque côté backend, clear côté frontend).
- Garde de session sur les routes `(dashboard)/*` via layout dédié.

---

## Erreurs API

- Gestion globale des erreurs via React Query (`onError`) + toast.
- Mapping des erreurs i18n du backend → messages utilisateur (clés `entity.notFound`, `validation.error`, …).
- **HTTP 406** (convention backend) : traité comme "Not Found" côté UX (le backend retourne 406 pour `EntityException`, voir `GlobalException`).

---

## Internationalisation

- Pour l'instant : interface en français.
- Plus tard : `next-intl` ou équivalent — au minimum FR, EN ensuite.
- **Aucun texte UI hardcodé** dès qu'on aura branché `next-intl`. Convention : clés `<domain>.<context>.<key>` (ex. `vente.facture.alreadyPaid`).

---

## Accessibilité

- Composants `@base-ui/react` (sous shadcn) → accessibles par défaut (focus, ARIA).
- Labels explicites sur tous les inputs, alt sur toutes les images, contrastes respectés.

---

## Tests

- **Vitest + Testing Library** (à mettre en place — nice-to-have actuellement).
- Tests par composant métier (forms, layouts complexes), pas par composant UI shadcn.
- E2E sur les flux critiques (login, inscription, vente, achat) via Playwright (futur).

---

## Règles transverses (héritées du backend, adaptées)

Les règles 22–37 de `CONVENTION_CODAGE_BACKEND.md` ont leur équivalent frontend. Cette section liste les adaptations.

### 22. Composants minimaux (équivalent backend : controllers minimaux)

Pas de logique métier dans le JSX d'un composant. Pas de branches métier (`if (statut === 'PAYEE' && ...)`), pas de transformations complexes (`facture.lignes.filter(...).reduce(...)`) inline dans le `return`. Extraire dans :
- Un hook personnalisé (`useFactureStatus`, `useCaisseResume`),
- Une fonction utilitaire (`lib/vente/helpers.ts`),
- Un sous-composant de présentation pur.

### 27. Pas de fonction privée non-réutilisable dans un composant (équivalent backend : pas de méthode privée dans services applicatifs)

Toute logique factorisable doit être **exportée** dans un hook ou un util `lib/`. Une fonction interne au composant qui n'est utilisée qu'à un seul endroit reste tolérée si elle est un helper trivial (formatage local, etc.). Sinon : extraction.

### 28. Strategy pour dispatch par sous-type (équivalent direct)

Pas de cascade `if (type === 'A') ... else if (type === 'B') ...` dans le JSX ou un handler. Utiliser :
```ts
const renderers: Record<MoyenPaiement, () => ReactNode> = {
  CASH: () => <CashIcon />,
  WAVE: () => <WaveIcon />,
  // ...
};
return renderers[paiement.moyen]?.() ?? <DefaultIcon />;
```

### 29. JSDoc concise sur les hooks et composants complexes

- **TSDoc de composant / hook** : 1 phrase qui annonce la responsabilité (+ 1 si garantie particulière).
- **Aucun commentaire à l'intérieur du corps**. Si nécessaire : c'est un signal qu'il manque un meilleur nom de variable / fonction.
- **Langue** : français pour les TSDoc, anglais pour les identifiants et logs (cohérence projet).

### 30. Max 3 paramètres / props "racines"

- **Fonctions / hooks** : ≤ 3 paramètres. Au-delà : regrouper dans un objet typé (`type <X>Params = { ... }`).
- **Composants** : ≤ 3 props "racines". Au-delà : grouper dans un objet `props` (sauf primitifs très utilisés comme `className`, `children`, `disabled`).

### 31. Indentation + doc des fonctions/composants multi-étapes

Toute fonction ou composant qui enchaîne plusieurs étapes (init, transformation, fetch, render) :
- **Indentée par blocs** : séparer les étapes par des lignes vides.
- **Documentée** : TSDoc concise (1-3 phrases).
- **Pas de commentaires inline** — l'indentation + le TSDoc structurent.

### 32. Variables, paramètres ET noms d'alias explicites

- Bannir noms d'1 lettre (`q`, `c`, `m`) et abréviations cryptiques (`dto`, `obj`, `data`).
- **Paramètres** : nom métier complet (`searchTerm` au lieu de `q`, `client` au lieu de `c`).
- **Variables locales** : pareil.
- **Refs / state** : `const userRef = useRef(...)`, pas `const r = useRef(...)`.
- **Lambdas Stream triviaux**, index de boucle `i`, et `e` pour `Event` ou `Exception` dans un catch restent tolérés.

### 33. Type `<X>Filter` dès 2 critères

Dès qu'un hook ou une fonction prend ≥ 2 critères de filtrage, créer un type dédié dans `src/types/` :
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
Le hook : `useCommandesVente(filter: CommandeVenteFilter)`. Le composant construit le filter localement.

### 34. Méthodes fonctionnelles par défaut (`.map / .filter / .reduce`)

`for` n'est autorisé que si la performance l'exige (hot path mesuré). Sinon : `array.map`, `.filter`, `.reduce`, `.forEach`, `.find`. Commenter une boucle `for` si nécessaire pour expliquer pourquoi.

### 35. Extraire les valeurs répétées dans une const locale

```ts
// Mal :
return <div>
  <h1>{commande.facture.numero}</h1>
  <span>Montant : {commande.facture.montantTotal}</span>
  <span>Reste : {commande.facture.montantTotal - commande.facture.montantPaye}</span>
</div>

// Bien :
const facture = commande.facture;
const montantRestant = facture.montantTotal - facture.montantPaye;
return <div>
  <h1>{facture.numero}</h1>
  <span>Montant : {facture.montantTotal}</span>
  <span>Reste : {montantRestant}</span>
</div>
```

### 36. Render item complexe → sous-composant

Tout corps de `.map(item => ...)` qui dépasse 3-5 lignes ou contient plusieurs branches conditionnelles doit être extrait en sous-composant :
```tsx
// Bien :
{commandes.map(commande => <CommandeRow key={commande.id} commande={commande} />)}
```
Le sous-composant `CommandeRow` vit dans le même fichier (si privé) ou dans `src/components/<domaine>/` (si réutilisé).

### 37. Lignes vides autour des blocs hooks / effets / transformations

```tsx
// Bien :
const { data: commandes, isLoading } = useCommandesVente(filter);

const totalMontant = useMemo(
  () => commandes?.content.reduce((acc, c) => acc + c.montantTotal, 0) ?? 0,
  [commandes]
);

useEffect(() => {
  setSidebarTitle('Ventes');
}, [setSidebarTitle]);

return ( ... );
```
Une ligne vide avant et après chaque bloc `useQuery`, `useMutation`, `useMemo`, `useCallback`, `useEffect`, ou transformation multi-ligne.

---

## Logs / debug

- **Pas de `console.log` en production**. Acceptable temporairement en dev, à supprimer avant commit.
- Erreurs catchées : `console.error` autorisé en complément du toast utilisateur (debug devtools).

---

## Conventions de commits

Style projet : **Conventional Commits en français** (`feat(scope): description`, `fix(scope): ...`, `chore: ...`, `docs: ...`, `refactor: ...`, `test: ...`). Aligné avec la convention backend.

```
feat(<scope>): <résumé court>

[corps optionnel : ce que ça fait, pourquoi, notes techniques]
```

**Scope** : nom du domaine (`auth`, `vente`, `produit`, `layout`, `forms`, …) entre parenthèses.

**Aucun footer `Co-Authored-By: Claude`** dans les commits. Pas de push automatique sans demande explicite.

**Quand committer** : à chaque fin de tâche validée (cf. `TODO.md`).
