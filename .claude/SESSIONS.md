# SESSIONS.md — Journal des sessions

> Claude Code remplit ce fichier à la fin de chaque session.
> À relire en priorité au démarrage d'une nouvelle session pour reprendre le contexte immédiatement.

---

## 📌 Dernière session

**Date :** 2026-05-13
**Sujet :** Externalisation RBAC (rôles + permissions) dans un YAML synchronisable, flag `security.rbac.sync`, i18n des exceptions, surcharge `LocalizedRuntimeException` avec cause, convention de documentation des services applicatifs

**Ce qui a été fait :**

1. **YAML RBAC** — `src/main/resources/security/roles-permissions.yml` : 76 permissions (4 legacy + 72 granulaires `MODULE_ACTION`) + 4 rôles (ADMIN/PROPRIETAIRE/MANAGER/VENDEUR) avec liste explicite de codes par rôle. Source de vérité pour le seed.

2. **`RbacProperties`** (`org.store.property`, record `@ConfigurationProperties("security.rbac")`) — 2 champs : `sync:boolean` + `file:Resource`. Spring résout automatiquement `classpath:security/roles-permissions.yml` en `ClassPathResource`.

3. **`application*.yml`** — bloc `security.rbac` ajouté dans les 4 fichiers (`application.yml`, `application-dev.yml`, `application-prod.yml`, `application-test.yml`). Overrides env `RBAC_SYNC` / `RBAC_FILE`. **Default `sync: false`** : posture défensive — le seed est explicite, activé via `RBAC_SYNC=true` au boot.

4. **`IRolesPermissionsSyncService` + `RolesPermissionsSyncServiceImpl`** (`security/application/service/...`) — orchestrateur de la synchronisation, **stratégie additive** :
   - Étape 1 : charge YAML via SnakeYAML (déjà dans Spring Boot, pas de dépendance Maven ajoutée).
   - Étape 2 : insère permissions manquantes, garde un `catalog Map<code, Permissions>` en mémoire pour éviter les SELECT en cascade lors de l'étape 3.
   - Étape 3 : `ensureRole(...)` pour chaque rôle YAML — création ou ajout des associations manquantes ; **les associations existantes en BD mais absentes du YAML sont conservées + WARN log**.
   - Étape 4 : log WARN des permissions/rôles orphelins (en BD, absents du YAML). Aucune suppression.
   - Tout est `@Transactional`. Retourne un `RbacSyncReport(added/updated/orphan...)`.

5. **DTOs** — `RbacConfig(permissions, List<RoleDef>)` + nested `RoleDef(libelle, description, permissions)` pour parser le YAML ; `RbacSyncReport` pour le rapport.

6. **`RbacConfigException extends LocalizedRuntimeException`** (`common/exceptions/`) — 4 clés i18n FR/EN :
   - `rbac.config.fileMissing`
   - `rbac.config.fileEmpty`
   - `rbac.config.loadFailed` (préserve la cause `IOException`)
   - `rbac.config.unknownPermission` (rôle référence permission non déclarée globalement)

7. **Surcharge `LocalizedRuntimeException(messageKey, Throwable cause, Object... args)`** — pour préserver la stacktrace racine quand on wrap une exception externe (IO, parsing, etc.). `RbacConfigException` expose aussi la surcharge.

8. **`DataInitializer` allégé** — passe de ~250 lignes (hardcodage permissions/rôles) à ~50 lignes : appelle `syncService.sync()` si `rbacProperties.sync()=true`, sinon log "skipped". Conserve seulement `ensureTrialPlan()` (responsabilité distincte).

9. **`PermissionsDomainService.findByCode(String)`** ajouté (était manquant).

10. **Javadoc complète sur `RolesPermissionsSyncServiceImpl`** — javadoc de classe, javadoc sur chaque méthode publique (étapes numérotées + exceptions avec clés i18n), commentaires de section pour la navigation, commentaires inline pour le **pourquoi** (catalog en mémoire, comparaison par ID, distinction added/updated, etc.).

11. **Nouvelle convention dans `ARCHITECTURE.md` (règle 29)** — **"Documentation des services applicatifs (process métier)"** : tout `<X>ServiceImpl` doit porter javadoc de classe + javadoc par méthode publique (entrée, règles, exceptions, sortie ; étapes numérotées si orchestration). Périmètre : services applicatifs uniquement (DomainServices et controllers libres). Modèle de référence : `RolesPermissionsSyncServiceImpl`. Règle sauvée en mémoire user (`feedback_doc_service_applicatif.md`).

12. **Tests** — `RolesPermissionsSyncServiceImplTest` (JUnit5 + Mockito) : 4 cas (création initiale, idempotence, mise à jour associations, détection orphelins sans suppression). Le YAML de test est embarqué via `ByteArrayResource`.

**Décisions / arbitrages :**

- **Stratégie additive (vs strict / vs additif sans suppression assoc)** : choisie après comparaison. Raison : une mauvaise édition du YAML en prod ne doit jamais retirer silencieusement des droits à des utilisateurs. Le WARN signale l'écart, l'opérateur décide.
- **Format YAML — liste explicite de permissions par rôle (vs groupes/includes)** : verbosité acceptée pour la lisibilité du diff git et l'absence d'ambiguïté.
- **Trigger sync — au boot uniquement** : pas d'endpoint admin pour l'instant. Le flag `security.rbac.sync` contrôle l'exécution. Resync = redémarrage avec `RBAC_SYNC=true`.
- **Default `sync: false`** : safety net pour éviter une modif YAML accidentelle qui pollue la BD. Le devops/dev active explicitement quand il veut.
- **Surcharge `LocalizedRuntimeException(key, cause, args)`** : la convention "constructeur unique" est étendue à "deux formes officielles". Justifié pour préserver la cause des wraps techniques (IO, parsing). À garder en tête pour les exceptions futures.
- **Logs non i18n** : convention projet — les logs visent l'opérateur système, pas l'utilisateur final. Restent en anglais figé.
- **Convention de doc — `<X>ServiceImpl` uniquement** : les DomainServices (passe-plats data) et controllers (handlers minimaux) sont auto-documentants. Doc concentrée là où vit la logique métier.

**Où on s'est arrêté :**

- **156 tests verts** (152 → 156 avec les 4 nouveaux).
- 2 commits poussés sur `origin/dev` : `0115ca4` "Externalisation seed RBAC dans roles-permissions.yml + flag security.rbac.sync" + `bef46eb` "Documentation services applicatifs (convention + javadoc RBAC sync)".
- Sync RBAC **désactivée par défaut** (`RBAC_SYNC=false`). La BD locale a été seedée lors du test d'intégration (orphelins préexistants : rôle `EMPLOYE` + perm `EMPLOYE_ACCESS` sur `PROPRIETAIRE` — non-bloquants).
- Convention de doc adoptée pour tous les futurs `<X>ServiceImpl`.

**Prochaine étape recommandée :**

1. **CRUD `Product`** — premier vrai module métier post-auth. Suit les patterns établis (Request/Response avec entity constructor, DomainService.create, ServiceImpl + interface, controller avec BASE_PATH, tests unit + slice web). **Appliquer la nouvelle convention de doc** (javadoc complète sur `ProductServiceImpl`).
2. **Migration progressive des `@PreAuthorize`** vers la nomenclature granulaire : remplacer `PROPRIETAIRE_ACCESS` par `COMPANY_READ/UPDATE` + `STORE_*` selon l'endpoint, `EMPLOYE_CREATE` par `USER_CREATE`. Toucher l'enum `PermissionCode` (ajouter les 72 valeurs granulaires) et tous les sites d'appel.
3. **Frontend** : `src/lib/api/client.ts` (axios + intercepteur JWT) + pages `(auth)/login` et `(auth)/register`.

---

### Session du 2026-05-12 (après-midi/soir)
**Sujet :** CRUD Magasin, CRUD Entreprise (ADMIN vs PROPRIETAIRE), refactor `registerEntrepriseByAdmin` (cycle DI), extraction `UserResponse`, Strategy pattern `UserPrincipalContextStrategy` (élimination `instanceof`), seed ERP (4 rôles + 79 permissions selon matrice PDF)

**Ce qui a été fait :**

1. **CRUD Magasin** — `IMagasinService` étendu : `findAll(Pageable)`, `findResponseById`, `activate`, `deactivate`, `updateMine`. Controller `MagasinController` (`@PreAuthorize("hasAuthority('PROPRIETAIRE_ACCESS')")` par défaut au niveau classe). Soft-delete via `actif=false`. Scoping : OWNER voit/édite tous les magasins de son entreprise, MANAGER scopé sur son magasin. Pagination via `@Query` custom + text block.

2. **CRUD Entreprise** — `IEntrepriseService` étendu : `findAll(Pageable)`, `findResponseById`, `activate`, `deactivate`, `findCurrentUserEntreprise`, `updateCurrentUserEntreprise`. Controller : ADMIN sur `POST/GET/PATCH /entreprises[/{id}]`, PROPRIETAIRE sur `/me`. `EntrepriseRepository.findAllProjected` utilise `SELECT new EntrepriseResponse(e)` (convention).

3. **`SELECT new <X>Response(entity)` dans `@Query`** — convention adoptée : projection JPQL via le constructeur secondaire de Response, jamais la liste des champs en clair (DRY, pas de désync). Appliquée à `EntrepriseRepository.findAllProjected` et `MagasinRepository.findResponsesByEntrepriseId`.

4. **`create()` dans DomainService** — toute construction d'entité (`new` + setters + `save`) vit dans `<X>DomainService.create(...)`. Les ServiceImpl applicatifs délèguent et appliquent les règles métier. `EntrepriseDomainService.create(EntrepriseRequest, Proprietaire)` et `MagasinDomainService.create(MagasinRequest, Entreprise)`.

5. **Réorganisation packages `impl`** — toutes les classes `<X>ServiceImpl` déplacées dans `<module>/application/service/impl/`. Cohérence transverse, plus de mélange interface/impl dans `service/`.

6. **`registerOwnerByAdmin` → `registerEntrepriseByAdmin`** — un ADMIN crée une entreprise via `POST /entreprises`. Premier essai : controller appelait `createAccount` directement + cast + `new EntrepriseResponse(...)` → règle "pas de logique métier au controller" violée. Tentative #1 : `IEntrepriseService.createByAdmin` → **cycle de DI** (`EntrepriseServiceImpl` injectait `IRegisterPropertyService` qui injectait déjà `IEntrepriseService`). Solution : méthode `IRegisterPropertyService.registerEntrepriseByAdmin(RegisterPropertyRequest) → EntrepriseResponse` (le service qui orchestre déjà héberge la méthode). Controller = 1 ligne.

7. **Extraction `UserResponse` DTO** — `AccountResponse` exposait à plat `(nom, prenom, email, telephone, adresse)` du `Utilisateur` lié. Refactor en `AccountResponse(..., UserResponse user)`. Nouveau record `users/application/dto/UserResponse(...)` avec constructeur `UserResponse(Utilisateur)`. Convention nouvelle : sous-DTO Response pour sous-entité (≥ 3 champs, réutilisable).

8. **Strategy pattern `UserPrincipalContextStrategy`** — élimination du `if (user instanceof Proprietaire) ... else if (user instanceof Employe)` dans `UserPrincipalFactoryImpl`. Nouveau package `security/application/strategies/` : record `UserPrincipalContext(entrepriseId, magasinId)`, interface `UserPrincipalContextStrategy(targetType, resolve)`, 3 impls (`ProprietairePrincipalContextStrategy`, `EmployePrincipalContextStrategy`, `UtilisateurPrincipalContextStrategy` fallback ADMIN). Factory injecte `List<Strategy>` et dispatch via `reduce` "most-specific wins" (`targetType.isAssignableFrom`). Modification utilisateur : `Proprietaire` ne porte plus de `magasinId` (un OWNER n'est rattaché à aucun magasin précis).

9. **Seed ERP** (`DataInitializer`) — adoption de la nomenclature granulaire du PDF `Roles Permissions Erp Saas.pdf` **sans casser le code existant** : 4 rôles (PROPRIETAIRE, MANAGER, VENDEUR, ADMIN = SUPER_ADMIN sémantiquement) + 79 permissions (4 anciennes conservées pour compat + 75 nouvelles MODULE_ACTION). Mapping selon la matrice PDF avec MANAGER absorbant Magasinier+Comptable. ADMIN = toutes permissions. PROPRIETAIRE ≈ OWNER du PDF. VENDEUR limité à SALE_* + lecture produits/stock. `static/liste_roles_permissions.md` archivé dans `resources/static/`.

10. **Pas de méthodes privées dans les services applicatifs** — règle posée. Toute factorisation devient une méthode publique de `I<X>Service`, paramétrée plutôt que spécialisée. Exemple : ancien `private doCreate(RegisterPropertyRequest)` qui hardcodait `ROLE_PROPRIETAIRE` → méthode publique `IRegisterPropertyService.createAccount(RegisterPropertyRequest, String roleName)`.

11. **Pas de logique métier dans le controller** — règle posée. Handler = `return service.method(request)`. Pas de cast, pas de `new Response(...)`, pas de getter-chain, pas d'orchestration multi-services. Si du code "déborde", créer une méthode publique dans `I<X>Service` qui retourne déjà le Response final. Choisir le service qui orchestre déjà pour éviter les cycles de DI.

**Décisions / arbitrages :**

- **Sens du cycle DI** : `IRegisterPropertyService` injecte `IEntrepriseService` (déjà fait). Donc toute méthode applicative qui doit orchestrer "inscription + retour entreprise" vit côté `IRegisterPropertyService`, pas l'inverse.
- **`Proprietaire` n'a pas de `magasinId`** dans `UserPrincipal` : l'OWNER possède tous les magasins de son entreprise, pas un magasin unique. Conséquence : strategy `ProprietairePrincipalContextStrategy` renvoie `(entreprise.id, null)`. Le `magasinId` n'est porté que par le rôle `Employe`.
- **Strategy dispatch "most-specific wins"** : `reduce((a, b) -> a.targetType.isAssignableFrom(b.targetType) ? b : a)` — la sous-classe gagne sur la super-classe. Permet à `UtilisateurPrincipalContextStrategy` d'être un fallback générique (ADMIN) tout en laissant `Proprietaire`/`Employe` matcher en priorité.
- **Seed ERP additif, pas remplaçant** : on ajoute 75 permissions nouvelles à côté des 4 anciennes (PROPRIETAIRE_ACCESS, EMPLOYE_ACCESS, EMPLOYE_CREATE, ADMIN_ACCESS) pour que le code Java actuel (`@PreAuthorize`, `PermissionCode` enum) continue de fonctionner. Le refactor des `@PreAuthorize` vers la nomenclature granulaire (`COMPANY_READ`, `STORE_CREATE`, ...) est reporté à plus tard.
- **Sous-DTO Response (`UserResponse`)** : critères d'extraction = ≥ 3 champs d'une autre entité + sous-objet réutilisable (Utilisateur l'est : Proprietaire, Employe, Admin). Le JSON devient hiérarchique au lieu de plat.
- **`MANAGER` du seed absorbe Magasinier+Comptable** : on n'introduit pas les rôles MAGASINIER/COMPTABLE/CAISSIER/SUPPORT du PDF pour le moment. MANAGER = manager opérationnel + gestion stock + reporting financier.

**Où on s'est arrêté :**

- **152 tests verts** (143 → 152 avec les nouveaux tests strategies)
- Commits dev : `6342cdd` Reorg packages impl + CRUD entreprise, `b55eaed` CRUD entreprise + UserResponse, `d16d6c0` Refactor Strategy pattern UserPrincipal, `f71b148` Seed permissions et rôles ERP
- La BD au boot sera seedée avec 4 rôles + 79 permissions selon la matrice (idempotent)

**Prochaine étape recommandée :**

1. **Migration progressive des `@PreAuthorize`** vers la nomenclature granulaire : remplacer `PROPRIETAIRE_ACCESS` par `COMPANY_READ/UPDATE` + `STORE_*` selon l'endpoint, `EMPLOYE_CREATE` par `USER_CREATE`, etc. Toucher l'enum `PermissionCode` (ajouter les 75 valeurs) et tous les sites d'appel.
2. **CRUD `Product`** — premier module métier post-auth. Suit les patterns établis (Request/Response avec entity constructor, DomainService.create, ServiceImpl + interface, controller avec BASE_PATH, tests unit + slice web). Permission `PRODUCT_CREATE/READ/UPDATE/DELETE` désormais en BD.
3. **Endpoint listing employés** par magasin (manager voit ses employés, owner voit tous).
4. **Frontend** : `src/lib/api/client.ts` + pages `(auth)/login`+`register`.

---

### Session du 2026-05-12 (matinée)

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
