# ARCHITECTURE.md — Stack & Structure

> Monorepo: **`store/`** (Spring Boot backend) + **`store-frontend/`** (Next.js frontend). The backend exposes a REST/JWT API consumed by the frontend.
>
> For **coding conventions** (naming, mandatory rules, tests, etc.) → see **`BACKEND_CODING_CONVENTIONS.md`** and **`FRONTEND_CODING_CONVENTIONS.md`**.

---

## Tech stack — Backend (`store/`)

| Item             | Choice                                         | Notes |
|------------------|------------------------------------------------|-------|
| Language         | Java 21                                        | Records, pattern matching used |
| Framework        | Spring Boot 4.0.6                              | web, data-jpa, security, validation, actuator |
| Database         | PostgreSQL                                     | `ddl-auto: validate` + Flyway |
| Auth             | Spring Security + JWT (`jjwt 0.11.5`)          | `UserPrincipal` (record) in the SecurityContext |
| API docs         | springdoc-openapi 2.8.1                        | Swagger UI at `/swagger-ui.html` |
| Boilerplate      | Lombok                                         | `@Getter` / `@Setter` only on entities |
| Tests            | JUnit 5 + Mockito + AssertJ                    | + spring-boot-starter-test |
| Deployment       | TBD                                            | Likely Docker / VPS |

## Tech stack — Frontend (`store-frontend/`)

| Item                | Choice                                                         | Notes |
|---------------------|----------------------------------------------------------------|-------|
| Framework           | **Next.js 16.2.6** (App Router, RSC)                           | ⚠️ Breaking changes vs earlier versions, see `store-frontend/AGENTS.md` |
| UI lib              | React 19.2.4                                                   | Server Components by default, `"use client"` when needed |
| Language            | TypeScript 5 (strict)                                          | Path alias `@/*` → `./src/*` |
| Styles              | Tailwind CSS 4                                                 | CSS-variables config in `src/app/globals.css`, no `tailwind.config.ts` anymore |
| Components          | shadcn/ui (style `base-nova`, baseColor `neutral`)             | RSC enabled, `components.json` at the frontend root |
| Headless primitives | `@base-ui/react` 1.4.1                                         | Replaces Radix in the new shadcn generation |
| Icons               | lucide-react                                                   | |
| Forms               | react-hook-form + zod + `@hookform/resolvers`                  | Zod schemas client-side |
| HTTP                | axios                                                          | Shared instance to be created in `src/lib/` |
| Data fetching       | TanStack React Query                                           | ⚠️ installed via `"root": "github:tanstack/react-query"` — verify the `@tanstack/react-query` import actually works |
| Client state        | zustand 5                                                      | For non-server global state |
| Lint                | ESLint + `eslint-config-next`                                  | |

---

## Application architecture — Backend

**DDD / hexagonal per business module**, with the same structure repeated in each module:

```
<module>/
├── domain/
│   ├── model/         # JPA entities (business core)
│   ├── enums/
│   ├── repository/    # pure ports: <Entity>Repository extends BaseRepository<Entity>
│   └── service/       # domain services (extend GlobalService<E, <Entity>Repository>)
├── application/
│   ├── service/       # use cases / orchestration (use domain ports)
│   ├── dto/
│   └── mappers/
├── infrastructure/
│   ├── repository/    # Spring Data adapters: <Entity>JpaRepository extends JpaRepository, <Entity>Repository
│   └── specifications/
└── presentation/      # REST controllers
```

Modules: `entreprise`, `magasin`, `users`, `security`, `produit`, `stock`, `inventaire`, `achat`, `vente`, `abonnement`, `notification`, `depense`, `common`, `config`, `property`.

> **Pragmatic hexagonal**: the domain does not depend on Spring Data. `domain/repository/<Entity>Repository` is a pure interface (extends `org.store.common.repository.BaseRepository<E>`). The Spring Data adapter `infrastructure/repository/<Entity>JpaRepository extends JpaRepository<E, UUID>, <Entity>Repository` provides the implementation — Spring Data automatically injects the bean wherever a `<Entity>Repository` is requested.
>
> Accepted trade-off: JPA entities (`@Entity`, `@Column`, Lombok) stay in `domain/model/`. Going all the way to pure POJOs + a separate persistence model + mappers has a cost disproportionate to the project's scale.

### Cross-cutting `common/` package

```
common/
├── base/           # BaseEntity, AuditableEntity
├── dto/            # generic DTOs (ImageDownloadResponse, ErrorResponse, …)
├── enums/
├── exceptions/     # LocalizedRuntimeException + custom exceptions
├── i18n/           # IMessageSourceService + impl
├── model/          # PieceJointe (used by several modules)
├── repository/     # BaseRepository<E>
├── service/        # GlobalService<E, R>, ValidatorService, IUploadFileService
├── tools/          # DateHelper, EnumHelper, UuidHelper (stateless helpers)
└── validation/     # custom validators @Phone, @EnumValue, @DatePattern, @Uuid
```

---

## Application architecture — Frontend

```
store-frontend/
├── src/
│   ├── app/                    # App Router (routes, layouts, RSC)
│   │   ├── layout.tsx          # root layout (to customize, lang="en")
│   │   ├── page.tsx            # landing (still the Next.js template)
│   │   └── globals.css         # Tailwind 4 + shadcn CSS variables
│   ├── components/
│   │   └── ui/                 # shadcn components (button.tsx already there)
│   └── lib/
│       └── utils.ts            # cn() helper (clsx + tailwind-merge)
├── components.json             # shadcn config (style base-nova, lucide)
├── next.config.ts
├── tsconfig.json               # alias @/* → ./src/*
├── eslint.config.mjs
└── postcss.config.mjs          # Tailwind 4 PostCSS plugin
```

### Target layout (to be built incrementally)

```
src/
├── app/
│   ├── (auth)/                 # route group: login, register
│   ├── (dashboard)/            # route group: authenticated pages
│   │   ├── layout.tsx          # shared sidebar + header
│   │   ├── produits/
│   │   ├── stock/
│   │   ├── ventes/
│   │   └── achats/
│   └── api/                    # Next.js route handlers (proxy/BFF if needed)
├── components/
│   ├── ui/                     # shadcn components (generated)
│   ├── forms/                  # business form components
│   └── layout/                 # sidebar, header, etc.
├── lib/
│   ├── api/                    # axios instance + typed backend calls
│   ├── auth/                   # JWT token handling (storage + refresh)
│   ├── utils.ts
│   └── validations/            # Zod schemas
├── hooks/                      # React Query hooks (useProducts, useLogin, …)
├── stores/                     # Zustand stores (UI state, current tenant)
└── types/                      # DTO types shared with the backend
```

---

## See also

- **`BACKEND_CODING_CONVENTIONS.md`** — all naming rules, coding conventions, mandatory rules, validation, tests, i18n, commits.
- **`FRONTEND_CODING_CONVENTIONS.md`** — same, for the frontend.
- **`PROJECT.md`** — vision, goals, scope, users.
- **`TODO.md`** — prioritized backlog.
- **`SESSIONS.md`** — work session journal.
- **`store/FEATURES.md`** — summary of business application services (input/flow/rules/output).
