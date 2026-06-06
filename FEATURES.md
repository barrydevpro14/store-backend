# FEATURES.md — Summary of business application services

> This file lists, for each application service (use case), the exposed endpoint, the applied business rules, the injected dependencies and the possible exceptions. To update with every new service.

---

## 1. Owner registration — `RegisterPropertyServiceImpl`

**Endpoint** : `POST /api/v1/auth/register` (public, no auth)

**Input** : `RegisterPropertyRequest`
```json
{
  "account":    { "username", "password" },
  "utilisateur":{ "nom", "prenom", "email", "telephone", "adresse" },
  "entreprise": { "sigle", "raisonSociale", "ninea", "rccm", "adresse", "countryId" },
  "magasin":    { "nom", "adresse" }
}
```

**Flow** :
1. Loads the `PROPRIETAIRE` role via `IRoleService.findByLibelle`.
2. Loads the active trial plan via `IPlanAbonnementService.findFirstTrialActif`.
3. Creates the `Account` (`IAccountService.create`) + the `Proprietaire` (`IProprietaireService.create`) + the `Entreprise` (`IEntrepriseService.create`) + the first `Magasin` (`IMagasinService.create`).
4. Links : `account.user = proprietaire`, `proprietaire.entreprise = entreprise`, `entreprise.magasins = [magasin]`.
5. Creates a trial subscription via `IAbonnementService.createTrial`.
6. Generates access JWT via `IJwtService.generateToken` + opaque UUID refresh token via `IRefreshTokenService.create`.

**Rules** :
- `account.username` must be unique (`UniqueResourceException("account.username.exists")`).
- Phone mandatory and in SN format (`@NotBlank @Phone`).
- The `PROPRIETAIRE` role must exist in DB (seeded by `DataInitializer`).
- An active trial plan must exist.

**Output** : `AuthResponse{ accessToken, refreshToken }` — HTTP 201.

---

## 2. Login — `LoginServiceImpl`

**Endpoint** : `POST /api/v1/auth/login` (public)

**Input** : `LoginRequest{ username, password }`

**Flow** :
1. `AuthenticationManager.authenticate(...)` checks credentials via `UserDetailsServiceImpl`.
2. If OK, reloads the `Account` via `IAccountService.findByUsername`.
3. Builds `UserPrincipal` via `IUserPrincipalFactory.build(account)` — resolves `entrepriseId`, `magasinId`, `currency`, `countryName` from `entreprise.country` via strategy pattern.
4. Generates access JWT (claims: `userId`, `entrepriseId`, `magasinId`, `username`, `currency`, `countryName`, `role`, `permissions`) + persisted refresh token.
5. Publishes `AuditEvent(LOGIN)` with IP (X-Forwarded-For aware) + User-Agent via `RequestHelper`.

**Rules** :
- `BadCredentialsException` (Spring Security) if wrong password.
- `EntityException("account.notFound")` if not found after auth (pathological case).

**Output** : `AuthResponse{ accessToken, refreshToken }` — HTTP 200.

---

## 3. Refresh token — `RefreshTokenServiceImpl.refresh`

**Endpoint** : `POST /api/v1/auth/refresh` (public)

**Input** : `RefreshTokenRequest{ refreshToken }`

**Flow** :
1. Refresh token lookup via `RefreshTokenDomainService.findByToken`.
2. Check : not revoked, not expired, linked to a `Utilisateur` that has an `Account`.
3. Rebuilds `UserPrincipal` then issues a new access JWT (the refresh token stays the same — **no rotation**).

**Rules / errors** :
- `UnauthorisedException("refreshToken.invalid|revoked|expired")` depending on the case.

**Output** : `AuthResponse{ accessToken (new), refreshToken (unchanged) }` — HTTP 200.

---

## 4. Logout — `RefreshTokenServiceImpl.revoke`

**Endpoint** : `POST /api/v1/auth/logout` (public, idempotent)

**Input** : `RefreshTokenRequest{ refreshToken }`

**Flow** :
1. Lookup ; if present and not revoked : `setRevoked(true) + save`.
2. Otherwise : silent.

**Output** : HTTP 204 (always).

---

## 5. Employee creation — `EmployeServiceImpl.create`

**Endpoint** : `POST /api/v1/employees` (auth required, `@PreAuthorize("hasAuthority('EMPLOYE_CREATE')")`)

**Input** : `EmployeRequest`
```json
{
  "account":     { "username", "password" },
  "utilisateur": { "nom", "prenom", "email", "@Phone telephone", "adresse" },
  "role":        "MANAGER" | "VENDEUR" | "CAISSIER" | "...",
  "magasinId":   "<uuid>"
}
```

**Flow** :
1. Retrieves `currentUser` via `ICurrentUserService.getCurrent`.
2. `IRoleService.findByLibelle(askedRole)` (404 if role unknown).
3. `IPermissionsService.findAllByRoleId(role.id)` → permission codes of the role.
4. Checks that the role has `EMPLOYE_ACCESS` (otherwise 403).
5. If the role has `EMPLOYE_CREATE` ("elevated" role) and the caller is not an owner → 403.
6. `IMagasinService.findById(magasinId)` + ownership check :
   - Owner : `magasin.entreprise.id == currentUser.entrepriseId`.
   - Otherwise (manager) : `magasin.id == currentUser.magasinId`.
7. If elevated role : `EmployeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, "EMPLOYE_CREATE")` → 403 if a manager already exists.
8. `IAccountService.create(account, role)` (unique username check).
9. `EmployeDomainService.create(utilisateur, account, magasin)` builds + saves the `Employe`, links `account.user`, returns an `EmployeResponse`.

**Business rules** :
| Rule | Mechanism |
|---|---|
| Owner can create any employee | Steps 5 and 7 only apply to non-owners |
| Manager can create everything except another manager | Step 5 (`EMPLOYE_CREATE && !PROPRIETAIRE_ACCESS`) |
| A single "elevated" role per store | Step 7 (data-driven on `EMPLOYE_CREATE`) |
| Owner can only create within **their company** | Step 6 |
| Manager can only create within **their store** | Step 6 |
| The created role must be an employee role | Step 4 |

**Exceptions** :
- `ForbiddenException("employe.create.role.notAllowed")`, `("employe.create.elevatedRole.forbidden")`, `("magasin.notOwned")`, `("magasin.alreadyHasManager")`.
- `EntityException("role.notFound" | "entity.notFound")`.
- `UniqueResourceException("account.username.exists")`.

**Output** : `EmployeResponse{ id, nom, prenom, email, telephone, adresse, username, role, magasinId }` — HTTP 201.

---

## 6. Store CRUD + activate/deactivate — `MagasinServiceImpl`

**Endpoints** (`@PreAuthorize("hasAuthority('PROPRIETAIRE_ACCESS')")` at class level) :

| Method | Endpoint | Action |
|---|---|---|
| `POST` | `/api/v1/magasins` | Creates a store in the caller's company (201) |
| `GET` | `/api/v1/magasins?page=0&size=10&sort=nom,asc` | Paginated list of the current company's stores (200, `Page<MagasinResponse>`) |
| `GET` | `/api/v1/magasins/{id}` | Reads a store (200) |
| `PUT` | `/api/v1/magasins/{id}` | Updates `nom`/`adresse` (200) |
| `PATCH` | `/api/v1/magasins/{id}/activate` | Soft-activate (`actif=true`) (200) |
| `PATCH` | `/api/v1/magasins/{id}/deactivate` | Soft-delete (`actif=false`) (200) |

**`MagasinRequest`** : `@NotBlank nom`, `@NotBlank adresse`.

**`MagasinResponse`** : `id, nom, adresse, actif, entrepriseId`. Secondary constructor `(Magasin)`.

**`actif` field** : visual flag only (no business effect in MVP — does not prevent sales/purchases on this store). Added via `V2__add_magasin_actif.sql` (`BOOLEAN NOT NULL DEFAULT TRUE`).

**Business rule (all operations)** : `ensureBelongsToCurrentEntreprise(magasin)` — `magasin.entreprise.id == currentUser.entrepriseId` otherwise `ForbiddenException("magasin.notOwned")`.

**Pagination** : custom JPQL query in `MagasinRepository.findResponsesByEntrepriseId` — direct projection to `MagasinResponse` via `SELECT new ... MagasinResponse(...)`, separate `countQuery`. Avoids materializing the full entities.

**Injected dependencies** in `MagasinServiceImpl` :
| Dependency | Why |
|---|---|
| `MagasinDomainService` | Store CRUD + paginated projected query |
| `IEntrepriseService` | Loads the caller's company (`findById`) |
| `ICurrentUserService` | `UserPrincipal` from SecurityContext |

**`IMagasinService.create(MagasinRequest, Entreprise) → Magasin`** : internal signature preserved (used by the owner registration flow) ; the controller uses the new `create(MagasinRequest) → MagasinResponse` which auto-scopes on the caller's company.

---

## 7. Company CRUD + activate/deactivate — `EntrepriseServiceImpl` + `RegisterPropertyServiceImpl.registerEntrepriseByAdmin`

**Endpoints** (`@PreAuthorize` at **method** level, mixed permissions) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/entreprises` | `ADMIN_ACCESS` | Creates a complete company (account + proprietaire + entreprise + magasin + trial). Delegates to `IRegisterPropertyService.adminCreate` (same flow as `/auth/register` but without token generation) (201) |
| `GET` | `/api/v1/entreprises?page=&size=` | `ADMIN_ACCESS` | Paginated list of all companies (200, `Page<EntrepriseResponse>`) |
| `GET` | `/api/v1/entreprises/{id}` | `ADMIN_ACCESS` | Read by id (200) |
| `PATCH` | `/api/v1/entreprises/{id}/activate` | `ADMIN_ACCESS` | Soft-activate (200) |
| `PATCH` | `/api/v1/entreprises/{id}/deactivate` | `ADMIN_ACCESS` | Soft-delete (200) |
| `GET` | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | Owner reads their own company (200) |
| `PUT` | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | Owner updates info of their own company (200) |

**`EntrepriseRequest`** : `@NotBlank sigle`, `@NotBlank raisonSociale`, `ninea`, `rccm`, `adresse`.

**`EntrepriseResponse`** : `id, sigle, raisonSociale, ninea, rccm, adresse, actif, trialUsed`. Secondary constructor `(Entreprise)`.

**`actif` field** : visual flag only (no business effect in MVP). Added via `V3__add_entreprise_actif.sql` (`BOOLEAN NOT NULL DEFAULT TRUE`).

**Reference data** :
- `ADMIN_ACCESS` permission (`PermissionCode` enum).
- `ADMIN` role seeded (`DataInitializer`). ⚠️ No ADMIN account auto-created — must be provisioned manually in DB.

**Admin creation (POST)** : the controller delegates to `IRegisterPropertyService.registerEntrepriseByAdmin(RegisterPropertyRequest)` which returns an `EntrepriseResponse` directly. This method calls `createAccount(request, "PROPRIETAIRE")` (common orchestration : Account + Proprietaire + Entreprise + Magasin + trial subscription) then extracts `proprietaire.getEntreprise()` and returns the DTO. ADMIN does not get tokens — the new owner has to `POST /auth/login` to connect.

> **Why the method lives in `IRegisterPropertyService`** : `RegisterPropertyServiceImpl` already injects `IEntrepriseService` to orchestrate registration. Placing the method in `IEntrepriseService` would create a DI cycle. The rule "the service that already orchestrates hosts the method" settles it.

---

## 8. `UserPrincipalContextStrategy` strategy — UserPrincipal composition

**Not an endpoint** — internal pattern invoked by `UserPrincipalFactoryImpl.build(Account)`.

**Package** : `org.store.security.application.strategies`

**Interface** :
```java
public interface UserPrincipalContextStrategy {
    Class<? extends Utilisateur> targetType();
    UserPrincipalContext resolve(Utilisateur user);
}
```

**Return record** : `UserPrincipalContext(UUID entrepriseId, UUID magasinId)` (+ static `empty()`).

**Three implementations** (`@Component`) :
- `ProprietairePrincipalContextStrategy` (targetType = `Proprietaire`) → `(entreprise.id, null)`. An OWNER is not attached to a specific store.
- `EmployePrincipalContextStrategy` (targetType = `Employe`) → `(magasin.entreprise.id, magasin.id)`.
- `UtilisateurPrincipalContextStrategy` (targetType = `Utilisateur`) → fallback `empty()` (ADMIN typically).

**Dispatch in `UserPrincipalFactoryImpl`** (zero `instanceof`, "most-specific wins") :
```java
strategies.stream()
    .filter(s -> s.targetType().isInstance(user))
    .reduce((a, b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)
    .map(s -> s.resolve(user))
    .orElseGet(UserPrincipalContext::empty);
```

**General rule** : any dispatch by entity subtype must follow this pattern (see ARCHITECTURE.md rule 28).

---

## 9. ERP permissions and roles seed — `DataInitializer`

**Not an endpoint** — idempotent bootstrap at app startup (`ApplicationRunner`).

**Reference** : `src/main/resources/static/liste_roles_permissions.md` + matrix from the PDF `Roles Permissions Erp Saas.pdf` (archived out of repo).

**Seeded roles** (4) :
| Code | Description | Notes |
|---|---|---|
| `ADMIN` | SaaS administrator | All 79 permissions (= `SUPER_ADMIN` semantically) |
| `PROPRIETAIRE` | Owner of a company | ≈ `OWNER` from the PDF — all permissions except `COMPANY_CREATE`/`DELETE` |
| `MANAGER` | Manager of a store | Absorbs MANAGER + MAGASINIER + COMPTABLE from the PDF |
| `VENDEUR` | Seller of a store | `AUTH_*`, `SALE_*`, `PAYMENT_CREATE/READ`, `PRODUCT_READ`, `STOCK_READ` |

**Seeded permissions** (79) :
- **4 legacy preserved** for code compat : `PROPRIETAIRE_ACCESS`, `EMPLOYE_ACCESS`, `EMPLOYE_CREATE`, `ADMIN_ACCESS` (referenced by current `@PreAuthorize`).
- **75 new** in `MODULE_ACTION` format : `AUTH_*` (5), `USER_*` (7), `COMPANY_*` (4), `STORE_*` (5), `PRODUCT_*` (7), `STOCK_*` (6), `PURCHASE_*` (6), `SALE_*` (6), `EXPENSE_*` (5), `PAYMENT_*` (4), `SUBSCRIPTION_*` (4), `DOCUMENT_*` (4), `DASHBOARD_READ + REPORT_*` (5), `SETTINGS_*` (2), `AUDIT_*` (2).

**Idempotency** : `findByCode` / `findByLibelle` ; only adds permissions missing from the role. Safe to re-run.

**Assumed debt** : current Java code (`@PreAuthorize('PROPRIETAIRE_ACCESS')`, etc.) still references the 4 legacy permissions. Migration to the granular nomenclature (`COMPANY_READ`, `STORE_CREATE`, ...) is partial : the `PermissionCode` enum now contains 16 values (4 legacy + 12 granular `CATEGORY_PRODUCT_*` / `QUALITY_*` / `PRODUCT_*` used by the product controllers). The remaining 70+ YAML permissions are not yet typed in the enum.

---

## 10. Product category CRUD — `CategoryProductServiceImpl`

**Endpoints** (`/api/v1/category-products`, granular permissions) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/category-products` | `CATEGORY_PRODUCT_CREATE` | Creates a category for the caller's company (201) |
| `GET` | `/api/v1/category-products?page=&size=` | `CATEGORY_PRODUCT_READ` | Paginated list of the current company's categories (200) |
| `GET` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_READ` | Reads a category (200, scoped) |
| `PUT` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_UPDATE` | Updates `libelle`/`description` (200) |
| `DELETE` | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_DELETE` | Deletes (204) |

**`CategoryProductRequest`** : `@NotBlank @Size(max=255) libelle`, `@Size(max=255) description`.

**`CategoryProductResponse`** : `id, libelle, description, entrepriseId`. Secondary constructor `(CategoryProduct)`.

**Multi-tenant scoping** : `@ManyToOne Entreprise entreprise` FK on the entity (migration `V4__add_entreprise_to_category_quality.sql`). `ensureBelongsToCurrentEntreprise(category)` on all operations (`categoryProduct.notOwned`).

**Label uniqueness per company** : `existsByLibelleAndEntrepriseId` before create. On update : check skipped if the label has not changed.

**Pagination** : `SELECT new CategoryProductResponse(c) FROM CategoryProduct c WHERE c.entreprise.id = :entrepriseId` (JPQL projection via the secondary constructor).

**Dependencies** : `CategoryProductDomainService`, `IEntrepriseService`, `ICurrentUserService`.

---

## 11. Quality CRUD — `QualityServiceImpl`

**Endpoints** (`/api/v1/qualities`) — same structure as CategoryProduct, permissions `QUALITY_*`. Same scoping rules, same Request/Response/projection/uniqueness patterns. See section 10.

---

## 12. Product CRUD — `ProductServiceImpl`

**Endpoints** (`/api/v1/products`) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/products` | `PRODUCT_CREATE` | Creates a product in the caller's company (201) |
| `GET` | `/api/v1/products?page=&size=` | `PRODUCT_READ` | Paginated list of the company's products (200) |
| `GET` | `/api/v1/products/{id}` | `PRODUCT_READ` | Reads a product (200, scoped) |
| `PUT` | `/api/v1/products/{id}` | `PRODUCT_UPDATE` | Updates name/reference/description + category/quality (200) |
| `DELETE` | `/api/v1/products/{id}` | `PRODUCT_DELETE` | Deletes (204) |

**`ProductRequest`** : `@NotBlank @Size(max=255) nom`, `@NotBlank @Size(max=255) reference`, `@Size(max=1000) description`, `@NotNull UUID categoryProductId`, `@NotNull UUID qualityId`.

**`ProductResponse`** (nested sub-DTOs — rule 23) : `id, nom, reference, description, CategoryProductResponse category, QualityResponse quality, entrepriseId, UUID imagePrincipalId`. Secondary constructor `(Product)`. `imagePrincipalId` = id of the `PieceJointe` or `null` if no main image.

**Business rules** :

| Rule | Mechanism |
|---|---|
| The product belongs to the caller's company | `ensureBelongsToCurrentEntreprise(product)` on read/update/delete |
| `categoryProductId` must belong to the same company | `categoryProductService.ensureBelongsToCurrentEntreprise(...)` on create/update |
| `qualityId` must belong to the same company | `qualityService.ensureBelongsToCurrentEntreprise(...)` on create/update |
| `reference` unique per company | `existsByReferenceAndEntrepriseId` on create, skipped if unchanged on update |

**`@Transactional(readOnly = true)` at class level** (reads), override `@Transactional` on mutations. Allows lazy JPQL projections to access `categoryProduct`/`quality` during the mapping to `ProductResponse`.

**Exceptions** :
- `EntityException("product.notFound" | "categoryProduct.notFound" | "quality.notFound")`.
- `ForbiddenException("product.notOwned" | "categoryProduct.notOwned" | "quality.notOwned")`.
- `UniqueResourceException("product.reference.alreadyExists")`.

**Dependencies** : `ProductDomainService`, `ICategoryProductService`, `IQualityService`, `IEntrepriseService`, `ICurrentUserService`, `IUploadFileService` (for image endpoints).

---

## 13. File upload — `UploadFileServiceImpl`

**Not an endpoint** — cross-cutting technical service in `org.store.common.service`.

**Interface** :
```java
public interface IUploadFileService {
    PieceJointe buildImage(MultipartFile file);
    List<PieceJointe> buildImages(List<MultipartFile> files);
}
```

**Behavior** :
- Validates non-emptiness of the file (`upload.file.empty`).
- Validates image MIME against `UploadProperties.allowedImageTypes` (configurable, default `image/{jpeg,png,webp,gif}`). Error : `upload.file.invalidImageType`.
- Wraps `IOException` (reading bytes) in `BadArgumentException("upload.file.readFailed")`.
- Builds a non-persisted `PieceJointe` with `document=bytes`, `date=LocalDate.now()`, `contentType=file.getContentType().toLowerCase()`.
- `buildImages` : validates non-empty (`upload.files.empty`), then applies `buildImage` in a loop (stops at the first invalid one).

**External configuration** (`record UploadProperties(Set<String> allowedImageTypes)`, `@ConfigurationProperties("upload")`) :
```yaml
upload:
  allowed-image-types:
    - image/jpeg
    - image/png
    - image/webp
    - image/gif
```
Compact constructor that normalizes to lowercase + makes immutable. Change = YAML edit, **zero Java**.

**Multipart** (`application.yml`) : `spring.servlet.multipart.{enabled=true, max-file-size=5MB, max-request-size=6MB}`.

---

## 14. Main image and product gallery — `ProductServiceImpl` (continued)

**Image endpoints** :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `PUT` | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | Uploads (replaces) the main image, multipart body `file` (200, `ProductResponse`) |
| `DELETE` | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | Deletes the main image (idempotent, 204) |
| `GET` | `/api/v1/products/{id}/image` | `PRODUCT_READ` | Serves the blob with the right `Content-Type` (200, `byte[]`) |
| `POST` | `/api/v1/products/{id}/images` | `PRODUCT_UPLOAD_IMAGE` | Cumulative upload of multiple images into the gallery, multipart body `files` (201, `List<UUID>`) |
| `GET` | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_READ` | Serves the blob of one gallery image (200, `byte[]`) |
| `DELETE` | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_UPLOAD_IMAGE` | Removes an image from the gallery (orphanRemoval purges the `PieceJointe`) (204) |

**Model** :
- `Product.imagePrincipal` — `@OneToOne(fetch=LAZY, cascade=ALL, orphanRemoval=true) PieceJointe`. Migration `V5__add_image_principal_to_product.sql`.
- `Product.images` — `@OneToMany(fetch=LAZY, cascade=ALL, orphanRemoval=true) List<PieceJointe>` (gallery independent of `imagePrincipal`).
- `PieceJointe.contentType` — `@Column(name="content_type", nullable=false, length=100) String` (migration `V6__add_content_type_to_piece_jointe.sql`). Filled at upload from `file.getContentType()` (lowercased). **Source of truth** to serve the image with the right `Content-Type`.

**Return DTO** (visualization) — `ImageDownloadResponse(byte[] content, String contentType)` in `common/dto`. The controller transforms into `ResponseEntity.ok().contentType(MediaType.parseMediaType(...)).body(...)`.

**Rules** :
- `ensureBelongsToCurrentEntreprise(product)` on all operations.
- `getImagePrincipal` throws `EntityException("product.image.notFound")` if no image.
- `getImage` / `deleteImage` throw `EntityException("product.image.galleryImageNotFound")` if imageId not in the product's gallery.
- `setImagePrincipal(product, null)` or `removeImage(product, image)` triggers the actual deletion of the `PieceJointe` via `orphanRemoval=true`.

**Performance** : visualization reads `pieceJointe.getContentType()` directly (stored field), no runtime detection. Extensible to any MIME (PDF, invoices, docs) with no Java change.

**Current limitations** :
- No endpoint listing the gallery (`GET /{id}/images` returning the list of metadata). The client must know the ids beforehand (returned at upload).
- No application-level size validation (relies on the Spring `max-file-size=5MB` limit, returns 400 `MaxUploadSizeExceededException` above — custom i18n handler to be added if needed).

---

## 15. Product gallery listing — `ProductServiceImpl.listImages`

**Endpoint** : `GET /api/v1/products/{id}/images` (permission `PRODUCT_READ`)

**Output** : `List<ImageMetadataResponse{id, date, contentType, url}>`. The `url` field is a relative path `/api/v1/products/{productId}/images/{imageId}` directly usable by the frontend (`<img src={img.url}>`), no need to know the routing convention.

**DTO** `ImageMetadataResponse` in `produit/application/dto/` (the product path is in the URL → DTO specific to the module). 2 constructors :
- `(PieceJointe, UUID productId)` for a gallery image (`/images/{imageId}`).
- `forPrincipal(PieceJointe, UUID productId)` factory for the main image (`/image`) — **not wired in this pass** but the user preferred to expose the URL directly via `ProductResponse.image`.

**Rule** : `IProductService.listImages(UUID)` scoped via `ensureBelongsToCurrentEntreprise`. No pagination (the gallery is rarely huge — to revisit if > 100 images).

**Current limit** : no endpoint to order / reorder the gallery. Images are returned in insertion order (FIFO).

---

## 16. Supplier CRUD — `FournisseurServiceImpl`

**Endpoints** (`/api/v1/suppliers`) :

| Method | Endpoint | Permission |
|---|---|---|
| `POST` | `/api/v1/suppliers` | `SUPPLIER_CREATE` |
| `GET` | `/api/v1/suppliers?page=&size=` | `SUPPLIER_READ` |
| `GET` | `/api/v1/suppliers/{id}` | `SUPPLIER_READ` |
| `PUT` | `/api/v1/suppliers/{id}` | `SUPPLIER_UPDATE` |
| `DELETE` | `/api/v1/suppliers/{id}` | `SUPPLIER_DELETE` |

**`FournisseurRequest`** : `@NotBlank nom`, `prenom`, `@Email email`, `@Phone telephone`, `adresse`, `reference`, `origine`.

**`FournisseurResponse`** : `id, nom, prenom, email, telephone, adresse, reference, origine, entrepriseId`. Secondary constructor `(Fournisseur)`.

**Model** : `Fournisseur extends Person` (JOINED inheritance → `person` table for the nom/prenom/email/telephone/adresse fields, `fournisseur` table for `reference` + `origine` + FK `entreprise_id`). Migration `V7__add_entreprise_to_fournisseur.sql` (NOT NULL + FK + index).

**Rules** :
- Scoping `ensureBelongsToCurrentEntreprise(fournisseur)` on all operations.
- `reference` uniqueness per company via `existsByReferenceAndEntrepriseId` — **skipped if `null` or `blank`** to allow suppliers without internal code.
- On update, uniqueness is rechecked only if `reference` has changed.

**Dependencies** : `FournisseurDomainService`, `IEntrepriseService`, `ICurrentUserService`.

---

## 17. ProductFournisseur CRUD — `ProductFournisseurServiceImpl`

**Endpoints** (`/api/v1/product-suppliers`, reused `SUPPLIER_*` permissions) :

| Method | Endpoint | Action |
|---|---|---|
| `POST` | `/api/v1/product-suppliers` | Creates a product ↔ supplier link (201) |
| `GET` | `/api/v1/product-suppliers?page=&size=` | Lists all the company's links (200) |
| `GET` | `/api/v1/product-suppliers?productId={id}` | Lists the suppliers of a given product (200) |
| `GET` | `/api/v1/product-suppliers/{id}` | Detail of a link (200) |
| `PUT` | `/api/v1/product-suppliers/{id}` | Updates prixAchat / referenceFournisseur / origine (200) |
| `DELETE` | `/api/v1/product-suppliers/{id}` | Deletes the link (204) |

**`ProductFournisseurRequest`** : `@NotNull productId`, `@NotNull fournisseurId`, `@NotNull @DecimalMin("0.0", inclusive=false) prixAchat`, `referenceFournisseur` (max 100), `origine` (max 100).

**`ProductFournisseurResponse`** : `id, ProductSummaryResponse product, FournisseurSummaryResponse fournisseur, prixAchat, referenceFournisseur, origine`. Nested sub-DTOs (rule 23). Secondary constructor `(ProductFournisseur)`.

**Reusable sub-DTOs** :
- `ProductSummaryResponse(id, nom, reference)` in `produit/application/dto`
- `FournisseurSummaryResponse(id, nom)` in `achat/application/dto`

**Model** : `ProductFournisseur` enriched with `referenceFournisseur` (max 100, supplier's internal code for this product) and `origine` (max 100, country/brand). `product` and `fournisseur` promoted to `optional=false` + `NOT NULL` columns. Migration `V8__add_traceability_to_product_fournisseur.sql` (2 `ALTER NOT NULL` + 2 new columns + 2 indexes).

**Rules** :
- **Cross-entity scoping** : `IProductService.ensureBelongsToCurrentEntreprise(product)` + `IFournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)` at creation. On read, scoping via `product.entreprise.id`.
- Uniqueness of the `(productId, fournisseurId)` pair via `existsByProductIdAndFournisseurId` — the same product cannot have the same supplier twice.
- **Update limited** to informational fields (prixAchat, referenceFournisseur, origine). The `product`/`fournisseur` FKs are **immutable** — to change the pair, delete + recreate.

**Reused permissions `SUPPLIER_*`** (no `PRODUCT_SUPPLIER_*` created). Whoever manages suppliers manages their pricing. To split later if differentiated access is needed (e.g., pricing reserved to accounting).

**Service `@Transactional(readOnly = true)` at class level** for reads (allows JPQL projections to access LAZY sub-relations during the mapping). `@Transactional` override on mutations.

**Dependencies** : `ProductFournisseurDomainService`, `IProductService`, `IFournisseurService`, `ICurrentUserService`.

---

## 28. Purchase module — 2-step workflow : DRAFT → VALIDATE — `AchatServiceImpl`

**Refactor 2026-05-18** : atomic creation was split into 2 steps to allow **visualization and editing** of the order before commitment (stock materialization + invoice). Symmetric with physical inventory (statuses EN_COURS / BILAN / CLOTURE).

### 28.a DRAFT creation — `POST /api/v1/achats`

**Permission** : `PURCHASE_CREATE` (ADMIN/PROPRIETAIRE/MANAGER).

**Input** : `AchatRequest{ magasinId, fournisseurId, dateCommande, lignes[] }` — **no more `facture`** (entered at validation).
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

**Flow** :
1. PF validations (company scoping + supplier consistency + `prixVente > prixAchat`).
2. Creates `CommandeAchat` in `DRAFT` status (auto reference `CMD-yyyyMMdd-HHmmssSSS`).
3. Persists each `LigneCommandeAchat` with price **snapshot** + lot traceability (numeroLot + dateExpiration, migrated onto the table — V23).
4. **No invoice, no stock entry, no PF prixVente update.**

**Output** : `AchatDraftResponse{ commande }` — HTTP 201. The order can then be inspected (`GET /{id}`), edited line by line or validated.

### 28.b Line edit / delete — `PUT/DELETE /api/v1/achats/orders/{commandeId}/lignes/{ligneId}`

**Permissions** : `PURCHASE_UPDATE` (PUT) / `PURCHASE_DELETE` (DELETE).

**Guard** : `ensureCommandeIsDraft` — forbids any modification once the order moves to RECEPTIONNEE. `ensureLigneBelongsToCommande` (anti URL forging) + `ensureNotLastLigne` (empty order not allowed).

**PUT body** : `LigneAchatUpdateRequest{ quantite, prixAchat, prixVente, numeroLot?, dateExpiration? }`. Re-validation `prixVente > prixAchat`. Returns the updated `LigneCommandeAchatResponse`.

**DELETE** : 204. Refuses if it is the last line (`commandeAchat.cannotDeleteLastLigne`).

### 28.c Validation — `POST /api/v1/achats/{commandeId}/validate`

**Permission** : `PURCHASE_APPROVE`.

**Input** : `AchatValidateRequest{ facture: { numero, date, dateEcheance } }`.

**Atomic flow (single transaction)** :
1. Validations : `ensureBelongsToCurrentEntreprise` + `ensureCommandeIsDraft`.
2. Recomputes `montantTotal = SUM(qty × prixAchat)` from the current lines (may have been edited).
3. Creates `FactureAchat` linked to the order (status NON_PAYEE).
4. For each line : creates `EntreeStock` (FIFO lot with numero lot + dateExpiration), upserts aggregate `Stock`, journals `MouvementStock(ENTREE_ACHAT)`, applies `pf.prixVente = ligne.prixVente` (moved from create — otherwise an unvalidated draft would contaminate the current sale price).
5. Switches `commande.statut → RECEPTIONNEE` via `commandeAchatDomainService.validate` (rule 26).

**Output** : `AchatResponse{ commande (status RECEPTIONNEE), facture }` — HTTP 200.

### 28.d Detail — `GET /api/v1/achats/{commandeId}`

**Permission** : `PURCHASE_READ`. Returns `AchatDetailsResponse(commande, facture (null if DRAFT), lignes[])`.

### DB migration V23

`ligne_commande_achat` : `+numero_lot VARCHAR(100)`, `+date_expiration DATE`. Previously this info was passed directly from `LigneAchatRequest` to `EntreeStockCreate` at atomic creation time ; with the DRAFT/VALIDATE split, traceability must be persisted on the line between the 2 phases.

**Exposed sub-services** :
- `ICommandeAchatService` : `findResponsesByFilter` (paginated listing), `findResponseById`.
- `IFactureAchatService` : same + `findEcheances(FactureAchatEcheanceFilter)` (unpaid invoices by time window).
- `IPaiementAchatService` : `addPayment(factureId, PaiementAchatRequest)` (checks `montantRestant`).

**Additional endpoints** :
| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `GET` | `/api/v1/purchases/orders?magasinId=&fournisseurId=&startDate=&endDate=&page=&size=` | `PURCHASE_READ` | Lists filtered orders |
| `GET` | `/api/v1/purchases/orders/{id}` | `PURCHASE_READ` | Order detail |
| `GET` | `/api/v1/purchases/invoices?...` | `PURCHASE_READ` | Lists filtered invoices |
| `GET` | `/api/v1/purchases/invoices/echeances?fromDate=&toDate=&page=&size=` | `PURCHASE_READ` | Unpaid invoices (with dateEcheance in the window) |
| `GET` | `/api/v1/purchases/invoices/{id}` | `PURCHASE_READ` | Invoice detail |
| `POST` | `/api/v1/purchases/invoices/{id}/payments` | `PAYMENT_CREATE` | Adds a payment to an invoice |
| `GET` | `/api/v1/purchases/invoices/{id}/payments` | `PAYMENT_READ` | Lists an invoice's payments |

**`<X>Create` records** (params >3 grouping, rule 30) :
- `FactureAchatCreate(numero, date, dateEcheance)`
- `LigneCommandeCreate(productFournisseur, quantite, prixUnitaire)`
- `PaiementAchatCreate(facture, montant, modePaiement)`
- Filters : `CommandeAchatFilter`, `FactureAchatFilter`, `FactureAchatEcheanceFilter` (validated by `ValidatorService`).

**Cross-cutting helper** : `ReferenceHelper.generate(String base)` in `org.store.common.tools` — returns `"{base}-yyyyMMdd-HHmmssSSS"`. Used for order references, to reuse for future references (sale invoice, etc.).

**Permissions** :
- `PURCHASE_CREATE` / `PURCHASE_READ` (ADMIN/PROPRIETAIRE/MANAGER).
- `PAYMENT_CREATE` / `PAYMENT_READ` (same roles).

**Business rules** :
- No partial reception : 1 delivery = 1 order + 1 invoice + N lines entered in one go.
- `montantFacture` and `montantAccompte` **computed** (never directly entered).
- `PaiementAchat.montant` cannot exceed `montantFacture - sum_existing_payments` (`BadArgumentException("paiementAchat.montant.exceedsRemaining")`).

**Tests refactor 2026-05-18** : suite **725 / 725 green** (+14 vs 711). 13 service `AchatServiceImplTest` (create DRAFT + validate + updateLigne + deleteLigne + findDetails) + 8 controller (DRAFT creation 201, validate 200, update line 200, delete 204, 400 on edge cases).

---

## 29. Expense module — CategoryDepense + Depense CRUD — `CategoryDepenseServiceImpl`, `DepenseServiceImpl`

### 29.a CategoryDepense — reference data scoped per company

**Endpoints** (`/api/v1/expense-categories`) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/expense-categories` | `EXPENSE_CATEGORY_CREATE` | Creates a category (201) |
| `GET` | `/api/v1/expense-categories?page=&size=` | `EXPENSE_CATEGORY_READ` | Paginated list (200) |
| `GET` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_READ` | Detail (200) |
| `PUT` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_UPDATE` | Update (200) |
| `DELETE` | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_DELETE` | Delete (204) |

**`CategoryDepenseRequest`** : `@NotBlank @Size(max=100) nom`, `@Size(max=500) description`, `Boolean actif` (default true).

**`CategoryDepenseResponse`** : `id, nom, description, actif`. Secondary constructor `(CategoryDepense)`.

**Model** : `CategoryDepense` enriched with a `@ManyToOne(optional=false) Entreprise entreprise`. Migration **V10** : adds FK `entreprise_id NOT NULL` + replaces the auto-generated global uniqueness on `nom` with a `(entreprise_id, nom)` uniqueness (via PostgreSQL `DO $$ ... $$` block to drop the auto-named Hibernate constraint).

**Rules** :
- Company scoping via `ICurrentUserService` (manager OR owner).
- `nom` uniqueness per company (`UniqueResourceException("categoryDepense.nom.alreadyExists")`).
- Cross-tenant forbidden : `ensureBelongsToCurrentEntreprise(category)` (otherwise `ForbiddenException("categoryDepense.notOwned")`).

---

### 29.b Depense — operation scoped per store

**Endpoints** (`/api/v1/depenses`) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/depenses` | `EXPENSE_CREATE` | Creates an expense (201) |
| `GET` | `/api/v1/depenses?magasinId=&categoryId=&modePaiement=&startDate=&endDate=&page=&size=` | `EXPENSE_READ` | Filtered paginated list (200) |
| `GET` | `/api/v1/depenses/total?magasinId=&categoryId=&modePaiement=&startDate=&endDate=` | `EXPENSE_READ` | Aggregated sum + count (200) |
| `GET` | `/api/v1/depenses/{id}` | `EXPENSE_READ` | Detail (200) |
| `PUT` | `/api/v1/depenses/{id}` | `EXPENSE_UPDATE` | Update (200) |
| `DELETE` | `/api/v1/depenses/{id}` | `EXPENSE_DELETE` | Delete (204) |

**`DepenseRequest`** : `@NotNull UUID magasinId`, `@NotNull UUID categoryId`, `@NotBlank @Size(max=200) libelle`, `@Size(max=1000) description`, `@NotNull LocalDate dateDepense`, `@NotNull @DecimalMin("0.0", inclusive=false) BigDecimal montant`, `@NotNull MoyenPaiement modePaiement`.

**`DepenseResponse`** : `id, MagasinSummaryResponse magasin, CategoryDepenseSummaryResponse category, libelle, description, dateDepense (String), montant, modePaiement, createdAt (String)`. Summary sub-DTOs. Dates formatted via `DateHelper.format()`.

**`DepenseFilter`** (record validated by `ValidatorService`, rule 30) : `@NotNull UUID magasinId`, `UUID categoryId`, `@EnumValue(MoyenPaiement.class) String modePaiement`, `@DatePattern("yyyy-MM-dd") String startDate`, `@DatePattern("yyyy-MM-dd") String endDate`, `@Min(0) int page`, `@Min(1) @Max(100) int size`. Utility methods : `modePaiementAsEnum()`, `startDateTime()`, `endDateTime()`, `toPageable()`.

**`DepenseTotalResponse`** : `magasinId, BigDecimal montantTotal, Long nombreDepenses`.

**Model** : `Depense` linked to `@ManyToOne Magasin` + `@ManyToOne CategoryDepense`. Migration **V10** : adds column `mode_paiement VARCHAR(20) NOT NULL DEFAULT 'CASH'` + `DROP COLUMN IF EXISTS date_echeance` (business decision : an expense is one-shot, not a debt with a due date).

**Rules** :
- Store scoping : `magasinService.ensureAccessibleByCurrentUser(magasin)` at creation / read / update / delete (`ForbiddenException("magasin.notOwned")`).
- Cross-entity scoping : `categoryDepenseService.ensureBelongsToCurrentEntreprise(category)` (`ForbiddenException("categoryDepense.notOwned")`).
- Listing filter : `magasinId` mandatory, others optional. JPQL query with SpEL `:#{#filter.X}`.

**Tests** : 15 new tests (controllers + service impls). Suite at **392 / 392 green**.

---

## 30. Client (sale, feature 1) — `ClientServiceImpl`

**Endpoints** (`/api/v1/clients`) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `POST` | `/api/v1/clients` | `CLIENT_CREATE` | Creates a client attached to a store accessible by the caller (201) |
| `GET` | `/api/v1/clients?nom=&prenom=&page=&size=` | `CLIENT_READ` | Paginated scoped list (employee = store, owner = company) with optional name/firstname filters (200) |
| `GET` | `/api/v1/clients/{id}` | `CLIENT_READ` | Detail (200) |
| `PUT` | `/api/v1/clients/{id}` | `CLIENT_UPDATE` | Update (store change allowed if new store accessible) (200) |
| `DELETE` | `/api/v1/clients/{id}` | `CLIENT_DELETE` | Delete (204) |

**`ClientRequest`** : `@NotBlank @Size(max=255) nom`, `@Size(max=255) prenom`, `@Email @Size(max=255) email`, `@Phone @Size(max=30) telephone`, `@Size(max=255) adresse`, `@NotNull UUID magasinId`.

**`ClientResponse`** : `id, nom, prenom, email, telephone, adresse`. No exposure of the store or the company (scoping invisible to the client). Secondary constructor `(Client)`.

**`ClientSummaryResponse(id, nomComplet)`** — reusable sub-DTO. `nomComplet = "nom prenom"` or just `nom` if firstname blank. Will be used in the future `SaleResponse`.

**`ClientFilter`** (record validated by `ValidatorService`, rule 30 + rule "Filter DTO ≥ 2 criteria") : `String nom`, `String prenom`, `@Min(0) int page`, `@Min(1) int size`. Utility method : `toPageable()`. Null handling directly in JPQL via `(:nom IS NULL OR LOWER(c.nom) LIKE ...)`.

**Model** : `Client extends Person` (nom/prenom/email/telephone/adresse inherited) + `@ManyToOne Magasin magasin`. No migration (`client` table already existed since `V1__init_schema.sql`).

**Rules** :
- Double scoping via `ensureAccessibleByCurrentUser(Client)` : employee = `client.magasin.id == currentUser.magasinId`, owner = `client.magasin.entreprise.id == currentUser.entrepriseId`. Otherwise `ForbiddenException("client.notOwned")`.
- Target store checked at creation / update via `IMagasinService.ensureAccessibleByCurrentUser` (cross-service, `ForbiddenException("magasin.notOwned")` if the caller has no access).
- No uniqueness on `telephone` (homonyms acceptable in a parts shop).
- Listing : store (employee) vs company (owner) selection is internal to the service ; no option exposed to the API.

**Related project decision** : the "anonymous client" is NOT a Client record — when we tackle F-V3 (atomic sale), `CommandeVente.client` will simply be nullable if the seller does not enter a client.

**Tests** : 15 service + 9 controller. Suite at **415 / 415 green**.

---

## 31. Seller product search (sale, feature 2) — `ProductSearchServiceImpl` + model adjustments

**Structural model changes (migration V11)** :
- **`Quality` moved from `Product` to `ProductFournisseur`** : the same product can be delivered by a supplier in several distinct qualities (e.g., *10mm nail* × *China* in *original* AND in *counterfeit* = 2 different PFs).
- **PF uniqueness** : `UNIQUE (product_id, fournisseur_id, quality_id)`.
- **`ProductFournisseur.prixVente`** : current sale price for the (product, supplier, quality) combination. Updated on every purchase or via a dedicated PUT endpoint.
- **`LigneCommandeAchat.prixVente`** : snapshot of the sale price at purchase time (invoice traceability).

**Business validation** : `prixVente > prixAchat` (strictly positive margin) on every entry (PF creation, purchase, PUT sale price). i18n key `productFournisseur.prixVente.belowOrEqualAchat`.

**`ProductController` endpoints** :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `GET` | `/api/v1/products/search?q=&magasinId=&page=&size=` | `PRODUCT_READ` | Search products with active lots in a store (200) |

**`ProductFournisseurController` endpoints** (in addition to CRUD) :

| Method | Endpoint | Permission | Action |
|---|---|---|---|
| `PUT` | `/api/v1/product-suppliers/{id}/prix-vente` | `SUPPLIER_UPDATE` | Freely updates the PF's current sale price (manager). Validation `> prixAchat` (200) |

**`ProductSearchResponse`** : `id, nom, reference, description, category, image, quantiteEnStock, productFournisseurs[]`.

**`ProductFournisseurStockResponse`** (sub-DTO) : `id, quality, fournisseur, prixVente, quantiteEnStock` (= SUM of the PF's active lots in the store).

**`magasinId` resolution** :
- EMPLOYE : if parameter absent, automatically derived from `UserPrincipal.magasinId`.
- PROPRIETAIRE : parameter mandatory (otherwise `BadArgumentException("product.search.magasinIdRequired")`).

**Architecture** :
- Dedicated service `IProductSearchService` (isolated to break the cycle `IProductService` ↔ `IEntreeStockService` ↔ `IProductFournisseurService`).
- 2 queries to avoid N+1 : (1) `Page<Product>` paginated by search + EXISTS active lot ; (2) list of active `EntreeStock` for the paginated IDs, fetch-joined PF/fournisseur/quality. Aggregation by product then by PF in Java.

**Automatic update of the PF's `prixVente` on every purchase** : `AchatServiceImpl.createLignesAndComputeTotal()` calls `IProductFournisseurService.applyPrixVenteFromPurchase(pf, ligne.prixVente())` after line creation.

**Tests** : 5 service search + 1 controller search + `updatePrixVente` tests + adaptations of existing ones. Target total : **425 green** (the only failing test is `StoreApplicationTests.contextLoads` because of a Flyway *failed* state to clean manually after a first V11 attempt).

---

## 32. Atomic sale (sale, feature 3) — `VenteServiceImpl`

> ⚠️ **Refactor 2026-05-18 — DRAFT → VALIDATE 2-step workflow** : atomic creation was split into 2 steps to allow visualization + editing of a sale before cash-in. **See section 48 for the new workflow** (this section describes the historical pre-refactor state).

**Main endpoint** : `POST /api/v1/ventes` (permission `SALE_CREATE`).
**Detail endpoint** : `GET /api/v1/ventes/{commandeId}` (permission `SALE_READ`).

**Business use case** : a seller (logged-in Employe) serves a client at the counter. The client picks one or more **variants** (`ProductFournisseur` = product × supplier × quality), with a unit price ≥ the PF's floor price. The system consumes FIFO stock from the PF, creates the order + lines + invoice + possible initial payment, all in a single transaction.

**Associated migrations** :
- **V12** `add_paiement_vente_and_pf_on_ligne_vente.sql` : creates the `paiement_vente` table (montant, datePaiement, moyen, FK invoice, audit) + adds `ligne_commande_vente.product_fournisseur_id` (FK to `product_fournisseur`) — for sale by variant.
- **V13** `remove_vendeur_from_commande_vente.sql` : drops `commande_vente.vendeur_id` (redundant with `createdBy` from `AuditableEntity` which already carries the creator's `accountId`). The seller is resolved on read via `IAccountService.findUserSummaryByAccountId(createdBy)`.
- **V14** `enforce_pf_not_null_on_ligne_vente.sql` : hardens `ligne_commande_vente.product_fournisseur_id` to `NOT NULL` (the application flow always sets the value, DB constraint for out-of-flow inserts).
- **V15** `rename_date_echeache_to_date_echeance_on_facture_client.sql` : historical typo fix `facture_client.date_echeache → date_echeance` + safety backfill (null due date → invoice date) + `SET NOT NULL`. The entity field changes from `dateEcheache` to `dateEcheance` (matches the spelling of the other tables `echeance`/`facture_achat`/`depense`).
- **V16** `drop_redundant_montants_on_commande_vente.sql` : drops `commande_vente.montant_total` and `commande_vente.montant_paye`. The amounts now live only on `FactureClient` (1:1 order↔invoice relation guaranteed since F-V3). Avoids de-sync. `CommandeVenteResponse` receives `montantTotal/montantPaye` as explicit arguments (from the associated invoice), fetched via `LEFT JOIN FactureClient` in the listing and detail queries.

**Input** : `VenteRequest` (4 fields)
```json
{
  "clientId": "uuid-or-null",
  "dateEcheance": "2026-05-30",    // MANDATORY, entered by the seller, @FutureOrPresent
  "lignes": [
    { "productFournisseurId": "uuid-chine", "quantite": 100, "prixUnitaire": 10 },
    { "productFournisseurId": "uuid-maroc", "quantite": 20,  "prixUnitaire": 15 }
  ],
  "premierPaiement": {              // optional
    "montant": 1300,
    "modePaiement": "CASH",
    "datePaiement": "2026-05-16"    // optional in the request, default now() on the service side, @PastOrPresent
  }
}
```
**`dateVente` is NOT in the request** — systematically set to `LocalDate.now()` by `VenteServiceImpl.create` (consistent with over-the-counter sale : business date = recording date). No more backdating possible via the API.

**Atomic flow (single transaction)** :
1. Seller fetched via `IEmployeService.findCurrentUser()` → throws `ForbiddenException("vente.user.required")` if the logged user is not an Employe (PROPRIETAIRE refused).
2. Store derived from `vendeur.magasin`.
3. Client resolved via `IClientService.findById` + double scoping if `clientId` non-null ; otherwise anonymous sale.
4. For each line : `productFournisseurService.ensureBelongsToCurrentEntreprise` + validation `prixUnitaire ≥ pf.prixVente` (otherwise `BadArgumentException("vente.prixUnitaire.belowFloor", floor)`).
5. Creates `CommandeVente` (auto reference `VTE-yyyyMMdd-HHmmssSSS` via `ReferenceHelper.generate("VTE")`, status `DELIVERED`). **No `vendeur` field** on the entity : the creator's identity is carried by audit `createdBy` (= stringified `accountId`).
6. For each line :
   - Creates `LigneCommandeVente` with `productFournisseur` + `product` + `quantite` + `prixUnitaire` + `montantTotal = qty × prix`.
   - Calls `ISortieStockService.consumeForVente(SortieStockForVente(magasin, pf, qty, prix, ligneVente))` :
     - Fetches FIFO lots of the PF in the store (`EntreeStockRepository.findAvailableLotsForFifoByProductFournisseur`).
     - Checks `SUM(qtyRestante) ≥ qty` (otherwise `BadArgumentException("stock.exit.insufficientQuantity", available, requested)`).
     - Consumes FIFO, creates 1 `SortieStock` per consumed lot (with margin = `(prixVente − prixAchatLot) × qty`), linked to the `LigneCommandeVente` via FK `sortie_stock.ligne_vente_id`.
     - Decrements `Stock.quantiteDisponible` and journals `MouvementStock(SORTIE_VENTE)`.
7. Computes `montantTotal = SUM(lignes.montantTotal)`, `applyMontantTotal` on the order.
8. Creates `FactureClient` (auto number `FAC-VTE-yyyyMMdd-HHmmssSSS`, status `NON_PAYEE`, `date = effective dateVente`, `dateEcheance = venteRequest.dateEcheance()` — the seller entered it, mandatory).
9. If `premierPaiement` : `PaiementVente` created via `PaiementVenteCreate(facture, montant, moyen, datePaiement)` where `datePaiement = request.datePaiement()` if provided otherwise `LocalDate.now()` + `factureClientDomainService.applyPaiement` (recomputes `montantPaye` + status `NON_PAYEE`/`PARTIELLEMENT_PAYEE`/`PAYEE`) + `commandeVenteDomainService.applyMontantPaye(commande, montant)` (increments from the existing).

**Output** : `VenteResponse{ commande, facture }` — HTTP 201. The full detail (lines + payments) is available via `GET /api/v1/ventes/{commandeId}`.

**`VenteDetailsResponse`** (`GET /api/v1/ventes/{commandeId}`) : `{ commande, facture, lignes[], paiements[] }`. `commande.user` = `UserSummaryResponse(id, nomComplet)` resolved via `IAccountService.findUserSummaryByAccountId(commande.createdBy)` — Minimalist Option pattern : no redundant `user_id` FK on the tables, we rely on the `createdBy` audit (= stringified accountId).

**`<X>Create` records** :
- `CommandeVenteCreate(client, magasin, dateVente, reference, statut)`
- `LigneCommandeVenteCreate(commande, productFournisseur, quantite, prixUnitaire)`
- `FactureClientCreate(commande, numero, date, dateEcheance, montantTotal)`
- `PaiementVenteCreate(facture, montant, moyen, datePaiement)` — groups the 4 values to respect rule 30 (max 3 parameters).
- `SortieStockCreate(lot, quantite, prixVente, ligneVente)` — also supports adjustments (ligneVente=null).
- `SortieStockForVente(magasin, productFournisseur, quantite, prixVente, ligneVente)` — parameter orchestrating the full sale exit flow.
- `LotConsumptionContext(totalAConsommer, prixVente, ligneVente)` — parameters common to both FIFO paths in the stock (`create` plain and `consumeForVente`) ; allows a single shared `consumeFifo` loop.
- `VenteContext(request, commande, magasin, user, productFournisseurs)` — parameter of VenteServiceImpl's internal methods.

**Cross-cutting helpers introduced / consumed** :
- `common/tools/NameHelper.formatNomComplet(String nom, String prenom)` : factors out the construction of the "nom + prenom" label previously duplicated in `AccountServiceImpl`, `VenteServiceImpl`, `ClientSummaryResponse` (and fixes in passing a latent bug that could produce `"null prenom"`).
- `common/tools/UuidHelper.parseOptional(String) → Optional<UUID>` : safe variant of `parse()` (empty if null/blank/invalid format), used to resolve `commande.createdBy` in `IAccountService.findUserSummaryByAccountId`.

**`UserPrincipal` refactor** (cross-cutting impact) :
- Adds `userId` (= `Utilisateur.id` business, = `Employe.id` for an employee).
- Existing `userId` renamed → `accountId` (= `Account.id` auth).
- New JWT claim `Claim.USER("userId")`.
- `AuditorAwareImpl` migrated to `principal.accountId()` (the value stored in `createdBy/updatedBy` remains the `accountId`).
- 24 tests adapted (`new UserPrincipal(accountId, userId, ...)`) across purchase / depense / entreprise / magasin / produit / security / stock / users / vente.

**Permissions** :
- `SALE_CREATE` : ADMIN, PROPRIETAIRE, MANAGER, VENDEUR (already in YAML).
- `SALE_READ` : same.

**Business rules** :
- Seller = Employe mandatory (`UserPrincipal.userId` must point to an Employe in DB ; otherwise Forbidden).
- Client nullable (anonymous sale — project decision `project_client_anonyme`).
- 1 line = 1 unique PF. Mix of variants of the same product → N lines in the same `VenteRequest`.
- Unit price ≥ `pf.prixVente` (PF floor). No FIFO MAX since the sale price lives on the PF (a single price per variant).
- Stock controlled by PF (not by Product), via `EntreeStockRepository.findAvailableLotsForFifoByProductFournisseur`.
- Order reference number + invoice number auto-generated (seller enters nothing). Frontend reader only.
- Seller (`commande.user`) displayed via `UserSummaryResponse(id, nomComplet)` resolved on read from `commande.createdBy` (= stringified accountId) → `IAccountService.findUserSummaryByAccountId`.
- **Dates** :
  - `dateVente` **no longer in the request** : set to `LocalDate.now()` at every creation. Stored on `CommandeVente.date` and `FactureClient.date`.
  - `dateEcheance` **mandatory in the request** (`@NotNull @FutureOrPresent`), entered by the seller. Cash sale → frontend passes `dateEcheance = today`.
  - `premierPaiement.datePaiement` optional (`@PastOrPresent`), default `LocalDate.now()` on the application service side (the domain no longer does `LocalDate.now()` implicitly, resolution lives in `VenteServiceImpl`).

**Tests** : 8 service tests `VenteServiceImplTest` (POST happy orchestration, default dateVente=today if null, throw non-Employe user, throw price below floor, apply first payment, request datePaiement honored, GET detail, GET 404 invoice, GET forbidden non-owned) + 3 controller (POST 201, POST 400 empty lines, GET 200). Dedicated `FactureClientDomainServiceTest` (5 `applyPaiement` transitions) and `SortieStockServiceImplTest.consumeForVente_*` (3 cases) tests. **448 / 448 green**.

---

## 33. Sale listings (sale, feature 4) — `CommandeVenteServiceImpl`, `FactureClientServiceImpl`, `PaiementVenteServiceImpl`

**5 endpoints** (all with `SALE_READ` permission) :
- `GET /api/v1/commandes-vente?magasinId=&clientId?&vendeurId?&statut?&reference?&montantMin?&montantMax?&startDate?&endDate?&page&size` — paginated filtered listing of sale orders with multi-criteria search.
- `GET /api/v1/commandes-vente/{id}` — order detail **with `user` (seller) resolved** via JPQL projection.
- `GET /api/v1/factures-client?magasinId=&clientId?&statut?&startDate?&endDate?&page&size` — paginated filtered listing of client invoices.
- `GET /api/v1/factures-client/{id}` — invoice detail.
- `GET /api/v1/factures-client/{id}/paiements` — payments of an invoice, paginated (Spring Data `Pageable`, sort configurable via `?sort=`).

**Business use case** : symmetric with the Purchase listings F12-F14 (`CommandeAchatController`, `FactureAchatController`). The seller / manager consults a store's sale, invoice and payment history. All views return `Page<>` sorted by `createdAt DESC` by default.

**JPQL projection strategy (rule 24)** :
- Listings (`SELECT new <X>Response(entity)`) without seller resolution on the query side (cost saving : no N+1 on Account/Utilisateur over pages of N items). The `user` field is null in the list items.
- Order detail (`GET /commandes-vente/{id}`) resolves the seller in **one JPQL query** via `LEFT JOIN org.store.security.domain.model.Account a ON CAST(a.id AS string) = c.createdBy LEFT JOIN a.user u` + `TRIM(BOTH FROM CONCAT(COALESCE(u.nom, ''), ' ', COALESCE(u.prenom, '')))`. The CAST is needed because `AuditableEntity.createdBy` is stored as `String` (stringified UUID).

**Added JPQL constructors** (`CommandeVenteResponse`) :
- `(CommandeVente commande)` — listing, `user = null`.
- `(CommandeVente commande, UUID userId, String nomComplet)` — GET by id detail, instantiates `UserSummaryResponse` only if `userId != null`.

**Filters** (records in `vente/application/dto/`) :
- `CommandeVenteFilter(magasinId, clientId?, vendeurId?, statut?, reference?, montantMin?, montantMax?, startDate?, endDate?, page, size)` — 11 fields. Multi-criteria search : seller, status (`@EnumValue(CommandeVenteStatut)`), reference (LIKE insensitive), total amount range, audit date range. `statutAsEnum()` via `EnumHelper.parse`. `toPageable()` sorts DESC `createdAt`. The `vendeurId` filter is resolved via `LEFT JOIN Account a ON CAST(a.id AS string) = c.createdBy + a.user.id = :vendeurId` (the `createdBy` audit is the stringified `accountId`, we trace back to the Employe via `Account.user`).
- `FactureClientFilter(magasinId, clientId?, statut?, startDate?, endDate?, page, size)` + `statutAsEnum()` via `EnumHelper.parse`.

**Multi-tenant** : each JPQL query mandatorily filters by `entrepriseId` from `currentUserService.getCurrent()`. Scoping is applied **in the query's WHERE** (not post-load), so an order / invoice / payment from another company is invisible (empty page or `EntityException("notFound")` rather than a `notOwned` that would leak the existence of the resource). Additionally, the controller goes through `IMagasinService.ensureAccessibleByCurrentUser` to verify the caller's store access on listings.

**Permissions** : `SALE_READ` on all endpoints (already in YAML : ADMIN, PROPRIETAIRE, MANAGER, VENDEUR).

**i18n** : 2 new FR/EN keys — `commandeVente.notFound`, `factureClient.notFound`.

**Tests** : 6 service `CommandeVenteServiceImplTest` (validate filter, magasin not accessible forbidden, getById happy with user, getById notFound) + 6 service `FactureClientServiceImplTest` + 2 service `PaiementVenteServiceImplTest` + 4 controller `CommandeVenteControllerTest` (list 200 without user, list_forward_all_filter_params via ArgumentCaptor on the 9 query params, getById 200 with user, getById 406 notFound) + 4 controller `FactureClientControllerTest` (list 200, getById 200, getById 406, listPaiements 200). **466 / 466 green** (+18 vs 448 pre-F-V4).

---

## 34. Daily cash register summary — `CaisseServiceImpl`

**Endpoint** : `GET /api/v1/ventes/caisse/resume?magasinId=&date=YYYY-MM-DD` (permission `SALE_READ`).

**Business use case** : the seller or manager closes their cash register day — how many orders recorded, how many items sold (sum of quantities, not number of lines), monetary total of orders, and total of payments actually cashed-in that day (cash drawer semantics : money that came into the till today, regardless of the originating sale's date).

**Input** : `CaisseResumeFilter(magasinId @NotNull UUID, date @NotBlank @DatePattern String)` + accessors `startOfDay()` / `endOfDay()` (`LocalDateTime`, via `DateHelper.parseStartOfDay/parseEndOfDay`) + `dateAsLocalDate()`.

**Output** : `CaisseResumeResponse(magasinId, date, nombreCommandes, nombreProduits, totalCommandes, totalPaiements)`.

**4 aggregated scalar JPQL queries** (filter `createdAt BETWEEN startOfDay AND endOfDay`, consistent with F-V4-bis) :

1. `SELECT COUNT(c) FROM CommandeVente c WHERE c.magasin.entreprise.id = :entrepriseId AND c.magasin.id = :magasinId AND c.createdAt BETWEEN ...` → `nombreCommandes`
2. `SELECT COALESCE(SUM(c.montantTotal), 0) FROM CommandeVente c WHERE ...` (same WHERE) → `totalCommandes`
3. `SELECT COALESCE(SUM(l.quantite), 0) FROM LigneCommandeVente l WHERE l.commande.magasin.entreprise.id = :entrepriseId AND l.commande.magasin.id = :magasinId AND l.commande.createdAt BETWEEN ...` → `nombreProduits` (= sum of quantities, not number of lines)
4. `SELECT COALESCE(SUM(p.montant), 0) FROM PaiementVente p WHERE p.facture.commande.magasin.entreprise.id = :entrepriseId AND p.facture.commande.magasin.id = :magasinId AND p.createdAt BETWEEN ...` → `totalPaiements` (all payments created that day, including installment payments on earlier sales count in today's cash drawer)

**Multi-tenant** : `entreprise.id` scoping in each of the 4 queries + preliminary check of the caller's store access via `IMagasinService.ensureAccessibleByCurrentUser` (a seller can only check the summary of a store they are attached to).

**Application service** (`CaisseServiceImpl`) : aggregates the 4 scalar values into a single response. No "all-in-one" query on purpose — each aggregation is isolated (readable, testable, reusable individually by other services if future dashboard needs arise).

**Permissions** : `SALE_READ` (already in YAML — ADMIN, PROPRIETAIRE, MANAGER, VENDEUR). No new permission.

**Tests** : 3 service `CaisseServiceImplTest` (happy path of the 4-value aggregation, magasin not accessible forbidden propagated from IMagasinService, zero values when no activity for the day — `COALESCE(SUM, 0)` on the JPQL side avoids null returns) + 1 controller `CaisseControllerTest` (GET 200 on `/resume?magasinId=&date=` with assertions on the 4 fields). **470 / 470 green** (+4 vs 466).

### 34.bis Top N best-selling products

**Endpoint** : `GET /api/v1/ventes/caisse/top-produits?magasinId=&date?&nombre?` (permission `SALE_READ`).
- `magasinId` mandatory, `date` optional (default today via `TopProduitsFilter.effectiveDate()`), `nombre` optional (default 3, `@Min(1)`).

**Use case** : display at cash register close "the 3 products that moved the most today". Sort by **total quantity sold** (user decision, not by revenue).

**Response** : `List<TopProduitResponse(productId, nom, reference, quantiteVendue, chiffreAffaires)>`.

**Filter** : `TopProduitsFilter(magasinId, date?, nombre)` with accessors `effectiveDate()` (today if null/blank), `startOfDay()` / `endOfDay()`, `toPageable()` = `PageRequest.of(0, nombre)`.

**JPQL query** (on `LigneCommandeVenteRepository`) :
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
Limit via `Pageable` (= `PageRequest.of(0, nombre)`). The repo returns `List<TopProduitResponse>`.

**Tests** : +2 service (`findTopProduits_should_delegate_and_return_list`, `findTopProduits_should_use_today_when_date_null`) + 2 controller (200 with default `nombre=3`, 200 with custom `nombre` + without `date`). **474 / 474 green**.

---

## 35. Installment payment (sale, feature 5) — `PaiementVenteServiceImpl`

**Endpoint** : `POST /api/v1/factures-client/{id}/paiements` (permission `SALE_PAY`). Body : `PaiementVenteRequest`. Status 201.

**Business use case** : client credit. A partially paid sale (status `PARTIELLEMENT_PAYEE`) can receive additional payments until reaching `PAYEE`. Direct symmetry with `POST /api/v1/factures-achat/{id}/paiements` (purchase side).

**Logic** :
1. `validatorService.validate(request)`.
2. Loads `FactureClient` via `findById` + `ensureBelongsToCurrentEntreprise(facture, currentUser.entrepriseId)` (compares `facture.commande.magasin.entreprise.id`).
3. `ensureNotAlreadyPaid(facture)` : rejects if status = `PAYEE` (`factureClient.alreadyPaid`).
4. `ensureAmountDoesNotExceedRemaining(facture, montant)` : `montant + montantPaye ≤ montantTotal` (`paiementVente.exceedsRemainingAmount`).
5. Resolves `datePaiement = request.datePaiement() ?? LocalDate.now()`.
6. Creates the payment via `PaiementVenteDomainService.create(PaiementVenteCreate)`.
7. Updates the invoice via `FactureClientDomainService.applyPaiement(facture, montant)` (recomputes status auto : `PARTIELLEMENT_PAYEE → PAYEE` when the total is reached).
8. Returns `PaiementVenteResponse`.

**No more propagation on `commande.montantPaye`** : since V16 (refactor to remove redundancy, see section 32), the amounts live only on `FactureClient`. The F-V5 code is thus simplified — only 1 update step (on the invoice).

**Public validations** (rule 27) in `PaiementVenteServiceImpl` : `ensureBelongsToCurrentEntreprise`, `ensureNotAlreadyPaid`, `ensureAmountDoesNotExceedRemaining`. Usable individually, testable in isolation.

**i18n** : 2 new FR/EN keys — `factureClient.alreadyPaid`, `paiementVente.exceedsRemainingAmount`.

**Permissions** : `SALE_PAY` (already in YAML — ADMIN, PROPRIETAIRE, MANAGER, VENDEUR). Permission distinct from `SALE_READ` (a read-only account cannot create a payment).

**Tests** : 5 service `PaiementVenteServiceImplTest` (create happy + custom datePaiement + forbidden if other company + alreadyPaid blocks + exceedsRemaining blocks) + 2 controller `FactureClientControllerTest` (POST 201 happy, POST 400 if amount missing). **481 / 481 green** (+7 vs 474).

---

## 36. Subscription catalog CRUDs (ADMIN) — `PlanAbonnementServiceImpl`, `SubscriptionTypeServiceImpl`, `CouponServiceImpl`, `PromotionServiceImpl`

**Endpoints** : `/api/v1/plans`, `/api/v1/subscription-types`, `/api/v1/coupons`, `/api/v1/promotions` (POST/GET/GET id/PUT/PATCH activate/PATCH deactivate/DELETE). Permissions `PLAN_*`, `SUBSCRIPTION_TYPE_*`, `COUPON_*`, `PROMOTION_*`.

**Business use case** : only the SaaS ADMIN manages the **global reference catalog** of subscription (tiers, durations, promo codes, automatic promotions). These entities are not multi-tenant — they are shared across all customer companies.

**Common logic (pattern copied from `CategoryProductServiceImpl`)** :
1. `validatorService.validate(filter)` as the first line of `findAll(filter)` (rule 33).
2. Creation : uniqueness (name/code), consistency validation (`SubscriptionRules.ensureReductionConsistent` + `ensurePeriodValid`), persistence via `<X>DomainService.create(request[, plan])`.
3. Update : `findById` + uniqueness recheck if name/code changed + consistency revalidation + `applyRequest` + save.
4. Activate/Deactivate via `<X>DomainService.setActive(entity, boolean)` (rule 26 — no setter+save in the app service).
5. For Coupon/Promotion : optional plan resolution via `IPlanAbonnementService.findByIdOrNull(UUID)` (default method).

**Common validations** (helper `org.store.common.tools.SubscriptionRules`, rules 4 + 27) :
- `ensureReductionConsistent(reductionType, valeurReduction, invalidKey)` : type without value (or inverse) forbidden, POURCENTAGE ≤ 100.
- `ensurePeriodValid(dateDebut, dateFin, invalidPeriodKey)` : `dateFin ≥ dateDebut`.

**i18n** : `plan.notFound/alreadyExists/reduction.invalid`, `subscriptionType.*`, `coupon.*`/`expired`/`exhausted`/`notApplicable`, `promotion.*`.

**Tests** : 14 service + 9 controller (Plan), 14 service + 8 controller (Type), 12 service + 8 controller (Coupon), 12 service + 7 controller (Promotion). +9 dedicated `SubscriptionRulesTest`.

---

## 37. Public subscription catalog — `PublicCatalogServiceImpl`

**Endpoint** : `GET /api/v1/catalog/public` (**permitAll** in `SecurityConfig` — no authentication). Status 200.

**Business use case** : the site's **pricing landing** (unauthenticated visitor) consumes this endpoint to display active plans + types + promotions. 1 server round-trip, complete aggregate returned.

**Logic** :
1. `today = LocalDate.now()`.
2. Loads in parallel (4 projected JPQL queries, no entities) :
   - `planAbonnementDomainService.findPublicResponses()` → `List<PublicPlanResponse>` (`actif=true && visible=true` sorted by `ordre, nom`)
   - `typeAbonnementDomainService.findAllActifResponses()` → `List<SubscriptionTypeResponse>` (`actif=true`)
   - `promotionDomainService.findActiveGlobalResponses(today)` → `List<PromotionResponse>` (`plan IS NULL && actif=true && dateDebut ≤ today ≤ dateFin`)
   - `promotionDomainService.findActiveScopedResponses(today)` → same but `plan IS NOT NULL`
3. Groups `scopedPromotions` by `planId` via stream.
4. Attaches each sub-list to its plan via `PublicPlanResponse.withPromotions(...)` (immutable records — returns a new record).
5. Returns `PublicCatalogResponse(plans, subscriptionTypes, globalPromotions)`.

**Why 4 projected queries instead of 1 aggregated LEFT JOIN** : the `global vs scoped` split happens in SQL (`WHERE plan IS NULL` / `IS NOT NULL`), not in Java post-load. More economical in transferred data than loading all active promotions + filtering server-side. A single Java stream for the final assembly (immutable records).

**Optimization** : `PublicPlanResponse` has 2 constructors — a canonical one (13 fields with `promotions`) and a secondary one (12 fields without, used by the JPQL projection). The `withPromotions(...)` method rebuilds the record with the injected list.

**Tests** : 3 service `PublicCatalogServiceImplTest` (happy with plans+types+promos, empty, multiple promos on 1 plan) + 2 controller `PublicCatalogControllerTest` (full catalog, empty catalog).

---

## 38. Owner subscribe — `AbonnementServiceImpl.subscribe`

**Endpoint** : `POST /api/v1/abonnements/subscribe` (permission `SUBSCRIPTION_CREATE`). Body `SubscribeRequest(planId, typeId, couponCode?, renouvellementAuto)`. Status 201.

**Business use case** : the owner subscribes (or upgrades/downgrades) a subscription. **2-step workflow** : `subscribe` only creates the Abonnement in EN_ATTENTE (with the amount breakdown for frontend info), then `POST /paiements-abonnement/abonnements/{id}` (use case 41) with a mandatory image proof activates the subscription after admin validation (use case 42).

**Logic** :
1. `currentUserService.getCurrent().entrepriseId()` → resolves the caller's company.
2. Loads `plan` via `IPlanAbonnementService.findById` then `ensurePlanSubscribable` (`actif && visible && !trial`).
3. Loads `type` via `ISubscriptionTypeService.findById` then `ensureTypeActif`.
4. Optional coupon resolution via `resolveCoupon(couponCode, planId)` :
   - `couponDomainService.findByCode(code)` → `EntityException("coupon.notFound")` if absent.
   - Validations : `actif=true`, `dateDebut ≤ today ≤ dateFin` (otherwise `coupon.expired`), `nombreUtilisationsMax == 0 || nombreUtilisations < max` (otherwise `coupon.exhausted`), `coupon.plan == null || coupon.plan.id == planId` (otherwise `coupon.notApplicable`).
5. Looks up active automatic promotion for the plan : `promotionDomainService.findFirstActivePromotionForPlan(planId, today)` (entity, small table, justified).
6. Computes the breakdown via `SubscriptionAmountCalculator.calculate(SubscriptionAmountInputs(plan, type, promotion, coupon))` : applies sequential reductions `prix×durée → type → promotion → coupon`, clamps to zero, scales BigDecimal 2 HALF_UP.
7. Creates the EN_ATTENTE Abonnement via `abonnementDomainService.createPending(entreprise, plan, type)` (without `dateDebut`/`dateFin` — set at step 7 payment).
8. Configures renouvellementAuto via `abonnementDomainService.setRenouvellementAuto(abonnement, request.renouvellementAuto())`.
9. If coupon : `reserveCoupon(coupon, entreprise, abonnement)` — delegates to `UtilisationCouponDomainService.create(coupon, entreprise, abonnement)` + `CouponDomainService.incrementUsage(coupon)`.
10. Returns `SubscribeResponse(abonnement, breakdown, couponCodeApplied, promotionNomApplied)`.

**Upgrade/downgrade strategy** : **replacement at `dateFin`** (user decision, MVP). The current subscription stays ACTIF until its `dateFin` ; the new one starts at `currentActif.dateFin+1` (actual computation at payment validation, use case 42). No proration.

**Public validations** (rule 27) : `ensurePlanSubscribable(plan)`, `ensureTypeActif(type)`, `resolveCoupon(code, planId)`, `ensureBelongsToCurrentEntreprise(abonnement)`.

**i18n** : `plan.notSubscribable`, `subscriptionType.notSubscribable`, `coupon.notFound/expired/exhausted/notApplicable`.

**Tests** : 9 service `AbonnementServiceImplTest` (happy without coupon / with reserved coupon / with promotion / inactive plan / trial plan / inactive type / coupon not found / expired / exhausted / invalid plan scope) + 6 calculator `SubscriptionAmountCalculatorTest` (base / percentage / fixed_amount / sequential / clamp zero) + 3 controller `AbonnementControllerTest`.

---

## 39. Toggle auto-renewal — `AbonnementServiceImpl.updateRenouvellementAuto`

**Endpoint** : `PATCH /api/v1/abonnements/{id}/renouvellement-auto` (permission `SUBSCRIPTION_UPDATE`). Body `RenouvellementAutoRequest(boolean)`. Status 200.

**Business use case** : the owner can **toggle the flag at any point** of the subscription's lifecycle (before/during/after activation), at their convenience.

**Logic** :
1. Loads the subscription via `abonnementDomainService.findById(id)`.
2. `ensureBelongsToCurrentEntreprise(abonnement)` → `ForbiddenException("abonnement.notOwned")` if the subscription belongs to another company.
3. Delegates set+save to the DomainService via `abonnementDomainService.setRenouvellementAuto(abonnement, request.renouvellementAuto())` (rule 26).
4. Returns `AbonnementResponse(abonnement)`.

**Note** : today payment is manual so the `renouvellementAuto=true` flag has no runtime effect yet (the automatic renewal worker is **deferred** — step 9, depends on the integration of a payment provider). The endpoint is shipped to prepare for the future.

**Tests** : 2 service (toggle + other company = Forbidden) + 1 controller.

---

## 40. Subscription listings + current company status — `AbonnementServiceImpl.findAll/findMyHistory/findMyCurrent`

**Endpoints** :
- `GET /api/v1/abonnements?entrepriseId=&statut=&planId=&page=&size=` (perm `ADMIN_ACCESS`) — ADMIN sees all subscriptions (free filter per company).
- `GET /api/v1/abonnements/me?statut=&planId=&page=&size=` (perm `SUBSCRIPTION_READ`) — PROPRIETAIRE sees their history (auto-scoped to their company).
- `GET /api/v1/abonnements/me/current` (perm `SUBSCRIPTION_READ`) — PROPRIETAIRE sees the current active subscription + remaining days + trial flag + plan features.

**Business use case** : visualization and reporting. ADMIN supervises all SaaS accounts, owner consults their current subscription to adapt the UI to the plan's limits (`nombreMagasinsMax`, `nombreEmployesMax`, `gestionStock/Vente/Achat/Comptabilite`).

**Logic** :
- `findAll(filter)` : `validatorService.validate(filter)` + `abonnementDomainService.findResponses(filter)` (JPQL projection `SELECT new AbonnementResponse(abonnement) ... LEFT JOIN FETCH plan/type/entreprise` + separate `countQuery` for pagination).
- `findMyHistory(filter)` : forces `filter.entrepriseId = currentUser.entrepriseId()` (record rebuilt with scope) before delegation.
- `findMyCurrent()` : `abonnementDomainService.findCurrentActif(entrepriseId)` (entity — needed for the `CurrentAbonnementResponse` secondary constructor). Computes `joursRestants = max(0, ChronoUnit.DAYS.between(today, dateFin))`. Returns `CurrentAbonnementResponse(abonnement, joursRestants, isTrial, PlanFeaturesResponse(...))`.

**Repository optimization** (R7a) : `AbonnementRepository.findResponsesByFilter` uses a JPQL projection with `LEFT JOIN FETCH` on the `@ManyToOne` relations (plan, type, entreprise) — no N+1 lazy load + `Pageable+collection` warning avoided since these are ManyToOne.

**i18n** : `abonnement.noActive` if no active subscription (404).

**Tests** : 4 service + 3 controller.

---

## 41. Manual payment (PROPRIETAIRE) — `PaiementAbonnementServiceImpl.create`

**Endpoint** : `POST /api/v1/paiements-abonnement/abonnements/{abonnementId}` (perm `SUBSCRIPTION_PAY`). Multipart : `data` (JSON `PaiementAbonnementRequest(moyen, referenceTransaction, datePaiement)`) + `file` (image, **mandatory**). Status 201.

**Business use case** : **no automatic payment integrator** in the project. The owner pays out-of-app (Wave/Orange Money/wire transfer/cash) then records the transaction in the app with a **mandatory image proof** (screenshot/photo of the receipt). The admin then validates (use case 42).

**Logic** :
1. Loads subscription + `ensureAbonnementBelongsToCurrentEntreprise` → `ForbiddenException` if another company.
2. `ensureAbonnementIsPending(abonnement)` : status must be EN_ATTENTE (otherwise `abonnement.notPending`).
3. `ensureNoPendingPayment(abonnementId)` : via `paiementAbonnementDomainService.existsPendingForAbonnement` (boolean projection, R7b) — otherwise `paiementAbonnement.alreadyPending`.
4. **Recomputes** the breakdown via `recomputeBreakdown(abonnement)` :
   - Active promotion at `today` (may differ from subscribe time).
   - Coupon : `utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId)` (UUID projection, R7c) → `couponDomainService.findById(couponId)`.
   - `amountCalculator.calculate(SubscriptionAmountInputs(plan, type, promotion, coupon))`.
5. Builds the proof `PieceJointe` via `IUploadFileService.buildImage(file)` (MIME validation + blob + contentType).
6. Creates `PaiementAbonnement` in EN_ATTENTE_VALIDATION via `paiementAbonnementDomainService.createPending(new PaiementAbonnementCreationContext(abonnement, request, breakdown, preuveImage))` (record context, R3 — rule 30 max 3 params).
7. Returns `PaiementAbonnementResponse(paiement)` (without proof bytes, just `preuveId` — download via dedicated endpoint).

**Server-side recomputed amount** : the owner does not enter an amount. The system recomputes at payment time (plan × duration − reductions). If the admin deems the paid amount incorrect, they reject (use case 42).

**Migration** : V21 enriches `paiement_abonnement` with `statut` (NOT NULL default EN_ATTENTE_VALIDATION), `preuve_id` (FK piece_jointe), `motif_rejet` (TEXT).

**i18n** : `abonnement.notPending`, `paiementAbonnement.alreadyPending`, `upload.file.empty`, `upload.file.invalidImageType`.

**Tests** : 4 service (happy create + other company + non-pending subscription + payment already pending).

---

## 42. Payment validation / rejection (ADMIN) — `PaiementAbonnementServiceImpl.validate/reject`

**Endpoints** :
- `PATCH /api/v1/paiements-abonnement/{id}/validate` (perm `SUBSCRIPTION_VALIDATE`). Empty body. Status 200.
- `PATCH /api/v1/paiements-abonnement/{id}/reject` (perm `SUBSCRIPTION_VALIDATE`). Body `RejectPaiementRequest(motifRejet)`. Status 200.

**Business use case** : the SaaS admin examines the image proof provided by the owner and :
- **Validates** → the subscription moves to ACTIF with `dateDebut`/`dateFin` computed per the **replacement at `dateFin`** strategy.
- **Rejects** with reason → the subscription stays EN_ATTENTE, the reserved coupon is **released** (rollback).

**`validate` logic** :
1. Loads payment + `ensurePaiementIsPendingValidation` (otherwise `paiementAbonnement.notPendingValidation`).
2. `activateAbonnement(abonnement)` :
   - `abonnementDomainService.findLatestActifDateFin(entrepriseId, abonnement.getId())` (JPQL `MAX(dateFin)`, R7a) → if present : `dateDebut = max+1`, otherwise `dateDebut = today`.
   - `dateFin = dateDebut + typeAbonnement.dureeMois`.
   - `abonnementDomainService.activate(abonnement, dateDebut, dateFin)` (rule 26 — setter+save in domain).
3. Marks payment VALIDE via `paiementAbonnementDomainService.markAsValide(paiement)`.

**`reject` logic** :
1. Loads payment + `ensurePaiementIsPendingValidation`.
2. `releaseReservedCouponIfAny(abonnementId)` :
   - `utilisationCouponDomainService.findCouponIdByAbonnementId` → if present : `couponDomainService.findById(couponId)` + `couponDomainService.decrementUsage(coupon)` + `utilisationCouponDomainService.deleteByAbonnementId(abonnementId)` (bulk delete R7c).
3. Marks payment REJETE with reason via `paiementAbonnementDomainService.markAsRejete(paiement, motifRejet)`.

**Listing/read scoping** :
- `findAll(filter)` : auto-scoped per company for non-ADMIN (`scopeFilterForNonAdmin`).
- `findResponseById` + `getPreuve` : ADMIN everything, otherwise the caller's company (`ensurePaiementAccessibleByCaller`).

**i18n** : `paiementAbonnement.notPendingValidation`, `paiementAbonnement.preuve.notFound`.

**Tests** : 4 validate/reject (activate dates today / dates currentActif+1 / already validated / release coupon / without coupon / reject without reason 400) + 5 controller (list / get / get preuve / validate / reject).

---

## 47. Sale cancellation (sale, critical multi-module workflow) — `VenteServiceImpl.cancel`

**Endpoint** : `POST /api/v1/ventes/{commandeId}/annuler` (permission `SALE_CANCEL` — ADMIN/PROPRIETAIRE/MANAGER, **not VENDEUR**).

**Business use case** : a DELIVERED sale is cancelled (entry mistake, client refusal, defective item). The FIFO stock consumed must be re-injected to make the items sellable again ; the client invoice is invalidated ; existing payments are kept for audit (out-of-app refund). Cancellation is allowed only within a configurable time window (default 24 h after creation).

**Input** : `AnnulationVenteRequest{ motif (enum `MotifAnnulationVente` : ERREUR_SAISIE, REFUS_CLIENT, ARTICLE_DEFECTUEUX, AUTRE) + optional commentaire ≤ 1000 chars }`.

**Atomic flow** (1 `@Transactional` transaction) :
1. `validatorService.validate(request)`.
2. Loads order + `ensureBelongsToCurrentEntreprise` (`commandeVente.notOwned` 403).
3. `ensureCancellable(commande)` :
   - If status = `ANNULEE` → `BadArgumentException("commandeVente.cancel.alreadyCancelled")`.
   - If status ≠ `DELIVERED` → `BadArgumentException("commandeVente.cancel.notDelivered", statut)`.
4. `ensureWithinCancelWindow(commande)` :
   - `commande.createdAt + saleProperties.cancelWindowHours` must be ≥ `now()`, otherwise `BadArgumentException("commandeVente.cancel.windowExpired", maxHours)`.
5. For each `LigneCommandeVente` :
   - Loads the `SortieStock(annulee=false)` via `sortieStockDomainService.findActiveByLigneVenteId(ligneId)`.
   - Loads the aggregate `Stock` of the (store, product) once per line.
   - For each exit : `entreeStockDomainService.creditQuantiteRestante(lot, qty)` + `sortieStockDomainService.markAsAnnulee(sortie)` + `stockDomainService.creditQuantite(stock, qty)` + `mouvementStockDomainService.journalize(stock, MouvementJournalize(RETOUR_CLIENT, qty, stockAvant, stockApres, null, null))`.
6. Switches order → `ANNULEE` + fills `motifAnnulation`, `commentaireAnnulation`, `dateAnnulation` (method `commandeVenteDomainService.cancel`, rule 26).
7. Switches invoice → `ANNULEE` if present (method `factureClientDomainService.cancel`, rule 26). `PaiementVente` payments kept as is.

**Output** : `AnnulationVenteResponse{ commandeId, reference, statut=ANNULEE, motif, commentaire, dateAnnulation (yyyy-MM-dd HH:mm:ss), totalQuantiteReinjectee, nombreMouvementsCrees }`. HTTP 200.

**Audit preservation** :
- `SortieStock.annulee = true` lets `MarginReportRepository.computeMargin` exclude the margins of cancelled sales via `WHERE sortie.annulee = false`.
- 6 Caisse queries adapted (sumQuantite / countCommandes / ventilationVendeur / sumMontantTotal / sumMontantPaiement / topProduits) : `WHERE commande.statut <> ANNULEE`. Cash drawer semantics : no "phantom" on invalidated sales.
- Initial `MouvementStock(SORTIE_VENTE)` movements are not deleted — each compensatory RETOUR_CLIENT is journaled alongside, history stays chronologically readable.

**Configuration** : `sale.cancel-window-hours: ${SALE_CANCEL_WINDOW_HOURS:24}` (record `SaleProperties` in `org.store.property`, rule 38).

**DB migration** : V22 — `commande_vente +motif_annulation VARCHAR(30) / +commentaire_annulation TEXT / +date_annulation TIMESTAMP`, `sortie_stock +annulee BOOLEAN NOT NULL DEFAULT FALSE` + index.

**Public validations** (rule 27) : `ensureBelongsToCurrentEntreprise`, `ensureCancellable`, `ensureWithinCancelWindow`, `reinjectStockForLigne`, `reinjectOneSortie`. All accessible individually, testable in isolation.

**i18n** : 3 FR/EN keys — `commandeVente.cancel.alreadyCancelled`, `commandeVente.cancel.windowExpired` ({0}=max hours), `commandeVente.cancel.notDelivered` ({0}=current status).

**Tests** : 5 service (nominal re-credit + journalize RETOUR_CLIENT + status ANNULEE / already cancelled 400 / not DELIVERED 400 / window exceeded 400 / cross-company 403) + 3 controller (200 OK / 400 invalid reason / 400 blank reason). **711 / 711 green** (+8 vs 703).

---

## 48. Sale refactor — 2-step workflow : DRAFT → VALIDATE — `VenteServiceImpl`

**Refactor 2026-05-18** : symmetric with the purchase refactor (section 28). Atomic creation was split to allow **visualization and editing** of a sale before cash-in (entry mistake correction, quantity or price adjustment by the seller, choice of another PF variant).

### 48.a DRAFT creation — `POST /api/v1/ventes`

**Permission** : `SALE_CREATE` (VENDEUR/MANAGER/PROPRIETAIRE/ADMIN).

**Input** : `VenteRequest{ clientId?, lignes[] }` — **no more `dateEcheance` or `premierPaiement`** (entered at validation).

**Flow** :
1. Validations : seller EMPLOYE mandatory (`employeService.findCurrentUser` throws 403 otherwise), client resolved if provided (double scoping), for each line : PF scoped company + `prixUnitaire ≥ pf.prixVente` (floor).
2. Creates `CommandeVente` in `DRAFT` status (auto reference `VTE-yyyyMMdd-HHmmssSSS`, `dateVente = today`).
3. Persists each `LigneCommandeVente` (snapshot prixUnitaire + computed montantTotal).
4. **No stock consumption, no invoice, no payment.**

**Output** : `VenteDraftResponse{ commande }` — HTTP 201.

### 48.b Line edit / delete — `PUT/DELETE /api/v1/ventes/orders/{commandeId}/lignes/{ligneId}`

**Permissions** : `SALE_UPDATE` (PUT) / `SALE_DELETE` (DELETE).

**Guard** : `ensureCommandeIsDraft` + `ensureLigneBelongsToCommande` (anti URL forging) + `ensureNotLastLigne` (DELETE refused on the last line).

**PUT body** : `LigneVenteUpdateRequest{ quantite, prixUnitaire }`. Re-validation `prixUnitaire ≥ pf.prixVente` (the line's PF stays immutable — to change variant : delete + recreate). Returns the updated `LigneCommandeVenteResponse`.

**DELETE** : 204. Refusal if last line (`commandeVente.cannotDeleteLastLigne`).

### 48.c Validation — `POST /api/v1/ventes/{commandeId}/validate`

**Permission** : `SALE_APPROVE` (new, created 2026-05-18 ; granted to VENDEUR+MANAGER+PROPRIETAIRE+ADMIN).

**Input** : `VenteValidateRequest{ dateEcheance (@FutureOrPresent), premierPaiement? }`.

**Atomic flow (single transaction)** :
1. Validations : `ensureBelongsToCurrentEntreprise` + `ensureCommandeIsDraft`.
2. For each line : `sortieStockService.consumeForVente(...)` — consumes the PF's FIFO lots, creates 1 SortieStock per consumed lot (linked to the line via FK), decrements aggregate Stock, journals `MouvementStock(SORTIE_VENTE)`. If insufficient stock → `BadArgumentException("stock.exit.insufficientQuantity")` (the user must adjust the line or wait for resupply).
3. Recomputes `montantTotal = SUM(lignes.montantTotal)` from the current lines (may have been edited).
4. Creates `FactureClient` (auto number `FAC-VTE-yyyyMMdd-HHmmssSSS`, status NON_PAYEE, entered `dateEcheance`).
5. If `premierPaiement` present : creates `PaiementVente` + `factureClientDomainService.applyPaiement` (recomputes PAYEE/PARTIELLEMENT_PAYEE status).
6. Switches `commande.statut → DELIVERED` via `commandeVenteDomainService.validate` (rule 26).

**Output** : `VenteResponse{ commande (DELIVERED), facture }` — HTTP 200.

### 48.d Detail — `GET /api/v1/ventes/{commandeId}`

Adapted to handle DRAFT : `facture` can be null if the order has not been validated yet. Payments are loaded only if the invoice exists.

### Caisse queries adapted : `= DELIVERED` (instead of `<> ANNULEE`)

6 reporting queries now only count effectively DELIVERED sales — excludes DRAFT (unfinished drafts) and ANNULEE (already excluded before). Strict cash drawer semantics.

Repos involved : `CommandeVenteRepository.countByMagasinAndDay / sumQuantiteLignes / ventilationParVendeur`, `FactureClientRepository.sumMontantTotalByMagasinAndDay`, `PaiementVenteRepository.sumMontantByMagasinAndDay / ventilationParMoyen`, `LigneCommandeVenteRepository.findTopProduitsByMagasinAndDay`.

### Compatibility with cancellation (section 47)

`cancel` (shipped morning 2026-05-18) keeps accepting only `DELIVERED` (status → ANNULEE + FIFO re-injection). A DRAFT is not formally "cancellable" — to abandon a draft, delete the lines one by one (leave the last) or let the DRAFT die without auto cleanup.

### DB migration

**None** — no new field to persist (lot traceability does not apply to sale, only to purchase where it required V23).

### Permissions

- `SALE_APPROVE` created (consistency with `PURCHASE_APPROVE`). Granted to the 4 roles (VENDEUR included) : the seller who creates the sale validates it themselves at cash-in time.
- No other change (all `SALE_*` already in place).

### Public validations (rule 27)

- `ensureCommandeIsDraft(commande)` (throws if status ≠ DRAFT).
- `ensureLigneBelongsToCommande(ligne, commande)` (anti URL forging).
- `ensureNotLastLigne(commande)` (delete refused if 1 single line).
- `ensurePrixUnitaireAboveFloor(prix, pf)` (factored — shared between `create` and `updateLigne`).

### i18n

4 new FR/EN keys : `commandeVente.notDraft` ({0}=status), `commandeVente.cannotDeleteLastLigne`, `ligneCommandeVente.notFound`, `ligneCommandeVente.notMatchingCommande`.

### Cleanup

`VenteContext` record removed (was no longer used after refactor of internal methods to `consumeStockForLigne` which no longer carries the old batch state).

### Tests

`VenteServiceImplTest` rewritten (23 tests) : 4 create DRAFT (nominal without stock/invoice, dateVente=today, seller EMPLOYE mandatory, floor price) + 5 validate (materialization, first payment, datePaiement, not draft, not owned) + 3 updateLigne (OK, price below floor, not draft) + 3 deleteLigne (OK, last ligne 400, not draft) + 3 findDetails (DELIVERED, DRAFT facture null, not owned) + 5 cancel (preserved). `VenteControllerTest` (11 tests) : 201 DRAFT, 400 empty lines, 200 validate, 400 validate dateEcheance missing, 200 get details, 200 PUT, 400 PUT quantite 0, 204 DELETE, 200 cancel, 400 cancel invalid reason, 400 cancel blank reason. **739 / 739 green** (+14 vs 725).

---

## 49. Purchase cancellation (purchase, critical multi-module workflow) — `AchatServiceImpl.cancel`

> **Note (refactor 50)** : since the introduction of partial reception, cancellation is allowed on **VALIDEE**, **PARTIELLEMENT_RECEPTIONNEE** and **RECEPTIONNEE** (not only RECEPTIONNEE as originally). If VALIDEE : no lot to remove (stock loop no-op). If PARTIELLEMENT_RECEPTIONNEE : only the lots already created are removed, same safeguards. The i18n message moved from `commandeAchat.cancel.notReceptionnee` to `commandeAchat.cancel.notCancellable`.

**Endpoint** : `POST /api/v1/achats/{commandeId}/annuler` (permission `PURCHASE_CANCEL` — ADMIN/PROPRIETAIRE/MANAGER, **not VENDEUR**).

**Business use case** : a purchase order is cancelled (entry mistake, supplier refusal, defective item delivered). The stock fed by this purchase must be removed ; the supplier invoice is invalidated ; existing payments are kept for audit (out-of-app refund). Cancellation is only allowed within a configurable time window (default 24 h after creation) **and only if no lot has already been partially or totally consumed by a sale** — otherwise the symmetric stock + invoice cancellation is no longer possible and you have to go through another flow (partial supplier return, out-of-scope).

**Input** : `AnnulationAchatRequest{ motif (enum `MotifAnnulationAchat` : ERREUR_SAISIE, REFUS_FOURNISSEUR, ARTICLE_DEFECTUEUX, AUTRE) + optional commentaire ≤ 1000 chars }`.

**Atomic flow** (1 `@Transactional` transaction) :
1. `validatorService.validate(request)`.
2. Loads order + `ensureBelongsToCurrentEntreprise` (`commandeAchat.notOwned` 403).
3. `ensureCancellable(commande)` :
   - If status = `ANNULEE` → `BadArgumentException("commandeAchat.cancel.alreadyCancelled")`.
   - If status ≠ `RECEPTIONNEE` → `BadArgumentException("commandeAchat.cancel.notReceptionnee", statut)`.
4. `ensureWithinCancelWindow(commande)` :
   - `commande.createdAt + purchaseProperties.cancelWindowHours` must be ≥ `now()`, otherwise `BadArgumentException("commandeAchat.cancel.windowExpired", maxHours)`.
5. Loads all `EntreeStock` from this order via `entreeStockDomainService.findByCommandeAchatId(commandeId)`.
6. `ensureNoLotConsumed(lots)` : if at least one lot has `quantiteRestante < quantiteInitiale` → `BadArgumentException("commandeAchat.cancel.lotAlreadyConsumed")`. All-or-nothing.
7. For each lot : `stockDomainService.findByMagasinIdAndProduitId(...)` → `stockDomainService.decrement(stock, lot.quantiteRestante)` + `entreeStockDomainService.markAsAnnulee(lot)` + `mouvementStockDomainService.journalize(stock, MouvementJournalize(RETOUR_FOURNISSEUR, qty, stockAvant, stockApres, commande.reference, null))`.
8. Switches order → `ANNULEE` + fills `motifAnnulation`, `commentaireAnnulation`, `dateAnnulation` (method `commandeAchatDomainService.cancel`, rule 26).
9. Switches invoice → `ANNULEE` if present (method `factureAchatDomainService.cancel`, rule 26). `PaiementAchat` payments kept as is.

**Output** : `AnnulationAchatResponse{ commandeId, reference, statut=ANNULEE, motif, commentaire, dateAnnulation (yyyy-MM-dd HH:mm:ss), totalQuantiteRetiree, nombreMouvementsCrees }`. HTTP 200.

**Audit preservation** :
- `EntreeStock.annulee = true` lets reporting queries exclude returned lots via `WHERE entree.annulee = false`. `entree.quantiteRestante` is intentionally preserved (historical snapshot at cancellation time), so the `quantiteRestante > 0` filter alone is not enough.
- 5 queries adapted : `EntreeStockRepository.findAvailableLotsForFifo`, `.findAvailableLotsForFifoByProductFournisseur`, `.findExpiringLots`, `.findActiveLotsByMagasinAndProductIds`, and `ProductRepository.searchByEntrepriseWithActiveLots`. FIFO consumption on the sale side, expiring lots, and product search no longer include returned lots.
- `Stock` aggregate (`computeValuation`, `findResponsesByFilter`, `findResponsesBelowThreshold`) already decremented by `cancel` → no query adjustment.
- `MarginReportRepository.computeMargin` joins on `SortieStock` which already filters `annulee=false` → no direct impact (purchase cancellation only affects margins if a sale had already consumed the lot, which is precisely forbidden).
- Initial `MouvementStock(ENTREE_ACHAT)` are not deleted — each compensatory RETOUR_FOURNISSEUR is journaled alongside, with the source order's reference in `referenceDocument`.

**Configuration** : `purchase.cancel-window-hours: ${PURCHASE_CANCEL_WINDOW_HOURS:24}` (record `PurchaseProperties` in `org.store.property`, rule 38).

**DB migration** : V24 — `commande_achat +motif_annulation VARCHAR(30) / +commentaire_annulation TEXT / +date_annulation TIMESTAMP`, `entree_stock +annulee BOOLEAN NOT NULL DEFAULT FALSE` + index.

**Public validations** (rule 27) : `ensureCancellable`, `ensureWithinCancelWindow`, `ensureNoLotConsumed`, `withdrawStockForLot`. All accessible individually, testable in isolation.

**i18n** : 4 FR/EN keys — `commandeAchat.cancel.alreadyCancelled`, `commandeAchat.cancel.notReceptionnee` ({0}=current status), `commandeAchat.cancel.windowExpired` ({0}=max hours), `commandeAchat.cancel.lotAlreadyConsumed`.

**Tests** : 6 service (nominal removal + journalize RETOUR_FOURNISSEUR + status ANNULEE / already cancelled 400 / not RECEPTIONNEE 400 / window exceeded 400 / cross-company 403 / lot already consumed 400) + 3 controller (200 OK / 400 invalid reason / 400 blank reason). **750 / 750 green** (+9 vs 741).

---

## 50. Partial purchase reception — `AchatServiceImpl.validate` (refactor) + `AchatServiceImpl.receive`

**Endpoints** :
- `POST /api/v1/achats/{commandeId}/validate` (permission `PURCHASE_APPROVE`) — refactored
- `POST /api/v1/achats/{commandeId}/receptions` (permission `PURCHASE_APPROVE`) — new

**Business use case** : a supplier delivers an order in several shipments (shortage, split transport, lots packaged differently, etc.). The accounting validation (invoice creation with frozen amount) must be able to precede the physical reception, and several partial deliveries must be able to feed the stock progressively until completion.

**Contract change — `validate` (DRAFT → VALIDEE, no longer RECEPTIONNEE)** :
- Before : `validate` materialized everything in one transaction (invoice + stock entries + journal + `pf.prixVente` update + switch to RECEPTIONNEE).
- After : `validate` only creates the invoice, computes `montantTotal` from the current lines, and switches to `VALIDEE`. **No `EntreeStock`, no `MouvementStock`, no `pf.prixVente` update at this step.** Validation becomes purely accounting.

**Atomic `receive` flow** (1 `@Transactional` transaction) :
1. `validatorService.validate(receptionAchatRequest)` (`@NotEmpty @Valid lignes`).
2. Loads order + `ensureBelongsToCurrentEntreprise` (`commandeAchat.notOwned` 403).
3. `ensureReceivable(commande)` : refuses if status ≠ `VALIDEE` and ≠ `PARTIELLEMENT_RECEPTIONNEE` (`commandeAchat.receive.notValidee`).
4. `ensureLignesDistinctes(lignes)` : refuses if the same `ligneId` appears multiple times in the request (`commandeAchat.receive.duplicateLine`).
5. Loads the possible invoice for the reference in the movement (`factureAchatDomainService.findByCommandeId`).
6. For each `LigneReceptionRequest` → `receiveOneLine(commande, facture, ligneReception)` :
   - `ensureLigneBelongsToCommande` (`ligneCommandeAchat.notMatchingCommande` 400).
   - `ensureQuantiteRecueNotExceeded(ligne, quantite)` : refuses if `quantite > ligne.quantite - ligne.quantiteRecue` (`commandeAchat.receive.lineQuantityExceeded`).
   - Computes `numeroLot` (request if provided, otherwise line snapshot) and `dateExpiration` (same).
   - `entreeStockDomainService.create(EntreeStockCreate(magasin, produit, pf, quantite, prixAchatLigne, numeroLot, dateExpiration, commande))` — creates 1 lot per received line (distinct lots possible for 1 same line over several receptions).
   - `stockDomainService.createOrUpdateEntry(magasin, produit, quantite, prixAchatLigne)` — upserts aggregate Stock (weighted average purchase price recomputed).
   - `mouvementStockDomainService.journalize(stock, MouvementJournalize(ENTREE_ACHAT, quantite, stockAvant, stockApres, facture?.numero, null))`.
   - `productFournisseurService.applyPrixVenteFromPurchase(pf, ligne.prixVente)` — re-applies the snapshot sale price at each reception (mirror of `validate`'s old behavior).
   - `ligneCommandeAchatDomainService.incrementQuantiteRecue(ligne, quantite)` — cumulative sum.
7. Switches status : if `Σ ligne.quantiteRecue == Σ ligne.quantite` over all lines → `markReceptionnee`, otherwise → `markPartiallyReceived`.

**Input** : `ReceptionAchatRequest{ lignes: List<LigneReceptionRequest{ ligneId, quantite (@Positive), numeroLot? (@Size max=100), dateExpiration? }> } (@NotEmpty @Valid)`.

**Output** : `ReceptionAchatResponse{ commandeId, reference, statut (PARTIELLEMENT_RECEPTIONNEE or RECEPTIONNEE), totalQuantiteRecueDansCetteReception, totalQuantiteRecueGlobale, totalQuantiteCommandee }`. HTTP 200.

**Several lots for the same line** : each reception creates a separate `EntreeStock`. If the 1st delivery brings 60 units on a line of 100, and the 2nd the remaining 40, there will be 2 distinct lots in stock (with potentially different lot numbers/expiration dates). This is the desired FIFO traceability.

**Notable decisions** :
- **`pf.prixVente` re-applied at every reception** : symmetric with old `validate`. If several receptions on the same PF, the last one wins. For a supplier price change mid-reception, this is consistent.
- **`prixAchat` line snapshot** : each created `EntreeStock` uses `ligne.prixAchat` (order snapshot), not entered at reception. The purchase price is locked from the order.
- **`numeroLot` / `dateExpiration` overridable at reception** : the supplier may deliver a different lot than ordered. The `LigneReceptionRequest` accepts these 2 fields, fallback on the order values if absent.
- **`ensureLignesDistinctes`** : the same `ligneId` cannot appear twice in **a single** `ReceptionAchatRequest` (anti accidental double-count). However it can be present in several successive `receive`s (this is the very point of the feature).
- **Previously unused VALIDEE status** : finally gets its business meaning (invoice created, amount frozen, stock not received yet).

**Cancellation impact** : `cancel` now accepts 3 statuses (VALIDEE, PARTIELLEMENT_RECEPTIONNEE, RECEPTIONNEE). See section 49 (head note).

**DB migration** : V25 — `ligne_commande_achat +quantite_recue INTEGER NOT NULL DEFAULT 0`.

**Public validations** (rule 27) : `ensureReceivable`, `ensureLignesDistinctes`, `ensureQuantiteRecueNotExceeded`, `receiveOneLine`, plus reuse of `ensureLigneBelongsToCommande`.

**i18n** : 4 FR/EN keys — `commandeAchat.receive.notValidee` ({0}=status), `commandeAchat.receive.lineQuantityExceeded` ({0}=requested qty, {1}=remaining qty), `commandeAchat.receive.duplicateLine`, `commandeAchat.receive.ligneNotInCommande`.

**Tests** : 10 service receive (complete → RECEPTIONNEE / partial → PARTIELLEMENT_RECEPTIONNEE / not VALIDEE / already RECEPTIONNEE / duplicate / qty exceeded / line from another order / cross-company 403 / lot override request / 2nd reception completing) + 2 additional service cancel (VALIDEE = no stock removal, PARTIELLEMENT_RECEPTIONNEE = partial removal) + 3 controller (200 / 400 empty lines / 400 quantite 0) + 2 adapted validate tests (no more verified stock materialization). **765 / 765 green** (+15 vs 750).

---

## 51. RBAC tightening + role rename FR→EN + Magasin permission split (2026-05-20)

**Goal** : harden the per-method authorization, align role libellés with the English code base, and split Magasin read into list-wide vs single-resource.

**Role rename** : PROPRIETAIRE → OWNER, VENDEUR → SELLER. The matching access permission code follows suite : PROPRIETAIRE_ACCESS → OWNER_ACCESS. Applied case-sensitive across all Java string literals, JPQL, YAML role/permission seeds, FR/EN properties files, tests, comments. The mixed-case `Proprietaire` entity (User profile sub-type) intentionally untouched — domain noun separate from the role identifier.

**V3 Flyway migration** : renames the existing DB rows in-place via `UPDATE role SET libelle = 'OWNER' WHERE libelle = 'PROPRIETAIRE'` (idem SELLER + permission code). Required because the RBAC sync is additive only — creating new role rows would have left orphans.

**STORE_READ_ONE split** : new permission code declared in the YAML and assigned to ADMIN / OWNER / MANAGER / SELLER. `STORE_READ` stays "list every magasin in your entreprise" (OWNER / ADMIN only). `STORE_READ_ONE` = "read the magasin you're allowed to access" (all employees, used by the profile Affectation card to resolve `magasinId` → name without needing list-level access). `MagasinController` per-method `@PreAuthorize` updated. `MagasinServiceImpl.findResponseById` + `getLogo` switched from `ensureBelongsToCurrentEntreprise` to `ensureAccessibleByCurrentUser` — employees can only read their own magasin id, not a foreign one in the same entreprise.

**AccessDenied → 403** : new `@ExceptionHandler(org.springframework.security.access.AccessDeniedException)` in `GlobalException` returning HTTP 403 + i18n key `access.denied` (FR/EN). Without it, Spring 6.1+ method-level `@PreAuthorize` denials (thrown as `AuthorizationDeniedException` from the AOP advice, outside the security filter chain) fell through to the catch-all `Exception` handler → 500 "error.unexpected". The dedicated filter-chain `CustomAccessDeniedHandler` only catches things thrown DURING the filter chain, not by AOP from inside the controller.

**Dev seed flag default flipped** : `application-dev.yml` `RBAC_SYNC` default switched from `false` → `true`. New permissions / roles in the YAML now land in DB on each dev boot without needing an env-var dance. Prod (`application-prod.yml`) still opt-in explicit.

**Tests** : full suite **774 / 774 green** through the rename. Existing role-name string literals in tests were swept by the same sed pass.

---

## 52. Data-layer hardening : ADMIN seed + partial unique on person + lower(bytea) workaround (2026-05-20)

**Goal** : three operational fixes that don't introduce new endpoints but keep the existing ones reliable.

**ADMIN seed** : `DataInitializer.ensureAdminAccount()` creates the SaaS super-admin (`admin` / `passer123` bcrypt) when `security.rbac.sync=true`. Idempotent : skips if a user `admin` already exists. The ADMIN Role must already be in DB (the sync runs just before in the same `run(args)` call). In prod the sync flag stays off by default so the known-credential admin is **not** seeded — bootstrapping the super-admin in prod is left as a follow-up (`@ConfigurationProperties` exposure).

**Person partial unique** (V4 migration) : `Person.email` and `Person.telephone` carried `@Column(unique = true)` which generated strict `person_email_key` / `person_telephone_key` UNIQUE constraints. PostgreSQL accepts multiple NULLs under UNIQUE but rejects multiple `""` — so saving two suppliers / clients / employees without a phone or email collided on the 2nd insert. V4 drops both constraints and rebuilds them as partial unique indexes : `CREATE UNIQUE INDEX person_email_unique ON person(email) WHERE email IS NOT NULL AND email <> ''` (idem telephone). Existing blank values are normalized to NULL beforehand. The `@Column(unique = true)` annotation stays as metadata-only documentation — Flyway is the authoritative DDL source. Frontend also strips empty optional strings to `undefined` before POST/PUT (`blankOptionalsToUndefined` in `clientApi` / `fournisseurApi`) as defense in depth.

**lower(bytea) workaround** (Client + Product search) : `ClientRepository.findResponsesBy{Magasin,Entreprise}Id` and `ProductRepository.searchByEntrepriseWithActiveLots` used bare `:nom` / `:prenom` / `:searchTerm` parameters in two contexts (`:term IS NULL` ambiguous + `LIKE LOWER(CONCAT('%', :term, '%'))`). Hibernate 7 on PostgreSQL infers parameter type from the union of usage contexts ; this specific shape ended up binding the parameter as `bytea`, and PostgreSQL refused `lower(bytea)` because the function only exists for text. Fix : new `org.store.common.tools.LikePatternHelper.toLikePattern(String)` builds the `%term%` lowercase pattern in Java (returns null for blank input). The repositories now bind a single pre-formed String (`LIKE :pattern`) — single-context bind locks the JDBC type to varchar. `ClientDomainService` and `ProductDomainService` call the helper before forwarding to the repo. Behavior preserved : null pattern = filter disabled, non-null = case-insensitive partial match.

**Listing ordering** (bonus) : added `ORDER BY createdAt DESC` on `ClientRepository` (both list queries) and `FournisseurRepository`. Without an explicit ORDER BY, PostgreSQL returned rows in undefined order — newly-created records landed on page 2 / 3 by accident and users believed the save had failed.

**Tests** : full backend suite **774 / 774 green**.

---

## 53. Contact module — `ContactMessageServiceImpl`

**Endpoints** :
- `POST /api/v1/contact` (permitAll) — public form submission
- `GET /api/v1/contact` (`CONTACT_READ`) — paginated + filtered admin listing
- `GET /api/v1/contact/{id}` (`CONTACT_READ`) — detail, auto-marks as LU on first access
- `PATCH /api/v1/contact/{id}/reply` (`CONTACT_RESPOND`) — save reply, mark REPONDU, send email

**Flow** :
1. `submit()` : saves `ContactMessage`, publishes `ContactMessageReceivedEvent` → ADMIN notified.
2. `findAll(ContactMessageFilter)` : paginated JPQL with nom/email/statut LIKE + sentinel date range.
3. `findById()` : auto-transitions `NOUVEAU → LU` on first admin access.
4. `reply()` : guard `contact.alreadyReplied` (throws `BadArgumentException` if statut == REPONDU). Sets `reponse` + `REPONDU`. Publishes `ContactMessageRepliedEvent` → `EmailEventListener` sends reply email to sender.

**Rules** : one reply only (idempotency guard). Email sent async via `IAuditEventPublisher` pattern.

**Filter** : `ContactMessageFilter(nom, email, statut, createdStartDate, createdEndDate, page=0, size=10)` with sentinel dates.

---

## 54. Email service — `EmailServiceImpl` / `NoOpEmailServiceImpl`

**Trigger** : `ContactMessageRepliedEvent` published on reply.

**Flow** : `@Async @EventListener EmailEventListener.onContactMessageReplied()` → `IEmailService.sendContactReply(event)`.

**Implementation** :
- `EmailServiceImpl` — `@ConditionalOnProperty(spring.mail.host)` — uses `JavaMailSender` + `IMessageSourceService` for subject/body i18n. `MailException` caught and logged, never re-thrown.
- `NoOpEmailServiceImpl` — `@ConditionalOnMissingBean` fallback — logs a warning when SMTP not configured.

**Config** : `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` env vars (SMTP port 587, STARTTLS).

---

## 55. ApplicationEvent notification system — `NotificationEventListener`

**Events wired** :

| Event | Trigger service | Recipients |
|-------|----------------|------------|
| `VenteValidatedEvent` | `VenteServiceImpl.validate()` | MANAGERs of the magasin |
| `StockBelowThresholdEvent` | `AjustementStockServiceImpl` | MANAGERs of the magasin |
| `PaiementAbonnementSubmittedEvent` | `PaiementAbonnementServiceImpl.create()` | All ADMINs |
| `PaiementAbonnementValidatedEvent` | `PaiementAbonnementServiceImpl.validate()` | OWNER of the entreprise |
| `PaiementAbonnementRejectedEvent` | `PaiementAbonnementServiceImpl.reject()` | OWNER of the entreprise |
| `ContactMessageReceivedEvent` | `ContactMessageServiceImpl.submit()` | All ADMINs |

**Flow** : each handler is `@Async @EventListener` — persists `Notification(IN_APP)` via `NotificationDomainService`. All titles/bodies resolved via `IMessageSourceService` (zero hardcoded strings). `contactMessage FK` stored on the notification for inline-reply from the notifications page.

**Notification API** : `GET /api/v1/notifications?statut=&page=&size=` + `GET /count-unread` + `PATCH /{id}/lue` + `PATCH /lue-tout`. Auto-scoped to current user. History page at `/dashboard/notifications/historique`.

---

## 56. Audit log module — `AuditLogServiceImpl` + `AuditEventListener`

**Endpoint** : `GET /api/v1/audit-logs` (`AUDIT_READ`) — paginated filtered listing.

**Filter** : `AuditLogFilter(action, entityType, entrepriseId, magasinId, performedByLabel LIKE, createdStart/EndDate, page=0, size=10)`.

**Scoping** (server-side, `AuditLogServiceImpl.scopeFilter`) :
- ADMIN → full access (no forced filter)
- OWNER → forced `entrepriseId` from JWT
- MANAGER → forced `magasinId` from JWT

**Actions wired** :

| Action | Service |
|--------|---------|
| `LOGIN` / `LOGOUT` | `LoginServiceImpl` / `RefreshTokenServiceImpl` |
| `EMPLOYE_CREATED/ACTIVATED/DEACTIVATED` | `EmployeServiceImpl` |
| `STOCK_ADJUSTMENT` | `AjustementStockServiceImpl` |
| `VENTE_CANCELLED` | `VenteServiceImpl.cancel()` |
| `ACHAT_CANCELLED` | `AchatServiceImpl.cancel()` |
| `PAIEMENT_ABONNEMENT_VALIDATED/REJECTED` | `PaiementAbonnementServiceImpl` |

**Details stored** : LOGIN/LOGOUT → `"IP: x.x.x.x | UA: Chrome"` / `"IP: x.x.x.x | Duration: 2h 15m"` (session duration computed from last LOGIN entry).

**Labels** : `entrepriseLabel` (sigle) + `magasinLabel` (nom) resolved at write time by `AuditEventListener` — self-contained rows, no lookup needed at read time.

**`AuditEvent` record** : carries `performedBy`, `performedByLabel`, `entrepriseId`, `magasinId`, `currency`, `countryName` — all captured in the main thread before `@Async` listener runs.

---

## 57. Admin + Owner + Magasin reporting — `AdminReportingServiceImpl`, `OwnerReportingServiceImpl`

**Admin overview** : `GET /api/v1/admin/reporting/overview` (`ADMIN_ACCESS`) — single `@Transactional(readOnly=true)` call: totalEntreprises/Actives/Inactives, totalMagasins/Actifs/Inactifs, totalEmployes, abonnements by statut, pending/rejected payments, new contact messages, revenueYtd.

**Owner overview** : `GET /api/v1/reporting/owner-overview` (`OWNER_ACCESS`) — company-wide KPIs from JWT `entrepriseId`: ventesTodayCount + ventesTodayTotal, stockBelowThresholdCount, achatsEnAttente (DRAFT), facturesImpayees (NON_PAYEE | PARTIELLEMENT_PAYEE).

**Magasin overview** : `GET /api/v1/reporting/magasin-overview` — per-store KPIs for MANAGER/SELLER.

---

## 58. Country module — `CountryDomainService` + JWT claims

**Endpoint** : `GET /api/v1/countries` (permitAll) — active countries sorted by name.

**Entity** : `Country(name VARCHAR(100), countryCode VARCHAR(5), currency VARCHAR(5), actif)`. 65 countries seeded (V24). V28 fixes accented names using PostgreSQL `U&` Unicode escapes.

**Entreprise FK** : `country_id NOT NULL` (V25 — backfills existing rows to Senegal). `EntrepriseRequest.@NotNull UUID countryId` — owner picks country at registration.

**JWT claims** : at login, both strategies (`ProprietairePrincipalContextStrategy`, `EmployePrincipalContextStrategy`) resolve `entreprise.country.currency` and `entreprise.country.getName()` → stored as `Claim.CURRENCY` + `Claim.COUNTRY_NAME` in the access token. ADMIN (no entreprise) receives null for both.

**Frontend** : `decodeJwtPayload` uses `TextDecoder('utf-8')` (not `atob()` which is Latin-1) to correctly decode accented characters. `useCurrency()` hook reads `user.currency` from auth-store. Country name displayed as a pill (MapPin icon, rounded border) beside the locale switcher in the dashboard header.

---

## 59. Sale invoice PDF — `InvoicePdfServiceImpl`

**Endpoint** : `GET /api/v1/factures-client/{id}/pdf` (`SALE_READ`) — returns `application/pdf` with `Content-Disposition: attachment; filename="FACT-....pdf"`

**Library** : OpenPDF 2.0.3 (LGPL, no server-side rendering required).

**Layout** :
1. **Header** (2 columns): company block (raisonSociale, NINEA, RCCM, adresse) + invoice title block (FACTURE, FACT-... numero, date, dateEcheance) in blue.
2. **Client section** : name, phone, email — or "Client anonyme" if no client.
3. **Lines table** : product nom + ref, quantité, prix unitaire, total HT. Alternating row background.
4. **Totals + payments** : total HT, one row per payment (moyen + date), remaining balance highlighted green (paid) or amber (due).
5. **Footer** : `{entreprise.sigle} – Document généré par Store ERP`.

**Scoping** : `OwnershipHelper.ensureOwnership` checks `facture.commande.magasin.entreprise.id` against current user's JWT `entrepriseId`.

**Reference prefix** : `FactureClientDomainService.generateNumero()` switched from `FAC-VTE` → `FACT` prefix — format `FACT-yyyyMMdd-HHmmssSSS`.

**Frontend** : Download button (Download icon) in `VenteFacturePaiementsSection` beside the invoice numero. `useDownloadFacturePdf` hook fetches with `responseType: 'blob'` and triggers browser file save.

---

## Cross-cutting conventions

- **i18n** : all error messages go through `IMessageSourceService` (keys in `messages*.properties`, fallback `useCodeAsDefaultMessage=true`).
- **Security** : `@PreAuthorize` at controller level for coarse-grained auth ; service responsible for fine-grained business rules.
- **Service isolation** : an `<X>ServiceImpl` only injects `<X>DomainService` + `I<Y>Service` from other aggregates (never a `<Y>Repository`).
- **Responses** : every `<X>Response` must expose a `(<X> entity)` constructor — or secondary constructors for multi-field JPQL projections (see `PublicPlanResponse` use case 37, `AbonnementResponse` use case 40).
- **Permissions** : centralized in the `PermissionCode` enum ; each value = code in DB.
- **Setters in DomainService** (rule 26) : every `entity.setX()` + `save(entity)` lives in `<X>DomainService` as a named business method (`setActive`, `activate`, `markAsValide`, `incrementUsage`, etc.). The ServiceImpls only orchestrate.
- **Record projections** (rule 24 + 38) : repository methods return `<X>Response` by default (JPQL `SELECT new` projection), not entities. Justified entity cases : FK for creation / small table shared by several use cases / closed domain.
- **Externalization of fixed values** (rule 38) : `@ConfigurationProperties` (records in `org.store.property/`) — no `private static final` for parameterizable business values (`SubscriptionProperties.trialDays`, `LoggingProperties.maxPayloadLength`, etc.).
