# Design — SaaS platform P&L: dépenses, revenus, bénéfice

**Date:** 2026-08-23
**Scope:** the SaaS vendor's own finances (platform operator), **not** a per-store/per-tenant P&L. Store-level `Depense`/`CategoryDepense` already exist and are untouched by this design. **This entire feature is additive**: nothing in the existing admin reporting surface (`AdminReportingController`, `PeriodReportResponse`, `AdminOverviewStatsResponse`, `PeriodTab.tsx`, …) or in `PaiementAbonnementServiceImpl` is modified beyond one new line publishing a new event — see 2.2 and 5.

## 1. Goal

Give the ADMIN a period-scoped P&L view of the SaaS business itself:
- **Revenus** = validated subscription payments (`PaiementAbonnement`, statut `VALIDE`)
- **Dépenses** = the vendor's own operating costs (hosting, tools, salaries…) — new concept, doesn't exist yet, optionally attributable to a `country` (some costs are market-specific, some are shared/global)
- **Bénéfice** = revenus − dépenses, scoped the same way on both sides: global when no `country` filter is set, per-country when one is

Selectable over month / quarter / year / custom range, with optional `country` and `abonnement` filters on the revenue side.

## 2. Backend

### 2.1 New module `org.store.plateforme` — platform expenses

Mirrors the existing `depense` module structure, but **global** (no `Entreprise`/`Magasin` FK — admin-only, like `PlanAbonnement`).

```
plateforme/
├── domain/
│   ├── model/CategoryDepensePlateforme.java   # nom (unique), description, actif
│   ├── model/DepensePlateforme.java           # category FK, libelle, description,
│   │                                          # dateDepense, montant, modePaiement FK (MoyenPaiement, reused as-is),
│   │                                          # country FK (nullable — null = global/shared cost)
│   ├── repository/{CategoryDepensePlateforme,DepensePlateforme}Repository.java
│   └── service/{CategoryDepensePlateforme,DepensePlateforme}DomainService.java
├── infrastructure/repository/…JpaRepository.java
├── application/
│   ├── dto/  # Request/Response/Filter/Summary/TotalResponse
│   │         # same shape as the existing Depense DTOs, minus magasinId/entrepriseId
│   │         # no ParCategorieResponse/by-category — not part of the requested scope (3 KPI cards only, see 3.3); YAGNI
│   └── service/I{…}Service + impl
└── presentation/
    ├── CategoryDepensePlateformeController  → /api/v1/admin/plateforme/expense-categories
    └── DepensePlateformeController          → /api/v1/admin/plateforme/depenses (+ /total)
```

Both entities extend `AuditableEntity`. `DepensePlateformeFilter` carries `categoryId`, `moyenPaiementId`, `countryId`, `libelle`, `startDate`, `endDate`, `page`, `size` (rule 40: `createdStartDate/EndDate` + `ORDER BY createdAt DESC` also applied) — same pattern as `DepenseFilter` minus `magasinId`, plus `countryId`. When `countryId` is set, only rows with a matching `country` are returned — global (`country IS NULL`) rows are excluded from a country-specific view, same semantics as the `Revenu` filter below.

**Permissions** (new, declared + granted to ADMIN only in `roles-permissions.yml`; **not** added to `PermissionCode.java` — that enum only holds permissions Java code checks programmatically via `hasPermission(PermissionCode.X)`, e.g. `ADMIN_ACCESS`/`SUBSCRIPTION_*`; `@PreAuthorize` uses raw string literals and most existing permissions, including `EXPENSE_*`/`REPORT_FINANCIAL`, aren't in that enum either):
`PLATFORM_EXPENSE_{CREATE,READ,UPDATE,DELETE}`, `PLATFORM_EXPENSE_CATEGORY_{CREATE,READ,UPDATE,DELETE}`.

### 2.2 New entity `Revenu` — validated subscription revenue

Placed in `org.store.abonnement.domain.model` (same rationale as `SortieStock` living next to `EntreeStock` in `stock`).

```java
@Entity
@Table(name = "revenu")
public class Revenu extends AuditableEntity {
    @ManyToOne(fetch = LAZY, optional = false)
    private Entreprise entreprise;      // drill-down + abonnementId filter resolution (see below)

    @ManyToOne(fetch = LAZY, optional = false)
    private Country country;            // snapshotted at validation time

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;         // = paiement.montantFinal

    @Column(nullable = false)
    private LocalDate datePaiement;     // = paiement.datePaiement — real cash-in date, drives period bucketing
}
```

No FK back to `PaiementAbonnement` — `entreprise` is enough for drill-down, and `montant`/`datePaiement` are copied at creation time so the row is self-sufficient. By construction, a `Revenu` row exists **only** for payments that reached `VALIDE` (see Creation below, which fires solely from `validate()`) — it can never contain pending/rejected amounts, which is the whole point ("ce sont les vraies rentrées d'argent").

**Creation — fully additive, via a new event.** `PaiementAbonnementServiceImpl.validate()` already injects `INotificationEventPublisher`, whose interface already exposes a generic `publishEvent(Object event)` (used today by `AlertScheduler`) alongside its typed `publish*` methods — so no change is needed to `INotificationEventPublisher`/`NotificationEventPublisher`. `validate()` gets exactly **one new line**, right after the existing `publishPaiementValidated(...)` call:

```java
notificationEventPublisher.publishEvent(new RevenuRecordedEvent(
        abonnement.getEntreprise().getId(),
        abonnement.getEntreprise().getCountry().getId(),
        validatedPaiement.getDatePaiement(),
        validatedPaiement.getMontantFinal()));
```

**Why ids, not the `Entreprise` entity:** this codebase already hit `IllegalStateException: Illegal pop()` once on these exact payment events (`PaiementAbonnementSubmittedEvent`/`ValidatedEvent`/`RejectedEvent`, fixed 2026-06-21) from passing JPA entities into `@Async` listeners and touching a lazy field after the originating Hibernate session had closed. `Entreprise.country` is `@ManyToOne(fetch = LAZY)`, so the same class of bug would resurface here. `getCountry().getId()` is read **inside** `validate()`'s own transaction (safe, synchronous) — only the resulting `UUID`s cross into the async listener.

New event record (new file, same package/convention as the existing `PaiementAbonnement*Event` records):
```java
package org.store.notification.application.event;
public record RevenuRecordedEvent(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant) {}
```

New listener (new file, e.g. `org.store.abonnement.application.listener.RevenuEventListener`), mirroring the existing `@Async @EventListener` pattern used by `NotificationEventListener`:
```java
@Async
@EventListener
public void onRevenuRecorded(RevenuRecordedEvent event) {
    revenuService.record(event.entrepriseId(), event.countryId(), event.datePaiement(), event.montant());
}
```

`IRevenuService.record(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant)` builds the `Revenu` row using `EntityManager.getReference(Entreprise.class, entrepriseId)` / `getReference(Country.class, countryId)` for the two FKs — no query, just a proxy reference, since JPA only needs an id to set a `@ManyToOne`. No business-layer lookup (`IEntrepriseService.findById` or similar) is needed at all. Net diff to existing files: **one added line** in `PaiementAbonnementServiceImpl.validate()`. Everything else (event, listener, service) is new. `PaiementAbonnementServiceImplTest` gets one new assertion that `publishEvent(...)` was called with a `RevenuRecordedEvent` carrying the right ids.

**Reads** (`IRevenuService`) — used only by the new endpoint in 2.3, `AdminReportingController`/`sumValidatedRevenueForYear` are untouched and keep computing `revenueYtd` their own (existing) way. `IRevenuService.getTotalForPeriod(String startDate, String endDate, UUID countryId, UUID abonnementId)` → `BigDecimal`:
- when `abonnementId` is set, the service (a plain synchronous request-scoped call — no async/lazy concern here) first resolves it to an `entrepriseId` via `IAbonnementService.findById(abonnementId).getEntreprise().getId()` (an `Abonnement` is a 1:1 permanent contract per entreprise, per the 2026-07-23 subscription redesign)
- it then calls `RevenuRepository.sumByPeriod(startDate, endDate, countryId, entrepriseId)` — the repository-layer query itself only ever knows `entrepriseId`, never `abonnementId`

JPQL (`RevenuRepository.sumByPeriod`, domain port + JPA adapter):
```
SELECT COALESCE(SUM(r.montant), 0)
FROM Revenu r
WHERE r.datePaiement BETWEEN :startDate AND :endDate
  AND (:countryId IS NULL OR r.country.id = :countryId)
  AND (:entrepriseId IS NULL OR r.entreprise.id = :entrepriseId)
```

### 2.3 New, separate reporting endpoint — nothing existing touched

`AdminReportingController`, `PeriodReportResponse`, `AdminOverviewStatsResponse`, `getStatistiquesPaiement`, `PaiementAbonnementStatsResponse`, `sumValidatedRevenueForYear` — **all stay exactly as they are today.** This feature ships as its own controller/service/DTOs, additive only, living next to the `plateforme` module from 2.1:

```
GET /api/v1/admin/plateforme/reporting/period?startDate=&endDate=&countryId=&abonnementId=
```

New filter record, `org.store.plateforme.application.dto`:
```java
public record PlateformePeriodFilter(String startDate, String endDate, UUID countryId, UUID abonnementId) {}
```

New response record:
```java
public record PlateformePeriodReportResponse(BigDecimal revenu, BigDecimal depensesPlateforme, BigDecimal benefice) {}
```

New `IPlateformeReportingService` + impl, new `PlateformeReportingController` (permission: new `PLATFORM_REPORT_READ`, **ADMIN only** — NOT `REPORT_FINANCIAL`: that permission is also granted to OWNER and MANAGER in `roles-permissions.yml` today, for a future store-level report page, and `AdminReportingController` itself has no extra ADMIN-only check beyond it — reusing it here would let a store owner read the vendor's own P&L. Discovered during planning; not fixed here since it's outside this feature's existing-code, but not repeated either):
```java
BigDecimal revenu = revenuService.getTotalForPeriod(filter.startDate(), filter.endDate(), filter.countryId(), filter.abonnementId());
BigDecimal depensesPlateforme = depensePlateformeService.computeTotal(filter.startDate(), filter.endDate(), filter.countryId());
BigDecimal benefice = revenu.subtract(depensesPlateforme);
```

**Filter scope per KPI** (deliberately not uniform — see rationale below):

| Field | Scoped by |
|---|---|
| `revenu` | `startDate`, `endDate`, `countryId`, `abonnementId` (via `Revenu`) |
| `depensesPlateforme` | `startDate`, `endDate`, `countryId` — **not** `abonnementId` (a platform expense has no client/subscription dimension) |
| `benefice` | = `revenu` (scoped as above) − `depensesPlateforme` (scoped as above) — global on both sides when `countryId` is absent, per-country on both sides when it's set |

Rationale: `depensesPlateforme` has a `country` (2.1) but no notion of "which subscription" — so `abonnementId` never touches it. `benefice` simply mirrors whatever `country` scoping was applied to its two operands, so it reads as a real P&L slice per market when a country is picked, and as the whole-platform P&L otherwise (confirmed). `IDepensePlateformeService.computeTotal(startDate, endDate, countryId)` sums `DepensePlateforme.montant` with the same `(:countryId IS NULL OR country.id = :countryId)` clause used everywhere else.

The existing `nouveauxAbonnements`/`paiementsValides`/`paiementsRejetes` KPIs on `AdminReportingController`'s `/period` endpoint are simply not part of this new endpoint's response — that endpoint and its DTO are untouched, so those fields remain available there exactly as before for whoever still wants them.

### 2.4 Migrations

- **V85** — `category_depense_plateforme` + `depense_plateforme` tables (audit columns, FK `depense_plateforme.category_id` → `category_depense_plateforme`, FK `depense_plateforme.moyen_paiement_id` → existing `moyen_paiement`, FK `depense_plateforme.country_id` → existing `country`, **nullable**).
- **V86** — `revenu` table (FK `entreprise_id`, FK `country_id`, audit columns) **+ backfill**:
  ```sql
  INSERT INTO revenu (id, entreprise_id, country_id, montant, date_paiement, created_at, updated_at)
  SELECT gen_random_uuid(), a.entreprise_id, e.country_id, pa.montant_final, pa.date_paiement, pa.created_at, pa.updated_at
  FROM paiement_abonnement pa
  JOIN abonnement a  ON pa.abonnement_id = a.id
  JOIN entreprise e  ON a.entreprise_id  = e.id
  WHERE pa.statut = 'VALIDE';
  ```

## 3. Frontend

### 3.1 Shared period selector — add quarter/year

`src/common/tools/dateHelpers.ts`:
- `ReportPeriod` → `'yesterday' | 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'`
- `getDateRange`: `quarter` → 1st day of the current calendar quarter → today; `year` → Jan 1st of current year → today.

`period-selector-props.ts`: `PERIODS` array gains `{ key: 'quarter', labelKey: 'quarter' }` and `{ key: 'year', labelKey: 'year' }`, inserted between `month` and `custom`.

`fr.json` / `en.json`: 2 new keys under `common.periodSelector` (`quarter` → "Trimestre" / "Quarter", `year` → "Année" / "Year").

This single shared change benefits every existing consumer (`PeriodTab`, `VentesReportingPage`, `ReportingMainPage`, `DepenseReportingPage`) automatically — no per-page work needed beyond this file.

Both new pages live under Administration nav, entirely separate from the existing `ReportingPage.tsx`/`PeriodTab.tsx` (untouched). New DDD slice `features/plateforme-depense/` (domain dtos, repository port, `plateforme-depense-api` axios adapter hitting `/api/v1/admin/plateforme/...`, one TanStack hook per file per rule 52) backs both pages.

### 3.2 CRUD page — `/dashboard/administration/depenses`

Mirrors the existing store `DepensesPage`/`CategoryDepensePage` (table, form dialog, confirm-delete dialog, filters), gated by `PLATFORM_EXPENSE_READ`/`_CREATE`/`_UPDATE`/`_DELETE`. The create/edit form gains an optional `country` `<Select>` ("Aucun / global" as the empty option); the list filters gain the same country select.

### 3.3 Reporting page — `/dashboard/administration/depenses/reporting`

Same nesting pattern already used for store-level depenses (`/dashboard/depenses` CRUD next to `/dashboard/depenses/reporting`). New page, new component, consumes the new `GET /api/v1/admin/plateforme/reporting/period` endpoint from 2.3:

- Reuses the shared `PeriodSelector` (3.1, quarter/year included) + a `country` `<Select>` (options from the existing `GET /api/v1/countries`, default "Tous pays") + an `abonnement` filter (search-by-entreprise-name select or free text resolving to an `abonnementId`, default "Tous").
- 3 `KpiCard`s: **Revenu**, **Dépenses plateforme**, **Bénéfice** (green/red variant on sign). `depensesPlateforme`/`benefice` react to the `country` select same as `revenu`; none of the three react to the `abonnement` select except `revenu` — worth a small caption when `abonnement` is set alone, clarifying that dépenses/bénéfice still show the whole-platform figure in that case.
- Gated by `PLATFORM_REPORT_READ` (new, ADMIN only — see 2.3 for why `REPORT_FINANCIAL` isn't reused).

## 4. Testing

- Backend: unit tests for `DepensePlateformeServiceImpl`/`CategoryDepensePlateformeServiceImpl` (CRUD + `computeTotal` with/without `countryId`, mirrors `DepenseServiceImplTest`), `RevenuEventListenerTest` (`onRevenuRecorded` calls `revenuService.record(...)` with the event's ids), `RevenuServiceImplTest` (`record` builds the row via `getReference` for both FKs, `getTotalForPeriod` with each filter combination incl. `abonnementId` → resolved `entrepriseId`), `PaiementAbonnementServiceImplTest` (asserts one new `publishEvent(RevenuRecordedEvent)` call after `markAsValide` — existing assertions untouched), `PlateformeReportingServiceImplTest` (benefice = revenu − depenses with matching `countryId` on both sides; `abonnementId` alone leaves depenses/benefice unchanged), controller tests for the 3 new controllers (2 CRUD + 1 reporting). No existing test file for `AdminReportingController`/`AdminReportingServiceImpl` needs to change.
- Frontend: `dateHelpers.test.ts` for the 2 new `getDateRange` branches, adapter/hook tests for the new `plateforme-depense` slice, tests for the 2 new pages (CRUD + reporting). No existing `PeriodTab`/`ReportingPage` test needs to change.
- Manual: run the V85/V86 migrations against a dev DB with existing `VALIDE` payments and confirm the backfilled `revenu` total (sum of `Revenu.montant`) matches the existing Admin Reporting overview's `revenueYtd` for the current year, as a sanity cross-check between the old and new revenue sources (they're expected to agree, even though nothing wires them together).

## 5. Explicitly out of scope

- Per-store/per-tenant P&L (revenu/dépenses/bénéfice for a store owner's own business) — separate existing feature area, untouched.
- Currency conversion across countries — amounts are assumed directly comparable (no conversion logic exists anywhere in the codebase today); flagged as an assumption, not a decision, since it wasn't explicitly confirmed.
- A "revenue by country" breakdown/chart (explicitly rejected in favor of `country` as a plain filter).
- Any modification to `AdminReportingController`, `PeriodReportResponse`, `AdminOverviewStatsResponse`, `getStatistiquesPaiement`, `PaiementAbonnementStatsResponse`, `sumValidatedRevenueForYear`, `ReportingPage.tsx`, `PeriodTab.tsx` — this whole feature is additive, built alongside the existing admin reporting surface, not inside it.
