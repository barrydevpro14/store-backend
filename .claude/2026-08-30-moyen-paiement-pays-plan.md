# Country-aware MoyenPaiement (Task 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a many-to-many `MoyenPaiement` ↔ `Country` relation and filter payment-method selection by the current entreprise's country everywhere `MoyenPaiement` is picked (vente, achat, dépense tenant, dépenses plateforme admin) — except subscription payment, which is Task 2's separate scope.

**Architecture:** `MoyenPaiement` gains a `@ManyToMany` `pays` collection (empty = global). A new `GET /api/v1/moyens-paiement/select` endpoint (separate from the existing unfiltered CRUD listing) returns `Page<DataSelect>`, filtered server-side by an explicit `countryId` query param, or auto-resolved from the caller's current entreprise, or unfiltered for ADMIN callers with no entreprise. All 9 frontend consumers of the old unfiltered `useMoyenPaiementList()` switch to this new endpoint.

**Tech Stack:** Spring Boot 4.0.6 / JPA / Flyway (backend), Next.js 16 / React 19 / TanStack Query / react-hook-form + zod (frontend).

**Spec:** `.claude/2026-08-30-moyen-paiement-pays-facturation-design.md`, section 1 (§1.1–§1.6). Section 5's open point and all of section 2 belong to Task 2, out of scope here.

## Global Constraints

- No server-side write-time validation of a submitted `moyenPaiementId` — filtering is UI-list-only (confirmed decision, spec §4.1).
- `MoyenPaiement` ↔ `Country` is many-to-many, not a single nullable FK (spec §4.2).
- Empty `pays` collection = global/available everywhere — the 4 existing seeded moyens (Cash, Wave, OM, Card) must keep working exactly as today with zero explicit country attached.
- No new permission for the `/select` endpoint — it stays unguarded like the existing plain `GET /api/v1/moyens-paiement` (confirmed: no `MOYEN_PAIEMENT_READ` permission exists in `roles-permissions.yml` today).
- Every backend DTO/entity change must not force edits to unrelated existing call sites — see Task 2's compact-constructor approach for `MoyenPaiementResponse`.
- **Pre-existing gap, now addressed by Task 12**: `FRONTEND_CODING_CONVENTIONS.md` rule 54 requires `<Combobox>` (not `<Select>`) in every form/dialog selector. The 5 files touched in Tasks 8–9 (`VenteForm`, `ValiderVenteDialog`, `CreatePaiementVenteDialog`, `ReceiveAchatDialog`, `CreatePaiementAchatDialog`) used `<Select>` for the payment-method field. Tasks 8–9 swap the data source only (country-filtered) and keep `<Select>` as-is; Task 12 then converts those same 5 fields to `<Combobox>` as its own separate, reviewable step.

---

## File Structure

| File | Change |
|---|---|
| `src/main/resources/db/migration/V91__create_moyen_paiement_pays.sql` | Create |
| `src/main/java/org/store/paiement/domain/model/MoyenPaiement.java` | Modify — add `pays` field |
| `src/main/java/org/store/paiement/application/dto/MoyenPaiementRequest.java` | Modify — add `paysIds` |
| `src/main/java/org/store/paiement/application/dto/MoyenPaiementResponse.java` | Modify — add `pays`, compact 3-arg constructor kept |
| `src/main/java/org/store/paiement/application/dto/MoyenPaiementSelectFilter.java` | Create — `countryId` + `searchTerm` + `page`/`size` (rule 33) |
| `src/main/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImpl.java` | Modify — resolve `paysIds`, new `findSelectItems` |
| `src/main/java/org/store/paiement/application/service/IMoyenPaiementService.java` | Modify — new `findSelectItems` signature |
| `src/main/java/org/store/paiement/domain/service/MoyenPaiementDomainService.java` | Modify — delegate `findSelectItems` |
| `src/main/java/org/store/paiement/domain/repository/MoyenPaiementRepository.java` | Modify — new `@Query findSelectItems` |
| `src/main/java/org/store/paiement/presentation/MoyenPaiementController.java` | Modify — new `/select` endpoint |
| `src/main/java/org/store/country/domain/repository/CountryRepository.java` | Modify — new `@Query findAllByIdIn` |
| `src/main/java/org/store/country/domain/service/CountryDomainService.java` | Modify — new `findAllByIds` (bulk, throws on missing id) |
| `src/main/java/org/store/entreprise/application/service/IEntrepriseService.java` | Modify — new `findCurrentUserCountryId` |
| `src/main/java/org/store/entreprise/application/service/impl/EntrepriseServiceImpl.java` | Modify — implements `findCurrentUserCountryId` |
| `src/test/java/org/store/entreprise/application/service/EntrepriseServiceImplTest.java` | Modify — 2 new tests |
| `src/test/java/org/store/country/domain/service/CountryDomainServiceTest.java` | Create |
| `src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java` | Create |
| `src/test/java/org/store/paiement/presentation/MoyenPaiementControllerTest.java` | Create |
| `store-frontend/src/features/moyen-paiement/domain/dtos/moyen-paiement-response.ts` | Modify |
| `store-frontend/src/features/moyen-paiement/infrastructure/moyen-paiement-api.ts` | Modify |
| `store-frontend/src/features/moyen-paiement/application/useCreateMoyenPaiement.ts` | Modify |
| `store-frontend/src/features/moyen-paiement/application/useUpdateMoyenPaiement.ts` | Modify |
| `store-frontend/src/features/moyen-paiement/application/useMoyenPaiementSelectList.ts` | Create |
| `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementFormDialog.tsx` | Modify — country multi-select |
| `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementTable.tsx` | Modify — Pays column |
| `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementSelect.tsx` | Modify — `countryId` prop |
| `store-frontend/src/features/vente/presentation/{VenteForm,ValiderVenteDialog,CreatePaiementVenteDialog}.tsx` | Modify |
| `store-frontend/src/features/achat/presentation/{ReceiveAchatDialog,CreatePaiementAchatDialog}.tsx` | Modify |
| `store-frontend/src/features/depense/presentation/{DepenseForm,DepenseFilters}.tsx` | Modify |
| `store-frontend/src/features/plateforme-depense/presentation/{DepensePlateformeForm,DepensePlateformeFilters}.tsx` | Modify |
| `store-frontend/src/messages/fr.json`, `store-frontend/src/messages/en.json` | Modify — new keys |

---

### Task 1: Migration + `MoyenPaiement.pays` relation

**Files:**
- Create: `src/main/resources/db/migration/V91__create_moyen_paiement_pays.sql`
- Modify: `src/main/java/org/store/paiement/domain/model/MoyenPaiement.java`

**Interfaces:**
- Produces: `MoyenPaiement.getPays(): Set<Country>` / `setPays(Set<Country>)` (Lombok `@Getter`/`@Setter`, class-level, already on the class).

- [ ] **Step 1: Create the migration**

```sql
CREATE TABLE moyen_paiement_pays (
    moyen_paiement_id UUID NOT NULL,
    country_id UUID NOT NULL,
    CONSTRAINT pk_moyen_paiement_pays PRIMARY KEY (moyen_paiement_id, country_id),
    CONSTRAINT fk_moyen_paiement_pays_moyen FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement (id),
    CONSTRAINT fk_moyen_paiement_pays_country FOREIGN KEY (country_id) REFERENCES country (id)
);
```

- [ ] **Step 2: Add the relation to the entity**

In `MoyenPaiement.java`, add imports `jakarta.persistence.ManyToMany`, `jakarta.persistence.JoinTable`, `jakarta.persistence.JoinColumn`, `org.store.country.domain.model.Country`, `java.util.HashSet`, `java.util.Set`, then add the field after `actif`:

```java
    @ManyToMany
    @JoinTable(
            name = "moyen_paiement_pays",
            joinColumns = @JoinColumn(name = "moyen_paiement_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> pays = new HashSet<>();
```

- [ ] **Step 3: Verify the mapping boots cleanly**

Run: `./mvnw test -Dtest=StoreApplicationTests`
Expected: PASS. This is the project's only Spring-context-level test; a broken `@JoinTable` column/table name mismatch or a bad migration would fail context startup here. (No `@DataJpaTest` exists anywhere in this codebase — this is the established way schema/mapping correctness gets caught.)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V91__create_moyen_paiement_pays.sql src/main/java/org/store/paiement/domain/model/MoyenPaiement.java
git commit -m "feat(paiement): add many-to-many MoyenPaiement-Country relation"
```

---

### Task 2: `MoyenPaiementRequest`/`Response` + service create/update with `paysIds`

> **Convention check (`.claude/BACKEND_CODING_CONVENTIONS.md`) applied to this task**: rule 27 forbids private methods on a `<X>ServiceImpl` beyond trivial DTO helpers — resolving a `Set<UUID>` to `Set<Country>` is business logic, not trivial, so it is NOT a private helper here; it is pushed down to `CountryDomainService.findAllByIds(...)`, a single JPQL round-trip (no N+1 loop) that already owns the null/empty-guard and the not-found check, matching rule 4 ("reusable code → public method on the aggregate's owning service") and rule 32 (explicit JPQL aliases, no `c`/`m`/`p`).

**Files:**
- Modify: `src/main/java/org/store/paiement/application/dto/MoyenPaiementRequest.java`
- Modify: `src/main/java/org/store/paiement/application/dto/MoyenPaiementResponse.java`
- Modify: `src/main/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImpl.java`
- Modify: `src/main/java/org/store/country/domain/repository/CountryRepository.java`
- Modify: `src/main/java/org/store/country/domain/service/CountryDomainService.java`
- Test: `src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java`
- Test: Create `src/test/java/org/store/country/domain/service/CountryDomainServiceTest.java`

**Interfaces:**
- Consumes: `MoyenPaiement.pays` (Task 1), `Country.getId()/getName()` (existing).
- Produces: `MoyenPaiementRequest(String libelle, Set<UUID> paysIds)`, `MoyenPaiementResponse(UUID id, String libelle, boolean actif, List<CountryResponse> pays)` with a **compact 3-arg constructor** `MoyenPaiementResponse(UUID, String, boolean)` preserved so the 8 existing call sites across `PaiementAchatResponse`, `PaiementVenteResponse`, `PreuvePaiementResponse`, `DepensePlateformeResponse`, `DepenseResponse`, and 6 existing test files (`PaiementVenteServiceImplTest`, `DepensePlateformeControllerTest`, `FactureAchatControllerTest`, `FactureClientControllerTest` ×2, `PaiementAbonnementControllerTest`, `PreuvePaiementControllerTest`, `DepenseControllerTest`) keep compiling unchanged. `CountryDomainService.findAllByIds(Set<UUID> countryIds): List<Country>` — single JPQL round-trip resolving every requested id at once, returning an empty list for a null/empty input, throwing `EntityException("entity.notFound", countryIds)` if any requested id doesn't resolve to a real `Country`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java`:

```java
package org.store.paiement.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.MoyenPaiementDomainService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MoyenPaiementServiceImplTest {

    private MoyenPaiementDomainService domainService;
    private ValidatorService validatorService;
    private CountryDomainService countryDomainService;
    private MoyenPaiementServiceImpl service;

    @BeforeEach
    void setUp() {
        domainService = mock(MoyenPaiementDomainService.class);
        validatorService = mock(ValidatorService.class);
        countryDomainService = mock(CountryDomainService.class);
        service = new MoyenPaiementServiceImpl(domainService, validatorService, countryDomainService);
        when(domainService.save(any(MoyenPaiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_should_leave_pays_empty_when_paysIds_is_null() {
        when(domainService.findAll()).thenReturn(List.of());
        when(countryDomainService.findAllByIds(null)).thenReturn(List.of());

        MoyenPaiementResponse response = service.create(new MoyenPaiementRequest("Wave", null));

        assertThat(response.pays()).isEmpty();
    }

    @Test
    void create_should_attach_pays_when_paysIds_provided() {
        when(domainService.findAll()).thenReturn(List.of());
        UUID senegalId = UUID.randomUUID();
        Country senegal = new Country();
        senegal.setId(senegalId);
        senegal.setName("Sénégal");
        when(countryDomainService.findAllByIds(Set.of(senegalId))).thenReturn(List.of(senegal));

        MoyenPaiementResponse response = service.create(new MoyenPaiementRequest("Wave", Set.of(senegalId)));

        assertThat(response.pays()).extracting("id").containsExactly(senegalId);
    }

    @Test
    void create_should_throw_when_libelle_already_exists() {
        MoyenPaiement existing = new MoyenPaiement();
        existing.setId(UUID.randomUUID());
        existing.setLibelle("Wave");
        when(domainService.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(new MoyenPaiementRequest("Wave", null)))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void update_should_replace_pays_entirely() {
        UUID id = UUID.randomUUID();
        MoyenPaiement existing = new MoyenPaiement();
        existing.setId(id);
        existing.setLibelle("Wave");
        UUID guineeId = UUID.randomUUID();
        Country guinee = new Country();
        guinee.setId(guineeId);
        guinee.setName("Guinée");
        when(domainService.findById(id)).thenReturn(existing);
        when(domainService.findAll()).thenReturn(List.of(existing));
        when(countryDomainService.findAllByIds(Set.of(guineeId))).thenReturn(List.of(guinee));

        MoyenPaiementResponse response = service.update(id, new MoyenPaiementRequest("Wave", Set.of(guineeId)));

        assertThat(response.pays()).extracting("id").containsExactly(guineeId);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=MoyenPaiementServiceImplTest`
Expected: compile error — `MoyenPaiementRequest`/`Response` don't have the new fields/constructor yet, `MoyenPaiementServiceImpl`'s constructor doesn't take a `CountryDomainService`, and `CountryDomainService.findAllByIds` doesn't exist.

- [ ] **Step 3: Update `MoyenPaiementRequest`**

```java
package org.store.paiement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record MoyenPaiementRequest(
        @NotBlank @Size(max = 100) String libelle,
        Set<UUID> paysIds
) {
}
```

- [ ] **Step 4: Update `MoyenPaiementResponse`**

```java
package org.store.paiement.application.dto;

import org.store.country.application.dto.CountryResponse;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.UUID;

public record MoyenPaiementResponse(
        UUID id,
        String libelle,
        boolean actif,
        List<CountryResponse> pays
) {
    public MoyenPaiementResponse(UUID id, String libelle, boolean actif) {
        this(id, libelle, actif, List.of());
    }

    public MoyenPaiementResponse(MoyenPaiement moyenPaiement) {
        this(
                moyenPaiement.getId(),
                moyenPaiement.getLibelle(),
                moyenPaiement.isActif(),
                moyenPaiement.getPays().stream().map(CountryResponse::new).toList()
        );
    }
}
```

- [ ] **Step 5: Add the bulk lookup to `CountryRepository` (domain port)**

Add imports `org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`, `java.util.Set`, `java.util.UUID` to `src/main/java/org/store/country/domain/repository/CountryRepository.java`, then add (explicit JPQL alias `country`, per rule 32 — no bare `c`):

```java
    @Query("""
            SELECT country
            FROM Country country
            WHERE country.id IN :countryIds
            """)
    List<Country> findAllByIdIn(@Param("countryIds") Set<UUID> countryIds);
```

- [ ] **Step 6: Add `CountryDomainService.findAllByIds`**

Add imports `org.store.common.exceptions.EntityException`, `java.util.Set`, `java.util.UUID` to `src/main/java/org/store/country/domain/service/CountryDomainService.java`, then add:

```java
    public List<Country> findAllByIds(Set<UUID> countryIds) {
        if (countryIds == null || countryIds.isEmpty()) {
            return List.of();
        }

        List<Country> foundCountries = repository.findAllByIdIn(countryIds);
        if (foundCountries.size() != countryIds.size()) {
            throw new EntityException("entity.notFound", countryIds);
        }

        return foundCountries;
    }
```

- [ ] **Step 7: Write `CountryDomainServiceTest`**

Create `src/test/java/org/store/country/domain/service/CountryDomainServiceTest.java`:

```java
package org.store.country.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.EntityException;
import org.store.country.domain.model.Country;
import org.store.country.domain.repository.CountryRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CountryDomainServiceTest {

    private CountryRepository repository;
    private CountryDomainService service;

    @BeforeEach
    void setUp() {
        repository = mock(CountryRepository.class);
        service = new CountryDomainService(repository);
    }

    @Test
    void findAllByIds_should_return_empty_list_when_ids_is_null() {
        assertThat(service.findAllByIds(null)).isEmpty();
    }

    @Test
    void findAllByIds_should_return_all_found_countries() {
        UUID senegalId = UUID.randomUUID();
        Country senegal = new Country();
        senegal.setId(senegalId);
        when(repository.findAllByIdIn(Set.of(senegalId))).thenReturn(List.of(senegal));

        List<Country> result = service.findAllByIds(Set.of(senegalId));

        assertThat(result).containsExactly(senegal);
    }

    @Test
    void findAllByIds_should_throw_when_a_requested_id_does_not_resolve() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findAllByIdIn(Set.of(unknownId))).thenReturn(List.of());

        assertThatThrownBy(() -> service.findAllByIds(Set.of(unknownId)))
                .isInstanceOf(EntityException.class);
    }
}
```

- [ ] **Step 8: Update `MoyenPaiementServiceImpl`**

Replace the class with:

```java
package org.store.paiement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.MoyenPaiementDomainService;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MoyenPaiementServiceImpl implements IMoyenPaiementService {

    private final MoyenPaiementDomainService domainService;
    private final ValidatorService validatorService;
    private final CountryDomainService countryDomainService;

    public MoyenPaiementServiceImpl(MoyenPaiementDomainService domainService,
                                    ValidatorService validatorService,
                                    CountryDomainService countryDomainService) {
        this.domainService = domainService;
        this.validatorService = validatorService;
        this.countryDomainService = countryDomainService;
    }

    @Override
    public List<MoyenPaiementResponse> findAll() {
        return domainService.findAll().stream()
                .map(MoyenPaiementResponse::new)
                .toList();
    }

    @Override
    public MoyenPaiement findById(UUID id) {
        return domainService.findById(id);
    }

    @Override
    @Transactional
    public MoyenPaiementResponse create(MoyenPaiementRequest request) {
        validatorService.validate(request);
        ensureLibelleUnique(request.libelle(), null);

        MoyenPaiement moyen = new MoyenPaiement();
        moyen.setLibelle(request.libelle());
        moyen.setCode(request.libelle().toUpperCase().replaceAll("[^A-Z0-9]", "_"));
        moyen.setPays(new HashSet<>(countryDomainService.findAllByIds(request.paysIds())));

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    @Override
    @Transactional
    public MoyenPaiementResponse update(UUID id, MoyenPaiementRequest request) {
        validatorService.validate(request);
        MoyenPaiement moyen = domainService.findById(id);
        ensureLibelleUnique(request.libelle(), id);

        moyen.setLibelle(request.libelle());
        moyen.setPays(new HashSet<>(countryDomainService.findAllByIds(request.paysIds())));

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    @Override
    @Transactional
    public MoyenPaiementResponse activate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(true);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    @Override
    @Transactional
    public MoyenPaiementResponse deactivate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(false);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    private void ensureLibelleUnique(String libelle, UUID excludeId) {
        boolean conflict = domainService.findAll().stream()
                .anyMatch(m -> m.getLibelle().equalsIgnoreCase(libelle)
                        && (excludeId == null || !m.getId().equals(excludeId)));
        if (conflict) {
            throw new BadArgumentException("moyenPaiement.libelle.alreadyExists", libelle);
        }
    }
}
```

(`ensureLibelleUnique` stays private — pre-existing method, not touched by this plan, left as-is per "no unrequested refactoring." `findSelectItems` is added in Task 3, which also adds a 4th constructor dependency — not yet part of this class.)

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw test -Dtest=MoyenPaiementServiceImplTest,CountryDomainServiceTest`
Expected: PASS (4/4 + 3/3).

- [ ] **Step 10: Run the full backend suite to confirm zero ripple on the 8 existing `MoyenPaiementResponse(id, libelle, actif)` call sites**

Run: `./mvnw test -Dtest=PaiementVenteServiceImplTest,DepensePlateformeControllerTest,FactureAchatControllerTest,FactureClientControllerTest,PaiementAbonnementControllerTest,PreuvePaiementControllerTest,DepenseControllerTest`
Expected: PASS — the compact 3-arg constructor keeps these untouched.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/org/store/paiement/application/dto/MoyenPaiementRequest.java \
        src/main/java/org/store/paiement/application/dto/MoyenPaiementResponse.java \
        src/main/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImpl.java \
        src/main/java/org/store/country/domain/repository/CountryRepository.java \
        src/main/java/org/store/country/domain/service/CountryDomainService.java \
        src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java \
        src/test/java/org/store/country/domain/service/CountryDomainServiceTest.java
git commit -m "feat(paiement): resolve MoyenPaiement.pays from paysIds on create/update"
```

---

### Task 3: `/select` endpoint — filter record, repository, domain service, application service, controller

> **Convention check applied to this task**: rule 33 mandates a dedicated `<X>Filter` record as soon as a listing endpoint has ≥ 2 criteria — this endpoint has `countryId` **and** `searchTerm`, so `MoyenPaiementSelectFilter` is introduced (avoids a 4-parameter `findSelectItems` on the application/domain service, which rule 30 caps at 3). Country resolution moves to a new **public** `IEntrepriseService.findCurrentUserCountryId()` rather than a private helper on `MoyenPaiementServiceImpl` — it's `Entreprise`'s own aggregate concern (rule 4: "reusable code between services → public method on the aggregate's owning service"), and rule 27 forbids private business-logic methods on a `<X>ServiceImpl` regardless. JPQL aliases are explicit (`moyenPaiement`, `pays`), never `m`/`p` (rule 32).

**Files:**
- Modify: `src/main/java/org/store/entreprise/application/service/IEntrepriseService.java`
- Modify: `src/main/java/org/store/entreprise/application/service/impl/EntrepriseServiceImpl.java`
- Test: `src/test/java/org/store/entreprise/application/service/EntrepriseServiceImplTest.java` (append)
- Create: `src/main/java/org/store/paiement/application/dto/MoyenPaiementSelectFilter.java`
- Modify: `src/main/java/org/store/paiement/domain/repository/MoyenPaiementRepository.java`
- Modify: `src/main/java/org/store/paiement/domain/service/MoyenPaiementDomainService.java`
- Modify: `src/main/java/org/store/paiement/application/service/IMoyenPaiementService.java`
- Modify: `src/main/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImpl.java`
- Modify: `src/main/java/org/store/paiement/presentation/MoyenPaiementController.java`
- Test: `src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java` (append)
- Test: Create `src/test/java/org/store/paiement/presentation/MoyenPaiementControllerTest.java`

**Interfaces:**
- Consumes: `LikePatternHelper.toLikePattern(String): String` (existing, `org.store.common.tools`), `DataSelect(String value, String label)` (existing, `org.store.common.dto`).
- Produces: `IEntrepriseService.findCurrentUserCountryId(): UUID` (nullable — `null` when the current user has no entreprise, i.e. ADMIN), `MoyenPaiementSelectFilter(UUID countryId, String searchTerm, int page, int size)` with `toPageable(): Pageable`, `IMoyenPaiementService.findSelectItems(MoyenPaiementSelectFilter filter): Page<DataSelect>`, `GET /api/v1/moyens-paiement/select?countryId=&q=&page=&size=`.

- [ ] **Step 1: Write the failing test for `IEntrepriseService.findCurrentUserCountryId`**

Append to `src/test/java/org/store/entreprise/application/service/EntrepriseServiceImplTest.java` (add import `org.store.country.domain.model.Country` at the top):

```java
    @Test
    void findCurrentUserCountryId_should_resolve_country_of_current_entreprise() {
        UUID countryId = UUID.randomUUID();
        Country country = new Country();
        country.setId(countryId);
        entreprise.setCountry(country);
        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);

        UUID result = service.findCurrentUserCountryId();

        assertThat(result).isEqualTo(countryId);
    }

    @Test
    void findCurrentUserCountryId_should_return_null_when_current_user_has_no_entreprise() {
        UserPrincipal adminPrincipal = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "admin", null, null, "ADMIN", List.of("ADMIN_ACCESS"));
        when(currentUserService.getCurrent()).thenReturn(adminPrincipal);

        UUID result = service.findCurrentUserCountryId();

        assertThat(result).isNull();
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw test -Dtest=EntrepriseServiceImplTest`
Expected: compile error — `findCurrentUserCountryId` doesn't exist yet.

- [ ] **Step 3: Add `findCurrentUserCountryId` to `IEntrepriseService` and `EntrepriseServiceImpl`**

Add to `IEntrepriseService.java` (near `findCurrentUserEntreprise`):

```java
    UUID findCurrentUserCountryId();
```

Add to `EntrepriseServiceImpl.java` (near `findCurrentUserEntreprise`):

```java
    @Override
    public UUID findCurrentUserCountryId() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        if (entrepriseId == null) {
            return null;
        }

        return entrepriseDomainService.findById(entrepriseId).getCountry().getId();
    }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./mvnw test -Dtest=EntrepriseServiceImplTest`
Expected: PASS (all, including the 2 new tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/store/entreprise/application/service/IEntrepriseService.java \
        src/main/java/org/store/entreprise/application/service/impl/EntrepriseServiceImpl.java \
        src/test/java/org/store/entreprise/application/service/EntrepriseServiceImplTest.java
git commit -m "feat(entreprise): add findCurrentUserCountryId for country-aware payment filtering"
```

- [ ] **Step 6: Write the failing service tests for `MoyenPaiementServiceImpl.findSelectItems`**

Add these imports to `MoyenPaiementServiceImplTest.java`: `org.springframework.data.domain.Page`, `org.mockito.ArgumentCaptor`, `org.store.entreprise.application.service.IEntrepriseService`, `org.store.paiement.application.dto.MoyenPaiementSelectFilter` — plus the static import `org.mockito.Mockito.verify`.

Update `setUp()` to add the new dependency:

```java
    private MoyenPaiementDomainService domainService;
    private ValidatorService validatorService;
    private CountryDomainService countryDomainService;
    private IEntrepriseService entrepriseService;
    private MoyenPaiementServiceImpl service;

    @BeforeEach
    void setUp() {
        domainService = mock(MoyenPaiementDomainService.class);
        validatorService = mock(ValidatorService.class);
        countryDomainService = mock(CountryDomainService.class);
        entrepriseService = mock(IEntrepriseService.class);
        service = new MoyenPaiementServiceImpl(domainService, validatorService, countryDomainService, entrepriseService);
        when(domainService.save(any(MoyenPaiement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
```

Append these 3 tests:

```java
    @Test
    void findSelectItems_should_use_explicit_countryId_when_provided() {
        UUID countryId = UUID.randomUUID();
        when(domainService.findSelectItems(any(MoyenPaiementSelectFilter.class))).thenReturn(Page.empty());

        service.findSelectItems(new MoyenPaiementSelectFilter(countryId, null, 0, 10));

        ArgumentCaptor<MoyenPaiementSelectFilter> captor = ArgumentCaptor.forClass(MoyenPaiementSelectFilter.class);
        verify(domainService).findSelectItems(captor.capture());
        assertThat(captor.getValue().countryId()).isEqualTo(countryId);
        verifyNoInteractions(entrepriseService);
    }

    @Test
    void findSelectItems_should_resolve_country_from_current_entreprise_when_countryId_absent() {
        UUID resolvedCountryId = UUID.randomUUID();
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(resolvedCountryId);
        when(domainService.findSelectItems(any(MoyenPaiementSelectFilter.class))).thenReturn(Page.empty());

        service.findSelectItems(new MoyenPaiementSelectFilter(null, null, 0, 10));

        ArgumentCaptor<MoyenPaiementSelectFilter> captor = ArgumentCaptor.forClass(MoyenPaiementSelectFilter.class);
        verify(domainService).findSelectItems(captor.capture());
        assertThat(captor.getValue().countryId()).isEqualTo(resolvedCountryId);
    }

    @Test
    void findSelectItems_should_return_unfiltered_when_no_countryId_and_no_current_entreprise() {
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(null);
        when(domainService.findSelectItems(any(MoyenPaiementSelectFilter.class))).thenReturn(Page.empty());

        service.findSelectItems(new MoyenPaiementSelectFilter(null, null, 0, 10));

        ArgumentCaptor<MoyenPaiementSelectFilter> captor = ArgumentCaptor.forClass(MoyenPaiementSelectFilter.class);
        verify(domainService).findSelectItems(captor.capture());
        assertThat(captor.getValue().countryId()).isNull();
    }
```

- [ ] **Step 7: Run tests to verify they fail**

Run: `./mvnw test -Dtest=MoyenPaiementServiceImplTest`
Expected: compile error — `MoyenPaiementSelectFilter` doesn't exist yet, `findSelectItems` doesn't exist on `MoyenPaiementServiceImpl`/`MoyenPaiementDomainService`, and the constructor doesn't take an `IEntrepriseService` yet.

- [ ] **Step 8: Create `MoyenPaiementSelectFilter`**

Create `src/main/java/org/store/paiement/application/dto/MoyenPaiementSelectFilter.java`:

```java
package org.store.paiement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record MoyenPaiementSelectFilter(
        UUID countryId,
        String searchTerm,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
```

- [ ] **Step 9: Add the repository query with explicit JPQL aliases**

In `MoyenPaiementRepository.java` (domain port), add imports `org.springframework.data.domain.Page`, `org.springframework.data.domain.Pageable`, `org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`, `org.store.common.dto.DataSelect`, then add:

```java
    @Query(value = """
            SELECT DISTINCT new org.store.common.dto.DataSelect(CAST(moyenPaiement.id AS string), moyenPaiement.libelle)
            FROM MoyenPaiement moyenPaiement
            LEFT JOIN moyenPaiement.pays pays
            WHERE moyenPaiement.actif = true
              AND (:countryId IS NULL OR pays IS NULL OR pays.id = :countryId)
              AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(moyenPaiement.libelle) LIKE :searchPattern)
            ORDER BY moyenPaiement.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(DISTINCT moyenPaiement.id)
            FROM MoyenPaiement moyenPaiement
            LEFT JOIN moyenPaiement.pays pays
            WHERE moyenPaiement.actif = true
              AND (:countryId IS NULL OR pays IS NULL OR pays.id = :countryId)
              AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(moyenPaiement.libelle) LIKE :searchPattern)
            """)
    Page<DataSelect> findSelectItems(@Param("countryId") UUID countryId,
                                      @Param("searchTerm") String searchTerm,
                                      @Param("searchPattern") String searchPattern,
                                      Pageable pageable);
```

(Spring Data `@Query` methods are explicitly exempt from the 3-parameter rule — rule 30/33.)

- [ ] **Step 10: Add the domain service delegation — destructures the filter**

In `MoyenPaiementDomainService.java`, add imports `org.springframework.data.domain.Page`, `org.store.common.dto.DataSelect`, `org.store.common.tools.LikePatternHelper`, `org.store.paiement.application.dto.MoyenPaiementSelectFilter`, then add:

```java
    public Page<DataSelect> findSelectItems(MoyenPaiementSelectFilter filter) {
        String searchPattern = LikePatternHelper.toLikePattern(filter.searchTerm());

        return repository.findSelectItems(filter.countryId(), filter.searchTerm(), searchPattern, filter.toPageable());
    }
```

- [ ] **Step 11: Add the application service method — single-param, resolves an effective filter**

Add to `IMoyenPaiementService.java` (imports `org.springframework.data.domain.Page`, `org.store.common.dto.DataSelect`, `org.store.paiement.application.dto.MoyenPaiementSelectFilter`):

```java
    Page<DataSelect> findSelectItems(MoyenPaiementSelectFilter filter);
```

Add the 4th constructor dependency and the method to `MoyenPaiementServiceImpl.java` (imports `org.springframework.data.domain.Page`, `org.store.common.dto.DataSelect`, `org.store.entreprise.application.service.IEntrepriseService`, `org.store.paiement.application.dto.MoyenPaiementSelectFilter`):

```java
    private final MoyenPaiementDomainService domainService;
    private final ValidatorService validatorService;
    private final CountryDomainService countryDomainService;
    private final IEntrepriseService entrepriseService;

    public MoyenPaiementServiceImpl(MoyenPaiementDomainService domainService,
                                    ValidatorService validatorService,
                                    CountryDomainService countryDomainService,
                                    IEntrepriseService entrepriseService) {
        this.domainService = domainService;
        this.validatorService = validatorService;
        this.countryDomainService = countryDomainService;
        this.entrepriseService = entrepriseService;
    }
```

```java
    @Override
    public Page<DataSelect> findSelectItems(MoyenPaiementSelectFilter filter) {
        validatorService.validate(filter);

        UUID resolvedCountryId = filter.countryId() != null ? filter.countryId() : entrepriseService.findCurrentUserCountryId();
        MoyenPaiementSelectFilter effectiveFilter = new MoyenPaiementSelectFilter(resolvedCountryId, filter.searchTerm(), filter.page(), filter.size());

        return domainService.findSelectItems(effectiveFilter);
    }
```

- [ ] **Step 12: Add the controller endpoint**

In `MoyenPaiementController.java`, add imports `org.springframework.data.domain.Page`, `org.store.common.dto.DataSelect`, `org.store.paiement.application.dto.MoyenPaiementSelectFilter`, then add (before the `@PostMapping create` method — external query param stays `q` per REST convention, rule 32, while the internal Java variable is `searchTerm`):

```java
    @GetMapping("/select")
    public ResponseEntity<Page<DataSelect>> select(@RequestParam(required = false) UUID countryId,
                                                     @RequestParam(value = "q", required = false) String searchTerm,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        MoyenPaiementSelectFilter filter = new MoyenPaiementSelectFilter(countryId, searchTerm, page, size);
        return ResponseEntity.ok(service.findSelectItems(filter));
    }
```

- [ ] **Step 13: Run the service tests to verify they pass**

Run: `./mvnw test -Dtest=MoyenPaiementServiceImplTest`
Expected: PASS (7/7).

- [ ] **Step 14: Write the controller test**

Create `src/test/java/org/store/paiement/presentation/MoyenPaiementControllerTest.java`:

```java
package org.store.paiement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.dto.DataSelect;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.application.service.IMoyenPaiementService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoyenPaiementControllerTest {

    private MockMvc mockMvc;
    private IMoyenPaiementService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(IMoyenPaiementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new MoyenPaiementController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void select_should_pass_query_params_to_service() throws Exception {
        UUID countryId = UUID.randomUUID();
        DataSelect item = new DataSelect(UUID.randomUUID().toString(), "Wave");
        when(service.findSelectItems(new MoyenPaiementSelectFilter(countryId, "wa", 0, 10)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(MoyenPaiementController.BASE_PATH + "/select")
                        .param("countryId", countryId.toString())
                        .param("q", "wa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].label").value("Wave"));
    }

    @Test
    void select_should_work_without_countryId() throws Exception {
        when(service.findSelectItems(new MoyenPaiementSelectFilter(null, null, 0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get(MoyenPaiementController.BASE_PATH + "/select"))
                .andExpect(status().isOk());
    }

    @Test
    void create_should_return_201_with_paysIds() throws Exception {
        MoyenPaiementRequest body = new MoyenPaiementRequest("Wave", Set.of());
        when(service.create(any(MoyenPaiementRequest.class)))
                .thenReturn(new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true, List.of()));

        mockMvc.perform(post(MoyenPaiementController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Wave"));
    }
}
```

- [ ] **Step 15: Run the controller test**

Run: `./mvnw test -Dtest=MoyenPaiementControllerTest`
Expected: PASS (3/3).

- [ ] **Step 16: Commit**

```bash
git add src/main/java/org/store/paiement/application/dto/MoyenPaiementSelectFilter.java \
        src/main/java/org/store/paiement/domain/repository/MoyenPaiementRepository.java \
        src/main/java/org/store/paiement/domain/service/MoyenPaiementDomainService.java \
        src/main/java/org/store/paiement/application/service/IMoyenPaiementService.java \
        src/main/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImpl.java \
        src/main/java/org/store/paiement/presentation/MoyenPaiementController.java \
        src/test/java/org/store/paiement/application/service/impl/MoyenPaiementServiceImplTest.java \
        src/test/java/org/store/paiement/presentation/MoyenPaiementControllerTest.java
git commit -m "feat(paiement): add country-filtered GET /moyens-paiement/select endpoint"
```

---

### Task 4: Frontend types + api adapter + `useMoyenPaiementSelectList`

> **Convention check** (`.claude/FRONTEND_CODING_CONVENTIONS.md` rule 32): parameter named `q` is the rule's own textbook example of what to fix (`searchTerm` instead of `q`) — the new hook below uses `searchTerm`. Sibling hooks `useEntrepriseSelectList`/`useCategoryDepensePlateformeSelectList` still use `q` (pre-existing, out of scope to rename here).

**Files:**
- Modify: `store-frontend/src/features/moyen-paiement/domain/dtos/moyen-paiement-response.ts`
- Modify: `store-frontend/src/features/moyen-paiement/infrastructure/moyen-paiement-api.ts`
- Modify: `store-frontend/src/features/moyen-paiement/application/useCreateMoyenPaiement.ts`
- Modify: `store-frontend/src/features/moyen-paiement/application/useUpdateMoyenPaiement.ts`
- Create: `store-frontend/src/features/moyen-paiement/application/useMoyenPaiementSelectList.ts`

**Interfaces:**
- Consumes: `DataSelect` type (`@/common/domain/dtos/data-select`, existing), `PageResponse<T>` (`@/common/domain/dtos/page-response`, existing), `Country` type (`@/features/country/domain/dtos/country`, existing).
- Produces: `useMoyenPaiementSelectList(countryId?: string, searchTerm = '', page = 0, size = 100, enabled = true)` returning a `UseQueryResult<PageResponse<DataSelect>>`; `moyenPaiementApi.create/update` now take `{ libelle: string; paysIds: string[] }`.

- [ ] **Step 1: Update the response type**

Replace `store-frontend/src/features/moyen-paiement/domain/dtos/moyen-paiement-response.ts`:

```ts
import type { Country } from '@/features/country/domain/dtos/country'

/**
 * Miroir frontend de `org.store.paiement.application.dto.MoyenPaiementResponse`.
 * Retourné par `GET /api/v1/moyens-paiement` et imbriqué dans les
 * réponses de paiement (vente, achat, abonnement, dépense).
 */
export type MoyenPaiementResponse = {
  id: string
  libelle: string
  actif: boolean
  pays: Country[]
}
```

- [ ] **Step 2: Update the api adapter**

In `store-frontend/src/features/moyen-paiement/infrastructure/moyen-paiement-api.ts`, change:

```ts
type MoyenPaiementRequest = { libelle: string }
```

to:

```ts
type MoyenPaiementRequest = { libelle: string; paysIds: string[] }
```

(the `create`/`update` function bodies are unchanged — they already forward `payload` as-is).

- [ ] **Step 3: Update the mutation hooks' payload types**

In `useCreateMoyenPaiement.ts`, change:
```ts
mutationFn: (payload: { libelle: string }) => moyenPaiementApi.create(payload),
```
to:
```ts
mutationFn: (payload: { libelle: string; paysIds: string[] }) => moyenPaiementApi.create(payload),
```

In `useUpdateMoyenPaiement.ts`, change:
```ts
mutationFn: ({ id, payload }: { id: string; payload: { libelle: string } }) =>
```
to:
```ts
mutationFn: ({ id, payload }: { id: string; payload: { libelle: string; paysIds: string[] } }) =>
```

- [ ] **Step 4: Create the select hook**

Create `store-frontend/src/features/moyen-paiement/application/useMoyenPaiementSelectList.ts`:

```ts
'use client'

import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/common/infrastructure/api-client'
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { DataSelect } from '@/common/domain/dtos/data-select'

/**
 * `GET /api/v1/moyens-paiement/select` — moyens actifs filtrés par pays.
 * `countryId` omis : le pays de l'entreprise courante est résolu côté
 * serveur (tenant) ou aucun filtre n'est appliqué (ADMIN sans entreprise).
 */
export function useMoyenPaiementSelectList(countryId?: string, searchTerm = '', page = 0, size = 100, enabled = true) {
  return useQuery({
    queryKey: ['moyens-paiement', 'select', countryId ?? null, searchTerm, page, size],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size }
      if (countryId) params.countryId = countryId
      if (searchTerm && searchTerm.trim() !== '') params.q = searchTerm.trim()
      const { data } = await apiClient.get<PageResponse<DataSelect>>(
        '/api/v1/moyens-paiement/select',
        { params },
      )
      return data
    },
    enabled,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
  })
}
```

- [ ] **Step 5: Verify types compile**

Run: `cd store-frontend && npx tsc --noEmit`
Expected: errors only in files not yet updated (`MoyenPaiementFormDialog.tsx` still calling mutations with the old 1-field payload, and any component reading `MoyenPaiementResponse` without `pays`) — these are fixed in Tasks 5–11. If `tsc` reports errors in files this task didn't touch, note them; do not fix files outside this task's scope here.

- [ ] **Step 6: Commit**

```bash
git add store-frontend/src/features/moyen-paiement/domain/dtos/moyen-paiement-response.ts \
        store-frontend/src/features/moyen-paiement/infrastructure/moyen-paiement-api.ts \
        store-frontend/src/features/moyen-paiement/application/useCreateMoyenPaiement.ts \
        store-frontend/src/features/moyen-paiement/application/useUpdateMoyenPaiement.ts \
        store-frontend/src/features/moyen-paiement/application/useMoyenPaiementSelectList.ts
git commit -m "feat(moyen-paiement): add country-filtered select hook and paysIds payload"
```

---

### Task 5: `MoyenPaiementFormDialog.tsx` — country multi-select

**Files:**
- Modify: `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementFormDialog.tsx`
- Modify: `store-frontend/src/messages/fr.json`, `store-frontend/src/messages/en.json`

**Interfaces:**
- Consumes: `useCountries()` (`@/features/country/application/useCountries`, existing, returns `Country[]`), `Checkbox` (`@/common/presentation/ui/checkbox`, existing, `checked: boolean`/`onCheckedChange: (checked: boolean) => void`), `useCreateMoyenPaiement`/`useUpdateMoyenPaiement` (Task 4's updated payload type).

- [ ] **Step 1: Add i18n keys**

In `store-frontend/src/messages/fr.json`, inside `dashboard.administration.moyensPaiement.form`, add after `"libellePlaceholder"`:
```json
    "paysLabel": "Pays",
    "paysHint": "Aucun pays sélectionné = disponible partout",
```

In `store-frontend/src/messages/en.json`, inside the same path, add:
```json
    "paysLabel": "Countries",
    "paysHint": "No country selected = available everywhere",
```

- [ ] **Step 2: Update the form**

Replace `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementFormDialog.tsx`:

```tsx
'use client'
import { zodResolver } from '@hookform/resolvers/zod'
import { useTranslations } from 'next-intl'
import { useEffect, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { Checkbox } from '@/common/presentation/ui/checkbox'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/common/presentation/ui/dialog'
import { FormField } from '@/common/presentation/shared/FormField'
import { Label } from '@/common/presentation/ui/label'
import { useCountries } from '@/features/country/application/useCountries'
import { useCreateMoyenPaiement } from '@/features/moyen-paiement/application/useCreateMoyenPaiement'
import { useUpdateMoyenPaiement } from '@/features/moyen-paiement/application/useUpdateMoyenPaiement'
import type { MoyenPaiementResponse } from '@/features/moyen-paiement/domain/dtos/moyen-paiement-response'

type FormValues = { libelle: string; paysIds: string[] }

type MoyenPaiementFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  target?: MoyenPaiementResponse
}

export function MoyenPaiementFormDialog({ open, onOpenChange, target }: MoyenPaiementFormDialogProps) {
  const t = useTranslations('dashboard.administration.moyensPaiement')
  const tForm = useTranslations('dashboard.administration.moyensPaiement.form')
  const tToast = useTranslations('dashboard.administration.moyensPaiement.toasts')
  const isEdit = Boolean(target)
  const countriesQuery = useCountries()

  const schema = useMemo(
    () => z.object({
      libelle: z.string().min(1, t('validation.libelleRequired')).max(100, t('validation.libelleMax')),
      paysIds: z.array(z.string()),
    }),
    [t],
  )

  const defaultValues: FormValues = {
    libelle: target?.libelle ?? '',
    paysIds: target?.pays.map((p) => p.id) ?? [],
  }

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues,
  })

  useEffect(() => {
    if (open) form.reset(defaultValues)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, target, form])

  const createMutation = useCreateMoyenPaiement()
  const updateMutation = useUpdateMoyenPaiement()
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  function toggleCountry(id: string, checked: boolean) {
    const current = form.getValues('paysIds')
    form.setValue(
      'paysIds',
      checked ? [...current, id] : current.filter((x) => x !== id),
      { shouldDirty: true },
    )
  }

  function handleSubmit(values: FormValues) {
    if (isEdit && target) {
      runMutationWithToast(updateMutation, { id: target.id, payload: values }, {
        successMessage: tToast('updated'),
        onSuccess: () => onOpenChange(false),
      })
    } else {
      runMutationWithToast(createMutation, values, {
        successMessage: tToast('created'),
        onSuccess: () => onOpenChange(false),
      })
    }
  }

  const selectedPaysIds = form.watch('paysIds')

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{isEdit ? tForm('editTitle') : tForm('createTitle')}</DialogTitle>
          <DialogDescription>{isEdit ? tForm('editDescription') : tForm('createDescription')}</DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="flex flex-col gap-4">
          <FormField<FormValues> name="libelle" label={tForm('libelleLabel')} placeholder={tForm('libellePlaceholder')} required />
          <div className="space-y-2">
            <Label>{tForm('paysLabel')}</Label>
            <div className="flex max-h-40 flex-col gap-2 overflow-y-auto rounded-md border border-border p-3">
              {(countriesQuery.data ?? []).map((country) => (
                <label key={country.id} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={selectedPaysIds.includes(country.id)}
                    onCheckedChange={(checked) => toggleCountry(country.id, checked === true)}
                  />
                  {country.name}
                </label>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">{tForm('paysHint')}</p>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>{tForm('cancel')}</Button>
            <Button type="submit" disabled={isSubmitting}>{isSubmitting ? tForm('submitting') : tForm('submit')}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
```

- [ ] **Step 3: Verify types compile**

Run: `cd store-frontend && npx tsc --noEmit`
Expected: no error in this file.

- [ ] **Step 4: Commit**

```bash
git add store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementFormDialog.tsx \
        store-frontend/src/messages/fr.json store-frontend/src/messages/en.json
git commit -m "feat(moyen-paiement): add country multi-select to the admin form"
```

---

### Task 6: `MoyenPaiementTable.tsx` — Pays column

**Files:**
- Modify: `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementTable.tsx`
- Modify: `store-frontend/src/messages/fr.json`, `store-frontend/src/messages/en.json`

- [ ] **Step 1: Add i18n keys**

In `fr.json`, inside `dashboard.administration.moyensPaiement.table`, add after `"statut"`:
```json
    "pays": "Pays",
    "paysGlobal": "Global"
```

In `en.json`, same path:
```json
    "pays": "Countries",
    "paysGlobal": "Global"
```

- [ ] **Step 2: Add the column**

In `MoyenPaiementTable.tsx`, change the header row (lines 46-50):
```tsx
          <TableRow>
            <TableHead>{t('table.libelle')}</TableHead>
            <TableHead>{t('table.statut')}</TableHead>
            <TableHead className="w-10" />
          </TableRow>
```
to:
```tsx
          <TableRow>
            <TableHead>{t('table.libelle')}</TableHead>
            <TableHead>{t('table.pays')}</TableHead>
            <TableHead>{t('table.statut')}</TableHead>
            <TableHead className="w-10" />
          </TableRow>
```

Then change the body row (lines 54-60):
```tsx
            <TableRow key={row.id}>
              <TableCell className="font-medium">{row.libelle}</TableCell>
              <TableCell>
                <Badge variant={row.actif ? 'default' : 'secondary'}>
                  {row.actif ? t('badge.actif') : t('badge.inactif')}
                </Badge>
              </TableCell>
```
to:
```tsx
            <TableRow key={row.id}>
              <TableCell className="font-medium">{row.libelle}</TableCell>
              <TableCell>
                {row.pays.length === 0 ? (
                  <Badge variant="outline">{t('table.paysGlobal')}</Badge>
                ) : (
                  row.pays.map((p) => p.name).join(', ')
                )}
              </TableCell>
              <TableCell>
                <Badge variant={row.actif ? 'default' : 'secondary'}>
                  {row.actif ? t('badge.actif') : t('badge.inactif')}
                </Badge>
              </TableCell>
```

- [ ] **Step 3: Verify types compile**

Run: `cd store-frontend && npx tsc --noEmit`
Expected: no error in this file.

- [ ] **Step 4: Commit**

```bash
git add store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementTable.tsx \
        store-frontend/src/messages/fr.json store-frontend/src/messages/en.json
git commit -m "feat(moyen-paiement): show linked countries in the admin table"
```

---

### Task 7: `MoyenPaiementSelect.tsx` — `countryId` prop

**Files:**
- Modify: `store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementSelect.tsx`

**Interfaces:**
- Consumes: `useMoyenPaiementSelectList` (Task 4).
- Produces: `MoyenPaiementSelectProps` gains an optional `countryId?: string`. Its only current consumer is `SubmitPaiementForm.tsx` (abonnement) — Task 2 (the separate Facturation plan) replaces that usage entirely; updating this component now is still correct since it's the shared, documented component for this purpose.

- [ ] **Step 1: Replace the component**

```tsx
'use client'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'

type MoyenPaiementSelectProps = {
  value: string
  onValueChange: (value: string) => void
  ariaLabel?: string
  countryId?: string
}

/**
 * Select partagé pour choisir un moyen de paiement actif, filtré par
 * pays (le pays de l'entreprise courante si `countryId` est omis).
 */
export function MoyenPaiementSelect({
  value,
  onValueChange,
  ariaLabel,
  countryId,
}: MoyenPaiementSelectProps) {
  const moyensQuery = useMoyenPaiementSelectList(countryId)
  const options = moyensQuery.data?.content ?? []
  const selectedLibelle = options.find((m) => m.value === value)?.label ?? '—'

  return (
    <Select value={value} onValueChange={(v) => onValueChange(v ?? '')}>
      <SelectTrigger aria-label={ariaLabel} className="w-full">
        <SelectValue>{selectedLibelle}</SelectValue>
      </SelectTrigger>
      <SelectContent>
        {options.map((moyen) => (
          <SelectItem key={moyen.value} value={moyen.value}>
            {moyen.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
```

- [ ] **Step 2: Verify types compile**

Run: `cd store-frontend && npx tsc --noEmit`
Expected: no error in this file.

- [ ] **Step 3: Commit**

```bash
git add store-frontend/src/features/moyen-paiement/presentation/MoyenPaiementSelect.tsx
git commit -m "feat(moyen-paiement): filter MoyenPaiementSelect by country"
```

---

### Task 8: Vente forms — swap to the country-filtered hook

**Files:**
- Modify: `store-frontend/src/features/vente/presentation/VenteForm.tsx`
- Modify: `store-frontend/src/features/vente/presentation/ValiderVenteDialog.tsx`
- Modify: `store-frontend/src/features/vente/presentation/CreatePaiementVenteDialog.tsx`

All 3 follow the identical pattern: a raw `<Select>` fed directly by `moyensQuery.data` (no intermediate `moyenItems` memo). No `countryId` is passed — auto-resolved server-side from the current user's entreprise.

- [ ] **Step 1: `VenteForm.tsx`**

Change the import:
```ts
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
```
to:
```ts
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'
```

Change:
```ts
  const moyensQuery = useMoyenPaiementList()
```
to:
```ts
  const moyensQuery = useMoyenPaiementSelectList()
```

Change:
```tsx
                        {moyensQuery.data?.find((m) => m.id === field.value)?.libelle ?? '—'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data ?? []).map((moyen) => (
                        <SelectItem key={moyen.id} value={moyen.id}>{moyen.libelle}</SelectItem>
                      ))}
```
to:
```tsx
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? '—'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>{moyen.label}</SelectItem>
                      ))}
```

- [ ] **Step 2: `ValiderVenteDialog.tsx`**

Same import/hook swap as Step 1. Then change:
```tsx
                            {moyensQuery.data?.find((m) => m.id === field.value)?.libelle ??
                              t('paiement.fields.moyenPlaceholder')}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {(moyensQuery.data ?? []).map((moyen) => (
                            <SelectItem key={moyen.id} value={moyen.id}>
                              {moyen.libelle}
                            </SelectItem>
                          ))}
```
to:
```tsx
                            {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ??
                              t('paiement.fields.moyenPlaceholder')}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {(moyensQuery.data?.content ?? []).map((moyen) => (
                            <SelectItem key={moyen.value} value={moyen.value}>
                              {moyen.label}
                            </SelectItem>
                          ))}
```

- [ ] **Step 3: `CreatePaiementVenteDialog.tsx`**

Same import/hook swap as Step 1. Then change:
```tsx
                        {moyensQuery.data?.find((m) => m.id === field.value)?.libelle ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data ?? []).map((moyen) => (
                        <SelectItem key={moyen.id} value={moyen.id}>
                          {moyen.libelle}
                        </SelectItem>
                      ))}
```
to:
```tsx
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>
                          {moyen.label}
                        </SelectItem>
                      ))}
```

- [ ] **Step 4: Verify types compile and existing tests still pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: `tsc` clean for these 3 files; `vitest run` stays at the current baseline count — confirmed no test file exists for `VenteForm`/`ValiderVenteDialog`/`CreatePaiementVenteDialog` today (`find src -iname "VenteForm.test.*" -o -iname "ValiderVenteDialog.test.*" -o -iname "CreatePaiementVenteDialog.test.*"` returns nothing), so there is no mock to update.

- [ ] **Step 5: Commit**

```bash
git add store-frontend/src/features/vente/presentation/VenteForm.tsx \
        store-frontend/src/features/vente/presentation/ValiderVenteDialog.tsx \
        store-frontend/src/features/vente/presentation/CreatePaiementVenteDialog.tsx
git commit -m "feat(vente): filter payment-method choices by entreprise country"
```

---

### Task 9: Achat dialogs — swap to the country-filtered hook

**Files:**
- Modify: `store-frontend/src/features/achat/presentation/ReceiveAchatDialog.tsx`
- Modify: `store-frontend/src/features/achat/presentation/CreatePaiementAchatDialog.tsx`

- [ ] **Step 1: `ReceiveAchatDialog.tsx`**

Same import/hook swap as Task 8 Step 1. Then change:
```tsx
                          {moyensQuery.data?.find((m) => m.id === field.value)?.libelle ?? t('paiement.fields.moyenPlaceholder')}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {(moyensQuery.data ?? []).map((moyen) => (
                          <SelectItem key={moyen.id} value={moyen.id}>
                            {moyen.libelle}
                          </SelectItem>
                        ))}
```
to:
```tsx
                          {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('paiement.fields.moyenPlaceholder')}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {(moyensQuery.data?.content ?? []).map((moyen) => (
                          <SelectItem key={moyen.value} value={moyen.value}>
                            {moyen.label}
                          </SelectItem>
                        ))}
```

- [ ] **Step 2: `CreatePaiementAchatDialog.tsx`**

Same import/hook swap. Then change:
```tsx
                        {moyensQuery.data?.find((m) => m.id === field.value)?.libelle ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data ?? []).map((moyen) => (
                        <SelectItem key={moyen.id} value={moyen.id}>
                          {moyen.libelle}
                        </SelectItem>
                      ))}
```
to:
```tsx
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>
                          {moyen.label}
                        </SelectItem>
                      ))}
```

- [ ] **Step 3: Verify types compile and existing tests still pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: clean; update any test mock of `useMoyenPaiementList` in this module's test files to the new `useMoyenPaiementSelectList`/`{content: [...]}` shape.

- [ ] **Step 4: Commit**

```bash
git add store-frontend/src/features/achat/presentation/ReceiveAchatDialog.tsx \
        store-frontend/src/features/achat/presentation/CreatePaiementAchatDialog.tsx
git commit -m "feat(achat): filter payment-method choices by entreprise country"
```

---

### Task 10: Dépense (tenant) form + filters — swap to the country-filtered hook

**Files:**
- Modify: `store-frontend/src/features/depense/presentation/DepenseForm.tsx`
- Modify: `store-frontend/src/features/depense/presentation/DepenseFilters.tsx`

Both already build a `moyenItems` memo — since `DataSelect` already has the exact `{value, label}` shape the `Combobox` expects, this simplifies to a direct assignment (no `.map()`, matching the project's established "zero client-side transform" convention for `/select` endpoints).

- [ ] **Step 1: `DepenseForm.tsx`**

Change the import:
```ts
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
```
to:
```ts
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'
```

Change:
```ts
  const moyensQuery = useMoyenPaiementList()
```
to:
```ts
  const moyensQuery = useMoyenPaiementSelectList()
```

Change:
```ts
  const moyenItems = useMemo(
    () => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })),
    [moyensQuery.data],
  )
```
to:
```ts
  const moyenItems = moyensQuery.data?.content ?? []
```

(remove the now-unused `useMemo` import only if this file has no other `useMemo` usage — check the rest of the file before removing the import; leave the import if another `useMemo` call remains).

- [ ] **Step 2: `DepenseFilters.tsx`**

Same import/hook swap as Step 1. Change:
```ts
  const moyenItems = useMemo(
    () => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })),
    [moyensQuery.data],
  )
```
to:
```ts
  const moyenItems = moyensQuery.data?.content ?? []
```

(this file also builds `actifItems`/`countryItems` etc. with `useMemo` elsewhere — check before removing the `useMemo` import; keep it if still used).

- [ ] **Step 3: Verify types compile and existing tests still pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: clean; confirmed no test file exists for `ReceiveAchatDialog`/`CreatePaiementAchatDialog` today, so no mock update is needed.

- [ ] **Step 4: Commit**

```bash
git add store-frontend/src/features/depense/presentation/DepenseForm.tsx \
        store-frontend/src/features/depense/presentation/DepenseFilters.tsx
git commit -m "feat(depense): filter payment-method choices by entreprise country"
```

---

### Task 11: Dépenses plateforme (admin) — reactive `countryId`

**Files:**
- Modify: `store-frontend/src/features/plateforme-depense/presentation/DepensePlateformeForm.tsx`
- Modify: `store-frontend/src/features/plateforme-depense/presentation/DepensePlateformeFilters.tsx`

Per the confirmed behavior (spec §1.5): no country selected in the form/filter → ADMIN sees everything (falls out naturally, since the caller is ADMIN with no `entrepriseId` and no explicit `countryId` is passed); a country selected → global + that country's moyens.

- [ ] **Step 1: `DepensePlateformeForm.tsx`**

Change the import:
```ts
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
```
to:
```ts
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'
```

`form.watch` needs `form` to exist first, so `moyensQuery` must move below the `useForm` call. Remove the old declaration (currently right after `categoriesQuery`):
```ts
  const categoriesQuery = useCategoryDepensePlateformeSelectList(categorySearch)
  const moyensQuery = useMoyenPaiementList()
  const countriesQuery = useCountries()
```
becomes:
```ts
  const categoriesQuery = useCategoryDepensePlateformeSelectList(categorySearch)
  const countriesQuery = useCountries()
```

Then add the new declaration right after `const form = useForm<FormValues>({ resolver: zodResolver(schema), mode: 'onTouched', defaultValues })`:
```ts
  const form = useForm<FormValues>({ resolver: zodResolver(schema), mode: 'onTouched', defaultValues })
  const watchedCountryId = form.watch('countryId')
  const moyensQuery = useMoyenPaiementSelectList(watchedCountryId || undefined)
```

Change:
```ts
  const moyenItems = useMemo(() => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })), [moyensQuery.data])
```
to:
```ts
  const moyenItems = moyensQuery.data?.content ?? []
```

- [ ] **Step 2: `DepensePlateformeFilters.tsx`**

Change the import:
```ts
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
```
to:
```ts
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'
```

Change:
```ts
  const moyensQuery = useMoyenPaiementList()
```
to:
```ts
  const moyensQuery = useMoyenPaiementSelectList(countryId || undefined)
```

(`countryId` is already a destructured prop of this component — no need to introduce a new watch, it's the filter's own controlled value.)

Change:
```ts
  const moyenItems = useMemo(() => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })), [moyensQuery.data])
```
to:
```ts
  const moyenItems = moyensQuery.data?.content ?? []
```

- [ ] **Step 3: Verify types compile and existing tests still pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: clean; confirmed no test file exists for `DepensePlateformeForm`/`DepensePlateformeFilters` today, so no mock update is needed.

- [ ] **Step 4: Manual check**

Start the dev server, open the "Dépenses plateforme" admin page's create form: with no country selected, the moyen combobox should list every active moyen; selecting a country should narrow it to global + that country's moyens (needs at least one `MoyenPaiement` with a country attached via Task 5's admin form to observe the narrowing — create one manually for the check).

- [ ] **Step 5: Commit**

```bash
git add store-frontend/src/features/plateforme-depense/presentation/DepensePlateformeForm.tsx \
        store-frontend/src/features/plateforme-depense/presentation/DepensePlateformeFilters.tsx
git commit -m "feat(depense-plateforme): make moyen combobox reactive to the selected country"
```

---

### Task 12: Replace `<Select>` with `<Combobox>` in the 5 payment-method fields

> **Convention fix** (`FRONTEND_CODING_CONVENTIONS.md` rule 54 — flagged as an out-of-scope pre-existing gap in Global Constraints, now brought in scope by explicit request). `<Select>` has no search input; `<Combobox>` is the mandatory component for form/dialog pickers. This task only touches the payment-method field in the 5 files below — no other field in these forms is affected.

**Files:**
- Modify: `store-frontend/src/features/vente/presentation/VenteForm.tsx`
- Modify: `store-frontend/src/features/vente/presentation/ValiderVenteDialog.tsx`
- Modify: `store-frontend/src/features/vente/presentation/CreatePaiementVenteDialog.tsx`
- Modify: `store-frontend/src/features/achat/presentation/ReceiveAchatDialog.tsx`
- Modify: `store-frontend/src/features/achat/presentation/CreatePaiementAchatDialog.tsx`

**Interfaces:**
- Consumes: `Combobox` (`@/common/presentation/ui/combobox`, existing — `items: {value,label}[]`, `value`, `onValueChange`, `placeholder?`, `ariaLabel?`, `emptyLabel?`; no `aria-invalid` prop exists on it).
- Each file's `moyensQuery` already returns `PageResponse<DataSelect>` (Tasks 8–9) — `moyensQuery.data?.content ?? []` is already the exact `{value,label}[]` shape `Combobox` expects, no mapping needed.
- **Accepted minor UX change**: the 4 files that today set `aria-invalid={...}` on the `SelectTrigger` to show a red border on validation error lose that border — `Combobox` has no such prop. The error message paragraph below the field (already present in all 4 files) still renders unchanged, so the error is still communicated, just without the red outline on the trigger itself. Not fixed here — extending `Combobox` with an `aria-invalid`/error-styling prop is a separate concern, out of scope for a data-source/component swap.

- [ ] **Step 1: `VenteForm.tsx`**

Remove the import block (this file already imports `Combobox` — no new import needed):
```ts
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
```

Replace:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={(v) => field.onChange(v ?? '')}>
                    <SelectTrigger aria-label={t('payment.moyen')} className="w-full">
                      <SelectValue>
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? '—'}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>{moyen.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
```
with:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Combobox
                    items={moyensQuery.data?.content ?? []}
                    value={field.value}
                    onValueChange={(v) => field.onChange(v ?? '')}
                    placeholder={t('payment.moyen')}
                    ariaLabel={t('payment.moyen')}
                    emptyLabel="—"
                  />
                )}
              />
```

- [ ] **Step 2: `ValiderVenteDialog.tsx`**

Replace the import block:
```ts
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
```
with:
```ts
import { Combobox } from '@/common/presentation/ui/combobox'
```

Replace:
```tsx
                  <Controller
                    control={form.control}
                    name="paiementMoyen"
                    render={({ field }) => (
                      <Select
                        value={field.value}
                        onValueChange={(value) => field.onChange(value ?? '')}
                      >
                        <SelectTrigger
                          className="w-full"
                          aria-label={t('paiement.fields.moyen')}
                          aria-invalid={Boolean(paiementMoyenError)}
                        >
                          <SelectValue>
                            {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ??
                              t('paiement.fields.moyenPlaceholder')}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {(moyensQuery.data?.content ?? []).map((moyen) => (
                            <SelectItem key={moyen.value} value={moyen.value}>
                              {moyen.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
```
with:
```tsx
                  <Controller
                    control={form.control}
                    name="paiementMoyen"
                    render={({ field }) => (
                      <Combobox
                        items={moyensQuery.data?.content ?? []}
                        value={field.value}
                        onValueChange={(value) => field.onChange(value ?? '')}
                        placeholder={t('paiement.fields.moyenPlaceholder')}
                        ariaLabel={t('paiement.fields.moyen')}
                        emptyLabel="—"
                      />
                    )}
                  />
```

- [ ] **Step 3: `CreatePaiementVenteDialog.tsx`**

Replace the import block:
```ts
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
```
with:
```ts
import { Combobox } from '@/common/presentation/ui/combobox'
```

Replace:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={(value) => field.onChange(value ?? '')}
                  >
                    <SelectTrigger
                      className="w-full"
                      aria-label={t('fields.moyen')}
                      aria-required="true"
                      aria-invalid={Boolean(form.formState.errors.moyenPaiementId)}
                    >
                      <SelectValue>
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>
                          {moyen.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
```
with:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Combobox
                    items={moyensQuery.data?.content ?? []}
                    value={field.value}
                    onValueChange={(value) => field.onChange(value ?? '')}
                    placeholder={t('fields.moyenPlaceholder')}
                    ariaLabel={t('fields.moyen')}
                    emptyLabel="—"
                  />
                )}
              />
```

- [ ] **Step 4: `ReceiveAchatDialog.tsx`**

Replace the import block:
```ts
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
```
with:
```ts
import { Combobox } from '@/common/presentation/ui/combobox'
```

Replace:
```tsx
                <Controller
                  control={form.control}
                  name="paiementMoyen"
                  render={({ field }) => (
                    <Select
                      value={field.value}
                      onValueChange={(value) => field.onChange(value ?? '')}
                    >
                      <SelectTrigger
                        className="w-full"
                        aria-label={t('paiement.fields.moyen')}
                        aria-invalid={Boolean(paiementMoyenError)}
                      >
                        <SelectValue>
                          {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('paiement.fields.moyenPlaceholder')}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        {(moyensQuery.data?.content ?? []).map((moyen) => (
                          <SelectItem key={moyen.value} value={moyen.value}>
                            {moyen.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
```
with:
```tsx
                <Controller
                  control={form.control}
                  name="paiementMoyen"
                  render={({ field }) => (
                    <Combobox
                      items={moyensQuery.data?.content ?? []}
                      value={field.value}
                      onValueChange={(value) => field.onChange(value ?? '')}
                      placeholder={t('paiement.fields.moyenPlaceholder')}
                      ariaLabel={t('paiement.fields.moyen')}
                      emptyLabel="—"
                    />
                  )}
                />
```

- [ ] **Step 5: `CreatePaiementAchatDialog.tsx`**

Replace the import block:
```ts
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/common/presentation/ui/select'
```
with:
```ts
import { Combobox } from '@/common/presentation/ui/combobox'
```

Replace:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={(value) => field.onChange(value ?? '')}>
                    <SelectTrigger
                      className="w-full"
                      aria-label={t('fields.moyen')}
                      aria-required="true"
                      aria-invalid={Boolean(moyenError)}
                    >
                      <SelectValue>
                        {moyensQuery.data?.content.find((m) => m.value === field.value)?.label ?? t('fields.moyenPlaceholder')}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {(moyensQuery.data?.content ?? []).map((moyen) => (
                        <SelectItem key={moyen.value} value={moyen.value}>
                          {moyen.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
```
with:
```tsx
              <Controller
                control={form.control}
                name="moyenPaiementId"
                render={({ field }) => (
                  <Combobox
                    items={moyensQuery.data?.content ?? []}
                    value={field.value}
                    onValueChange={(value) => field.onChange(value ?? '')}
                    placeholder={t('fields.moyenPlaceholder')}
                    ariaLabel={t('fields.moyen')}
                    emptyLabel="—"
                  />
                )}
              />
```

- [ ] **Step 6: Verify types compile and existing tests still pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: clean. Confirmed earlier (Task 8/9) that none of these 5 files have a test file today, so no mock/assertion update is needed.

- [ ] **Step 7: Manual check**

Start the dev server. For each of the 5 flows (vente creation, vente validation payment, vente installment payment, achat receiving payment, achat installment payment): open the payment-method field and confirm it now shows a search input and filters as you type, and that selecting/clearing a value still works exactly as before.

- [ ] **Step 8: Commit**

```bash
git add store-frontend/src/features/vente/presentation/VenteForm.tsx \
        store-frontend/src/features/vente/presentation/ValiderVenteDialog.tsx \
        store-frontend/src/features/vente/presentation/CreatePaiementVenteDialog.tsx \
        store-frontend/src/features/achat/presentation/ReceiveAchatDialog.tsx \
        store-frontend/src/features/achat/presentation/CreatePaiementAchatDialog.tsx
git commit -m "refactor(vente,achat): replace Select with Combobox for payment-method fields"
```

---

### Task 13: Final verification

**Files:** none (verification only).

- [ ] **Step 1: Full backend suite**

Run: `./mvnw clean test`
Expected: all tests green (baseline was 1064 before this plan; expect 1064 + 15 new tests: 7 in `MoyenPaiementServiceImplTest`, 3 in `CountryDomainServiceTest`, 3 in `MoyenPaiementControllerTest`, 2 appended to `EntrepriseServiceImplTest`).

- [ ] **Step 2: Full frontend suite**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: `tsc` clean, vitest all green (baseline 342 + any test-mock updates from Tasks 8–11, no new test files planned).

- [ ] **Step 3: Manual QA checklist**

Using the `run` skill or a manual dev-server session:
- Admin → Moyens de paiement: create a moyen with 1 country attached, confirm the table shows that country's name (not "Global"); create one with 0 countries, confirm it shows "Global".
- As an OWNER/EMPLOYE in a given country, open the vente payment dialog, achat payment dialog, and dépense form: confirm the country-specific moyen from the previous step only appears for entreprises in that country, and the "Global" ones appear everywhere.
- Admin → Dépenses plateforme: confirm the country-reactive combobox behavior from Task 11 Step 4.
- Subscription payment submission (`SubmitPaiementForm`) is **not functionally unaffected** by this plan: it shares `MoyenPaiementSelect`, which Task 7 switches to the country-filtered `/select` endpoint. Today this is invisible (all 4 seeded moyens are global), but the first time an admin attaches a country to a moyen (via Task 5's form), subscription-payment moyen choices become filtered by the *paying tenant's own country* too — even though the tenant is paying the platform, not a local counterparty. Confirm with the user whether this transitional behavior is acceptable until the separate Facturation plan replaces this flow entirely, or whether `SubmitPaiementForm` needs to keep using an unfiltered picker in the meantime.

- [ ] **Step 4: Report**

Summarize pass/fail of Steps 1–3 back to the user. Do not commit anything in this task — it's verification-only.
