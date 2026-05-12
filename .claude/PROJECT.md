# PROJECT.md — Specs du projet

---

## 1. Vision générale

**Nom du projet** → STORE

**En une phrase** → Plateforme SaaS multi‑tenant pour la gestion d'un magasin de pièces détachées (vente, achat, stock, comptabilité). Mono‑repo composé de **`store/`** (backend Spring Boot) et **`store-frontend/`** (frontend Next.js).

**Quel problème ça résout ? Pour qui ?**
→ Informatiser la gestion complète d'un magasin de pièces de rechange. ERP conçu comme une plateforme **SaaS multi‑magasins et multi‑utilisateurs**, vendue par abonnement aux propriétaires de magasins.

---

## 2. Objectif principal — modules métier

1. Authentification & Sécurité (JWT, RBAC)
2. Gestion Entreprise (tenant racine)
3. Gestion Magasin (sous‑tenant : un propriétaire peut avoir plusieurs magasins)
4. Gestion Utilisateurs (Propriétaire, Employé)
5. Gestion Produits (catalogue, catégories, qualité, fournisseurs liés)
6. Gestion Stock (entrées/sorties FIFO, mouvements, seuils)
7. Gestion Achats (commandes, factures, paiements fournisseur)
8. Gestion Ventes (commandes, factures, paiements client)
9. Gestion Dépenses
10. Gestion Paiements (achat / vente / abonnement)
11. Gestion Abonnements SaaS (plans, types, coupons, promotions)
12. Gestion Notifications (notifications + échéances + templates)
13. Gestion Documents & Images (`PieceJointe`)
14. Dashboard & Reporting (à venir)
15. Paramétrage Système
16. Audit & Historique (`AuditableEntity` : created/updated by/at)
17. Multi‑tenant & Permissions
18. API & Intégrations (REST + OpenAPI)

---

## 3. Utilisateurs

**Qui utilise** → Propriétaires de magasins (clients SaaS) + leurs employés (vendeurs).

**Rôles** :
- `Proprietaire` — possède une `Entreprise`, gère ses magasins, ses employés et son abonnement.
- `Employe` — rattaché à un magasin, gère ventes/achats/stock selon ses permissions.
- (Futur) Admin SaaS — gestion plateforme côté éditeur.

Permissions fines via `Role` ↔ `Permissions` (N‑N).

---

## 4. Fonctionnalités

**Indispensables (MVP)** :
- Inscription propriétaire + création entreprise + premier magasin
- Authentification JWT + refresh token
- CRUD produits / catégories / qualité
- Gestion stock FIFO (entrées via achats, sorties via ventes)
- Cycle achat : commande → facture → paiement
- Cycle vente : commande → facture → paiement
- Abonnement SaaS avec essai gratuit (`PlanAbonnement.trial`, `Entreprise.trialUsed`)

**Secondaires (nice‑to‑have)** :
- Notifications email/SMS sur échéances impayées
- Coupons et promotions sur abonnements
- Inventaires physiques avec écarts
- Dashboard et rapports

**Hors scope** :
- Caisse physique / TPV
- Comptabilité analytique avancée
- Mobile natif (le frontend web devra rester responsive)

---

## 5. Stack technique

### Backend (`store/`)
- **Langage** → Java 21
- **Framework** → Spring Boot 4.0.6 (web, data‑jpa, security, validation, actuator)
- **Base de données** → PostgreSQL (Hibernate, `ddl-auto: update` en dev)
- **Auth** → Spring Security + JWT (`io.jsonwebtoken:jjwt 0.11.5`)
- **API doc** → springdoc‑openapi 2.8.1 (Swagger UI)
- **Boilerplate** → Lombok (`@Getter` / `@Setter` sur entités, jamais `@Data`)
- **Tests** → JUnit 5 + Mockito

### Frontend (`store-frontend/`)
- **Framework** → Next.js 16.2.6 (App Router, React Server Components)
  ⚠️ Next.js 16 a des **breaking changes** vs versions antérieures — voir `store-frontend/AGENTS.md` et `node_modules/next/dist/docs/` avant de coder.
- **Lib UI** → React 19.2.4 + TypeScript 5
- **Styles** → Tailwind CSS 4 (config CSS‑variables dans `src/app/globals.css`, plus de `tailwind.config.ts`)
- **Composants** → shadcn/ui (style `base-nova`, baseColor `neutral`) + `@base-ui/react` 1.4.1 (primitives headless, remplace Radix)
- **Icônes** → lucide‑react
- **Forms** → react‑hook‑form + zod + `@hookform/resolvers`
- **State client** → zustand 5
- **Data fetching** → TanStack React Query + axios
- **Lint** → ESLint (`eslint-config-next`)

### Déploiement
À définir (probable : Docker pour le backend, Vercel ou Docker pour le frontend).

---

## 6. Contraintes

**Techniques** :
- Multi‑tenant strict : toute requête doit être scopée à l'`Entreprise` / `Magasin` de l'utilisateur courant.
- Audit obligatoire sur toutes les entités métier (héritage `AuditableEntity`).
- IDs en `UUID` (pas de `Long auto-increment`).

**Données sensibles** :
- Mots de passe (hashés côté `Account.password`)
- Tokens (JWT + `RefreshToken`)
- Données financières (montants `BigDecimal precision=19 scale=2`)

---

## 7. Priorité absolue

Si une seule chose à livrer : **flux d'inscription d'un propriétaire avec création d'entreprise + premier magasin + activation d'un abonnement d'essai**, suivi de l'authentification JWT (backend) + **écran d'inscription / login sur le frontend qui consomme ces endpoints**. Tout le reste s'enchaîne derrière ce flux.
