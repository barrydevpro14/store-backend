# MODULES_OVERVIEW.md — Cartographie globale du backend

> Vue d'ensemble compacte de tous les modules métier livrés : use cases couverts + endpoints REST + permissions.
> Pour le détail use case par use case (entrée/flux/règles/sortie), voir `FONCTIONNALITIES.md`.
> Pour l'architecture des packages, voir `.claude/ARCHITECTURE.md`.

**Dernière mise à jour** : 2026-05-17
**Total modules** : 11 modules métier livrés + 1 squelette (notification)
**Total endpoints REST** : ~163
**Total permissions YAML** : 70+ (centralisées dans `org.store.security.application.enums.PermissionCode` + `roles-permissions.yml`)
**Rôles** : ADMIN, PROPRIETAIRE, MANAGER, VENDEUR

---

## 1. Module SECURITY (auth)

**Use cases livrés :**
- Inscription d'un propriétaire (création atomique Account + Proprietaire + Entreprise + premier Magasin + abonnement trial)
- Connexion (login + JWT + refresh token)
- Rafraîchissement du JWT
- Déconnexion (révocation refresh token)

| Méthode | Path | Permission | Acteur |
|---------|------|------------|--------|
| POST | `/api/v1/auth/register` | permitAll | public |
| POST | `/api/v1/auth/login` | permitAll | public |
| POST | `/api/v1/auth/refresh` | permitAll | public |
| POST | `/api/v1/auth/logout` | permitAll | public |

**Total endpoints** : 4

---

## 2. Module ENTREPRISE

**Use cases livrés :**
- CRUD admin des entreprises (création, listing filtré, détail, update, activate/deactivate)
- Self-service propriétaire (consultation + update de sa propre entreprise)
- Logo entreprise (upload, download bytes, delete)

| Méthode | Path | Permission | Acteur |
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

**Total endpoints** : 11

---

## 3. Module MAGASIN

**Use cases livrés :**
- CRUD magasin (scoping entreprise, accès ADMIN au même titre que PROPRIETAIRE)
- Listing filtré paginé (`nom`, `actif`)
- Activate/deactivate
- Logo magasin (upload, download bytes, delete)

| Méthode | Path | Permission | Acteur |
|---------|------|------------|--------|
| POST | `/api/v1/magasins` | `PROPRIETAIRE_ACCESS` OR `ADMIN_ACCESS` | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins?nom=&actif=&page=&size=` | idem | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins/{id}` | idem | PROPRIETAIRE/ADMIN |
| PUT | `/api/v1/magasins/{id}` | idem | PROPRIETAIRE/ADMIN |
| PATCH | `/api/v1/magasins/{id}/activate` | idem | PROPRIETAIRE/ADMIN |
| PATCH | `/api/v1/magasins/{id}/deactivate` | idem | PROPRIETAIRE/ADMIN |
| PUT (multipart) | `/api/v1/magasins/{id}/logo` | idem | PROPRIETAIRE/ADMIN |
| GET | `/api/v1/magasins/{id}/logo` | idem | PROPRIETAIRE/ADMIN |
| DELETE | `/api/v1/magasins/{id}/logo` | idem | PROPRIETAIRE/ADMIN |

**Total endpoints** : 9

---

## 4. Module USERS

**Use cases livrés :**
- Profil self-service (consultation, update, change-password)
- Photo de profil (upload, download, delete)
- CRUD employé (création avec règles RBAC data-driven, listing, détail, update, soft delete, activate)
- Reset password admin (distinct du change-password self-service)

| Méthode | Path | Permission | Acteur |
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

**Total endpoints** : 13

---

## 5. Module PRODUIT

**Use cases livrés :**
- CRUD catégorie de produit (scoping entreprise, unicité libellé)
- CRUD qualité (scoping entreprise, unicité libellé)
- CRUD produit (catégorie + qualité, scoping entreprise, unicité référence)
- Image principale produit (upload, download, delete via `PieceJointe @OneToOne`)
- Galerie produit (upload multiple, listing, download par id, delete par id)
- Recherche produit côté vendeur (`q=*&magasinId=*` retournant produit + variantes PF + stock)
- CRUD ProductFournisseur (variantes produit × fournisseur × qualité + prix achat/vente)
- Ajustement prix vente par variante PF

| Méthode | Path | Permission | Acteur |
|---------|------|------------|--------|
| **Catégories** | | | |
| POST | `/api/v1/category-products` | `CATEGORY_PRODUCT_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/category-products` | `CATEGORY_PRODUCT_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/category-products/{id}` | `CATEGORY_PRODUCT_DELETE` | PROPRIETAIRE/MANAGER |
| **Qualités** | | | |
| POST | `/api/v1/qualities` | `QUALITY_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/qualities` | `QUALITY_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/qualities/{id}` | `QUALITY_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/qualities/{id}` | `QUALITY_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/qualities/{id}` | `QUALITY_DELETE` | PROPRIETAIRE/MANAGER |
| **Produits** | | | |
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
| **ProductFournisseur (variantes)** | | | |
| POST | `/api/v1/product-suppliers` | `SUPPLIER_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/product-suppliers?productId=&page=&size=` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/product-suppliers/{id}` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/product-suppliers/{id}` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/product-suppliers/{id}/prix-vente` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/product-suppliers/{id}` | `SUPPLIER_DELETE` | PROPRIETAIRE/MANAGER |

**Total endpoints** : 29

---

## 6. Module STOCK

**Use cases livrés :**
- Consultation stock (par magasin, par produit, paginé)
- Stock en dessous du seuil d'approvisionnement
- Valorisation stock (`SUM(qty × prixAchatMoyen)`)
- Lots expirants (par fenêtre de jours)
- Mise à jour seuil d'approvisionnement
- Entrée stock manuelle (lot FIFO + mouvement journalisé)
- Ajustement stock manuel (POSITIF/NEGATIF avec motifs typés)
- Consultation mouvements stock (historique paginé filtré)
- Reporting marges (par produit/fournisseur/période)

| Méthode | Path | Permission | Acteur |
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

**Total endpoints** : 10

---

## 7. Module INVENTAIRE

**Use cases livrés :**
- Création inventaire (scoping magasin, statut EN_COURS)
- CRUD lignes inventaire (PF + quantité comptée, snapshot quantité théorique FIFO)
- Passage en BILAN (fige les lignes + génère `RapportInventaire` avec calculs benefice/perte/écart)
- Clôture (applique les ajustements stock + dateValidation)
- Annulation (abandon, statut ANNULE)
- Consultation rapport inventaire (formule comptable benefice + dépenses agrégées + fond de roulement)
- Listing inventaires paginé

| Méthode | Path | Permission | Acteur |
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

**Total endpoints** : 11

---

## 8. Module ACHAT

**Use cases livrés :**
- CRUD fournisseur (scoping entreprise, unicité référence)
- Création d'une commande d'achat en DRAFT (lignes + traçabilité lot persistées, pas de stock ni facture)
- Édition d'une ligne d'une commande DRAFT (quantité, prix, lot)
- Suppression d'une ligne d'une commande DRAFT (refus si dernière ligne)
- Validation d'une commande DRAFT (matérialisation : facture + entrées stock + journal + update prixVente PF + bascule RECEPTIONNEE)
- Listing commandes d'achat paginé filtré
- Listing factures d'achat paginé filtré
- Échéances factures (factures non payées)
- Détail commande/facture (facture null si DRAFT)
- Paiement échelonné sur facture (recalcul statut auto)
- Listing paiements d'une facture

| Méthode | Path | Permission | Acteur |
|---------|------|------------|--------|
| **Fournisseurs** | | | |
| POST | `/api/v1/suppliers` | `SUPPLIER_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/suppliers` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/suppliers/{id}` | `SUPPLIER_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/suppliers/{id}` | `SUPPLIER_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/suppliers/{id}` | `SUPPLIER_DELETE` | PROPRIETAIRE/MANAGER |
| **Achats** | | | |
| POST | `/api/v1/achats` | `PURCHASE_CREATE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/achats/{commandeId}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| PUT | `/api/v1/achats/orders/{commandeId}/lignes/{ligneId}` | `PURCHASE_UPDATE` | PROPRIETAIRE/MANAGER |
| DELETE | `/api/v1/achats/orders/{commandeId}/lignes/{ligneId}` | `PURCHASE_DELETE` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/achats/{commandeId}/validate` | `PURCHASE_APPROVE` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/commandes-achat?magasinId=&fournisseurId=&statut=&page=&size=` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/commandes-achat/{id}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat?...` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/echeances` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/{id}` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |
| POST | `/api/v1/factures-achat/{id}/paiements` | `PURCHASE_PAY` | PROPRIETAIRE/MANAGER |
| GET | `/api/v1/factures-achat/{id}/paiements` | `PURCHASE_READ` | PROPRIETAIRE/MANAGER |

**Total endpoints** : 17

---

## 9. Module VENTE

**Use cases livrés :**
- CRUD client (scoping magasin/entreprise, recherche nom/prénom)
- Création d'une vente en DRAFT (lignes par PF + validation prix plancher, pas de stock ni facture)
- Édition d'une ligne d'une vente DRAFT (quantité, prixUnitaire)
- Suppression d'une ligne d'une vente DRAFT (refus si dernière ligne)
- Validation d'une vente DRAFT (consomme stock FIFO + crée FactureClient + paiement initial éventuel + bascule DELIVERED)
- Validation `prixUnitaire ≥ pf.prixVente` (plancher PF)
- Vendeur EMPLOYE obligatoire (PROPRIETAIRE → 403), client nullable (vente anonyme)
- Listing commandes vente paginé filtré (magasinId, clientId, vendeurId, statut, montant, référence, dates)
- Listing factures client + paiements par facture
- Paiement échelonné sur facture client (recalcul statut)
- Résumé caisse journalier (nombre commandes/produits, totaux commandes/paiements — exclut DRAFT et ANNULEE)
- Top N produits les plus vendus par jour (tri par quantité vendue, exclut DRAFT et ANNULEE)
- Annulation de vente avec ré-injection stock FIFO (compensation `MouvementStock(RETOUR_CLIENT)`, fenêtre temporelle configurable)

| Méthode | Path | Permission | Acteur |
|---------|------|------------|--------|
| **Clients** | | | |
| POST | `/api/v1/clients` | `CLIENT_CREATE` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/clients?nom=&prenom=&page=&size=` | `CLIENT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/clients/{id}` | `CLIENT_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| PUT | `/api/v1/clients/{id}` | `CLIENT_UPDATE` | PROPRIETAIRE/MANAGER/VENDEUR |
| DELETE | `/api/v1/clients/{id}` | `CLIENT_DELETE` | PROPRIETAIRE/MANAGER |
| **Ventes** | | | |
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
| **Caisse** | | | |
| GET | `/api/v1/ventes/caisse/resume?magasinId=&date=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |
| GET | `/api/v1/ventes/caisse/top-produits?magasinId=&date=&nombre=` | `SALE_READ` | PROPRIETAIRE/MANAGER/VENDEUR |

**Total endpoints** : 19

---

## 10. Module DÉPENSE

**Use cases livrés :**
- CRUD catégorie de dépense (scoping entreprise, unicité nom)
- CRUD dépense (scoping magasin, libellé, date, montant, mode paiement, catégorie)
- Listing dépenses paginé filtré (magasinId, categoryId, modePaiement, startDate, endDate)
- Total dépenses agrégé (même filtres)

| Méthode | Path | Permission | Acteur |
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

**Total endpoints** : 11

---

## 11. Module ABONNEMENT

**Use cases livrés :**
- CRUD ADMIN : PlanAbonnement / SubscriptionType / Coupon / Promotion
- Catalogue public (sans auth, agrégé : plans + types + promotions avec promotions imbriquées par plan)
- Souscription propriétaire (création EN_ATTENTE + breakdown calcul réductions séquentiel type→promotion→coupon)
- Toggle renouvellement auto
- Paiement manuel (PROPRIETAIRE upload preuve image obligatoire)
- Validation/rejet admin (active l'abonnement avec stratégie "remplacement à dateFin", rollback coupon au rejet)
- Listing ADMIN tous abonnements + historique PROPRIETAIRE + statut courant (jours restants, trial, fonctionnalités)

> **Étape 9 DIFFÉRÉE** : renouvellement automatique. Dépend de l'intégration d'un intégrateur de paiement automatique (Wave / Orange Money / Stripe / PayPal). Aujourd'hui le paiement est manuel, donc le concept "auto-débit à `dateFin`" n'a pas de support technique. Le flag `Abonnement.renouvellementAuto`, perm `SUBSCRIPTION_RENEW` et endpoint `PATCH /{id}/renouvellement-auto` restent en place (réutilisables tels quels).

| Méthode | Path | Permission | Acteur |
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
| **Catalogue public** | | | |
| GET | `/api/v1/catalog/public` | permitAll | public |
| **Souscription / Statut** | | | |
| POST | `/api/v1/abonnements/subscribe` | `SUBSCRIPTION_CREATE` | PROPRIETAIRE |
| PATCH | `/api/v1/abonnements/{id}/renouvellement-auto` | `SUBSCRIPTION_UPDATE` | PROPRIETAIRE |
| GET | `/api/v1/abonnements?entrepriseId=&statut=&planId=&page=&size=` | `ADMIN_ACCESS` | ADMIN |
| GET | `/api/v1/abonnements/me?statut=&planId=&page=&size=` | `SUBSCRIPTION_READ` | PROPRIETAIRE |
| GET | `/api/v1/abonnements/me/current` | `SUBSCRIPTION_READ` | PROPRIETAIRE |
| **Paiements abonnement** | | | |
| POST (multipart) | `/api/v1/paiements-abonnement/abonnements/{abonnementId}` | `SUBSCRIPTION_PAY` | PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement?statut=&abonnementId=&entrepriseId=&page=&size=` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement/{id}` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| GET | `/api/v1/paiements-abonnement/{id}/preuve` | `SUBSCRIPTION_READ` | ADMIN/PROPRIETAIRE |
| PATCH | `/api/v1/paiements-abonnement/{id}/validate` | `SUBSCRIPTION_VALIDATE` | ADMIN |
| PATCH | `/api/v1/paiements-abonnement/{id}/reject` | `SUBSCRIPTION_VALIDATE` | ADMIN |

**Total endpoints** : 40

---

## 12. Module NOTIFICATION (squelette)

> Structure DDD posée mais **aucun controller implémenté**. Entités présentes : `Notification`, `Echeance`, `TemplateNotification`.
> Use case envisagé : worker qui parcourt `Echeance` (factures impayées approchant, abonnements expirants…) et envoie via canal `EMAIL` / `SMS`.

**Total endpoints** : 0

---

## 📊 Récap global

| Module | Endpoints | Permissions clés |
|--------|-----------|------------------|
| security (auth) | 4 | (public) |
| entreprise | 11 | `ADMIN_ACCESS`, `PROPRIETAIRE_ACCESS` |
| magasin | 9 | `PROPRIETAIRE_ACCESS`, `ADMIN_ACCESS` |
| users | 13 | `EMPLOYE_*`, `AUTH_CHANGE_PASSWORD` |
| produit | 29 | `PRODUCT_*`, `CATEGORY_PRODUCT_*`, `QUALITY_*`, `SUPPLIER_*` |
| stock | 10 | `STOCK_READ`, `STOCK_ENTRY`, `STOCK_ADJUSTMENT`, `REPORT_STOCK` |
| inventaire | 11 | `STOCK_INVENTORY`, `STOCK_READ` |
| achat | 14 | `SUPPLIER_*`, `PURCHASE_*` |
| vente | 15 | `CLIENT_*`, `SALE_*` |
| depense | 11 | `EXPENSE_*`, `EXPENSE_CATEGORY_*` |
| abonnement | 40 | `PLAN_*`, `SUBSCRIPTION_TYPE_*`, `COUPON_*`, `PROMOTION_*`, `SUBSCRIPTION_*` |
| notification | 0 | — |
| **TOTAL** | **167** | **70+ permissions** |

---

## 👥 Matrice rôles ↔ capacités

| Rôle | Capacités principales |
|------|----------------------|
| **ADMIN** | CRUD entreprises (autres que la sienne), CRUD plans/types/coupons/promotions abonnement, validation/rejet paiements abonnement, listing global abonnements |
| **PROPRIETAIRE** | Tout sur sa propre entreprise (magasins, employés, produits, stocks, ventes, achats, dépenses), souscription abonnement, paiement abonnement, statut courant |
| **MANAGER** | Idem PROPRIETAIRE mais scopé à son magasin (employés, stocks, ventes, achats, dépenses, inventaires) |
| **VENDEUR** | Lecture produits + recherche, gestion clients, ventes (création/lecture/paiement), caisse |

---

## 🗂️ Modules transverses (non métier)

- **`common/`** — `BaseEntity` / `AuditableEntity`, `BaseRepository`, `GlobalService<E,R>`, `ValidatorService`, `IUploadFileService`, `LocalizedRuntimeException` + 12 custom exceptions, `IMessageSourceService`, `PieceJointe`, validators custom (`@Phone`, `@EnumValue`, `@DatePattern`, `@Uuid`), helpers `DateHelper`/`EnumHelper`/`UuidHelper`/`SubscriptionRules`/`NameHelper`/`ReferenceHelper`/`LotConsumptionContext`.
- **`config/`** — `StoreApplication`, `I18nConfig`, `AuditorAwareImpl`, `DataInitializer`, `HttpRequestLoggingFilter`.
- **`property/`** — `JwtProperties`, `RbacProperties` (records `@ConfigurationProperties`).

---

## 📝 Pour aller plus loin

- **Detail use case par use case** (entrée/flux/règles/sortie) : voir `FONCTIONNALITIES.md`.
- **Structure des packages, stack, conventions** : voir `.claude/ARCHITECTURE.md`.
- **Règles de codage obligatoires** : voir `.claude/CONVENTION_CODAGE_BACKEND.md`.
- **Backlog / TODO** : voir `.claude/TODO.md`.
- **Journal des sessions** : voir `.claude/SESSIONS.md`.
