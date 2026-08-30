# Design — Country-aware payment methods + subscription billing configuration (Facturation)

**Date:** 2026-08-30
**Scope:** two independent, decoupled tasks, documented together because they were brainstormed in the same session and both touch payment-method selection. **They will be implemented and delivered as 2 separate plans/commit series, in either order** — Task 2 does not depend on Task 1's schema.

- **Task 1** — make the existing shared `MoyenPaiement` entity country-aware, and filter its selection everywhere it's picked *except* subscription payment: vente, achat, dépense (tenant), and the admin "Dépenses plateforme" module.
- **Task 2** — a new `Facturation` reference table (moyen + pays + numéro) that entirely replaces how the subscription-payment submission flow (`SubmitPaiementForm`) picks a payment method today.

## 0. Problem statement

Today, `MoyenPaiement` (`org.store.paiement.domain.model.MoyenPaiement`, table `moyen_paiement`) is a single shared, DB-backed reference entity (`libelle`, `code`, `actif`) with **no country awareness at all**. It's used, via a plain FK, by `PaiementVente`, `PaiementAchat`, `Depense`, `DepensePlateforme`, and `PreuvePaiement` (introduced by migration `V41`). Every active `MoyenPaiement` row is shown to every user regardless of their entreprise's country — e.g. a Guinea-based entreprise's vente/achat/dépense/abonnement forms all show "Wave" as a selectable option even where Wave has no real presence, because nothing in the schema or the selection endpoints encodes "this payment method only exists in these countries."

`Country` (`org.store.country.domain.model.Country`, table `country`) already exists and already scopes `Entreprise` (mandatory FK, `Entreprise.country`), so the entreprise's country is always known.

## 1. Task 1 — Country-aware `MoyenPaiement`

### 1.1 Data model

`MoyenPaiement` gains a many-to-many relation to `Country`, **not** a single nullable FK — a payment method like Wave or Orange Money can legitimately operate in several countries at once (e.g. Wave in Sénégal *and* Guinée), and a single-FK model would force duplicate entries ("Wave Sénégal", "Wave Guinée") for the same real-world payment method. An empty relation = **global** (available everywhere) — this is the default/backward-compatible state for all 4 currently-seeded moyens (Cash, Wave, OM, Card), so nothing changes for them unless an admin explicitly attaches countries.

New join table, new migration (next available Flyway version at implementation time — `V91` if this task ships first):
```sql
CREATE TABLE moyen_paiement_pays (
    moyen_paiement_id UUID NOT NULL REFERENCES moyen_paiement(id) ON DELETE CASCADE,
    country_id        UUID NOT NULL REFERENCES country(id) ON DELETE CASCADE,
    PRIMARY KEY (moyen_paiement_id, country_id)
);
```

`MoyenPaiement` entity:
```java
@ManyToMany
@JoinTable(
    name = "moyen_paiement_pays",
    joinColumns = @JoinColumn(name = "moyen_paiement_id"),
    inverseJoinColumns = @JoinColumn(name = "country_id")
)
private Set<Country> pays = new HashSet<>();
```

### 1.2 Backend — admin CRUD (`MoyenPaiement`)

- `MoyenPaiementRequest` gains `Set<UUID> paysIds` (nullable/empty allowed — no `@NotEmpty`).
- `MoyenPaiementResponse` gains `List<CountrySummaryResponse> pays` (or reuse whatever summary shape `Country` already has elsewhere — check for precedent before adding a new one).
- `MoyenPaiementServiceImpl.create`/`update`: resolve each id via `ICountryService`/`CountryDomainService.findById`, build the `Set<Country>`.
- No change to `activate`/`deactivate`/`delete`/permissions (`MOYEN_PAIEMENT_{CREATE,UPDATE,DELETE}` — unchanged; the plain `GET /api/v1/moyens-paiement` stays permission-free, unchanged, still returns everything unfiltered for the admin CRUD table).

### 1.3 Backend — new country-filtered select endpoint

New endpoint, **separate from the existing CRUD listing** (established project convention — never mix a CRUD listing with a selection endpoint):

```
GET /api/v1/moyens-paiement/select?countryId=&q=&page=&size=
```

Behavior:
- `countryId` provided (any caller) → `actif = true AND (pays is empty OR pays contains :countryId)`.
- `countryId` absent → resolve the current user's entreprise country automatically via `UserPrincipal.entrepriseId()` (already populated for both OWNER and EMPLOYE by the existing `ProprietairePrincipalContextStrategy`/`EmployePrincipalContextStrategy`) → `IEntrepriseService`/domain lookup → `entreprise.getCountry().getId()` — then filter the same way.
- `countryId` absent **and** the current user has no `entrepriseId` (ADMIN) → no filtering, return everything active (matches the "Dépenses plateforme" requirement in 1.4).

Response: `Page<DataSelect>` (reuse `org.store.common.dto.DataSelect`, no new per-entity DTO — same pattern just established for `CategoryDepensePlateforme`'s `/select` endpoint).

No specific `@PreAuthorize` beyond authentication — matches the existing unguarded `GET /api/v1/moyens-paiement`.

**Explicitly no server-side write-time validation** (confirmed by the user): `PaiementVente`, `PaiementAchat`, `Depense`, `DepensePlateforme` creation paths keep accepting any active `moyenPaiementId` they're given — this is a UI-level convenience filter only, not a security boundary.

### 1.4 Frontend

New hook `useMoyenPaiementSelectList(countryId?: string, q = '', page = 0, size = 10, enabled = true)` in `features/moyen-paiement/application/`, mirroring `useEntrepriseSelectList`'s signature/behavior (`keepPreviousData`, no debounce), calling the new `/select` endpoint.

**`MoyenPaiementSelect.tsx`** (`features/moyen-paiement/presentation/`) — the one shared component already used by every consumer below — switches from `useMoyenPaiementList()` to `useMoyenPaiementSelectList()`. All of its current consumers benefit automatically, **except `SubmitPaiementForm.tsx`**, which Task 2 removes from this list entirely:

| File | Module | Country source |
|---|---|---|
| `features/vente/presentation/VenteForm.tsx` | Vente | auto (current user's entreprise) |
| `features/vente/presentation/ValiderVenteDialog.tsx` | Vente | auto |
| `features/vente/presentation/CreatePaiementVenteDialog.tsx` | Vente | auto |
| `features/achat/presentation/ReceiveAchatDialog.tsx` | Achat | auto |
| `features/achat/presentation/CreatePaiementAchatDialog.tsx` | Achat | auto |
| `features/depense/presentation/DepenseForm.tsx` | Dépense (tenant) | auto |
| `features/depense/presentation/DepenseFilters.tsx` | Dépense (tenant) | auto |
| `features/plateforme-depense/presentation/DepensePlateformeForm.tsx` | Dépense plateforme (admin) | **explicit, from the form's own `pays` field** (see 1.5) |
| `features/plateforme-depense/presentation/DepensePlateformeFilters.tsx` | Dépense plateforme (admin) | **explicit, from the filter's own `pays` field** |
| ~~`features/abonnement/presentation/SubmitPaiementForm.tsx`~~ | Abonnement | **removed from this component's consumer list — Task 2** |

`MoyenPaiementFormDialog.tsx` (admin CRUD, `features/moyen-paiement/presentation/`) gains a multi-select `Country` field (reuses `useCountries()`), and `MoyenPaiementTable.tsx` gains a "Pays" column (comma-separated names, or a "Global" badge when empty).

### 1.5 "Dépenses plateforme" — confirmed behavior

Per explicit user answer: *"si [l'admin] ne sélectionne pas pays il voit tout ; s'il sélectionne pays, il voit les moyens globaux + les moyens du pays sélectionné."* `DepensePlateformeForm`/`DepensePlateformeFilters` already have their own optional `pays` field (independent of any "current entreprise" — this module is global/admin, not tenant-scoped). `MoyenPaiementSelect`'s `countryId` prop is wired to **that form's own currently-selected `pays` value**, live:
- no country selected in the form → `MoyenPaiementSelect` calls the select hook with no `countryId` → since the caller is ADMIN (no `entrepriseId`) → unfiltered, matches "voit tout".
- country selected in the form → `countryId` passed explicitly → global + that country's moyens.

### 1.6 Explicitly out of scope for Task 1

- `SubmitPaiementForm.tsx` / any subscription-payment selection logic — entirely Task 2's responsibility.
- Any server-side rejection of a "wrong-country" `moyenPaiementId` on write — confirmed not wanted.
- Any change to `PaiementVente`/`PaiementAchat`/`Depense`/`DepensePlateforme` entities themselves.

## 2. Task 2 — `Facturation` (subscription payment billing configuration)

### 2.1 Data model

New entity, global/platform-level (no `Entreprise`/`Magasin` FK — like `PlanAbonnement`, `MoyenPaiement`, `Country`), placed in `org.store.paiement.domain.model` (sibling of `MoyenPaiement`, the entity it directly configures):

```java
@Entity
@Table(name = "facturation")
public class Facturation extends AuditableEntity {

    @ManyToOne(optional = false)
    private MoyenPaiement moyenPaiement;

    @ManyToOne(optional = true)
    private Country pays;                 // nullable = global, valid for every country

    @Column(nullable = false, length = 100)
    private String numeroFacturation;

    @Column(nullable = false)
    private boolean actif = true;
}
```

Uniqueness — one billing number per (moyen, pays) pair, confirmed by the user, **including** the global case (at most one global `Facturation` row per moyen). Because standard SQL `UNIQUE` treats `NULL` as distinct from any other `NULL`, this needs **two** constraints, mirroring the existing `person_email_key`/`person_telephone_key` partial-unique-index pattern (`V4` migration):

```sql
-- migration: next available Flyway version at implementation time (V92 if Task 1 ships first)
CREATE TABLE facturation (
    id                  UUID PRIMARY KEY,
    moyen_paiement_id   UUID NOT NULL REFERENCES moyen_paiement(id),
    pays_id             UUID NULL REFERENCES country(id),
    numero_facturation  VARCHAR(100) NOT NULL,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP, updated_at TIMESTAMP, created_by VARCHAR(255), updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX facturation_moyen_pays_key
    ON facturation (moyen_paiement_id, pays_id)
    WHERE pays_id IS NOT NULL;

CREATE UNIQUE INDEX facturation_moyen_global_key
    ON facturation (moyen_paiement_id)
    WHERE pays_id IS NULL;
```

### 2.2 Backend — admin CRUD (`Facturation`)

New module files (mirrors the `MoyenPaiement`/`plateforme` reference-entity pattern): `domain/repository/FacturationRepository` (port) + `infrastructure/repository/FacturationJpaRepository` (adapter), `domain/service/FacturationDomainService`, `application/dto/{FacturationRequest,FacturationResponse}`, `application/service/{IFacturationService,FacturationServiceImpl}`, `presentation/FacturationController` → `/api/v1/facturations`.

- `FacturationRequest(UUID moyenPaiementId, UUID paysId /* nullable */, @NotBlank @Size(max=100) String numeroFacturation)`.
- `FacturationResponse(UUID id, MoyenPaiementSummary moyenPaiement, CountrySummaryResponse pays /* nullable */, String numeroFacturation, boolean actif)` — reuse whatever `Country` summary shape already exists elsewhere in the codebase (check for precedent before adding a new one; same note applies to `MoyenPaiementResponse.pays` in 1.2).
- CRUD: create (validates the (moyen, pays) uniqueness — including the "global" case — with a friendly i18n error, not just a raw DB constraint violation), update, activate/deactivate (soft-disable, matching `MoyenPaiement`'s own pattern — **no hard delete**, consistent with the project's established soft-delete convention for reference entities), paginated list with `moyenPaiementId`/`paysId`/`actif` filters (rule 40: `createdStartDate/EndDate` + `ORDER BY createdAt DESC`).
- New permissions, ADMIN-only in `roles-permissions.yml` (mirrors `MOYEN_PAIEMENT_*` naming, sibling entity): `FACTURATION_{CREATE,READ,UPDATE,DELETE}`.

### 2.3 Backend — subscription-payment read endpoint

```
GET /api/v1/facturations/select
```
- No `countryId` param — always resolves the **current user's** entreprise country server-side (same resolution as 1.3, reused).
- Filter: `actif = true AND (pays.id = :countryId OR pays IS NULL)`.
- Response: a small dedicated DTO (not `DataSelect` — the frontend needs to display the billing number, not just an id/label pair), e.g. `FacturationOptionResponse(UUID facturationId, String moyenLibelle, String numeroFacturation)`.
- Permission: `SUBSCRIPTION_PAY` (same permission already guarding `POST /api/v1/paiements-abonnement/{id}/payer`) — not `FACTURATION_READ`, which stays ADMIN-only.

### 2.4 Backend — `PreuvePaiement` submission changes

- `PreuvePaiementRequest`: `moyenPaiementId` → **`facturationId`** (the owner now picks a specific billing line, not a raw payment method).
- `PreuvePaiementServiceImpl.create`: resolve `Facturation` by id (`facturationService.findById(...)`, `EntityException("facturation.notFound")` if missing/inactive), then `preuve.setMoyen(facturation.getMoyenPaiement())` — `PreuvePaiement.moyen` (existing FK, unchanged shape) is set from the resolved facturation's moyen. **No new column on `PreuvePaiement`** — the chosen `Facturation` is not stored for history, only its `moyenPaiement` (matches today's behavior, where only the moyen is recorded, not "which billing line the owner was shown").
- Add a country/entreprise consistency check (not a hard security wall per se, just correctness): the resolved `Facturation.pays` must be `null` or match the current entreprise's country, else `BadArgumentException("facturation.notAvailableForCountry")` — this one **is** worth validating server-side since it's the actual money-collection destination, not a cosmetic filter like Task 1's; flagging this divergence from Task 1's "no server validation" call explicitly for the user's spec review.

### 2.5 Frontend

New `features/facturation/` slice (admin CRUD, mirrors `features/moyen-paiement/` exactly): DTOs, repository port, api adapter, TanStack hooks (one per file, rule 52), `FacturationTable`/`FacturationFormDialog`/`FacturationRowActions`, new admin page. Exact sidebar placement (a tab alongside "Moyens de paiement" under Administration, vs. its own top-level module) is navigation wiring, not a design decision — resolved during plan-writing by following existing sidebar conventions, not requiring a fresh user decision.

**`SubmitPaiementForm.tsx`**: drops `MoyenPaiementSelect` entirely. New `FacturationSelect` component (or inline `Select`) backed by a new `useFacturationOptions()` hook (`GET /api/v1/facturations/select`, no params — server resolves the country). Each option renders `moyenLibelle` in the trigger; once selected, the form displays `numeroFacturation` prominently (e.g. a highlighted "Envoyez votre paiement au numéro : {numero}" line) so the owner knows where to send funds before uploading proof. Submit payload changes from `{moyenPaiementId, referenceTransaction}` to `{facturationId, referenceTransaction}`.

### 2.6 Explicitly out of scope for Task 2

- Historical tracking of which exact `Facturation` line an owner was shown/picked (only the resulting `moyenPaiement` is kept on `PreuvePaiement`, as today).
- Any change to `MoyenPaiement`'s own country relation from Task 1 — `Facturation.pays` is independent, per the user's explicit call to decouple the two tasks.
- Currency-aware formatting of `numeroFacturation` — it's a plain string (a phone number or merchant reference), no format validation beyond `@NotBlank`/max length.

## 3. Testing

- **Task 1**: `MoyenPaiementServiceImplTest` (create/update with `paysIds`), a new `MoyenPaiementSelectServiceImplTest`-equivalent (or the same test class) covering all 3 branches (`countryId` provided / auto-resolved tenant / ADMIN-no-country), `MoyenPaiementControllerTest` for the new endpoint, `DepensePlateformeServiceImplTest`/controller — no server-side behavior change so no new backend tests strictly needed there beyond confirming the existing unfiltered create/update paths still pass. Frontend: hook test for `useMoyenPaiementSelectList`, a couple of consumer smoke-tests (e.g. `DepensePlateformeForm` re-queries when `pays` changes).
- **Task 2**: `FacturationServiceImplTest` (CRUD + uniqueness incl. the global-row case), `FacturationControllerTest`, `PreuvePaiementServiceImplTest` updated for `facturationId` resolution + the country-consistency check (2.4), migration V92 partial-index behavior sanity-checked with a raw SQL test or a repository-level duplicate-insert test. Frontend: `SubmitPaiementForm` test updated for the new field/payload, new `features/facturation/` slice gets the same test coverage shape as `features/moyen-paiement/`.

## 4. Decisions log (confirmed live with the user, 2026-08-29/30)

1. Server-side validation on Task 1's country filter: **not wanted** — UI-list filtering only (this decision does **not** extend to Task 2's own country-consistency check, which the user has not yet reviewed — see 2.4).
2. `MoyenPaiement` ↔ `Country`: **many-to-many**, not a single nullable FK — avoids duplicating "Wave Sénégal"/"Wave Guinée" as separate moyens.
3. Country-aware filtering applies **everywhere** `MoyenPaiement` is picked (vente, achat, dépense tenant, dépenses plateforme admin) — **except abonnement**, which uses `Facturation` instead.
4. "Dépenses plateforme": admin sees everything with no country selected; global + that country's moyens once a country is picked in the form/filter.
5. `Facturation` is a **global platform-level** reference (ADMIN-only), not per-Entreprise.
6. Entity name kept as **"Facturation"** despite the naming-overlap risk with existing `Facture*` entities — explicit user choice.
7. One billing number per (moyen, pays) pair, **including** the global (`pays IS NULL`) case.
8. `Facturation.pays` is **optional** — a global billing number (e.g. a universal card gateway) is allowed.
9. **Confirmed 2026-08-30 (post Task 1 ship)**: unlike Task 1, `Facturation` selection on subscription-payment submission **does** get server-side validation — `PreuvePaiementServiceImpl` rejects (400) if the resolved `Facturation.pays` is neither null nor equal to the current entreprise's country. This is the real money-collection destination, not a cosmetic filter.
