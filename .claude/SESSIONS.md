# SESSIONS.md — Journal des sessions

> Claude Code remplit ce fichier à la fin de chaque session.
> À relire en priorité au démarrage d'une nouvelle session pour reprendre le contexte immédiatement.

---

## 📌 Dernière session

**Date :** 2026-05-12
**Sujet :** Flyway, validators custom (@Phone, @EnumValue, @DatePattern, @Uuid), `PermissionCode` enum, refactorisation app→domain service partout, création d'employés avec règles propriétaire/manager, `CurrentUserService`, doc `FONCTIONNALITIES.md`

**Ce qui a été fait :**

1. **Migration Flyway** — `spring-boot-starter-flyway` 4.0.6 (autoconfig sortie de `spring-boot-autoconfigure` en SB 4) + `flyway-database-postgresql`. `V1__init_schema.sql` rédigé à la main (~485 lignes, 40 tables, héritage JOINED `person/utilisateur`, table de jointure `role_permission`, FK déclarées en `ALTER TABLE` à la fin). Tous `application*.yml` : `ddl-auto: validate`, `flyway.enabled=true`, `baseline-on-migrate=true`, `baseline-version=1`. Tests : 37 verts.

2. **Mise à jour `RegisterPropertyServiceImpl`** — règle d'isolation des services posée puis renforcée : un `<X>ServiceImpl` n'utilise plus `<Y>Repository` mais `I<Y>Service`. Création de `IRoleService` + `IPlanAbonnementService`. Étendu `IAccountService.findByUsername`. Refactor `LoginServiceImpl` aussi.

3. **`@Phone` validator** — `common/validation/Phone` + `PhoneValidation` (pattern SN `^(70|75|76|77|78|33)\d{7,9}$`). i18n `validation.phone.invalid`. Telephone obligatoire dans `UtilisateurRequest`. `PhoneValidationTest` (13 cas paramétrés).

4. **`@EnumValue`, `@DatePattern`, `@Uuid`** — 3 validateurs additionnels dans `common/validation/`. Chacun avec sa clé i18n et son test paramétré. `@Uuid` valide une String UUID via `UUID.fromString`. `@DatePattern` reste en `ResolverStyle.SMART` (par défaut Java) → "2026-02-30" auto-corrigé en "02-28".

5. **Factorisation complète "app service → domain service"** — 10 `*ServiceImpl` revus pour ne plus injecter de `*Repository` direct. `AccountDomainService.findByUsername`, `RefreshTokenDomainService.findByToken`, `RoleDomainService.findByLibelle`, `PlanAbonnementDomainService.findFirstTrialActif` ajoutés. Convention : DomainService renvoie `Optional<E>`, ServiceImpl throw.

6. **`PermissionCode` enum** — `PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`. Overload `UserPrincipal.hasPermission(PermissionCode)`. `DataInitializer` + `EmployeServiceImpl` utilisent l'enum. Plus aucune chaîne hardcodée éparpillée.

7. **`PermissionsDomainService.findAllByRoleId(UUID) → List<String>`** — JPQL projection `SELECT p.code FROM Role r JOIN r.permissions p WHERE r.id = :roleId`. Évite l'accès LAZY à `role.permissions` hors transaction. Exposé via `IPermissionsService`. Remplace `extractPermissionCodes(Role)` qui vivait sur RoleServiceImpl (supprimé). Utilisé par `UserPrincipalFactoryImpl`, `UserDetailsServiceImpl`, `EmployeServiceImpl`.

8. **`UserPrincipal` enrichi** — champ `String role` (libellé du rôle) ajouté entre `username` et `permissions`. Claim JWT `ROLE("role")` ajouté à l'enum `Claim`. `JwtServiceImpl` sérialise/désérialise le rôle. `UserPrincipalFactoryImpl` peuple via `account.role.libelle`.

9. **`ICurrentUserService` + impl** — récupère le `UserPrincipal` du `SecurityContextHolder`, throw `UnauthorisedException("auth.current.missing")` si absent/anonyme/mauvais type. Remplace `@AuthenticationPrincipal UserPrincipal caller` au niveau controller : l'injection se fait directement dans le service. `CurrentUserServiceImplTest` (4 cas).

10. **Création d'employés** — `POST /api/v1/employees` (`@PreAuthorize("hasAuthority('EMPLOYE_CREATE')")`). DTOs `EmployeRequest` (avec `@NotNull UUID magasinId`, `@NotBlank String role`) et `EmployeResponse` (avec constructeur `EmployeResponse(Employe)`). `EmployeDomainService.create(...)` orchestre l'instanciation + sauvegarde + lien `account.user`. Règles métier :
    - Le rôle demandé doit avoir `EMPLOYE_ACCESS`.
    - Seul un propriétaire (currentUser avec `PROPRIETAIRE_ACCESS`) peut créer un rôle élevé (`EMPLOYE_CREATE`).
    - Un seul rôle élevé par magasin (`EmployeRepository.existsByMagasinIdAndRolePermissionCode`).
    - Propriétaire scopé sur son entreprise, manager scopé sur son magasin.
    - `MANAGER` + `VENDEUR` seedés dans `DataInitializer`.

11. **`FONCTIONNALITIES.md`** créé à la racine du backend — récap des 5 services applicatifs métier (register/login/refresh/logout/employé create), avec entrée, flux numéroté, règles, exceptions, sortie.

12. **Mémoire enrichie** — nouvelles règles : isolation des services (DomainService obligatoire), Response from Entity (constructeur depuis l'entité), code réutilisable = méthode publique du service propriétaire, cd dans `store/` avant tout git, commit sans Co-Authored-By.

**Décisions / arbitrages :**

- **Flyway / Spring Boot 4** : l'autoconfig Flyway a été extraite de `spring-boot-autoconfigure`. Il faut désormais `spring-boot-starter-flyway` (module séparé en SB 4). `baseline-version: 1` pour que les BDD existantes (déjà peuplées par `ddl-auto: update`) considèrent V1 comme déjà appliquée.
- **`@DatePattern`** : volontairement en SMART (auto-correction des dates impossibles). À passer en STRICT (`uuuu-MM-dd`) si besoin.
- **DomainService = couche d'accès** : tous les CRUD passent par le DomainService du module, jamais le repository directement depuis l'app service. Custom queries renvoient `Optional<E>` ; l'app service throw. `UserDetailsServiceImpl` (Spring Security) suit la même règle.
- **`ICurrentUserService`** : remplace l'usage de `@AuthenticationPrincipal` côté controller. Le service métier appelle `currentUserService.getCurrent()` quand il a besoin de l'utilisateur connecté → testable plus simplement (mock du service vs setup SecurityContext + custom argument resolver).
- **Règles employé data-driven** : aucun libellé `"MANAGER"` hardcodé. La règle "seuls les propriétaires créent des rôles élevés" se base sur `rolePermissions.contains(EMPLOYE_CREATE)`. Si demain on crée `CAISSIER` (rôle non‑élevé) ou `SUPER_MANAGER` (rôle élevé), aucun changement applicatif.
- **`UserPrincipal.role`** : libellé String (pas UUID, pas enum) — flexibilité pour ajouter de nouveaux rôles sans toucher le code.
- **EMPLOYE_CREATE check** : déjà fait par `@PreAuthorize` au niveau controller, **ne pas dupliquer** au service.

**Où on s'est arrêté :**

- 100 tests verts (37 → 100). Toute la couche backend auth + création employé compile et tourne sur Postgres avec Flyway.
- Commits dev : `0fd806e` Flyway, `17d2228` MAJ registration propriétaire, `a08ffa2` @Phone, `3362d30` @EnumValue + @DatePattern, `9301cdb` création employé.
- `FONCTIONNALITIES.md` à la racine documente les 5 use cases.
- Plus aucune chaîne hardcodée pour les permissions/rôles côté service.

**Prochaine étape recommandée :**

1. **CRUD `Product`** (premier vrai module métier post-auth, post-employé) — suit les mêmes patterns : `ProductRequest`/`Response` (avec constructeur entité), `IProductService` + `ProductServiceImpl` qui utilise `ProductDomainService`, controller `@PreAuthorize` selon le rôle, tests unit + slice web.
2. **Frontend** : `src/lib/api/client.ts` (axios + intercepteur JWT) + pages `(auth)/login` et `(auth)/register`.
3. **Endpoint de listing des employés** par magasin (pour que le proprietaire/manager voit ses employés).

---

### Session du 2026-05-11

**Sujet :** AuditorAware, refactoring hexagonal complet (35 entités), i18n backend FR/EN, use case d'inscription, endpoints `/auth/*`, extraction module `entreprise`, conventions de code obligatoires

**Ce qui a été fait :**

1. **`AuditorAwareImpl`** — `@Component` qui lit le `UserPrincipal` du `SecurityContext`, renvoie `userId.toString()` ou `Optional.empty()` (anonyme). `createdBy`/`updatedBy` désormais peuplés via `@EnableJpaAuditing`.

2. **Refactoring hexagonal pragmatique complet** sur les 35 entités :
   - ➕ `org.store.common.repository.BaseRepository<E>` (port CRUD générique pur)
   - 35 × port `<Entity>Repository` (interface pure dans `domain/repository/`) + adapter `<Entity>JpaRepository` (Spring Data dans `infrastructure/repository/`)
   - `GlobalService<E, R extends BaseRepository<E>>` typé sur le port (plus de dépendance Spring Data dans le domaine)
   - `EntityException` partout (`findById`/`deleteById`)
   - Faute `infrastructure/repositorty/` corrigée
   - Entités JPA restent dans `domain/model/` (compromis hexagonal pragmatique assumé)

3. **i18n backend FR/EN** en 3 étapes :
   - **A.** `I18nConfig` (`MessageSource` UTF-8, `AcceptHeaderLocaleResolver` default FR/supported [FR, EN], `LocalValidatorFactoryBean` câblé) + `IMessageSourceService` (4 surcharges) + `MessageSourceServiceImpl` dans `common/i18n/`
   - **B.** `LocalizedRuntimeException` (abstract, `messageKey` + `args`) parente des 12 exceptions custom. `GlobalException` (advice) injecte `IMessageSourceService` et résout via helper. Constructeur unique `(messageKey, args...)` ; fallback `useCodeAsDefaultMessage=true` pour rester rétro-compatible avec des messages bruts non répertoriés.
   - **C.** 22 clés Bean Validation Jakarta (`NotNull`, `NotBlank`, `Size`, `Email`, `Min`, `Max`, `Pattern`, `Past/Future`, `Positive/Negative`, `Digits`, `AssertTrue/False`...) ajoutées dans `messages*.properties`.

4. **Use case d'inscription propriétaire** (4 sous-étapes) :
   - **A.** Préparation : `findByLibelle` (Role), `findFirstByTrialTrueAndActifTrue` (PlanAbonnement), `findByCode` (Permissions). PasswordEncoder déjà bean.
   - **B.** `DataInitializer` (`ApplicationRunner`) : seeds idempotents au boot — Permissions (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`), Roles (`PROPRIETAIRE`, `EMPLOYE`), PlanAbonnement "Essai" (gratuit, 1 magasin, 3 employés, 30j).
   - **C.** DTOs (`RegisterPropertyRequest` composite + sous-DTOs `AccountRequest`/`UtilisateurRequest`/`EntrepriseRequest`/`MagasinRequest` répartis dans `<module>/application/dto/`) + services applicatifs par entité (`AccountServiceImpl`, `ProprietaireServiceImpl`, `EntrepriseServiceImpl`, `MagasinServiceImpl`, `AbonnementServiceImpl`) + `RegisterPropertyServiceImpl` orchestrateur (`@Transactional`).
   - **D.** `AuthController` : `POST /api/v1/auth/register` → 201 Created + tokens.

5. **`/auth/login`** : `LoginServiceImpl` injecte `AuthenticationManager`, fait l'auth, reconstruit `UserPrincipal` via `IUserPrincipalFactory` (extrait `entrepriseId`/`magasinId` selon `Proprietaire`/`Employe`/admin nu).

6. **`/auth/refresh`** + **`/auth/logout`** :
   - `IRefreshTokenService` : `create(Account)` génère UUID opaque + persiste (7j), `refresh(String)` valide non révoqué + non expiré et émet un nouvel access token, `revoke(String)` idempotent.
   - `AuthResponse` étendu en `(accessToken, refreshToken)`.
   - Toutes les générations de token passent désormais par `IUserPrincipalFactory` (factorisé entre Login, Register, Refresh).

7. **Extraction du module `entreprise`** : sous-domaine `Entreprise` déplacé de `magasin/` vers son propre module `org.store.entreprise.*` (entité, port, adapter, services, DTO, test). Imports cross-modules ajustés (`Magasin.entreprise`, `Entreprise.magasins`).

8. **Renommage paramètres DTO** : `info` → `<entityCamelCase>Request` (ex. `accountRequest`, `entrepriseRequest`) sur les 8 fichiers `<X>Service` + interfaces.

9. **5 règles obligatoires inscrites en mémoire** (s'appliqueront automatiquement aux futurs développements) :
   - Controller `BASE_PATH = "/api/v1/<scope>"` (version hardcodée) + `@RequestMapping(BASE_PATH)`
   - Chaque service applicatif a une interface `I<X>Service` + impl `<X>ServiceImpl`
   - DTOs : suffixe `<X>Request` / `<X>Response`, jamais `Info`/`Dto` générique
   - Paramètre DTO : nom = camelCase complet du type (`AccountRequest accountRequest`)
   - Tests : `<X>ImplTest` (JUnit5 + Mockito) sur chaque service applicatif, `<X>ControllerTest` sur chaque controller

**Décisions / arbitrages :**

- **Pas d'entité `Administrator`** : un admin SaaS = `Utilisateur` racine + `Account` avec `Role` "ADMIN". Distinction au runtime via `Account.role`.
- **CORS** : déjà configuré via `CorsOriginFilter` (`Allow-Origin: *`, OK pour dev/JWT header, à durcir en prod si cookies httpOnly).
- **Hexagonal pragmatique** : entités JPA restent dans `domain/model/` (pas de POJO purs séparés + mappers — coût disproportionné). Décision 2026-05-09 sur "JpaRepository dans domain/" annulée.
- **i18n exceptions** : un seul constructeur `(messageKey, args)` par exception custom. Grâce à `useCodeAsDefaultMessage=true`, passer un message littéral non répertorié dans `.properties` est retourné tel quel → migration progressive possible.
- **`IMessageSourceService`** (pattern interface/impl) plutôt qu'un wrapper simple — fourni par l'utilisateur, plus complet (4 surcharges + Locale custom + `Class<?>`).
- **Refresh token** : UUID opaque persisté, pas de rotation, pas de blacklist d'access JWT (compromis MVP).
- **`/auth/logout` idempotent** : si refresh token inconnu ou déjà révoqué → 204 (pas de side-channel).
- **Tests controller** : `MockMvcBuilders.standaloneSetup()` au lieu de `@WebMvcTest` car Spring Boot 4 a déplacé `@WebMvcTest` dans `spring-boot-starter-webmvc-test` (non présent au pom) et supprimé `@MockBean` (remplacé par `@MockitoBean`). `standaloneSetup` est plus léger et ne nécessite aucune dépendance supplémentaire.

**Où on s'est arrêté :**

- API auth complète et testée : `POST /api/v1/auth/register|login|refresh|logout`.
- 37 tests passants (services + slice web controller).
- 2 commits poussés sur `origin/dev` : `e65c8c6` "Gestion auth (register,login,logout)" et `74bf4cf` "Mise a jour de l'architecture en ajoutant le module entreprise et renommage de variables".
- Modules métier toujours à câbler côté API : produit/stock/vente/achat/inventaire/abonnement/depense/notification (couches `application/`/`presentation/` quasi-vides).

**Prochaine étape recommandée :**

1. **Migration Flyway** (passer `ddl-auto: update` → `validate`) — prérequis avant de figer le schéma.
2. **CRUD produit** (premier vrai module métier post-auth) : `ProductController` + DTOs + services applicatifs + tests, en suivant strictement les 5 règles posées.
3. Frontend : `src/lib/api/client.ts` (axios + intercepteur JWT) + pages `(auth)/register` et `(auth)/login` qui consomment les endpoints.

---

### Session du 2026-05-10

**Sujet :** Implémentation complète de la chaîne JWT côté backend

**Ce qui a été fait :**
1. **`JwtService`** (`security/application/service/`) — `generateToken(UserPrincipal)`, `isTokenValid(String)`, `extractUserPrincipal(String)`, `parseClaims(String)`. jjwt 0.11.5, HS512.
2. **Externalisation config JWT** :
   - Création de `org.store.property.JwtProperties` (record + nested `Expiration(accessToken, refreshToken)`) avec `@ConfigurationProperties(prefix = "security.jwt")`.
   - `@ConfigurationPropertiesScan("org.store.property")` ajouté sur `StoreApplication`.
   - Bloc `security.jwt` dans `application.yml` : secret (`${JWT_SECRET:...}`), header `Authorization`, prefix `Bearer `, expirations 1h / 7d.
3. **Enum `Claim`** (`security/application/enums/`) — clés JWT centralisées : `ENTREPRISE("entrepriseId")`, `MAGASIN("magasinId")`, `USERNAME("username")`, `PERMISSIONS("permissions")`.
4. **Renommage `UserPrincipal.email` → `username`** (cohérence avec `Account.username`).
5. **`UserDetailsServiceImpl.loadUserByUsername`** :
   - `findByUsername(String)` ajouté à `AccountJpaRepository`.
   - Constructor injection du repo, `@Transactional(readOnly = true)`.
   - Authorities = `Role.permissions.code` mappées en `SimpleGrantedAuthority` (null/blank-safe).
6. **`JwtAuthenticationFilter.doFilterInternal`** :
   - Extraction header Bearer (via `JwtProperties.header()` / `prefix()`).
   - Si token valide → `UserPrincipal` placé dans le `SecurityContext`, authorities = `principal.permissions()`.
   - Constructeur passé de `(JwtService, UserDetailsService)` à `(JwtService, JwtProperties)`.
7. **Nettoyage `SecurityConfig`** :
   - Suppression du `@Bean public JwtAuthenticationFilter ...` qui dupliquait le `@Component`.
   - Suppression du `@Bean public UserDetailsServiceImpl ...` qui dupliquait le `@Service`.
   - Filtre injecté en paramètre de `securityFilterChain(...)`.
8. **Activation `@EnableJpaAuditing`** sur `StoreApplication` (`AuditorAware` reste à brancher).
9. Correction d'un import fantôme dans `CustomAccessDeniedHandler` (`com.poc.backenddeclarationpoc.exceptions.ErrorResponse` → `org.store.common.exceptions.ErrorResponse`) — fait par l'utilisateur en parallèle.

**Décisions / arbitrages :**
- Le `JwtAuthenticationFilter` ne re-vérifie pas le statut du compte (`enabled`/`locked`) à chaque requête — il fait confiance au JWT signé. À rebrancher si on veut durcir.
- `UserDetailsServiceImpl` retourne un `User` Spring (pas de wrapper custom autour d'`Account`). Conséquence : le service de login devra refaire un `findByUsername` pour récupérer l'`Account` et construire le `UserPrincipal` du JWT.
- Typo `PERMISIONS` → corrigée en `PERMISSIONS` dans le `Claim` enum.
- Toujours utiliser `Claim.X.getKey()` pour lire/écrire un claim — jamais de string littérale.

**Où on s'est arrêté :**
- Chaîne sécurité technique complète et compilable.
- `@EnableJpaAuditing` actif mais **`AuditorAware<String>` manquant** → `createdBy`/`updatedBy` resteront `null`.
- Aucun endpoint REST `/auth/...` encore en place — il faut maintenant produire les tokens.

**Prochaine étape recommandée :**
1. `AuditorAware<String>` qui lit le `UserPrincipal` du `SecurityContext` (renvoie `userId.toString()` ou anonymous).
2. CORS bean pour autoriser `http://localhost:3000`.
3. Use case d'inscription `Account` + `Proprietaire` + `Entreprise` + premier `Magasin` + abonnement d'essai.
4. Endpoints `/auth/register`, `/auth/login`, `/auth/refresh`.

---

## 🗂️ Historique des sessions

### Session du 2026-05-09 (suite — frontend init)

**Sujet :** Initialisation du frontend `store-frontend/` + analyse + mise à jour `.claude/`

**Ce qui a été fait :**
1. Analyse du nouveau dossier `store-frontend/` : Next.js 16.2.6, React 19.2.4, TypeScript 5, Tailwind CSS 4, shadcn/ui (style `base-nova`), `@base-ui/react`, react-hook-form + zod, axios + TanStack React Query, zustand, lucide-react.
2. Lecture de `store-frontend/AGENTS.md` ⚠️ → Next.js 16 a des **breaking changes** vs versions antérieures, consulter `node_modules/next/dist/docs/` avant de coder.
3. Constat de l'état initial : scaffold `create-next-app` quasi vierge — un seul composant shadcn (`button.tsx`), `src/lib/utils.ts` avec `cn()`, layout/page encore le template par défaut.
4. Mise à jour des 4 fichiers `.claude/` (PROJECT, ARCHITECTURE, TODO, SESSIONS) pour refléter le mono-repo.

**Décisions :**
- Le frontend reste dans le même mono‑repo que le backend, à la racine (`store-frontend/`).
- Stack frontend figée : Tailwind 4, shadcn `base-nova`, RHF + Zod, axios + React Query, Zustand.
- Server Components par défaut ; `"use client"` uniquement quand nécessaire.

**Où on s'est arrêté :**
- Backend : couche `domain/` complète ; pas de `SecurityConfig` ni d'auditing actif.
- Frontend : scaffold initial avec un seul composant UI.

### Session du 2026-05-09 (matinée — backend scaffolding)

**Ce qui a été fait :**
1. Analyse complète du backend : 14 modules métier, ~56 entités JPA, incohérences identifiées.
2. Remplissage du `README.md` du backend.
3. Refactor **Lombok** sur toutes les entités (ajout dépendance + `@Getter`/`@Setter` partout, conservation du setter custom `Product.setImages`).
4. Tentative d'harmonisation FR des noms de tables — **annulée** sur demande.
5. Création de **35 JPA repositories** dans `<module>/domain/repository/`.
6. Création de `GlobalService<E, R>` (CRUD générique abstrait) et `ValidatorService` dans `common/service`.
7. Création de **35 services domaine** héritant de `GlobalService` dans `<module>/domain/service/`.
8. Première mise à jour des fichiers `.claude/`.

**Décisions :**
- `JpaRepository` dans `domain/repository/` (pragmatique).
- Lombok `@Getter`/`@Setter` uniquement.
- `EntityException` custom à la place de `jakarta.persistence.EntityNotFoundException`.
- Naming strict : `<Entity>JpaRepository`, `<Entity>DomainService`.

**Où on s'est arrêté :**
- Domain backend complet, mais aucune couche application/infra/presentation, aucune sécurité, aucun auditing actif.
