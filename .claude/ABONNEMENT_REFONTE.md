# ABONNEMENT_REFONTE.md — Refonte facturation mensuelle automatique

> Décision prise en session 2026-07-23. Implémentation backend complétée en session 2026-07-24.
> Migrations appliquées : **V65** · **V66** · **V67** · **V68** (bugfix `plan_abonnement_id`).
> Prochaine migration disponible : **V69**.

---

## 1. Bilan de l'existant

### Entités

| Entité | Rôle |
|---|---|
| `PlanAbonnement` | Plan tarifaire (nom, prix, quotas, fonctionnalités, actif/visible) |
| `TypePlanAbonnement` | Durée rattachée à un plan (dureeMois, réduction, trial flag) |
| `Abonnement` | Instance d'abonnement d'une entreprise (statut, dateDebut, dateFin) |
| `PaiementAbonnement` | Paiement soumis par l'OWNER (preuve, montant, statut validation) |
| `Coupon` | Code promo (global ou plan-scopé, usage limité, fenêtre dates) |
| `Promotion` | Réduction auto-appliquée sans code |
| `UtilisationCoupon` | Ticket de réservation coupon (créé à subscribe, supprimé si rejet) |

### Statuts actuels

**`AbonnementStatut`** : `EN_ATTENTE` · `ACTIF` · `EXPIRE` · `SUSPENDU` · `TRIAL`

**`StatutPaiementAbonnement`** : `EN_ATTENTE_VALIDATION` · `VALIDE` · `REJETE`

### Flux actuel (problème)

```
subscribe()  → nouvel Abonnement EN_ATTENTE  (OWNER choisit le plan)
payer()      → PaiementAbonnement EN_ATTENTE_VALIDATION (OWNER upload preuve)
admin valide → Abonnement ACTIF, dateFin = today + dureeMois
               ↓ expire
subscribe()  → NOUVEL Abonnement EN_ATTENTE  (OWNER doit rechoisir le plan !)
payer()      → ...
```

**Problème** : chaque cycle de facturation force l'OWNER à rechoisir son plan.
Un `Abonnement` devrait être un contrat permanent ; le `PaiementAbonnement` le versement périodique.

### Alertes / Notifications existantes

| Déclencheur | Type | Destinataire | État |
|---|---|---|---|
| AlertScheduler J-1/J-3/J-5 avant `dateFin` | `ABONNEMENT_EXPIRING` | OWNER | → restreindre au TRIAL uniquement |
| Owner soumet preuve | `PaiementAbonnementSubmittedEvent` | ADMIN | → garder |
| Admin valide | `PaiementAbonnementValidatedEvent` | OWNER | → garder, adapter message |
| Admin rejette | `PaiementAbonnementRejectedEvent` | OWNER | → garder |

---

## 2. Modèle cible

### Modèle de facturation — Option B (anniversaire)

Chaque OWNER a son propre cycle basé sur sa date de souscription. La facture est générée **10 jours avant `dateFin`**. Il n'y a pas de date fixe commune (pas de 5 du mois pour tout le monde).

```
Exemple : souscription le 25 juillet
  subscribe()  → Abonnement EN_ATTENTE
               → 1ère facture FACTURE_GENEREE, dateEcheance=25 juillet
  OWNER paie immédiatement → Admin valide → Abonnement ACTIF, dateFin=25 août

  15 août (dateFin - 10j) → Scheduler quotidien détecte dateFin dans 10j
                           → génère PaiementAbonnement { FACTURE_GENEREE, dateEcheance=25 août }
                           → Notification OWNER
  25 août → deadline paiement (dateEcheance = dateFin)
  Admin valide → dateFin prolongée à 25 septembre

  15 sept (dateFin - 10j) → nouvelle facture générée
  ...cycle perpétuel

  Si non payé au 25 août → Scheduler quotidien détecte dateFin < today sans paiement VALIDE
                         → Abonnement SUSPENDU, PaiementAbonnement EN_RETARD
                         → Notification OWNER + ADMIN
```

### Cycle de vie complet

```
Trial (inchangé)
  → gratuit X jours, pas un vrai abonnement
  → à la fin : OWNER souscrit à un plan payant
  → alertes ABONNEMENT_EXPIRING J-1/J-3/J-5 maintenues pour le TRIAL uniquement

subscribe() — une seule fois par contrat
  → Abonnement EN_ATTENTE (plan verrouillé)
  → 1ère PaiementAbonnement { FACTURE_GENEREE, dateEcheance=today }
  → OWNER uploade sa preuve immédiatement
  → Admin valide → Abonnement ACTIF, dateDebut=today, dateFin=today + 1 mois

Chaque mois (scheduler quotidien 08h00) :
  dateFin - 10j → génère PaiementAbonnement { FACTURE_GENEREE, dateEcheance=dateFin }
                  Notification OWNER
  dateFin       → deadline paiement
  dateFin + 1j  → si pas VALIDE → Abonnement SUSPENDU, facture EN_RETARD
                  Notification OWNER + ADMIN
  Admin valide  → dateFin += 1 mois

Changement de plan → `PATCH /abonnements/current/plan` → enregistre `prochainPlan`
                    → prend effet au cycle suivant (facture + validate())
```

### Décisions produit

| Question | Décision |
|---|---|
| Modèle facturation | **Option B — anniversaire** (cycle lié à la date de souscription) |
| Première facture | Générée le jour de la souscription, à payer immédiatement |
| Délai avant facture | 10 jours avant `dateFin` |
| Deadline paiement | `dateEcheance = dateFin` du cycle courant |
| Suspension | Le lendemain de `dateFin` si facture non validée |
| Changement de plan | Possible, prend effet au cycle suivant |
| Prorata | Non — mois complet uniquement |
| Trial | Inchangé — n'est pas un vrai abonnement |

---

## 3. Plan d'implémentation

### 3.1 Backend

#### Étape 1 — Migration Flyway V65

```sql
-- Nouveaux statuts paiement
ALTER TABLE paiement_abonnement
  DROP CONSTRAINT IF EXISTS paiement_abonnement_statut_check;
ALTER TABLE paiement_abonnement
  ADD CONSTRAINT paiement_abonnement_statut_check
  CHECK (statut IN ('EN_ATTENTE_VALIDATION','VALIDE','REJETE','FACTURE_GENEREE','EN_RETARD'));

-- Nouveaux champs paiement_abonnement
ALTER TABLE paiement_abonnement
  ADD COLUMN date_echeance DATE;

-- Changement de plan différé
ALTER TABLE abonnement
  ADD COLUMN prochain_plan_abonnement_id UUID REFERENCES plan_abonnement(id);
```

#### Étape 2 — Entité `PaiementAbonnement`

Ajouter :
- `LocalDate dateEcheance`

Ajouter sur `Abonnement` :
- `PlanAbonnement prochainPlan` (ManyToOne, nullable) — plan demandé pour le prochain cycle

#### Étape 3 — Enum `StatutPaiementAbonnement`

Ajouter :
- `FACTURE_GENEREE` — facture générée automatiquement, preuve non soumise
- `EN_RETARD` — 16 passé, paiement non validé

#### Étape 4 — Repository `PaiementAbonnementRepository`

Nouvelles méthodes :
- `existsByAbonnementIdAndDateEcheance(UUID, LocalDate) → boolean` (anti-doublon scheduler)
- `findAbonnementsToFacture(LocalDate targetDate) → List<Abonnement>` — abonnements ACTIF dont `dateFin = targetDate` et sans `PaiementAbonnement` existant avec `dateEcheance = dateFin`
- `findAbonnementsToSuspend(LocalDate today) → List<PaiementAbonnement>` — factures `FACTURE_GENEREE` ou `EN_ATTENTE_VALIDATION` dont `dateEcheance < today`

#### Étape 5 — Refactoring `subscribe()`

Avant : crée `Abonnement EN_ATTENTE` → l'OWNER crée ensuite lui-même le `PaiementAbonnement`.

Après :
1. Crée `Abonnement { statut=EN_ATTENTE }` (plan verrouillé)
2. Crée immédiatement `PaiementAbonnement { statut=FACTURE_GENEREE, dateEcheance=today }`
3. Retourne les deux dans `SubscribeResponse`

#### Étape 6 — Nouveau endpoint `POST /paiements-abonnement/{id}/payer`

Remplace l'ancien `POST /paiements-abonnement/abonnements/{abonnementId}`.

- OWNER soumet sa preuve contre une facture **existante** (`FACTURE_GENEREE`)
- Upload du fichier (multipart)
- Guard : facture doit être `FACTURE_GENEREE` et appartenir à l'entreprise du caller
- Transition : `FACTURE_GENEREE → EN_ATTENTE_VALIDATION`
- Publie `PaiementAbonnementSubmittedEvent` (notification ADMIN — inchangé)

> L'ancien endpoint `POST /paiements-abonnement/abonnements/{abonnementId}` est déprécié et retiré.

#### Étape 7 — Nouveau endpoint `PATCH /abonnements/current/plan`

- OWNER demande un changement de plan pour le prochain cycle
- Guards : `Abonnement.statut = ACTIF`, plan cible actif et visible, plan différent du plan courant
- Enregistre `Abonnement.prochainPlan = nouveauPlan`
- Quotas inchangés — `AbonnementQuotaService` lit toujours `planAbonnement` (jamais `prochainPlan`)
- Le changement prend effet uniquement après paiement et validation de la prochaine facture

#### Étape 8 — Refactoring `validate()`

Avant : réactive l'abonnement depuis zéro (recalcule dateDebut/dateFin).

Après :
- Si c'est le premier paiement (Abonnement `EN_ATTENTE`) → `ACTIF`, `dateDebut=today`, `dateFin=today + 1 mois`
- Si c'est un renouvellement (Abonnement `ACTIF`) :
  - Si `prochainPlan != null` → `planAbonnement ← prochainPlan`, `prochainPlan = null`
  - `dateFin += 1 mois`
- `PaiementAbonnement → VALIDE`
- Publie `PaiementAbonnementValidatedEvent` (message adapté : "actif jusqu'au [dateFin]")

#### Étape 9 — `FacturationAbonnementScheduler`

```
Cron : ${CRON_FACTURATION_ABONNEMENT:0 0 8 * * *}   (défaut : tous les jours à 08h00)
```

`application.properties` :
```properties
cron.facturation.abonnement=${CRON_FACTURATION_ABONNEMENT:0 0 8 * * *}
```

Annotation Spring :
```java
@Scheduled(cron = "${cron.facturation.abonnement}")
```

Logique :
- `targetDate = today + 10 jours`
- Cherche tous les `Abonnement { statut=ACTIF, dateFin=targetDate }` sans `PaiementAbonnement` existant avec `dateEcheance=dateFin` (guard anti-doublon)
- Pour chacun :
  1. `planEffectif = prochainPlan ?? planAbonnement`
     `montantNormal = planEffectif.prix`
  3. Chercher coupon applicable (actif, entreprise ciblée ou global, plan match, quotas OK)
  4. Si coupon trouvé : calculer `montantPromotionnel`, créer `UtilisationCoupon`
  5. Crée `PaiementAbonnement { FACTURE_GENEREE, dateEcheance=dateFin, montantNormal }`
  6. Publie `FactureAbonnementGenereeEvent` → notification OWNER

#### Étape 10 — `SuspensionAbonnementScheduler`

```
Cron : ${CRON_SUSPENSION_ABONNEMENT:0 0 8 * * *}   (défaut : tous les jours à 08h00)
```

`application.properties` :
```properties
cron.suspension.abonnement=${CRON_SUSPENSION_ABONNEMENT:0 0 8 * * *}
```

Annotation Spring :
```java
@Scheduled(cron = "${cron.suspension.abonnement}")
```

Logique :
- Cherche tous les `PaiementAbonnement { statut IN (FACTURE_GENEREE, EN_ATTENTE_VALIDATION), dateEcheance < today }`
- Pour chacun :
  1. Marquer `PaiementAbonnement → EN_RETARD`
  2. Marquer `Abonnement → SUSPENDU`
  3. Publier `AbonnementSuspenduEvent` → notification OWNER + ADMIN

> Les deux schedulers tournent chaque jour — `FacturationAbonnementScheduler` ne génère que pour les abonnements dont `dateFin = today + 10` et sans `PaiementAbonnement` existant avec `dateEcheance = dateFin`. `dateEcheance` est unique par abonnement par cycle et sert de guard anti-doublon.

#### Étape 11 — Alertes

| Alerte | Changement |
|---|---|
| `ABONNEMENT_EXPIRING` (AlertScheduler J-1/J-3/J-5) | Filtrer `statut=TRIAL` uniquement (ne plus déclencher pour `ACTIF`) |
| `FACTURE_ABONNEMENT_GENEREE` | Nouveau — déclenché par `FacturationAbonnementScheduler` |
| `ABONNEMENT_SUSPENDU` | Nouveau — déclenché par `SuspensionAbonnementScheduler` |
| `PaiementAbonnementSubmittedEvent` | Inchangé |
| `PaiementAbonnementValidatedEvent` | Message adapté : ajouter `dateFin` |
| `PaiementAbonnementRejectedEvent` | Inchangé |

#### Étape 12 — i18n FR/EN

Nouvelles clés :
```properties
# FR
paiementAbonnement.statut.FACTURE_GENEREE=Facture générée
paiementAbonnement.statut.EN_RETARD=En retard
notification.abonnement.facture.titre=Facture d''abonnement disponible
notification.abonnement.facture.message=Votre facture du mois {0} est disponible. Montant : {1} XOF. À payer avant le {2}.
notification.abonnement.suspendu.titre=Abonnement suspendu
notification.abonnement.suspendu.message=Votre abonnement a été suspendu pour non-paiement. Échéance dépassée : {0}.
paiementAbonnement.notFactureGeneree=Cette facture ne peut plus recevoir de preuve (statut : {0})

# EN
paiementAbonnement.statut.FACTURE_GENEREE=Invoice generated
paiementAbonnement.statut.EN_RETARD=Overdue
notification.abonnement.facture.titre=Subscription invoice available
notification.abonnement.facture.message=Your invoice for {0} is available. Amount: {1} XOF. Pay before {2}.
notification.abonnement.suspendu.titre=Subscription suspended
notification.abonnement.suspendu.message=Your subscription has been suspended due to non-payment. Due date exceeded: {0}.
paiementAbonnement.notFactureGeneree=This invoice can no longer receive proof (status: {0})
```

#### Étape 13 — Tests

- `PaiementAbonnementServiceImplTest` : adapter les tests existants (suppression du flow `abonnementId`) + nouveaux cas (`payer()` nominal, guard statut, guard ownership)
- `AbonnementServiceImplTest` : adapter `subscribe()` (1ère facture créée), `validate()` (prolongation vs activation)
- `FacturationAbonnementSchedulerTest` : génération nominale, anti-doublon
- `SuspensionAbonnementSchedulerTest` : suspension nominale, abonnements VALIDE non touchés

---

### 3.2 Frontend (après validation backend)

| Page | Changement |
|---|---|
| OWNER "Mon abonnement" | Affiche la facture du mois en cours (statut, montant, dateEcheance) + bouton "Payer" |
| Bouton "Payer" | Modal upload preuve simple — appelle `POST /paiements-abonnement/{id}/payer` |
| Flow souscription | Choix plan → paiement immédiat (sans étape intermédiaire) |
| Historique paiements | Affiche `FACTURE_GENEREE` / `EN_RETARD` avec les bons labels |
| Admin paiements | Ajout colonne `dateEcheance`, badge `EN_RETARD` en rouge |

---

## 4. Fichiers impactés (backend)

| Fichier | Action |
|---|---|
| `V65__type_plan_suppression.sql` | Supprimer `type_plan_abonnement_id`, ajouter `plan_abonnement_id` sur `abonnement` ; supprimer `actif`, `renouvellement_auto` sur `abonnement` ; ajouter `trial` sur `plan_abonnement` |
| `V66__paiement_abonnement_refonte.sql` | Ajouter `date_echeance` sur `paiement_abonnement` ; nouveaux statuts `FACTURE_GENEREE` / `EN_RETARD` |
| `V67__coupon_promotion_refonte.sql` | Ajouter `entreprise_id` (nullable) sur `coupon` ; supprimer `date_debut`, `date_fin` sur `coupon` ; ajouter `paiement_abonnement_id` sur `utilisation_coupon` ; supprimer table `promotion` |
| `Abonnement.java` | Remplacer `typePlanAbonnement` par `planAbonnement` (ManyToOne direct) ; ajouter `prochainPlan` (ManyToOne nullable) ; supprimer `actif`, `renouvellementAuto` |
| `AbonnementController.java` | Ajouter `PATCH /abonnements/current/plan` |
| `IAbonnementService.java` + impl | Ajouter `changerPlan()` |
| `PlanAbonnement.java` | Ajouter `trial: boolean` |
| `TypePlanAbonnement.java` | Supprimer |
| `Coupon.java` | Ajouter `entreprise` (ManyToOne nullable) ; supprimer `dateDebut`, `dateFin` (`actif`, `nombreUtilisationsMax`, `nombreUtilisations` conservés) |
| `Promotion.java` | Supprimer |
| `IPromotionService.java` + impl | Supprimer |
| `PromotionController.java` | Supprimer |
| `UtilisationCoupon.java` | Ajouter `paiementAbonnement` (ManyToOne) |
| `PaiementAbonnement.java` | Ajouter `dateEcheance` (champs montant existants conservés) |
| `StatutPaiementAbonnement.java` | Ajouter `FACTURE_GENEREE`, `EN_RETARD` |
| `PaiementAbonnementRepository.java` | Nouvelles méthodes |
| `IPaiementAbonnementService.java` + impl | Refactorer `create()` → `payer()`, adapter `validate()` |
| `IAbonnementService.java` + impl | Refactorer `subscribe()` |
| `AbonnementController.java` | Inchangé |
| `PaiementAbonnementController.java` | Remplacer endpoint `abonnements/{id}` par `{id}/payer` |
| `FacturationAbonnementScheduler.java` | Créer (génération + application coupon auto) |
| `SuspensionAbonnementScheduler.java` | Créer |
| `AlertScheduler.java` | Filtrer `ABONNEMENT_EXPIRING` sur TRIAL uniquement |
| `messages.properties` + `messages_en.properties` | Nouvelles clés |
| Tests impactés | Adapter + créer |

---

## 5. Décisions sur `TypePlanAbonnement` et `Coupon`

### `TypePlanAbonnement` — **supprimé**

Avec la facturation mensuelle fixe, `TypePlanAbonnement` perd tout intérêt :
- `dureeMois` → toujours 1, plus de choix de durée
- Réductions de durée → inutiles (on facture toujours `plan.prix`)
- `trial` flag → migré vers `PlanAbonnement.trial: boolean`

**Migration** : supprimer `type_plan_abonnement_id` sur `abonnement`, ajouter `plan_abonnement_id` direct.

### `Coupon` — **refonte**

#### Nouveau champ : `entreprise` (ManyToOne, nullable)

| `entreprise` | Comportement |
|---|---|
| `null` | Coupon global — s'applique à tous les abonnements |
| `= entreprise X` | Coupon ciblé — s'applique uniquement à cette entreprise |

L'admin sélectionne une entreprise ou laisse vide (= tous) à la création du coupon. Pour cibler plusieurs entreprises, l'admin crée un coupon par entreprise.

**Le coupon n'est jamais saisi par l'OWNER.** Il n'y a pas de code à entrer, pas de partage possible.

#### Champ : `actif` (boolean, non nullable, défaut `true`) — déjà existant

Positionné à `false` automatiquement par le scheduler dès que `nombreUtilisations >= nombreUtilisationsMax`. L'admin peut aussi le désactiver manuellement.

#### Champs conservés de `Coupon`

- `nombreUtilisationsMax` — quota total d'applications
- `nombreUtilisations` — compteur incrémenté à chaque application par le scheduler

#### Champs supprimés de `Coupon`

| Champ | Raison |
|---|---|
| `dateDebut` | `actif` suffit à contrôler la validité |
| `dateFin` | Idem |

#### `Promotion` — **supprimée**

`Promotion` est redondante avec `Coupon` dans le nouveau modèle :
- Un coupon `entreprise=null` couvre le cas global (toutes entreprises)
- `actif` remplace `dateDebut/dateFin`
- Un seul concept, un seul scheduler

`Promotion` est donc supprimée (entité, table, service, controller).

#### Point d'application : `FacturationAbonnementScheduler`

Le coupon est **appliqué automatiquement** lors de la génération de chaque facture mensuelle. Aucune saisie utilisateur.

Logique dans le scheduler (après calcul `montantNormal`) :
```
couponApplicable = trouver premier coupon WHERE actif=true
                   ET (coupon.entreprise IS NULL OU coupon.entreprise = abonnement.entreprise)
                   ET plan match
                   ET (coupon.nombreUtilisations + 1) <= coupon.nombreUtilisationsMax

si couponApplicable trouvé :
  réduction = appliquer réduction(montantNormal, coupon)
  PaiementAbonnement.reduction = réduction
  PaiementAbonnement.montantFinal = montantNormal - réduction
  créer UtilisationCoupon { coupon, abonnement, paiementAbonnement }
  coupon.nombreUtilisations += 1
  si coupon.nombreUtilisations >= coupon.nombreUtilisationsMax → coupon.actif = false
sinon :
  PaiementAbonnement.reduction = null
  PaiementAbonnement.montantFinal = montantNormal
```

#### Champs `PaiementAbonnement` — champs existants réutilisés + ajouts

Champs existants conservés tels quels :
- `montantAvantReduction` → prix brut du plan (= `montantNormal`)
- `reduction` → montant de la réduction appliquée par le coupon
- `montantFinal` → prix effectivement dû après coupon

Nouveaux champs à ajouter :
| Champ | Type | Nullable | Description |
|---|---|---|---|
| `dateEcheance` | LocalDate | non | Date limite de paiement (= `dateFin` de l'abonnement) |
| `periodeFacturation` | String | non | Période couverte (YYYY-MM), guard anti-doublon |
| `coupon` | ManyToOne Coupon | oui | Coupon auto-appliqué (null si aucun) |

#### `UtilisationCoupon` — refonte

Avant : ticket de réservation créé à `subscribe()`, supprimé si rejet.

Après : **une ligne créée par le scheduler** à chaque application sur une facture, jamais supprimée.

```
UtilisationCoupon { coupon, abonnement, paiementAbonnement }
```

`paiementAbonnement` → FK vers la facture sur laquelle le coupon a été appliqué.

---

## 6. Ce qui ne change pas

- `PlanAbonnement` CRUD — inchangé (sauf ajout du flag `trial: boolean`)
- `Promotion` — **supprimée** (remplacée par `Coupon` global)
- Gate login (`hasActiveSubscription`) — inchangé
- Quotas magasins/employés (`AbonnementQuotaService`) — inchangés
- Trial (`createTrialForSignup`) — inchangé
- Catalogue public (`/catalog/public`) — inchangé (retire les types, affiche les plans directs)

---

## 7. État d'implémentation backend (2026-07-24)

### 7.0 Migrations appliquées

| Migration | Contenu |
|---|---|
| V65 | Suppression `type_plan_abonnement` + `type_plan_abonnement_id` sur `abonnement` ; ajout `trial` sur `plan_abonnement` ; nouveaux statuts `AbonnementStatut` |
| V66 | Ajout `date_echeance` + nouveaux statuts `FACTURE_GENEREE` / `EN_RETARD` sur `paiement_abonnement` ; ajout `prochain_plan_abonnement_id` sur `abonnement` |
| V67 | Suppression table `promotion` ; refonte `coupon` (ajout `entreprise_id` nullable ; suppression `date_debut`/`date_fin`) ; refonte `utilisation_coupon` (ajout FK `paiement_abonnement_id`) |
| V68 | **Bugfix** — ajout colonne nullable `plan_abonnement_id` sur `abonnement` (présente dans l'entité JPA mais absente du schéma après V65 ; Hibernate `validate` échouait au démarrage) |

### 7.1 Étapes backend — statut

| Étape | Statut |
|---|---|
| Étape 1 — Migrations V65/V66/V67 | ✅ |
| Étape 2 — Entités (`prochainPlan`, `dateEcheance`) | ✅ |
| Étape 3 — `StatutPaiementAbonnement` (`FACTURE_GENEREE`, `EN_RETARD`) | ✅ |
| Étape 4 — Repositories (nouvelles méthodes) | ✅ |
| Étape 5 — `subscribe()` refonte | ✅ (voir §7.2) |
| Étape 6 — Endpoint `payer()` | ✅ |
| Étape 7 — `changerPlan()` | ✅ |
| Étape 8 — `validate()` → `activateOrExtend()` | ✅ |
| Étape 9 — `FacturationAbonnementScheduler` (J−10) | ✅ |
| Étape 10 — `SuspensionAbonnementScheduler` (J+1) | ✅ |
| Étape 11 — Alertes (`ABONNEMENT_EXPIRING` restreint au TRIAL) | ✅ |
| Étape 12 — i18n FR/EN | ✅ |
| Étape 13 — Tests | ✅ — 915/915 verts |

### 7.2 Décision : coupon supprimé du flow `subscribe()`

**Décision prise en session 2026-07-24.**

Le flow `subscribe()` ne prend plus de coupon en entrée :

- `SubscribeRequest` : uniquement `planId` (plus de `couponCode` ni `typeId` ni `renouvellementAuto`)
- `SubscribeResponse` : uniquement `abonnement + breakdown` (plus de `couponCodeApplied`)
- `subscribe()` passe `null` coupon au calculateur : `new SubscriptionAmountInputs(plan, null)`

`SubscriptionAmountBreakdown`, `SubscriptionAmountInputs` et `SubscriptionAmountCalculator` sont **conservés intacts** — le coupon reste utilisé par `FacturationAbonnementScheduler` lors des factures de renouvellement.

### 7.3 Conformité Rule 1 et Rule 27 — `AbonnementServiceImpl`

- **Rule 1** — plus de `PlanAbonnementRepository` ni `PaiementAbonnementDomainService` injectés directement. Remplacés par `IPlanAbonnementService` et `IPaiementAbonnementService`.
- **Rule 27** — méthode privée `findPlan(UUID)` supprimée ; appel inliné directement dans `subscribe()` et `changerPlan()`.
- **Rule 30** — `createFactureGeneree(Abonnement, SubscriptionAmountBreakdown, LocalDate)` ajouté sur `IPaiementAbonnementService` (3 params regroupant les 5 params de la méthode domain).

### 7.4 Frontend

Section 3.2 — **pas encore commencé**.
