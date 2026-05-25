# BACKEND_CODING_CONVENTIONS.md — Backend coding rules

> Conventions and mandatory rules that apply to every addition or modification of **backend** code.
> For stack and package structure, see `ARCHITECTURE.md`. For the frontend, see `FRONTEND_CODING_CONVENTIONS.md`.

---

## Coding conventions

**Classes / files**: `PascalCase.java`
**Methods / variables**: `camelCase`
**Constants**: `UPPER_SNAKE_CASE`
**DB tables**: `snake_case` via the `<Entity>.TABLE_NAME` constant + `@Table(name = TABLE_NAME)`
**Unit and integration tests for each feature**
**Every component or service (class) must have its interface**
**Methods must use DTO record projection via `@Query` to avoid returning the entire entity**

### Project-specific naming (strict)

| Item                          | Pattern                              | Example                          |
|-------------------------------|--------------------------------------|----------------------------------|
| JPA entity                    | `<BusinessName>`                     | `Product`, `CommandeAchat`       |
| Domain port (repo)            | `<Entity>Repository`                 | `ProductRepository` (`domain/repository/`) |
| Spring Data adapter           | `<Entity>JpaRepository`              | `ProductJpaRepository` (`infrastructure/repository/`) |
| Domain service (CRUD)         | `<Entity>DomainService`              | `ProductDomainService`           |
| Application service           | interface `I<X>Service` + impl `<X>ServiceImpl` | `IAccountService` + `AccountServiceImpl` (mandatory) |
| Input DTO                     | `<X>Request`                         | `AccountRequest`, `RegisterPropertyRequest` (suffix `Request` mandatory, never `Info`/`Dto`) |
| Output DTO                    | `<X>Response`                        | `AuthResponse`, `EmployeResponse` — must expose a `(<X> entity)` constructor that extracts fields |
| Filter DTO                    | `<X>Filter`                          | `StockFilter`, `MouvementStockFilter` — record with Jakarta validations + helper methods |
| DTO parameter in method       | full camelCase of the type           | `AccountRequest accountRequest` (no generic `info`/`request` — mandatory) |
| Controller                    | `<Entity>Controller`                 | `AuthController` (`public static final String BASE_PATH = "/api/v1/<scope>"` — mandatory) |
| Service test                  | `<X>ServiceImplTest`                 | JUnit 5 + Mockito, mock dependencies, happy path + errors (mandatory) |
| Controller test               | `<X>ControllerTest`                  | `@WebMvcTest` + MockMvc or `MockMvcBuilders.standaloneSetup()` (mandatory) |

---

## Project-specific rules

### Entities

- Every entity extends `BaseEntity` (UUID id) or `AuditableEntity` (UUID id + createdAt/updatedAt/createdBy/updatedBy).
- Lombok annotations: **`@Getter` + `@Setter`** at class level.
  - **Never** `@Data` (breaks `equals`/`hashCode` with JPA collections).
  - **Never** `@ToString` (lazy-loading → `LazyInitializationException`).
  - **Never** `@EqualsAndHashCode` on an entity (same).
- All `@ManyToOne` / `@OneToOne` relations: `fetch = FetchType.LAZY` by default.
- Amounts: `BigDecimal` with `@Column(precision = 19, scale = 2)`.
- Public `TABLE_NAME` constant on each entity, used in `@Table(name = TABLE_NAME)`.

### Repositories

- **Domain port**: `<Entity>Repository extends BaseRepository<Entity>` in `<module>/domain/repository/`. Pure interface, no Spring annotation. Extra business methods (`findByXxx`, etc.) are added here.
- **Spring Data adapter**: `<Entity>JpaRepository extends JpaRepository<Entity, UUID>, <Entity>Repository` in `<module>/infrastructure/repository/`. Spring Data provides CRUD and `findByXxx` automatically.
- `BaseRepository<E>` (in `common/repository/`) declares generic CRUD (`save`, `findById`, `findAll`, `findAll(Pageable)`, `existsById`, `count`, `deleteById`, `delete`) with the same signatures as `CrudRepository`/`JpaRepository` to allow inheritance on the Spring Data side.

### Domain services

- Extend `GlobalService<Entity, <Entity>Repository>` (generic CRUD provided: `save`, `findById`, `findAll`, `findAll(Pageable)`, `existsById`, `count`, `deleteById`, `delete`).
- **Depend only on the domain port `<Entity>Repository`** — never on `<Entity>JpaRepository` directly.
- `GlobalService` throws `EntityException` (custom, in `org.store.common.exceptions`) when the entity is not found.
- Annotated `@Service`. The constructor passes the repo to `super(...)`.
- **Every custom query** (`findByXxx`, `existsByXxx`, etc.) is added here as a public method that delegates to the repo. Convention: return `Optional<E>`; the app service is responsible for throwing.

### Application services

- Pattern: interface `I<X>Service` + impl `<X>ServiceImpl` (mandatory).
- **Only inject**:
  - `<X>DomainService` (its own aggregate) for CRUD/queries.
  - `I<Y>Service` (other aggregates) — never `<Y>Repository` directly.
  - Technical beans (`PasswordEncoder`, `IJwtService`, `ICurrentUserService`, `IMessageSourceService`, `ValidatorService`, etc.).
- Orchestrate use cases: business rules, i18n exceptions (`ForbiddenException`, `EntityException`, `BadArgumentException`), transactions (`@Transactional`).
- **Reusable code** between services: a public method on the aggregate's owner service, never a duplicated private helper.

### Bean Validation + custom validators

- `ValidatorService` (in `common/service`) wraps `jakarta.validation.Validator`.
  - `validate(obj, groups…)` throws `ConstraintViolationException`.
  - `check(obj, groups…)` returns the violation set.
- **Custom validators** in `org.store.common.validation/`:
  - `@Phone` / `PhoneValidation` — **E.164** international format (`^\+[1-9]\d{1,14}$`): `+` + country code + subscriber number, max 15 digits after `+`. Examples: `+221770000000`, `+33612345678`, `+14155551234`.
  - `@EnumValue(enumClass, ignoreCase)` / `EnumValidator` — the received String must match an `enum.name()`.
  - `@DatePattern(pattern)` / `DatePatternValidation` — `LocalDate.parse(value, formatter)`; default pattern `"yyyy-MM-dd"`.
  - `@Uuid` / `UuidValidator` — `UUID.fromString(value)`.
- All skip `null`/empty (combine with `@NotBlank`/`@NotNull` if required). Each validator has a parameterized test.
- i18n keys: `validation.phone.invalid`, `validation.enum.invalid`, `validation.date.pattern.invalid`, `validation.uuid.invalid`.

### Cross-cutting helpers (`common/tools/`)

- `DateHelper`: `parseStartOfDay(String) → LocalDateTime`, `parseEndOfDay(String) → LocalDateTime`, `format(LocalDateTime) → String "yyyy-MM-dd HH:mm:ss"`, `format(LocalDate) → String "yyyy-MM-dd"`.
- `EnumHelper.parse(Class<E>, String) → E` (null if null/blank).
- `UuidHelper.parse(String) → UUID` (null if null/blank).
- Use them for any String ↔ Date / Enum / UUID conversion in `Filter` DTOs (helper methods `xxxAsEnum()`, `fromDateTime()`, etc.) and in `Response` DTOs (date fields serialized as String).

### Multi-tenant

- On every business operation, scope on `getCurrentEntrepriseId()` / `getCurrentMagasinId()` (via `UserPrincipal` in the `SecurityContext`).
- ⚠️ No Hibernate `@Filter` or global interceptor yet — services must filter explicitly for now.

### Audit

- `AuditableEntity` uses `@CreatedDate / @LastModifiedDate / @CreatedBy / @LastModifiedBy`.
- `@EnableJpaAuditing` is active on `StoreApplication`.
- `AuditorAwareImpl` (`org.store.config`) — reads the `UserPrincipal` from the `SecurityContext` and returns `userId.toString()`; `Optional.empty()` if no authentication.

### Persistence / migrations

- **Flyway** enabled on all profiles. Driver `flyway-database-postgresql`.
- Dependency: `org.springframework.boot:spring-boot-starter-flyway` (Spring Boot 4 removed Flyway's autoconfig from `spring-boot-autoconfigure`).
- `ddl-auto: validate` — Hibernate verifies the schema matches the entities, creates nothing.
- `baseline-on-migrate: true`, `baseline-version: 1` — for historical DBs populated by `ddl-auto: update`, Flyway silently baselines and does not apply V1.
- Scripts: `src/main/resources/db/migration/V<n>__<description>.sql` (Flyway standard naming).

### Security / JWT

- **Externalized configuration** in `application.yml` under `security.jwt` (secret, header, prefix, access/refresh expiration) → bound via the record `org.store.property.JwtProperties` (`@ConfigurationProperties` + `@ConfigurationPropertiesScan` on `StoreApplication`).
- **`JwtService`** (`security/application/service/`) — `generateToken(UserPrincipal)`, `isTokenValid(String)`, `extractUserPrincipal(String)`. jjwt 0.11.5, HS512.
- **`JwtAuthenticationFilter`** (`security/config/`) — extracts the Bearer (via `JwtProperties.header()`/`prefix()`), validates, sets a `UserPrincipal` in the `SecurityContext`. Authorities = `UserPrincipal.permissions()`.
- **`UserDetailsServiceImpl`** — `loadUserByUsername` via the port `AccountRepository.findByUsername`, authorities = `Role.permissions.code`. `@Transactional(readOnly = true)` to initialize lazy collections.
- **`UserPrincipal` (record)** — `userId / entrepriseId / magasinId / username / role / permissions`. Stored in `Authentication.principal`. Overload `hasPermission(PermissionCode)` to avoid strings.
- **JWT claim keys** centralized in the enum `org.store.security.application.enums.Claim` (`ENTREPRISE`, `MAGASIN`, `ROLE`, `USERNAME`, `PERMISSIONS`) — always use `Claim.X.getKey()`, never a literal string.
- **Permission codes** centralized in the enum `org.store.security.application.enums.PermissionCode` (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, …). The DB code = `enum.name()`.
- **`ICurrentUserService`** (`security/application/service/`) — reads the `UserPrincipal` from the `SecurityContextHolder`. Throws `UnauthorisedException("auth.current.missing")` if auth is missing/anonymous/wrong type. **Prefer injecting this** over `@AuthenticationPrincipal UserPrincipal` in the controller (easier to test).
- **`SecurityConfig`**: `/auth/**` permitAll, the rest authenticated, sessions stateless, JWT filter before `UsernamePasswordAuthenticationFilter`.
- ⚠️ The filter does **not** re-check the account status (`enabled`/`locked`) on each request — it trusts the signed JWT.

### Internationalization (i18n)

- **Supported languages**: `fr` (default), `en`. Per-request locale resolved via the `Accept-Language` header (`AcceptHeaderLocaleResolver`).
- **Config**: `org.store.config.I18nConfig` — beans `MessageSource` (`ReloadableResourceBundleMessageSource`, basename `classpath:messages`, UTF-8, `useCodeAsDefaultMessage=true`, default `Locale.FRENCH`), `LocaleResolver`, and `LocalValidatorFactoryBean` wired to the `MessageSource` (→ translated Bean Validation errors).
- **Files**: `src/main/resources/messages.properties` (FR default, no suffix) and `messages_en.properties` (EN). Content grouped in sections: application messages (`entity.*`, `validation.*`, `error.*`…) then Bean Validation keys (`jakarta.validation.constraints.*.message`).
- **Resolution service**: `org.store.common.i18n.IMessageSourceService` (4 overloads: `getMessage(code)`, `getMessage(code, args)`, `getMessage(code, classType)`, `getMessage(code, args, locale)`) + impl `MessageSourceServiceImpl`. Inject in any application service or handler that needs a localized message. For jobs/emails outside the HTTP context, use the overload with explicit `Locale` (otherwise `LocaleContextHolder` may be empty).
- **Localized exceptions**: `LocalizedRuntimeException` (abstract, in `common/exceptions/`) carries `messageKey` + `args`. **All custom exceptions extend this class** (except `AuthentificationException` which extends Spring Security's). Single constructor: `(String messageKey, Object... args)`. The `RestControllerAdvice` `GlobalException` injects `IMessageSourceService` and resolves via a `resolve(LocalizedRuntimeException)` helper.
- **When throwing**: pass an i18n key + args (`throw new EntityException("entity.notFound", id)`). If the key does not exist in the `.properties` files, it is returned as-is (`useCodeAsDefaultMessage` fallback), so a hardcoded raw message remains acceptable during transition.
- **Key naming convention**: prefix by domain — `entity.<verb>` (`entity.notFound`), `validation.<type>` (`validation.error`), `error.<type>` (`error.unexpected`), `auth.<type>`, etc.

---

## Use case documentation

The summary (input, flow, rules, exceptions, output) of every business application service lives in **`store/FEATURES.md`** at the backend root. Keep it up to date with every new service.

---

## Cross-cutting coding rules (recap — apply on every addition)

### Layers and injection

1. **Repository → DomainService → ServiceImpl**: a `<X>ServiceImpl` NEVER injects a repository — it goes through its `<X>DomainService`. For another aggregate, it goes through the `I<Y>Service` interface (never `<Y>Repository`, never `<Y>DomainService` cross-aggregate).
2. **DomainService returns `Optional<E>`** for custom queries; the app service throws the appropriate business exception.
3. **Every custom query** (`findByXxx`, `existsByXxx`, projection) is exposed as a public method on the owning DomainService, then optionally fronted by `I<X>Service`.
4. **Reusable code** (extract, transform, compute) shared between services → public method on the aggregate's owning service. Never a duplicated private helper across files.

### DTO and mapping

5. **`<X>Request`** (input) with mandatory `Request` suffix, never `Info`/`Dto`/`Form`. Placed in `<module>/application/dto/`.
6. **`<X>Response`** (output) with mandatory `Response` suffix. **Must expose a secondary constructor `(<X> entity)`** that extracts fields from the entity (centralizes mapping, removes duplication).
7. **Method parameter of DTO type**: name = full camelCase of the type (`AccountRequest accountRequest`), no generic `info`/`request`/`dto`.
8. **Mandatory FK fields in a Request**: `@NotNull UUID xxxId` (Jackson handles UUID parsing, returns 400 on invalid format).

### Permissions, roles, security

9. **`PermissionCode` enum** centralizes codes (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, …). DB code = `enum.name()`. No more permission strings in application code.
10. **No hardcoded role labels** in app services (`"MANAGER"`, `"VENDEUR"`, …). Rules are expressed through **role permissions**: adding `CAISSIER` tomorrow = adding an `ensureRole(...)` in `DataInitializer`, **zero application change**.
11. **`UserPrincipal`**: `userId / entrepriseId / magasinId / username / role (label) / permissions`. Overload `hasPermission(PermissionCode)`. Fetched via **`ICurrentUserService.getCurrent()`** in business services (not `@AuthenticationPrincipal` on the controller — easier to test).
12. **No duplicate `@PreAuthorize`**: if the controller filters by `hasAuthority(X)`, the service does not re-check X. It focuses on the fine business rules.

### Bean Validation

13. **Custom validators** in `org.store.common.validation/`: `@Phone`, `@EnumValue`, `@DatePattern`, `@Uuid`. Pattern: annotation + `<Name>Validation` class implementing `ConstraintValidator<X, String>`. Skip `null`/empty (combine with `@NotBlank`/`@NotNull` if required). Each validator has its own i18n key + parameterized test.

### i18n

14. **All error messages** go through an i18n key (FR + EN in `messages*.properties`). No hardcoded French/English message in exceptions or throws. For exceptions outside `LocalizedRuntimeException` (Spring Security's `UsernameNotFoundException`, e.g.), resolve via `IMessageSourceService.getMessage(key, args)`.
15. **Key naming convention**: `<domain>.<verb>` or `<domain>.<sub-case>` (`entity.notFound`, `validation.phone.invalid`, `employe.create.elevatedRole.forbidden`, `magasin.notOwned`).

### Tests

16. **Every application service** has an `<X>ImplTest` (JUnit 5 + Mockito + AssertJ). Happy path + error case.
17. **Every controller** has an `<X>ControllerTest` (`MockMvcBuilders.standaloneSetup()` in SB 4 — `@WebMvcTest` requires `spring-boot-starter-webmvc-test`).
18. **Mock the service interface** in tests of other services (never directly mock DomainServices internal to other aggregates — they should not be injected there).

### Naming

19. **Controller**: `public static final String BASE_PATH = "/api/v1/<scope>"` + `@RequestMapping(BASE_PATH)`. No `@PathVariable("version")`.
20. **Application service**: interface `I<X>Service` + impl `<X>ServiceImpl`. **The impl lives in `<module>/application/service/impl/`** (cross-cutting consistency). Inject via the interface.
21. **`<Entity>JpaRepository extends JpaRepository, <Entity>Repository`**; `<Entity>Repository extends BaseRepository<E>` (pure port).

### Minimal controllers

22. **No business logic in the controller**: handler = `return service.method(request)`. Forbidden: entity casts, `new <X>Response(...)`, getter-chains on an aggregate, multi-service orchestration, business branches. If code "overflows", create a public method in `I<X>Service` that returns the final `<X>Response`. Pick the service that **already orchestrates** the chain to avoid DI cycles (seen: `IRegisterPropertyService.registerEntrepriseByAdmin` rather than `IEntrepriseService.createByAdmin`).

### DTO Response — sub-DTO

23. **Sub-Response DTO for a sub-entity**: if a `<X>Response` aggregates **≥ 3 fields** of another reusable entity (User, Store, Company…), create a dedicated `<Y>Response` (`AccountResponse(..., UserResponse user)`) instead of flattening fields. The sub-DTO follows the **Response(Entity)** rule. Hierarchical JSON, DRY, evolutive.

### JPQL Query — projection

24. **`SELECT new <X>Response(entity)` in `@Query`**: JPQL projection via the Response's secondary constructor, never the field list (DRY, no desync when adding a field). Prerequisite: Response(Entity) rule applied.
25. **All JPQL/SQL `@Query` in text block `"""..."""`**, never string concat.

### Domain — entity creation

26. **`create()` lives in the DomainService**: construction (`new` + setters + `save`) in `<X>DomainService.create(...)`. The app service delegates + applies business rules (permissions, scoping, exceptions). Separates "persistence" (domain) from "orchestration" (application).

### No private methods in application services

27. **No private methods** in `<X>ServiceImpl`. Any factoring becomes a public method of `I<X>Service`, **parameterized rather than specialized** (e.g., `createAccount(request, String roleName)` instead of `doCreate(request)` that hardcoded `ROLE_PROPRIETAIRE`). Tolerated exceptions: trivial helpers on DTO or utility object (not a business service).

### Strategy pattern for dispatch by subtype

28. **No `if (x instanceof A) ... else if (x instanceof B) ...`** in business dispatch code. Pattern: interface `<X>Strategy { Class<? extends Parent> targetType(); <Y> resolve(Parent x); }` + impls `@Component` + Spring injects `List<Strategy>` + dispatch `most-specific-wins`:
    ```java
    strategies.stream()
        .filter(s -> s.targetType().isInstance(x))
        .reduce((a, b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)
        .map(s -> s.resolve(x))
        .orElseGet(<X>Result::empty);
    ```
    The subclass wins over the superclass → the strategy on `Parent` is the fallback. Example: `security/application/strategies/UserPrincipalContextStrategy` with Proprietaire/Employe/Utilisateur impls.

### Documentation of application services (business process)

29. **Every application service (`<X>ServiceImpl`) must carry a concise Javadoc**. Strict format:
    - **Class Javadoc**: 1 sentence announcing the service's responsibility (+ 1 sentence if there's a particular guarantee, e.g. "Additive strategy").
    - **Javadoc on each method** (public AND private): 1 sentence announcing **what the method does**, period. No enumeration of input/output/exceptions — the signature speaks.
    - **No comments inside method bodies**. If code requires inline explanation, it lacks a better variable/function name, or it needs a refactor. Identifiers must speak for themselves.
    - **Language**: English for Javadocs, English for logs/identifiers (project consistency).
    - **Reference**: see `RolesPermissionsSyncServiceImpl` as the model.

    Intent: the doc explains the **what** at a glance (Javadoc), the code explains the **how** without visual noise. `<X>DomainService` and controllers stay free (self-documenting by convention).

### Max 3 parameters per method

30. **Every method (public or private) must have at most 3 parameters**. Beyond that: group into a dedicated record (`<X>Command`, `<X>Filter`, or existing DTO).
    - For **business services** doing a filtered listing: take a record `<X>Filter` as a single parameter, validate via `ValidatorService.validate(filter)`, and apply scoping by building an effective filter.
    - The record `<X>Filter` may carry: criteria + `@Min(0) int page` + `@Min(1) int size` + helper methods (`toPageable()`, `xxxAsEnum()`, `fromDateTime()`, etc.).
    - **Explicit exemption**: **Spring Data repositories** keep their `@Query` with individual params (`@Param`), or use SpEL `:#{#filter.X}` to pass a record. Domain services destructure the filter.
    - **Spring controllers**: individual `@RequestParam` + local construction of the `<X>Filter` before calling the service.

### Indentation + documentation of multi-process methods

31. **Every method that chains several processes / logical steps** must:
    - **Be indented by blocks**: separate steps with blank lines to make the sequence visible at a glance.
    - **Be documented**: concise Javadoc (1-3 sentences) announcing what the method does, without enumerating input/output/exceptions.
    - **No inline comments** — indentation + Javadoc structure the method.
    - A trivial method (1-3 lines, single process) needs neither indentation nor mandatory Javadoc.
    - **Reference**: see `StockDomainService.createOrUpdateEntry` and `EntreeStockServiceImpl.create`.

### Explicit variables

32. **Every variable, parameter, field AND alias must carry an explicit business name**. No single-letter names, no cryptic abbreviations (`q`, `c`, `m`, `ent`, `f`, `dto`, `obj`). This rule also applies to **JPQL/SQL aliases** in `@Query` (`FROM Client client`, not `FROM Client c`).
    - **Method parameters**: full name describing the role (`String searchTerm`, not `String q`; `Magasin attachedMagasin`, not `Magasin m`; `Entreprise entreprise`, not `Entreprise ent`).
    - **Local variables**: same rule (`Client client`, `Fournisseur fournisseur`, `Client foreignClient` for a client not owned by the caller).
    - **HTTP `@RequestParam`**: the **external value** (`value = "q"`) may stay short (REST convention), but the **internal Java variable** must be explicit (`String searchTerm`).
    - **JPQL/SQL `@Param`**: must match the Java variable name on the repo side (`@Param("searchTerm") String searchTerm`).
    - **JPQL/SQL aliases**: explicit business name (`FROM Client client`, `LEFT JOIN client.commandes commande`, `FROM PaiementVente paiement`). No more single letters (`c`, `f`, `a`, `u`, `p`, `m`, `s`, `e`, `l`) or abbreviations (`pf`, `pv`). Reference: 24 project repositories migrated in `refactor: reverse relation CommandeVente.facture + explicit JPQL aliases on all repositories`.
    - **Tests**: factory `sample(...)` accepts an explicit parameter (`Magasin attachedMagasin`) and instantiates a named variable (`Client client = new Client()`, not `Client c = new Client()`).
    - **Tolerated exceptions**: trivial Stream lambdas (`stream.map(item -> item.getX())`), loop index `i`, and standardized third-party API names (`e` for `Exception` in a catch).

### Filter DTO from 2 criteria

33. **As soon as a listing/search endpoint has at least 2 filter criteria**, create a dedicated record `<X>Filter` (reinforces rule 30, which only mandates the record beyond 3 parameters).
    - **2+ criteria** = create `<X>Filter` (record in `<module>/application/dto/`). A single criterion alone = individual parameter allowed.
    - **Structure**: validated criteria (`@Min`, `@DatePattern`, `@EnumValue`, `@Uuid`, `@NotNull`) + `@Min(0) int page` + `@Min(1) int size` + `toPageable()` + accessors if transformation is needed (`xxxAsEnum()`, `fromDate()`, etc.).
    - **Application service**: signature `findX(<X>Filter filter)` or `findX(UUID ownerId, <X>Filter filter)`. `validatorService.validate(filter)` as the first line of the method.
    - **Controller**: individual `@RequestParam` with `required = false` and `defaultValue` for page/size, local construction of the Filter before calling the service.
    - **Spring Data repo**: exemption — individual params via `@Param` or SpEL `:#{#filter.X}` allowed beyond 3 parameters.
    - **Null handling**: prefer a JPQL condition `(:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')))` rather than a `normalize()` helper on the service side.
    - **Reference**: `DepenseFilter` + `DepenseController` + `DepenseServiceImpl.findAllByCurrentEntreprise`; `ClientFilter` + `ClientController` + `ClientServiceImpl.findAllForCurrentUser`.

### Streams by default + extract repeated calls

34. **Always use streams (`forEach`, `map`, `filter`, `reduce`, etc.) by default to iterate a collection**. An indexed `for (int i = 0; i < n; i++)` loop is allowed **only when performance requires it** (very large collections, measured hot path, mandatory random access, etc.). If you use `for(int i)`, justify it in a comment.
    - **Simple iteration (side-effects)**: `forEach`.
      ```java
      lignes.forEach(ligne -> {
          ...
      });
      ```
    - **Transformation / aggregation**: `stream().map(...).reduce(...)` or `collect(...)`.
      ```java
      BigDecimal total = lignes.stream()
              .map(ligne -> ligne.prixAchat().multiply(BigDecimal.valueOf(ligne.quantite())))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      ```
    - **Two parallel collections**: `forEach` (or `stream`) on one + synchronized `Iterator` on the other. Coupling is guaranteed by construction.
      ```java
      Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();
      lignes.forEach(ligne -> {
          ProductFournisseur productFournisseur = productFournisseurIterator.next();
          ...
      });
      ```
    - **Benefits**: clear intent (transformation vs side-effects), no stray index, shorter, parallelizable if ever needed.

35. **Any method call or getter chain that is repeated inside the same method (and whose value doesn't change between calls) must be extracted into a local variable with an explicit name**.
    - **`request.lignes()`** called several times? Capture at the start of the method: `List<LigneAchatRequest> lignes = request.lignes();`.
    - **Getter chain** (`commande.getMagasin().getEntreprise().getId()`) repeated: extract into a variable on first use.
    - **Benefits**: readability (the variable name says what we're manipulating), one single call, easier debugging.
    - **Variable name**: follows rule 32 (explicit, business — `lignes`, `entrepriseId`, not `l`/`id`).
    - **Exception**: a call used once stays inline.
    - **Reference**: `AchatServiceImpl.createLignesAndComputeTotal` + `createEntriesAndUpdateStock`.

### Long loop body → dedicated method

36. **Any loop body (`forEach`, `for`, `while`, `stream`) that exceeds a few lines or chains several logical steps must be extracted into a dedicated method**. The loop then contains only a delegation call.
    - **Pragmatic threshold**: if the body is more than ~3 lines or runs several distinct business steps (create + read + update + journal, etc.), extract.
    - **Clean loop**:
      ```java
      lignes.forEach(ligne -> processPurchaseLineEntry(context, ligne, productFournisseurIterator.next()));
      ```
    - **Extracted method**: explicit business name (rule 32), ≤ 3 parameters (rule 30 — group into a record if needed), concise Javadoc (rule 29), indented blocks (rule 31). Public in the class (but not necessarily on the interface) — consistent with rule 27.
    - **Benefits**: the loop reads like a table of contents, each business step is isolated and testable, debugging jumps straight into the unit logic.
    - **Reference**: `AchatServiceImpl.processPurchaseLineEntry` (factored out of `createEntriesAndUpdateStock`).

### Blank lines before and after a stream block

37. **Every multi-line stream expression (chained `.stream().map()...`, `forEach(...)`, `findByX().orElseGet(...)`, etc.) must be surrounded by a blank line before AND a blank line after**, to visually isolate the transformation/iteration block.
    - **Before**:
      ```java
      Set<Permissions> current = role.getPermissions();
      long ajouts = roleDef.permissions().stream()
              .map(code -> resolvePermissionFromCatalog(roleDef, code, context))
              .filter(required -> attachPermissionIfAbsent(current, required))
              .count();
      return ajouts > 0;
      ```
    - **After**:
      ```java
      Set<Permissions> current = role.getPermissions();

      long ajouts = roleDef.permissions().stream()
              .map(code -> resolvePermissionFromCatalog(roleDef, code, context))
              .filter(required -> attachPermissionIfAbsent(current, required))
              .count();

      return ajouts > 0;
      ```
    - **Exceptions**:
      - First statement of the method after `{`: no blank line before (right after `{`).
      - Last statement before `}`: no blank line after (right before `}`).
      - Inline single-line stream (`list.forEach(System.out::println);`): no forced separation.
    - **Consistent with rule 31** (block indentation with blank lines between logical steps).

### Externalizing fixed values

38. **Any tunable business value must be externalized via `@ConfigurationProperties` (records in `org.store.property`)**, never hardcoded (`private static final int TRIAL_DAYS = 30;` forbidden for business values).
    - **Concerned**: durations (trial, expiration, refresh), thresholds, limits (max files, retry, batch size), external URLs, business parameters.
    - **Not concerned**: pure mathematical constants (`HUNDRED`, `SCALE` BigDecimal, divisions by 2, `Math.PI`), technical constants without possible variation (HTTP header `Authorization`, format regex), closed-domain values (enum.name()).
    - **Pattern**: record `<X>Properties` in `org.store.property/`, kebab-case prefix in `application.yml`, override possible via env var `${MY_VAR:defaultValue}`. Example:
      ```java
      // org.store.property.SubscriptionProperties
      @ConfigurationProperties(prefix = "subscription")
      public record SubscriptionProperties(int trialDays) {}
      ```
      ```yaml
      # application.yml
      subscription:
        trial-days: ${SUBSCRIPTION_TRIAL_DAYS:30}
      ```
    - **Injection**: by constructor in the services that need it (`subscriptionProperties.trialDays()`).
    - **Test**: `@Mock SubscriptionProperties subscriptionProperties` + `when(subscriptionProperties.trialDays()).thenReturn(30)`.
    - **`@ConfigurationPropertiesScan`**: already enabled on `StoreApplication` for the `org.store.property` package. New records are auto-discovered.
    - **Reference**: `JwtProperties`, `RbacProperties`, `UploadProperties`, `SubscriptionProperties`.

### Code smells — always extract into a shared function

39. **Code smells are mandatory to address. Any duplicated logic — whether the IDE flags it, a reviewer spots it, or you notice it yourself — must be extracted into a shared external function.** No "I'll dedupe later", no "it's only twice so it's fine". The fix is always: pull the pattern out, name it, call it from both sides. **No exception.**
    - **What counts as duplication**: same statement sequence in two methods (even with different types, if a generic / parameter would cover it); identical `onSuccess`/`onError` boilerplate across mutation handlers; repeated entity-mapping snippets; copy-pasted validation, formatting, or guard blocks.
    - **Where the extracted function lives**:
      - Inside the same aggregate → public method on the owning `<X>DomainService` or `<X>ServiceImpl` (cf. rule 4).
      - Cross-aggregate / cross-module utility → `org.store.common.service/` (e.g. `MutationToastService` analogue, `IdResolver`, etc.).
      - Pure value object / converter without side effects → `org.store.common.util/`.
    - **Naming**: explicit business name (cf. rule 32). Never `helper`, `util`, `doStuff`. The function name must describe *what the duplication did*, not where it was copied from.
    - **Tests**: the new function is itself unit-tested (cf. rule 16). The call sites stop testing the extracted logic — they only assert the wiring.
    - **Reason**: duplication is the #1 source of bugs that only show up after a partial fix lands on one copy. Extraction makes the next change land in one place.
    - **Mirror rule frontend**: `FRONTEND_CODING_CONVENTIONS.md` rule 45 — same meta-rule, applied to React/TS code (handlers, hooks, util modules).

### Created-date filter + ORDER BY createdAt DESC on every CRUD list

40. **Every `<X>Filter` record backing a CRUD list endpoint must expose two optional date fields — `LocalDate createdStartDate` + `LocalDate createdEndDate` — and the matching JPQL query must explicitly `ORDER BY entity.createdAt DESC` in the SELECT.** The pageable carries only `page` + `size` ; sort lives in the JPQL, not in `Sort.by(...)` on `toPageable()`.

    - **Filter record shape**:
      ```java
      public record <X>Filter(
              /* existing business criteria */,
              LocalDate createdStartDate,
              LocalDate createdEndDate,
              @Min(0) int page,
              @Min(1) int size
      ) {
          public Pageable toPageable() {
              return PageRequest.of(page, size);
          }
      }
      ```
    - **JPQL pattern**:
      ```java
      @Query(value = """
              SELECT new ...Response(entity)
              FROM Entity entity
              WHERE /* business filters */
                AND (:#{#filter.createdStartDate} IS NULL OR entity.createdAt >= :#{#filter.createdStartDate})
                AND (:#{#filter.createdEndDate}   IS NULL OR entity.createdAt < :#{#filter.createdEndDate.plusDays(1)})
              ORDER BY entity.createdAt DESC
              """,
             countQuery = """ /* same WHERE, no ORDER BY */ """)
      Page<...Response> findResponsesByFilter(@Param("filter") <X>Filter filter, Pageable pageable);
      ```
    - **Controller**:
      ```java
      public ResponseEntity<Page<...>> list(@RequestParam(required = false) /* business params */,
                                            @RequestParam(required = false) LocalDate createdStartDate,
                                            @RequestParam(required = false) LocalDate createdEndDate,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) { ... }
      ```
    - **Inclusive end-of-day semantics**: `createdEndDate` is interpreted as "every row created on or before that day" — JPQL uses `< createdEndDate + 1 day` so the comparison stays inclusive on the entered date without bringing time-of-day into the API.

    - **Why ORDER BY in JPQL, not via `Sort.by(...)` on the Pageable**:
      - The sort is part of the contract (deterministic listing for the UI) — it belongs to the query definition, not to a runtime call-site.
      - Avoids surprises when the SpEL filter has a `JOIN FETCH` (Hibernate sometimes refuses `Sort.by` on joined column).
      - Makes the query self-contained for tooling / EXPLAIN / DBA review.

    - **Scope**: CRUD list endpoints (`/api/v1/<resource>` GET that returns a `Page<...Response>`). Specialized analytics endpoints with their own time dimension (`/reports/margins`, `/stocks/expiring-lots`, `/ventes/caisse/*`) keep their domain date — the createdAt filter is **additive** and applies to *every* CRUD list.

    - **Mirror rule frontend**: `FRONTEND_CODING_CONVENTIONS.md` rule 48 — every CRUD list filter UI exposes a `<DateRangeFilter />` ("Du" / "Au") wired to these two backend params.

### i18n messages — never interpolate raw IDs

User-facing i18n messages (`messages.properties` + EN counterpart) must never include a UUID or DB id in the placeholder. If the message takes `{0}`, fill it with a human label (`nom`, `libelle`, `username`, …) at the call site — never `entity.getId()`.

❌ Bad:
```java
// message: magasin.notFound = Magasin "{0}" introuvable
.orElseThrow(() -> new EntityException("magasin.notFound", id));   // {0} = UUID
```

✅ Good (two options):
```java
// Option A — drop the placeholder, rephrase:
// message: magasin.notFound = Magasin introuvable
.orElseThrow(() -> new EntityException("magasin.notFound"));

// Option B — keep the placeholder but pass a name. Only possible when
// you already have the entity loaded (e.g. you re-throw after a sanity check):
throw new EntityException("magasin.notFound", magasin.getNom());
```

**Why:** an end-user (the merchant) seeing `Magasin "1e35bbba-…" introuvable` in a toast is bad UX and a trust killer. The mirror rule lives in `FRONTEND_CODING_CONVENTIONS.md` (rule 43).

**Scope:** every `Localized*Exception` constructor call across the codebase. When in doubt: read the corresponding `messages.properties` line and ask "what does the merchant see?" — if any arg is an id, fix it.

**Already-correct precedents:** `utilisateur.email.alreadyExists` is called with the email string (user input → safe). `fournisseur.reference.alreadyExists` is called with the reference (business code → safe). It's specifically `xxx.notFound`-style messages on `findById(...).orElseThrow(...)` chains that tend to leak ids.

### Business-semantic filters live in JPQL, never in the client
41. **Any filter rooted in business semantics — visibility per role, soft-delete masking, tenant scoping, "subscribable plans = plans with ≥ 1 active non-trial type", etc. — must be implemented as a dedicated JPQL repository method on the backend.** The frontend never re-filters an already-fetched list with `array.filter(...)` to enforce a business rule.

❌ Bad (frontend):
```ts
// SubscribePage.tsx
const paidPlans = catalog.plans.filter((plan) => plan.prix > 0)
```

✅ Good (backend):
```java
// PlanAbonnementRepository.java
@Query("""
    SELECT new ...PublicPlanResponse(...)
    FROM PlanAbonnement plan
    WHERE plan.actif = true AND plan.visible = true
      AND EXISTS (
        SELECT 1 FROM TypePlanAbonnement t
        WHERE t.plan = plan AND t.actif = true AND t.trial = false
      )
    ORDER BY plan.ordre ASC, plan.nom ASC
    """)
List<PublicPlanResponse> findSubscribableResponses();
```

**Why:** filtering on the client (a) leaks data the user shouldn't see over the wire, (b) breaks pagination/counts, (c) couples UI to invariants the server should own, (d) silently regresses if another client (mobile, integration partner) forgets the filter. Doing it in JPQL keeps the rule central and enforceable.

**How to apply:**
- Add a new repository method with its own JPQL — never overload an existing one with optional `?` params if the semantics differ ("exclude trial plans" is a distinct intent from "list all visible plans").
- Expose a dedicated endpoint when the filter is permission-bound (e.g. `/api/v1/catalog/subscribable` gated `SUBSCRIPTION_CREATE` ≠ `/api/v1/catalog/public` permitAll).
- Gate the endpoint via `@PreAuthorize` so URL access is enforced too.
- Frontend: add the matching adapter method + hook, then call it from the page. No `.filter(...)` over the generic catalog.

**Mirror on the frontend:** rule 49 in `FRONTEND_CODING_CONVENTIONS.md`.

### 42. Each endpoint must use its own atomic permission — never a module gate as the sole guard

**Every `@PreAuthorize` annotation must reference a dedicated operation-level permission code, not a broad module access gate like `ADMIN_ACCESS`, `OWNER_ACCESS`, or `EMPLOYE_ACCESS`.**

Module access gates (`ADMIN_ACCESS`, `OWNER_ACCESS`, `EMPLOYE_ACCESS`, `ENTREPRISE_ACCESS`) are **entry tickets** that guard the sidebar section and the module shell (`PermissionGuard`). They must not be the sole guard on individual endpoints — that would give any admin/owner carte blanche on every operation in the module.

❌ Bad:
```java
@GetMapping
@PreAuthorize("hasAuthority('ADMIN_ACCESS')")         // too broad — every admin op uses the same gate
public ResponseEntity<Page<EntrepriseResponse>> list(...) { ... }

@PostMapping
@PreAuthorize("hasAuthority('ADMIN_ACCESS')")         // create and list share the same permission
public ResponseEntity<EntrepriseResponse> create(...) { ... }
```

✅ Good:
```java
@GetMapping
@PreAuthorize("hasAuthority('COMPANY_READ')")         // list = read
public ResponseEntity<Page<EntrepriseResponse>> list(...) { ... }

@PostMapping
@PreAuthorize("hasAuthority('COMPANY_CREATE')")       // create has its own code
public ResponseEntity<EntrepriseResponse> create(...) { ... }

@PutMapping("/{id}")
@PreAuthorize("hasAuthority('COMPANY_UPDATE')")       // update has its own code
public ResponseEntity<EntrepriseResponse> update(...) { ... }
```

**Naming convention for operation-level codes**: `<DOMAIN>_<VERB>` where VERB ∈ `{READ, CREATE, UPDATE, DELETE}` for standard CRUD. Non-CRUD verbs are allowed when the operation has clear distinct semantics (`LOCK`, `UNLOCK`, `ASSIGN_ROLE`, `RESET_PASSWORD`, `EXPORT`, `UPLOAD_IMAGE`, etc.).

**Steps when adding a new endpoint:**
1. Identify the operation type (READ / CREATE / UPDATE / DELETE / special).
2. Check `PermissionCode` for an existing code that matches the semantic. Reuse it if it fits.
3. If no matching code exists, add a new entry to `PermissionCode` + the global `permissions:` list in `roles-permissions.yml` + assign to relevant roles.
4. Put `@PreAuthorize("hasAuthority('NEW_CODE')")` on the endpoint.
5. Add to the `BACKEND_CODING_CONVENTIONS.md` if it introduces a new pattern.

**Why:** module gates are coarse-grained by design. Atomic codes let you grant `COMPANY_READ` to a read-only support role without granting `COMPANY_CREATE` or `COMPANY_DELETE`. They also make security audits possible (grep the codebase for who has `COMPANY_DELETE`).

---

## Commit conventions

Project style: direct title (no `feat:`/`fix:` prefix), description of the "why" in the body. **Language: English** (entire repo is English now).

```
<short functional summary>

[optional body: what it does, why, technical notes]
```

Examples:
- `Manual stock entry (stock module — feature 3)`
- `Stock listing (stock module — feature 4)`
- `Update SESSIONS.md and FEATURES.md`

Scope: module name (`vente`, `stock`, `auth`, …) in parentheses if relevant.

**When to commit**: at the end of each validated task (cf. `TODO.md`), with a message describing the "why", not the "what".

**Never** `Co-Authored-By: Claude` in commits. No automatic push without explicit request.
