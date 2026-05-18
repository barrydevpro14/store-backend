# FONCTIONNALITIES.md — Récap des services applicatifs métier

> Ce fichier liste, pour chaque service applicatif (use case), l'endpoint exposé, les règles métier appliquées, les dépendances injectées et les exceptions possibles. À mettre à jour à chaque nouveau service.

---

## 1. Inscription d'un propriétaire — `RegisterPropertyServiceImpl`

**Endpoint** : `POST /api/v1/auth/register` (public, pas d'auth)

**Entrée** : `RegisterPropertyRequest`
```json
{
  "account":    { "username", "password" },
  "utilisateur":{ "nom", "prenom", "email", "telephone", "adresse" },
  "entreprise": { "sigle", "raisonSociale", "ninea", "rccm", "adresse" },
  "magasin":    { "nom", "adresse" }
}
```

**Flux** :
1. Charge le rôle `PROPRIETAIRE` via `IRoleService.findByLibelle`.
2. Charge le plan d'essai actif via `IPlanAbonnementService.findFirstTrialActif`.
3. Crée l'`Account` (`IAccountService.create`) + le `Proprietaire` (`IProprietaireService.create`) + l'`Entreprise` (`IEntrepriseService.create`) + le premier `Magasin` (`IMagasinService.create`).
4. Lie : `account.user = proprietaire`, `proprietaire.entreprise = entreprise`, `entreprise.magasins = [magasin]`.
5. Crée un abonnement d'essai via `IAbonnementService.createTrial`.
6. Génère access JWT via `IJwtService.generateToken` + refresh token UUID opaque via `IRefreshTokenService.create`.

**Règles** :
- `account.username` doit être unique (`UniqueResourceException("account.username.exists")`).
- Téléphone obligatoire et au format SN (`@NotBlank @Phone`).
- Rôle `PROPRIETAIRE` doit exister en BDD (seedé par `DataInitializer`).
- Un plan trial actif doit exister.

**Sortie** : `AuthResponse{ accessToken, refreshToken }` — HTTP 201.

---

## 2. Connexion — `LoginServiceImpl`

**Endpoint** : `POST /api/v1/auth/login` (public)

**Entrée** : `LoginRequest{ username, password }`

**Flux** :
1. `AuthenticationManager.authenticate(...)` vérifie credentials via `UserDetailsServiceImpl`.
2. Si OK, recharge l'`Account` via `IAccountService.findByUsername`.
3. Construit `UserPrincipal` via `IUserPrincipalFactory.build(account)`.
4. Génère access JWT + refresh token persisté.

**Règles** :
- `BadCredentialsException` (Spring Security) si mauvais mot de passe.
- `EntityException("account.notFound")` si pas trouvé après auth (cas pathologique).

**Sortie** : `AuthResponse{ accessToken, refreshToken }` — HTTP 200.

---

## 3. Refresh token — `RefreshTokenServiceImpl.refresh`

**Endpoint** : `POST /api/v1/auth/refresh` (public)

**Entrée** : `RefreshTokenRequest{ refreshToken }`

**Flux** :
1. Lookup du refresh token via `RefreshTokenDomainService.findByToken`.
2. Vérif : non‑révoqué, non‑expiré, lié à un `Utilisateur` qui a un `Account`.
3. Reconstruit `UserPrincipal` puis émet un nouvel access JWT (le refresh token reste le même — **pas de rotation**).

**Règles / erreurs** :
- `UnauthorisedException("refreshToken.invalid|revoked|expired")` selon le cas.

**Sortie** : `AuthResponse{ accessToken (nouveau), refreshToken (inchangé) }` — HTTP 200.

---

## 4. Logout — `RefreshTokenServiceImpl.revoke`

**Endpoint** : `POST /api/v1/auth/logout` (public, idempotent)

**Entrée** : `RefreshTokenRequest{ refreshToken }`

**Flux** :
1. Lookup ; si présent et non‑révoqué : `setRevoked(true) + save`.
2. Sinon : silencieux.

**Sortie** : HTTP 204 (toujours).

---

## 5. Création d'un employé — `EmployeServiceImpl.create`

**Endpoint** : `POST /api/v1/employees` (auth requise, `@PreAuthorize("hasAuthority('EMPLOYE_CREATE')")`)

**Entrée** : `EmployeRequest`
```json
{
  "account":     { "username", "password" },
  "utilisateur": { "nom", "prenom", "email", "@Phone telephone", "adresse" },
  "role":        "MANAGER" | "VENDEUR" | "CAISSIER" | "...",
  "magasinId":   "<uuid>"
}
```

**Flux** :
1. Récupère `currentUser` via `ICurrentUserService.getCurrent`.
2. `IRoleService.findByLibelle(askedRole)` (404 si rôle inconnu).
3. `IPermissionsService.findAllByRoleId(role.id)` → codes permissions du rôle.
4. Vérifie que rôle a `EMPLOYE_ACCESS` (sinon 403).
5. Si rôle a `EMPLOYE_CREATE` (rôle "élevé") et caller n'est pas propriétaire → 403.
6. `IMagasinService.findById(magasinId)` + check ownership :
   - Propriétaire : `magasin.entreprise.id == currentUser.entrepriseId`.
   - Sinon (manager) : `magasin.id == currentUser.magasinId`.
7. Si rôle élevé : `EmployeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, "EMPLOYE_CREATE")` → 403 si déjà un manager.
8. `IAccountService.create(account, role)` (vérif username unique).
9. `EmployeDomainService.create(utilisateur, account, magasin)` construit + sauve l'`Employe`, lie `account.user`, retourne un `EmployeResponse`.

**Règles métier** :
| Règle | Mécanisme |
|---|---|
| Propriétaire peut créer n'importe quel employé | Étapes 5 et 7 ne s'appliquent qu'aux non‑propriétaires |
| Manager peut créer tout sauf un autre manager | Étape 5 (`EMPLOYE_CREATE && !PROPRIETAIRE_ACCESS`) |
| Un seul rôle "élevé" par magasin | Étape 7 (data‑driven sur `EMPLOYE_CREATE`) |
| Propriétaire ne peut créer que dans **son entreprise** | Étape 6 |
| Manager ne peut créer que dans **son magasin** | Étape 6 |
| Le rôle créé doit être un rôle d'employé | Étape 4 |

**Exceptions** :
- `ForbiddenException("employe.create.role.notAllowed")`, `("employe.create.elevatedRole.forbidden")`, `("magasin.notOwned")`, `("magasin.alreadyHasManager")`.
- `EntityException("role.notFound" | "entity.notFound")`.
- `UniqueResourceException("account.username.exists")`.

**Sortie** : `EmployeResponse{ id, nom, prenom, email, telephone, adresse, username, role, magasinId }` — HTTP 201.

---

## 6. CRUD Magasin + activate/deactivate — `MagasinServiceImpl`

**Endpoints** (`@PreAuthorize("hasAuthority('PROPRIETAIRE_ACCESS')")` au niveau classe) :

| Méthode | Endpoint | Action |
|---|---|---|
| `POST` | `/api/v1/magasins` | Crée un magasin dans l'entreprise du caller (201) |
| `GET` | `/api/v1/magasins?page=0&size=10&sort=nom,asc` | Liste paginée des magasins de l'entreprise courante (200, `Page<MagasinResponse>`) |
| `GET` | `/api/v1/magasins/{id}` | Lit un magasin (200) |
| `PUT` | `/api/v1/magasins/{id}` | Met à jour `nom`/`adresse` (200) |
| `PATCH` | `/api/v1/magasins/{id}/activate` | Soft‑activate (`actif=true`) (200) |
| `PATCH` | `/api/v1/magasins/{id}/deactivate` | Soft‑delete (`actif=false`) (200) |

**`MagasinRequest`** : `@NotBlank nom`, `@NotBlank adresse`.

**`MagasinResponse`** : `id, nom, adresse, actif, entrepriseId`. Constructeur secondaire `(Magasin)`.

**Champ `actif`** : flag visuel uniquement (pas d'effet métier en MVP — n'empêche pas les ventes/achats sur ce magasin). Ajouté via `V2__add_magasin_actif.sql` (`BOOLEAN NOT NULL DEFAULT TRUE`).

**Règle métier (toutes opérations)** : `ensureBelongsToCurrentEntreprise(magasin)` — `magasin.entreprise.id == currentUser.entrepriseId` sinon `ForbiddenException("magasin.notOwned")`.

**Pagination** : query JPQL custom dans `MagasinRepository.findResponsesByEntrepriseId` — projection directe vers `MagasinResponse` via `SELECT new ... MagasinResponse(...)`, `countQuery` séparée. Évite de matérialiser les entités complètes.

**Dépendances injectées** dans `MagasinServiceImpl` :
| Dépendance | Pourquoi |
|---|---|
| `MagasinDomainService` | CRUD Magasin + query projetée paginée |
| `IEntrepriseService` | Chargement de l'entreprise du caller (`findById`) |
| `ICurrentUserService` | `UserPrincipal` du SecurityContext |

**`IMagasinService.create(MagasinRequest, Entreprise) → Magasin`** : signature interne conservée (utilisée par le flow d'inscription propriétaire) ; le controller utilise la nouvelle `create(MagasinRequest) → MagasinResponse` qui scope automatiquement sur l'entreprise du caller.

---

## 7. CRUD Entreprise + activate/deactivate — `EntrepriseServiceImpl` + `RegisterPropertyServiceImpl.registerEntrepriseByAdmin`

**Endpoints** (`@PreAuthorize` au niveau **méthode**, permissions mixtes) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/entreprises` | `ADMIN_ACCESS` | Crée une entreprise complète (account + proprietaire + entreprise + magasin + trial). Délègue à `IRegisterPropertyService.adminCreate` (même flow que `/auth/register` mais sans génération de tokens) (201) |
| `GET` | `/api/v1/entreprises?page=&size=` | `ADMIN_ACCESS` | Liste paginée toutes entreprises (200, `Page<EntrepriseResponse>`) |
| `GET` | `/api/v1/entreprises/{id}` | `ADMIN_ACCESS` | Read par id (200) |
| `PATCH` | `/api/v1/entreprises/{id}/activate` | `ADMIN_ACCESS` | Soft‑activate (200) |
| `PATCH` | `/api/v1/entreprises/{id}/deactivate` | `ADMIN_ACCESS` | Soft‑delete (200) |
| `GET` | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | Proprietaire lit sa propre entreprise (200) |
| `PUT` | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | Proprietaire modifie infos de sa propre entreprise (200) |

**`EntrepriseRequest`** : `@NotBlank sigle`, `@NotBlank raisonSociale`, `ninea`, `rccm`, `adresse`.

**`EntrepriseResponse`** : `id, sigle, raisonSociale, ninea, rccm, adresse, actif, trialUsed`. Constructeur secondaire `(Entreprise)`.

**Champ `actif`** : flag visuel uniquement (pas d'effet métier en MVP). Ajouté via `V3__add_entreprise_actif.sql` (`BOOLEAN NOT NULL DEFAULT TRUE`).

**Référentiel** :
- Permission `ADMIN_ACCESS` (enum `PermissionCode`).
- Rôle `ADMIN` seedé (`DataInitializer`). ⚠️ Aucun compte ADMIN auto‑créé — à provisionner manuellement en BDD.

**Création admin (POST)** : le controller délègue à `IRegisterPropertyService.registerEntrepriseByAdmin(RegisterPropertyRequest)` qui retourne directement un `EntrepriseResponse`. Cette méthode appelle `createAccount(request, "PROPRIETAIRE")` (orchestration commune : Account + Proprietaire + Entreprise + Magasin + abonnement trial) puis extrait `proprietaire.getEntreprise()` et renvoie le DTO. ADMIN n'obtient pas de tokens — le nouveau proprietaire devra `POST /auth/login` pour se connecter.

> **Pourquoi la méthode vit dans `IRegisterPropertyService`** : `RegisterPropertyServiceImpl` injecte déjà `IEntrepriseService` pour orchestrer l'inscription. Mettre la méthode dans `IEntrepriseService` créerait un cycle de DI. La règle "le service qui orchestre déjà héberge la méthode" tranche.

---

## 8. Strategy `UserPrincipalContextStrategy` — composition de UserPrincipal

**Pas un endpoint** — pattern interne invoqué par `UserPrincipalFactoryImpl.build(Account)`.

**Package** : `org.store.security.application.strategies`

**Interface** :
```java
public interface UserPrincipalContextStrategy {
    Class<? extends Utilisateur> targetType();
    UserPrincipalContext resolve(Utilisateur user);
}
```

**Record retour** : `UserPrincipalContext(UUID entrepriseId, UUID magasinId)` (+ `empty()` statique).

**Trois implémentations** (`@Component`) :
- `ProprietairePrincipalContextStrategy` (targetType = `Proprietaire`) → `(entreprise.id, null)`. Un OWNER n'est pas rattaché à un magasin précis.
- `EmployePrincipalContextStrategy` (targetType = `Employe`) → `(magasin.entreprise.id, magasin.id)`.
- `UtilisateurPrincipalContextStrategy` (targetType = `Utilisateur`) → fallback `empty()` (ADMIN typiquement).

**Dispatch dans `UserPrincipalFactoryImpl`** (zéro `instanceof`, "most-specific wins") :
```java
strategies.stream()
    .filter(s -> s.targetType().isInstance(user))
    .reduce((a, b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)
    .map(s -> s.resolve(user))
    .orElseGet(UserPrincipalContext::empty);
```

**Règle générale** : tout dispatch par sous-type d'entité doit suivre ce pattern (cf. ARCHITECTURE.md règle 28).

---

## 9. Seed permissions et rôles ERP — `DataInitializer`

**Pas un endpoint** — bootstrap idempotent au démarrage de l'app (`ApplicationRunner`).

**Référence** : `src/main/resources/static/liste_roles_permissions.md` + matrice du PDF `Roles Permissions Erp Saas.pdf` (archivé hors repo).

**Rôles seedés** (4) :
| Code | Description | Notes |
|---|---|---|
| `ADMIN` | Administrateur SaaS | Toutes les 79 permissions (= `SUPER_ADMIN` sémantiquement) |
| `PROPRIETAIRE` | Propriétaire d'une entreprise | ≈ `OWNER` du PDF — toutes permissions sauf `COMPANY_CREATE`/`DELETE` |
| `MANAGER` | Manager d'un magasin | Absorbe MANAGER + MAGASINIER + COMPTABLE du PDF |
| `VENDEUR` | Vendeur d'un magasin | `AUTH_*`, `SALE_*`, `PAYMENT_CREATE/READ`, `PRODUCT_READ`, `STOCK_READ` |

**Permissions seedées** (79) :
- **4 anciennes conservées** pour compat code : `PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, `ADMIN_ACCESS` (référencées par `@PreAuthorize` actuels).
- **75 nouvelles** au format `MODULE_ACTION` : `AUTH_*` (5), `USER_*` (7), `COMPANY_*` (4), `STORE_*` (5), `PRODUCT_*` (7), `STOCK_*` (6), `PURCHASE_*` (6), `SALE_*` (6), `EXPENSE_*` (5), `PAYMENT_*` (4), `SUBSCRIPTION_*` (4), `DOCUMENT_*` (4), `DASHBOARD_READ + REPORT_*` (5), `SETTINGS_*` (2), `AUDIT_*` (2).

**Idempotence** : `findByCode` / `findByLibelle` ; ajoute uniquement les permissions manquantes au rôle. Sûr à re-exécuter.

**Dette assumée** : le code Java actuel (`@PreAuthorize('PROPRIETAIRE_ACCESS')`, etc.) référence encore les 4 anciennes permissions. La migration vers la nomenclature granulaire (`COMPANY_READ`, `STORE_CREATE`, ...) est partielle : l'enum `PermissionCode` contient désormais 16 valeurs (4 legacy + 12 granulaires `CATEGORY_PRODUCT_*` / `QUALITY_*` / `PRODUCT_*` utilisées par les controllers produits). Les 70+ permissions YAML restantes ne sont pas encore typées en enum.

---

## 10. CRUD Catégorie de produit — `CategoryProductServiceImpl`

**Endpoints** (`/api/v1/category-products`, permissions granulaires) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/category-products` | `CATEGORY_PRODUCT_CREATE` | Crée une catégorie pour l'entreprise du caller (201) |
| `GET` | `/api/v1/category-products?page=&size=` | `CATEGORY_PRODUCT_READ` | Liste paginée des catégories de l'entreprise courante (200) |
| `GET` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_READ` | Lit une catégorie (200, scopée) |
| `PUT` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_UPDATE` | Met à jour `libelle`/`description` (200) |
| `DELETE` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_DELETE` | Supprime (204) |

**`CategoryProductRequest`** : `@NotBlank @Size(max=255) libelle`, `@Size(max=255) description`.

**`CategoryProductResponse`** : `id, libelle, description, entrepriseId`. Constructeur secondaire `(CategoryProduct)`.

**Scoping multi-tenant** : FK `@ManyToOne Entreprise entreprise` sur l'entité (migration `V4__add_entreprise_to_category_quality.sql`). `ensureBelongsToCurrentEntreprise(category)` sur toutes les opérations (`categoryProduct.notOwned`).

**Unicité libellé par entreprise** : `existsByLibelleAndEntrepriseId` avant create. À l'update : vérification skippée si le libellé n'a pas changé.

**Pagination** : `SELECT new CategoryProductResponse(c) FROM CategoryProduct c WHERE c.entreprise.id = :entrepriseId` (projection JPQL via le constructeur secondaire).

**Dépendances** : `CategoryProductDomainService`, `IEntrepriseService`, `ICurrentUserService`.

---

## 11. CRUD Qualité — `QualityServiceImpl`

**Endpoints** (`/api/v1/qualities`) — structure identique à CategoryProduct, permissions `QUALITY_*`. Mêmes règles de scoping, mêmes patterns Request/Response/projection/unicité. Voir section 10.

---

## 12. CRUD Produit — `ProductServiceImpl`

**Endpoints** (`/api/v1/products`) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/products` | `PRODUCT_CREATE` | Crée un produit dans l'entreprise du caller (201) |
| `GET` | `/api/v1/products?page=&size=` | `PRODUCT_READ` | Liste paginée des produits de l'entreprise (200) |
| `GET` | `/api/v1/products/{id}` | `PRODUCT_READ` | Lit un produit (200, scopé) |
| `PUT` | `/api/v1/products/{id}` | `PRODUCT_UPDATE` | Met à jour nom/reference/description + catégorie/qualité (200) |
| `DELETE` | `/api/v1/products/{id}` | `PRODUCT_DELETE` | Supprime (204) |

**`ProductRequest`** : `@NotBlank @Size(max=255) nom`, `@NotBlank @Size(max=255) reference`, `@Size(max=1000) description`, `@NotNull UUID categoryProductId`, `@NotNull UUID qualityId`.

**`ProductResponse`** (sous-DTOs imbriqués — règle 23) : `id, nom, reference, description, CategoryProductResponse category, QualityResponse quality, entrepriseId, UUID imagePrincipalId`. Constructeur secondaire `(Product)`. `imagePrincipalId` = id de la `PieceJointe` ou `null` si pas d'image principale.

**Règles métier** :

| Règle | Mécanisme |
|---|---|
| Le produit appartient à l'entreprise du caller | `ensureBelongsToCurrentEntreprise(product)` sur read/update/delete |
| La `categoryProductId` doit appartenir à la même entreprise | `categoryProductService.ensureBelongsToCurrentEntreprise(...)` à create/update |
| La `qualityId` doit appartenir à la même entreprise | `qualityService.ensureBelongsToCurrentEntreprise(...)` à create/update |
| `reference` unique par entreprise | `existsByReferenceAndEntrepriseId` à create, skippé si inchangé en update |

**`@Transactional(readOnly = true)` au niveau classe** (lectures), override `@Transactional` sur les mutations. Permet aux projections JPQL lazy d'accéder à `categoryProduct`/`quality` lors du mapping en `ProductResponse`.

**Exceptions** :
- `EntityException("product.notFound" | "categoryProduct.notFound" | "quality.notFound")`.
- `ForbiddenException("product.notOwned" | "categoryProduct.notOwned" | "quality.notOwned")`.
- `UniqueResourceException("product.reference.alreadyExists")`.

**Dépendances** : `ProductDomainService`, `ICategoryProductService`, `IQualityService`, `IEntrepriseService`, `ICurrentUserService`, `IUploadFileService` (pour les endpoints image).

---

## 13. Upload de fichiers — `UploadFileServiceImpl`

**Pas un endpoint** — service technique transverse dans `org.store.common.service`.

**Interface** :
```java
public interface IUploadFileService {
    PieceJointe buildImage(MultipartFile file);
    List<PieceJointe> buildImages(List<MultipartFile> files);
}
```

**Comportement** :
- Valide non-vacuité du fichier (`upload.file.empty`).
- Valide MIME image contre `UploadProperties.allowedImageTypes` (configurable, par défaut `image/{jpeg,png,webp,gif}`). Erreur : `upload.file.invalidImageType`.
- Wrap `IOException` (lecture des bytes) en `BadArgumentException("upload.file.readFailed")`.
- Construit une `PieceJointe` non persistée avec `document=bytes`, `date=LocalDate.now()`, `contentType=file.getContentType().toLowerCase()`.
- `buildImages` : valide non-vide (`upload.files.empty`), puis applique `buildImage` en boucle (s'arrête au premier invalide).

**Configuration externe** (`record UploadProperties(Set<String> allowedImageTypes)`, `@ConfigurationProperties("upload")`) :
```yaml
upload:
  allowed-image-types:
    - image/jpeg
    - image/png
    - image/webp
    - image/gif
```
Constructeur compact qui normalise en minuscules + rend immutable. Modification = changement YAML, **zéro Java**.

**Multipart** (`application.yml`) : `spring.servlet.multipart.{enabled=true, max-file-size=5MB, max-request-size=6MB}`.

---

## 14. Image principale et galerie produit — `ProductServiceImpl` (suite)

**Endpoints image** :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `PUT` | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | Upload (remplace) l'image principale, body multipart `file` (200, `ProductResponse`) |
| `DELETE` | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | Supprime l'image principale (idempotent, 204) |
| `GET` | `/api/v1/products/{id}/image` | `PRODUCT_READ` | Sert le blob avec le bon `Content-Type` (200, `byte[]`) |
| `POST` | `/api/v1/products/{id}/images` | `PRODUCT_UPLOAD_IMAGE` | Upload cumulatif de plusieurs images dans la galerie, body multipart `files` (201, `List<UUID>`) |
| `GET` | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_READ` | Sert le blob d'une image de la galerie (200, `byte[]`) |
| `DELETE` | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_UPLOAD_IMAGE` | Retire une image de la galerie (orphanRemoval purge la `PieceJointe`) (204) |

**Modèle** :
- `Product.imagePrincipal` — `@OneToOne(fetch=LAZY, cascade=ALL, orphanRemoval=true) PieceJointe`. Migration `V5__add_image_principal_to_product.sql`.
- `Product.images` — `@OneToMany(fetch=LAZY, cascade=ALL, orphanRemoval=true) List<PieceJointe>` (galerie indépendante de `imagePrincipal`).
- `PieceJointe.contentType` — `@Column(name="content_type", nullable=false, length=100) String` (migration `V6__add_content_type_to_piece_jointe.sql`). Rempli à l'upload depuis `file.getContentType()` (lowercased). **Source de vérité** pour servir l'image avec le bon `Content-Type`.

**DTO retour** (visualisation) — `ImageDownloadResponse(byte[] content, String contentType)` dans `common/dto`. Le controller transforme en `ResponseEntity.ok().contentType(MediaType.parseMediaType(...)).body(...)`.

**Règles** :
- `ensureBelongsToCurrentEntreprise(product)` sur toutes les opérations.
- `getImagePrincipal` lève `EntityException("product.image.notFound")` si pas d'image.
- `getImage` / `deleteImage` lèvent `EntityException("product.image.galleryImageNotFound")` si imageId pas dans la galerie du product.
- Le `setImagePrincipal(product, null)` ou `removeImage(product, image)` déclenche la suppression effective de la `PieceJointe` via `orphanRemoval=true`.

**Performance** : la visualisation lit directement `pieceJointe.getContentType()` (champ stocké), pas de détection runtime. Extensible à tout MIME (PDF, factures, docs) sans modif code Java.

**Limitations actuelles** :
- Pas d'endpoint listing de la galerie (`GET /{id}/images` retournant la liste des métadonnées). Le client doit connaître les ids à l'avance (retournés à l'upload).
- Pas de validation de taille applicative (s'appuie sur la limite Spring `max-file-size=5MB`, renvoie 400 `MaxUploadSizeExceededException` au-delà — handler i18n custom à ajouter si besoin).

---

## 15. Listing galerie produit — `ProductServiceImpl.listImages`

**Endpoint** : `GET /api/v1/products/{id}/images` (permission `PRODUCT_READ`)

**Sortie** : `List<ImageMetadataResponse{id, date, contentType, url}>`. Le champ `url` est un path relatif `/api/v1/products/{productId}/images/{imageId}` directement utilisable côté front (`<img src={img.url}>`), pas besoin de connaître la convention de routing.

**DTO** `ImageMetadataResponse` dans `produit/application/dto/` (le path produit est dans l'URL → DTO spécifique au module). 2 constructeurs :
- `(PieceJointe, UUID productId)` pour une image de la galerie (`/images/{imageId}`).
- factory `forPrincipal(PieceJointe, UUID productId)` pour l'image principale (`/image`) — **non câblée dans ce passage** mais l'utilisateur a préféré exposer directement l'URL via `ProductResponse.image`.

**Règle** : `IProductService.listImages(UUID)` scopé via `ensureBelongsToCurrentEntreprise`. Pas de pagination (la galerie est rarement énorme — à voir si > 100 images).

**Limite actuelle** : pas d'endpoint d'ordonnancement / réordonnancement de la galerie. Les images sont retournées dans l'ordre d'insertion (FIFO).

---

## 16. CRUD Fournisseur — `FournisseurServiceImpl`

**Endpoints** (`/api/v1/suppliers`) :

| Méthode | Endpoint | Permission |
|---|---|---|
| `POST` | `/api/v1/suppliers` | `SUPPLIER_CREATE` |
| `GET` | `/api/v1/suppliers?page=&size=` | `SUPPLIER_READ` |
| `GET` | `/api/v1/suppliers/{id}` | `SUPPLIER_READ` |
| `PUT` | `/api/v1/suppliers/{id}` | `SUPPLIER_UPDATE` |
| `DELETE` | `/api/v1/suppliers/{id}` | `SUPPLIER_DELETE` |

**`FournisseurRequest`** : `@NotBlank nom`, `prenom`, `@Email email`, `@Phone telephone`, `adresse`, `reference`, `origine`.

**`FournisseurResponse`** : `id, nom, prenom, email, telephone, adresse, reference, origine, entrepriseId`. Constructeur secondaire `(Fournisseur)`.

**Modèle** : `Fournisseur extends Person` (héritage JOINED → table `person` pour les champs nom/prenom/email/telephone/adresse, table `fournisseur` pour `reference` + `origine` + FK `entreprise_id`). Migration `V7__add_entreprise_to_fournisseur.sql` (NOT NULL + FK + index).

**Règles** :
- Scoping `ensureBelongsToCurrentEntreprise(fournisseur)` sur toutes les opérations.
- Unicité de `reference` par entreprise via `existsByReferenceAndEntrepriseId` — **skippée si `null` ou `blank`** pour autoriser les fournisseurs sans code interne.
- En update, l'unicité est revérifiée uniquement si `reference` a changé.

**Dépendances** : `FournisseurDomainService`, `IEntrepriseService`, `ICurrentUserService`.

---

## 17. CRUD ProductFournisseur — `ProductFournisseurServiceImpl`

**Endpoints** (`/api/v1/product-suppliers`, permissions réutilisées `SUPPLIER_*`) :

| Méthode | Endpoint | Action |
|---|---|---|
| `POST` | `/api/v1/product-suppliers` | Crée un lien produit ↔ fournisseur (201) |
| `GET` | `/api/v1/product-suppliers?page=&size=` | Liste tous les liens de l'entreprise (200) |
| `GET` | `/api/v1/product-suppliers?productId={id}` | Liste les fournisseurs d'un produit donné (200) |
| `GET` | `/api/v1/product-suppliers/{id}` | Détail d'un lien (200) |
| `PUT` | `/api/v1/product-suppliers/{id}` | Met à jour prixAchat / referenceFournisseur / origine (200) |
| `DELETE` | `/api/v1/product-suppliers/{id}` | Supprime le lien (204) |

**`ProductFournisseurRequest`** : `@NotNull productId`, `@NotNull fournisseurId`, `@NotNull @DecimalMin("0.0", inclusive=false) prixAchat`, `referenceFournisseur` (max 100), `origine` (max 100).

**`ProductFournisseurResponse`** : `id, ProductSummaryResponse product, FournisseurSummaryResponse fournisseur, prixAchat, referenceFournisseur, origine`. Sous-DTOs imbriqués (règle 23). Constructeur secondaire `(ProductFournisseur)`.

**Sous-DTOs réutilisables** :
- `ProductSummaryResponse(id, nom, reference)` dans `produit/application/dto`
- `FournisseurSummaryResponse(id, nom)` dans `achat/application/dto`

**Modèle** : `ProductFournisseur` enrichi de `referenceFournisseur` (max 100, code interne fournisseur pour ce produit) et `origine` (max 100, pays/marque). `product` et `fournisseur` promus en `optional=false` + colonnes `NOT NULL`. Migration `V8__add_traceability_to_product_fournisseur.sql` (2 `ALTER NOT NULL` + 2 nouvelles colonnes + 2 index).

**Règles** :
- **Scoping cross-entity** : `IProductService.ensureBelongsToCurrentEntreprise(product)` + `IFournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)` à la création. À la lecture, scoping via `product.entreprise.id`.
- Unicité paire `(productId, fournisseurId)` via `existsByProductIdAndFournisseurId` — un même produit ne peut avoir le même fournisseur 2 fois.
- **Update limité** aux champs informationnels (prixAchat, referenceFournisseur, origine). Les FK `product`/`fournisseur` sont **immuables** — pour changer la paire, supprimer + recréer.

**Permissions réutilisées `SUPPLIER_*`** (pas de `PRODUCT_SUPPLIER_*` créées). Qui gère les fournisseurs gère leurs tarifs. À séparer plus tard si besoin d'accès différencié (ex : tarification réservée au comptable).

**Service `@Transactional(readOnly = true)` au niveau classe** pour les lectures (permet aux projections JPQL d'accéder aux sous-relations LAZY pendant le mapping). `@Transactional` override sur les mutations.

**Dépendances** : `ProductFournisseurDomainService`, `IProductService`, `IFournisseurService`, `ICurrentUserService`.

---

## 28. Module Achat — Workflow 2 étapes : DRAFT → VALIDATE — `AchatServiceImpl`

**Refactor 2026-05-18** : la création atomique a été éclatée en 2 étapes pour permettre la **visualisation et l'édition** de la commande avant engagement (matérialisation stock + facture). Symétrique à l'inventaire physique (statuts EN_COURS / BILAN / CLOTURE).

### 28.a Création DRAFT — `POST /api/v1/achats`

**Permission** : `PURCHASE_CREATE` (ADMIN/PROPRIETAIRE/MANAGER).

**Entrée** : `AchatRequest{ magasinId, fournisseurId, dateCommande, lignes[] }` — **plus de `facture`** (saisie à la validation).
```json
{
  "magasinId": "uuid",
  "fournisseurId": "uuid",
  "dateCommande": "2026-05-18",
  "lignes": [
    { "productFournisseurId": "uuid", "quantite": 10, "prixAchat": 100.00, "prixVente": 150.00,
      "numeroLot": "LOT-A", "dateExpiration": "2027-06-01" }
  ]
}
```

**Flux** :
1. Validations PF (scoping entreprise + cohérence fournisseur + `prixVente > prixAchat`).
2. Crée `CommandeAchat` en statut `DRAFT` (référence auto `CMD-yyyyMMdd-HHmmssSSS`).
3. Persiste chaque `LigneCommandeAchat` avec **snapshot** prix + traçabilité lot (numeroLot + dateExpiration, migrés sur la table — V23).
4. **Pas de facture, pas d'entrée stock, pas d'update PF prixVente.**

**Sortie** : `AchatDraftResponse{ commande }` — HTTP 201. La commande peut alors être consultée (`GET /{id}`), éditée ligne par ligne ou validée.

### 28.b Édition / suppression de ligne — `PUT/DELETE /api/v1/achats/orders/{commandeId}/lignes/{ligneId}`

**Permissions** : `PURCHASE_UPDATE` (PUT) / `PURCHASE_DELETE` (DELETE).

**Garde** : `ensureCommandeIsDraft` — interdit toute modification dès que la commande passe en RECEPTIONNEE. `ensureLigneBelongsToCommande` (anti URL forgée) + `ensureNotLastLigne` (commande vide non autorisée).

**PUT body** : `LigneAchatUpdateRequest{ quantite, prixAchat, prixVente, numeroLot?, dateExpiration? }`. Re-validation `prixVente > prixAchat`. Retourne le `LigneCommandeAchatResponse` mis à jour.

**DELETE** : 204. Refuse si c'est la dernière ligne (`commandeAchat.cannotDeleteLastLigne`).

### 28.c Validation — `POST /api/v1/achats/{commandeId}/validate`

**Permission** : `PURCHASE_APPROVE`.

**Entrée** : `AchatValidateRequest{ facture: { numero, date, dateEcheance } }`.

**Flux atomique (transaction unique)** :
1. Validations : `ensureBelongsToCurrentEntreprise` + `ensureCommandeIsDraft`.
2. Recalcule `montantTotal = SUM(qty × prixAchat)` depuis les lignes courantes (peuvent avoir été éditées).
3. Crée `FactureAchat` liée à la commande (statut NON_PAYEE).
4. Pour chaque ligne : crée `EntreeStock` (lot FIFO avec numéro lot + dateExpiration), upsert `Stock` agrégé, journalise `MouvementStock(ENTREE_ACHAT)`, applique `pf.prixVente = ligne.prixVente` (déplacé depuis create — sinon un draft non validé contaminerait le prix de vente courant).
5. Bascule `commande.statut → RECEPTIONNEE` via `commandeAchatDomainService.validate` (règle 26).

**Sortie** : `AchatResponse{ commande (statut RECEPTIONNEE), facture }` — HTTP 200.

### 28.d Détail — `GET /api/v1/achats/{commandeId}`

**Permission** : `PURCHASE_READ`. Retourne `AchatDetailsResponse(commande, facture (null si DRAFT), lignes[])`.

### Migration BDD V23

`ligne_commande_achat` : `+numero_lot VARCHAR(100)`, `+date_expiration DATE`. Auparavant ces infos étaient transmises directement de `LigneAchatRequest` à `EntreeStockCreate` au moment de la création atomique ; avec la séparation DRAFT/VALIDATE, la traçabilité doit être persistée sur la ligne entre les 2 phases.

**Sous-services exposés** :
- `ICommandeAchatService` : `findResponsesByFilter` (listing paginé), `findResponseById`.
- `IFactureAchatService` : idem + `findEcheances(FactureAchatEcheanceFilter)` (factures impayées par fenêtre temporelle).
- `IPaiementAchatService` : `addPayment(factureId, PaiementAchatRequest)` (vérifie `montantRestant`).

**Endpoints additionnels** :
| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `GET` | `/api/v1/purchases/orders?magasinId=&fournisseurId=&startDate=&endDate=&page=&size=` | `PURCHASE_READ` | Liste les commandes filtrées |
| `GET` | `/api/v1/purchases/orders/{id}` | `PURCHASE_READ` | Détail commande |
| `GET` | `/api/v1/purchases/invoices?...` | `PURCHASE_READ` | Liste les factures filtrées |
| `GET` | `/api/v1/purchases/invoices/echeances?fromDate=&toDate=&page=&size=` | `PURCHASE_READ` | Factures impayées (avec dateEcheance dans la fenêtre) |
| `GET` | `/api/v1/purchases/invoices/{id}` | `PURCHASE_READ` | Détail facture |
| `POST` | `/api/v1/purchases/invoices/{id}/payments` | `PAYMENT_CREATE` | Ajoute un paiement à une facture |
| `GET` | `/api/v1/purchases/invoices/{id}/payments` | `PAYMENT_READ` | Liste les paiements d'une facture |

**Records `<X>Create`** (regroupement params >3, règle 30) :
- `FactureAchatCreate(numero, date, dateEcheance)`
- `LigneCommandeCreate(productFournisseur, quantite, prixUnitaire)`
- `PaiementAchatCreate(facture, montant, modePaiement)`
- Filtres : `CommandeAchatFilter`, `FactureAchatFilter`, `FactureAchatEcheanceFilter` (validés par `ValidatorService`).

**Helper transverse** : `ReferenceHelper.generate(String base)` dans `org.store.common.tools` — retourne `"{base}-yyyyMMdd-HHmmssSSS"`. Utilisé pour références commande, à réutiliser pour futures références (facture vente, etc.).

**Permissions** :
- `PURCHASE_CREATE` / `PURCHASE_READ` (ADMIN/PROPRIETAIRE/MANAGER).
- `PAYMENT_CREATE` / `PAYMENT_READ` (mêmes rôles).

**Règles métier** :
- Pas de réception partielle : 1 livraison = 1 commande + 1 facture + N lignes saisies en une fois.
- `montantFacture` et `montantAccompte` **calculés** (jamais saisis directement).
- `PaiementAchat.montant` ne peut pas dépasser `montantFacture - somme_paiements_existants` (`BadArgumentException("paiementAchat.montant.exceedsRemaining")`).

**Tests refactor 2026-05-18** : suite **725 / 725 verts** (+14 vs 711). 13 service `AchatServiceImplTest` (create DRAFT + validate + updateLigne + deleteLigne + findDetails) + 8 controller (création DRAFT 201, validate 200, update ligne 200, delete 204, 400 sur edge cases).

---

## 29. Module Dépense — CRUD CategoryDepense + Depense — `CategoryDepenseServiceImpl`, `DepenseServiceImpl`

### 29.a CategoryDepense — référentiel scopé par entreprise

**Endpoints** (`/api/v1/expense-categories`) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/expense-categories` | `EXPENSE_CATEGORY_CREATE` | Crée une catégorie (201) |
| `GET` | `/api/v1/expense-categories?page=&size=` | `EXPENSE_CATEGORY_READ` | Liste paginée (200) |
| `GET` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_READ` | Détail (200) |
| `PUT` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_UPDATE` | Mise à jour (200) |
| `DELETE` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_DELETE` | Suppression (204) |

**`CategoryDepenseRequest`** : `@NotBlank @Size(max=100) nom`, `@Size(max=500) description`, `Boolean actif` (default true).

**`CategoryDepenseResponse`** : `id, nom, description, actif`. Constructeur secondaire `(CategoryDepense)`.

**Modèle** : `CategoryDepense` enrichi d'un `@ManyToOne(optional=false) Entreprise entreprise`. Migration **V10** : ajout FK `entreprise_id NOT NULL` + remplacement de l'unicité globale auto-générée sur `nom` par une unicité `(entreprise_id, nom)` (via bloc PostgreSQL `DO $$ ... $$` pour drop la contrainte auto-nommée Hibernate).

**Règles** :
- Scoping entreprise via `ICurrentUserService` (manager OU propriétaire).
- Unicité `nom` par entreprise (`UniqueResourceException("categoryDepense.nom.alreadyExists")`).
- Cross-tenant interdit : `ensureBelongsToCurrentEntreprise(category)` (sinon `ForbiddenException("categoryDepense.notOwned")`).

---

### 29.b Depense — opération scopée par magasin

**Endpoints** (`/api/v1/depenses`) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/depenses` | `EXPENSE_CREATE` | Crée une dépense (201) |
| `GET` | `/api/v1/depenses?magasinId=&categoryId=&modePaiement=&startDate=&endDate=&page=&size=` | `EXPENSE_READ` | Liste paginée filtrée (200) |
| `GET` | `/api/v1/depenses/total?magasinId=&categoryId=&modePaiement=&startDate=&endDate=` | `EXPENSE_READ` | Somme agrégée + nombre (200) |
| `GET` | `/api/v1/depenses/{id}` | `EXPENSE_READ` | Détail (200) |
| `PUT` | `/api/v1/depenses/{id}` | `EXPENSE_UPDATE` | Mise à jour (200) |
| `DELETE` | `/api/v1/depenses/{id}` | `EXPENSE_DELETE` | Suppression (204) |

**`DepenseRequest`** : `@NotNull UUID magasinId`, `@NotNull UUID categoryId`, `@NotBlank @Size(max=200) libelle`, `@Size(max=1000) description`, `@NotNull LocalDate dateDepense`, `@NotNull @DecimalMin("0.0", inclusive=false) BigDecimal montant`, `@NotNull MoyenPaiement modePaiement`.

**`DepenseResponse`** : `id, MagasinSummaryResponse magasin, CategoryDepenseSummaryResponse category, libelle, description, dateDepense (String), montant, modePaiement, createdAt (String)`. Sous-DTOs Summary. Dates formatées via `DateHelper.format()`.

**`DepenseFilter`** (record validé par `ValidatorService`, règle 30) : `@NotNull UUID magasinId`, `UUID categoryId`, `@EnumValue(MoyenPaiement.class) String modePaiement`, `@DatePattern("yyyy-MM-dd") String startDate`, `@DatePattern("yyyy-MM-dd") String endDate`, `@Min(0) int page`, `@Min(1) @Max(100) int size`. Méthodes utilitaires : `modePaiementAsEnum()`, `startDateTime()`, `endDateTime()`, `toPageable()`.

**`DepenseTotalResponse`** : `magasinId, BigDecimal montantTotal, Long nombreDepenses`.

**Modèle** : `Depense` lié à `@ManyToOne Magasin` + `@ManyToOne CategoryDepense`. Migration **V10** : ajout colonne `mode_paiement VARCHAR(20) NOT NULL DEFAULT 'CASH'` + `DROP COLUMN IF EXISTS date_echeance` (décision métier : une dépense est ponctuelle, pas une dette à échéance).

**Règles** :
- Scoping magasin : `magasinService.ensureAccessibleByCurrentUser(magasin)` à la création / lecture / update / delete (`ForbiddenException("magasin.notOwned")`).
- Scoping cross-entité : `categoryDepenseService.ensureBelongsToCurrentEntreprise(category)` (`ForbiddenException("categoryDepense.notOwned")`).
- Filtre listing : `magasinId` obligatoire, autres facultatifs. Query JPQL avec SpEL `:#{#filter.X}`.

**Tests** : 15 nouveaux tests (controllers + service impls). Suite à **392 / 392 verts**.

---

## 30. Client (vente, fonctionnalité 1) — `ClientServiceImpl`

**Endpoints** (`/api/v1/clients`) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/clients` | `CLIENT_CREATE` | Crée un client rattaché à un magasin accessible par le caller (201) |
| `GET` | `/api/v1/clients?nom=&prenom=&page=&size=` | `CLIENT_READ` | Liste paginée scopée (employé = magasin, propriétaire = entreprise) avec filtres nom/prénom optionnels (200) |
| `GET` | `/api/v1/clients/{id}` | `CLIENT_READ` | Détail (200) |
| `PUT` | `/api/v1/clients/{id}` | `CLIENT_UPDATE` | Mise à jour (changement de magasin autorisé si nouveau magasin accessible) (200) |
| `DELETE` | `/api/v1/clients/{id}` | `CLIENT_DELETE` | Suppression (204) |

**`ClientRequest`** : `@NotBlank @Size(max=255) nom`, `@Size(max=255) prenom`, `@Email @Size(max=255) email`, `@Phone @Size(max=30) telephone`, `@Size(max=255) adresse`, `@NotNull UUID magasinId`.

**`ClientResponse`** : `id, nom, prenom, email, telephone, adresse`. Pas d'exposition du magasin ni de l'entreprise (scoping invisible côté client). Constructeur secondaire `(Client)`.

**`ClientSummaryResponse(id, nomComplet)`** — sous-DTO réutilisable. `nomComplet = "nom prenom"` ou juste `nom` si prénom blank. Sera utilisé dans la future `SaleResponse`.

**`ClientFilter`** (record validé par `ValidatorService`, règle 30 + règle "DTO Filter ≥ 2 critères") : `String nom`, `String prenom`, `@Min(0) int page`, `@Min(1) int size`. Méthode utilitaire : `toPageable()`. Gestion null directement en JPQL via `(:nom IS NULL OR LOWER(c.nom) LIKE ...)`.

**Modèle** : `Client extends Person` (nom/prénom/email/téléphone/adresse hérités) + `@ManyToOne Magasin magasin`. Pas de migration (table `client` existait déjà depuis `V1__init_schema.sql`).

**Règles** :
- Scoping double via `ensureAccessibleByCurrentUser(Client)` : employé = `client.magasin.id == currentUser.magasinId`, propriétaire = `client.magasin.entreprise.id == currentUser.entrepriseId`. Sinon `ForbiddenException("client.notOwned")`.
- Magasin cible vérifié à la création / update via `IMagasinService.ensureAccessibleByCurrentUser` (cross-service, `ForbiddenException("magasin.notOwned")` si le caller n'y a pas accès).
- Aucune unicité sur `telephone` (homonymes acceptables en boutique de pièces détachées).
- Listing : la sélection magasin (employé) vs entreprise (propriétaire) est interne au service ; aucune option exposée côté API.

**Décision projet liée** : le "client anonyme" n'est PAS un enregistrement Client — quand on attaquera F-V3 (vente atomique), `CommandeVente.client` sera simplement nullable si le vendeur ne saisit pas de client.

**Tests** : 15 service + 9 controller. Suite à **415 / 415 verts**.

---

## 31. Recherche produit vendeur (vente, fonctionnalité 2) — `ProductSearchServiceImpl` + adaptations modèle

**Changements de modèle structurants (migration V11)** :
- **`Quality` déplacée de `Product` vers `ProductFournisseur`** : un même produit peut être livré par un fournisseur en plusieurs qualités distinctes (ex. *Clou 10mm* × *Chine* en *original* ET en *contrefaçon* = 2 PF différents).
- **Unicité PF** : `UNIQUE (product_id, fournisseur_id, quality_id)`.
- **`ProductFournisseur.prixVente`** : prix de vente courant pour la combinaison (produit, fournisseur, qualité). Mis à jour à chaque achat ou via endpoint PUT dédié.
- **`LigneCommandeAchat.prixVente`** : snapshot du prix de vente au moment de l'achat (traçabilité facture).

**Validation métier** : `prixVente > prixAchat` (marge strictement positive) à toute saisie (création PF, achat, PUT prix-vente). Clé i18n `productFournisseur.prixVente.belowOrEqualAchat`.

**Endpoints `ProductController`** :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `GET` | `/api/v1/products/search?q=&magasinId=&page=&size=` | `PRODUCT_READ` | Recherche produits avec lots actifs dans un magasin (200) |

**Endpoints `ProductFournisseurController`** (en plus du CRUD) :

| Méthode | Endpoint | Permission | Action |
|---|---|---|---|
| `PUT` | `/api/v1/product-suppliers/{id}/prix-vente` | `SUPPLIER_UPDATE` | Met à jour librement le prix de vente courant du PF (manager). Validation `> prixAchat` (200) |

**`ProductSearchResponse`** : `id, nom, reference, description, category, image, quantiteEnStock, productFournisseurs[]`.

**`ProductFournisseurStockResponse`** (sous-DTO) : `id, quality, fournisseur, prixVente, quantiteEnStock` (= SUM des lots actifs du PF dans le magasin).

**Résolution `magasinId`** :
- EMPLOYE : si paramètre absent, dérivé automatiquement de `UserPrincipal.magasinId`.
- PROPRIETAIRE : paramètre obligatoire (sinon `BadArgumentException("product.search.magasinIdRequired")`).

**Architecture** :
- Service dédié `IProductSearchService` (isolé pour casser le cycle `IProductService` ↔ `IEntreeStockService` ↔ `IProductFournisseurService`).
- 2 queries pour éviter N+1 : (1) `Page<Product>` paginée par recherche + EXISTS lot actif ; (2) liste des `EntreeStock` actifs pour les IDs paginés, fetch joints PF/fournisseur/quality. Agrégation par produit puis par PF en Java.

**Mise à jour automatique du `prixVente` du PF à chaque achat** : `AchatServiceImpl.createLignesAndComputeTotal()` appelle `IProductFournisseurService.applyPrixVenteFromPurchase(pf, ligne.prixVente())` après création de la ligne.

**Tests** : 5 service search + 1 controller search + tests `updatePrixVente` + adaptations existantes. Total cible : **425 verts** (l'unique test KO est `StoreApplicationTests.contextLoads` à cause d'un état Flyway *failed* à nettoyer manuellement après une première tentative de V11).

---

## 32. Vente atomique (vente, fonctionnalité 3) — `VenteServiceImpl`

> ⚠️ **Refactor 2026-05-18 — Workflow 2 étapes DRAFT → VALIDATE** : la création atomique a été éclatée en 2 étapes pour permettre la visualisation + l'édition d'une vente avant encaissement. **Voir section 48 pour le nouveau workflow** (cette section décrit l'état historique pré-refactor).

**Endpoint principal** : `POST /api/v1/ventes` (permission `SALE_CREATE`).
**Endpoint détails** : `GET /api/v1/ventes/{commandeId}` (permission `SALE_READ`).

**Cas d'usage métier** : un vendeur (Employe connecté) sert un client au comptoir. Le client choisit une ou plusieurs **variantes** (`ProductFournisseur` = produit × fournisseur × qualité), avec un prix unitaire ≥ prix plancher du PF. Le système consomme le stock FIFO du PF, crée la commande + lignes + facture + paiement initial éventuel, le tout dans une seule transaction.

**Migrations associées** :
- **V12** `add_paiement_vente_and_pf_on_ligne_vente.sql` : crée la table `paiement_vente` (montant, datePaiement, moyen, facture FK, audit) + ajoute `ligne_commande_vente.product_fournisseur_id` (FK vers `product_fournisseur`) — pour la vente par variante.
- **V13** `remove_vendeur_from_commande_vente.sql` : drop `commande_vente.vendeur_id` (redondant avec `createdBy` de `AuditableEntity` qui porte déjà l'`accountId` du créateur). Le vendeur est résolu en lecture via `IAccountService.findUserSummaryByAccountId(createdBy)`.
- **V14** `enforce_pf_not_null_on_ligne_vente.sql` : durcit `ligne_commande_vente.product_fournisseur_id` en `NOT NULL` (le flux applicatif set toujours la valeur, contrainte BDD pour les insertions hors flux).
- **V15** `rename_date_echeache_to_date_echeance_on_facture_client.sql` : correction typo historique `facture_client.date_echeache → date_echeance` + backfill garde-fou (échéance null → date facture) + `SET NOT NULL`. Le champ entité passe de `dateEcheache` à `dateEcheance` (rejoint l'orthographe des autres tables `echeance`/`facture_achat`/`depense`).
- **V16** `drop_redundant_montants_on_commande_vente.sql` : supprime `commande_vente.montant_total` et `commande_vente.montant_paye`. Les montants vivent désormais uniquement sur `FactureClient` (relation 1:1 commande↔facture garantie depuis F-V3). Évite la désynchronisation. Le `CommandeVenteResponse` reçoit `montantTotal/montantPaye` en arguments explicites (depuis la facture associée), récupérés via `LEFT JOIN FactureClient` dans les queries de listing et détail.

**Entrée** : `VenteRequest` (4 champs)
```json
{
  "clientId": "uuid-or-null",
  "dateEcheance": "2026-05-30",    // OBLIGATOIRE, saisi par le vendeur, @FutureOrPresent
  "lignes": [
    { "productFournisseurId": "uuid-chine", "quantite": 100, "prixUnitaire": 10 },
    { "productFournisseurId": "uuid-maroc", "quantite": 20,  "prixUnitaire": 15 }
  ],
  "premierPaiement": {              // optionnel
    "montant": 1300,
    "modePaiement": "CASH",
    "datePaiement": "2026-05-16"    // optionnel dans le request, defaut now() cote service, @PastOrPresent
  }
}
```
**`dateVente` n'est PAS dans le request** — fixée systématiquement à `LocalDate.now()` par `VenteServiceImpl.create` (cohérent vente-au-comptoir : la date métier = date d'enregistrement). Plus de backdate possible côté API.

**Flux atomique (transaction unique)** :
1. Vendeur récupéré via `IEmployeService.findCurrentUser()` → throw `ForbiddenException("vente.user.required")` si l'utilisateur connecté n'est pas un Employe (PROPRIETAIRE refusé).
2. Magasin dérivé de `vendeur.magasin`.
3. Client résolu via `IClientService.findById` + scoping double si `clientId` non null ; sinon vente anonyme.
4. Pour chaque ligne : `productFournisseurService.ensureBelongsToCurrentEntreprise` + validation `prixUnitaire ≥ pf.prixVente` (sinon `BadArgumentException("vente.prixUnitaire.belowFloor", floor)`).
5. Création `CommandeVente` (référence auto `VTE-yyyyMMdd-HHmmssSSS` via `ReferenceHelper.generate("VTE")`, statut `DELIVERED`). **Pas de champ `vendeur`** sur l'entité : l'identité du créateur est portée par l'audit `createdBy` (= `accountId` stringifié).
6. Pour chaque ligne :
   - Création `LigneCommandeVente` avec `productFournisseur` + `product` + `quantite` + `prixUnitaire` + `montantTotal = qty × prix`.
   - Appel `ISortieStockService.consumeForVente(SortieStockForVente(magasin, pf, qty, prix, ligneVente))` :
     - Récupère lots FIFO du PF dans le magasin (`EntreeStockRepository.findAvailableLotsForFifoByProductFournisseur`).
     - Vérifie `SUM(qtyRestante) ≥ qty` (sinon `BadArgumentException("stock.exit.insufficientQuantity", dispo, demande)`).
     - Consomme FIFO, crée 1 `SortieStock` par lot consommé (avec marge = `(prixVente − prixAchatLot) × qty`), lié à la `LigneCommandeVente` via FK `sortie_stock.ligne_vente_id`.
     - Décrémente `Stock.quantiteDisponible` et journalise `MouvementStock(SORTIE_VENTE)`.
7. Calcul `montantTotal = SUM(lignes.montantTotal)`, `applyMontantTotal` sur la commande.
8. Création `FactureClient` (numéro auto `FAC-VTE-yyyyMMdd-HHmmssSSS`, statut `NON_PAYEE`, `date = dateVente effective`, `dateEcheance = venteRequest.dateEcheance()` — le vendeur l'a saisie, obligatoire).
9. Si `premierPaiement` : `PaiementVente` créé via `PaiementVenteCreate(facture, montant, moyen, datePaiement)` où `datePaiement = request.datePaiement()` si fourni sinon `LocalDate.now()` + `factureClientDomainService.applyPaiement` (recalcule `montantPaye` + statut `NON_PAYEE`/`PARTIELLEMENT_PAYEE`/`PAYEE`) + `commandeVenteDomainService.applyMontantPaye(commande, montant)` (incrémente depuis l'existant).

**Sortie** : `VenteResponse{ commande, facture }` — HTTP 201. Le détail complet (lignes + paiements) est dispo via `GET /api/v1/ventes/{commandeId}`.

**`VenteDetailsResponse`** (`GET /api/v1/ventes/{commandeId}`) : `{ commande, facture, lignes[], paiements[] }`. `commande.user` = `UserSummaryResponse(id, nomComplet)` résolu via `IAccountService.findUserSummaryByAccountId(commande.createdBy)` — pattern Option Minimaliste : pas de FK `user_id` redondante sur les tables, on s'appuie sur l'audit `createdBy` (= accountId stringifié).

**Records `<X>Create`** :
- `CommandeVenteCreate(client, magasin, dateVente, reference, statut)`
- `LigneCommandeVenteCreate(commande, productFournisseur, quantite, prixUnitaire)`
- `FactureClientCreate(commande, numero, date, dateEcheance, montantTotal)`
- `PaiementVenteCreate(facture, montant, moyen, datePaiement)` — groupe les 4 valeurs pour respecter la règle 30 (max 3 paramètres).
- `SortieStockCreate(lot, quantite, prixVente, ligneVente)` — supporte aussi les ajustements (ligneVente=null).
- `SortieStockForVente(magasin, productFournisseur, quantite, prixVente, ligneVente)` — paramètre orchestrant le flux sortie vente complet.
- `LotConsumptionContext(totalAConsommer, prixVente, ligneVente)` — paramètres communs aux deux chemins FIFO du stock (`create` simple et `consumeForVente`) ; permet une seule boucle `consumeFifo` partagée.
- `VenteContext(request, commande, magasin, user, productFournisseurs)` — paramètre des méthodes internes de VenteServiceImpl.

**Helpers transverses introduits / consommés** :
- `common/tools/NameHelper.formatNomComplet(String nom, String prenom)` : factorise la construction du libellé "nom + prenom" précédemment dupliquée dans `AccountServiceImpl`, `VenteServiceImpl`, `ClientSummaryResponse` (et corrige au passage un bug latent qui pouvait produire `"null prenom"`).
- `common/tools/UuidHelper.parseOptional(String) → Optional<UUID>` : variante safe de `parse()` (empty si null/blank/format invalide), utilisée pour résoudre `commande.createdBy` dans `IAccountService.findUserSummaryByAccountId`.

**Refactor `UserPrincipal`** (impact transverse) :
- Ajout `userId` (= `Utilisateur.id` métier, = `Employe.id` pour un employé).
- Rename `userId` existant → `accountId` (= `Account.id` auth).
- Nouveau claim JWT `Claim.USER("userId")`.
- `AuditorAwareImpl` migré vers `principal.accountId()` (la valeur stockée dans `createdBy/updatedBy` reste l'`accountId`).
- 24 tests adaptés (`new UserPrincipal(accountId, userId, ...)`) répartis sur achat / depense / entreprise / magasin / produit / security / stock / users / vente.

**Permissions** :
- `SALE_CREATE` : ADMIN, PROPRIETAIRE, MANAGER, VENDEUR (déjà en YAML).
- `SALE_READ` : idem.

**Règles métier** :
- Vendeur = Employe obligatoire (`UserPrincipal.userId` doit pointer vers un Employe en BD ; sinon Forbidden).
- Client nullable (vente anonyme — décision projet `project_client_anonyme`).
- 1 ligne = 1 PF unique. Mix de variantes du même produit → N lignes dans la même `VenteRequest`.
- Prix unitaire ≥ `pf.prixVente` (plancher PF). Pas de MAX FIFO car le prix de vente vit sur le PF (un seul prix par variante).
- Stock contrôlé par PF (pas par Product), via `EntreeStockRepository.findAvailableLotsForFifoByProductFournisseur`.
- Numéro de référence commande + numéro de facture auto-générés (le vendeur ne saisit rien). Frontend lecteur uniquement.
- Vendeur (`commande.user`) affiché via `UserSummaryResponse(id, nomComplet)` résolu côté lecture depuis `commande.createdBy` (= accountId stringifié) → `IAccountService.findUserSummaryByAccountId`.
- **Dates** :
  - `dateVente` **n'est plus dans le request** : fixée à `LocalDate.now()` à chaque création. Stockée sur `CommandeVente.date` et `FactureClient.date`.
  - `dateEcheance` **obligatoire dans le request** (`@NotNull @FutureOrPresent`), saisie par le vendeur. Vente comptant → frontend passe `dateEcheance = today`.
  - `premierPaiement.datePaiement` optionnel (`@PastOrPresent`), défaut `LocalDate.now()` côté service applicatif (le domain ne fait plus `LocalDate.now()` implicite, la résolution vit dans `VenteServiceImpl`).

**Tests** : 8 tests service `VenteServiceImplTest` (orchestration POST happy, default dateVente=today si null, throw user non-Employe, throw prix below floor, apply premier paiement, datePaiement du request respectée, GET détails, GET 404 facture, GET forbidden non-owned) + 3 controller (POST 201, POST 400 lignes vides, GET 200). Tests dédiés `FactureClientDomainServiceTest` (5 transitions `applyPaiement`) et `SortieStockServiceImplTest.consumeForVente_*` (3 cas). **448 / 448 verts**.

---

## 33. Listings vente (vente, fonctionnalité 4) — `CommandeVenteServiceImpl`, `FactureClientServiceImpl`, `PaiementVenteServiceImpl`

**5 endpoints** (tous en permission `SALE_READ`) :
- `GET /api/v1/commandes-vente?magasinId=&clientId?&vendeurId?&statut?&reference?&montantMin?&montantMax?&startDate?&endDate?&page&size` — listing paginé filtré commandes vente avec recherche multi-critères.
- `GET /api/v1/commandes-vente/{id}` — détail commande **avec `user` (vendeur) résolu** via projection JPQL.
- `GET /api/v1/factures-client?magasinId=&clientId?&statut?&startDate?&endDate?&page&size` — listing paginé filtré factures client.
- `GET /api/v1/factures-client/{id}` — détail facture.
- `GET /api/v1/factures-client/{id}/paiements` — paiements d'une facture, paginé (`Pageable` Spring Data, tri configurable via `?sort=`).

**Cas d'usage métier** : symétrie avec les listings Achat F12-F14 (`CommandeAchatController`, `FactureAchatController`). Le vendeur / manager consulte l'historique des ventes, factures et paiements d'un magasin. Toutes les vues retournent des `Page<>` triées par `createdAt DESC` par défaut.

**Stratégie projection JPQL (règle 24)** :
- Listings (`SELECT new <X>Response(entity)`) sans résolution du vendeur côté query (économie : pas de N+1 sur Account/Utilisateur sur des pages de N éléments). Le champ `user` est nul dans les éléments de liste.
- Détail commande (`GET /commandes-vente/{id}`) résout le vendeur en **une seule query JPQL** via `LEFT JOIN org.store.security.domain.model.Account a ON CAST(a.id AS string) = c.createdBy LEFT JOIN a.user u` + `TRIM(BOTH FROM CONCAT(COALESCE(u.nom, ''), ' ', COALESCE(u.prenom, '')))`. Le CAST est nécessaire car `AuditableEntity.createdBy` est stocké en `String` (UUID stringifié).

**Constructeurs JPQL ajoutés** (`CommandeVenteResponse`) :
- `(CommandeVente commande)` — listing, `user = null`.
- `(CommandeVente commande, UUID userId, String nomComplet)` — détail GET by id, instancie `UserSummaryResponse` seulement si `userId != null`.

**Filters** (records dans `vente/application/dto/`) :
- `CommandeVenteFilter(magasinId, clientId?, vendeurId?, statut?, reference?, montantMin?, montantMax?, startDate?, endDate?, page, size)` — 11 champs. Recherche multi-critères : vendeur, statut (`@EnumValue(CommandeVenteStatut)`), référence (LIKE insensitive), fourchette de montant total, plage de dates audit. `statutAsEnum()` via `EnumHelper.parse`. `toPageable()` trie DESC `createdAt`. Le filtre `vendeurId` est résolu via `LEFT JOIN Account a ON CAST(a.id AS string) = c.createdBy + a.user.id = :vendeurId` (l'audit `createdBy` est l'`accountId` stringifié, on remonte à l'Employe via `Account.user`).
- `FactureClientFilter(magasinId, clientId?, statut?, startDate?, endDate?, page, size)` + `statutAsEnum()` via `EnumHelper.parse`.

**Multi-tenant** : chaque query JPQL filtre obligatoirement par `entrepriseId` issu de `currentUserService.getCurrent()`. Le scoping est appliqué **dans la WHERE de la query** (pas en post-load), donc une commande / facture / paiement d'une autre entreprise est invisible (page vide ou `EntityException("notFound")` plutôt qu'un `notOwned` qui divulguerait l'existence de la ressource). En complément, le controller passe par `IMagasinService.ensureAccessibleByCurrentUser` pour vérifier l'accès magasin du caller sur les listings.

**Permissions** : `SALE_READ` sur tous les endpoints (déjà en YAML : ADMIN, PROPRIETAIRE, MANAGER, VENDEUR).

**i18n** : 2 nouvelles clés FR/EN — `commandeVente.notFound`, `factureClient.notFound`.

**Tests** : 6 service `CommandeVenteServiceImplTest` (validate filter, magasin not accessible forbidden, getById happy with user, getById notFound) + 6 service `FactureClientServiceImplTest` + 2 service `PaiementVenteServiceImplTest` + 4 controller `CommandeVenteControllerTest` (list 200 sans user, list_forward_all_filter_params via ArgumentCaptor sur les 9 query params, getById 200 avec user, getById 406 notFound) + 4 controller `FactureClientControllerTest` (list 200, getById 200, getById 406, listPaiements 200). **466 / 466 verts** (+18 vs 448 pré-F-V4).

---

## 34. Résumé caisse journalier — `CaisseServiceImpl`

**Endpoint** : `GET /api/v1/ventes/caisse/resume?magasinId=&date=YYYY-MM-DD` (permission `SALE_READ`).

**Cas d'usage métier** : le vendeur ou le manager clôture sa journée de caisse — combien de commandes enregistrées, combien d'articles vendus (somme des quantités, pas nombre de lignes), total monétaire des commandes, et total des paiements réellement encaissés ce jour-là (sémantique "tiroir-caisse" : argent qui est entré dans la caisse aujourd'hui, peu importe la date de la vente d'origine).

**Entrée** : `CaisseResumeFilter(magasinId @NotNull UUID, date @NotBlank @DatePattern String)` + accesseurs `startOfDay()` / `endOfDay()` (`LocalDateTime`, via `DateHelper.parseStartOfDay/parseEndOfDay`) + `dateAsLocalDate()`.

**Sortie** : `CaisseResumeResponse(magasinId, date, nombreCommandes, nombreProduits, totalCommandes, totalPaiements)`.

**4 queries JPQL agrégées scalaires** (filtre `createdAt BETWEEN startOfDay AND endOfDay`, cohérent avec F-V4-bis) :

1. `SELECT COUNT(c) FROM CommandeVente c WHERE c.magasin.entreprise.id = :entrepriseId AND c.magasin.id = :magasinId AND c.createdAt BETWEEN ...` → `nombreCommandes`
2. `SELECT COALESCE(SUM(c.montantTotal), 0) FROM CommandeVente c WHERE ...` (même WHERE) → `totalCommandes`
3. `SELECT COALESCE(SUM(l.quantite), 0) FROM LigneCommandeVente l WHERE l.commande.magasin.entreprise.id = :entrepriseId AND l.commande.magasin.id = :magasinId AND l.commande.createdAt BETWEEN ...` → `nombreProduits` (= somme des quantités, pas le nombre de lignes)
4. `SELECT COALESCE(SUM(p.montant), 0) FROM PaiementVente p WHERE p.facture.commande.magasin.entreprise.id = :entrepriseId AND p.facture.commande.magasin.id = :magasinId AND p.createdAt BETWEEN ...` → `totalPaiements` (tous paiements créés ce jour-là, même les paiements échelonnés sur des ventes antérieures comptent dans le tiroir-caisse du jour)

**Multi-tenant** : scoping `entreprise.id` dans chacune des 4 queries + vérification préalable de l'accès magasin du caller via `IMagasinService.ensureAccessibleByCurrentUser` (un vendeur ne peut consulter que le résumé d'un magasin auquel il est rattaché).

**Service applicatif** (`CaisseServiceImpl`) : agrège les 4 valeurs scalaires en une seule réponse. Pas de query "tout-en-un" volontairement — chaque agrégation est isolée (lisible, testable, réutilisable individuellement par d'autres services si besoin futur de dashboard).

**Permissions** : `SALE_READ` (déjà en YAML — ADMIN, PROPRIETAIRE, MANAGER, VENDEUR). Pas de nouvelle permission.

**Tests** : 3 service `CaisseServiceImplTest` (happy path d'agrégation des 4 valeurs, magasin not accessible forbidden propagé depuis IMagasinService, valeurs à zéro quand pas d'activité du jour — `COALESCE(SUM, 0)` côté JPQL évite les retours null) + 1 controller `CaisseControllerTest` (GET 200 sur `/resume?magasinId=&date=` avec assertions sur les 4 champs). **470 / 470 verts** (+4 vs 466).

### 34.bis Top N produits les plus vendus

**Endpoint** : `GET /api/v1/ventes/caisse/top-produits?magasinId=&date?&nombre?` (permission `SALE_READ`).
- `magasinId` obligatoire, `date` optionnel (défaut today via `TopProduitsFilter.effectiveDate()`), `nombre` optionnel (défaut 3, `@Min(1)`).

**Cas d'usage** : afficher en clôture caisse "les 3 produits qui ont le plus tourné aujourd'hui". Tri par **quantité totale vendue** (décision utilisateur, pas par CA).

**Réponse** : `List<TopProduitResponse(productId, nom, reference, quantiteVendue, chiffreAffaires)>`.

**Filter** : `TopProduitsFilter(magasinId, date?, nombre)` avec accesseurs `effectiveDate()` (today si null/blank), `startOfDay()` / `endOfDay()`, `toPageable()` = `PageRequest.of(0, nombre)`.

**Query JPQL** (sur `LigneCommandeVenteRepository`) :
```sql
SELECT new TopProduitResponse(p.id, p.nom, p.reference, SUM(l.quantite), COALESCE(SUM(l.montantTotal), 0))
FROM LigneCommandeVente l
JOIN l.product p
JOIN l.commande c
WHERE c.magasin.entreprise.id = :entrepriseId
  AND c.magasin.id = :magasinId
  AND c.createdAt BETWEEN :startOfDay AND :endOfDay
GROUP BY p.id, p.nom, p.reference
ORDER BY SUM(l.quantite) DESC
```
Limit via `Pageable` (= `PageRequest.of(0, nombre)`). Le repo retourne `List<TopProduitResponse>`.

**Tests** : +2 service (`findTopProduits_should_delegate_and_return_list`, `findTopProduits_should_use_today_when_date_null`) + 2 controller (200 avec défaut `nombre=3`, 200 avec `nombre` custom + sans `date`). **474 / 474 verts**.

---

## 35. Paiement échelonné (vente, fonctionnalité 5) — `PaiementVenteServiceImpl`

**Endpoint** : `POST /api/v1/factures-client/{id}/paiements` (permission `SALE_PAY`). Body : `PaiementVenteRequest`. Status 201.

**Cas d'usage métier** : crédit client. Une vente partiellement payée (statut `PARTIELLEMENT_PAYEE`) peut recevoir des paiements supplémentaires jusqu'à atteindre `PAYEE`. Symétrie directe avec `POST /api/v1/factures-achat/{id}/paiements` (côté achat).

**Logique** :
1. `validatorService.validate(request)`.
2. Charge `FactureClient` via `findById` + `ensureBelongsToCurrentEntreprise(facture, currentUser.entrepriseId)` (compare `facture.commande.magasin.entreprise.id`).
3. `ensureNotAlreadyPaid(facture)` : rejette si statut = `PAYEE` (`factureClient.alreadyPaid`).
4. `ensureAmountDoesNotExceedRemaining(facture, montant)` : `montant + montantPaye ≤ montantTotal` (`paiementVente.exceedsRemainingAmount`).
5. Résout `datePaiement = request.datePaiement() ?? LocalDate.now()`.
6. Crée le paiement via `PaiementVenteDomainService.create(PaiementVenteCreate)`.
7. Met à jour la facture via `FactureClientDomainService.applyPaiement(facture, montant)` (recalcule statut auto : `PARTIELLEMENT_PAYEE → PAYEE` quand le total est atteint).
8. Retourne `PaiementVenteResponse`.

**Plus de propagation sur `commande.montantPaye`** : depuis V16 (refactor de suppression de redondance, voir section 32), les montants vivent uniquement sur `FactureClient`. Le code F-V5 est ainsi simplifié — 1 seule étape de mise à jour (sur facture).

**Validations publiques** (règle 27) dans `PaiementVenteServiceImpl` : `ensureBelongsToCurrentEntreprise`, `ensureNotAlreadyPaid`, `ensureAmountDoesNotExceedRemaining`. Utilisables individuellement, testables isolément.

**i18n** : 2 nouvelles clés FR/EN — `factureClient.alreadyPaid`, `paiementVente.exceedsRemainingAmount`.

**Permissions** : `SALE_PAY` (déjà en YAML — ADMIN, PROPRIETAIRE, MANAGER, VENDEUR). Permission distincte de `SALE_READ` (un compte avec lecture seule ne peut pas créer de paiement).

**Tests** : 5 service `PaiementVenteServiceImplTest` (create happy + datePaiement custom + forbidden si autre entreprise + alreadyPaid bloque + exceedsRemaining bloque) + 2 controller `FactureClientControllerTest` (POST 201 happy, POST 400 si montant manquant). **481 / 481 verts** (+7 vs 474).

---

## 36. CRUDs catalogue abonnement (ADMIN) — `PlanAbonnementServiceImpl`, `SubscriptionTypeServiceImpl`, `CouponServiceImpl`, `PromotionServiceImpl`

**Endpoints** : `/api/v1/plans`, `/api/v1/subscription-types`, `/api/v1/coupons`, `/api/v1/promotions` (POST/GET/GET id/PUT/PATCH activate/PATCH deactivate/DELETE). Permissions `PLAN_*`, `SUBSCRIPTION_TYPE_*`, `COUPON_*`, `PROMOTION_*`.

**Cas d'usage métier** : seul l'ADMIN SaaS gère le **référentiel global** d'abonnement (tiers, durées, codes promo, promotions automatiques). Ces entités ne sont pas multi-tenant — elles sont partagées entre toutes les entreprises clientes.

**Logique commune (pattern décalqué sur `CategoryProductServiceImpl`)** :
1. `validatorService.validate(filter)` en première ligne du `findAll(filter)` (règle 33).
2. Création : unicité (nom/code), validation cohérence (`SubscriptionRules.ensureReductionConsistent` + `ensurePeriodValid`), persistence via `<X>DomainService.create(request[, plan])`.
3. Update : `findById` + revérification unicité si nom/code changé + revalidation cohérence + `applyRequest` + save.
4. Activate/Deactivate via `<X>DomainService.setActive(entity, boolean)` (règle 26 — pas de setter+save dans l'app service).
5. Pour Coupon/Promotion : résolution du plan optionnel via `IPlanAbonnementService.findByIdOrNull(UUID)` (default method).

**Validations communes** (helper `org.store.common.tools.SubscriptionRules`, règles 4 + 27) :
- `ensureReductionConsistent(reductionType, valeurReduction, invalidKey)` : type sans valeur (ou inverse) interdit, POURCENTAGE ≤ 100.
- `ensurePeriodValid(dateDebut, dateFin, invalidPeriodKey)` : `dateFin ≥ dateDebut`.

**i18n** : `plan.notFound/alreadyExists/reduction.invalid`, `subscriptionType.*`, `coupon.*`/`expired`/`exhausted`/`notApplicable`, `promotion.*`.

**Tests** : 14 service + 9 controller (Plan), 14 service + 8 controller (Type), 12 service + 8 controller (Coupon), 12 service + 7 controller (Promotion). +9 tests dédiés `SubscriptionRulesTest`.

---

## 37. Catalogue public d'abonnement — `PublicCatalogServiceImpl`

**Endpoint** : `GET /api/v1/catalog/public` (**permitAll** dans `SecurityConfig` — pas d'authentification). Status 200.

**Cas d'usage métier** : la **landing pricing** du site (visiteur non-authentifié) consomme cet endpoint pour afficher les plans + types + promotions actives. 1 round-trip serveur, agrégat complet retourné.

**Logique** :
1. `today = LocalDate.now()`.
2. Charge en parallèle (4 queries JPQL projetées, pas d'entités) :
   - `planAbonnementDomainService.findPublicResponses()` → `List<PublicPlanResponse>` (`actif=true && visible=true` triés `ordre, nom`)
   - `typeAbonnementDomainService.findAllActifResponses()` → `List<SubscriptionTypeResponse>` (`actif=true`)
   - `promotionDomainService.findActiveGlobalResponses(today)` → `List<PromotionResponse>` (`plan IS NULL && actif=true && dateDebut ≤ today ≤ dateFin`)
   - `promotionDomainService.findActiveScopedResponses(today)` → idem mais `plan IS NOT NULL`
3. Groupe les `scopedPromotions` par `planId` via stream.
4. Attache chaque sous-liste à son plan via `PublicPlanResponse.withPromotions(...)` (records immutables — retourne nouveau record).
5. Retourne `PublicCatalogResponse(plans, subscriptionTypes, globalPromotions)`.

**Pourquoi 4 queries projetées au lieu d'1 LEFT JOIN agrégé** : la séparation `global vs scoped` se fait en SQL (`WHERE plan IS NULL` / `IS NOT NULL`), pas en Java post-load. Plus économe en données transférées que charger toutes les promotions actives + filtrer côté serveur. Un seul stream Java pour l'assemblage final (records immutables).

**Optimisation** : `PublicPlanResponse` a 2 constructeurs — un canonique (13 champs avec `promotions`) et un secondaire (12 champs sans, utilisé par la projection JPQL). La méthode `withPromotions(...)` reconstruit le record avec la liste injectée.

**Tests** : 3 service `PublicCatalogServiceImplTest` (happy avec plans+types+promos, vide, multiples promos sur 1 plan) + 2 controller `PublicCatalogControllerTest` (catalogue complet, catalogue vide).

---

## 38. Souscription propriétaire — `AbonnementServiceImpl.subscribe`

**Endpoint** : `POST /api/v1/abonnements/subscribe` (permission `SUBSCRIPTION_CREATE`). Body `SubscribeRequest(planId, typeId, couponCode?, renouvellementAuto)`. Status 201.

**Cas d'usage métier** : le propriétaire souscrit (ou upgrade/downgrade) un abonnement. **Workflow 2 étapes** : `subscribe` crée juste l'Abonnement en EN_ATTENTE (avec breakdown du montant à payer pour info frontend), puis `POST /paiements-abonnement/abonnements/{id}` (use case 41) avec preuve image obligatoire active l'abonnement après validation admin (use case 42).

**Logique** :
1. `currentUserService.getCurrent().entrepriseId()` → résout l'entreprise du caller.
2. Charge `plan` via `IPlanAbonnementService.findById` puis `ensurePlanSubscribable` (`actif && visible && !trial`).
3. Charge `type` via `ISubscriptionTypeService.findById` puis `ensureTypeActif`.
4. Résolution coupon optionnel via `resolveCoupon(couponCode, planId)` :
   - `couponDomainService.findByCode(code)` → `EntityException("coupon.notFound")` si absent.
   - Validations : `actif=true`, `dateDebut ≤ today ≤ dateFin` (sinon `coupon.expired`), `nombreUtilisationsMax == 0 || nombreUtilisations < max` (sinon `coupon.exhausted`), `coupon.plan == null || coupon.plan.id == planId` (sinon `coupon.notApplicable`).
5. Recherche promotion automatique active pour le plan : `promotionDomainService.findFirstActivePromotionForPlan(planId, today)` (entité, table petite, justifié).
6. Calcule le breakdown via `SubscriptionAmountCalculator.calculate(SubscriptionAmountInputs(plan, type, promotion, coupon))` : applique réductions séquentielles `prix×durée → type → promotion → coupon`, clamp à zéro, scale BigDecimal 2 HALF_UP.
7. Crée l'Abonnement EN_ATTENTE via `abonnementDomainService.createPending(entreprise, plan, type)` (sans `dateDebut`/`dateFin` — fixés au paiement étape 7).
8. Configure renouvellementAuto via `abonnementDomainService.setRenouvellementAuto(abonnement, request.renouvellementAuto())`.
9. Si coupon : `reserveCoupon(coupon, entreprise, abonnement)` — délègue à `UtilisationCouponDomainService.create(coupon, entreprise, abonnement)` + `CouponDomainService.incrementUsage(coupon)`.
10. Retourne `SubscribeResponse(abonnement, breakdown, couponCodeApplied, promotionNomApplied)`.

**Stratégie upgrade/downgrade** : **remplacement à `dateFin`** (décision utilisateur, MVP). L'abonnement actuel reste ACTIF jusqu'à sa `dateFin` ; le nouveau démarre `currentActif.dateFin+1` (calcul effectif à la validation du paiement, use case 42). Pas de prorata.

**Validations publiques** (règle 27) : `ensurePlanSubscribable(plan)`, `ensureTypeActif(type)`, `resolveCoupon(code, planId)`, `ensureBelongsToCurrentEntreprise(abonnement)`.

**i18n** : `plan.notSubscribable`, `subscriptionType.notSubscribable`, `coupon.notFound/expired/exhausted/notApplicable`.

**Tests** : 9 service `AbonnementServiceImplTest` (happy sans coupon / avec coupon réservé / avec promotion / plan inactif / plan trial / type inactif / coupon inexistant / expired / exhausted / scope plan invalide) + 6 calculateur `SubscriptionAmountCalculatorTest` (base / pourcentage / montant_fixe / séquentiel / clamp zero) + 3 controller `AbonnementControllerTest`.

---

## 39. Toggle renouvellement auto — `AbonnementServiceImpl.updateRenouvellementAuto`

**Endpoint** : `PATCH /api/v1/abonnements/{id}/renouvellement-auto` (permission `SUBSCRIPTION_UPDATE`). Body `RenouvellementAutoRequest(boolean)`. Status 200.

**Cas d'usage métier** : le propriétaire peut **basculer le flag à tout moment** du cycle de vie de l'abonnement (avant/pendant/après activation), à sa convenance.

**Logique** :
1. Charge l'abonnement via `abonnementDomainService.findById(id)`.
2. `ensureBelongsToCurrentEntreprise(abonnement)` → `ForbiddenException("abonnement.notOwned")` si l'abonnement appartient à une autre entreprise.
3. Délègue le set+save au DomainService via `abonnementDomainService.setRenouvellementAuto(abonnement, request.renouvellementAuto())` (règle 26).
4. Retourne `AbonnementResponse(abonnement)`.

**Note** : aujourd'hui le paiement est manuel donc le flag `renouvellementAuto=true` n'a pas encore d'effet runtime (le worker de renouvellement automatique est **différé** — étape 9, dépend de l'intégration d'un intégrateur paiement). L'endpoint est livré pour préparer le futur.

**Tests** : 2 service (toggle + autre entreprise = Forbidden) + 1 controller.

---

## 40. Listings abonnement + statut courant entreprise — `AbonnementServiceImpl.findAll/findMyHistory/findMyCurrent`

**Endpoints** :
- `GET /api/v1/abonnements?entrepriseId=&statut=&planId=&page=&size=` (perm `ADMIN_ACCESS`) — ADMIN voit tous les abonnements (filtre libre par entreprise).
- `GET /api/v1/abonnements/me?statut=&planId=&page=&size=` (perm `SUBSCRIPTION_READ`) — PROPRIETAIRE voit son historique (auto-scopé à son entreprise).
- `GET /api/v1/abonnements/me/current` (perm `SUBSCRIPTION_READ`) — PROPRIETAIRE voit l'abonnement actif courant + jours restants + flag trial + fonctionnalités du plan.

**Cas d'usage métier** : visualisation et reporting. ADMIN supervise tous les comptes SaaS, propriétaire consulte son abonnement courant pour adapter l'UI selon les limites du plan (`nombreMagasinsMax`, `nombreEmployesMax`, `gestionStock/Vente/Achat/Comptabilite`).

**Logique** :
- `findAll(filter)` : `validatorService.validate(filter)` + `abonnementDomainService.findResponses(filter)` (projection JPQL `SELECT new AbonnementResponse(abonnement) ... LEFT JOIN FETCH plan/type/entreprise` + `countQuery` séparé pour la pagination).
- `findMyHistory(filter)` : force `filter.entrepriseId = currentUser.entrepriseId()` (record reconstruit avec scope) avant délégation.
- `findMyCurrent()` : `abonnementDomainService.findCurrentActif(entrepriseId)` (entité — besoin pour le constructeur secondaire de `CurrentAbonnementResponse`). Calcule `joursRestants = max(0, ChronoUnit.DAYS.between(today, dateFin))`. Retourne `CurrentAbonnementResponse(abonnement, joursRestants, isTrial, PlanFeaturesResponse(...))`.

**Optimisation repository** (R7a) : `AbonnementRepository.findResponsesByFilter` utilise une projection JPQL avec `LEFT JOIN FETCH` sur les relations `@ManyToOne` (plan, type, entreprise) — pas de N+1 lazy load + warning `Pageable+collection` évité car ce sont des ManyToOne.

**i18n** : `abonnement.noActive` si pas d'abonnement actif (404).

**Tests** : 4 service + 3 controller.

---

## 41. Paiement manuel (PROPRIETAIRE) — `PaiementAbonnementServiceImpl.create`

**Endpoint** : `POST /api/v1/paiements-abonnement/abonnements/{abonnementId}` (perm `SUBSCRIPTION_PAY`). Multipart : `data` (JSON `PaiementAbonnementRequest(moyen, referenceTransaction, datePaiement)`) + `file` (image, **obligatoire**). Status 201.

**Cas d'usage métier** : **pas d'intégrateur de paiement automatique** dans le projet. Le propriétaire paie hors-app (Wave/Orange Money/virement/cash) puis enregistre la transaction dans l'app avec une **preuve image obligatoire** (capture/photo du reçu). L'admin valide ensuite (use case 42).

**Logique** :
1. Charge abonnement + `ensureAbonnementBelongsToCurrentEntreprise` → `ForbiddenException` si autre entreprise.
2. `ensureAbonnementIsPending(abonnement)` : statut doit être EN_ATTENTE (sinon `abonnement.notPending`).
3. `ensureNoPendingPayment(abonnementId)` : via `paiementAbonnementDomainService.existsPendingForAbonnement` (boolean projection, R7b) — sinon `paiementAbonnement.alreadyPending`.
4. **Recalcule** le breakdown via `recomputeBreakdown(abonnement)` :
   - Promotion active à `today` (peut différer de la souscription).
   - Coupon : `utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId)` (projection UUID, R7c) → `couponDomainService.findById(couponId)`.
   - `amountCalculator.calculate(SubscriptionAmountInputs(plan, type, promotion, coupon))`.
5. Build de la `PieceJointe` preuve via `IUploadFileService.buildImage(file)` (validation MIME + blob + contentType).
6. Crée `PaiementAbonnement` en EN_ATTENTE_VALIDATION via `paiementAbonnementDomainService.createPending(new PaiementAbonnementCreationContext(abonnement, request, breakdown, preuveImage))` (record context, R3 — règle 30 max 3 params).
7. Retourne `PaiementAbonnementResponse(paiement)` (sans bytes preuve, juste `preuveId` — download via endpoint dédié).

**Montant recalculé côté serveur** : le propriétaire ne saisit pas de montant. Le système recalcule au moment du paiement (plan × durée − réductions). Si l'admin estime que le montant payé est incorrect, il rejette (use case 42).

**Migration** : V21 enrichit `paiement_abonnement` de `statut` (NOT NULL default EN_ATTENTE_VALIDATION), `preuve_id` (FK piece_jointe), `motif_rejet` (TEXT).

**i18n** : `abonnement.notPending`, `paiementAbonnement.alreadyPending`, `upload.file.empty`, `upload.file.invalidImageType`.

**Tests** : 4 service (happy create + autre entreprise + abonnement non pending + paiement pending déjà).

---

## 42. Validation / rejet paiement (ADMIN) — `PaiementAbonnementServiceImpl.validate/reject`

**Endpoints** :
- `PATCH /api/v1/paiements-abonnement/{id}/validate` (perm `SUBSCRIPTION_VALIDATE`). Body vide. Status 200.
- `PATCH /api/v1/paiements-abonnement/{id}/reject` (perm `SUBSCRIPTION_VALIDATE`). Body `RejectPaiementRequest(motifRejet)`. Status 200.

**Cas d'usage métier** : l'admin SaaS examine la preuve image fournie par le propriétaire et :
- **Valide** → l'abonnement passe en ACTIF avec `dateDebut`/`dateFin` calculés selon la stratégie de **remplacement à `dateFin`**.
- **Rejette** avec motif → l'abonnement reste EN_ATTENTE, le coupon réservé est **libéré** (rollback).

**Logique `validate`** :
1. Charge paiement + `ensurePaiementIsPendingValidation` (sinon `paiementAbonnement.notPendingValidation`).
2. `activateAbonnement(abonnement)` :
   - `abonnementDomainService.findLatestActifDateFin(entrepriseId, abonnement.getId())` (JPQL `MAX(dateFin)`, R7a) → si présent : `dateDebut = max+1`, sinon `dateDebut = today`.
   - `dateFin = dateDebut + typeAbonnement.dureeMois`.
   - `abonnementDomainService.activate(abonnement, dateDebut, dateFin)` (règle 26 — setter+save dans domain).
3. Marque paiement VALIDE via `paiementAbonnementDomainService.markAsValide(paiement)`.

**Logique `reject`** :
1. Charge paiement + `ensurePaiementIsPendingValidation`.
2. `releaseReservedCouponIfAny(abonnementId)` :
   - `utilisationCouponDomainService.findCouponIdByAbonnementId` → si présent : `couponDomainService.findById(couponId)` + `couponDomainService.decrementUsage(coupon)` + `utilisationCouponDomainService.deleteByAbonnementId(abonnementId)` (bulk delete R7c).
3. Marque paiement REJETE avec motif via `paiementAbonnementDomainService.markAsRejete(paiement, motifRejet)`.

**Scoping listing/lecture** :
- `findAll(filter)` : auto-scopé entreprise pour non-ADMIN (`scopeFilterForNonAdmin`).
- `findResponseById` + `getPreuve` : ADMIN tout, sinon entreprise du caller (`ensurePaiementAccessibleByCaller`).

**i18n** : `paiementAbonnement.notPendingValidation`, `paiementAbonnement.preuve.notFound`.

**Tests** : 4 validate/reject (activate dates today / dates currentActif+1 / already validated / release coupon / without coupon / reject sans motif 400) + 5 controller (list / get / get preuve / validate / reject).

---

## 47. Annulation de vente (vente, workflow critique multi-modules) — `VenteServiceImpl.cancel`

**Endpoint** : `POST /api/v1/ventes/{commandeId}/annuler` (permission `SALE_CANCEL` — ADMIN/PROPRIETAIRE/MANAGER, **pas VENDEUR**).

**Cas d'usage métier** : une vente DELIVERED est annulée (erreur de saisie, refus client, article défectueux). Le stock FIFO consommé doit être ré-injecté pour rendre les pièces à nouveau vendables ; la facture client est invalidée ; les paiements existants sont conservés pour audit (remboursement hors-app). L'annulation n'est autorisée que dans une fenêtre temporelle configurable (défaut 24 h après création).

**Entrée** : `AnnulationVenteRequest{ motif (enum `MotifAnnulationVente` : ERREUR_SAISIE, REFUS_CLIENT, ARTICLE_DEFECTUEUX, AUTRE) + commentaire optional ≤ 1000 chars }`.

**Flux atomique** (1 transaction `@Transactional`) :
1. `validatorService.validate(request)`.
2. Charge commande + `ensureBelongsToCurrentEntreprise` (`commandeVente.notOwned` 403).
3. `ensureCancellable(commande)` :
   - Si statut = `ANNULEE` → `BadArgumentException("commandeVente.cancel.alreadyCancelled")`.
   - Si statut ≠ `DELIVERED` → `BadArgumentException("commandeVente.cancel.notDelivered", statut)`.
4. `ensureWithinCancelWindow(commande)` :
   - `commande.createdAt + saleProperties.cancelWindowHours` doit être ≥ `now()`, sinon `BadArgumentException("commandeVente.cancel.windowExpired", maxHours)`.
5. Pour chaque `LigneCommandeVente` :
   - Charge les `SortieStock(annulee=false)` via `sortieStockDomainService.findActiveByLigneVenteId(ligneId)`.
   - Charge le `Stock` agrégé du (magasin, produit) une seule fois par ligne.
   - Pour chaque sortie : `entreeStockDomainService.creditQuantiteRestante(lot, qty)` + `sortieStockDomainService.markAsAnnulee(sortie)` + `stockDomainService.creditQuantite(stock, qty)` + `mouvementStockDomainService.journalize(stock, MouvementJournalize(RETOUR_CLIENT, qty, stockAvant, stockApres, null, null))`.
6. Bascule commande → `ANNULEE` + remplit `motifAnnulation`, `commentaireAnnulation`, `dateAnnulation` (méthode `commandeVenteDomainService.cancel`, règle 26).
7. Bascule facture → `ANNULEE` si présente (méthode `factureClientDomainService.cancel`, règle 26). Paiements `PaiementVente` conservés tels quels.

**Sortie** : `AnnulationVenteResponse{ commandeId, reference, statut=ANNULEE, motif, commentaire, dateAnnulation (yyyy-MM-dd HH:mm:ss), totalQuantiteReinjectee, nombreMouvementsCrees }`. HTTP 200.

**Conservation audit** :
- `SortieStock.annulee = true` permet à `MarginReportRepository.computeMargin` d'exclure les marges des ventes annulées via `WHERE sortie.annulee = false`.
- 6 queries Caisse adaptées (sumQuantite / countCommandes / ventilationVendeur / sumMontantTotal / sumMontantPaiement / topProduits) : `WHERE commande.statut <> ANNULEE`. Sémantique tiroir-caisse : pas de "fantôme" sur des ventes invalidées.
- Les `MouvementStock(SORTIE_VENTE)` initiaux ne sont pas supprimés — chaque RETOUR_CLIENT compensatoire est journalisé en pendant, l'historique reste lisible chronologiquement.

**Configuration** : `sale.cancel-window-hours: ${SALE_CANCEL_WINDOW_HOURS:24}` (record `SaleProperties` dans `org.store.property`, règle 38).

**Migration BDD** : V22 — `commande_vente +motif_annulation VARCHAR(30) / +commentaire_annulation TEXT / +date_annulation TIMESTAMP`, `sortie_stock +annulee BOOLEAN NOT NULL DEFAULT FALSE` + index.

**Validations publiques** (règle 27) : `ensureBelongsToCurrentEntreprise`, `ensureCancellable`, `ensureWithinCancelWindow`, `reinjectStockForLigne`, `reinjectOneSortie`. Toutes accessibles individuellement, testables isolément.

**i18n** : 3 clés FR/EN — `commandeVente.cancel.alreadyCancelled`, `commandeVente.cancel.windowExpired` ({0}=max hours), `commandeVente.cancel.notDelivered` ({0}=statut courant).

**Tests** : 5 service (nominal recrédit + journalize RETOUR_CLIENT + statut ANNULEE / déjà annulée 400 / pas DELIVERED 400 / fenêtre dépassée 400 / cross-entreprise 403) + 3 controller (200 OK / 400 motif invalide / 400 motif blank). **711 / 711 verts** (+8 vs 703).

---

## 48. Refactor vente — Workflow 2 étapes : DRAFT → VALIDATE — `VenteServiceImpl`

**Refactor 2026-05-18** : symétrie avec le refactor achat (section 28). La création atomique a été éclatée pour permettre **visualisation et édition** d'une vente avant encaissement (correction d'erreur de saisie, ajustement de quantité ou prix par le vendeur, choix d'une autre variante PF).

### 48.a Création DRAFT — `POST /api/v1/ventes`

**Permission** : `SALE_CREATE` (VENDEUR/MANAGER/PROPRIETAIRE/ADMIN).

**Entrée** : `VenteRequest{ clientId?, lignes[] }` — **plus de `dateEcheance` ni `premierPaiement`** (saisis à la validation).

**Flux** :
1. Validations : vendeur EMPLOYE obligatoire (`employeService.findCurrentUser` throw 403 sinon), client résolu si fourni (scoping double), pour chaque ligne : PF scopé entreprise + `prixUnitaire ≥ pf.prixVente` (plancher).
2. Crée `CommandeVente` en statut `DRAFT` (référence auto `VTE-yyyyMMdd-HHmmssSSS`, `dateVente = today`).
3. Persiste chaque `LigneCommandeVente` (snapshot prixUnitaire + montantTotal calculé).
4. **Pas de consommation stock, pas de facture, pas de paiement.**

**Sortie** : `VenteDraftResponse{ commande }` — HTTP 201.

### 48.b Édition / suppression de ligne — `PUT/DELETE /api/v1/ventes/orders/{commandeId}/lignes/{ligneId}`

**Permissions** : `SALE_UPDATE` (PUT) / `SALE_DELETE` (DELETE).

**Garde** : `ensureCommandeIsDraft` + `ensureLigneBelongsToCommande` (anti URL forgée) + `ensureNotLastLigne` (DELETE refusé sur la dernière ligne).

**PUT body** : `LigneVenteUpdateRequest{ quantite, prixUnitaire }`. Re-validation `prixUnitaire ≥ pf.prixVente` (le PF de la ligne reste immuable — pour changer de variante : supprimer + recréer). Retourne le `LigneCommandeVenteResponse` mis à jour.

**DELETE** : 204. Refus si dernière ligne (`commandeVente.cannotDeleteLastLigne`).

### 48.c Validation — `POST /api/v1/ventes/{commandeId}/validate`

**Permission** : `SALE_APPROVE` (nouvelle, créée 2026-05-18 ; attribuée VENDEUR+MANAGER+PROPRIETAIRE+ADMIN).

**Entrée** : `VenteValidateRequest{ dateEcheance (@FutureOrPresent), premierPaiement? }`.

**Flux atomique (transaction unique)** :
1. Validations : `ensureBelongsToCurrentEntreprise` + `ensureCommandeIsDraft`.
2. Pour chaque ligne : `sortieStockService.consumeForVente(...)` — consomme les lots FIFO du PF, crée 1 SortieStock par lot consommé (lié à la ligne via FK), décrémente Stock agrégé, journalise `MouvementStock(SORTIE_VENTE)`. Si stock insuffisant → `BadArgumentException("stock.exit.insufficientQuantity")` (le user doit ajuster la ligne ou attendre approvisionnement).
3. Recalcule `montantTotal = SUM(lignes.montantTotal)` depuis les lignes courantes (peuvent avoir été éditées).
4. Crée `FactureClient` (numéro auto `FAC-VTE-yyyyMMdd-HHmmssSSS`, statut NON_PAYEE, `dateEcheance` saisie).
5. Si `premierPaiement` présent : crée `PaiementVente` + `factureClientDomainService.applyPaiement` (recalcule statut PAYEE/PARTIELLEMENT_PAYEE).
6. Bascule `commande.statut → DELIVERED` via `commandeVenteDomainService.validate` (règle 26).

**Sortie** : `VenteResponse{ commande (DELIVERED), facture }` — HTTP 200.

### 48.d Détail — `GET /api/v1/ventes/{commandeId}`

Adapté pour gérer DRAFT : `facture` peut être null si la commande n'a pas encore été validée. Les paiements sont chargés uniquement si la facture existe.

### Queries Caisse adaptées : `= DELIVERED` (au lieu de `<> ANNULEE`)

6 queries de reporting ne comptent désormais que les ventes effectivement DELIVERED — exclut DRAFT (brouillons non finalisés) et ANNULEE (déjà exclu auparavant). Sémantique tiroir-caisse stricte.

Repos concernés : `CommandeVenteRepository.countByMagasinAndDay / sumQuantiteLignes / ventilationParVendeur`, `FactureClientRepository.sumMontantTotalByMagasinAndDay`, `PaiementVenteRepository.sumMontantByMagasinAndDay / ventilationParMoyen`, `LigneCommandeVenteRepository.findTopProduitsByMagasinAndDay`.

### Compatibilité avec l'annulation (section 47)

`cancel` (livré matinée 2026-05-18) continue à n'accepter que `DELIVERED` (statut → ANNULEE + ré-injection FIFO). Un DRAFT n'est pas "annulable" formellement — pour abandonner un brouillon, supprimer les lignes une à une (laisser la dernière) ou laisser le DRAFT mourir sans cleanup automatique.

### Migration BDD

**Aucune** — pas de nouveau champ à persister (la traçabilité lot ne s'applique pas à la vente, seulement à l'achat où elle a nécessité la V23).

### Permissions

- `SALE_APPROVE` créée (cohérence avec `PURCHASE_APPROVE`). Attribuée aux 4 rôles (VENDEUR inclus) : le vendeur qui crée la vente la valide lui-même au moment de l'encaissement.
- Aucune autre modification (toutes `SALE_*` déjà en place).

### Validations publiques (règle 27)

- `ensureCommandeIsDraft(commande)` (throw si statut ≠ DRAFT).
- `ensureLigneBelongsToCommande(ligne, commande)` (anti URL forgée).
- `ensureNotLastLigne(commande)` (delete refusé si 1 seule ligne).
- `ensurePrixUnitaireAboveFloor(prix, pf)` (factorisée — partagée entre `create` et `updateLigne`).

### i18n

4 nouvelles clés FR/EN : `commandeVente.notDraft` ({0}=statut), `commandeVente.cannotDeleteLastLigne`, `ligneCommandeVente.notFound`, `ligneCommandeVente.notMatchingCommande`.

### Cleanup

`VenteContext` record supprimé (n'était plus utilisé après refactor des méthodes internes vers `consumeStockForLigne` qui ne porte plus l'ancien batch state).

### Tests

`VenteServiceImplTest` réécrit (23 tests) : 4 create DRAFT (nominal sans stock/facture, dateVente=today, vendeur EMPLOYE obligatoire, prix plancher) + 5 validate (matérialisation, premier paiement, datePaiement, not draft, not owned) + 3 updateLigne (OK, prix below floor, not draft) + 3 deleteLigne (OK, last ligne 400, not draft) + 3 findDetails (DELIVERED, DRAFT facture null, not owned) + 5 cancel (préservés). `VenteControllerTest` (11 tests) : 201 DRAFT, 400 lignes vides, 200 validate, 400 validate dateEcheance manquante, 200 get details, 200 PUT, 400 PUT quantite 0, 204 DELETE, 200 cancel, 400 cancel motif invalide, 400 cancel motif blank. **739 / 739 verts** (+14 vs 725).

---

## Conventions transverses

- **i18n** : tous les messages d'erreur passent par `IMessageSourceService` (clés dans `messages*.properties`, fallback `useCodeAsDefaultMessage=true`).
- **Sécurité** : `@PreAuthorize` au niveau controller pour la coarse‑grained auth ; service responsable des règles métier fines.
- **Isolation services** : un `<X>ServiceImpl` n'injecte que `<X>DomainService` + des `I<Y>Service` d'autres agrégats (jamais un `<Y>Repository`).
- **Responses** : tout `<X>Response` doit exposer un constructeur `(<X> entity)` — ou des constructeurs secondaires pour les projections JPQL multi-champs (cf. `PublicPlanResponse` use case 37, `AbonnementResponse` use case 40).
- **Permissions** : centralisées dans l'enum `PermissionCode` ; chaque valeur = code en BDD.
- **Setters dans DomainService** (règle 26) : tout `entity.setX()` + `save(entity)` vit dans `<X>DomainService` comme méthode métier nommée (`setActive`, `activate`, `markAsValide`, `incrementUsage`, etc.). Les ServiceImpl orchestrent uniquement.
- **Projections record** (règle 24 + 38) : les méthodes repository retournent par défaut des `<X>Response` (projection JPQL `SELECT new`), pas des entités. Cas justifiés en entité : FK pour création / petite table partagée par plusieurs use cases / domaine fermé.
- **Externalisation valeurs fixes** (règle 38) : `@ConfigurationProperties` (records dans `org.store.property/`) — pas de `private static final` pour valeurs métier paramétrables (`SubscriptionProperties.trialDays`, `LoggingProperties.maxPayloadLength`, etc.).
