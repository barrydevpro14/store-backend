# SESSIONS.md — Session journal

> Claude Code fills this file at the end of each session.
> Read it first when starting a new session to pick up the context immediately.

> **Language note**: from 2026-05-18 onwards, this journal is kept in English. The full French history prior to that date is archived in [`SESSIONS_FR_ARCHIVE.md`](SESSIONS_FR_ARCHIVE.md).

---

## 📌 Latest session

**Date:** 2026-05-24 — Close the achat stock-update gap: fold reception into a single `receive()` operation, drop VALIDEE + PARTIELLEMENT_RECEPTIONNEE statuts (backend + frontend)

**Subject:** Short, focused refactor. Previous session shipped the achat module end-to-end but removed the reception UI, leaving validated commandes stuck without ever entering stock (validate created only the facture ; the orphaned `POST /receptions` endpoint that materialized stock was unreachable from the UI). User pushed back: "did you update stock while achat process?". After a short clarifier (Option A restore the partial-reception flow vs Option B fold reception into validate) the user picked B, then asked to rename the merged method `validate` → `receive` and to drop the now-unreachable intermediate statuts entirely. 3 atomic commits pushed to `origin/dev` on both repos.

**Notable decisions:**

### Backend — Single `receive()` operation (commits `7d58af6`, `48229d0`)

- **`AchatServiceImpl.receive(UUID, AchatReceiveRequest)`** is now the sole engagement step out of DRAFT. One transaction: recompute `montantTotal` from current lignes → create `FactureAchat` (auto-numero via `ReferenceHelper` if blank, else uniqueness check) → apply optional initial paiement → for each ligne create `EntreeStock` (snapshot lot/expiration from the ligne), upsert aggregate `Stock`, journal `MouvementStock(ENTREE_ACHAT)` with `facture.numero` as `referenceDocument`, update `productFournisseur.prixVente`, increment `LigneCommandeAchat.quantiteRecue` → bascule DRAFT → RECEPTIONNEE. Helper `materializeStockForLigne(commande, facture, ligne)` extracted (rule 30 — 3 params).
- **DTO rename** `AchatValidateRequest` → `AchatReceiveRequest`. Same shape `(facture: FactureAchatCreateRequest, paiement: PaiementAchatRequest?)`.
- **Endpoint rename** `POST /api/v1/achats/{id}/validate` → `POST /api/v1/achats/{id}/receive`. `PURCHASE_APPROVE` permission unchanged.
- **Drop the partial-reception batch** : `IAchatService.receive(commandeId, ReceptionAchatRequest)` + helpers (`receiveOneLine`, `ensureReceivable`, `ensureLignesDistinctes`, `ensureQuantiteRecueNotExceeded`), the controller `POST /receptions` endpoint, and the 3 DTOs `ReceptionAchatRequest` / `LigneReceptionRequest` / `ReceptionAchatResponse` all deleted.
- **`ensureCancellable` tightened** to allow only `RECEPTIONNEE`. DRAFT was never cancellable (no stock to reverse — delete lignes to abandon a brouillon) and the intermediate statuts no longer exist.
- **Enum prune** : `CommandeAchatStatut` keeps only `DRAFT / RECEPTIONNEE / ANNULEE`. `CommandeAchatDomainService.validate()` and `markPartiallyReceived()` removed (no production caller).
- **Flyway `V15__drop_validee_partielle_statuts.sql`** : backfill any leftover row (`UPDATE commande_achat SET statut='RECEPTIONNEE' WHERE statut IN ('VALIDEE','PARTIELLEMENT_RECEPTIONNEE')`) then rebuild `commande_achat_statut_check` with the narrowed allowed set. Idempotent (`DROP CONSTRAINT IF EXISTS`).
- **i18n** : drop the 4 `commandeAchat.receive.*` keys (FR + EN). `commandeAchat.cancel.notCancellable` wording updated to mention only RECEPTIONNEE.
- **Tests** : `AchatServiceImplTest` + `AchatControllerTest` rewritten. New service test cases (`receive_should_create_facture_and_materialize_stock_for_every_ligne`, `receive_should_compute_total_from_lines`, `receive_should_persist_initial_paiement_when_provided`, `receive_should_throw_when_paiement_exceeds_total`, `receive_should_throw_when_commande_not_draft`, `receive_should_propagate_forbidden_when_not_owned`, `cancel_should_throw_when_facture_has_paiement`) assert facture creation + montant computation + stock side-effects (`EntreeStock` creation, `MouvementStock(ENTREE_ACHAT)` journal, `pf.prixVente` update, `quantiteRecue` increment) + statut bascule. Old `validate_*` + `receive_*` (reception batch) + `cancel_should_allow_validee_*` + `cancel_should_allow_partiellement_*` tests removed. Backend suite : **787 / 787 green**.

### Frontend — Cascade rename + statut prune (commit `988a8fa`)

- **File renames** : `achat-validate-request.ts` → `achat-receive-request.ts`, `useValidateAchat.ts` → `useReceiveAchat.ts`, `ValidateAchatDialog.tsx` → `ReceiveAchatDialog.tsx`. Git tracked one of those as a rename, two as new+delete because content changed.
- **Adapter** `commande-achat-api.ts` : method `validate()` → `receive()`, endpoint `${ACHAT_PATH}/${commandeId}/validate` → `/receive`. Port `ICommandeAchatRepository.receive(...)` renamed accordingly.
- **`AchatDetailsContent.tsx`** : import `ReceiveAchatDialog`, state vars `receiveOpen` / `canReceive` / `handleOpenReceive`, action button label switched to `t('actions.receive')`, `CANCELLABLE_STATUTS` narrowed to `['RECEPTIONNEE']`. Comment block clarifies the one-step lifecycle.
- **TS enum prune** `commande-achat-statut.ts` : union narrowed to `'DRAFT' | 'RECEPTIONNEE' | 'ANNULEE'`. `CommandeAchatStatutBadge.tsx` drops the corresponding variants.
- **i18n** `messages/{fr,en}.json` : rename `validateDialog` → `receiveDialog` block (description, submit + submitting copy), `toasts.validated` → `toasts.received`, `details.actions.validate` → `details.actions.receive`. Drop `statut.VALIDEE` + `statut.PARTIELLEMENT_RECEPTIONNEE` entries.
- **Stale comments** swept in `useUpdateLigne`, `useDeleteLigne`, `ligne-achat-update-request`, `achat-response` — references to "statut ≥ VALIDEE" replaced by "la commande n'est plus en DRAFT" wording.
- Vitest : **314 / 314 green**. `tsc --noEmit` clean apart from the pre-existing `FormField.test.tsx` resolver typing error (unrelated, parked from last session).

### Commit strategy

3 atomic commits, each compiling + tests passing independently :
1. `7d58af6` **refactor(achat)**: fold reception into single `receive()` operation — service + interface + controller + DTOs + i18n drops + tests (12 files, +198/−596).
2. `48229d0` **chore(achat)**: drop VALIDEE + PARTIELLEMENT_RECEPTIONNEE statuts — enum + Flyway V15 + domain service cleanup (3 files, +16/−14). Temporarily restored the enum + domain service methods between commits 1 and 2 to keep each commit compileable (verified the achat slice 37/37 green at every step).
3. `988a8fa` **refactor(achat)**: mirror backend rename + drop dead statuts in frontend (16 files, +127/−138).

### Verification

- Backend `./mvnw test` : **787 / 787 green** end-to-end (full suite, not just the achat slice).
- Frontend `vitest run` : **314 / 314 green**.
- Frontend `tsc --noEmit` : clean (modulo the parked `FormField.test.tsx` resolver typing error).
- Pushed to `origin/dev` on both repos : backend `eacb28c..48229d0`, frontend `4b5acca..988a8fa`. GitLab is suggesting MR links on both.

### Open follow-ups (parked)

- **Cancel "issue refund" workflow** — paiement-blocking cancel still enforced (`ensureNoPaiementRecorded` refuses cancel if `facture.montantPaye > 0`) ; the proper refund flow is hors-scope V1.
- **Vitest runnable in env** — Node v20.19.5 via nvm `default` works now (previous Node v20.15.1 vs `std-env` mismatch resolved last session). Confirmed runnable this session.
- **`FormField.test.tsx` RHF resolver typing error** still untriaged — pre-existing from last session, unrelated to this refactor.
- **MR to main** — GitLab suggesting MR links on both repos when ready.

---

## 2026-05-23 — Module Achat end-to-end : create-order modal, paginated line table, details-in-modal, validate-with-initial-paiement, paiement list, cancel guard, frontend rule 51 (required-field asterisk)

**Subject:** Long, multi-chunk session covering the full Achat module from scratch on the frontend plus three coordinated backend hardenings (constraint-violation handler, optional facture numero + auto-generation, optional initial paiement at validate, refuse cancel with paiement). Heavy UX iteration on the create-order surface (paginated line table, autocomplete combobox, NaN-safe number inputs, density passes, asterisk-only required markers, total + pagination on the same row). Receive step removed from the UI. Details page promoted to a modal (with a deep-link page route kept). 13 atomic commits pushed (4 backend + 9 frontend).

**Notable decisions:**

### Backend — Validate / cancel lifecycle hardening (commits `f1905cc`, `5092f59`, `ecc4d6e`)

- **`LigneCommandeAchatResponse.quantiteRecue`** — added the cumulative received-quantity field to the read DTO (constructor + record). Frontend `ligne-commande-achat.ts` mirror updated to match.
- **`DataIntegrityViolationException` handler** — new `@ExceptionHandler` in `GlobalException` extracts the Hibernate constraint name from the cause chain and maps to a known i18n key (`facture_achat_numero_key` → `factureAchat.numero.alreadyExists`). Unknown constraints fall back to the generic `error.dataIntegrity` (new key in both locales) and log the full stack at ERROR for debug. Defense-in-depth for race conditions or code paths that bypass the service-level pre-check.
- **`FactureAchat.numero` made optional with auto-generation** — `FactureAchatCreateRequest.numero` dropped `@NotBlank`, kept `@Size(max=100)`. `FactureAchatDomainService.generateNumero()` builds `FACT-yyyyMMdd-HHmmssSSS` via the existing `ReferenceHelper`. `existsByNumero` added on the repository + domain service. `AchatServiceImpl.resolveNumeroFacture(...)` routes: empty → auto-generate ; provided → pre-check via `existsByNumero` and throw `UniqueResourceException("factureAchat.numero.alreadyExists", numero)`.
- **`FactureAchat.dateEcheance` now required** (`@NotNull`) — comptable needs an échéance for payment tracking. Frontend Zod schema mirrors `.min(1, …)`.
- **Optional initial paiement at validate** — `AchatValidateRequest` gains `@Valid PaiementAchatRequest paiement` (nullable). `AchatServiceImpl.applyOptionalInitialPaiement(...)` runs inside the same `@Transactional` as facture creation : refuses overpaiement (`paiementAchat.montant.exceedsRemaining`), persists the paiement via `PaiementAchatDomainService.create`, applies it to the facture (`montantPaye` + statut update). Atomic — no half-state if either leg fails.
- **`ensureNoPaiementRecorded` in cancel** — refuses to cancel a commande whose facture has `montantPaye > 0` with new key `commandeAchat.cancel.hasPaiements`. Rationale : cancel reverses stock entries but doesn't touch paiements ; accepting it would leave an orphan paiement against a facture basculée en ANNULEE. Correct workflow is a refund (hors-scope V1), not a cancel.
- **`AchatControllerTest`** : new `should_return_200_when_validate_with_blank_numero` (auto-gen path) + renamed `should_return_400_when_validate_facture_dateEcheance_missing` (numero blank is now legal — dateEcheance missing is the new 400 path). `AchatServiceImplTest` injects the new `PaiementAchatDomainService` mock. Backend achat slice **49 / 49 green**.

### Frontend — Searchable Combobox + Label `required` (commits `30fda07`, `470a9f8`)

- **Combobox primitive** (`src/common/presentation/ui/combobox.tsx`) wrapping `@base-ui/react/combobox` with the existing aesthetic. Drop-in replacement for `<Select>` when the list grows past ~30 entries. Takes `{ value, label }[]`, emits the value string.
- **`<Label required>`** prop renders a trailing red `*` (`aria-hidden`). Pair with `aria-required` on the input. Documented as **frontend rule 51** in `.claude/FRONTEND_CODING_CONVENTIONS.md` and saved as the `required-field-asterisk` feedback memory. Form-by-form rollout same cadence as rule 50. Applied across the three achat forms this session ; existing `(facultatif)` / `(optional)` suffixes dropped from the matching i18n keys.

### Frontend — Product fournisseur catalog (commit `654e727`)

- `ProductFournisseur` DTO + `productFournisseurApi.{list,create}` against `/api/v1/product-suppliers` + `useProductFournisseurList()`. The create-line flow uses this to resolve `(productId, fournisseurId, qualityId)` → existing junction first, otherwise creates one on the fly. The OWNER never has to maintain the supplier-catalog by hand.

### Frontend — Achat module foundation (commits `479a133`, `76bb6fa`, `8acc4ad`)

- **Domain** : ~17 DTOs mirroring backend records (commande, lignes with `quantiteRecue`, statuts + motif-annulation enums, facture + paiement request/response, draft / validate / cancel / update-ligne payloads, summary lookups). `ICommandeAchatRepository` port covers list / findById / findDetailsById / createDraft / validate / cancel / updateLigne / deleteLigne.
- **Infrastructure** : `commande-achat-api.ts` (orchestration) + `paiement-achat-api.ts` (facture-side list + create).
- **Application** : `commandeAchatKeys` / `paiementAchatKeys` factories + mutation hooks (`useCreateAchat`, `useValidateAchat`, `useCancelAchat`, `useUpdateLigne`, `useDeleteLigne`, `useCreatePaiementAchat`) with scoped invalidation (paiement creation invalidates both the paiement list AND the commande's full-detail block, so the open details modal reflects the new `montantPaye` / statut).
- **i18n** : full `dashboard.achats.*` namespace (~550 lines FR + EN) — listing, filters, statut + motifAnnulation enums, dialogs (create / validate / cancel / edit-ligne / paiement / details), moyenPaiement labels, toasts.

### Frontend — Achat read + workflow + paiement UI (commit `dfbee6f`)

- **Listing** : `CommandeAchatTable` (TanStack DataTable + status badges + client-side total HT), `CommandeAchatRowActions` (Voir le détail → callback opens modal, replaced the previous `<Link>`), `CommandeAchatFilters` (rule-50 right-aligned CTA + `DateRangeFilter` per rule 48), `CommandeAchatStatutBadge`, `AchatMagasinSelect` (OWNER picker when JWT has no magasinId).
- **Details surfaced in a modal** : extracted `AchatDetailsContent` (shared body — status grid, facture summary, lignes table, sub-dialog wiring) + `AchatDetailsDialog` (modal wrapper that routes the workflow action bar into the `DialogFooter` via a `renderActionBar` render-prop). The full-page route at `/dashboard/achats/[id]` reuses the same content component, so the page and the modal never drift.
- **Workflow dialogs** :
  * `ValidateAchatDialog` — facture fields (numero optional + hint, dateEcheance required) + an optional "Paiement initial" section riding the same POST. Required-field `*` markers per rule 51. `prixVente > prixAchat` enforced on Ajouter click only (toast, not inline) — kept it out of the Zod schema so it doesn't fire as the user types.
  * `CancelAchatDialog` — motif + commentaire form.
  * `EditLigneDialog` — DRAFT-only ligne edit (quantite / prixAchat / prixVente / lot / expiration), same `prixVente > prixAchat` refine.
- **Paiement** : `AchatFacturePaiementsSection` (compact Date / Moyen / Montant table under the facture summary) + `CreatePaiementAchatDialog` (`montant ≤ montantRestant` enforced). The "Nouveau paiement" button is gated by `PURCHASE_PAY` AND `facture.statut !∈ {PAYEE, ANNULEE}` ; the Cancel button is hidden when `facture.montantPaye > 0` (matches the backend guard).
- **Receive step removed from the UI** : dropped the Réceptionner button, `ReceiveAchatDialog`, `useReceiveAchat`, the 3 reception DTOs, and the `receive` port + adapter. Backend `receive(...)` endpoint left in place (unused but not deleted).

### Frontend — Create-order UX (commit `c08cb72`)

- Two-zone layout :
  1. Header (fournisseur + dateCommande) + an inline `AddAchatLineRow` (own RHF sub-form, resets on Add). Combobox primitive for product + quality. NaN-safe numeric inputs via `parseNumericInput` + Controller pattern (`form.reset(EMPTY_LINE)` clears every field visibly — register+valueAsNumber was leaving stale DOM values for the numeric inputs).
  2. `AchatLineTable` — paginated read-only view of the appended lignes. Display order is reverse-chrono (newest at top) AND auto-jumps to page 1 on each Add — the manager sees confirmation of their last click without scrolling. Per-row delete. Total HT lives on the same row as the pagination controls (right side). Pagination card density tweaked : `[&_[data-slot=select-trigger]]:!h-7` to defeat the primitive's `data-[size=default]:h-12` ; hidden range "X-Y sur Z" helper.
- Submit resolves each line to a `productFournisseurId` (cached lookup, otherwise `POST /api/v1/product-suppliers`) before posting `/achats`. `CreateAchatDialog` is the modal wrapper, widened to `sm:max-w-6xl xl:max-w-7xl`.
- Submit button now disabled only on `!hasLines || isSubmitting` (dropped the redundant fournisseur check — Zod catches it).
- Native HTML `required` attribute removed from line-row inputs (replaced with `aria-required="true"`) : it was tripping the parent form's submit when those inputs were empty after an Ajouter reset.

### Frontend — Routes (commit `4b5acca`)

- `/dashboard/achats` (`AchatsPage`) : rule-47 search-prompt listing scoped by magasinId. After-create flips the new commande's id into `detailsId` and opens the modal instead of routing.
- `/dashboard/achats/[id]` (`AchatDetailsPage`) : kept for deep-link / bookmark access. Same shared content + thin page chrome.

### Verification

- Backend `./mvnw test -Dtest='AchatControllerTest,AchatServiceImplTest'` : **49 / 49 green** at the final commit. Full suite not re-run end-to-end this session (parked).
- Frontend `vitest run` : **314 / 314 green** at every checkpoint.
- Frontend `tsc --noEmit` : clean (only the pre-existing `FormField.test.tsx` Resolver-typing error, unrelated).
- 13 commits pushed to `origin/dev` : 4 backend (`12f8e2f → ecc4d6e`) + 9 frontend (`30fda07 → 4b5acca`). All atomic per theme.

### Open follow-ups (parked)

- **Cancel "issue refund" workflow** — paiement-blocking cancel is enforced ; the proper refund flow is hors-scope V1. When tackled, also remove the now-orphan `ReceiveAchatDialog` / `useReceiveAchat` / receive endpoints if reception is definitively out.
- **Vitest runnable in env** — Node v20.15.1 vs `std-env` ≥ 20.17 still pre-existing.
- **`FormField.test.tsx` RHF resolver typing error** still untriaged.
- **Full backend test suite re-run** — only the achat slice was verified this session.
- **MR to main** — GitLab is suggesting MR links on both repos when ready.

---

## 2026-05-21 (evening) — Refonte module Abonnement + OWNER Mon abonnement + Plans CRUD admin + MANAGER employee fix

**Subject:** Large, multi-chunk session on the subscription module. The data model swung several times before landing: trial as separate statut on existing Abonnement → reverted → trial as `Entreprise.trialPlan`+`trialEndDate` → reverted → final state where trial is a full Abonnement row with `AbonnementStatut.TRIAL` bound to a `TypePlanAbonnement` flagged `trial=true`. Plus a one-shot subscription login gate, a complete frontend reshape of the catalogue + OWNER self-service + admin views, a MANAGER employee-creation fix, and finally a Plans CRUD page on the admin side.

**Notable decisions:**

### Backend — Subscription module refonte (commit `c50255c`)

- **Entity rename + FK change**: `TypeAbonnement` → `TypePlanAbonnement` with a mandatory `@ManyToOne(optional=false)` to `PlanAbonnement`. Model locked after iteration: **1 Plan ↔ N Types** (one plan has many durations: Monthly / Trimestly / Annual). REST routes nested: `/api/v1/plans/{planId}/types` (CRUD + activate/deactivate).
- **Trial model swing**: ended on trial being a full `Abonnement` row with new `AbonnementStatut.TRIAL`, bound to a `TypePlanAbonnement` flagged `trial=true`. `Entreprise` loses `trialPlan`/`trialEndDate`; only `trialUsed` boolean remains. `DataInitializer.ensureTrialPlanHasDefaultType` seeds a single "Essai" `TypePlanAbonnement` with `trial=true` on the trial plan (uniqueness invariant). `TypePlanAbonnementRepository.findByTrial()` becomes a one-line JPQL `WHERE type.trial = true` returning `Optional` (no ordering / limit needed). `RegisterPropertyServiceImpl.create` → `abonnementService.createTrialForSignup(entreprise)` → `AbonnementDomainService.createTrial` creates the TRIAL Abonnement (dateDebut=today, dateFin=+trialDays, statut=TRIAL, actif=true, renouvellementAuto=false).
- **Login gate**: new `LoginServiceImpl.ensureEntrepriseHasActiveSubscription(principal)` blocks non-ADMIN users whose entreprise has no current Abonnement (ACTIF or TRIAL with `dateFin >= today`). 403 `auth.subscription.required` (FR + EN messages).
- **Migrations V5..V11**: V5 `TypeAbonnement → TypePlanAbonnement` table rename + FK to plan + `TRUNCATE … CASCADE` to bypass the dangling FKs ; V6 permission codes `SUBSCRIPTION_TYPE_*` → `TYPE_PLAN_*` ; V8 trial backfill for the non-trial-used entreprises ; V9 retrial entreprises stuck in limbo (relaxed WHERE) ; V10 widens the `abonnement.statut` CHECK to accept TRIAL + seeds the trial type + migrates legacy `entreprise.trial_plan_id` / `trial_end_date` to TRIAL Abonnement rows + drops the obsolete columns ; V11 adds `type_plan_abonnement.trial` + backfill.
- **Response surface tweaks**: `AbonnementResponse` gains `entrepriseSigle` for the admin list (label-only rendering — rule 43). `CurrentAbonnementResponse` flattened to `{abonnement, joursRestants, fonctionnalites}` — `abonnement` is always populated, the statut distinguishes paid vs trial. `consumeTrialIfAny` simplified to `entreprise.trialUsed = true`.
- **798 / 798 backend tests green** through the chain (including `NonUniqueResult` + `null abonnementService` issue in `PaiementAbonnementServiceImplTest` fixed by mocking `IAbonnementService` after deduplicating `ensureBelongsToCurrentEntreprise`).

### Frontend — Abonnement reshape + OWNER + admin (commits `f4c213d`, `911db81`)

- **Public catalog**: durées nested under each `PublicPlan` (`PlanCard.subscriptionTypes`) — no separate table. Extracted `format-reduction.ts` helper (rule 45) consumed by both `PlanCard` and `SubscriptionTypeTable`. Deleted the old `SubscriptionTypesTable`.
- **Administration → Abonnements**: new list page (`/dashboard/administration/abonnements`) listing every paid + pending + TRIAL Abonnement. Filters entreprise / statut / plan. Aligned with the new `AbonnementResponse.entrepriseSigle` for label-only rendering.
- **Administration → Types d'abonnement**: durée CRUD relocated from the (mis-named) "Abonnements" tab. Header Select picks a plan ; CRUD scoped under that plan via the nested REST `/plans/{planId}/types`. Rule 43 fix on the Plan Select via `<SelectValue>{selectedPlanNom}</SelectValue>` (was leaking the UUID).
- **OWNER Entreprise → Abonnement**: placeholder replaced by `MyAbonnementPage` consuming `/abonnements/me/current`. Branches on `abonnement.statut === 'TRIAL'` (primary-tinted card + sablier icon + jours restants) vs ACTIF (plan + dates + auto-renew toggle). Lists the plan's `fonctionnalites`.
- **Mutation factory**: `useSubscriptionTypeMutation` extracted — shared boilerplate for the 5 hooks (Create / Update / Activate / Deactivate / Delete).
- **Frontend DTOs added**: `Abonnement`, `AbonnementStatut` (now includes `TRIAL`), `AbonnementFilter`, `CurrentAbonnement` (always-populated `abonnement`), `PlanFeatures`, `PlanAdmin`, `PlanAbonnementSummary`, `SubscriptionTypeSummary`.
- **i18n FR + EN**: namespaces `dashboard.administration.abonnements` (admin list), `dashboard.administration.typesAbonnement` (CRUD), `dashboard.entreprise.abonnement` (OWNER), plus `statut.TRIAL`.
- **MANAGER employee-creation bug**: frontend was asking MANAGER to choose a magasin even though their `magasinId` is on the JWT — `useMagasinList` was 403-ing for them (no `STORE_READ`), the empty list triggered `fields.magasinEmptyHint`. Fix: `EmployeForm` reads `user.magasinId` from auth-store, skips `useMagasinList` when locked (`enabled: !isMagasinLocked`), resolves the locked magasin label via `useMagasin(callerMagasinId)`, pre-fills `defaultValues.magasinId`, and renders a read-only field for MANAGER / SELLER. OWNER (`magasinId=null`) keeps the full Select.
- **305 / 305 frontend tests green** at commit time.

### Frontend — Plans CRUD admin (this iteration)

- New `Plans` tab under Administration (gated `ADMIN_ACCESS`, placed between Abonnements and Types d'abonnement). Mirrors the existing TypesAbonnement slice.
- **Domain**: `plan-request.ts`, `plan-admin-filter.ts`, port `IPlanAdminRepository`.
- **Infrastructure**: `plan-admin-api.ts` rewritten as the typed adapter — `listAll` (selector usage, kept for existing callers), `findPage` (paginated + filtered), `findById`, `create`, `update`, `activate`, `deactivate`, `delete`.
- **Application**: query keys factory (`planAdminKeys`) + shared `usePlanAdminMutation` factory (rule 45) ; hooks `usePlanAdminList` / `usePlanAdminPage` / `useCreatePlan` / `useUpdatePlan` / `useActivatePlan` / `useDeactivatePlan` / `useDeletePlan` (the latter uses `removeQueries` for the detail key + `invalidateQueries` on lists, distinct cache strategy from the factory).
- **Presentation**: `PlanAdminFilters` (debounced `nom` search using the **"adjust state in render"** pattern to satisfy `react-hooks/set-state-in-effect` cleanly, no setState-in-effect cascading) + actif/visible Selects ; `PlanForm` (sections identité, description, tarif + limites, fonctionnalités as checkbox group, statut ; `trial` flag **not exposed** — forced `false` on create, preserved on edit, since `DataInitializer` enforces a single trial plan) ; `PlanFormDialog` ; `PlanAdminTable` (row actions: edit / activate-or-deactivate / delete ; XOF-formatted price ; trial badge).
- **Page** at `/dashboard/administration/plans` with the standard LoadingState / EmptyState / NoResults / Pagination plumbing + confirm dialogs for deactivate / delete.
- **i18n FR + EN**: full `dashboard.administration.plans` namespace (filters, table, badges, row actions, form fields / validation, empty / no-results / confirm dialogs / toasts).

### Verification

- Backend `./mvnw test` : **798 / 798 green** at `c50255c`.
- Frontend `vitest run` : **305 / 305 green** at `f4c213d` and `911db81`. Vitest could not run for this final Plans-CRUD chunk because of a pre-existing Node version mismatch in the local env (`v20.15.1` vs `std-env`'s `^20.17.0`) ; eslint + tsc remain clean on every new / modified file.
- Frontend `tsc --noEmit` : no new errors (only the pre-existing `FormField.test.tsx` Resolver-typing issue, unrelated).
- 3 commits pushed to `origin/dev` (1 backend + 2 frontend), 4th commit + push covers this final iteration (TODO + SESSIONS + Plans CRUD).

### Open follow-ups (parked)

- **Paiement abonnement admin** (list + validate + reject) — explicitly stopped after Plans CRUD this iteration. Speculative DTO extension to `PaiementAbonnementResponse` (entreprise/plan labels) and the `paiements` tab were reverted clean. To be resumed later — entail backend `PaiementAbonnementResponse` extension or a join-fetch on the lazy chain `paiement.abonnement.entreprise / .typePlanAbonnement.plan`.
- **Trial uniqueness invariant**: `TypePlanAbonnement.trial` is currently enforced only by `DataInitializer`. Consider a partial unique index `WHERE trial = true` if admin gets ability to toggle the flag later.
- **Vitest runnable in env**: Node v20.15.1 vs std-env requires ≥ 20.17. Either upgrade nvm default or pin std-env to a Node-20.15-compatible version. Pre-existing.
- Administration sub-tabs Coupons / Promotions still placeholders.
- `FormField.test.tsx` RHF resolver typing error still untriaged.
- Prod ADMIN bootstrap via `@ConfigurationProperties`.

---

## 2026-05-21 — route-level PermissionGuard on Entreprise + Administration modules + ADMIN loses OWNER_ACCESS

**Subject:** Short, focused session. Two related changes — one frontend, one backend — both around making the module isolation between SaaS-admin (Administration) and tenant-owner (Entreprise) airtight.

**Notable decisions:**

### Frontend — PermissionGuard on module layouts

- New `PermissionGuard` client component in `common/presentation/shared/PermissionGuard.tsx`. Single prop `requiredPermission`. Returns `null` while `useAuthStore.isHydrated === false` (prevents SSR/CSR mismatch flashes). Delegates the check to the existing `navGuard.canSee({ requiredPermission }, user)` — single decision point for "can this user access this surface", same code path as sidebar / quick-links / row-actions gating.
- Denied path: renders a localized `ForbiddenCard` (ShieldAlert icon + title + description + "Back to dashboard" Link). New i18n keys `common.permissionGuard.{title, description, backToDashboard}` (FR + EN).
- Wraps the two module layouts:
  - `app/(dashboard)/dashboard/entreprise/layout.tsx` with `requiredPermission="OWNER_ACCESS"`
  - `app/(dashboard)/dashboard/administration/layout.tsx` with `requiredPermission="ADMIN_ACCESS"`
- Belt-and-suspenders with the sidebar nav filtering: even if a user pastes a direct URL or has a stale bookmark, the guard intercepts before the page renders.
- 3 new tests (`test/common/presentation/shared/PermissionGuard.test.tsx`): not-hydrated → empty render, permission OK → children, permission KO → ForbiddenCard with Back link.

### Backend — Retire OWNER_ACCESS from ADMIN role

- ADMIN was carrying `OWNER_ACCESS` in `roles-permissions.yml`, which made the Entreprise module visible to SaaS super-admins (sidebar entry + sub-nav). OWNER_ACCESS is meant for company self-service (single OWNER managing their own Entreprise) — granting it to the SaaS super-admin had no functional value and polluted the admin UI.
- Removed `OWNER_ACCESS` from the ADMIN block in YAML.
- Ran `DELETE FROM role_permission WHERE role.libelle='ADMIN' AND permission.code='OWNER_ACCESS'` on the dev DB to flush the existing assignment — `security.rbac.sync` is **additive only** by design (`IRolesPermissionsSyncService.sync` never deletes; logs orphans only), so the seed-time cleanup must be done by hand.
- Live login verified: ADMIN sidebar no longer surfaces the Entreprise module. Direct URL access would be blocked by the new `PermissionGuard` anyway.

### Verification

- Frontend `vitest run` : **293 / 293 green** (+3 vs 290).
- Backend `mvn test` : **774 / 774 green**.
- 2 commits pushed to `origin/dev` (one per repo).

---

## 2026-05-20 — heavy session : RBAC tightening + role rename FR→EN + 3 new modules (Settings + Administration + Entreprise) + data-layer hardening + ADMIN seed + cross-session cache fix + several smell extractions

**Subject:** Massive consolidation day spanning RBAC, UI permission gating, and new modules. Backend got tightened: per-method @PreAuthorize on `MagasinController`, role rename to English (PROPRIETAIRE → OWNER, VENDEUR → SELLER), a `STORE_READ_ONE` permission split, a proper `AccessDeniedException` handler, an ADMIN account seed in `DataInitializer`, a partial-unique-index migration on `person.email/telephone`, and a workaround for a Hibernate 7 `lower(bytea)` inference bug. Frontend got the full permission gating layer (`navGuard` service + `usePermission` hooks), three new modules (Settings, Administration, Entreprise) consolidating multiple CRUD slices each, a cross-session cache leak fix, and several helper extractions per rule 45.

**Notable decisions:**

### Backend — RBAC + per-method authorization

- **Role rename FR → EN** : PROPRIETAIRE → OWNER, VENDEUR → SELLER. The matching access permission code follows : PROPRIETAIRE_ACCESS → OWNER_ACCESS. Case-sensitive sed across all Java + YAML + properties + tests + JPQL. The mixed-case `Proprietaire` entity (User profile sub-type) intentionally untouched — domain noun separate from role identifier. **V3 Flyway migration** renames the existing DB rows in-place via UPDATE — required because the RBAC sync is additive only (creating new role rows would leave orphans + break existing user_role assignments).
- **STORE_READ_ONE split** : new permission declared in the YAML + assigned to all 4 roles. `STORE_READ` stays "list every magasin in your entreprise" (OWNER / ADMIN). `STORE_READ_ONE` = "read the magasin you can access" (every employee). Used by the new profile Affectation card to resolve `magasinId` → name without listing the whole entreprise. `MagasinController` per-method `@PreAuthorize` rewired. `MagasinServiceImpl.findResponseById` + `getLogo` switched from `ensureBelongsToCurrentEntreprise` → `ensureAccessibleByCurrentUser` so an employee can't read a foreign magasin id in the same entreprise.
- **AccessDenied → 403** : new `@ExceptionHandler(AccessDeniedException)` in `GlobalException` returning 403 + `access.denied` i18n key. Without it, Spring 6.1+ method-level @PreAuthorize denials (thrown as `AuthorizationDeniedException` from AOP advice, outside the filter chain) fell through to the catch-all `Exception` handler → 500 "error.unexpected".
- **Dev RBAC_SYNC flipped to true** : `application-dev.yml` default false → true. New permissions / roles in the YAML now land in DB on each dev boot without an env-var dance. Prod stays opt-in explicit.

### Backend — Data layer hardening

- **ADMIN seed** : `DataInitializer.ensureAdminAccount()` creates the SaaS super-admin (`admin` / `passer123` bcrypt) when `security.rbac.sync=true`. Idempotent. Doc note : prod will need `@ConfigurationProperties` exposure when the time comes.
- **person.email / telephone partial unique** (V4 migration) : `@Column(unique = true)` generated strict UNIQUE constraints. PostgreSQL accepts multiple NULLs but rejects multiple `""`, so two clients / suppliers without coordinates collided. V4 drops both constraints + rebuilds as partial unique indexes `WHERE col IS NOT NULL AND col <> ''`. Existing blank values normalized to NULL beforehand.
- **lower(bytea) workaround on Client + Product search** : both repos used bare `:term` parameters in two contexts (`:term IS NULL` + `LIKE LOWER(CONCAT('%', :term, '%'))`). Hibernate 7 inferred the type from the union of contexts and bound bytea → PostgreSQL refused `lower(bytea)`. New `LikePatternHelper.toLikePattern(String)` builds `%term%` lowercase in Java. Repos bind a single pre-formed String (`LIKE :pattern`). Type locked to varchar.
- **ORDER BY createdAt DESC** added to `ClientRepository` (both queries) + `FournisseurRepository`. Without it new rows landed in undefined positions and users believed the save had failed.

### Frontend — Permission UI gating

- **navGuard service** (`common/application/nav-guard.ts`) : `canSee(item, subject)` + `filterVisible(items, subject)`. Single decision point. Decoupled from Zustand → testable without provider. 8 unit tests.
- **usePermission / useHasAnyPermission / useHasAllPermissions** hooks reading the JWT `permissions` claim. Stable Zustand selector (returns `user` whole, not a derived array). 7 unit tests.
- **Mass gating sweep** : sidebar nav + DashboardWelcome QUICK_LINKS + onboarding checklist + magasin MetricCard + MagasinTable row actions all routed through navGuard. Mass case-sensitive rename PROPRIETAIRE → OWNER, VENDEUR → SELLER across auth-store JWT claims, all i18n FR/EN, test fixtures, DTOs.
- **`useMagasinList` `enabled` option** : caller can opt-out of fetch when lacking STORE_READ. Used by DashboardWelcome to avoid 403s for employees.
- **Cross-session cache leak fix** : both `useLogin` AND `useLogout` now call `queryClient.clear()`. Previous symptom : re-login as a different user displayed the previous user's data until manual refresh.
- **Shared Select wrapper** coerces undefined → '' when no `defaultValue`. Kills the base-ui "uncontrolled → controlled" warning globally (rule 44).
- **Rule 45** (smells → extract function) added to conventions. Applied : `runMutationWithToast` + `toastApiError` (10 mutation sites incl. RegisterWizard / LoginForm via the new `onError` extension), `LikePatternHelper`, `blankOptionalsToUndefined`.

### Frontend — Three new modules

- **Settings** (`/dashboard/settings/`) : shell with sub-nav layout + permission-aware index redirect (`_tabs.ts` shared between layout + index). 5 CRUD vertical slices : CategoryProduct, Quality, Product (+ Category Select), Client (+ Magasin Select), Fournisseur (+ reference/origine). Existing `/dashboard/magasins` + `/dashboard/employees` routes relocated under Settings (top-level sidebar entries removed, breadcrumbs updated, DashboardWelcome quick links re-pointed). ~50 new files.
- **Administration** (`/dashboard/administration/`, gated ADMIN_ACCESS) : sub-nav with 4 tabs (entreprises functional + abonnements / coupons / promotions placeholders). Entreprise admin slice : list with 5 text filters + statut + edit + activate/deactivate. No create button — backend `RegisterPropertyRequest` is a wizard, deferred. ~17 new files.
- **Entreprise** (`/dashboard/entreprise/`, gated OWNER_ACCESS) : OWNER self-service with sub-nav 3 tabs (mon-entreprise functional + abonnement / paiements placeholders). Mon entreprise : view + edit identité légale + logo upload/preview/delete via dedicated `my-entreprise-api.ts` and `MyEntrepriseLogoSection`. ~14 new files.

### Frontend — Profile Affectation card

- New section on the profile page's left column showing the user's magasin label. Resolved via `useMagasin(user.magasinId)` (uses `STORE_READ_ONE`). Rule 43 compliance : label only, never the UUID. Hidden for OWNER / ADMIN. Side-by-side label / value layout.

### Frontend — Auth + API normalization fixes

- **UserPrincipal.userId nullable** : backend `UserPrincipalFactory` emits `userId: null` for ADMIN accounts (no `Utilisateur` métier attached). The strict frontend check rejected admin JWTs and bounced them to /login ("connected but no dashboard"). `auth-store.buildUserFromToken` now requires only `accountId / username / role`.
- **blank-to-undefined helper** (`common/infrastructure/blank-to-undefined.ts`) : strips empty optional strings to `undefined` (omitted from JSON → backend reads NULL). Applied to `fournisseurApi` + `clientApi` before POST/PUT. Defense in depth alongside the backend V4 partial index.

### Docs

- `MODULES_OVERVIEW.md` updated : MAGASIN section rewritten with new per-method permissions + STORE_READ_ONE; header bumped to mention the rename (OWNER/SELLER) + new permission count + 2026-05-20 last-updated date.
- `FEATURES.md` : appended use cases 51 (RBAC tightening) + 52 (Data-layer hardening).
- `TODO.md` : 6 done entries under 2026-05-20 (2 backend + 4 frontend).
- Conventions : rule 39 backend / rule 44 + 45 frontend.

### Verification

- Backend `mvn test` : **774 / 774 green** through every commit.
- Frontend `vitest run` : **290 / 290 green** through every commit. `tsc --noEmit` clean (only the pre-existing `FormField.test.tsx` Resolver-typing error remains — unrelated).
- 7 commits pushed to `origin/dev` across both repos.

---

## 2026-05-19 — evening continuation (Magasin CRUD vertical slice, logo UX, backend bugfixes, dev-DB reset, V1 baseline rewrite, auth design pass, phone country selector)

**Subject:** Day 2 of 2026-05-19 — much heavier session than the morning. Shipped the Magasin CRUD as a full vertical slice (data layer + UI layer + logo dialog), and hit + fixed a chain of underlying bugs along the way (lazy-init 500 on logo GET, multipart Content-Type without boundary, route-group URL mismatch, delete-logo cache-race 500). Refactored V1 to be a clean entity-derived baseline and deleted V2..V26 (resolved an inconsistency between V1 snapshot and V17/V18). Bumped Node default to 20.19.5 via the user's .bashrc. Promoted the auth forms to a senior-designer pass (per-page widths, heavier inputs, rhythm, headings). Bumped root font-size 16→17px globally. Added a country-code phone selector backed by libphonenumber-js. Added a unique-email + unique-telephone constraint on Person + a friendly UniqueResourceException pre-check across Proprietaire / Employe / UserProfile services.

For the morning session of the same day, see the earlier journal entry below ("2026-05-19 — morning session").

**Notable decisions:**

### Backend — Person email/telephone uniqueness
- `Person.email` and `Person.telephone` now `@Column(unique=true)`. New Flyway migration V26 adds matching `uk_person_email` / `uk_person_telephone`. Constraint propagates to all `Person` subclasses (Proprietaire, Employe).
- `UtilisateurRepository`: 4 derived methods (`existsByEmail`, `existsByTelephone`, `existsByEmailAndIdNot`, `existsByTelephoneAndIdNot`).
- `UtilisateurDomainService.ensureContactsAvailable` / `ensureContactsAvailableForUpdate` — friendly pre-check throwing `UniqueResourceException` with separate `utilisateur.email.alreadyExists` / `utilisateur.telephone.alreadyExists` i18n keys (FR + EN). Branched into 4 call sites: `ProprietaireServiceImpl.create` (signup), `EmployeServiceImpl.create` + `update`, `UserProfileServiceImpl.updateCurrentProfile`. Pre-check runs **before** `accountService.create` to avoid orphan accounts.
- Tests: new `UtilisateurDomainServiceTest` (6 cases) + ProprietaireServiceImplTest extended with InOrder + dup-email rejection assertion. Backend 772 / 772 green.

### Frontend — Phone country selector
- New `PhoneField` shared component (`common/presentation/shared/`) combining a shadcn Select for country (full ISO list, locale-aware sort via `Intl.DisplayNames` + `Intl.Collator`, flag emoji rendered from country code) and an Input for the national number. Default Senegal (target market).
- Emits a consolidated E.164 string via `onChange` — compatible with backend `@Phone`. Hydrates internal state by parsing an E.164 prop.
- Validation upgraded in RegisterWizard: replaced the E.164 regex with `isValidPhoneNumber` from `libphonenumber-js` (blocks E.164-syntactic-but-impossible numbers per country).
- New dep: `libphonenumber-js` (~15 kb gzipped). PhoneField has 4 dedicated tests.

### nvm default Node 20.19.5
- Discovered Node 20.15.1 was pinned via `NODE_PATH` line 4 in `~/.bashrc` (then added to PATH on line 9), overriding the nvm `default` alias which was already pointing at 20.19.5.
- Edited the dotfile (with explicit user authorization after a classifier block) to remove that hard pin and added `nvm use default --silent` after the nvm.sh source on line 123. New interactive shells now resolve to 20.19.5 cleanly. Verified via fresh `bash -ic` subshell.

### Dev DB reset + V1 rewrite
- Dropped the dev schema cleanly. Boot from V1 → V26 chain failed at V17 because V1 was a forward-looking snapshot that already declared `inventaire` / `rapport_inventaire` / `notification` / `notification_template` tables — V17 / V18 then collided. V1 ALSO had `inventaire.date_inventaire` while the JPA entity expects `inventaire.date` (latent divergence — V1 had never actually been applied to a real environment).
- After exploring 3 recovery options (insert into flyway_schema_history, idempotent IF NOT EXISTS, ddl-auto bootstrap), the user opted for the scorched-earth option: **rewrite V1 as a clean baseline from the actual JPA entities, delete V2..V26**.
- Materialised the canonical schema by booting the app once with `flyway.enabled=false` + `ddl-auto=create`, then `pg_dump --schema-only --no-owner --no-privileges --no-comments`, then a small Python pass to strip the pg_dump preamble + the `flyway_schema_history` table + the `public.` prefix.
- Cleaned V1 (~995 lines, all CHECK constraints + FKs + unique indices including V26's `person_email_key` / `person_telephone_key`). Verified live: empty schema → `./mvnw spring-boot:run` → "Started StoreApplication in 6.7 seconds". 46 tables (45 + `flyway_schema_history`), one row in history (V1, success=true).
- **Production note**: prod's `flyway_schema_history` still references the old V1..V26 with the old V1 checksum. A coordinated `flyway:repair` (or controlled cutover) is required before the next deploy. Flagged in the commit message.

### Magasin CRUD frontend (full vertical slice)
- Backend was already complete (CRUD + activate / deactivate + logo upload / get / delete). This was a frontend-only task.
- **Data layer** (commit `748619a`): 3 DTOs (Request / Response / Filter), `IMagasinRepository` port, `magasin-api` axios adapter (8 endpoints, query-string filters with empty-string + undefined skip-logic), 8 TanStack hooks (list with `keepPreviousData`, detail, create, update, activate, deactivate, upload-logo, delete-logo) + a query-key factory. 9 adapter tests.
- **UI layer** (commit `866f094`): MagasinTable (DataTable + per-row dropdown), MagasinForm (RHF + Zod, reused by create + edit, `disabled` until dirty to avoid no-op PUTs), MagasinFormDialog (mutation dispatch + toast routing), MagasinFilters (nom search + actif Select + reset), MagasinLogoDialog (initial version), and the orchestrator `MagasinsPage`. Full `dashboard.magasins.*` i18n namespace in FR + EN. Tests: 5 form + 3 table.
- **Bug**: page initially landed at `/magasins` instead of `/dashboard/magasins` because of a route-group confusion — Next's `(dashboard)` group doesn't contribute to the URL, so files must live in `app/(dashboard)/dashboard/magasins/`. Fixed via `git mv` in commit `7264523`.

### Magasin logo — UX overhaul (3 rounds of fixes)
- Initial round: the table cell was a 2-state icon placeholder (no real thumbnail). The dialog fetched the blob inside `useEffect` + `URL.createObjectURL` and never picked up cache invalidation after an upload.
- **Round 1 — proper UX rebuild** (commit `1c467a8`): new `useMagasinLogoBlob` hook (TanStack-cached blob URL, keyed by `magasinKeys.logo(id)`, invalidated by upload + delete). LogoCell now renders the real image (skeleton / error / `<img>`). Dialog rewritten with drag-and-drop zone, client-side preview of selected file **before** upload (staged blob URL with "non enregistré" caption), file validation (PNG/JPG/WebP + 2 MB max with inline FR/EN error), confirm-before-delete via `ConfirmDialog`, internal `useMagasin(target.id)` so the dialog auto-refreshes when the cache invalidates. Multipart Content-Type-without-boundary bug fixed (axios needs the user to NOT set Content-Type for FormData so it can generate the boundary). PreviewBlock distinguishes "loading", "error", "no logo defined".
- **Round 2 — lazy-init 500 on GET logo** (backend commit `3e5c15d`): `Magasin.logo` is `@OneToOne(LAZY)` and `PieceJointe.document` is `@Lob @Basic(LAZY)`, but `MagasinServiceImpl.getLogo` had no `@Transactional` — session closed between `findById` and the lazy traversal, throwing on the wire. Added `@Transactional(readOnly=true)`. Same pattern + same fix on `EntrepriseServiceImpl.getCurrentUserLogo` (`UserProfileServiceImpl` already has class-level `@Transactional(readOnly=true)`, no change needed).
- **Round 3 — 500 on delete logo + preview-above-dropzone layout** (commit `67de14c`): the delete mutation invalidated `magasinKeys.logo(id)` which fired a refetch with a closure where `magasin.logo` had become `null` → axios sent `null` to baseURL → `/` → "No static resource for request '/'" 500. Fix: `removeQueries` instead of `invalidateQueries` for the logo key (purges the cache entry, no refetch) + a defensive guard in `useMagasinLogoBlob.queryFn` that throws if `magasin?.logo` is falsy. Same commit reordered the dialog body so the preview now sits **above** the drop zone (per user request).

### Auth forms — senior-designer pass
- Per-page widths: login `max-w-md` (448 px, focus single-column), register `max-w-2xl` (672 px, room for the 2-col grids + the new PhoneField). The `(auth)/layout.tsx` no longer dictates width; each form owns its own.
- Global Input + Select trigger heights bumped `h-8` → `h-10` (32 → 40 px), padding `px-2.5 py-1` → `px-3 py-2`. Significantly nicer affordance app-wide.
- More generous rhythm — card padding `p-6 sm:p-8` → `p-8 sm:p-10`, login gap-5 → gap-6, register gap-6 → gap-7, register inner grids gap-4 → gap-5, FormField label↔input space-y-1.5 → space-y-2.
- Heading scale on desktop: `text-2xl` → `text-2xl sm:text-3xl` for both forms.
- PhoneField country trigger w-28 → w-32 to balance flag + dial code.

### Root font-size 17 px
- One-line CSS bump: `html { font-size: 17px }` in `globals.css`. Every Tailwind `rem`-based utility lifts proportionally (text + spacing + component heights) — design stays in tune.

**Work shipped (post-morning session):**

### Backend (`store/`)
- `18a3cbe` feat(users): unicité email & téléphone au niveau de la table person
- `2febdbf` feat(users): pré-check email & téléphone unique sur création et mise à jour
- `7081277` chore(db): réécriture de V1 comme baseline unique + suppression de V2..V26
- `3e5c15d` fix(magasin,entreprise): @Transactional(readOnly=true) sur getLogo / getCurrentUserLogo

### Frontend (`store-frontend/`)
- `b99c1a8` feat(shared): composant PhoneField avec sélecteur d'indicatif pays
- `02eee63` feat(security): intégrer PhoneField dans le wizard d'inscription
- `3226857` feat(ui): refonte design des formulaires d'authentification
- `87a83fe` feat(ui): bump root font-size à 17px
- `748619a` feat(magasin): couche données — repo, adapter axios, hooks TanStack
- `866f094` feat(magasin): page CRUD complète — table, form modal, logo modal, filtres
- `7264523` fix(magasins): déplacer la page sous /dashboard/magasins (route group)
- `1c467a8` feat(magasin): refonte UX du logo — vraies vignettes, drag-and-drop, états distincts
- `67de14c` fix(magasin): 500 sur suppression du logo + preview au-dessus du dropzone

**Verifications:**
- Backend `./mvnw test`: **772 / 772 green** (765 + 7 new from uniqueness tests).
- Frontend `npx vitest run`: **200 / 200 green** (179 + 17 magasin + 4 PhoneField).
- Live e2e: dev DB reset, fresh boot, login, navigate to `/dashboard/magasins`, create + edit + activate / deactivate + upload + delete logo all working after the round-3 fixes.

**Open follow-ups (post-session):**
- Backend was stopped during the session — restart via `./mvnw spring-boot:run` (under Node-independent JVM, no impact).
- `PageImpl` Spring 3.3+ WARN ("Serializing PageImpl instances as-is is not supported") explicitly accepted as a known annoyance — user opted to leave it rather than migrate to `VIA_DTO` or wrap responses in a custom `PagedResponse<T>`. Three options were proposed; the chosen one is "leave the warning".
- Production migration: the new V1 has a different checksum than what prod's `flyway_schema_history` recorded. Coordinated `mvn flyway:repair` (or a controlled cutover) required before the next deploy.
- The pre-existing `FormField.test.tsx` RHF resolver type error still untriaged.

---

## 2026-05-19 — morning session

**Subject:** Shipped most of the unaccompagned frontend backlog as a series of atomic phases, then built and translated the entire public-facing UI. Five logical chunks landed: (1) auth feature + API error contract realignment + locale switcher + next-intl infrastructure bundled as a single catch-up commit, (2) Phase 2 i18n public marketing, (3) Phase 3 i18n auth surfaces, (4) Phase 4 i18n dashboard, (5) Phase 5 i18n shared sweep. Backend got a one-line security fix for the `/api/v1/catalog/public` 401, plus the new frontend i18n rules registered in `FRONTEND_CODING_CONVENTIONS.md`. Wizard StepMagasin UX bug fixed (ghost errors + Suivant gate). Naming rule 32 reinforced with explicit banned list. Local user-memory updated with a new feedback rule (resume-and-announce) earlier in the session.

### Frontend Phase 1.3 — Auth feature
- **DDD slice** `features/security/` with `IAuthRepository` port, `auth-api` axios adapter, `auth-store` Zustand wrapping localStorage + cookie mirror, `useLogin` / `useRegister` / `useLogout` mutation hooks, plus `LoginForm` + `RegisterWizard` (4-step Compte → Vous → Entreprise → Magasin).
- **JWT decoded client-side** via `decodeJwtPayload` (no dependency, base64url+JSON.parse, exception-free). Claim shape verified live against backend `JwtServiceImpl`: `sub` / `userId` / `username` / `role` / `entrepriseId` / `magasinId` / `permissions`.
- **401 refresh-and-retry** interceptor on `apiClient` with single in-flight refresh promise. Auth endpoints excluded from retry (a 401 on `/auth/login` = wrong password, not an expired session).
- **Frontend Zod schemas mirror the backend Bean Validation contract** strictly: `account.username` 3-50 chars, `account.password` 8-100 chars, `utilisateur.prenom` required, `utilisateur.telephone` matches the E.164 regex `^\+[1-9]\d{1,14}$`, `entreprise.adresse` required (frontend-stricter than backend by user choice).

### API error contract realignment
- Backend really returns `{statusCode, message, errors: [{title, description}]}` — my old parser was reading `errors[i].message` (which doesn't exist), so everything fell back to `errors.unknown`. Verified by reproducing live (POST /auth/register with a bad payload).
- New shape `ApiError = { status, message, fieldErrors: { field, description }[] }` propagates the top-level message + per-field errors faithfully.
- Auth forms now **no banner**: global `message` → `toast.error(...)`, per-field `fieldErrors` → `form.setError(field, ...)` (inline under the matching input). Choice driven by the user during the session.

### Locale switcher
- **User-toggleable FR / EN** via a shadcn Select dropdown in the Navbar and the Dashboard header. Default FR (cible Sénégal).
- Choice persisted in **both** localStorage (read by `apiClient` for `Accept-Language`) and a `store-locale` cookie (read by `next-intl/getRequestConfig` at SSR — same value source of truth, two storage backings for SSR/CSR access).
- `setLocale` triggers `router.refresh()` so Server Components re-render with the new locale without a hard reload.

### Full frontend i18n with next-intl (5 phases)
- Library choice: **next-intl** (canonical Next.js choice). Standard Provider + `useTranslations` (CC) and `getTranslations` (SC).
- Dictionaries live at `src/messages/{fr,en}.json` — moved here from project-root after the initial setup hit a `@/messages/` path resolution issue.
- **22+ surfaces migrated** across phases 2-5: Hero, Features, Faq, FinalCta, Navbar, Footer, Pricing slice (PricingSection, PlanCard, SubscriptionTypesTable, GlobalPromoBanner), About, Contact, LoginForm, RegisterWizard, DashboardShell, DashboardWelcome, ErrorBanner, Pagination, ConfirmDialog.
- Server components became `async function … { const t = await getTranslations('namespace') }`. Client components use `const t = useTranslations('namespace')`.
- Zod schemas in CC re-build via `useMemo(t)` so validation messages stay locale-aware when the user flips languages.
- Shared test helper `src/test/intl-wrapper.tsx` (`renderWithIntl`) wraps tests in `NextIntlClientProvider` with FR messages, passed via the `wrapper` option so `rerender(...)` preserves the intl context.

### Backend `/api/v1/catalog/public` 401 fix
- `SecurityConfig` matcher was `"/api/v1/catalog/public/**"` — that only matches **children** of `/public/`, not the exact `/api/v1/catalog/public` path the controller exposes. So the supposedly-public endpoint was 401-ing.
- One-line fix: `requestMatchers("/api/v1/catalog/public", "/api/v1/catalog/public/**").permitAll()`.
- 765 / 765 backend tests still green after the change.

### StepMagasin wizard UX fix + rule 32 reinforcement
- **Bug**: on a fresh wizard step, inline Zod errors appeared on fields the user hadn't touched yet. Root cause: `FormField` rendered `fieldState.error.message` unconditionally — even though the form is in `mode: 'onTouched'`, the error map was already populated (most likely from a previous `form.trigger(...)` on Suivant click of an earlier step).
- **Fix A (FormField)**: only render inline error if `fieldState.error && (fieldState.isTouched || formState.isSubmitted)`. Submit-attempt path still surfaces backend `setError` calls because `isSubmitted` becomes true after `handleSubmit` calls `onSubmit`.
- **Fix B (RegisterWizard)**: `Suivant` / `Créer mon compte` button disabled when any required field of the current step is empty. Implemented via `form.watch()` + a per-step `STEP_REQUIRED_FIELDS` map. Full Zod validation still runs on click as a backstop.
- **Rule 32 (explicit variable names)** reinforced: explicit banned list (`ok`, `result`, `tmp`, `res`, `val`, `data`, `dto`, `obj`, `cfg`, `evt`, single letters), explicit "name setter callback args after the value not generically as `prev`/`current`" (`previousIndex` for `setStepIndex`). Tolerated library idioms ring-fenced: `t` for next-intl translator, `state` for Zustand selector arg, `error` in catch, `i` for loop index. Concrete applications: `const ok` → `const isStepValid` in `goNext`, `const data` → `const errorPayload` in `parseApiError`.

### New durable feedback memory
- [[feedback_resume_and_announce]] — every new turn / chunk: short recap of the last task, then `📋 NEXT ACTION` overview, then stop and wait for "go". Saved earlier in the session.

**Work shipped (commits since `65c92b8` baseline on the frontend):**

### Frontend (`store-frontend/`)
- `2c3a63e` feat(security,i18n): auth feature + API error contract + locale switcher + next-intl
- `a30396d` feat(i18n): translate public marketing surfaces (Phase 2)
- `0b9065d` feat(i18n): translate auth surfaces (Phase 3)
- `5a1354b` feat(i18n): translate dashboard shell + welcome (Phase 4)
- `93dd814` feat(i18n): translate shared components (Phase 5 — final sweep)
- (pending this commit) chore + StepMagasin fix + rule 32 refactor

### Backend (`store/`)
- (pending this commit) `SecurityConfig` `/catalog/public` matcher fix + rule 32 update in `FRONTEND_CODING_CONVENTIONS.md` + TODO + this SESSIONS entry

**Verifications:**
- Backend `./mvnw test`: **765 / 765 green** (BUILD SUCCESS) after the SecurityConfig change.
- Frontend `npx vitest run`: **179 / 179 green** at every commit point throughout the session.
- Frontend `tsc --noEmit`: clean modulo the pre-existing `FormField.test.tsx` RHF resolver type mismatch (predates this session, untouched).
- Live e2e against the running backend confirmed: register → login → /entreprises/me → refresh → logout → refresh-revoked-401 chain works end-to-end.
- Live locale flip via `Accept-Language: fr / en` confirmed: backend i18n keys translate, frontend dictionary toggles, `router.refresh()` re-renders SC without a full page reload.

**Open follow-ups:**
- Backend was stopped during the session (Ctrl-C requested). Restart via `./mvnw spring-boot:run` to pick up the SecurityConfig fix.
- Phases 2-5 frontend commits are local-only — need an explicit push.
- Pre-existing `FormField.test.tsx` TS error (RHF generic resolver) still untriaged.
- Auth flow doesn't yet hit a real protected feature page (no Magasin / Produit / Vente CRUD yet on the frontend). Natural next chunk: Phase 2 of the bootstrap checklist (Employe CRUD or Magasin CRUD as the first feature dashboard page).
