# MODULES_OVERVIEW.md — Backend global map

> Compact overview of every delivered business module: covered use cases + REST endpoints + permissions.
> For per-use-case detail (input/flow/rules/output), see `FEATURES.md`.
> For package architecture, see `.claude/ARCHITECTURE.md`.

**Last updated**: 2026-05-17
**Total modules**: 11 business modules delivered + 1 skeleton (notification)
**Total REST endpoints**: ~167
**Total YAML permissions**: 70+ (centralized in `org.store.security.application.enums.PermissionCode` + `roles-permissions.yml`)
**Roles**: ADMIN, PROPRIETAIRE, MANAGER, VENDEUR

---

## 1. SECURITY (auth) module

**Delivered use cases:**
- Owner registration (atomic creation of Account + Proprietaire + Entreprise + first Magasin + trial subscription)
- Login (login + JWT + refresh token)
- JWT refresh
- Logout (refresh token revocation)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| POST | `/api/v1/auth/register` | permitAll | public |
| POST | `/api/v1/auth/login` | permitAll | public |
| POST | `/api/v1/auth/refresh` | permitAll | public |
| POST | `/api/v1/auth/logout` | permitAll | public |

**Total endpoints**: 4

---

## 2. ENTREPRISE module

**Delivered use cases:**
- Admin CRUD for companies (create, filtered listing, detail, update, activate/deactivate)
- Owner self-service (read + update of one's own company)
- Company logo (upload, download bytes, delete)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| POST | `/api/v1/entreprises` | `ADMIN_ACCESS` | ADMIN |
| GET | `/api/v1/entreprises?sigle=&raisonSociale=&ninea=&rccm=&actif=&page=&size=` | `ADMIN_ACCESS` | ADMIN |
| GET | `/api/v1/entreprises/{id}` | `ADMIN_ACCESS` | ADMIN |
| PUT | `/api/v1/entreprises/{id}` | `ADMIN_ACCESS` | ADMIN |
| PATCH | `/api/v1/entreprises/{id}/activate` | `ADMIN_ACCESS` | ADMIN |
| PATCH | `/api/v1/entreprises/{id}/deactivate` | `ADMIN_ACCESS` | ADMIN |
| GET | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | PROPRIETAIRE |
| PUT | `/api/v1/entreprises/me` | `PROPRIETAIRE_ACCESS` | PROPRIETAIRE |
| PUT (multipart) | `/api/v1/entreprises/me/logo` | `PROPRIETAIRE_ACCESS` | PROPRIETAIRE |
| GET | `/api/v1/entreprises/me/logo` | `PROPRIETAIRE_ACCESS` | PROPRIETAIRE |
| DELETE | `/api/v1/entreprises/me/logo` | `PROPRIETAIRE_ACCESS` | PROPRIETAIRE |

**Total endpoints**: 11

---

## 3. MAGASIN module

**Delivered use cases:**
- Store CRUD (company scoping, ADMIN access alongside PROPRIETAIRE)
- Paginated filtered listing (`nom`, `actif`)
- Activate/deactivate
- Store logo (upload, download bytes, delete)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| POST | `/api/v1/magasins` | `PROPRIETAIRE_ACCESS` OR `ADMIN_ACCESS` | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins?nom=&actif=&page=&size=` | same | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins/{id}` | same | PROPRIETAIRE/ADMIN |
| PUT | `/api/v1/magasins/{id}` | same | PROPRIETAIRE/ADMIN |
| PATCH | `/api/v1/magasins/{id}/activate` | same | PROPRIETAIRE/ADMIN |
| PATCH | `/api/v1/magasins/{id}/deactivate` | same | PROPRIETAIRE/ADMIN |
| PUT (multipart) | `/api/v1/magasins/{id}/logo` | same | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins/{id}/logo` | same | PROPRIETAIRE/ADMIN |
| DELETE | `/api/v1/magasins/{id}/logo` | same | PROPRIETAIRE/ADMIN |

**Total endpoints**: 9

---

## 4. USERS module

**Delivered use cases:**
- Self-service profile (read, update, change-password)
- Profile photo (upload, download, delete)
- Employee CRUD (creation with data-driven RBAC rules, listing, detail, update, soft delete, activate)
- Admin password reset (distinct from self-service change-password)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| GET | `/api/v1/users/me` | `isAuthenticated` | EMPLOYE/PROPRIETAIRE |
| PUT | `/api/v1/users/me` | `isAuthenticated` | EMPLOYE/PROPRIETAIRE |
| POST | `/api/v1/users/me/change-password` | `AUTH_CHANGE_PASSWORD` | EMPLOYE/PROPRIETAIRE |
| PUT (multipart) | `/api/v1/users/me/photo` | `isAuthenticated` | EMPLOYE/PROPRIETAIRE |
| GET | `/api/v1/users/me/photo` | `isAuthenticated` | EMPLOYE/PROPRIETAIRE |
| DELETE | `/api/v1/users/me/photo` | `isAuthenticated` | EMPLOYE/PROPRIETAIRE |
| POST | `/api/v1/employees` | `EMPLOYE_CREATE` | PROPRIETAIRE/ADMIN/MANAGER |
| GET | `/api/v1/employees?nom=&prenom=&magasinId=&actif=&page=&size=` | `EMPLOYE_READ` | PROPRIETAIRE/ADMIN/MANAGER |
| GET | `/api/v1/employees/{id}` | `EMPLOYE_READ` | PROPRIETAIRE/ADMIN/MANAGER |
| PUT | `/api/v1/employees/{id}` | `EMPLOYE_UPDATE` | PROPRIETAIRE/ADMIN/MANAGER |
| DELETE | `/api/v1/employees/{id}` | `EMPLOYE_DELETE` | PROPRIETAIRE/ADMIN/MANAGER |
| PATCH | `/api/v1/employees/{id}/activate` | `EMPLOYE_DELETE` | PROPRIETAIRE/ADMIN/MANAGER |
| POST | `/api/v1/employees/{id}/reset-password` | `EMPLOYE_RESET_PASSWORD` | PROPRIETAIRE/ADMIN/MANAGER |

**Total endpoints**: 13

---

## 5. PRODUIT module

**Delivered use cases:**
- Product category CRUD (company scoping, label uniqueness)
- Quality CRUD (company scoping, label uniqueness)
- Product CRUD (category + quality, company scoping, reference uniqueness)
- Main product image (upload, download, delete via `PieceJointe @OneToOne`)
- Product gallery (multi-upload, listing, download by id, delete by id)
- Seller-side product search (`q=*&magasinId=*` returning product + PF variants + stock)
- ProductFournisseur CRUD (product × supplier × quality variants + purchase/sale price)
- Sale-price adjustment per PF variant

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| **Categories** | | | |
| POST | `/api/v1/category-products` | `CATEGORY_PRODUCT_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/category-products` | `CATEGORY_PRODUCT_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_DELETE` | PROPRIETAIRE/MANAGER |
| **Qualities** | | | |
| POST | `/api/v1/qualities` | `QUALITY_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/qualities` | `QUALITY_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/qualities/{id}` | `QUALITY_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/qualities/{id}` | `QUALITY_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/qualities/{id}` | `QUALITY_DELETE` | PROPRIETAIRE/MANAGER |
| **Products** | | | |
| POST | `/api/v1/products` | `PRODUCT_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/products` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/products/search?q=&magasinId=&page=&size=` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/products/{id}` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/products/{id}` | `PRODUCT_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/products/{id}` | `PRODUCT_DELETE` | PROPRIETAIRE/MANAGER |
| PUT (multipart) | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/products/{id}/image` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| DELETE | `/api/v1/products/{id}/image` | `PRODUCT_UPLOAD_IMAGE` | PROPRIETAIRE/MANAGER |
| POST (multipart) | `/api/v1/products/{id}/images` | `PRODUCT_UPLOAD_IMAGE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/products/{id}/images` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| DELETE | `/api/v1/products/{id}/images/{imageId}` | `PRODUCT_UPLOAD_IMAGE` | PROPRIETAIRE/MANAGER |
| **ProductFournisseur (variants)** | | | |
| POST | `/api/v1/product-suppliers` | `SUPPLIER_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/product-suppliers?productId=&page=&size=` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/product-suppliers/{id}` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/product-suppliers/{id}` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/product-suppliers/{id}/prix-vente` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/product-suppliers/{id}` | `SUPPLIER_DELETE` | PROPRIETAIRE/MANAGER |

**Total endpoints**: 29

---

## 6. STOCK module

**Delivered use cases:**
- Stock read (per store, per product, paginated)
- Stock below the supply threshold
- Stock valuation (`SUM(qty × prixAchatMoyen)`)
- Expiring lots (per days window)
- Update of supply threshold
- Manual stock entry (FIFO lot + journaled movement)
- Manual stock adjustment (POSITIF/NEGATIF with typed reasons)
- Stock movements read (filtered paginated history)
- Margin reporting (per product/supplier/period)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| GET | `/api/v1/stocks?magasinId=&productId=&page=&size=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/stocks/below-threshold?magasinId=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/stocks/valuation?magasinId=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/stocks/expiring-lots?magasinId=&productId=&daysAhead=&page=&size=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/stocks/{id}` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| PATCH | `/api/v1/stocks/{id}/threshold` | `STOCK_ADJUSTMENT` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/stocks/entries` | `STOCK_ENTRY` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/stocks/adjustments` | `STOCK_ADJUSTMENT` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/stock-movements?magasinId=&stockId=&productId=&type=&startDate=&endDate=&page=&size=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/reports/margins?magasinId=&productId=&fournisseurId=&startDate=&endDate=` | `REPORT_STOCK` | PROPRIETAIRE/MANAGER |

**Total endpoints**: 10

---

## 7. INVENTAIRE module

**Delivered use cases:**
- Inventory creation (store scoping, status EN_COURS)
- Inventory line CRUD (PF + counted quantity, theoretical FIFO quantity snapshot)
- Switch to BILAN (freezes lines + generates `RapportInventaire` with profit/loss/variance computations)
- Closure (applies stock adjustments + dateValidation)
- Cancellation (abandon, status ANNULE)
- Read inventory report (accounting formula benefit + aggregated expenses + working capital)
- Paginated inventory listing

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| POST | `/api/v1/inventaires` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/inventaires?magasinId=&statut=&page=&size=` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/inventaires/{id}` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/inventaires/{id}/lignes` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/inventaires/{id}/lignes` | `STOCK_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/inventaires/{id}/lignes/{ligneId}` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/inventaires/{id}/lignes/{ligneId}` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/inventaires/{id}/bilan` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/inventaires/{id}/cloturer` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/inventaires/{id}/annuler` | `STOCK_INVENTORY` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/inventaires/{id}/rapport` | `STOCK_READ` | PROPRIETAIRE/MANAGER |

**Total endpoints**: 11

---

## 8. ACHAT module

**Delivered use cases:**
- Supplier CRUD (company scoping, reference uniqueness)
- Create a purchase order in DRAFT (lines + lot traceability persisted, no stock nor invoice)
- Edit a DRAFT order line (quantity, price, lot)
- Delete a DRAFT order line (refused if it's the last line)
- Accounting validation of a DRAFT order (invoice creation, amounts frozen, switch to VALIDEE — no stock materialization)
- Physical reception in one or more steps (`POST /receptions`): creates the `EntreeStock` along the way, increments `quantiteRecue` per line, switches to `PARTIELLEMENT_RECEPTIONNEE` or `RECEPTIONNEE` based on progress
- Cancel a VALIDEE / PARTIELLEMENT_RECEPTIONNEE / RECEPTIONNEE order with stock withdrawal (flag `EntreeStock.annulee=true`, movement `RETOUR_FOURNISSEUR`, configurable time window, refused if a lot has already been consumed by a sale)
- Paginated filtered purchase order listing
- Paginated filtered purchase invoice listing
- Invoice due dates (unpaid invoices)
- Order/invoice detail (invoice null if DRAFT)
- Installment payment on an invoice (auto status recompute)
- Listing of payments per invoice

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| **Suppliers** | | | |
| POST | `/api/v1/suppliers` | `SUPPLIER_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/suppliers` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/suppliers/{id}` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/suppliers/{id}` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/suppliers/{id}` | `SUPPLIER_DELETE` | PROPRIETAIRE/MANAGER |
| **Purchases** | | | |
| POST | `/api/v1/achats` | `PURCHASE_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/achats/{commandeId}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/achats/orders/{commandeId}/lignes/{ligneId}` | `PURCHASE_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/achats/orders/{commandeId}/lignes/{ligneId}` | `PURCHASE_DELETE` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/achats/{commandeId}/validate` | `PURCHASE_APPROVE` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/achats/{commandeId}/receptions` | `PURCHASE_APPROVE` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/achats/{commandeId}/annuler` | `PURCHASE_CANCEL` | ADMIN/PROPRIETAIRE/MANAGER |
| GET | `/api/v1/commandes-achat?magasinId=&fournisseurId=&statut=&page=&size=` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/commandes-achat/{id}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat?...` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/echeances` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/{id}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/factures-achat/{id}/paiements` | `PURCHASE_PAY` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/{id}/paiements` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |

**Total endpoints**: 19

---

## 9. VENTE module

**Delivered use cases:**
- Client CRUD (store/company scoping, name/first-name search)
- Create a sale in DRAFT (lines per PF + price floor validation, no stock nor invoice)
- Edit a DRAFT sale line (quantity, unit price)
- Delete a DRAFT sale line (refused if last line)
- Validate a DRAFT sale (consumes FIFO stock + creates FactureClient + optional initial payment + switches to DELIVERED)
- Validation `prixUnitaire ≥ pf.prixVente` (PF floor)
- EMPLOYE seller required (PROPRIETAIRE → 403), client nullable (anonymous sale)
- Paginated filtered sale order listing (magasinId, clientId, vendeurId, statut, montant, reference, dates)
- Client invoices + payments per invoice listing
- Installment payment on a client invoice (status recompute)
- Daily cash summary (orders/products count, orders/payments totals — excludes DRAFT and ANNULEE)
- Top N best-selling products per day (sorted by quantity sold, excludes DRAFT and ANNULEE)
- Sale cancellation with FIFO stock re-injection (`MouvementStock(RETOUR_CLIENT)` compensation, configurable time window)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| **Clients** | | | |
| POST | `/api/v1/clients` | `CLIENT_CREATE` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/clients?nom=&prenom=&page=&size=` | `CLIENT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/clients/{id}` | `CLIENT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/clients/{id}` | `CLIENT_UPDATE` | PROPRIETAIRE/MANAGER/VENDEUR |
| DELETE | `/api/v1/clients/{id}` | `CLIENT_DELETE` | PROPRIETAIRE/MANAGER |
| **Sales** | | | |
| POST | `/api/v1/ventes` | `SALE_CREATE` | VENDEUR/MANAGER |
| GET | `/api/v1/ventes/{commandeId}` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/ventes/orders/{commandeId}/lignes/{ligneId}` | `SALE_UPDATE` | VENDEUR/MANAGER |
| DELETE | `/api/v1/ventes/orders/{commandeId}/lignes/{ligneId}` | `SALE_DELETE` | VENDEUR/MANAGER |
| POST | `/api/v1/ventes/{commandeId}/validate` | `SALE_APPROVE` | VENDEUR/MANAGER |
| POST | `/api/v1/ventes/{commandeId}/annuler` | `SALE_CANCEL` | ADMIN/PROPRIETAIRE/MANAGER |
| GET | `/api/v1/commandes-vente?magasinId=&clientId=&vendeurId=&statut=&reference=&montantMin=&montantMax=&startDate=&endDate=&page=&size=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/commandes-vente/{id}` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/factures-client?statut=&clientId=&startDate=&endDate=&page=&size=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/factures-client/{id}` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/factures-client/{id}/paiements` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| POST | `/api/v1/factures-client/{id}/paiements` | `SALE_PAY` | PROPRIETAIRE/MANAGER/VENDEUR |
| **Cash register** | | | |
| GET | `/api/v1/ventes/caisse/resume?magasinId=&date=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/ventes/caisse/top-produits?magasinId=&date=&nombre=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |

**Total endpoints**: 19

---

## 10. DEPENSE module

**Delivered use cases:**
- Expense category CRUD (company scoping, name uniqueness)
- Expense CRUD (store scoping, label, date, amount, payment method, category)
- Paginated filtered expense listing (magasinId, categoryId, modePaiement, startDate, endDate)
- Aggregated expense total (same filters)

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| POST | `/api/v1/expense-categories` | `EXPENSE_CATEGORY_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/expense-categories` | `EXPENSE_CATEGORY_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/expense-categories/{id}` | `EXPENSE_CATEGORY_DELETE` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/depenses` | `EXPENSE_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/depenses?magasinId=&categoryId=&modePaiement=&startDate=&endDate=&page=&size=` | `EXPENSE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/depenses/total?magasinId=&categoryId=&modePaiement=&startDate=&endDate=` | `EXPENSE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/depenses/{id}` | `EXPENSE_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/depenses/{id}` | `EXPENSE_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/depenses/{id}` | `EXPENSE_DELETE` | PROPRIETAIRE/MANAGER |

**Total endpoints**: 11

---

## 11. ABONNEMENT module

**Delivered use cases:**
- ADMIN CRUD: PlanAbonnement / SubscriptionType / Coupon / Promotion
- Public catalog (no auth, aggregated: plans + types + promotions with per-plan nested promotions)
- Owner subscription (create EN_ATTENTE + sequential discount-calc breakdown type → promotion → coupon)
- Auto-renewal toggle
- Manual payment (PROPRIETAIRE uploads mandatory proof image)
- Admin validation/rejection (activates the subscription with "replacement at dateFin" strategy, coupon rollback on rejection)
- ADMIN listing of all subscriptions + PROPRIETAIRE history + current status (days left, trial, features)

> **Step 9 DEFERRED**: automatic renewal. Depends on integrating an automatic payment provider (Wave / Orange Money / Stripe / PayPal). Today the payment is manual, so the concept "auto-debit at `dateFin`" has no technical support. The flag `Abonnement.renouvellementAuto`, permission `SUBSCRIPTION_RENEW` and endpoint `PATCH /{id}/renouvellement-auto` remain in place (reusable as-is).

| Method | Path | Permission | Actor |
|---------|------|------------|--------|
| **Plans (ADMIN)** | | | |
| POST | `/api/v1/plans` | `PLAN_CREATE` | ADMIN |
| GET | `/api/v1/plans?nom=&actif=&visible=&trial=&page=&size=` | `PLAN_READ` | ADMIN |
| GET | `/api/v1/plans/{id}` | `PLAN_READ` | ADMIN |
| PUT | `/api/v1/plans/{id}` | `PLAN_UPDATE` | ADMIN |
| PATCH | `/api/v1/plans/{id}/activate` | `PLAN_UPDATE` | ADMIN |
| PATCH | `/api/v1/plans/{id}/deactivate` | `PLAN_UPDATE` | ADMIN |
| DELETE | `/api/v1/plans/{id}` | `PLAN_DELETE` | ADMIN |
| **Types (ADMIN)** | | | |
| POST | `/api/v1/subscription-types` | `SUBSCRIPTION_TYPE_CREATE` | ADMIN |
| GET | `/api/v1/subscription-types?nom=&actif=&recommande=&page=&size=` | `SUBSCRIPTION_TYPE_READ` | ADMIN |
| GET | `/api/v1/subscription-types/{id}` | `SUBSCRIPTION_TYPE_READ` | ADMIN |
| PUT | `/api/v1/subscription-types/{id}` | `SUBSCRIPTION_TYPE_UPDATE` | ADMIN |
| PATCH | `/api/v1/subscription-types/{id}/activate` | `SUBSCRIPTION_TYPE_UPDATE` | ADMIN |
| PATCH | `/api/v1/subscription-types/{id}/deactivate` | `SUBSCRIPTION_TYPE_UPDATE` | ADMIN |
| DELETE | `/api/v1/subscription-types/{id}` | `SUBSCRIPTION_TYPE_DELETE` | ADMIN |
| **Coupons (ADMIN)** | | | |
| POST | `/api/v1/coupons` | `COUPON_CREATE` | ADMIN |
| GET | `/api/v1/coupons?code=&actif=&planId=&page=&size=` | `COUPON_READ` | ADMIN |
| GET | `/api/v1/coupons/{id}` | `COUPON_READ` | ADMIN |
| PUT | `/api/v1/coupons/{id}` | `COUPON_UPDATE` | ADMIN |
| PATCH | `/api/v1/coupons/{id}/activate` | `COUPON_UPDATE` | ADMIN |
| PATCH | `/api/v1/coupons/{id}/deactivate` | `COUPON_UPDATE` | ADMIN |
| DELETE | `/api/v1/coupons/{id}` | `COUPON_DELETE` | ADMIN |
| **Promotions (ADMIN)** | | | |
| POST | `/api/v1/promotions` | `PROMOTION_CREATE` | ADMIN |
| GET | `/api/v1/promotions?nom=&actif=&planId=&page=&size=` | `PROMOTION_READ` | ADMIN |
| GET | `/api/v1/promotions/{id}` | `PROMOTION_READ` | ADMIN |
| PUT | `/api/v1/promotions/{id}` | `PROMOTION_UPDATE` | ADMIN |
| PATCH | `/api/v1/promotions/{id}/activate` | `PROMOTION_UPDATE` | ADMIN |
| PATCH | `/api/v1/promotions/{id}/deactivate` | `PROMOTION_UPDATE` | ADMIN |
| DELETE | `/api/v1/promotions/{id}` | `PROMOTION_DELETE` | ADMIN |
| **Public catalog** | | | |
| GET | `/api/v1/catalog/public` | permitAll | public |
| **Subscription / Status** | | | |
| POST | `/api/v1/abonnements/subscribe` | `SUBSCRIPTION_CREATE` | PROPRIETAIRE |
| PATCH | `/api/v1/abonnements/{id}/renouvellement-auto` | `SUBSCRIPTION_UPDATE` | PROPRIETAIRE |
| GET | `/api/v1/abonnements?entrepriseId=&statut=&planId=&page=&size=` | `ADMIN_ACCESS` | ADMIN |
| GET | `/api/v1/abonnements/me?statut=&planId=&page=&size=` | `SUBSCRIPTION_READ` | PROPRIETAIRE |
| GET | `/api/v1/abonnements/me/current` | `SUBSCRIPTION_READ` | PROPRIETAIRE |
| **Subscription payments** | | | |
| POST (multipart) | `/api/v1/paiements-abonnement/abonnements/{abonnementId}` | `SUBSCRIPTION_PAY` | PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement?statut=&abonnementId=&entrepriseId=&page=&size=` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement/{id}` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement/{id}/preuve` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| PATCH | `/api/v1/paiements-abonnement/{id}/validate` | `SUBSCRIPTION_VALIDATE` | ADMIN |
| PATCH | `/api/v1/paiements-abonnement/{id}/reject` | `SUBSCRIPTION_VALIDATE` | ADMIN |

**Total endpoints**: 40

---

## 12. NOTIFICATION module (skeleton)

> DDD structure in place but **no controller implemented**. Entities present: `Notification`, `Echeance`, `TemplateNotification`.
> Planned use case: a worker that walks `Echeance` (overdue invoices approaching, expiring subscriptions…) and sends via `EMAIL` / `SMS` channel.

**Total endpoints**: 0

---

## 📊 Global summary

| Module | Endpoints | Key permissions |
|--------|-----------|------------------|
| security (auth) | 4 | (public) |
| entreprise | 11 | `ADMIN_ACCESS`, `PROPRIETAIRE_ACCESS` |
| magasin | 9 | `PROPRIETAIRE_ACCESS`, `ADMIN_ACCESS` |
| users | 13 | `EMPLOYE_*`, `AUTH_CHANGE_PASSWORD` |
| produit | 29 | `PRODUCT_*`, `CATEGORY_PRODUCT_*`, `QUALITY_*`, `SUPPLIER_*` |
| stock | 10 | `STOCK_READ`, `STOCK_ENTRY`, `STOCK_ADJUSTMENT`, `REPORT_STOCK` |
| inventaire | 11 | `STOCK_INVENTORY`, `STOCK_READ` |
| achat | 19 | `SUPPLIER_*`, `PURCHASE_*` |
| vente | 19 | `CLIENT_*`, `SALE_*` |
| depense | 11 | `EXPENSE_*`, `EXPENSE_CATEGORY_*` |
| abonnement | 40 | `PLAN_*`, `SUBSCRIPTION_TYPE_*`, `COUPON_*`, `PROMOTION_*`, `SUBSCRIPTION_*` |
| notification | 0 | — |
| **TOTAL** | **176** | **70+ permissions** |

---

## 👥 Roles ↔ capabilities matrix

| Role | Main capabilities |
|------|----------------------|
| **ADMIN** | CRUD on companies (other than their own), CRUD on subscription plans/types/coupons/promotions, validation/rejection of subscription payments, global subscription listing |
| **PROPRIETAIRE** | Everything on their own company (stores, employees, products, stock, sales, purchases, expenses), subscription, subscription payment, current status |
| **MANAGER** | Same as PROPRIETAIRE but scoped to their store (employees, stock, sales, purchases, expenses, inventories) |
| **VENDEUR** | Products read + search, client management, sales (create/read/pay), cash register |

---

## 🗂️ Cross-cutting modules (non-business)

- **`common/`** — `BaseEntity` / `AuditableEntity`, `BaseRepository`, `GlobalService<E,R>`, `ValidatorService`, `IUploadFileService`, `LocalizedRuntimeException` + 12 custom exceptions, `IMessageSourceService`, `PieceJointe`, custom validators (`@Phone`, `@EnumValue`, `@DatePattern`, `@Uuid`), helpers `DateHelper`/`EnumHelper`/`UuidHelper`/`SubscriptionRules`/`NameHelper`/`ReferenceHelper`/`LotConsumptionContext`.
- **`config/`** — `StoreApplication`, `I18nConfig`, `AuditorAwareImpl`, `DataInitializer`, `HttpRequestLoggingFilter`.
- **`property/`** — `JwtProperties`, `RbacProperties` (records `@ConfigurationProperties`).

---

## 📝 Further reading

- **Per-use-case detail** (input/flow/rules/output): see `FEATURES.md`.
- **Package structure, stack, conventions**: see `.claude/ARCHITECTURE.md`.
- **Mandatory coding rules**: see `.claude/BACKEND_CODING_CONVENTIONS.md`.
- **Backlog / TODO**: see `.claude/TODO.md`.
- **Session journal**: see `.claude/SESSIONS.md`.
