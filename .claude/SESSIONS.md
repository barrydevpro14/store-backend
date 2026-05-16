# SESSIONS.md — Journal des sessions

> Claude Code remplit ce fichier à la fin de chaque session.
> À relire en priorité au démarrage d'une nouvelle session pour reprendre le contexte immédiatement.

---

## 📌 Dernière session

**Date :** 2026-05-16
**Sujet :** **Module Vente — fonctionnalité 3 : Vente atomique** (`POST /api/v1/ventes` + `GET /api/v1/ventes/{id}`) avec ligne = ProductFournisseur (vente par variante, pas par Product), FIFO scopé par PF, vendeur = Employe obligatoire, validation `prixUnitaire ≥ pf.prixVente`. **Refactor `UserPrincipal` transverse** (rename `userId` → `accountId` + ajout `userId` métier = Utilisateur.id, claim JWT `USER`). 4 règles backend ajoutées en session précédente déjà inscrites (34-37) ; cette session focus 100% sur F-V3 + le refactor JWT.

**Ce qui a été fait :**

1. **Migration Flyway V12** `add_paiement_vente_and_pf_on_ligne_vente.sql` :
   - Table `paiement_vente` (montant, datePaiement, moyen, facture FK, audit).
   - `ligne_commande_vente.product_fournisseur_id` (FK vers `product_fournisseur`) — pour la vente par variante.

2. **Entités** :
   - `PaiementVente` (montant, datePaiement, moyen MoyenPaiement, facture FK).
   - `LigneCommandeVente` : ajout `productFournisseur` (en plus du `product` historique).
   - `SortieStock.ligneVente` déjà présent en V1 — utilisé pour la 1ère fois.

3. **Refactor `UserPrincipal` (impact transverse)** :
   - Structure : `(accountId, userId, entrepriseId, magasinId, username, role, permissions)` — 7 champs au lieu de 6.
   - `accountId` (anciennement `userId`) = `Account.id` (auth).
   - `userId` (nouveau) = `Utilisateur.id` métier (= `Employe.id` pour un employé).
   - `Claim.USER("userId")` ajouté au JWT.
   - `AuditorAwareImpl` migré vers `principal.accountId()`.
   - `UserPrincipalFactoryImpl` passe `account.user.id`.
   - 18 tests adaptés via sed : `new UserPrincipal(X, UUID.randomUUID(), Y, ...)`.

4. **Domain services Vente** :
   - `CommandeVenteDomainService` : `create(CommandeVenteCreate)`, `applyMontantTotal(commande, montant)`, `generateReference()` (VTE-).
   - `LigneCommandeVenteDomainService.create(LigneCommandeVenteCreate)` : `montantTotal = qty × prixUnitaire`, set `product` + `productFournisseur`.
   - `FactureClientDomainService` : `create(FactureClientCreate)`, `generateNumero()` (FAC-VTE-), `applyPaiement(facture, montant)`, `findByCommandeId(...)`.
   - `PaiementVenteDomainService.create(facture, montant, moyen)` + `findAllByFactureId`.

5. **Extension Stock** :
   - `EntreeStockRepository.findAvailableLotsForFifoByProductFournisseur(magasinId, pfId)` (FIFO scopé par PF, pas par Product).
   - Record `SortieStockCreate(lot, quantite, prixVente, ligneVente)` — supporte aussi ajustements (ligneVente=null).
   - `SortieStockDomainService.create(SortieStockCreate)` (avec lien ligneVente) + surcharge existante.
   - Record `SortieStockForVente(magasin, productFournisseur, quantite, prixVente, ligneVente)`.
   - `ISortieStockService.consumeForVente(SortieStockForVente)` : vérifie stock, consomme FIFO par PF, lie chaque sortie à la ligne vente, décrémente Stock, journalise mouvement.

6. **Services applicatifs** :
   - `IEmployeService.findCurrentUser()` : throw `ForbiddenException("vente.user.required")` si l'utilisateur connecté n'est pas un Employe.
   - `IAccountService.findUserSummaryByAccountId(String)` : résout `createdBy` (audit) → `UserSummaryResponse(id, nomComplet)` via Account → Utilisateur (parent Person avec nom/prenom). Optional empty si introuvable ou format invalide.
   - `IAccountService.findOptionalById(UUID)` : lecture safe d'Account (sans throw).
   - `AccountDomainService.findOptionalById(UUID)` + `EmployeDomainService.findOptionalById(UUID)`.

7. **`IVenteService` (création atomique + détails)** :
   - `create(VenteRequest)` : valide, récupère vendeur Employe + magasin, résout client (nullable), valide lignes (`prixUnitaire ≥ pf.prixVente`), crée commande + lignes + déclenche `sortieStockService.consumeForVente()` par ligne, applique montantTotal, crée facture, applique premier paiement éventuel.
   - `findDetailsById(commandeId)` : récupère commande + facture + lignes + paiements, scoping entreprise, user résolu via `accountService.findUserSummaryByAccountId(commande.getCreatedBy())`.
   - `ensureBelongsToCurrentEntreprise(commande)` public dans VenteServiceImpl.

8. **DTOs (11 nouveaux)** :
   - Request : `VenteRequest(clientId?, dateVente, lignes[], premierPaiement?)`, `LigneVenteRequest(productFournisseurId, quantite, prixUnitaire)`, `PaiementVenteRequest(montant, modePaiement)`.
   - Response : `VenteResponse(commande, facture)`, `VenteDetailsResponse(commande, facture, lignes[], paiements[])`, `CommandeVenteResponse(...)` avec sous-DTO `UserSummaryResponse(id, nomComplet)`, `LigneCommandeVenteResponse`, `FactureClientResponse`, `PaiementVenteResponse`.
   - Records internes : `CommandeVenteCreate`, `LigneCommandeVenteCreate`, `FactureClientCreate`, `VenteContext`.
   - `UserSummaryResponse(id, nomComplet)` déplacé dans `common/dto/` (réutilisable).

9. **Controller** `VenteController` :
   - `POST /api/v1/ventes` (`SALE_CREATE`) → 201.
   - `GET /api/v1/ventes/{commandeId}` (`SALE_READ`) → 200.

10. **i18n** : 4 nouvelles clés FR/EN (`vente.user.required`, `vente.prixUnitaire.belowFloor`, `commandeVente.notOwned`, `factureClient.notFoundForCommande`).

11. **Tests** : 6 service + 3 controller (POST happy + 400 lignes vides, GET 200, find* + validations métier). 436/436 verts (+9 vs précédemment).

**Décisions / arbitrages :**

- **Vente par ProductFournisseur, pas par Product** — le client choisit la variante (Clou Chine vs Maroc vs France), pas le lot. Chaque ligne cible 1 PF unique. Mix de variantes = N lignes. FIFO scopé par PF.
- **Plancher prix = `pf.prixVente`** simple (pas de MAX FIFO) car le prixVente vit sur le PF (1 valeur par variante). Validation `ligne.prixUnitaire ≥ pf.prixVente`.
- **Vendeur EMPLOYE obligatoire** — PROPRIETAIRE refusé avec Forbidden. Récupération via `IEmployeService.findCurrentUser()` qui lit `UserPrincipal.userId` (métier) et query `Employe.findById`.
- **Option Minimaliste sur `commande.user`** — pas de FK `user_id` ajoutée sur les tables métier (commande/paiement vente, commande/paiement achat). On s'appuie sur l'audit `createdBy` existant (= `Account.id` stringifié, set par `AuditorAware`). Pour le listing : résolution via `IAccountService.findUserSummaryByAccountId(createdBy)` → 1 query supplémentaire mais modèle minimaliste sans redondance. Refactor `UserPrincipal` quand même fait (utile pour audit, futurs use cases).
- **Refactor `UserPrincipal`** — décision validée par l'utilisateur (option A) pour avoir l'`utilisateurId` métier disponible dans le service vente sans query supplémentaire. Renommage `userId` → `accountId` + ajout `userId` métier = solution propre vs alternatives (`@Lazy`, query par vente).
- **Référence + numéro auto-générés** (`VTE-...`, `FAC-VTE-...`) via `ReferenceHelper.generate(base)` — pas de saisie manuelle (vendeur au comptoir, frontend ne pré-prépare pas).
- **Numéro de facture du module Achat reste saisi** (fournisseur le donne) ; numéro vente est auto (on émet la facture nous-mêmes).
- **`SortieStock.ligneVente`** : FK existait depuis V1, set pour la 1ère fois via `SortieStockCreate.ligneVente` passé jusqu'au domain service.
- **2 méthodes `SortieStockDomainService.create()`** : nouvelle signature `create(SortieStockCreate)` (avec ligneVente) + surcharge existante `create(lot, qty, prix)` qui wrappe avec ligneVente=null (rétro-compat pour ajustements stock).
- **Méthodes publiques (règle 27)** : `processVenteLine`, `applyPremierPaiementIfPresent`, `ensureBelongsToCurrentEntreprise` etc. publiques dans VenteServiceImpl (pas sur l'interface) — cohérent avec le pattern AchatServiceImpl.
- **Lambda `lignes.forEach(...)` + `Iterator` synchronisé** pour parcourir lignes + productFournisseurs en parallèle (règle 34, pattern récent).

**Où on s'est arrêté :**

- **436 tests verts** (+9 vs précédent). Compile vert.
- **4 commits atomiques sur `dev`** (non pushés) — découpage par axe pour faciliter relecture / revert :
  - `492ddec` — `refactor(security): split UserPrincipal accountId/userId métier (claim USER)` (29 fichiers : 5 main security/config + 24 tests adaptés)
  - `b42a590` — `feat(security): résolution audit createdBy + lookup employé courant` (7 fichiers : `UserSummaryResponse` commun + `IAccountService.findUserSummaryByAccountId` + `IEmployeService.findCurrentUser`)
  - `ec5fb4a` — `feat(stock): consumeForVente FIFO scopé par ProductFournisseur` (7 fichiers : records `SortieStockCreate`/`SortieStockForVente` + repo FIFO par PF + surcharge `consumeForVente`)
  - `32126e8` — `feat(vente): F-V3 vente atomique + facture + premier paiement` (32 fichiers : entités + domain + DTOs + service + controller + V12/V13 + i18n + tests)
- **Migrations V12 (`paiement_vente` + `pf` sur ligne) et V13 (drop `commande_vente.vendeur_id` — Option Minimaliste)** : à appliquer sur la BD locale au démarrage de l'app.
- **Permissions SALE_*** : déjà en YAML, pas de modif.
- **`FactureClient.dateEcheache`** : typo V1 conservée (correction reportée).

**Prochaine étape recommandée :**

1. **F-V4 — Listings ventes** (`GET /api/v1/ventes?magasinId=&from=&to=&statut=&page=&size=`) + listing factures + paiements paginés filtrés (symétrie achat F12-F14).
2. **F-V5 — Paiement échelonné** (`POST /api/v1/ventes/factures/{id}/paiements`) — ajouter des paiements après la création de la vente.
3. **Module dashboard / reporting** — CA, marges, top clients, KPIs.
4. **Inventaire physique** (fonct. 12 stock reportée) — comparer stock théorique vs physique.

---

### Session du 2026-05-15 (soirée)
**Sujet :** **Module Vente — fonctionnalité 2 : Recherche produit vendeur + déplacement structurel de `Quality` vers `ProductFournisseur` + introduction de `prixVente` (sur PF et lignes d'achat) + validation `prixVente > prixAchat` + endpoint PUT prix-vente manager.** Refactor architectural significatif : extraction d'un service dédié `IProductSearchService` pour casser un cycle d'injection.

**Ce qui a été fait :**

1. **Migration Flyway V11** `move_quality_to_product_fournisseur_and_add_prix_vente.sql` :
   - `product_fournisseur` : `ADD COLUMN quality_id UUID` (nullable temp) + FK + backfill `UPDATE pf SET quality_id = product.quality_id` + `NOT NULL` + `UNIQUE (product_id, fournisseur_id, quality_id)` + index.
   - `product_fournisseur` : `ADD COLUMN prix_vente NUMERIC(19,2)` + backfill `= prix_achat` + `NOT NULL`.
   - `ligne_commande_achat` : `ADD COLUMN prix_vente NUMERIC(19,2)` + backfill `= prix_achat` + `NOT NULL`.
   - `product` : `DROP CONSTRAINT` dynamique (bloc `DO $$`) pour la FK quality + `DROP COLUMN IF EXISTS quality_id`.

2. **Entités** : `Product` perd `quality`. `ProductFournisseur` gagne `quality` (`@ManyToOne LAZY optional=false`) + `prixVente` (`@Column nullable=false precision=19 scale=2`). `LigneCommandeAchat` gagne `prixVente` (idem).

3. **DTOs adaptés** :
   - `ProductRequest` : retire `qualityId`. `ProductResponse` : retire `quality`.
   - `ProductFournisseurRequest` : ajoute `@NotNull UUID qualityId` + `@NotNull @DecimalMin(0, exclusive) BigDecimal prixVente`.
   - `ProductFournisseurResponse` : ajoute `quality: QualitySummaryResponse` + `prixVente`.
   - `LigneAchatRequest`, `LigneCommandeAchatCreate`, `LigneCommandeAchatResponse` : ajoutent `prixVente`.
   - Nouveau `ProductFournisseurPrixVenteRequest(prixVente)` pour PUT dédié.

4. **DTOs recherche (nouveaux)** :
   - `ProductSearchResponse(id, nom, reference, description, category, image, quantiteEnStock, productFournisseurs[])` + constructeur secondaire `(Product, quantiteEnStock, lots)`.
   - `ProductFournisseurStockResponse(id, quality, fournisseur, prixVente, quantiteEnStock)` + constructeur secondaire `(ProductFournisseur, quantiteEnStock)`.

5. **Services adaptés** :
   - `ProductDomainService.create()` sans quality + nouvelle `searchByEntrepriseWithActiveLots`.
   - `ProductFournisseurDomainService.create(...)` accepte quality + prixVente, + `updatePrixVente`, unicité triplet.
   - `LigneCommandeAchatDomainService.create()` accepte prixVente.
   - `EntreeStockDomainService.findActiveLotsByMagasinAndProductIds(...)` (avec fetch joins PF/fournisseur/quality).
   - `IProductFournisseurService` : nouvelles méthodes `updatePrixVente(id, price)`, `applyPrixVenteFromPurchase(pf, price)`, `ensurePrixVenteGreaterThanPrixAchat(prixVente, prixAchat)` (publique réutilisable).
   - `IEntreeStockService.findActiveLotsByMagasinAndProductIds(...)` (façade pour `IProductSearchService`).
   - `AchatServiceImpl` : validation `prixVente > prixAchat` à chaque ligne via `productFournisseurService.ensurePrixVenteGreaterThanPrixAchat(...)`, et **mise à jour automatique** `pf.prixVente = ligne.prixVente` après chaque ligne d'achat.

6. **Service dédié `IProductSearchService` + `ProductSearchServiceImpl`** :
   - **Raison** : le cycle `IProductService → IEntreeStockService → IProductFournisseurService → IProductService` empêchait Spring de résoudre les beans (Spring Boot 3+ interdit les références circulaires par défaut).
   - Extraction d'un service dédié qui orchestre la recherche (n'est injecté par personne, donc pas de cycle).
   - Stratégie 2 queries pour éviter N+1 : (1) `Page<Product>` paginée filtrée par `searchTerm + EXISTS lot actif dans magasin` ; (2) `List<EntreeStock>` actifs (fetch joints PF/fournisseur/quality) pour les IDs paginés. Agrégation par produit puis par PF en Java.
   - Résolution `magasinId` : EMPLOYE = implicite (`UserPrincipal.magasinId`), PROPRIETAIRE = obligatoire (sinon `BadArgumentException("product.search.magasinIdRequired")`).

7. **Endpoints** :
   - `GET /api/v1/products/search?q=&magasinId=&page=&size=` (permission `PRODUCT_READ`, dans `ProductController` injectant `IProductSearchService`).
   - `PUT /api/v1/product-suppliers/{id}/prix-vente` (permission `SUPPLIER_UPDATE`, modification libre par manager, validation `> prixAchat`).

8. **i18n** : nouvelles clés `productFournisseur.prixVente.belowOrEqualAchat` + `product.search.magasinIdRequired` (FR + EN). Reformulation `productFournisseur.alreadyExists` pour mentionner la qualité.

9. **Tests adaptés + nouveaux** :
   - `ProductServiceImplTest` : retiré `qualityService`, retiré `setQuality`, signature `create(request, category, entreprise)`, supprimé le test `quality_belongs_to_other_entreprise`.
   - `ProductControllerTest` : retiré `QualitySummaryResponse`, mock `IProductSearchService` ajouté, nouveau test search.
   - `ProductFournisseurServiceImplTest` : ajouté `qualityService`, sample avec quality + prixVente, signature create avec quality, nouveaux tests pour `updatePrixVente`, validation `prixVente > prixAchat`, `ensureTripletAvailable`.
   - `ProductFournisseurControllerTest` : sample/validBody enrichis, nouveau test PUT prix-vente, nouveau test 400 prix-vente null.
   - `AchatServiceImplTest` + `AchatControllerTest` : `LigneAchatRequest` enrichi de `prixVente`.
   - Nouveau `ProductSearchServiceImplTest` : 4 tests (BadArgument propriétaire sans magasinId, employé implicite, agrégation par PF, page vide).

**Décisions / arbitrages :**

- **Modèle : quality sur PF, pas sur Product** — décision utilisateur explicite avec exemple concret "Clou 10mm : Chine 10 FCFA, Maroc 15 FCFA, France 25 FCFA". Un même produit (nom/réf) peut être livré par un même fournisseur en plusieurs qualités, d'où unicité `(product, fournisseur, quality)`.
- **prixVente vit sur ProductFournisseur** (pas sur le lot) — itération : initialement pensé sur EntreeStock, puis corrigé après l'exemple Clou. Un PF = un prix de vente courant unique, mis à jour à chaque achat.
- **Validation `prixVente > prixAchat`** (strict, pas `>=`) — marge zéro interdite. Backfill historique `= prix_achat` toléré (validation applicative aux nouveaux flux uniquement).
- **Pas de règle "≥ ancien prix"** — utilisateur a refusé la rigidité ; à la place : endpoint PUT séparé géré par le manager, sans contrainte de baisse. Le marché peut baisser, le manager décide.
- **Endpoint PUT prix-vente dédié** : `PUT /product-suppliers/{id}/prix-vente`. Permission `SUPPLIER_UPDATE` (manager, propriétaire, admin ; pas vendeur). Validation `> prixAchat actuel`.
- **Mise à jour auto à l'achat** : `pf.prixVente = ligne.prixVente` après chaque ligne d'achat (cohérent avec "le prix de vente est renseigné lors d'un achat"). Pas de contrainte ≥ ancien à l'achat (le manager peut donc baisser indirectement via un nouvel achat — c'est le canal métier normal).
- **Service dédié pour la recherche** : `IProductSearchService` extrait pour casser un cycle d'injection détecté à l'exécution (Spring Boot ne tolère pas les cycles par défaut). Cassage cycle propre vs `@Lazy` hack. Le controller `ProductController` injecte maintenant 2 services (cohérent avec la règle "controller minimal" qui autorise plusieurs services injectés).
- **2 queries pour éviter N+1** : pagination JPQL sur Product, puis fetch séparé des lots actifs pour les productIds paginés. Performance acceptable pour les volumes attendus (centaines de produits, dizaines de lots actifs par produit).
- **Backfill V11 conservateur** : `prix_vente = prix_achat` (marge zéro affichée). Les lots historiques restent vendables à exactement prix_achat. Le manager corrigera au prochain achat ou via PUT prix-vente.
- **DROP CONSTRAINT dynamique** : sécurité pour BD baselinée où le nom de FK peut être auto-généré par Hibernate (pattern V10 réutilisé).

**Bug rencontré : Flyway en état failed**

Lors de la première tentative de V11, le DROP CONSTRAINT statique a échoué (FK auto-généré par Hibernate avec un nom différent de `fk_product_quality`). Flyway a marqué l'entrée comme `success=false` dans `flyway_schema_history`. PostgreSQL a rollbacké la migration (transactional DDL), mais l'entrée failed bloque les retries. La V11 a été corrigée avec un bloc `DO $$` dynamique, mais l'entrée doit être nettoyée manuellement avant que le `StoreApplicationTests.contextLoads` ne passe :

```bash
PGPASSWORD=passer psql -h localhost -U barrydevit -d db_store \
  -c "DELETE FROM flyway_schema_history WHERE success = false;"
```

Après nettoyage, V11 se rejouera automatiquement et `StoreApplicationTests.contextLoads` passera (425e test vert).

**Où on s'est arrêté :**

- **Tests : 415 → 424 (passants) + 1 KO (`StoreApplicationTests` à débloquer via le DELETE flyway_schema_history ci-dessus)**. Compile vert.
- **Migration V11** créée et corrigée (drop FK dynamique + `DROP COLUMN IF EXISTS`).
- **Pas encore committé** sur dev (branche en avance de 1 commit sur `origin/dev` depuis F-V1).
- **3 décisions métier importantes inscrites en doc** : (a) quality sur PF (multi-qualité même fournisseur possible), (b) prixVente sur PF mis à jour à chaque achat + endpoint manager, (c) marge stricte (`prixVente > prixAchat`).
- **Pattern d'extraction de service pour casser un cycle** documenté (`IProductSearchService` isolé sans dépendances dans le cycle).

**Prochaine étape recommandée :**

1. **Cleanup BD** : exécuter le DELETE Flyway ci-dessus pour débloquer le 425e test.
2. **F-V3 — Vente atomique** (`POST /api/v1/sales`) — symétrique au module Achat F12-F14. `CommandeVente` (référence auto `VTE-yyyyMMdd-HHmmssSSS`) + `LigneCommandeVente` + appel `ISortieStockService.create()` FIFO (déjà livré) + `FactureClient` (numéro auto) + `PaiementVente` initial. À la vente, contrainte : prix saisi par le vendeur ≥ MAX(prixVente des PF des lots consommés en FIFO).
3. **F-V4 — Listings ventes** (commandes, factures, échéances).
4. **F-V5 — Paiement échelonné** (multiple PaiementVente par facture).
5. **Module dashboard / reporting** ensuite.

---

### Session du 2026-05-15
**Sujet :** **Démarrage du module Vente — fonctionnalité 1 : CRUD Client** + 3 nouvelles règles de codage transverses (variables explicites, DTO Filter ≥ 2 critères, retrait de l'helper `normalize` au profit d'une gestion null directe en JPQL). Pattern décalqué sur le CRUD Fournisseur (module Achat). Scoping double (employé = magasin, propriétaire = entreprise). Décision métier sur le "client anonyme" : pas d'enregistrement BDD dédié, juste `CommandeVente.clientId` qui sera nullable à F-V3.

**Ce qui a été fait :**

1. **Feuille de route Module Vente** validée (5 fonctionnalités) : F-V1 CRUD Client, F-V2 Recherche produit, F-V3 Vente atomique (cycle complet panier → FIFO → facture → paiement), F-V4 Listings, F-V5 Paiement échelonné. Arbitrages métier verrouillés en début de session (client optionnel, scoping Client.magasin, numéro de facture auto-généré via `ReferenceHelper`).

2. **F-V1 — CRUD Client complet** (`/api/v1/clients`) — `Client extends Person` rattaché à `Magasin` (l'entité, le port `ClientRepository` et le `ClientDomainService` existaient déjà vides depuis le squelette initial). Endpoints POST/GET paginé/GET id/PUT/DELETE avec `@PreAuthorize('CLIENT_*')`. Scoping double via `IMagasinService.ensureAccessibleByCurrentUser` (employé = magasin, propriétaire = entreprise). DTOs : `ClientRequest`, `ClientResponse(Client)`, `ClientSummaryResponse(id, nomComplet)`, `ClientFilter(nom, prenom, page, size)`. Permissions `CLIENT_{CRUD}` ajoutées (ADMIN/PROPRIETAIRE/MANAGER en CRUD, VENDEUR en CREATE+READ). i18n FR/EN `client.{notFound,notOwned}`. 15 tests service + 9 controller, **415 verts** (+23 vs 392). Pas de migration Flyway (la table `client` était déjà dans `V1__init_schema.sql`).

3. **3 refactors itératifs sur F-V1 suite à feedback utilisateur** :
   - **Retrait `magasinId` + `entrepriseId` de `ClientResponse`** : le scoping reste interne, le front n'a pas besoin de connaître le magasin du client. Tests adaptés (ArgumentCaptor<Client> pour vérifier le changement de magasin en update).
   - **Variables explicites** (nouvelle règle de codage projet) : `String q` → `String searchTerm`, `Client c` → `Client client`, `Magasin m` → `Magasin attachedMagasin`, `Client foreign` → `Client foreignClient`. Règle inscrite en mémoire : bannir les noms courts (`q, c, m, ent, f`), garder le nom externe court côté HTTP (`@RequestParam value = "q"`) mais nommer explicitement la variable Java.
   - **Scission `searchTerm` → `nom` + `prenom`** + adoption du pattern `ClientFilter` (record + `toPageable()` + validation via `ValidatorService`). Nouvelle règle de codage projet : **toujours créer un DTO `<X>Filter` dès qu'un endpoint a ≥2 critères de recherche** (plus stricte que la règle 30 "max 3 paramètres"). `normalize(searchTerm)` retiré du `ClientDomainService` (gestion null directement en JPQL via `(:nom IS NULL OR ...)`). Repo Spring Data conserve l'exemption (params individuels `@Param` autorisés au-delà de 3).

4. **3 nouvelles règles mémoire enregistrées** :
   - `feedback_variables_explicites` — bannir noms courts, variables locales et params = noms métier explicites.
   - `feedback_dto_filter` — `<X>Filter` record dès ≥2 critères. Service prend filter unique, `validatorService.validate(filter)`.
   - `project_client_anonyme` — pas d'enregistrement dédié, `CommandeVente.clientId` nullable plus tard.

**Décisions / arbitrages :**

- **Client anonyme = nullable, pas d'entité** — décision explicite de l'utilisateur. Évite migration + flag + enregistrements artificiels. À F-V3, `CommandeVente.client` sera nullable, front affichera "Client anonyme" si vide.
- **`ClientResponse` ne porte pas le magasin** — le scoping est invisible côté API. Si plus tard le PROPRIETAIRE multi-magasins en a besoin pour afficher "à quel magasin appartient ce client", on ajoutera un sous-DTO `magasin: MagasinSummaryResponse` (règle 23).
- **VENDEUR peut créer des clients** — saisie au comptoir lors d'une vente. Modifier/supprimer reste réservé MANAGER/PROPRIETAIRE.
- **Aucune unicité téléphone** — homonymes/téléphones partagés fréquents en boutique de pièces détachées.
- **Recherche `nom` + `prenom` séparés** (pas un `searchTerm` qui matche les deux + téléphone) — décision utilisateur en cours de session. Téléphone retiré du périmètre de recherche pour cette F-V1.
- **Plan mode utilisé** une fois en début de F-V1 (avant écriture du plan validé) — bénéfice marginal sur un livrable qui décalque exactement le pattern Fournisseur. À réserver à des features sans précédent dans le projet.

**Où on s'est arrêté :**

- **Tests : 392 → 415** (+23 : 15 service + 9 controller -1 retiré après refactor Filter).
- **1 commit poussé** sur `dev` (pas de push, branche en avance de 1 sur `origin/dev`) :
  - `8d70ba1` — CRUD Client (module vente - fonctionnalité 1)
- **Pas de migration Flyway** créée (la table existait).
- **Enum `PermissionCode`** : +4 valeurs (`CLIENT_{CRUD}`). YAML aligné sur les 4 rôles.
- **Pattern `<X>Filter` officialisé** : `ClientFilter` est le 7e du projet (après Stock, MouvementStock, ExpiringLots, MarginReport, Depense, FactureAchatEcheance, CommandeAchat).
- **3 nouvelles règles de codage** inscrites en mémoire user (`feedback_variables_explicites`, `feedback_dto_filter`, `project_client_anonyme`).

**Prochaine étape recommandée :**

1. **F-V2 — Recherche produit vendeur** (`GET /api/v1/products/search?q=...` par nom/référence). Simple et rapide, utile au vendeur pour préparer la vente. Le pattern projet va probablement imposer un `ProductSearchFilter` (≥2 critères : nom + reference, peut-être code-barres futur).
2. **F-V3 — Vente atomique** (`POST /api/v1/sales`) : symétrie directe avec Achat F12-F14. `CommandeVente` (référence auto `VTE-yyyyMMdd-HHmmssSSS`) + lignes + appel `ISortieStockService.create()` FIFO interne + `FactureClient` (numéro auto `FAC-...`) + `PaiementVente` initial. Sous-services à créer (`ICommandeVenteService`, `IFactureClientService`, `IPaiementVenteService`). Migration Flyway V11 pour ajouter l'entité `PaiementVente` (sera la première de cette session).
3. **F-V4 / F-V5** dans la foulée (listings + paiement échelonné) — petits par rapport à F-V3.
4. **Module dashboard / reporting** une fois le cycle Vente livré : recap CA, marges, dépenses, clients top, etc.

---

### Session du 2026-05-14 (soirée)
**Sujet :** **Module Achat complet (F12-F14)** + **Module Dépense complet** — orchestration achat atomique (commande + facture + paiements + entrée stock), CRUD CategoryDepense scopé entreprise + CRUD Depense scopé magasin. `ReferenceHelper` ajouté pour la génération auto de références (`CMD-yyyyMMdd-HHmmssSSS`). Décision métier sur `Depense` : suppression de `dateEcheance` (pas pertinent — une dépense n'a pas d'échéance, c'est une dette ponctuelle).

**Ce qui a été fait :**

1. **Module Achat F12-F14 — commande + facture + paiements atomique** (`POST /api/v1/purchases` permission `PURCHASE_CREATE`) — flux métier unique : le manager appelle le fournisseur, reçoit la marchandise + facture en même temps. Endpoint orchestre dans une seule transaction : création `CommandeAchat` (référence auto via `ReferenceHelper.generate("CMD")`) + `FactureAchat` (numéro saisi unique par entreprise) + N lignes de produits avec entrée stock immédiate (réutilise `IEntreeStockService`) + premier paiement éventuel (`PaiementAchat`). Sous-services : `ICommandeAchatService`, `IFactureAchatService`, `IPaiementAchatService` + listings dédiés (`GET /purchases/orders`, `GET /purchases/invoices`, `GET /purchases/invoices/echeances`). Permissions : `PURCHASE_{CREATE,READ}` + `PAYMENT_{CREATE,READ}`. 11 DTOs + 5 `<X>Create` records (regroupement params >3 selon règle 30). Records helpers : `FactureAchatCreate`, `LigneCommandeCreate`, `PaiementAchatCreate`. Helper transverse `ReferenceHelper.generate(base)` dans `common/tools/`. 28 tests, 384 / 384 verts.

2. **Module Dépense complet — CRUD CategoryDepense + CRUD Depense** — deux ressources REST distinctes :
   - **`CategoryDepense`** scopé entreprise. Migration V10 : ajout FK `entreprise_id` NOT NULL + unicité `(entreprise_id, nom)` (au lieu de l'unicité globale auto-générée par Hibernate, drop via DO $$ dynamic SQL). DTO `CategoryDepenseRequest(nom, description, actif?)`, response avec id+nom+description+actif. Controller `/api/v1/expense-categories` avec `EXPENSE_CATEGORY_{CREATE,READ,UPDATE,DELETE}`.
   - **`Depense`** scopé magasin. Champs : `magasin`, `category`, `libelle`, `description`, `dateDepense`, `montant`, `modePaiement (MoyenPaiement)`. Migration V10 : ajout colonne `mode_paiement` NOT NULL + `DROP COLUMN date_echeance` (décision suite à itération avec l'utilisateur : "dateEcheance n'est pas utile pour depense"). DTOs `DepenseRequest` (validations Jakarta) + `DepenseFilter` (record validé par `ValidatorService` selon règle 30 — magasinId, categoryId, modePaiement, startDate, endDate, page, size). Controller `/api/v1/depenses` avec `EXPENSE_{CREATE,READ,UPDATE,DELETE}` et endpoints `/total` (somme agrégée filtre) + listing paginé. `DepenseTotalResponse(magasinId, montantTotal, nombreDepenses)`. 15 tests, **392 / 392 verts**.

3. **Helper `ReferenceHelper`** dans `common/tools/` — `generate(String base) → "{base}-yyyyMMdd-HHmmssSSS"`. Réutilisable pour toutes les références métier (commande achat, future facture vente, etc.).

4. **Refactor itératif sur l'API `DepenseRequest`** :
   - V1 : `String magasinId` + validateur `@Uuid` côté DTO.
   - V2 : `UUID magasinId` parsé au controller, `@Valid` standard.
   - V3 (finale) : `UUID magasinId` typé directement dans le record DTO (parsing Jackson natif + `@NotNull`). Itération avec l'utilisateur ("le dto aussi doit avoir UUID magasinId").

**Décisions / arbitrages :**

- **Achat = transaction atomique commande+facture+stock** — pas de réception partielle, pas de workflow multi-étapes. La manager renseigne tout d'un coup quand le livreur passe (use case réel décrit par l'utilisateur). Si paiement partiel : champ `montantAccompte` initial sur la facture (calculé, pas saisi — somme des `PaiementAchat` créés). `montantFacture` calculé à partir des lignes.
- **Génération auto de référence commande** — pas saisie par l'utilisateur. Pattern `CMD-yyyyMMdd-HHmmssSSS` lisible humainement et unique en pratique. À étendre via `ReferenceHelper` aux autres modules.
- **`<X>Create` records pour params >3** — quand une méthode privée orchestre la création d'une entité composée, regrouper les params dans un record `<X>Create` (immutable). Ex : `FactureAchatCreate(numero, date, dateEcheance)`, `LigneCommandeCreate(productFournisseur, quantite, prixUnitaire)`. Convention : records suffixés `Create` pour params de méthodes domain (vs `Request` pour DTOs HTTP).
- **`CategoryDepense` scopée par entreprise** (pas par magasin) — une catégorie de dépense (Loyer, Salaires, Fournitures) est un référentiel partagé entre tous les magasins du tenant. Migration V10 ajoute FK Entreprise NOT NULL + unicité (entreprise_id, nom).
- **`Depense` scopée par magasin** — une dépense est concrète, rattachée à un magasin précis (le manager du magasin enregistre les dépenses opérationnelles). FK Magasin sur `Depense`.
- **`dateEcheance` retiré de `Depense`** — décision métier de l'utilisateur en fin de session. Une dépense est ponctuelle (payée à date `dateDepense`), elle n'a pas de date d'échéance comme une facture fournisseur. Migration V10 : `DROP COLUMN IF EXISTS date_echeance`. Tous les artefacts associés supprimés (`DepenseEcheanceFilter`, `findEcheances`, endpoint `/echeances`).
- **`DROP CONSTRAINT` Hibernate auto-named** — pour supprimer l'unicité globale `nom` sur `category_depense` créée auto par Hibernate (nom non déterministe), utilisé bloc PostgreSQL `DO $$ DECLARE cname TEXT; BEGIN SELECT conname INTO cname FROM pg_constraint ... ; EXECUTE 'ALTER TABLE ... DROP CONSTRAINT ' || cname; END $$;`. Patron à réutiliser pour migrations futures qui touchent des contraintes auto-générées.
- **Inventaire physique reporté** — déjà décidé en session précédente : on bouclera après le module Vente (un inventaire opérationnel s'appuie sur les ventes/sorties enregistrées). Module Stock à **11/12** fonctionnalités, à finaliser plus tard.

**Où on s'est arrêté :**

- **Tests : 384 → 392** (+15 dépense ; +28 cumulés sur la journée incluant achat).
- **3 commits poussés sur `origin/dev`** dans cette session :
  - `16184b5` — Module Achat complet (F12-F14)
  - (commit consolidé) — Module Dépense complet
  - `45fe501` — Module Dépense complet — CRUD CategoryDepense + Depense avec scoping entreprise/magasin
- **Migration Flyway V10** : `add_entreprise_to_category_depense_and_mode_paiement.sql` — FK entreprise sur category_depense + unicité (entreprise_id, nom) + mode_paiement NOT NULL sur depense + drop date_echeance.
- **`ReferenceHelper`** ajouté dans `common/tools/`. Pattern `<X>Create` records officialisé.

**Prochaine étape recommandée :**

1. **Module Vente** — cycle complet : panier → `CommandeVente` → `LigneCommandeVente` → consomme `ISortieStockService.create(...)` (FIFO interne déjà livré) → `Paiement` → `Facture`. Prérequis vendeur terrain. Symétrie naturelle avec l'achat qui vient d'être livré.
2. **Recherche produit pour le vendeur** (`GET /products/search?q=...` par nom/référence/code-barres) — utile pour préparer la vente, simple et rapide.
3. **Inventaire physique** (fonct. 12 stock, reportée) — à reprendre une fois Vente livré.
4. **Reporting cross-module** — synthèse dépenses + ventes + marges, dashboard manager.

---

### Session du 2026-05-14 (matin — Module Stock)
**Sujet :** Module Stock complété de la fonctionnalité 3 à 11 (9 livraisons) — entrée stock manuelle, consultation stock, consultation mouvements, sortie FIFO (service interne), ajustement manuel (positif/négatif avec motifs), seuils d'approvisionnement, valorisation, reporting marges, listing lots expirants. Adoption du pattern **record `<X>Filter` validé par `ValidatorService`** comme convention (règle 30), création des helpers transverses `common/tools/` (`DateHelper`, `EnumHelper`, `UuidHelper`), séparation des conventions de codage en `CONVENTION_CODAGE_BACKEND.md` + `CONVENTION_CODAGE_FRONTEND.md`. 7 nouveaux records Filter (StockFilter, MouvementStockFilter, MouvementJournalize, ExpiringLotsFilter, MarginReportFilter, etc.), 4 DTOs Response date passés en `String` formatés via `DateHelper.format()` pour cohérence frontend.

**Ce qui a été fait :**

1. **Fonctionnalité 3 — Entrée stock manuelle** (`POST /api/v1/stocks/entries`) — création `EntreeStock` + upsert `Stock` (moyenne pondérée prix d'achat) + journalise `MouvementStock(ENTREE_ACHAT)`. Migration V9 (drop `Stock.productFournisseur` + UNIQUE `(magasin_id, produit_id)`). Sémantique `Stock` clarifiée : 1 ligne par paire (magasin, produit) ; le fournisseur reste sur `EntreeStock` pour préserver la traçabilité multi-fournisseur (validation décision Option A + A2 par l'utilisateur).

2. **Fonctionnalité 4 — Consultation stock** (`GET /api/v1/stocks?...` + `GET /api/v1/stocks/{id}`) — paginé avec filtres optionnels. Premier usage du pattern **record `StockFilter`** validé par `ValidatorService.validate(filter)` côté service métier. Query JPQL via SpEL `:#{#filter.X}`.

3. **Fonctionnalité 5 — Consultation mouvements** (`GET /api/v1/stock-movements?...`) — listing du journal avec filtres riches (stockId, magasinId, productId, type, période). Sous-DTO `MouvementDetailResponse` extrait. Création des helpers `common/tools/` (`DateHelper`, `EnumHelper`, `UuidHelper`).

4. **Réorganisation des docs (.claude/)** — `ARCHITECTURE.md` réduit à la stack et structure, conventions séparées en `CONVENTION_CODAGE_BACKEND.md` (31 règles, incluant la nouvelle règle 30 "max 3 paramètres" et règle 31 "indentation + documentation multi-process") et `CONVENTION_CODAGE_FRONTEND.md`. `CLAUDE.md` mis à jour (6 → 7 fichiers).

5. **Fonctionnalité 6 — Sortie stock FIFO (service interne)** — `ISortieStockService.create(...)` consomme les `EntreeStock` du plus ancien au plus récent (FIFO pur par `createdAt ASC`), crée une `SortieStock` par lot consommé avec marge pré-calculée, décrémente `Stock`, journalise `MouvementStock(SORTIE_VENTE)`. **Pas d'endpoint REST exposé** : sera consommé par le futur module Vente (`POST /sales`). Permission `STOCK_EXIT` réservée pour usage interne futur.

6. **Fonctionnalité 7 — Ajustement stock manuel** (`POST /api/v1/stocks/adjustments`) — types `POSITIF` (mini entrée stock, fournisseur obligatoire) / `NEGATIF` (consomme FIFO sans créer de `SortieStock`). Motifs typés via enum (`RETROUVAILLE`, `PERTE`, `CASSE`, `VOL`, `ERREUR_INVENTAIRE`, `AUTRE`). Validation cohérence motif/type. Décision métier : **MANAGER conserve `STOCK_ADJUSTMENT`** (responsable opérationnel du magasin, fraude tracée a posteriori via `MouvementStock.createdBy` + audit listing).

7. **Fonctionnalités 8, 9, 10, 11 livrées dans un commit groupé :**
   - **8 — Seuils d'approvisionnement** : `PATCH /api/v1/stocks/{id}/threshold` + `GET /api/v1/stocks/below-threshold?magasinId=`
   - **9 — Valorisation stock** : `GET /api/v1/stocks/valuation?magasinId=` retourne `valeurTotale = SUM(qty × prixAchatMoyen)`
   - **10 — Reporting marges** : `GET /api/v1/reports/margins?...` agrège sur `SortieStock` JOIN `EntreeStock` avec filtres produit/fournisseur/période. Service dédié `IMarginReportService` + controller `MarginReportController`.
   - **11 — Lots expirants** : `GET /api/v1/stocks/expiring-lots?magasinId=&daysAhead=N` retourne les `EntreeStock` avec `dateExpiration <= today+N` triés `ASC`. Service dédié `IExpiringLotsService`.

**Décisions / arbitrages :**

- **`Stock` unique par (magasin, produit)** — décision Option A (sémantique ERP classique). Le champ `Stock.productFournisseur` supprimé via migration V9. La traçabilité fournisseur passe par `EntreeStock.productFournisseur`. **Prix moyen pondéré (`Stock.prixAchatMoyen`) sert au reporting/affichage, JAMAIS pour le calcul de marge à la vente** : c'est le `prixAchat` du lot consommé (snapshot dans `SortieStock`) qui détermine la marge réelle.
- **Pattern record `<X>Filter` validé par `ValidatorService`** systématisé (règle 30) : services métier prennent un filter en paramètre unique, validation Jakarta intégrée (`@NotNull`, `@Min`, `@DatePattern`, `@EnumValue`, `@Uuid`), méthodes utilitaires sur le record (`toPageable()`, `typeAsEnum()`, `fromDateTime()`/`toDateTime()` via helpers). **Exemption explicite des repositories Spring Data** : queries `@Query` gardent leurs params individuels via SpEL `:#{#filter.X}` (queries JPQL plus lisibles avec params nommés).
- **Helpers transverses** dans `org.store.common.tools` (`DateHelper`, `EnumHelper`, `UuidHelper`) — stateless, réutilisables, format date par défaut `yyyy-MM-dd HH:mm:ss` (LocalDateTime) et `yyyy-MM-dd` (LocalDate). DTOs Response exposent les dates en `String` formatés (pas de timezone ambiguë côté client).
- **Règle "indentation + documentation"** posée (règle 31) : toute méthode multi-process doit être indentée par blocs (lignes vides entre étapes logiques) et documentée (javadoc concise). Pas de commentaires inline.
- **Sortie FIFO sans endpoint REST exposé** — la primitive technique est livrée comme service interne pour qu'elle soit consommée par le futur module Vente (1 vente = N appels au service de sortie). Évite le risque de fraude (vendre sans passer par la caisse + ajuster ensuite).
- **Permission `STOCK_ADJUSTMENT` accordée à MANAGER** assumée — choix métier sauvegardé en project memory (`project_stock_adjustment_permission.md`). La fraude potentielle est tracée via `MouvementStock.createdBy` (audit a posteriori) plutôt que bloquée en amont.
- **Date helpers** : aller-retour entre `String` (API), `LocalDate` (parsing intermédiaire) et `LocalDateTime` (BD). Garde `LocalDateTime` pour les comparaisons `>=`/`<=` sur `m.createdAt` (sinon Hibernate cast `LocalDate` en `startOfDay`, exclut la journée).
- **Décision déléguée à l'utilisateur** sur la génération auto du numéro de lot : ✗ pas de génération (option A retenue — `numeroLot` reste saisi librement, optionnel).

**Où on s'est arrêté :**

- **356 tests verts** (300 → 356, +56 dans la session).
- **7 commits poussés sur `origin/dev`** :
  - `b74151e` — Entrée stock manuelle (fonct. 3)
  - `7a9a9ee` — Consultation stock (fonct. 4)
  - `d4d12d2` — Consultation mouvements + refactor filters/helpers (fonct. 5)
  - `a5419ca` — Réorganisation conventions backend/frontend
  - `e23045a` — Sortie stock FIFO service interne (fonct. 6)
  - `270e7ef` — Ajustement stock manuel (fonct. 7)
  - `47de2a6` — Seuils + valorisation + reporting marges + lots expirants (fonct. 8-11)
- **Migration Flyway** : V9 (refactor Stock — drop productFournisseur + UNIQUE magasin/produit).
- **Module Stock à 11/12 fonctionnalités livrées.** Reste : inventaire physique (fonct. 12).
- **Score axes métier exigés** : 9/10 (Entrées/Sorties/Ajustements ✓, Traçabilité ✓, Fournisseurs ✓, Mouvements ✓, Seuils ✓, Valorisation ✓, Lots/Expirations ✓, FIFO ✓, Marges ✓, Inventaire ❌). LIFO non implémenté, non demandé.
- **3 nouvelles règles de codage** inscrites dans `CONVENTION_CODAGE_BACKEND.md` : règle 30 (max 3 paramètres → records), règle 31 (indentation + doc multi-process). Helpers transverses `common/tools/` documentés.
- **Nouvelles mémoires créées** : `feedback_max_3_parametres`, `feedback_indentation_documentation`, `project_stock_adjustment_permission`.

**Prochaine étape recommandée :**

1. **Fonctionnalité 12 — Inventaire physique** : endpoint pour comptage physique d'un magasin, écarts théorique vs réel, génération automatique d'`Ajustement` (positif si trouvé en plus, négatif si manquant) pour chaque ligne en écart. Boucle l'axe "Inventaires" du cahier des charges et termine le module Stock.
2. Ou bascule vers le **module Vente** (cycle complet : panier → CommandeVente → LigneCommandeVente → appel interne FIFO `ISortieStockService.create(...)` → paiement → facture), prérequis vendeur sur le terrain.
3. Ou **Recherche produit** (`GET /products/search?q=...` par nom/référence/code-barres) — utile au vendeur pour préparer la vente, simple et rapide.
4. Reporting **marges groupées par produit / fournisseur / employé** (extension de la fonct. 10) si besoin de dashboards.

---

### Session du 2026-05-13 (soirée)
**Sujet :** Listing galerie `GET /products/{id}/images` + simplification `ProductResponse` (URL `image` au lieu d'`imagePrincipalId`, sous-DTOs Summary pour category/quality), démarrage du **module Stock** par les fonctionnalités fondations 1 et 2 : CRUD Fournisseur (scopé entreprise) et CRUD ProductFournisseur (lien produit ↔ fournisseur avec prix d'achat, référence fournisseur et origine — base de la traçabilité multi-fournisseur)

**Ce qui a été fait :**

1. **Listing galerie produit** — `GET /api/v1/products/{id}/images` (permission `PRODUCT_READ`) retourne `List<ImageMetadataResponse{id, date, contentType, url}>` où `url` est le path relatif `/api/v1/products/{productId}/images/{imageId}` directement utilisable par le frontend dans `<img src={img.url}>`. DTO `ImageMetadataResponse` créé dans `produit/application/dto` (path produit dans l'URL → spécifique au module). `IProductService.listImages(UUID)` scopé via `ensureBelongsToCurrentEntreprise`.

2. **Simplification `ProductResponse`** — 3 itérations successives :
   - V1 (rejetée) : ajout endpoint séparé `GET /products/{id}/image-info` avec factory `ImageMetadataResponse.forPrincipal(...)`.
   - V2 (proposée par l'utilisateur) : ajout direct du champ `image` (URL relative) dans `ProductResponse`, suppression de `imagePrincipalId`. Plus naturel, pas d'endpoint séparé.
   - V3 (itération) : remplacement de `CategoryProductResponse category` + `QualityResponse quality` (complets, 4 champs chacun) par juste `String category` + `String quality` (libellés) — testée puis rollback car le frontend a besoin des ids pour l'édition (`<select>`).
   - V4 (forme finale) : **`CategoryProductSummaryResponse(id, libelle)`** et **`QualitySummaryResponse(id, libelle)`** créés dans `produit/application/dto/`. `ProductResponse.category`/`quality` exposent juste id+libelle (pas les `description`/`entrepriseId` qui polluent). Le frontend a tout pour afficher + éditer.

3. **Lancement du module Stock** — analyse de la spec utilisateur (8 modèles d'entités, multi-fournisseur, FIFO, lots, dates d'expiration, valorisation, inventaire). Toutes les entités domain existent déjà dans `org.store.{achat, inventaire, produit, stock}.domain.*` (entités + ports + adapters + domain services vides). Il manque tout ce qui est couche application + presentation + tests. Établissement d'une feuille de route en **12 fonctionnalités** (TODO.md). Traitement itératif "1 par 1".

4. **Fonctionnalité 1 : CRUD Fournisseur** — FK `@ManyToOne Entreprise` ajoutée sur `Fournisseur` (héritage `Person` inchangé). Migration Flyway **V7** (NOT NULL direct, table vide). Enum `PermissionCode` enrichi de `SUPPLIER_{CRUD}` (4 nouvelles valeurs, total enum = 20). YAML aligné (ADMIN/PROPRIETAIRE/MANAGER = CRUD, VENDEUR = READ seul). DTOs `FournisseurRequest`(validations `@Email`/`@Phone`) + `FournisseurResponse(Fournisseur)` exposant raison sociale + contact + traçabilité. `FournisseurDomainService` enrichi (projection JPQL scopée + queries unicité reference). `IFournisseurService` + impl : scoping `ICurrentUserService`, unicité `reference` par entreprise (skippée si null/blank pour permettre les fournisseurs sans code interne). Controller `/api/v1/suppliers`. 13 tests service + 7 controller.

5. **Fonctionnalité 2 : CRUD ProductFournisseur** — entité enrichie de `referenceFournisseur` + `origine` (selon spec), `product_id`/`fournisseur_id` promus en NOT NULL. Migration Flyway **V8** (3 ALTER + 2 index). Sous-DTOs **`ProductSummaryResponse(id, nom, reference)`** dans `produit/application/dto` et **`FournisseurSummaryResponse(id, nom)`** dans `achat/application/dto`. DTOs `ProductFournisseurRequest(productId, fournisseurId, prixAchat, refFournisseur?, origine?)` avec validations + `ProductFournisseurResponse(ProductFournisseur)` avec sous-DTOs imbriqués. `ProductFournisseurDomainService` enrichi : projection JPQL avec scoping via `product.entreprise.id`, query par produit, unicité paire. `IProductFournisseurService` + impl `@Transactional(readOnly=true)` : scoping cross-entity (délégation à `IProductService.ensureBelongsToCurrentEntreprise` + `IFournisseurService.ensureBelongsToCurrentEntreprise`), unicité `(product, fournisseur)`, update limité aux champs informationnels (prix/refFournisseur/origine — FK product/fournisseur immuables). Controller `/api/v1/product-suppliers` avec filtre optionnel `?productId=`. **Permissions réutilisées `SUPPLIER_*`** (pas de nouvelles permissions — qui gère les fournisseurs gère leurs tarifs). 13 tests service + 8 controller.

**Décisions / arbitrages :**

- **`ProductResponse.image` (URL) plutôt qu'endpoint séparé `/image-info`** : choix utilisateur — éviter une 2e requête pour récupérer l'URL d'une image qui n'apparaîtra que dans le contexte du produit affiché. Une seule donnée fournie naturellement avec le `ProductResponse`.
- **Sous-DTOs Summary (id + libellé seuls) plutôt que sous-DTOs complets** : itération avec l'utilisateur. Les sous-DTOs CRUD complets (`CategoryProductResponse`, `QualityResponse`) ont 4 champs (id, libelle, description, entrepriseId) — trop pour un payload Product. Les Summary exposent seulement ce dont le frontend a besoin (affichage par libellé, édition par id). Pattern à reproduire pour les futurs sous-DTOs.
- **`ProductSummaryResponse(id, nom, reference)` exposé avec 3 champs** : reference du produit ajoutée car potentiellement utile (catalogue, scan code barre futur), pas que id + libellé.
- **Spec Fournisseur vs réalité** : la spec utilisateur listait `private String entreprise` (raison sociale fournisseur) + `private String email` + `@ManyToOne Magasin magasin`. En réalité : `Fournisseur extends Person` apporte déjà nom (raison sociale), email, telephone, adresse. Pas besoin d'ajouter ces champs. Le scoping `Magasin` proposé dans la spec → écarté au profit de `Entreprise` (un fournisseur sert tous les magasins du tenant, plus naturel).
- **Permissions `SUPPLIER_*` nouvelles (pas `PURCHASE_*`)** : permission granulaire spécifique à la gestion fournisseur ; `PURCHASE_*` sera réservé aux transactions (commandes/factures).
- **`SUPPLIER_*` réutilisées pour `ProductFournisseur`** : pas de nouvelle permission `PRODUCT_SUPPLIER_*` créée. Qui gère les fournisseurs gère leurs tarifs. Évite la prolifération de permissions. À séparer si un jour besoin d'accès différencié (ex : tarification réservée au comptable).
- **`reference` Fournisseur skippable** : la convention `ensureReferenceAvailable` skip si `null` ou `blank`. Permet de créer des fournisseurs "informels" sans code interne, tout en gardant l'unicité quand un code est saisi.
- **`product` et `fournisseur` immuables après création** sur `ProductFournisseur` : update modifie seulement prix/refFournisseur/origine. Pour changer la paire, on supprime + recrée. Plus simple, plus traçable, évite les comportements bizarres en cas de modification cross-fournisseur.
- **`ImageMetadataResponse` dans `produit/application/dto` (pas `common/dto`)** : l'URL hardcode le path `/api/v1/products/...`, donc le DTO devient spécifique au module produit. `ImageDownloadResponse` reste dans `common/dto` (générique pour tout binaire).
- **Feuille de route stock en 12 phases** : permet d'avoir une vision claire de bout en bout (achat → stock → vente → inventaire → reporting) tout en livrant en mode incrémental. Phase actuelle : 2/12.

**Où on s'est arrêté :**

- **300 tests verts** (255 → 300, +45 cette session : 4 listing galerie + 20 Fournisseur + 21 ProductFournisseur).
- **4 commits poussés sur `origin/dev`** :
  - `3184aa0` "Listing galerie + URL image principale dans ProductResponse"
  - `beb16b8` "CRUD Fournisseur (module stock - fonctionnalité 1)"
  - `cd1988f` "CRUD ProductFournisseur (module stock - fonctionnalité 2)"
- **Migrations Flyway pendantes** au prochain démarrage : V7 (FK entreprise sur fournisseur), V8 (referenceFournisseur + origine + NOT NULL sur product_fournisseur).
- **Enum `PermissionCode`** : 20 valeurs (4 legacy + 12 produit/qualité/catégorie + 4 supplier).
- **Permissions YAML** : 99 valeurs.
- **Sous-DTOs Summary disponibles** : `CategoryProductSummaryResponse`, `QualitySummaryResponse`, `ProductSummaryResponse`, `FournisseurSummaryResponse`.
- **Phases 1-2 du module Stock livrées**. Phases 3-12 restent à faire.

**Prochaine étape recommandée :**

1. **Fonctionnalité 3 : Entrée stock manuelle** — `POST /api/v1/stocks/entries`. Cœur du module stock. Crée une `EntreeStock` (lot FIFO avec qty initiale + restante = qty, prix d'achat, numéro de lot optionnel, date d'expiration optionnelle, lien optionnel vers `productFournisseur`) + upsert `Stock` (incrémente `quantiteDisponible`, recalcule `prixAchatMoyen` pondéré) + journalise `MouvementStock(type=ENTREE_ACHAT)` avec `stockAvant`/`stockApres`. Permission `STOCK_ENTRY`. Réutilise les `SUPPLIER_*` patterns pour scoping.
2. **Fonctionnalité 4 : Consultation stock** — `GET /stocks?magasinId=&productId=`, `GET /stocks/{id}`. Read-only sur l'état courant.
3. **Fonctionnalité 5 : Consultation mouvements** — `GET /stocks/movements?...` (journal immuable).
4. **Fonctionnalité 6 : Sortie stock FIFO** — `POST /stocks/exits`, consomme les `EntreeStock` du plus ancien au plus récent.
5. **Migration progressive des `@PreAuthorize` legacy** vers les permissions granulaires — reportée mais à planifier.

---

### Session du 2026-05-13 (suite — après-midi/soir)
**Sujet :** CRUD CategoryProduct + Quality + Product (scopés par entreprise, permissions granulaires), `Product.imagePrincipal` + galerie `images`, service `IUploadFileService` (commun, valide MIME images, externalise config via `UploadProperties`), endpoints upload/visualisation/suppression image principale et galerie, bascule de la détection magic bytes vers un champ `contentType` stocké sur `PieceJointe`

**Ce qui a été fait :**

1. **Pré-requis transverses** :
   - FK `@ManyToOne Entreprise entreprise` ajoutée sur `CategoryProduct` et `Quality` (modèle modifié avec validation explicite de l'utilisateur). Migration Flyway **V4** (`add_entreprise_to_category_quality.sql`, NOT NULL + FK + index).
   - Enum `PermissionCode` enrichi de **12 valeurs granulaires** : `CATEGORY_PRODUCT_{CRUD}`, `QUALITY_{CRUD}`, `PRODUCT_{CRUD}` (passe de 4 à 16 valeurs).
   - `roles-permissions.yml` aligné : ajout `CATEGORY_PRODUCT_*` + `QUALITY_*` (ADMIN/PROPRIETAIRE/MANAGER = CRUD, VENDEUR = READ seul). Permissions `PRODUCT_*` déjà présentes.
   - i18n FR/EN : 9 clés (`categoryProduct.{notFound,libelle.alreadyExists,notOwned}`, `quality.*`, `product.{notFound,reference.alreadyExists,notOwned}`).

2. **CRUD CategoryProduct** (référentiel scopé entreprise) — DTOs `CategoryProductRequest` + `CategoryProductResponse(CategoryProduct)`, `CategoryProductDomainService` enrichi (`create`, queries scopées + unicité libellé), `ICategoryProductService` + impl scoping via `ICurrentUserService` (unicité libellé skippée si inchangée en update), controller `/api/v1/category-products` (POST/GET paginé/GET id/PUT/DELETE) avec `@PreAuthorize` granulaires. 14 tests service + 6 controller.

3. **CRUD Quality** — calqué sur CategoryProduct. Mêmes structures, mêmes permissions adaptées. 14 + 6 tests.

4. **CRUD Product** — DTOs avec **sous-DTOs imbriqués** `CategoryProductResponse` + `QualityResponse` (règle 23), `ProductDomainService.create(request, category, quality, entreprise)`, `IProductService` + impl `@Transactional(readOnly=true)` au niveau classe. **Cohérence cross-tenant** : vérif que catégorie & qualité utilisées appartiennent bien à l'entreprise du caller (via `ensureBelongsToCurrentEntreprise` cross-services). Unicité `reference` par entreprise, skippée si inchangée. Controller `/api/v1/products` avec `@PreAuthorize('PRODUCT_*')`. 15 + 7 tests.

5. **`Product.imagePrincipal`** — relation `@OneToOne(fetch=LAZY, cascade=ALL, orphanRemoval=true) PieceJointe`, **indépendante** de la galerie `images` (choix explicite). Migration Flyway **V5** (`add_image_principal_to_product.sql`). `ProductResponse` expose `imagePrincipalId` (id seul, pas le blob). `ProductRequest` inchangé (upload via endpoint dédié).

6. **`IUploadFileService` + `UploadFileServiceImpl`** (`common/service/`) — service technique réutilisable. Une seule méthode au début : `buildImage(MultipartFile) → PieceJointe` (validation non-vide + MIME `image/{jpeg,png,webp,gif}`, lit les bytes, `date=now`). Wrap `IOException` en `BadArgumentException`.

7. **Config multipart** : `application.yml` enrichi `spring.servlet.multipart.{enabled, max-file-size=5MB, max-request-size=6MB}`.

8. **Endpoints upload/delete image principale** :
   - `PUT /api/v1/products/{id}/image` (multipart `@RequestPart("file")`), `@PreAuthorize('PRODUCT_UPLOAD_IMAGE')` → 200 OK + `ProductResponse`.
   - `DELETE /api/v1/products/{id}/image` → 204. orphanRemoval purge la `PieceJointe`.
   - `ProductDomainService.setImagePrincipal(Product, PieceJointe)`.

9. **Upload multiple — galerie `Product.images`** :
   - `IUploadFileService.buildImages(List<MultipartFile>) → List<PieceJointe>` (réutilise `buildImage` en boucle, stop au 1er invalide).
   - `ProductDomainService.addImages(Product, List<PieceJointe>) → Product` (append cumulatif).
   - `IProductService.uploadImages(UUID, List<MultipartFile>) → List<UUID>` (retourne les ids des images créées pour affichage immédiat côté client).
   - `POST /api/v1/products/{id}/images` (multipart `@RequestPart("files")`), `@PreAuthorize('PRODUCT_UPLOAD_IMAGE')` → 201 + `List<UUID>`.

10. **Externalisation config types MIME** — record `UploadProperties` (`@ConfigurationProperties("upload")`, **aplati** après itération : initialement avec sous-record `Image`, puis nested, puis remis en fichier séparé, finalement aplati en `UploadProperties(Set<String> allowedImageTypes)` avec **constructeur compact** qui normalise (toLowerCase) + rend immutable. Évite la duplication de logique côté service. `application.yml` : bloc `upload.allowed-image-types`.

11. **Visualisation et suppression d'image** :
   - DTO `ImageDownloadResponse(byte[] content, String contentType)` dans `common/dto`.
   - `ProductDomainService.findImageInProduct(Product, UUID) → Optional<PieceJointe>` + `removeImage(Product, PieceJointe)` (orphanRemoval purge).
   - `IProductService.getImagePrincipal(UUID)` / `getImage(productId, imageId)` / `deleteImage(productId, imageId)`.
   - Endpoints `GET /products/{id}/image` (`PRODUCT_READ`), `GET /products/{id}/images/{imageId}` (`PRODUCT_READ`), `DELETE /products/{id}/images/{imageId}` (`PRODUCT_UPLOAD_IMAGE`).
   - Première implémentation via **détection magic bytes** dans `IUploadFileService.detectImageContentType(byte[])` : JPEG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`, GIF `GIF8`, WebP `RIFF...WEBP`, fallback `application/octet-stream`. Robuste, pas de modif modèle, mais limité aux 4 formats codés.

12. **Bascule vers `contentType` stocké sur `PieceJointe`** (après réflexion) — ajout `@Column(name="content_type", nullable=false, length=100) String contentType` sur `PieceJointe` + migration Flyway **V6** (table vide → NOT NULL direct). `UploadFileServiceImpl.buildImage` stocke `file.getContentType()` (lowercased) à l'upload. Lecture directe via `pieceJointe.getContentType()`. **Suppression** de `detectImageContentType` (+30 lignes) et de ses 5 tests. Extensible à tout MIME (PDF/factures/docs) sans modif code.

13. **Tests** : suite portée de 162 → **255 verts** (+93 sur la session). Structure :
    - `CategoryProductServiceImplTest` (14) + `CategoryProductControllerTest` (6)
    - `QualityServiceImplTest` (14) + `QualityControllerTest` (6)
    - `ProductServiceImplTest` (24, dont upload image principale + galerie + visualisation + suppression) + `ProductControllerTest` (12)
    - `UploadFileServiceImplTest` (9, après suppression des 5 magic bytes)

14. **Convention "ne pas modifier les entités sans validation"** — formalisée en mémoire user après l'incident initial où j'avais proposé d'ajouter une FK Entreprise sur CategoryProduct/Quality sans demander. Désormais toute modif d'entité JPA passe par une demande explicite. Voir `feedback_ne_pas_modifier_entites.md`.

15. **Convention commits projet** — formalisée en mémoire user : Conventional Commits FR informel, **jamais de `Co-Authored-By: Claude`**, pas de push automatique, `git add` ciblé. Voir `feedback_commit_style.md`.

**Décisions / arbitrages :**

- **FK Entreprise sur CategoryProduct/Quality** : décidée après que l'utilisateur a clarifié "chaque entreprise a ses catégories/qualités". Initialement il avait refusé, puis confirmé que sans FK on ne pouvait pas vraiment scoper → accepté la modif modèle. Règle "demander avant de modifier" appliquée.
- **Permissions granulaires (vs legacy)** : ajout des 12 nouvelles permissions à l'enum + YAML dès le départ pour les nouveaux endpoints. Les permissions legacy (`PROPRIETAIRE_ACCESS` etc.) restent inchangées pour ne pas casser les contrôleurs existants. Migration progressive reportée.
- **Référentiel scopé par entreprise, pas par magasin** : un produit appartient à une entreprise (pas à un magasin spécifique). Cohérent avec le modèle (`Product.entreprise` était déjà là). Le stock par magasin sera géré par le module `stock` (FIFO).
- **`Product.imagePrincipal` indépendante de `images`** : choix utilisateur — pas de contrainte "imagePrincipal doit être dans images". Plus simple, moins de couplage.
- **Galerie non exposée dans `ProductResponse`** : choix utilisateur — `ProductResponse` reste minimal, la galerie sera exposée via un endpoint dédié `GET /{id}/images` plus tard (non livré dans cette session).
- **POST cumulatif sur galerie (vs PUT remplaçant)** : choix utilisateur. Le client doit utiliser DELETE pour retirer une image avant d'en ajouter une nouvelle s'il veut remplacer. Plus naturel pour une galerie.
- **Format de retour `POST /images`** : `List<UUID>` plutôt que `ProductResponse` complet. Utile au client pour afficher immédiatement les nouvelles images sans rerequérir.
- **`UploadProperties` aplati (vs nested record `Image`)** : itéré 3 fois (nested → record séparé `Image.java` → tentative `@Component` rejetée car casserait Spring → aplati). Forme finale : `UploadProperties(Set<String> allowedImageTypes)` avec constructeur compact qui normalise. Pas besoin de sous-record pour un seul champ.
- **Magic bytes → contentType stocké** : magic bytes solide pour le périmètre images, mais le champ stocké :
  - Extensible à tout MIME sans modif code (PDF, docs, factures à venir).
  - Source de vérité unique (déclaration client à l'upload).
  - Code service plus court, plus simple.
  - Trade-off : si client envoie un mauvais Content-Type, on stocke le mensonge. Acceptable car validation du MIME contre `upload.allowed-image-types` est faite à l'upload.
- **`PieceJointe.contentType NOT NULL` direct** (vs nullable + backfill) : tables vides en local, pas de données existantes à migrer. Si déploiement prod un jour avec données, faudra revoir.
- **Multi-validations au lieu d'une seule** : pour upload, validation MIME se fait à l'upload (`UploadFileServiceImpl`) et le contentType résultant est stocké. On ne re-valide pas à la lecture (gain perf vs robustesse).

**Où on s'est arrêté :**

- **255 tests verts** (162 → 255, +93 cette session).
- **2 commits poussés sur `origin/dev`** :
  - `d008da0` "CRUD Catégorie/Qualité/Produit + upload image principale et galerie" (42 fichiers, +2829)
  - `8bacfe4` "Visualisation/suppression image produit + contentType stocké sur PieceJointe" (14 fichiers, +272)
- **Migrations Flyway pendantes** au prochain démarrage : V4 (FK entreprise sur category_product/quality), V5 (image_principal_id sur product), V6 (content_type sur piece_jointe).
- **Permissions YAML** : 95 valeurs au total. `CATEGORY_PRODUCT_*` + `QUALITY_*` à synchroniser en BD au prochain boot avec `RBAC_SYNC=true`.
- **Enum `PermissionCode`** : 16 valeurs (4 legacy + 12 granulaires).
- **Galerie `Product.images`** : upload (POST), suppression individuelle (DELETE), visualisation (GET) câblés. Pas d'endpoint de listing (GET `/{id}/images` retournant `List<ImageMetadata>`) — reporté.

**Prochaine étape recommandée :**

1. **Endpoint listing galerie** — `GET /api/v1/products/{id}/images` qui retourne `List<ImageMetadata{id, date, contentType}>` pour qu'un client puisse récupérer la liste avant de demander chaque blob.
2. **Migration progressive des `@PreAuthorize` legacy** — remplacer `PROPRIETAIRE_ACCESS` / `EMPLOYE_ACCESS` par les permissions granulaires correspondantes (`COMPANY_READ/UPDATE`, `STORE_*`, `USER_*` etc.). L'enum `PermissionCode` doit recevoir les 70+ valeurs granulaires restantes.
3. **Module `stock`** — FIFO basé sur les produits créés (premier vrai use case du module stock). EntreeStock alimentée par CommandeAchat, SortieStock consommée par CommandeVente.
4. **Frontend** : `src/lib/api/client.ts` (axios + intercepteur JWT) + pages `(auth)/login` et `(auth)/register` pour rendre le flux d'inscription/connexion testable bout en bout.

---

### Session du 2026-05-13 (matinée)
**Sujet :** Externalisation RBAC (rôles + permissions) dans un YAML synchronisable, flag `security.rbac.sync`, i18n des exceptions, surcharge `LocalizedRuntimeException` avec cause, convention de documentation des services applicatifs (puis révision stricte), logs HTTP entrants/sortants + requestId MDC, GlobalException WARN/ERROR

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

13. **`HttpRequestLoggingFilter`** (`org/store/config/`, `@Component OncePerRequestFilter`) — trace chaque requête HTTP :
    - `→ METHOD path from ip` à l'entrée + `← status METHOD path in Xms | request: {...} | response: {...}` à la sortie (status code inclus).
    - `ContentCachingRequestWrapper(req, MAX_PAYLOAD_LENGTH*2)` + `ContentCachingResponseWrapper` pour relire les bodies sans empêcher controllers/clients de les consommer.
    - **Masquage automatique** des champs sensibles par regex JSON : `password`, `accessToken`, `refreshToken`, `secret`, `token` → `"***"`.
    - **Tronquage** bodies > 2 KB → `...[truncated]`. Skip binaire (`multipart/*`, `image/*`, `video/*`, `audio/*`, `octet-stream`) → `[binary]`.
    - **Skip paths** : `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`.
    - **`requestId` UUID dans MDC** posé au début, `MDC.clear()` en `finally` (anti-fuite entre requêtes sur thread recyclé).
    - **`X-Forwarded-For`** privilégié sur `remoteAddr` (utile derrière reverse proxy).
    - Pas branché via `addFilterBefore` dans `SecurityConfig` — Spring Security 7 refuse les références à des filtres hors chaîne Security. Le filtre `@Component` est auto-enregistré par Spring Boot dans la chaîne servlet avant `springSecurityFilterChain` — comportement voulu.

14. **`logging.pattern.console`** dans `application.yml` enrichi avec `[%X{requestId:-}]` → chaque ligne de log porte automatiquement le requestId, facilitant le tracing.

15. **`GlobalException` — log WARN/ERROR différencié** :
    - 4xx → `logger.warn("HTTP {} - {}", statusCode, message)` (sans stack — c'est le client qui a mal appelé).
    - 5xx → `logger.error("HTTP {} - {}", statusCode, message, cause)` (avec stack — c'est un bug serveur).
    - Helper `buildError(message, status, throwable)` surchargé. `MethodArgumentNotValidException` et `ConstraintViolationException` loguent aussi en WARN avec les champs en erreur.
    - Handlers 5xx (RestTemplate/NPE/Sse/Mail/catch-all `Exception`) passent l'exception au helper pour conserver la stack.

16. **`HttpRequestLoggingFilterTest`** — 6 cas : MDC posé pendant la chaîne + clear après, log entrée+sortie avec status/durée, masquage `password` dans request body, masquage `accessToken/refreshToken` dans response body, skip actuator/swagger/openapi, priorité `X-Forwarded-For`. Capture via `ListAppender<ILoggingEvent>` Logback.

17. **Révision règle 29** (convention doc services applicatifs) — d'abord posée comme "javadoc complète + commentaires de section + commentaires inline pour le **pourquoi**", puis **resserrée** à : **javadoc concise (1 phrase) sur classe + chaque méthode, AUCUN commentaire à l'intérieur du corps**. Justification : si le code nécessite un commentaire inline, c'est un signal de mauvais nommage ou de refactor manquant. Nettoyage en conséquence de `RolesPermissionsSyncServiceImpl` et `HttpRequestLoggingFilter`. Mémoire user `feedback_doc_service_applicatif.md` mise à jour.

**Décisions / arbitrages :**

- **Stratégie additive (vs strict / vs additif sans suppression assoc)** : choisie après comparaison. Raison : une mauvaise édition du YAML en prod ne doit jamais retirer silencieusement des droits à des utilisateurs. Le WARN signale l'écart, l'opérateur décide.
- **Format YAML — liste explicite de permissions par rôle (vs groupes/includes)** : verbosité acceptée pour la lisibilité du diff git et l'absence d'ambiguïté.
- **Trigger sync — au boot uniquement** : pas d'endpoint admin pour l'instant. Le flag `security.rbac.sync` contrôle l'exécution. Resync = redémarrage avec `RBAC_SYNC=true`.
- **Default `sync: false`** : safety net pour éviter une modif YAML accidentelle qui pollue la BD. Le devops/dev active explicitement quand il veut.
- **Surcharge `LocalizedRuntimeException(key, cause, args)`** : la convention "constructeur unique" est étendue à "deux formes officielles". Justifié pour préserver la cause des wraps techniques (IO, parsing). À garder en tête pour les exceptions futures.
- **Logs non i18n** : convention projet — les logs visent l'opérateur système, pas l'utilisateur final. Restent en anglais figé.
- **Convention de doc — `<X>ServiceImpl` uniquement** : les DomainServices (passe-plats data) et controllers (handlers minimaux) sont auto-documentants. Doc concentrée là où vit la logique métier.
- **Convention de doc révisée (resserrée)** : 1 phrase de javadoc par classe + 1 phrase par méthode, **zéro commentaire inline**. Le code doit parler de lui-même via le nommage ; un commentaire inline signale un refactor manquant.
- **Filtre HTTP custom hors SecurityConfig** : Spring Security 7 (Spring Boot 4) refuse `addFilterBefore(x, F.class)` si `F` n'est pas un filtre de la chaîne Security. Solution adoptée : laisser le filtre `@Component OncePerRequestFilter` être auto-enregistré par Spring Boot dans la chaîne servlet (s'exécute avant `springSecurityFilterChain`).
- **Logs HTTP en INFO** (vs DEBUG) : visibles en dev/prod par défaut. Les bodies sensibles sont masqués par regex (`"password":"***"`) — pas de risque de fuite tant que les champs déclarés couvrent les cas du projet.
- **GlobalException — WARN 4xx / ERROR 5xx** : aligne le niveau de log sur la sévérité. Le client n'est pas un bug serveur → WARN suffit ; un 500 est anormal → ERROR + stack.

**Où on s'est arrêté :**

- **162 tests verts** (152 → 162 avec les 4 sync + 6 filter).
- 5 commits poussés sur `origin/dev` : `0115ca4` "Externalisation seed RBAC dans roles-permissions.yml + flag security.rbac.sync" + `bef46eb` "Documentation services applicatifs (convention + javadoc RBAC sync)" + `1cf01e0` "Logs HTTP entrants/sortants + requestId MDC + GlobalException WARN/ERROR".
- Sync RBAC **désactivée par défaut** (`RBAC_SYNC=false`). La BD locale a été seedée lors du test d'intégration (orphelins préexistants : rôle `EMPLOYE` + perm `EMPLOYE_ACCESS` sur `PROPRIETAIRE` — non-bloquants).
- Convention de doc resserrée adoptée pour tous les futurs `<X>ServiceImpl`.
- Logs HTTP actifs sur toutes les requêtes (hors paths techniques), avec `requestId` MDC propagé dans chaque ligne.

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
