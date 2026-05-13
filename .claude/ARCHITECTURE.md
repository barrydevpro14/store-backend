# ARCHITECTURE.md — Stack & Conventions

> Mono‑repo : **`store/`** (backend Spring Boot) + **`store-frontend/`** (frontend Next.js). Le backend expose une API REST/JWT consommée par le frontend.

---

## Stack technique — Backend (`store/`)

| Élément          | Choix                                          | Notes |
|------------------|------------------------------------------------|-------|
| Langage          | Java 21                                        | Records, pattern matching utilisés |
| Framework        | Spring Boot 4.0.6                              | web, data-jpa, security, validation, actuator |
| Base de données  | PostgreSQL                                     | `ddl-auto: update` (dev), à passer à `validate` + Flyway en prod |
| Auth             | Spring Security + JWT (`jjwt 0.11.5`)          | `UserPrincipal` (record) dans le SecurityContext |
| API doc          | springdoc-openapi 2.8.1                        | Swagger UI sur `/swagger-ui.html` |
| Boilerplate      | Lombok                                         | `@Getter` / `@Setter` uniquement sur entités |
| Tests            | JUnit 5 + Mockito                              | + spring-boot-starter-test |
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

## Conventions de code — Backend

**Classes / fichiers** : `PascalCase.java`
**Méthodes / variables** : `camelCase`
**Constantes** : `UPPER_SNAKE_CASE`
**Tables BD** : `snake_case` via `<Entity>.TABLE_NAME` constant + `@Table(name = TABLE_NAME)`
**Tests Unitaires et Integrations pour chaque fonctionnalites**
**Chaque Composant ou Service (Class) doit avoir son interface**
**Les methodes doivent utilises la projection avec DTO record en utilisant Query afin d'eviter de retourner toutes les donnees de l'entites**  

### Nommage spécifique au projet (à respecter strictement)

| Élément                       | Pattern                              | Exemple                          |
|-------------------------------|--------------------------------------|----------------------------------|
| Entité JPA                    | `<NomMétier>`                        | `Product`, `CommandeAchat`       |
| Port domaine (repo)           | `<Entity>Repository`                 | `ProductRepository` (`domain/repository/`) |
| Adapter Spring Data           | `<Entity>JpaRepository`              | `ProductJpaRepository` (`infrastructure/repository/`) |
| Service domaine (CRUD)        | `<Entity>DomainService`              | `ProductDomainService`           |
| Service applicatif            | interface `I<X>Service` + impl `<X>ServiceImpl` | `IAccountService` + `AccountServiceImpl` (règle obligatoire) |
| DTO entrée                    | `<X>Request`                         | `AccountRequest`, `RegisterPropertyRequest` (suffix `Request` obligatoire, jamais `Info`/`Dto`) |
| DTO sortie                    | `<X>Response`                        | `AuthResponse`, `EmployeResponse` — doit exposer un constructeur `(<X> entity)` qui extrait les champs |
| Paramètre DTO en méthode      | camelCase complet du type            | `AccountRequest accountRequest` (pas `info`/`request` générique — règle obligatoire) |
| Controller                    | `<Entity>Controller`                 | `AuthController` (`public static final String BASE_PATH = "/api/v1/<scope>"` — règle obligatoire) |
| Test service                  | `<X>ServiceImplTest`                 | JUnit 5 + Mockito, mock des deps, chemin nominal + erreurs (règle obligatoire) |
| Test controller               | `<X>ControllerTest`                  | `@WebMvcTest` + MockMvc ou `MockMvcBuilders.standaloneSetup()` (règle obligatoire) |

---

## Conventions de code — Frontend

**Composants / fichiers TSX** : `PascalCase.tsx` (sauf routes Next.js : `page.tsx`, `layout.tsx`, etc.)
**Hooks** : `useXxx.ts`
**Utils / lib** : `kebab-case.ts` ou `camelCase.ts`
**Types** : `PascalCase`
**Variables / fonctions** : `camelCase`
**Imports internes** : utiliser l'alias `@/...` (ex. `@/components/ui/button`, `@/lib/utils`)

### Règles spécifiques

- **Server Components par défaut** ; ajouter `"use client"` uniquement quand c'est nécessaire (hooks React, état, événements DOM).
- **Tailwind 4** : tous les tokens (couleurs, radius, etc.) viennent de `globals.css` (CSS variables) — pas de `theme.extend`.
- **shadcn** : générer les composants via la CLI shadcn dans `src/components/ui/`. Ne pas les modifier directement, étendre via composition.
- **Forms** : `react-hook-form` + résolveur `zod`. Schéma Zod dans `src/lib/validations/`.
- **Data fetching côté client** : React Query (`useQuery`, `useMutation`) ; côté serveur (RSC) : `fetch` natif.
- **HTTP** : une seule instance axios partagée dans `src/lib/api/client.ts` qui injecte le bearer JWT.
- **Auth** : token stocké de façon sécurisée (à arbitrer : `httpOnly cookie` via route handler vs `localStorage`).

---

## Règles spécifiques à ce projet

### Entités

- Toute entité étend `BaseEntity` (UUID id) ou `AuditableEntity` (UUID id + createdAt/updatedAt/createdBy/updatedBy).
- Annotations Lombok : **`@Getter` + `@Setter`** au niveau classe.
  - **Jamais** `@Data` (casse `equals`/`hashCode` avec collections JPA).
  - **Jamais** `@ToString` (lazy-loading → `LazyInitializationException`).
  - **Jamais** `@EqualsAndHashCode` sur entité (idem).
- Toutes les relations `@ManyToOne` / `@OneToOne` : `fetch = FetchType.LAZY` par défaut.
- Montants : `BigDecimal` avec `@Column(precision = 19, scale = 2)`.
- Constante `TABLE_NAME` publique sur chaque entité, utilisée dans `@Table(name = TABLE_NAME)`.

### Repositories

- **Port domaine** : `<Entity>Repository extends BaseRepository<Entity>` dans `<module>/domain/repository/`. Interface pure, aucune annotation Spring. Méthodes métier supplémentaires (`findByXxx`, etc.) s'ajoutent ici.
- **Adapter Spring Data** : `<Entity>JpaRepository extends JpaRepository<Entity, UUID>, <Entity>Repository` dans `<module>/infrastructure/repository/`. Spring Data fournit l'impl du CRUD et des `findByXxx` automatiquement.
- `BaseRepository<E>` (dans `common/repository/`) déclare le CRUD générique (`save`, `findById`, `findAll`, `findAll(Pageable)`, `existsById`, `count`, `deleteById`, `delete`) avec les mêmes signatures que `CrudRepository`/`JpaRepository` pour permettre l'héritage côté Spring Data.

### Services domaine

- Étendent `GlobalService<Entity, <Entity>Repository>` (CRUD générique fourni : `save`, `findById`, `findAll`, `findAll(Pageable)`, `existsById`, `count`, `deleteById`, `delete`).
- **Dépendent uniquement du port domaine `<Entity>Repository`** — jamais du `<Entity>JpaRepository` directement.
- `GlobalService` lève `EntityException` (custom, dans `org.store.common.exceptions`) si entité introuvable.
- Annotés `@Service`. Le constructeur passe le repo à `super(...)`.
- **Toute query custom** (`findByXxx`, `existsByXxx`, etc.) s'ajoute ici sous forme de méthode publique qui délègue au repo. Convention : renvoyer `Optional<E>` ; c'est l'app service qui throw.

### Services applicatifs (couche application)

- Pattern : interface `I<X>Service` + impl `<X>ServiceImpl` (règle obligatoire).
- **Injectent uniquement** :
  - `<X>DomainService` (leur propre agrégat) pour CRUD/queries.
  - `I<Y>Service` (autres agrégats) — jamais `<Y>Repository` directement.
  - Des beans techniques (`PasswordEncoder`, `IJwtService`, `ICurrentUserService`, `IMessageSourceService`, etc.).
- Orchestrent les use cases : règles métier, exceptions i18n (`ForbiddenException`, `EntityException`, `BadArgumentException`), transactions (`@Transactional`).
- **Code réutilisable** entre services : méthode publique du service propriétaire de l'agrégat, jamais helper privé dupliqué.

### Validation Bean Validation + validators custom

- `ValidatorService` (dans `common/service`) wrappe `jakarta.validation.Validator`.
  - `validate(obj, groups…)` lève `ConstraintViolationException`.
  - `check(obj, groups…)` retourne le set de violations.
- **Validateurs custom** dans `org.store.common.validation/` :
  - `@Phone` / `PhoneValidation` — regex SN (`^(70|75|76|77|78|33)\d{7,9}$`).
  - `@EnumValue(enumClass, ignoreCase)` / `EnumValidator` — la String reçue doit matcher un `enum.name()`.
  - `@DatePattern(pattern)` / `DatePatternValidation` — `LocalDate.parse(value, formatter)` ; pattern par défaut `"yyyy-MM-dd"`.
  - `@Uuid` / `UuidValidator` — `UUID.fromString(value)`.
- Tous skippent `null`/empty (combiner avec `@NotBlank`/`@NotNull` si requis). Chaque validateur a son test paramétré.
- Clés i18n : `validation.phone.invalid`, `validation.enum.invalid`, `validation.date.pattern.invalid`, `validation.uuid.invalid`.

### Multi-tenant

- À chaque opération métier, scoper sur `getCurrentEntrepriseId()` / `getCurrentMagasinId()` (via `UserPrincipal` dans le `SecurityContext`).
- ⚠️ Pas encore d'`@Filter` Hibernate ni d'intercepteur global — c'est aux services de filtrer explicitement pour l'instant.

### Audit

- `AuditableEntity` utilise `@CreatedDate / @LastModifiedDate / @CreatedBy / @LastModifiedBy`.
- `@EnableJpaAuditing` est actif sur `StoreApplication`.
- `AuditorAwareImpl` (`org.store.config`) — lit le `UserPrincipal` du `SecurityContext` et renvoie `userId.toString()` ; `Optional.empty()` si pas d'authentification.

### Persistance / Migrations

- **Flyway** activé sur tous les profils. Driver `flyway-database-postgresql`.
- Dépendance : `org.springframework.boot:spring-boot-starter-flyway` (Spring Boot 4 a sorti l'autoconfig Flyway de `spring-boot-autoconfigure`).
- `ddl-auto: validate` — Hibernate vérifie que le schéma matche les entités, ne crée rien.
- `baseline-on-migrate: true`, `baseline-version: 1` — pour les BDD historiques peuplées par `ddl-auto: update`, Flyway baseline silencieusement et n'applique pas V1.
- Scripts : `src/main/resources/db/migration/V<n>__<description>.sql` (Flyway naming standard).

### Sécurité / JWT

- **Configuration externalisée** dans `application.yml` sous `security.jwt` (secret, header, prefix, expiration access/refresh) → bindée via le record `org.store.property.JwtProperties` (`@ConfigurationProperties` + `@ConfigurationPropertiesScan` sur `StoreApplication`).
- **`JwtService`** (`security/application/service/`) — `generateToken(UserPrincipal)`, `isTokenValid(String)`, `extractUserPrincipal(String)`. jjwt 0.11.5, HS512.
- **`JwtAuthenticationFilter`** (`security/config/`) — extrait le Bearer (via `JwtProperties.header()`/`prefix()`), valide, pose un `UserPrincipal` dans le `SecurityContext`. Authorities = `UserPrincipal.permissions()`.
- **`UserDetailsServiceImpl`** — `loadUserByUsername` via le port `AccountRepository.findByUsername`, authorities = `Role.permissions.code`. `@Transactional(readOnly = true)` pour initialiser les collections lazy.
- **`UserPrincipal` (record)** — `userId / entrepriseId / magasinId / username / role / permissions`. Stocké dans `Authentication.principal`. Overload `hasPermission(PermissionCode)` pour éviter les strings.
- **Clés de claims JWT** centralisées dans l'enum `org.store.security.application.enums.Claim` (`ENTREPRISE`, `MAGASIN`, `ROLE`, `USERNAME`, `PERMISSIONS`) — toujours utiliser `Claim.X.getKey()`, jamais de string littérale.
- **Codes de permissions** centralisés dans l'enum `org.store.security.application.enums.PermissionCode` (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`). Le code en BDD = `enum.name()`.
- **`ICurrentUserService`** (`security/application/service/`) — lit le `UserPrincipal` du `SecurityContextHolder`. Throw `UnauthorisedException("auth.current.missing")` si auth absente/anonyme/mauvais type. **Préférer cette injection** plutôt que `@AuthenticationPrincipal UserPrincipal` au controller (testable plus simplement).
- **`SecurityConfig`** : `/auth/**` permitAll, reste authentifié, sessions stateless, JWT filter avant `UsernamePasswordAuthenticationFilter`.
- ⚠️ Le filtre ne re-vérifie PAS le statut du compte (`enabled`/`locked`) à chaque requête — il fait confiance au JWT signé.

### Internationalisation (i18n)

- **Langues supportées** : `fr` (défaut), `en`. Locale par requête résolue via le header `Accept-Language` (`AcceptHeaderLocaleResolver`).
- **Config** : `org.store.config.I18nConfig` — beans `MessageSource` (`ReloadableResourceBundleMessageSource`, basename `classpath:messages`, UTF-8, `useCodeAsDefaultMessage=true`, default `Locale.FRENCH`), `LocaleResolver`, et `LocalValidatorFactoryBean` câblé sur le `MessageSource` (→ erreurs Bean Validation traduites).
- **Fichiers** : `src/main/resources/messages.properties` (FR par défaut, sans suffixe) et `messages_en.properties` (EN). Le contenu est regroupé en sections : messages applicatifs (`entity.*`, `validation.*`, `error.*`…) puis clés Bean Validation (`jakarta.validation.constraints.*.message`).
- **Service de résolution** : `org.store.common.i18n.IMessageSourceService` (4 surcharges : `getMessage(code)`, `getMessage(code, args)`, `getMessage(code, classType)`, `getMessage(code, args, locale)`) + impl `MessageSourceServiceImpl`. À injecter dans tout service applicatif/handler qui doit rendre un message localisé. Pour les jobs/mails hors contexte HTTP, utiliser la surcharge avec `Locale` explicite (sinon `LocaleContextHolder` peut être vide).
- **Exceptions localisées** : `LocalizedRuntimeException` (abstract, dans `common/exceptions/`) porte `messageKey` + `args`. **Toutes les exceptions custom héritent de cette classe** (sauf `AuthentificationException` qui hérite de Spring Security). Constructeur unique : `(String messageKey, Object... args)`. Le `RestControllerAdvice` `GlobalException` injecte `IMessageSourceService` et résout via un helper `resolve(LocalizedRuntimeException)`.
- **Au throw** : passer une clé i18n + des args (`throw new EntityException("entity.notFound", id)`). Si la clé n'existe pas dans les `.properties`, elle est retournée telle quelle (fallback `useCodeAsDefaultMessage`), donc un message brut hardcodé reste accepté en transition.
- **Convention de naming des clés** : préfixe par domaine — `entity.<verb>` (`entity.notFound`), `validation.<type>` (`validation.error`), `error.<type>` (`error.unexpected`), `auth.<type>`, etc.

---

## Documentation use cases

Le récap (entrée, flux, règles, exceptions, sortie) de chaque service applicatif métier vit dans **`store/FONCTIONNALITIES.md`** à la racine du backend. À mettre à jour à chaque nouveau service.

---

## Règles de codage transverses (récap — à respecter sur chaque ajout)

### Couches et injection

1. **Repository → DomainService → ServiceImpl** : un `<X>ServiceImpl` n'injecte JAMAIS un repository — il passe par son `<X>DomainService`. Pour un autre agrégat, il passe par l'interface `I<Y>Service` (jamais `<Y>Repository`, jamais `<Y>DomainService` cross-agrégat).
2. **DomainService renvoie `Optional<E>`** pour les queries custom ; c'est l'app service qui throw l'exception métier appropriée.
3. **Toute query custom** (`findByXxx`, `existsByXxx`, projection) est exposée comme méthode publique du DomainService propriétaire, puis (au besoin) façadée par `I<X>Service`.
4. **Code réutilisable** (extract, transform, compute) entre plusieurs services → méthode publique du service propriétaire de l'agrégat. Jamais de helper privé dupliqué entre fichiers.

### DTO et mapping

5. **`<X>Request`** (entrée) avec suffixe `Request` obligatoire, jamais `Info`/`Dto`/`Form`. Placés dans `<module>/application/dto/`.
6. **`<X>Response`** (sortie) avec suffixe `Response` obligatoire. **Doit exposer un constructeur secondaire `(<X> entity)`** qui extrait les champs depuis l'entité (centralise le mapping, supprime la duplication).
7. **Paramètre méthode de type DTO** : nom = camelCase complet du type (`AccountRequest accountRequest`), pas `info`/`request`/`dto` génériques.
8. **Champs FK obligatoires dans un Request** : `@NotNull UUID xxxId` (Jackson gère le parsing UUID, retourne 400 sur format invalide).

### Permissions, rôles, sécurité

9. **`PermissionCode` enum** centralise les codes (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, …). Code en BDD = `enum.name()`. Plus aucune chaîne de permission dans le code applicatif.
10. **Aucun libellé de rôle hardcodé** dans l'app service (`"MANAGER"`, `"VENDEUR"`, …). Les règles s'expriment via les **permissions du rôle** : ajouter `CAISSIER` demain = ajouter une `ensureRole(...)` dans `DataInitializer`, **zéro changement applicatif**.
11. **`UserPrincipal`** : `userId / entrepriseId / magasinId / username / role (libellé) / permissions`. Overload `hasPermission(PermissionCode)`. Récupéré via **`ICurrentUserService.getCurrent()`** dans les services métier (pas `@AuthenticationPrincipal` au controller — testable plus simplement).
12. **Pas de duplication du `@PreAuthorize`** : si le controller filtre par `hasAuthority(X)`, le service ne re-checke pas X. Il se concentre sur les règles métier fines.

### Validation Bean Validation

13. **Validators custom** dans `org.store.common.validation/` : `@Phone`, `@EnumValue`, `@DatePattern`, `@Uuid`. Pattern type : annotation + classe `<Name>Validation` qui implémente `ConstraintValidator<X, String>`. Skip `null`/empty (combiner avec `@NotBlank`/`@NotNull` si requis). Chaque validateur a sa clé i18n + son test paramétré.

### i18n

14. **Tous les messages d'erreur** passent par une clé i18n (FR + EN dans `messages*.properties`). Aucun message hardcodé en français/anglais dans les exceptions ou les throws. Pour les exceptions hors `LocalizedRuntimeException` (`UsernameNotFoundException` Spring Security par ex.), résoudre via `IMessageSourceService.getMessage(key, args)`.
15. **Convention de naming des clés** : `<domaine>.<verbe>` ou `<domaine>.<sous-cas>` (`entity.notFound`, `validation.phone.invalid`, `employe.create.elevatedRole.forbidden`, `magasin.notOwned`).

### Tests

16. **Chaque service applicatif** a un `<X>ImplTest` (JUnit 5 + Mockito + AssertJ). Cas nominal + cas d'erreur.
17. **Chaque controller** a un `<X>ControllerTest` (`MockMvcBuilders.standaloneSetup()` en SB 4 — `@WebMvcTest` exige `spring-boot-starter-webmvc-test`).
18. **Mock du service interface** dans les tests d'autres services (jamais mock direct des DomainService internes à d'autres agrégats — ils ne devraient pas être injectés là).

### Naming

19. **Controller** : `public static final String BASE_PATH = "/api/v1/<scope>"` + `@RequestMapping(BASE_PATH)`. Pas de `@PathVariable("version")`.
20. **Service applicatif** : interface `I<X>Service` + impl `<X>ServiceImpl`. **L'impl vit dans `<module>/application/service/impl/`** (cohérence transverse). Injection via l'interface.
21. **`<Entity>JpaRepository extends JpaRepository, <Entity>Repository`** ; `<Entity>Repository extends BaseRepository<E>` (port pur).

### Controllers minimaux

22. **Aucune logique métier dans le controller** : handler = `return service.method(request)`. Interdit : cast d'entité, `new <X>Response(...)`, getter-chain dans un agrégat, orchestration multi-services, branches métier. Si du code "déborde", créer une méthode publique dans `I<X>Service` qui retourne déjà le `<X>Response` final. Choisir le service qui **orchestre déjà** la chaîne pour éviter les cycles de DI (vu : `IRegisterPropertyService.registerEntrepriseByAdmin` plutôt que `IEntrepriseService.createByAdmin`).

### DTO Response — sous-DTO

23. **Sous-DTO Response pour sous-entité** : si un `<X>Response` agrège **≥ 3 champs** d'une autre entité réutilisable (Utilisateur, Magasin, Entreprise…), créer un `<Y>Response` dédié (`AccountResponse(..., UserResponse user)`) plutôt que de mettre les champs à plat. Le sous-DTO suit la règle **Response(Entity)**. JSON hiérarchique, DRY, évolutif.

### Query JPQL — projection

24. **`SELECT new <X>Response(entity)` dans `@Query`** : projection JPQL via le constructeur secondaire de Response, jamais la liste des champs (DRY, pas de désync sur ajout de champ). Pré-requis : règle Response(Entity) appliquée.
25. **Toute `@Query` JPQL/SQL en text block `"""..."""`**, jamais en string concat.

### Domaine — création d'entité

26. **`create()` vit dans le DomainService** : construction (`new` + setters + `save`) dans `<X>DomainService.create(...)`. L'app service délègue + applique les règles métier (permissions, scoping, exceptions). Sépare "persistance" (domain) de "orchestration" (application).

### Pas de méthodes privées dans les services applicatifs

27. **Aucune méthode privée** dans les `<X>ServiceImpl`. Toute factorisation devient une méthode publique de `I<X>Service`, **paramétrée plutôt que spécialisée** (ex. `createAccount(request, String roleName)` au lieu de `doCreate(request)` qui hardcodait `ROLE_PROPRIETAIRE`). Exceptions tolérées : helpers triviaux sur DTO ou objet utilitaire (pas un service métier).

### Strategy pattern pour dispatch par sous-type

28. **Pas de `if (x instanceof A) ... else if (x instanceof B) ...`** dans le code de dispatch métier. Pattern : interface `<X>Strategy { Class<? extends Parent> targetType(); <Y> resolve(Parent x); }` + impls `@Component` + Spring injecte `List<Strategy>` + dispatch `most-specific-wins` :
    ```java
    strategies.stream()
        .filter(s -> s.targetType().isInstance(x))
        .reduce((a, b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)
        .map(s -> s.resolve(x))
        .orElseGet(<X>Result::empty);
    ```
    La sous-classe gagne sur la super-classe → la strategy sur `Parent` sert de fallback. Exemple : `security/application/strategies/UserPrincipalContextStrategy` avec impls Proprietaire/Employe/Utilisateur.

### Documentation des services applicatifs (process métier)

29. **Tout service applicatif (`<X>ServiceImpl`) doit être documenté en javadoc** — c'est l'endroit où vit la logique métier, donc l'endroit qui mérite l'effort de doc. Périmètre obligatoire :
    - **Javadoc de classe** : responsabilité du service en 1–3 phrases, garanties éventuelles (idempotent, transactionnel, stratégie particulière).
    - **Javadoc sur chaque méthode publique** : entrée (rôle de chaque paramètre non trivial), règles métier appliquées, exceptions levées avec leur clé i18n, sortie attendue. Pour une orchestration multi-étapes, lister les étapes numérotées.
    - **Commentaires de section** dans les méthodes longues (> 30 lignes) pour marquer les étapes ; commentaires inline pour expliquer le **pourquoi** d'un choix non évident (invariant, contrainte JPA, choix de comparaison par ID vs equals, etc.) — jamais pour décrire **quoi** le code fait (les identifiants y suffisent).
    - **Langue** : français pour la documentation, anglais pour les messages de log/identifiants (cohérence projet).
    - **Référence** : voir `RolesPermissionsSyncServiceImpl` comme modèle.

    L'intention : un nouveau développeur (ou Claude dans 3 semaines) doit comprendre un use case sans relire les services appelés. Les `<X>DomainService` et controllers restent libres de doc (auto-documentants par convention).

---

## Conventions de commits

Format Conventional Commits recommandé :

```
<type>(<scope>): <résumé court>

[corps optionnel]
```

Types : `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`.
Scope : nom du module (`vente`, `stock`, `auth`, …).

**Quand committer** : à chaque fin de tâche validée (cf. `TODO.md`), avec un message qui décrit le « pourquoi », pas le « quoi ».
