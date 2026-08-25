# Plan — Audit integration + soft delete for platform expenses/categories

Source: `.claude/TODO.md` open item "Audit integration + soft delete for platform expenses/categories".
No separate design doc — the two open design questions were resolved live with the user before this plan was written (see Design decisions below).

## Context

`org.store.plateforme` module has two CRUD entities, both global (no `Entreprise`/`Magasin` scope):
- `CategoryDepensePlateforme` (already has a business `actif` flag, default `true`)
- `DepensePlateforme` (no flag at all today)

Both `*ServiceImpl.delete()` methods currently hard-delete via `domainService.delete(entity)`. Neither publishes `AuditEvent`s on create/update/delete, unlike every other CRUD service in the codebase (e.g. `EmployeServiceImpl`, `PaiementAbonnementServiceImpl`).

## Design decisions (already settled with the user — do not re-litigate)

1. **Soft-delete reuses `actif`, no dedicated `deleted` flag.** This matches the only existing pattern in the codebase (`Magasin`, `Entreprise`, `Country`, `Role`, `PlanAbonnement`, `Coupon`, `CategoryDepense`, `CategoryDepensePlateforme` all use `actif` this way — no `deleted`/`supprime` field exists anywhere in `src/main/java/org/store`). Trade-off accepted by the user: an admin manually deactivating a category to hide it from a form and one who "deletes" it are indistinguishable in the data — this is fine.
2. **`DepensePlateforme.actif=false` (soft-deleted) is excluded from financial aggregates** (`computeTotal`, `sumByPeriod` — the P&L `benefice` reporting path) but **stays visible in the plain paginated listing** (`findResponsesByFilter`), exactly like `CategoryDepensePlateforme` already behaves with its `actif` filter param.
3. Both entities are global/non-tenant-scoped: every `AuditEvent` published from these services carries **`entrepriseId = null` and `magasinId = null`**, regardless of the caller's own scope. This mirrors the TODO note written when the item was scoped: "Both entities are global/non-tenant-scoped, so entrepriseId/magasinId on the AuditEvent will be null."

## Global Constraints

- Follow `.claude/BACKEND_CODING_CONVENTIONS.md` in full (already-known rules: max 3 params per method, explicit variable names, Javadoc on `<X>ServiceImpl` classes/methods with no inline comments, streams by default, etc.) — nothing in this plan overrides them.
- No new i18n keys are needed (no new user-facing exceptions are introduced by this plan).
- No frontend changes are in scope. This plan is backend-only.
- Do not touch `CategoryDepensePlateforme`'s existing `actif` semantics beyond what's specified (its filter/create/update behavior stays exactly as-is).
- Every task ends with `./mvnw test` run for the whole module (or full suite for the last task) and a clean, committed state before moving to the next task.
- Mirror the existing audit-publish pattern used in `org.store.users.application.service.impl.EmployeServiceImpl` (private `audit(...)` helper wrapping `currentUserService.getCurrent()` + `auditEventPublisher.publish(new AuditEvent(...))`) and in `org.store.abonnement.application.service.impl.PaiementAbonnementServiceImpl` (inline `auditEventPublisher.publish(new AuditEvent(...))` after the state change, using `UserPrincipal caller = currentUserService.getCurrent()`). Do not invent a different pattern.
- `AuditEvent` record shape (do not change): `AuditEvent(AuditAction action, AuditEntityType entityType, UUID entityId, String entityLabel, String performedBy, String performedByLabel, UUID entrepriseId, UUID magasinId, String details)`.
- `ICurrentUserService.getCurrent()` returns `UserPrincipal(UUID accountId, UUID userId, UUID entrepriseId, UUID magasinId, String username, String currency, String countryName, String role, List<String> permissions)`. Use `caller.accountId().toString()` for `performedBy` and `caller.username()` for `performedByLabel` (same as both reference implementations).

---

## Task 1 — Foundation: `DepensePlateforme.actif` field, migration, audit enum values

**Files:**
- `src/main/java/org/store/plateforme/domain/model/DepensePlateforme.java`
- `src/main/resources/db/migration/V87__add_actif_to_depense_plateforme.sql` (new)
- `src/main/java/org/store/audit/domain/enums/AuditAction.java`
- `src/main/java/org/store/audit/domain/enums/AuditEntityType.java`

**Changes:**

1. In `DepensePlateforme.java`, add a new field exactly mirroring `CategoryDepensePlateforme`'s:
   ```java
   private boolean actif = true;
   ```
   Place it as the last field in the class (after `country`). `@Getter`/`@Setter` are already class-level Lombok annotations — no per-field annotation needed.

2. Create `src/main/resources/db/migration/V87__add_actif_to_depense_plateforme.sql` (check `ls src/main/resources/db/migration/ | sort -V | tail -5` first to confirm `V87` is still the next free number — if a `V87` already exists, use the next free integer and adjust the entity-comment/task text accordingly):
   ```sql
   ALTER TABLE depense_plateforme ADD COLUMN actif BOOLEAN NOT NULL DEFAULT true;
   ```

3. In `AuditAction.java`, add six new enum constants (alphabetical grouping is not enforced elsewhere in this file — append after the existing last constant `PAIEMENT_ABONNEMENT_REJECTED`):
   ```java
   DEPENSE_PLATEFORME_CREATED,
   DEPENSE_PLATEFORME_UPDATED,
   DEPENSE_PLATEFORME_DELETED,
   CATEGORY_DEPENSE_PLATEFORME_CREATED,
   CATEGORY_DEPENSE_PLATEFORME_UPDATED,
   CATEGORY_DEPENSE_PLATEFORME_DELETED
   ```

4. In `AuditEntityType.java`, add two new enum constants (append after `PAIEMENT_ABONNEMENT`):
   ```java
   DEPENSE_PLATEFORME,
   CATEGORY_DEPENSE_PLATEFORME
   ```

**Verification:** `./mvnw compile` succeeds. `./mvnw test` — full suite still green (this task touches no behavior, only adds a field/column/enum constants, so no test should break; the app must still boot for any test needing a full context, since the migration must apply cleanly against the test DB).

**Report:** commit message theme "feat(plateforme): add actif field + migration + audit enum values (foundation)".

---

## Task 2 — `CategoryDepensePlateforme`: audit integration + soft delete

**Depends on:** Task 1 (needs the new `AuditAction`/`AuditEntityType` constants).

**Files:**
- `src/main/java/org/store/plateforme/application/service/impl/CategoryDepensePlateformeServiceImpl.java`
- `src/test/java/org/store/plateforme/application/service/CategoryDepensePlateformeServiceImplTest.java`

**Current state (read the files, this is the exact code you're changing):**

`CategoryDepensePlateformeServiceImpl` currently has a single constructor dependency (`CategoryDepensePlateformeDomainService domainService`) and 5 public methods: `create`, `findById`, `findResponseById`, `findAll`, `update`, `delete`. `delete(UUID id)` currently does `domainService.delete(domainService.findById(id))` — a hard delete.

**Changes:**

1. Add two constructor dependencies: `org.store.audit.application.service.IAuditEventPublisher auditEventPublisher` and `org.store.security.application.service.ICurrentUserService currentUserService`. Update the constructor and store both as `private final` fields.

2. Add a private helper method (mirrors `EmployeServiceImpl.audit(...)`):
   ```java
   private void audit(AuditAction action, UUID entityId, String label) {
       UserPrincipal caller = currentUserService.getCurrent();
       auditEventPublisher.publish(new AuditEvent(action, AuditEntityType.CATEGORY_DEPENSE_PLATEFORME, entityId, label,
               caller.accountId().toString(), caller.username(), null, null, null));
   }
   ```
   (`entrepriseId`/`magasinId` hardcoded `null` — see Design decision 3.)

3. `create(request)`: after `domainService.create(request)` succeeds, call `audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_CREATED, created.getId(), created.getNom())` before building/returning the response. Keep the existing `ensureNomAvailable` check unchanged.

4. `update(id, request)`: after `domainService.save(category)` succeeds, call `audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_UPDATED, category.getId(), category.getNom())` before building/returning the response. Keep the existing nom-uniqueness check unchanged.

5. `delete(id)`: replace the hard delete with a soft delete:
   ```java
   @Override
   @Transactional
   public void delete(UUID id) {
       CategoryDepensePlateforme category = domainService.findById(id);
       category.setActif(false);
       domainService.save(category);
       audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_DELETED, category.getId(), category.getNom());
   }
   ```
   Do **not** call `domainService.delete(...)` anymore — the row must stay in the table.

6. Add/update the class-level and method-level Javadoc per `BACKEND_CODING_CONVENTIONS.md` rule (1-sentence class doc already exists as a doc-comment above the class — extend it if needed to mention audit+soft-delete; add a 1-sentence method Javadoc on `delete` explaining it deactivates rather than removes the row. No inline comments.)

**Tests (`CategoryDepensePlateformeServiceImplTest`):**

Add `@Mock private IAuditEventPublisher auditEventPublisher;` and `@Mock private ICurrentUserService currentUserService;` (both injected via the existing `@InjectMocks` field — Mockito's constructor injection picks up all declared `@Mock` fields automatically, no other wiring needed).

Stub `currentUserService.getCurrent()` to return a `UserPrincipal` fixture only in tests that reach the `audit(...)` call (i.e. `create` success path, `update` success path, `delete`) — lenient stubbing is not needed if you scope the `when(...)` to only the tests that need it. Use `new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), null, null, "admin", null, null, "ADMIN", java.util.List.of())` as the fixture (fields beyond `accountId`/`username` are irrelevant to these tests).

Add these new test cases:
- `create_should_publish_audit_event_on_success` — verify `auditEventPublisher.publish(...)` was called once with an `AuditEvent` whose `action() == AuditAction.CATEGORY_DEPENSE_PLATEFORME_CREATED` and `entityType() == AuditEntityType.CATEGORY_DEPENSE_PLATEFORME`.
- `create_should_not_publish_audit_event_when_nom_already_taken` — the existing throw-path test; assert `verify(auditEventPublisher, never()).publish(any())` (add this assertion to the existing `create_should_throw_when_nom_already_taken` test, do not duplicate it).
- `update_should_publish_audit_event_on_success` — you'll need to add a `findById` stub returning an existing `CategoryDepensePlateforme` and a `save` stub, following the existing constructor-call shape used in `create_should_persist_when_nom_available`.
- `delete_should_deactivate_instead_of_hard_delete` — stub `domainService.findById(id)` to return a category with `actif=true`; call `service.delete(id)`; assert `verify(domainService).save(argThat(c -> !c.isActif()))` and `verify(domainService, never()).delete(any())`.
- `delete_should_publish_audit_event` — same setup, assert the publish call carries `CATEGORY_DEPENSE_PLATEFORME_DELETED`.

**Verification:** `./mvnw test -Dtest=CategoryDepensePlateformeServiceImplTest,CategoryDepensePlateformeControllerTest` green, then full `./mvnw test` green (the controller test mocks the service interface and should need no changes, but confirm).

**Report:** commit message theme "feat(plateforme): audit integration + soft delete for CategoryDepensePlateforme".

---

## Task 3 — `DepensePlateforme`: audit integration + soft delete + `actif` filter + reporting exclusion

**Depends on:** Task 1 (enum constants) and the new `actif` column from Task 1's migration. Independent of Task 2's code changes (different files) but do this task after Task 2 so both services follow the identical audit pattern for a consistent final review — if dispatched in parallel by mistake, note there is no file overlap with Task 2.

**Files:**
- `src/main/java/org/store/plateforme/application/service/impl/DepensePlateformeServiceImpl.java`
- `src/main/java/org/store/plateforme/domain/service/DepensePlateformeDomainService.java`
- `src/main/java/org/store/plateforme/domain/repository/DepensePlateformeRepository.java`
- `src/main/java/org/store/plateforme/application/dto/DepensePlateformeFilter.java`
- `src/main/java/org/store/plateforme/presentation/DepensePlateformeController.java`
- `src/test/java/org/store/plateforme/application/service/DepensePlateformeServiceImplTest.java`
- `src/test/java/org/store/plateforme/presentation/DepensePlateformeControllerTest.java`

**Current state:** read all 7 files above before starting — the current shape of each is exactly as it was when this plan was written (no other task touches these files).

**Changes:**

1. **`DepensePlateformeFilter`** — add one field, `Boolean actif`, as the 4th component (after `countryId`, before `libelle`) to group related identity/status filters together:
   ```java
   public record DepensePlateformeFilter(
           UUID categoryId,
           UUID moyenPaiementId,
           UUID countryId,
           Boolean actif,
           String libelle,
           @DatePattern String startDate,
           @DatePattern String endDate,
           @Min(0) int page,
           @Min(1) int size
   ) { ... }
   ```
   `toPageable()` is unchanged.

2. **`DepensePlateformeRepository`**:
   - `findResponsesByFilter` (plain listing): add `@Param("actif") Boolean actif` parameter and the clause `AND (:actif IS NULL OR depense.actif = :actif)` — same optional-filter shape as `CategoryDepensePlateformeRepository.findResponsesByFilter`'s `actif` clause. This means the plain listing shows both active and soft-deleted rows by default (matches Category's existing behavior) unless the caller explicitly filters.
   - `computeTotal`: add the **unconditional** clause `AND depense.actif = true` (not an optional filter — soft-deleted rows must never count here, per Design decision 2). Do not add an `actif` method parameter to this query — it's hardcoded true.
   - `sumByPeriod`: same — add unconditional `AND depense.actif = true`.

3. **`DepensePlateformeDomainService`**:
   - `findResponsesByFilter(filter)`: add `filter.actif()` to the `repository.findResponsesByFilter(...)` call, in the same position as the new repository parameter.
   - `computeTotal(filter)` and `sumByPeriod(...)`: no parameter changes (the repository queries now hardcode the `actif = true` filter internally).

4. **`DepensePlateformeController`**:
   - `list(...)`: add `@RequestParam(required = false) Boolean actif` and pass it into the `new DepensePlateformeFilter(...)` call in the correct new position.
   - `computeTotal(...)`: this endpoint's `DepensePlateformeFilter` is built with hardcoded `page=0, size=1` and no filters are meant to change its totals semantics beyond what the repository already enforces — pass `null` for the new `actif` param here (the repository's `computeTotal` query ignores the filter's `actif` field entirely — see point 2 above — so this is inert either way, but keep the constructor call consistent). Do **not** add an `actif` query parameter to this endpoint.

5. **`DepensePlateformeServiceImpl`**:
   - Add two constructor dependencies: `IAuditEventPublisher auditEventPublisher`, `ICurrentUserService currentUserService` (same pattern as Task 2).
   - Add the private `audit(AuditAction action, UUID entityId, String label)` helper, identical shape to Task 2's but with `AuditEntityType.DEPENSE_PLATEFORME`.
   - `create(request)`: after `domainService.create(...)` succeeds, call `audit(AuditAction.DEPENSE_PLATEFORME_CREATED, created.getId(), created.getLibelle())` (use the local variable holding the created `DepensePlateforme` entity — rename/introduce one if the current code inlines the call to `new DepensePlateformeResponse(...)` without a named variable).
   - `update(id, request)`: after `domainService.save(depense)` succeeds, call `audit(AuditAction.DEPENSE_PLATEFORME_UPDATED, depense.getId(), depense.getLibelle())`.
   - `delete(id)`: replace the hard delete with:
     ```java
     @Override
     @Transactional
     public void delete(UUID id) {
         DepensePlateforme depense = domainService.findById(id);
         depense.setActif(false);
         domainService.save(depense);
         audit(AuditAction.DEPENSE_PLATEFORME_DELETED, depense.getId(), depense.getLibelle());
     }
     ```
   - Add/extend Javadoc per convention (class + each touched method, no inline comments).

**Tests:**

`DepensePlateformeServiceImplTest` — add `@Mock private IAuditEventPublisher auditEventPublisher;` and `@Mock private ICurrentUserService currentUserService;`, same `UserPrincipal` fixture pattern as Task 2. Update every existing `DepensePlateformeFilter` constructor call in this file to include the new `actif` field (pass `null` where the test doesn't care about it, positioned correctly). Add:
- `create_should_publish_audit_event_on_success`
- `update_should_publish_audit_event_on_success` (add a full test for `update` if none exists today — check the current file; if there's no existing `update` test, write one following the `create` tests' fixture pattern: stub `domainService.findById`, `categoryService.findById`, `moyenPaiementService.findById`, `domainService.save`, assert the response and the audit publish)
- `delete_should_deactivate_instead_of_hard_delete` — assert `verify(domainService).save(argThat(d -> !d.isActif()))` and `verify(domainService, never()).delete(any())`
- `delete_should_publish_audit_event`
- `findAll_should_pass_actif_filter_through` — extend or add alongside the existing `findAll_should_validate_filter_before_delegating` test, asserting the `actif` field flows through unchanged to `domainService.findResponsesByFilter`.

`DepensePlateformeControllerTest` — update every `list(...)` / filter-construction call site affected by the new `actif` query param and `DepensePlateformeFilter` field (read the file first: check how the existing tests build the mocked filter/call the endpoint, and add the new param consistently). Add one new controller test: `list_should_accept_actif_query_param` verifying the param round-trips to the service call.

**Verification:** `./mvnw test -Dtest=DepensePlateformeServiceImplTest,DepensePlateformeControllerTest,CategoryDepensePlateformeServiceImplTest,CategoryDepensePlateformeControllerTest` green, then full `./mvnw test` green — this is the last task, so the whole suite must be clean before final review.

**Report:** commit message theme "feat(plateforme): audit integration + soft delete + actif filter for DepensePlateforme, exclude soft-deleted rows from P&L totals".

---

## Post-plan (controller owns this, not the tasks above)

After Task 3's task review is clean, dispatch the final whole-branch review (per subagent-driven-development), covering all 3 tasks' combined diff against `dev-barry`. Points the final reviewer should specifically check (in addition to its normal rubric):
- The two `computeTotal`/`sumByPeriod` queries in `DepensePlateformeRepository` truly hardcode `actif = true` unconditionally (Design decision 2) — this is the one place a reviewer might "helpfully" turn it into an optional filter param matching the Category pattern, which would be wrong here.
- No `entrepriseId`/`magasinId` leak onto any of the 6 new audit events (must always be `null` — Design decision 3).
- `CategoryDepensePlateforme.delete()` and `DepensePlateforme.delete()` never call `domainService.delete(...)` anymore.
