# CONVENTION_CODAGE_BACKEND.md — Règles de codage du backend

> Conventions et règles obligatoires applicables sur chaque ajout/modification de code **backend**.
> Pour la stack et la structure des packages, voir `ARCHITECTURE.md`. Pour le frontend, voir `CONVENTION_CODAGE_FRONTEND.md`.

---

## Conventions de code

**Classes / fichiers** : `PascalCase.java`
**Méthodes / variables** : `camelCase`
**Constantes** : `UPPER_SNAKE_CASE`
**Tables BD** : `snake_case` via `<Entity>.TABLE_NAME` constant + `@Table(name = TABLE_NAME)`
**Tests Unitaires et Intégrations pour chaque fonctionnalité**
**Chaque Composant ou Service (Class) doit avoir son interface**
**Les méthodes doivent utiliser la projection avec DTO record en utilisant `@Query` afin d'éviter de retourner toutes les données de l'entité**

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
| DTO filtre                    | `<X>Filter`                          | `StockFilter`, `MouvementStockFilter` — record avec validations Jakarta + méthodes utilitaires |
| Paramètre DTO en méthode      | camelCase complet du type            | `AccountRequest accountRequest` (pas `info`/`request` générique — règle obligatoire) |
| Controller                    | `<Entity>Controller`                 | `AuthController` (`public static final String BASE_PATH = "/api/v1/<scope>"` — règle obligatoire) |
| Test service                  | `<X>ServiceImplTest`                 | JUnit 5 + Mockito, mock des deps, chemin nominal + erreurs (règle obligatoire) |
| Test controller               | `<X>ControllerTest`                  | `@WebMvcTest` + MockMvc ou `MockMvcBuilders.standaloneSetup()` (règle obligatoire) |

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
  - Des beans techniques (`PasswordEncoder`, `IJwtService`, `ICurrentUserService`, `IMessageSourceService`, `ValidatorService`, etc.).
- Orchestrent les use cases : règles métier, exceptions i18n (`ForbiddenException`, `EntityException`, `BadArgumentException`), transactions (`@Transactional`).
- **Code réutilisable** entre services : méthode publique du service propriétaire de l'agrégat, jamais helper privé dupliqué.

### Validation Bean Validation + validators custom

- `ValidatorService` (dans `common/service`) wrappe `jakarta.validation.Validator`.
  - `validate(obj, groups…)` lève `ConstraintViolationException`.
  - `check(obj, groups…)` retourne le set de violations.
- **Validateurs custom** dans `org.store.common.validation/` :
  - `@Phone` / `PhoneValidation` — format **E.164** international (`^\+[1-9]\d{1,14}$`) : `+` + code pays + numéro abonné, max 15 chiffres après le `+`. Ex. `+221770000000`, `+33612345678`, `+14155551234`.
  - `@EnumValue(enumClass, ignoreCase)` / `EnumValidator` — la String reçue doit matcher un `enum.name()`.
  - `@DatePattern(pattern)` / `DatePatternValidation` — `LocalDate.parse(value, formatter)` ; pattern par défaut `"yyyy-MM-dd"`.
  - `@Uuid` / `UuidValidator` — `UUID.fromString(value)`.
- Tous skippent `null`/empty (combiner avec `@NotBlank`/`@NotNull` si requis). Chaque validateur a son test paramétré.
- Clés i18n : `validation.phone.invalid`, `validation.enum.invalid`, `validation.date.pattern.invalid`, `validation.uuid.invalid`.

### Helpers transverses (`common/tools/`)

- `DateHelper` : `parseStartOfDay(String) → LocalDateTime`, `parseEndOfDay(String) → LocalDateTime`, `format(LocalDateTime) → String "yyyy-MM-dd HH:mm:ss"`, `format(LocalDate) → String "yyyy-MM-dd"`.
- `EnumHelper.parse(Class<E>, String) → E` (null si null/blank).
- `UuidHelper.parse(String) → UUID` (null si null/blank).
- À utiliser pour toute conversion String ↔ Date / Enum / UUID dans les DTOs Filter (méthodes utilitaires `xxxAsEnum()`, `fromDateTime()`, etc.) et dans les DTOs Response (champs date sérialisés en String).

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
- **Codes de permissions** centralisés dans l'enum `org.store.security.application.enums.PermissionCode` (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, …). Le code en BDD = `enum.name()`.
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

29. **Tout service applicatif (`<X>ServiceImpl`) doit porter une javadoc concise**. Format strict :
    - **Javadoc de classe** : 1 phrase qui annonce la responsabilité du service (+ 1 phrase si garantie particulière, ex. "Stratégie additive").
    - **Javadoc sur chaque méthode** (publique ET privée) : 1 phrase qui annonce **ce que fait la méthode**, point. Pas d'énumération entrée/sortie/exceptions — les types et la signature suffisent.
    - **Aucun commentaire à l'intérieur du corps des méthodes**. Si le code nécessite une explication inline, c'est qu'il manque un meilleur nom de variable / fonction, ou un refactor. Les identifiants doivent parler d'eux-mêmes.
    - **Langue** : français pour les javadocs, anglais pour les logs/identifiants (cohérence projet).
    - **Référence** : voir `RolesPermissionsSyncServiceImpl` comme modèle.

    L'intention : la doc explique le **quoi** au survol (javadoc), le code explique le **comment** sans bruit visuel. Les `<X>DomainService` et controllers restent libres (auto-documentants par convention).

### Max 3 paramètres par méthode

30. **Toute méthode (publique ou privée) doit avoir au maximum 3 paramètres**. Au-delà : regrouper dans un record dédié (`<X>Command`, `<X>Filter`, ou DTO existant).
    - Pour les **services métier** qui font un listing filtré : prendre un record `<X>Filter` en paramètre unique, valider via `ValidatorService.validate(filter)`, et appliquer le scoping en construisant un filter effectif.
    - Le record `<X>Filter` peut porter : critères + `@Min(0) int page` + `@Min(1) int size` + méthodes utilitaires (`toPageable()`, `xxxAsEnum()`, `fromDateTime()`, etc.).
    - **Exemption explicite** : les **repositories Spring Data** gardent leurs queries `@Query` avec params individuels (`@Param`), ou utilisent SpEL `:#{#filter.X}` pour passer un record. Les domain services déstructurent le filter.
    - **Controllers Spring** : `@RequestParam` individuels + construction du `<X>Filter` localement avant l'appel au service.

### Indentation + documentation des méthodes multi-process

31. **Toute méthode qui enchaîne plusieurs process / étapes logiques** doit :
    - **Être indentée par blocs** : séparer les étapes par des sauts de ligne vides pour rendre la séquence visible au survol.
    - **Être documentée** : javadoc concise (1-3 phrases) qui annonce ce que fait la méthode, sans énumérer entrée/sortie/exceptions.
    - **Pas de commentaires inline** — c'est l'indentation + la javadoc qui structurent.
    - Une méthode triviale (1-3 lignes, 1 seul process) n'a besoin ni d'indentation, ni de javadoc obligatoire.
    - **Référence** : voir `StockDomainService.createOrUpdateEntry` et `EntreeStockServiceImpl.create`.

### Variables explicites

32. **Toute variable, paramètre, champ ET alias doit porter un nom métier explicite**. Pas de noms d'une seule lettre ni d'abréviations cryptiques (`q`, `c`, `m`, `ent`, `f`, `dto`, `obj`). Cette règle s'applique aussi aux **alias JPQL/SQL** dans les `@Query` (`FROM Client client` et non `FROM Client c`).
    - **Paramètres de méthode** : nom complet qui décrit le rôle (`String searchTerm`, pas `String q` ; `Magasin attachedMagasin`, pas `Magasin m` ; `Entreprise entreprise`, pas `Entreprise ent`).
    - **Variables locales** : même règle (`Client client`, `Fournisseur fournisseur`, `Client foreignClient` pour un client qui n'appartient pas au caller).
    - **`@RequestParam` HTTP** : la **valeur externe** (`value = "q"`) peut rester courte (convention REST), mais la **variable Java interne** doit être explicite (`String searchTerm`).
    - **`@Param` JPQL/SQL** : doit matcher le nom de la variable Java côté repo (`@Param("searchTerm") String searchTerm`).
    - **Alias JPQL/SQL** : nom métier explicite (`FROM Client client`, `LEFT JOIN client.commandes commande`, `FROM PaiementVente paiement`). Plus aucune lettre seule (`c`, `f`, `a`, `u`, `p`, `m`, `s`, `e`, `l`) ni abréviation (`pf`, `pv`). Référence : 24 repositories du projet migrés en `refactor: relation inverse CommandeVente.facture + alias JPQL explicites sur tous les repositories`.
    - **Tests** : factory `sample(...)` accepte un paramètre explicite (`Magasin attachedMagasin`) et instancie une variable nommée (`Client client = new Client()`, pas `Client c = new Client()`).
    - **Exceptions tolérées** : lambdas Stream triviaux (`stream.map(item -> item.getX())`), index de boucle `i`, et noms standardisés d'API tierces (`e` pour `Exception` dans un catch).

### DTO Filter dès 2 critères

33. **Dès qu'un endpoint de listing/recherche a au moins 2 critères de filtrage**, créer un record `<X>Filter` dédié (renforce la règle 30 qui n'impose le record qu'au-delà de 3 paramètres).
    - **2+ critères** = créer `<X>Filter` (record dans `<module>/application/dto/`). 1 critère seul = paramètre individuel autorisé.
    - **Structure** : critères validés (`@Min`, `@DatePattern`, `@EnumValue`, `@Uuid`, `@NotNull`) + `@Min(0) int page` + `@Min(1) int size` + `toPageable()` + accesseurs si transformation nécessaire (`xxxAsEnum()`, `fromDate()`, etc.).
    - **Service applicatif** : signature `findX(<X>Filter filter)` ou `findX(UUID ownerId, <X>Filter filter)`. `validatorService.validate(filter)` en première ligne de la méthode.
    - **Controller** : `@RequestParam` individuels avec `required = false` et `defaultValue` pour page/size, construction du Filter localement avant l'appel au service.
    - **Repo Spring Data** : exemption — params individuels via `@Param` ou SpEL `:#{#filter.X}` autorisés au-delà de 3 paramètres.
    - **Gestion des valeurs nulles** : préférer une condition JPQL `(:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')))` plutôt qu'un helper `normalize()` côté service.
    - **Référence** : `DepenseFilter` + `DepenseController` + `DepenseServiceImpl.findAllByCurrentEntreprise` ; `ClientFilter` + `ClientController` + `ClientServiceImpl.findAllForCurrentUser`.

### Streams par défaut + extraire les appels répétés

34. **Toujours utiliser les streams (`forEach`, `map`, `filter`, `reduce`, etc.) par défaut pour itérer une collection**. La boucle indexée `for (int i = 0; i < n; i++)` n'est autorisée **que si la performance l'exige** (très grandes collections, hot path mesuré, accès aléatoire indispensable, etc.). Si tu utilises `for(int i)`, justifie-le en commentaire.
    - **Itération simple (side-effects)** : `forEach`.
      ```java
      lignes.forEach(ligne -> {
          ...
      });
      ```
    - **Transformation / agrégation** : `stream().map(...).reduce(...)` ou `collect(...)`.
      ```java
      BigDecimal total = lignes.stream()
              .map(ligne -> ligne.prixAchat().multiply(BigDecimal.valueOf(ligne.quantite())))
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      ```
    - **Deux collections parallèles** : `forEach` (ou `stream`) sur l'une + `Iterator` synchronisé sur l'autre. Le couplage est garanti par construction.
      ```java
      Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();
      lignes.forEach(ligne -> {
          ProductFournisseur productFournisseur = productFournisseurIterator.next();
          ...
      });
      ```
    - **Bénéfices** : intention claire (transformation vs side-effects), absence d'index parasite, plus court, parallélisable si besoin futur.

35. **Tout appel de méthode ou chaînage de getters répété au sein d'une même méthode (et dont la valeur ne change pas entre les appels) doit être extrait dans une variable locale au nom explicite**.
    - **`request.lignes()`** appelé plusieurs fois ? Capter en début de méthode : `List<LigneAchatRequest> lignes = request.lignes();`.
    - **Chaînage de getters** (`commande.getMagasin().getEntreprise().getId()`) répété : extraire dans une variable au premier usage.
    - **Bénéfices** : lisibilité (le nom de variable dit ce qu'on manipule), un seul appel, debug plus facile.
    - **Nom de variable** : suit la règle 32 (explicite, métier — `lignes`, `entrepriseId`, pas `l`/`id`).
    - **Exception** : un appel utilisé une seule fois reste inline.
    - **Référence** : `AchatServiceImpl.createLignesAndComputeTotal` + `createEntriesAndUpdateStock`.

### Corps de boucle long → méthode dédiée

36. **Tout corps de boucle (`forEach`, `for`, `while`, `stream`) qui dépasse quelques lignes ou enchaîne plusieurs étapes logiques doit être extrait dans une méthode dédiée**. La boucle ne contient alors qu'un appel de délégation.
    - **Seuil pragmatique** : si le corps fait plus de ~3 lignes ou exécute plusieurs étapes métier distinctes (création + lecture + update + journal, etc.), extraire.
    - **Boucle propre** :
      ```java
      lignes.forEach(ligne -> processPurchaseLineEntry(context, ligne, productFournisseurIterator.next()));
      ```
    - **Méthode extraite** : nom métier explicite (règle 32), ≤ 3 paramètres (règle 30 — regrouper dans un record si besoin), javadoc concise (règle 29), blocs indentés (règle 31). Publique dans la classe (mais pas forcément sur l'interface) — cohérent avec règle 27.
    - **Bénéfices** : la boucle se lit comme un index, chaque étape métier est isolée et testable, le debug saute directement dans la logique unitaire.
    - **Référence** : `AchatServiceImpl.processPurchaseLineEntry` (corps factorisé depuis `createEntriesAndUpdateStock`).

### Lignes vides avant et après un bloc stream

37. **Toute expression stream multi-ligne (chaîne `.stream().map()...`, `forEach(...)`, `findByX().orElseGet(...)`, etc.) doit être encadrée d'une ligne vide avant ET une ligne vide après**, pour visuellement isoler le bloc de transformation/itération.
    - **Avant** :
      ```java
      Set<Permissions> current = role.getPermissions();
      long ajouts = roleDef.permissions().stream()
              .map(code -> resolvePermissionFromCatalog(roleDef, code, context))
              .filter(required -> attachPermissionIfAbsent(current, required))
              .count();
      return ajouts > 0;
      ```
    - **Après** :
      ```java
      Set<Permissions> current = role.getPermissions();

      long ajouts = roleDef.permissions().stream()
              .map(code -> resolvePermissionFromCatalog(roleDef, code, context))
              .filter(required -> attachPermissionIfAbsent(current, required))
              .count();

      return ajouts > 0;
      ```
    - **Exceptions** :
      - Première instruction de la méthode après `{` : pas de ligne vide avant (juste après le `{`).
      - Dernière instruction avant `}` : pas de ligne vide après (juste avant le `}`).
      - Stream inline d'une seule ligne (`list.forEach(System.out::println);`) : pas de séparation forcée.
    - **Cohérent avec règle 31** (indentation par blocs avec lignes vides entre étapes logiques).

### Externalisation des valeurs fixes

38. **Toute valeur métier paramétrable doit être externalisée via `@ConfigurationProperties` (records dans `org.store.property`)**, jamais codée en dur (`private static final int TRIAL_DAYS = 30;` interdit pour les valeurs métier).
    - **Concerné** : durées (trial, expiration, refresh), seuils, limites (max files, retry, batch size), URLs externes, paramètres business.
    - **Non concerné** : constantes mathématiques pures (`HUNDRED`, `SCALE` BigDecimal, divisions par 2, `Math.PI`), constantes techniques sans variation possible (header HTTP `Authorization`, regex de format), valeurs de domaine fermé (enum.name()).
    - **Pattern** : record `<X>Properties` dans `org.store.property/`, préfixe en kebab-case dans `application.yml`, override possible via variable d'env `${MY_VAR:defaultValue}`. Exemple :
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
    - **Injection** : par constructeur dans les services qui en ont besoin (`subscriptionProperties.trialDays()`).
    - **Test** : `@Mock SubscriptionProperties subscriptionProperties` + `when(subscriptionProperties.trialDays()).thenReturn(30)`.
    - **`@ConfigurationPropertiesScan`** : déjà activé sur `StoreApplication` pour le package `org.store.property`. Les nouveaux records sont auto-découverts.
    - **Référence** : `JwtProperties`, `RbacProperties`, `UploadProperties`, `SubscriptionProperties`.

---

## Conventions de commits

Style projet : titre direct (pas de préfixe `feat:`/`fix:` etc), description du « pourquoi » dans le corps.

```
<résumé court fonctionnel>

[corps optionnel : ce que ça fait, pourquoi, notes techniques]
```

Exemples :
- `Entrée stock manuelle (module stock - fonctionnalité 3)`
- `Consultation stock (module stock - fonctionnalité 4)`
- `Mise à jour SESSIONS.md et FONCTIONNALITIES.md`

Scope : nom du module (`vente`, `stock`, `auth`, …) entre parenthèses si pertinent.

**Quand committer** : à chaque fin de tâche validée (cf. `TODO.md`), avec un message qui décrit le « pourquoi », pas le « quoi ».

**Jamais** `Co-Authored-By: Claude` dans les commits. Pas de push automatique sans demande explicite.
