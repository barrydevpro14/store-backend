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

## 28. Module Achat — Création atomique commande + facture + paiements — `AchatServiceImpl`

**Endpoint** : `POST /api/v1/purchases` (auth requise, `@PreAuthorize("hasAuthority('PURCHASE_CREATE')")`)

**Cas d'usage métier** : le manager appelle/visite le fournisseur, commande la marchandise, le livreur arrive avec la marchandise + la facture. Le manager renseigne tout en une seule fois (pas de réception partielle, pas de workflow multi-étapes).

**Entrée** : `AchatRequest`
```json
{
  "magasinId": "uuid",
  "fournisseurId": "uuid",
  "facture": { "numero": "F2026-001", "date": "2026-05-14", "dateEcheance": "2026-06-14" },
  "lignes": [ { "productFournisseurId": "uuid", "quantite": 10, "prixUnitaire": 1500.00 } ],
  "premierPaiement": { "montant": 5000.00, "modePaiement": "CASH" }  // optionnel
}
```

**Flux atomique (transaction unique)** :
1. Vérifier scoping magasin + fournisseur (entreprise du caller).
2. Vérifier cohérence `productFournisseur.fournisseur == fournisseurId` pour chaque ligne (sinon `BadArgumentException("achat.fournisseur.productMismatch")`).
3. Vérifier unicité `factureAchat.numero` par entreprise.
4. Créer `CommandeAchat` avec référence auto via `ReferenceHelper.generate("CMD")` → format `CMD-yyyyMMdd-HHmmssSSS`.
5. Créer `FactureAchat` liée à la commande, `montantFacture = SUM(qty × prixUnitaire)` calculé.
6. Pour chaque ligne : créer `LigneCommandeAchat` + appeler `IEntreeStockService.create(...)` (entrée stock immédiate avec lot FIFO + upsert Stock + journalisation `MouvementStock(ENTREE_ACHAT)`).
7. Si `premierPaiement` présent : créer `PaiementAchat` (vérifier `montant <= montantFacture`).

**Sortie** : `AchatResponse{ commande, facture, lignes, paiements }` — HTTP 201.

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

**Tests** : 28 nouveaux tests (controller + services + domain). Suite à 384 / 384 verts.

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

## Conventions transverses

- **i18n** : tous les messages d'erreur passent par `IMessageSourceService` (clés dans `messages*.properties`, fallback `useCodeAsDefaultMessage=true`).
- **Sécurité** : `@PreAuthorize` au niveau controller pour la coarse‑grained auth ; service responsable des règles métier fines.
- **Isolation services** : un `<X>ServiceImpl` n'injecte que `<X>DomainService` + des `I<Y>Service` d'autres agrégats (jamais un `<Y>Repository`).
- **Responses** : tout `<X>Response` doit exposer un constructeur `(<X> entity)`.
- **Permissions** : centralisées dans l'enum `PermissionCode` ; chaque valeur = code en BDD.
