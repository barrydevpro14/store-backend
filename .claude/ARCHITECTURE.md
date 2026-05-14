# ARCHITECTURE.md — Stack & Structure

> Mono‑repo : **`store/`** (backend Spring Boot) + **`store-frontend/`** (frontend Next.js). Le backend expose une API REST/JWT consommée par le frontend.
>
> Pour les **conventions de codage** (nommage, règles obligatoires, tests, etc.) → voir **`CONVENTION_CODAGE.md`**.

---

## Stack technique — Backend (`store/`)

| Élément          | Choix                                          | Notes |
|------------------|------------------------------------------------|-------|
| Langage          | Java 21                                        | Records, pattern matching utilisés |
| Framework        | Spring Boot 4.0.6                              | web, data-jpa, security, validation, actuator |
| Base de données  | PostgreSQL                                     | `ddl-auto: validate` + Flyway |
| Auth             | Spring Security + JWT (`jjwt 0.11.5`)          | `UserPrincipal` (record) dans le SecurityContext |
| API doc          | springdoc-openapi 2.8.1                        | Swagger UI sur `/swagger-ui.html` |
| Boilerplate      | Lombok                                         | `@Getter` / `@Setter` uniquement sur entités |
| Tests            | JUnit 5 + Mockito + AssertJ                    | + spring-boot-starter-test |
| Déploiement      | À définir                                      | Docker / VPS probables |

## Stack technique — Frontend (`store-frontend/`)

| Élément             | Choix                                                          | Notes |
|---------------------|----------------------------------------------------------------|-------|
| Framework           | **Next.js 16.2.6** (App Router, RSC)                           | ⚠️ Breaking changes vs versions antérieures, voir `store-frontend/AGENTS.md` |
| Lib UI              | React 19.2.4                                                   | Server Components par défaut, `"use client"` au besoin |
| Langage             | TypeScript 5 (strict)                                          | path alias `@/*` → `./src/*` |
| Styles              | Tailwind CSS 4                                                 | Config CSS‑variables dans `src/app/globals.css`, plus de `tailwind.config.ts` |
| Composants          | shadcn/ui (style `base-nova`, baseColor `neutral`)             | RSC activé, `components.json` à la racine du frontend |
| Primitives headless | `@base-ui/react` 1.4.1                                         | Remplace Radix dans la nouvelle génération de shadcn |
| Icônes              | lucide-react                                                   | |
| Forms               | react-hook-form + zod + `@hookform/resolvers`                  | Validation schémas Zod côté client |
| HTTP                | axios                                                          | Instance partagée à créer dans `src/lib/` |
| Data fetching       | TanStack React Query                                           | ⚠️ installé via `"root": "github:tanstack/react-query"` — vérifier que l'import fonctionne |
| State client        | zustand 5                                                      | Pour l'état global non‑serveur |
| Lint                | ESLint + `eslint-config-next`                                  | |

---

## Architecture applicative — Backend

**DDD / hexagonale par module métier**, structure répétée dans chaque module :

```
<module>/
├── domain/
│   ├── model/         # entités JPA (cœur métier)
│   ├── enums/
│   ├── repository/    # ports purs : <Entity>Repository extends BaseRepository<Entity>
│   └── service/       # services domaine (héritent de GlobalService<E, <Entity>Repository>)
├── application/
│   ├── service/       # cas d'usage / orchestration (utilisent les ports domaine)
│   ├── dto/
│   └── mappers/
├── infrastructure/
│   ├── repository/    # adapters Spring Data : <Entity>JpaRepository extends JpaRepository, <Entity>Repository
│   └── specifications/
└── presentation/      # controllers REST
```

Modules : `entreprise`, `magasin`, `users`, `security`, `produit`, `stock`, `inventaire`, `achat`, `vente`, `abonnement`, `notification`, `depense`, `common`, `config`, `property`.

> **Hexagonal pragmatique** : le domaine ne dépend pas de Spring Data. `domain/repository/<Entity>Repository` est une interface pure (extends `org.store.common.repository.BaseRepository<E>`). L'adaptateur Spring Data `infrastructure/repository/<Entity>JpaRepository extends JpaRepository<E, UUID>, <Entity>Repository` fournit l'implémentation — Spring Data injecte automatiquement le bean partout où on demande un `<Entity>Repository`.
>
> Compromis assumé : les entités JPA (`@Entity`, `@Column`, Lombok) restent dans `domain/model/`. Aller jusqu'à des POJO purs + persistence model séparé + mappers a un coût disproportionné pour le projet.

### Package transverse `common/`

```
common/
├── base/           # BaseEntity, AuditableEntity
├── dto/            # DTOs génériques (ImageDownloadResponse, ErrorResponse, …)
├── enums/
├── exceptions/     # LocalizedRuntimeException + custom exceptions
├── i18n/           # IMessageSourceService + impl
├── model/          # PieceJointe (utilisé par plusieurs modules)
├── repository/     # BaseRepository<E>
├── service/        # GlobalService<E, R>, ValidatorService, IUploadFileService
├── tools/          # DateHelper, EnumHelper, UuidHelper (helpers stateless)
└── validation/     # validators custom @Phone, @EnumValue, @DatePattern, @Uuid
```

---

## Architecture applicative — Frontend

```
store-frontend/
├── src/
│   ├── app/                    # App Router (routes, layouts, RSC)
│   │   ├── layout.tsx          # root layout (à customiser, lang="fr")
│   │   ├── page.tsx            # landing (encore le template Next.js)
│   │   └── globals.css         # Tailwind 4 + CSS variables shadcn
│   ├── components/
│   │   └── ui/                 # composants shadcn (button.tsx déjà là)
│   └── lib/
│       └── utils.ts            # cn() helper (clsx + tailwind-merge)
├── components.json             # config shadcn (style base-nova, lucide)
├── next.config.ts
├── tsconfig.json               # alias @/* → ./src/*
├── eslint.config.mjs
└── postcss.config.mjs          # Tailwind 4 PostCSS plugin
```

### Découpage cible (à créer au fur et à mesure)

```
src/
├── app/
│   ├── (auth)/                 # route group : login, register
│   ├── (dashboard)/            # route group : pages authentifiées
│   │   ├── layout.tsx          # sidebar + header partagés
│   │   ├── produits/
│   │   ├── stock/
│   │   ├── ventes/
│   │   └── achats/
│   └── api/                    # route handlers Next.js (proxy/BFF si besoin)
├── components/
│   ├── ui/                     # composants shadcn (générés)
│   ├── forms/                  # composants de formulaire métier
│   └── layout/                 # sidebar, header, etc.
├── lib/
│   ├── api/                    # instance axios + appels backend typés
│   ├── auth/                   # gestion token JWT (storage + refresh)
│   ├── utils.ts
│   └── validations/            # schémas Zod
├── hooks/                      # React Query hooks (useProducts, useLogin, …)
├── stores/                     # stores Zustand (UI state, current tenant)
└── types/                      # types DTO partagés avec le backend
```

---

## Voir aussi

- **`CONVENTION_CODAGE.md`** — toutes les règles de nommage, conventions de code, règles obligatoires, validation, tests, i18n, commits.
- **`PROJECT.md`** — vision, objectifs, périmètre, utilisateurs.
- **`TODO.md`** — backlog priorisé.
- **`SESSIONS.md`** — journal des sessions de travail.
- **`store/FONCTIONNALITIES.md`** — récap des services applicatifs métier (entrée/flux/règles/sortie).
