# TODO.md — Backlog du projet

> Légende des statuts :
> `[ ]` À faire | `[~]` En cours | `[x]` Terminé | `[!]` Bloqué

---

## 🔴 Priorité haute

### Backend
- [x] Brancher `AuditorAware<String>` (`@EnableJpaAuditing` déjà actif sur `StoreApplication`, mais sans `AuditorAware` les `createdBy`/`updatedBy` resteront `null`)
- [x] `SecurityConfig` + filtre JWT + `UserDetailsService` — chaîne complète : `JwtService`, `JwtAuthenticationFilter`, `UserDetailsServiceImpl`, `JwtProperties`, enum `Claim`
- [x] Use case d'inscription propriétaire : `Account` + `Proprietaire` + `Entreprise` + premier `Magasin` + abonnement d'essai
- [x] Endpoints `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`
- [x] Migration Flyway (passer `ddl-auto` à `validate`) — `spring-boot-starter-flyway` 4.0.6 + `V1__init_schema.sql` manuel (40 tables, JOINED), `baseline-on-migrate=true` `baseline-version=1`
- [x] Activer CORS pour autoriser `http://localhost:3000` (frontend dev) — fait via `CorsOriginFilter` dans `security/config/`
- [x] **Création d'employés** (`POST /api/v1/employees`) — règles data-driven (propriétaire = tout, manager = tout sauf manager, un seul rôle élevé par magasin), `EmployeServiceImpl` + `EmployeController` + tests
- [x] **CRUD Magasin** (2026-05-12) — `findAll(Pageable)`, `findResponseById`, `activate`, `deactivate`, `updateMine`, soft delete. Scoping OWNER vs MANAGER. `@Query` projection en text block.
- [x] **CRUD Entreprise** (2026-05-12) — ADMIN sur `POST/GET/PATCH /entreprises`, PROPRIETAIRE sur `/me`. Convention `SELECT new EntrepriseResponse(e)` adoptée.
- [x] **Strategy pattern `UserPrincipalContextStrategy`** (2026-05-12) — élimination `instanceof` dans `UserPrincipalFactoryImpl`. Package `security/application/strategies/` : `UserPrincipalContext`, interface + 3 impls (Proprietaire/Employe/Utilisateur fallback). Dispatch "most-specific wins".
- [x] **Extraction `UserResponse` DTO** (2026-05-12) — `AccountResponse` agrège un `UserResponse` au lieu des 5 champs à plat. Constructeur `UserResponse(Utilisateur)`.
- [x] **Seed ERP** (2026-05-12) — 4 rôles (PROPRIETAIRE, MANAGER, VENDEUR, ADMIN) + 79 permissions (4 anciennes + 75 nouvelles MODULE_ACTION) selon matrice PDF `Roles Permissions Erp Saas.pdf`. MANAGER absorbe Magasinier+Comptable. `DataInitializer` étendu (idempotent).
- [x] **RBAC YAML synchronisable** (2026-05-13) — Externalisation rôles+permissions dans `src/main/resources/security/roles-permissions.yml`. `RbacProperties` (`security.rbac.sync` + `security.rbac.file`). `IRolesPermissionsSyncService` (additif + log des orphelins, jamais de suppression). `DataInitializer` allégé : appel conditionnel à `sync()` si `sync=true`. 4 tests unitaires (création, idempotence, mise à jour assoc, détection orphelins).

### Frontend
- [ ] Vérifier l'install de TanStack React Query (clé `"root": "github:tanstack/react-query"` dans `package.json` est suspecte — l'import `@tanstack/react-query` fonctionne‑t‑il vraiment ?)
- [ ] Customiser `src/app/layout.tsx` : metadata projet (titre/description), `lang="fr"`
- [ ] Créer `src/lib/api/client.ts` (instance axios + intercepteur JWT)
- [ ] Brancher `QueryClientProvider` dans un `Providers` client‑component
- [ ] Pages auth `(auth)/login` et `(auth)/register` (formulaire RHF + Zod, appel API)
- [ ] Layout authentifié `(dashboard)/layout.tsx` (sidebar + header + garde de session)

---

## 🟡 Priorité normale

### Backend
- [ ] Couche `application/` : services use case + DTO + mappers (par module, en commençant par `security`, `magasin`, `users`)
- [ ] Couche `presentation/` : controllers REST (CRUD basique sur chaque ressource)
- [ ] Specifications JPA (`infrastructure/specifications/`) pour les filtres complexes (recherche produits, etc.)
- [ ] Use case stock FIFO : `EntreeStock` consommée par `SortieStock` lors d'une vente
- [ ] Génération automatique des références (`commande_vente.reference`, `facture_*.numero`)
- [ ] Notifications : worker qui parcourt `Echeance` et envoie via canal `EMAIL` / `SMS`

### Frontend
- [ ] Pages CRUD de base (produits, catégories, qualité) : table + form modal
- [ ] Dashboard accueil (KPIs simples : ventes du jour, stock bas, échéances à venir)
- [ ] Cycle vente UI : sélection client → ajout produits → paiement → impression facture
- [ ] Cycle achat UI : sélection fournisseur → commande → réception → facture
- [ ] Sélecteur de magasin (si propriétaire en a plusieurs) → propage dans store Zustand
- [ ] Gestion erreurs API globale (toast + retry) via React Query

---

## 🟢 Priorité basse / Nice-to-have

### Backend
- [ ] Tests d'intégration sur les flux critiques (inscription, vente, paiement)
- [ ] Dashboard / endpoints de reporting (CA, marge, stock dormant)
- [ ] OpenAPI groupé par module + auth bearer dans Swagger UI
- [ ] Dockerfile + docker-compose (app + Postgres)
- [ ] Documentation API enrichie

### Frontend
- [ ] Génération auto des types DTO depuis l'OpenAPI du backend (`openapi-typescript`)
- [ ] i18n (next-intl ou équivalent) — au minimum FR ; EN plus tard
- [ ] Dark mode toggle (les CSS variables shadcn le supportent déjà)
- [ ] PWA / mode offline minimal
- [ ] Tests Vitest + Testing Library

---

## ✅ Terminées

- [x] **Analyse initiale du projet** — cartographie des 14 modules, ~56 entités, identification des incohérences
- [x] **Refactor Lombok** — remplacement des getters/setters manuels par `@Getter`/`@Setter` sur toutes les entités
- [x] **Ajout dépendance Lombok** dans `pom.xml` + exclusion dans plugin Spring Boot
- [x] **Création des JPA repositories** (35 fichiers) — `<Entity>JpaRepository extends JpaRepository<Entity, UUID>` dans `<module>/domain/repository/`
- [x] **`GlobalService<E, R>`** — classe abstraite générique CRUD dans `common/service`
- [x] **`ValidatorService`** — wrapper `jakarta.validation.Validator` dans `common/service`
- [x] **Création des Domain Services** (35 fichiers) — `<Entity>DomainService extends GlobalService<...>` dans `<module>/domain/service/`
- [x] **Remplissage README.md** — présentation, stack, architecture, modèle de données
- [x] **Chaîne JWT complète** — `JwtService` (generate/validate/extract), `JwtAuthenticationFilter` (Bearer → `UserPrincipal` dans `SecurityContext`), `UserDetailsServiceImpl` (lookup `Account` + authorities depuis `Role.permissions.code`)
- [x] **Externalisation config JWT** — record `org.store.property.JwtProperties` (`security.jwt` : secret, header, prefix, expiration access/refresh) + `@ConfigurationPropertiesScan` sur `StoreApplication`
- [x] **Enum `Claim`** — clés JWT centralisées (`ENTREPRISE`, `MAGASIN`, `USERNAME`, `PERMISSIONS`)
- [x] **Nettoyage `SecurityConfig`** — suppression des doubles bindings (`@Component`/`@Bean` sur `JwtAuthenticationFilter` et `@Service`/`@Bean` sur `UserDetailsServiceImpl`)
- [x] **Renommage `UserPrincipal.email` → `username`** — cohérence avec `Account.username`
- [x] **Activation `@EnableJpaAuditing`** sur `StoreApplication` (⚠️ `AuditorAware` reste à brancher)
- [x] **`AuditorAwareImpl`** (`org.store.config`) — lit le `UserPrincipal` du `SecurityContext` et renvoie `userId.toString()` ; `Optional.empty()` si pas d'authentification
- [x] **Refactoring hexagonal pragmatique** — extraction des ports `<Entity>Repository` (interface pure dans `domain/repository/`) et adapters `<Entity>JpaRepository` (Spring Data dans `infrastructure/repository/`) pour les 35 entités. `BaseRepository<E>` dans `common/repository/`. `GlobalService` typé sur le port. Faute `repositorty/` corrigée.
- [x] **i18n backend FR/EN** — `I18nConfig` (`MessageSource` UTF-8, `AcceptHeaderLocaleResolver` default FR + supported [FR, EN], `LocalValidatorFactoryBean` câblé), `IMessageSourceService` + `MessageSourceServiceImpl`, `LocalizedRuntimeException` parente des 12 exceptions custom (porte `messageKey` + `args`), `GlobalException` advice résout via `IMessageSourceService`, `messages.properties` / `messages_en.properties` (clés applicatives + 22 clés Bean Validation Jakarta)
- [x] **`DataInitializer`** (`org.store.config`, `ApplicationRunner`) — seeds idempotents au boot : `Permissions` (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`), `Role` (`PROPRIETAIRE`, `EMPLOYE`), `PlanAbonnement` trial gratuit (1 magasin, 3 employés, 30 jours)
- [x] **Endpoints `/api/v1/auth/*`** — `POST /register` (201 + tokens), `POST /login` (200 + tokens), `POST /refresh` (200 + nouveau access), `POST /logout` (204, idempotent). `RefreshToken` UUID opaque persisté (7 jours), pas de rotation. `AuthController` + 11 services applicatifs (`IAccountService`, `IProprietaireService`, `IEntrepriseService`, `IMagasinService`, `IAbonnementService`, `ILoginService`, `IRefreshTokenService`, `IRegisterPropertyService`, `IUserPrincipalFactory`, `IJwtService`)
- [x] **Module `entreprise`** extrait du module `magasin` — `Entreprise` (entité, port, adapter, services, DTO, test) déplacés dans `org.store.entreprise.*`. Imports cross-modules ajustés.
- [x] **Tests unitaires + slice web** — 37 tests passants (services + `AuthController` en `MockMvcBuilders.standaloneSetup`, Spring Boot 4 ayant retiré `@WebMvcTest`/`@MockBean` du starter de base)
- [x] **Flyway** (2026-05-12) — `spring-boot-starter-flyway` 4.0.6 + `flyway-database-postgresql`, `V1__init_schema.sql` manuel (~485 lignes, 40 tables, héritage JOINED `person`/`utilisateur`, table jointe `role_permission`, FK via `ALTER TABLE` à la fin), `ddl-auto: validate`, `baseline-on-migrate=true`, `baseline-version=1`
- [x] **Validators custom** (`common/validation/`) — `@Phone` (regex SN), `@EnumValue` (vérif contre une classe enum), `@DatePattern` (parseable via `DateTimeFormatter`), `@Uuid` (parseable via `UUID.fromString`). Chacun avec test paramétré + clé i18n
- [x] **Téléphone obligatoire** dans la registration propriétaire (`UtilisateurRequest.telephone` `@NotBlank @Phone`)
- [x] **Service isolation** — tous les `<X>ServiceImpl` injectent désormais leur `<X>DomainService` (jamais un `<X>Repository` direct). DomainServices enrichis de méthodes custom (`findByUsername`, `findByToken`, `findByLibelle`, `findFirstTrialActif`, `findAllByRoleId`). `UserDetailsServiceImpl` aussi
- [x] **Enum `PermissionCode`** — codes permissions centralisés (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`). Overload `UserPrincipal.hasPermission(PermissionCode)`. `DataInitializer` et `EmployeServiceImpl` utilisent l'enum
- [x] **`UserPrincipal.role`** — libellé du rôle ajouté au record + claim JWT `ROLE("role")` dans l'enum `Claim`
- [x] **`ICurrentUserService`** — récupère le `UserPrincipal` depuis `SecurityContextHolder` ; les services métier l'injectent au lieu d'utiliser `@AuthenticationPrincipal` au controller
- [x] **`PermissionsDomainService.findAllByRoleId`** — JPQL projection sur `role_permission`, retourne `List<String>` de codes. Évite l'accès LAZY. Utilisé par `UserPrincipalFactoryImpl`, `UserDetailsServiceImpl`, `EmployeServiceImpl`
- [x] **`EmployeResponse(Employe)`** — constructeur secondaire qui extrait les champs depuis l'entité (convention applicable à tous les `<X>Response`)
- [x] **`FONCTIONNALITIES.md`** à la racine du backend — récap des 5 services applicatifs métier (auth × 4 + employé)

---

## 🐛 Erreurs connues / dette

### Backend
- [ ] `org.store.common.tools` — classe vide, nom en minuscule (à supprimer ou renommer)
- [ ] `Entreprise.proprietaire nullable=false` + héritage `Utilisateur` : chaîne d'inscription à clarifier (poule/œuf entre `Account` et `Proprietaire`)
- [ ] `application.yml` contient des credentials par défaut hardcodés → à externaliser

### Frontend
- [ ] `package.json` : entrée `"root": "github:tanstack/react-query"` non standard — devrait être `"@tanstack/react-query": "..."`. À corriger ou vérifier que l'install fonctionne.
- [ ] `src/app/layout.tsx` : metadata `Create Next App` à remplacer ; `lang="en"` → `lang="fr"`.
- [ ] `src/app/page.tsx` : encore le template Next.js par défaut.
- [ ] `README.md` du frontend : encore le README générique de `create-next-app`.

---

## 🗒️ Notes & décisions

- **2026-05-09** — `JpaRepository` placés dans `domain/repository/` plutôt que `infrastructure/`, choix pragmatique vs hexagonal strict.
- **2026-05-09** — Tous les services domaine héritent de `GlobalService<E, R>` qui fournit le CRUD de base. Logique métier spécifique à ajouter au cas par cas.
- **2026-05-09** — Lombok `@Getter`/`@Setter` uniquement, jamais `@Data` ni `@ToString` (sécurité JPA).
- **2026-05-09** — Harmonisation FR des noms de tables (`product` → `produit`, etc.) **annulée** : on garde les noms d'origine.
- **2026-05-09** — Custom `EntityException` (dans `common/exceptions`) utilisée à la place de `jakarta.persistence.EntityNotFoundException`.
- **2026-05-09** — Mono‑repo confirmé : ajout du frontend `store-frontend/` (Next.js 16 + shadcn + base‑ui).
- **2026-05-09** — Frontend stack figée : Tailwind 4, shadcn `base-nova`, react-hook-form + zod, axios + React Query, zustand.
- **⚠️ Next.js 16** — version récente avec breaking changes ; toujours consulter `store-frontend/AGENTS.md` et `node_modules/next/dist/docs/` avant d'écrire du code Next.
- **2026-05-10** — Config JWT externalisée dans `application.yml` sous `security.jwt`, bindée via record `JwtProperties` (package `org.store.property`).
- **2026-05-10** — Clés de claims JWT centralisées dans l'enum `org.store.security.application.enums.Claim` — toujours utiliser `Claim.X.getKey()`, jamais de string littérale.
- **2026-05-10** — `JwtAuthenticationFilter` ne re-valide PAS le statut du compte (`enabled`/`locked`) à chaque requête — il fait confiance au JWT signé. À rebrancher si besoin de durcir.
- **2026-05-10** — `UserDetailsServiceImpl` retourne un `User` Spring (pas de wrapper custom autour d'`Account`). Conséquence : le service de login devra refaire un `findByUsername` pour construire le `UserPrincipal` du JWT.
- **2026-05-10** — `UserPrincipal.email` renommé en `username` pour rester cohérent avec `Account.username`.
- **2026-05-11** — Pas d'entité `Administrator` dédiée : un admin SaaS = `Utilisateur` racine (sans sous-classe `Proprietaire`/`Employe`) + `Account` avec un `Role` "ADMIN". Distinction au runtime via `Account.role`. Si besoin futur de champs propres aux admins → créer `Administrator extends Utilisateur` à ce moment-là.
- **2026-05-11** — **Refactoring hexagonal pragmatique** : décision précédente (2026-05-09) sur `JpaRepository` dans `domain/repository/` **annulée**. Désormais : port `<Entity>Repository extends BaseRepository<E>` dans `domain/repository/` (interface pure), adapter `<Entity>JpaRepository extends JpaRepository, <Entity>Repository` dans `infrastructure/repository/`. `GlobalService` typé sur `<Entity>Repository`. Les entités JPA restent dans `domain/model/` (compromis assumé). Faute de frappe `repositorty/` corrigée partout.
- **2026-05-11** — **i18n backend FR/EN** mis en place : default `Locale.FRENCH`, locale résolue via header `Accept-Language` (`AcceptHeaderLocaleResolver`). Toutes les exceptions custom (sauf `AuthentificationException` qui hérite de Spring Security) héritent désormais de `LocalizedRuntimeException` et portent `(messageKey, args)`. L'advice `GlobalException` résout via `IMessageSourceService`. Clés Bean Validation Jakarta overridées dans `messages*.properties`. `useCodeAsDefaultMessage=true` → si une clé est absente, le code est retourné tel quel (fallback safe). Convention : préfixer les clés par domaine (`entity.*`, `validation.*`, `error.*`, `auth.*`, etc.).
- **2026-05-11** — **Règles obligatoires posées** (toutes inscrites en mémoire user) :
  - Controller : `public static final String BASE_PATH = "/api/v1/<scope>"` (version hardcodée) + `@RequestMapping(BASE_PATH)`, pas de `@PathVariable("version")` sur les handlers
  - Chaque service applicatif a **obligatoirement** une interface `I<X>Service` + impl `<X>ServiceImpl` ; sites d'injection utilisent l'interface
  - DTOs : suffixe `<X>Request` (entrée) ou `<X>Response` (sortie), jamais `Info` ni `Dto` générique
  - Paramètre de méthode de type DTO : nom = camelCase complet du type (`AccountRequest accountRequest`), pas `info`/`request`/`dto` génériques
  - Tests : chaque service applicatif a un `<X>ImplTest` (JUnit5 + Mockito), chaque controller un `<X>ControllerTest` (`@WebMvcTest` ou `MockMvcBuilders.standaloneSetup`)
- **2026-05-11** — **Module `entreprise`** : sous-domaine `Entreprise` extrait du module `magasin` pour cohérence métier (l'entreprise est un agrégat racine multi-tenant, distinct du magasin). Le module `magasin` ne porte plus que `Magasin`. Imports croisés ajoutés (`Magasin.entreprise`, `Entreprise.magasins`).
- **2026-05-11** — **Refresh token** : UUID opaque persisté (table `refresh_token`, durée `properties.expiration().refreshToken()` = 7j). **Pas de rotation** pour MVP. `/auth/logout` révoque (idempotent). L'access token JWT ne peut pas être révoqué côté serveur (stateless) — il expire seul en 1h.
- **2026-05-11** — **Spring Boot 4 — packages test restructurés** : `@WebMvcTest` n'est plus dans `spring-boot-starter-test`, il faut `spring-boot-starter-webmvc-test`. `@MockBean` supprimé → remplacé par `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`). Solution actuelle : `MockMvcBuilders.standaloneSetup()` pour les tests controller (aucune dépendance pom supplémentaire).
- **2026-05-12** — **Flyway / Spring Boot 4** : l'autoconfig Flyway est sortie de `spring-boot-autoconfigure`. Il faut `spring-boot-starter-flyway` (module dédié SB 4) + `flyway-database-postgresql`. `baseline-version: 1` pour que les BDD existantes (déjà peuplées par `ddl-auto: update`) considèrent V1 comme déjà appliquée. `V1__init_schema.sql` rédigé à la main (40 tables, JOINED `person`/`utilisateur`, `role_permission`, FK via `ALTER TABLE` à la fin).
- **2026-05-12** — **Service isolation renforcée** : tous les `<X>ServiceImpl` injectent leur `<X>DomainService`, jamais un `<X>Repository`. Les DomainServices exposent les queries custom (`findByUsername`, `findByToken`, `findByLibelle`, `findFirstTrialActif`, `findAllByRoleId`) — renvoient `Optional<E>`, l'app service throw. `UserDetailsServiceImpl` (Spring Security) suit la même règle.
- **2026-05-12** — **Validators custom** (`org.store.common.validation`) : `@Phone` (regex SN `^(70|75|76|77|78|33)\d{7,9}$`), `@EnumValue(enumClass, ignoreCase)`, `@DatePattern(pattern)` (SMART mode), `@Uuid`. Tous skippent null/empty (combiner avec `@NotBlank` si requis). Chaque validateur a sa clé i18n + test paramétré.
- **2026-05-12** — **`PermissionCode` enum** : codes permissions centralisés (`PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`). Le `code` en BDD = `enum.name()`. Overload `UserPrincipal.hasPermission(PermissionCode)`. Plus aucune chaîne de permission éparpillée.
- **2026-05-12** — **`UserPrincipal.role`** : champ String ajouté (libellé du rôle, pas UUID). Sérialisé dans le JWT via `Claim.ROLE("role")`. `UserPrincipalFactoryImpl` utilise `account.role.libelle`. (Décision : libellé > UUID pour la flexibilité d'ajout de rôles.)
- **2026-05-12** — **`ICurrentUserService`** : remplace `@AuthenticationPrincipal UserPrincipal` au controller. Les services métier appellent `currentUserService.getCurrent()` → testable avec un simple mock (vs setup SecurityContext + custom argument resolver dans MockMvc).
- **2026-05-12** — **Règles employé data-driven** : aucun libellé `"MANAGER"` hardcodé dans `EmployeServiceImpl`. La règle "rôle élevé = peut créer des employés" se base sur la permission `EMPLOYE_CREATE`. Ajouter un futur `CAISSIER` (avec `EMPLOYE_ACCESS`) ou `SUPER_MANAGER` (avec `EMPLOYE_CREATE`) ne demande **aucun changement applicatif** — seulement un `ensureRole(...)` dans `DataInitializer`.
- **2026-05-12** — **`<X>Response(<X> entity)`** : tout DTO de sortie doit exposer un constructeur secondaire qui extrait les champs depuis l'entité. Centralise le mapping, supprime la duplication dans les services.
- **2026-05-12** — **`SELECT new <X>Response(entity)` dans `@Query`** : projection JPQL via le constructeur secondaire de Response, jamais la liste des champs en clair (DRY, pas de désync sur ajout de champ). Pré-requis : la convention `Response(Entity)` doit être appliquée.
- **2026-05-12** — **`create()` dans le DomainService** : construction d'entité (`new` + setters + `save`) vit dans `<X>DomainService.create(...)`. L'app service délègue + applique les règles métier. Sépare clairement "persistance" (domain) de "orchestration" (application).
- **2026-05-12** — **Réorganisation packages `impl`** : toutes les classes `<X>ServiceImpl` déplacées dans `<module>/application/service/impl/`. Cohérence transverse (interface dans `service/`, impl dans `service/impl/`).
- **2026-05-12** — **Pas de méthodes privées dans les services applicatifs** : toute factorisation devient une méthode publique de `I<X>Service`, paramétrée plutôt que spécialisée (ex. `createAccount(request, String roleName)` vs `doCreate(request)` qui hardcodait `ROLE_PROPRIETAIRE`). Exceptions tolérées : helpers triviaux sur DTO/utilitaire, pas un service métier.
- **2026-05-12** — **Pas de logique métier dans le controller** : handler = `return service.method(request)`. Pas de cast, pas de `new Response(...)`, pas de getter-chain, pas d'orchestration multi-services. Si la méthode métier nécessaire crée un cycle de DI, la créer dans le service qui orchestre déjà (vu : `IRegisterPropertyService.registerEntrepriseByAdmin` plutôt que `IEntrepriseService.createByAdmin`).
- **2026-05-12** — **Sous-DTO Response pour sous-entité** : quand un `<X>Response` agrège ≥ 3 champs d'une autre entité réutilisable, créer un `<Y>Response` dédié (ex. `AccountResponse.user: UserResponse`) plutôt que de mettre les champs à plat. JSON hiérarchique, DRY, suit la convention `Response(Entity)`.
- **2026-05-12** — **Strategy pattern pour dispatch par sous-type** : éviter `if (x instanceof A) ... else if (x instanceof B)` dans le code de dispatch. Pattern : interface `<X>Strategy { Class<? extends Parent> targetType(); <Y> resolve(Parent x); }` + impls `@Component` + Spring injecte `List<Strategy>` + dispatch via `.filter(s -> s.targetType().isInstance(x)).reduce((a,b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)` → "most-specific wins". Permet fallback sur le type parent.
- **2026-05-12** — **Seed ERP additif** : les 4 anciennes permissions (PROPRIETAIRE_ACCESS, EMPLOYE_ACCESS, EMPLOYE_CREATE, ADMIN_ACCESS) conservées en BD pour que le code Java actuel continue de fonctionner. Les 75 nouvelles (MODULE_ACTION) cohabitent. Migration progressive des `@PreAuthorize` à faire plus tard.
- **2026-05-12** — **`Proprietaire` n'a pas de `magasinId`** dans `UserPrincipal` : un OWNER possède tous les magasins de son entreprise, pas un magasin particulier. Le `magasinId` n'est porté que par les `Employe` (Manager/Vendeur).
- **2026-05-12** — **DomainService = port d'accès aux données** : tout CRUD ou query custom est dans `<X>DomainService`. L'application ne voit jamais `<X>Repository`. Une méthode réutilisable (ex. `extractPermissionCodes`) doit vivre dans le service propriétaire de l'agrégat, jamais en helper privé dupliqué entre services.
