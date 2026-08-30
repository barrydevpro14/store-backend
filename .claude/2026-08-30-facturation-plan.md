# Facturation (Task 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a global, platform-level `Facturation` reference table (moyen de paiement + pays + numéro à créditer), ADMIN-managed, and use it to entirely replace how `SubmitPaiementForm` (subscription payment submission) picks a payment method today.

**Architecture:** New `Facturation` entity (`org.store.paiement.domain.model`, sibling of `MoyenPaiement`) with a FK to `MoyenPaiement`, an optional FK to `Country` (null = global), a `numeroFacturation` string, and `actif`. Full admin CRUD mirroring `MoyenPaiement`'s own shape. A new `GET /api/v1/facturations/select` endpoint (no query params — always auto-resolves the caller's entreprise country via the existing `IEntrepriseService.findCurrentUserCountryId()`) returns the valid billing options for that country + any global ones. `PreuvePaiementRequest` moves from `moyenPaiementId` to `facturationId`; `PreuvePaiementServiceImpl` resolves the `Facturation`, validates it's actually available for the caller's country (server-side — unlike Task 1, this is the real money-collection destination), and sets `PreuvePaiement.moyen` from `facturation.getMoyenPaiement()`.

**Tech Stack:** Spring Boot 4.0.6 / JPA / Flyway (backend), Next.js 16 / React 19 / TanStack Query / react-hook-form + zod (frontend).

**Spec:** `.claude/2026-08-30-moyen-paiement-pays-facturation-design.md`, section 2 (§2.1–§2.6) + decision 9. Section 1 (Task 1) is already implemented and merged — this plan only touches new files plus the 2 integration points named below (`PreuvePaiementRequest`/`PreuvePaiementServiceImpl` on the backend, `SubmitPaiementForm`/`preuve-paiement-request.ts` on the frontend).

## Global Constraints

- `Facturation.pays` is **optional** (nullable) — a global billing number (e.g. a universal card gateway) is valid for every country.
- Uniqueness is one billing number per `(moyenPaiement, pays)` pair, **including** the global case (`pays IS NULL`) — enforced both at the DB level (two partial unique indexes, mirroring the `person.email/telephone` `V4` migration pattern) and at the service level with a friendly i18n error (not a raw constraint-violation 500).
- **Unlike Task 1**, subscription-payment `Facturation` selection **does** get server-side validation: `PreuvePaiementServiceImpl` must reject (400) if the resolved `Facturation.pays` is neither `null` nor equal to the current entreprise's country.
- No hard requirement to store which `Facturation` line was shown/picked on `PreuvePaiement` — only its `moyenPaiement` is persisted (matches today's behavior).
- `FACTURATION_*` permissions are plain strings in `roles-permissions.yml` (ADMIN-only) — they do **not** need an entry in `PermissionCode.java` (that enum is a separate, partial list for programmatic checks; `@PreAuthorize` uses raw `hasAuthority('STRING')`, matching `MOYEN_PAIEMENT_*`'s own pattern exactly).
- New i18n keys for not-found/already-exists errors must **never** interpolate a raw id (rule 43) — e.g. `facturation.notFound` takes no `{0}` argument, unlike the pre-existing (non-compliant, not to be copied) `moyenPaiement.notFound`.
- All new form/dialog selectors use `<Combobox>`, never `<Select>` (rule 54) — built correctly from the start in this plan, no later conversion task needed.

---

## File Structure

| File | Change |
|---|---|
| `src/main/resources/db/migration/V92__create_facturation.sql` | Create |
| `src/main/java/org/store/paiement/domain/model/Facturation.java` | Create |
| `src/main/java/org/store/paiement/domain/repository/FacturationRepository.java` | Create |
| `src/main/java/org/store/paiement/infrastructure/repository/FacturationJpaRepository.java` | Create |
| `src/main/java/org/store/paiement/domain/service/FacturationDomainService.java` | Create |
| `src/main/java/org/store/paiement/application/dto/FacturationRequest.java` | Create |
| `src/main/java/org/store/paiement/application/dto/FacturationResponse.java` | Create |
| `src/main/java/org/store/paiement/application/dto/FacturationFilter.java` | Create |
| `src/main/java/org/store/paiement/application/dto/FacturationOptionResponse.java` | Create |
| `src/main/java/org/store/paiement/application/service/IFacturationService.java` | Create |
| `src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java` | Create |
| `src/main/java/org/store/paiement/presentation/FacturationController.java` | Create |
| `src/main/resources/security/roles-permissions.yml` | Modify — add `FACTURATION_*` |
| `src/main/resources/messages.properties`, `messages_en.properties` | Modify — add `facturation.*` keys |
| `src/main/java/org/store/abonnement/application/dto/PreuvePaiementRequest.java` | Modify |
| `src/main/java/org/store/abonnement/application/service/impl/PreuvePaiementServiceImpl.java` | Modify |
| `src/test/java/org/store/paiement/domain/service/FacturationDomainServiceTest.java` | Create |
| `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java` | Create |
| `src/test/java/org/store/paiement/presentation/FacturationControllerTest.java` | Create |
| `src/test/java/org/store/abonnement/application/service/PreuvePaiementServiceImplTest.java` | Modify |
| `store-frontend/src/features/facturation/domain/dtos/{facturation-response,facturation-option}.ts` | Create |
| `store-frontend/src/features/facturation/infrastructure/facturation-api.ts` | Create |
| `store-frontend/src/features/facturation/application/{moyen-paiement,use*}.ts` (7 hooks) | Create |
| `store-frontend/src/features/facturation/presentation/{FacturationTable,FacturationFormDialog}.tsx` | Create |
| `store-frontend/src/app/(dashboard)/dashboard/administration/facturation/{page,FacturationPage}.tsx` | Create |
| `store-frontend/src/app/(dashboard)/dashboard/administration/_tabs.ts` | Modify — add `facturation` tab |
| `store-frontend/src/app/(dashboard)/dashboard/administration/layout.tsx` | Modify — add tab icon |
| `store-frontend/src/features/abonnement/domain/dtos/preuve-paiement-request.ts` | Modify |
| `store-frontend/src/features/abonnement/presentation/SubmitPaiementForm.tsx` | Modify |
| `store-frontend/src/messages/fr.json`, `en.json` | Modify — `facturation` + `submitDialog` keys |

---

### Task 1: Migration + `Facturation` entity + domain layer

**Files:**
- Create: `src/main/resources/db/migration/V92__create_facturation.sql`
- Create: `src/main/java/org/store/paiement/domain/model/Facturation.java`
- Create: `src/main/java/org/store/paiement/domain/repository/FacturationRepository.java`
- Create: `src/main/java/org/store/paiement/infrastructure/repository/FacturationJpaRepository.java`
- Create: `src/main/java/org/store/paiement/domain/service/FacturationDomainService.java`
- Create: `src/main/java/org/store/paiement/application/dto/FacturationRequest.java` (needed now — the domain service's `create` signature takes it)
- Test: `src/test/java/org/store/paiement/domain/service/FacturationDomainServiceTest.java`

**Interfaces:**
- Consumes: `MoyenPaiement` (existing, `org.store.paiement.domain.model`), `Country` (existing, `org.store.country.domain.model`), `AuditableEntity` (existing, `org.store.common.base`).
- Produces: `Facturation` entity, `FacturationRepository.existsByMoyenAndPays(UUID moyenPaiementId, UUID paysId, UUID excludeId): boolean`, `FacturationDomainService.create(FacturationRequest, MoyenPaiement, Country): Facturation`, `FacturationDomainService.ensureUniqueMoyenPaysPair(UUID moyenPaiementId, UUID paysId, UUID excludeId): void` (throws `BadArgumentException("facturation.alreadyExists")`), `findById`/`findAll`/`save`/`delete` inherited from `GlobalService`.

- [ ] **Step 1: Create the migration**

```sql
CREATE TABLE facturation (
    id                  UUID NOT NULL,
    moyen_paiement_id   UUID NOT NULL,
    pays_id             UUID NULL,
    numero_facturation  VARCHAR(100) NOT NULL,
    actif               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT pk_facturation PRIMARY KEY (id),
    CONSTRAINT fk_facturation_moyen_paiement FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement (id),
    CONSTRAINT fk_facturation_pays FOREIGN KEY (pays_id) REFERENCES country (id)
);

CREATE UNIQUE INDEX facturation_moyen_pays_key
    ON facturation (moyen_paiement_id, pays_id)
    WHERE pays_id IS NOT NULL;

CREATE UNIQUE INDEX facturation_moyen_global_key
    ON facturation (moyen_paiement_id)
    WHERE pays_id IS NULL;
```

- [ ] **Step 2: Create the `Facturation` entity**

```java
package org.store.paiement.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;

@Getter
@Setter
@Entity
@Table(name = Facturation.TABLE_NAME)
public class Facturation extends AuditableEntity {
    public static final String TABLE_NAME = "facturation";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moyen_paiement_id", nullable = false)
    private MoyenPaiement moyenPaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pays_id")
    private Country pays;

    @Column(name = "numero_facturation", nullable = false, length = 100)
    private String numeroFacturation;

    @Column(nullable = false)
    private boolean actif = true;
}
```

- [ ] **Step 3: Create `FacturationRequest`** (needed by the domain service's `create` method)

```java
package org.store.paiement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FacturationRequest(
        @NotNull UUID moyenPaiementId,
        UUID paysId,
        @NotBlank @Size(max = 100) String numeroFacturation
) {
}
```

- [ ] **Step 4: Create the domain port `FacturationRepository`**

```java
package org.store.paiement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.paiement.domain.model.Facturation;

import java.util.UUID;

public interface FacturationRepository extends BaseRepository<Facturation> {

    @Query("""
            SELECT COUNT(facturation) > 0
            FROM Facturation facturation
            WHERE facturation.moyenPaiement.id = :moyenPaiementId
              AND ((:paysId IS NULL AND facturation.pays IS NULL) OR facturation.pays.id = :paysId)
              AND (:excludeId IS NULL OR facturation.id <> :excludeId)
            """)
    boolean existsByMoyenAndPays(@Param("moyenPaiementId") UUID moyenPaiementId,
                                  @Param("paysId") UUID paysId,
                                  @Param("excludeId") UUID excludeId);
}
```

- [ ] **Step 5: Create the Spring Data adapter `FacturationJpaRepository`**

```java
package org.store.paiement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

@Repository
public interface FacturationJpaRepository extends JpaRepository<Facturation, UUID>, FacturationRepository {
}
```

- [ ] **Step 6: Write the failing domain-service tests**

Create `src/test/java/org/store/paiement/domain/service/FacturationDomainServiceTest.java`:

```java
package org.store.paiement.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacturationDomainServiceTest {

    private FacturationRepository repository;
    private FacturationDomainService service;

    @BeforeEach
    void setUp() {
        repository = mock(FacturationRepository.class);
        service = new FacturationDomainService(repository);
        when(repository.save(any(Facturation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_should_build_and_save_facturation_with_pays() {
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        Country pays = new Country();
        pays.setId(UUID.randomUUID());
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), pays.getId(), "77 000 00 00");

        Facturation result = service.create(request, moyenPaiement, pays);

        assertThat(result.getMoyenPaiement()).isEqualTo(moyenPaiement);
        assertThat(result.getPays()).isEqualTo(pays);
        assertThat(result.getNumeroFacturation()).isEqualTo("77 000 00 00");
        assertThat(result.isActif()).isTrue();
    }

    @Test
    void create_should_build_and_save_global_facturation_when_pays_is_null() {
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(UUID.randomUUID());
        FacturationRequest request = new FacturationRequest(moyenPaiement.getId(), null, "CARD-GLOBAL-001");

        Facturation result = service.create(request, moyenPaiement, null);

        assertThat(result.getPays()).isNull();
    }

    @Test
    void ensureUniqueMoyenPaysPair_should_pass_when_no_conflict() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        when(repository.existsByMoyenAndPays(moyenPaiementId, paysId, null)).thenReturn(false);

        service.ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);
    }

    @Test
    void ensureUniqueMoyenPaysPair_should_throw_when_conflict_exists() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        when(repository.existsByMoyenAndPays(moyenPaiementId, paysId, null)).thenReturn(true);

        assertThatThrownBy(() -> service.ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null))
                .isInstanceOf(BadArgumentException.class);
    }
}
```

- [ ] **Step 7: Run tests to verify they fail**

Run: `./mvnw test -Dtest=FacturationDomainServiceTest`
Expected: compile error — `FacturationDomainService` doesn't exist yet.

- [ ] **Step 8: Create `FacturationDomainService`**

```java
package org.store.paiement.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.GlobalService;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

@Service
public class FacturationDomainService extends GlobalService<Facturation, FacturationRepository> {

    public FacturationDomainService(FacturationRepository repository) {
        super(repository);
    }

    public Facturation create(FacturationRequest request, MoyenPaiement moyenPaiement, Country pays) {
        Facturation facturation = new Facturation();
        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());
        facturation.setActif(true);
        return save(facturation);
    }

    public void ensureUniqueMoyenPaysPair(UUID moyenPaiementId, UUID paysId, UUID excludeId) {
        if (repository.existsByMoyenAndPays(moyenPaiementId, paysId, excludeId)) {
            throw new BadArgumentException("facturation.alreadyExists");
        }
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationDomainServiceTest`
Expected: PASS (4/4).

- [ ] **Step 10: Verify the mapping boots cleanly**

Run: `./mvnw test -Dtest=StoreApplicationTests`
Expected: PASS — confirms the migration's table/column names match the `@JoinColumn`s exactly (no `@DataJpaTest` exists in this codebase; this context-boot test is how a mapping mismatch would be caught).

- [ ] **Step 11: Commit**

```bash
git add src/main/resources/db/migration/V92__create_facturation.sql \
        src/main/java/org/store/paiement/domain/model/Facturation.java \
        src/main/java/org/store/paiement/domain/repository/FacturationRepository.java \
        src/main/java/org/store/paiement/infrastructure/repository/FacturationJpaRepository.java \
        src/main/java/org/store/paiement/domain/service/FacturationDomainService.java \
        src/main/java/org/store/paiement/application/dto/FacturationRequest.java \
        src/test/java/org/store/paiement/domain/service/FacturationDomainServiceTest.java
git commit -m "feat(paiement): add Facturation entity and domain layer"
```

---

### Task 2: `FacturationResponse` + `IFacturationService`/`FacturationServiceImpl` (create/update/activate/deactivate/delete) + `FacturationController` + permissions

**Files:**
- Create: `src/main/java/org/store/paiement/application/dto/FacturationResponse.java`
- Create: `src/main/java/org/store/paiement/application/service/IFacturationService.java`
- Create: `src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java`
- Create: `src/main/java/org/store/paiement/presentation/FacturationController.java`
- Modify: `src/main/resources/security/roles-permissions.yml`
- Modify: `src/main/resources/messages.properties`, `src/main/resources/messages_en.properties`
- Test: `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java`
- Test: `src/test/java/org/store/paiement/presentation/FacturationControllerTest.java`

**Interfaces:**
- Consumes: `IMoyenPaiementService.findById(UUID): MoyenPaiement` (existing), `CountryDomainService.findById(UUID): Country` (existing, inherited from `GlobalService`), `FacturationDomainService` (Task 1), `ValidatorService.validate(Object)` (existing).
- Produces: `FacturationResponse(UUID id, MoyenPaiementResponse moyenPaiement, CountryResponse pays, String numeroFacturation, boolean actif)`, `IFacturationService.{create,update,activate,deactivate,delete,findResponseById}`, `POST/PUT/PATCH/DELETE /api/v1/facturations`, permissions `FACTURATION_{CREATE,UPDATE,DELETE}` (ADMIN-only).

- [ ] **Step 1: Add permissions to `roles-permissions.yml`**

In the global permissions list, right after the existing `# MOYEN_PAIEMENT` block:

```yaml
  # MOYEN_PAIEMENT
  - MOYEN_PAIEMENT_CREATE
  - MOYEN_PAIEMENT_UPDATE
  - MOYEN_PAIEMENT_DELETE

  # FACTURATION
  - FACTURATION_CREATE
  - FACTURATION_UPDATE
  - FACTURATION_DELETE
  - FACTURATION_READ
```

(unlike `MOYEN_PAIEMENT`, `FACTURATION_READ` is declared because the frontend admin tab needs a real, granted permission to gate its visibility — Task 1 left `MOYEN_PAIEMENT`'s own tab broken this exact way, since fixed separately; this plan does it correctly from the start. The subscription-payment `/select` endpoint reuses the existing `SUBSCRIPTION_PAY` permission, not `FACTURATION_READ` — see Task 4.)

In the ADMIN role's permission block, right after the existing `MOYEN_PAIEMENT_*` lines:

```yaml
      - MOYEN_PAIEMENT_CREATE
      - MOYEN_PAIEMENT_UPDATE
      - MOYEN_PAIEMENT_DELETE
      - FACTURATION_CREATE
      - FACTURATION_UPDATE
      - FACTURATION_DELETE
      - FACTURATION_READ
```

- [ ] **Step 2: Add i18n keys**

In `src/main/resources/messages.properties`, add (near the other `moyenPaiement.*` keys):

```properties
facturation.notFound=Facturation introuvable
facturation.alreadyExists=Une facturation existe déjà pour ce moyen de paiement et ce pays
facturation.notAvailableForCountry=Ce moyen de paiement n'est pas disponible pour votre pays
```

In `src/main/resources/messages_en.properties`:

```properties
facturation.notFound=Facturation not found
facturation.alreadyExists=A facturation already exists for this payment method and country
facturation.notAvailableForCountry=This payment method is not available for your country
```

(No `{0}` placeholder on any of these — rule 43, never interpolate a raw id.)

- [ ] **Step 3: Write the failing service tests**

Create `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java`:

```java
package org.store.paiement.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.FacturationDomainService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FacturationServiceImplTest {

    private FacturationDomainService domainService;
    private IMoyenPaiementService moyenPaiementService;
    private CountryDomainService countryDomainService;
    private ValidatorService validatorService;
    private FacturationServiceImpl service;

    @BeforeEach
    void setUp() {
        domainService = mock(FacturationDomainService.class);
        moyenPaiementService = mock(IMoyenPaiementService.class);
        countryDomainService = mock(CountryDomainService.class);
        validatorService = mock(ValidatorService.class);
        service = new FacturationServiceImpl(domainService, moyenPaiementService, countryDomainService, validatorService);
    }

    @Test
    void create_should_resolve_moyen_and_pays_then_delegate_to_domain_service() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, paysId, "77 000 00 00");
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        Country pays = new Country();
        pays.setId(paysId);
        Facturation created = new Facturation();
        created.setId(UUID.randomUUID());
        created.setMoyenPaiement(moyenPaiement);
        created.setPays(pays);
        created.setNumeroFacturation("77 000 00 00");
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(countryDomainService.findById(paysId)).thenReturn(pays);
        when(domainService.create(request, moyenPaiement, pays)).thenReturn(created);

        FacturationResponse response = service.create(request);

        assertThat(response.numeroFacturation()).isEqualTo("77 000 00 00");
        verify(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);
    }

    @Test
    void create_should_not_resolve_pays_when_paysId_is_null() {
        UUID moyenPaiementId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, null, "CARD-GLOBAL");
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        Facturation created = new Facturation();
        created.setId(UUID.randomUUID());
        created.setMoyenPaiement(moyenPaiement);
        created.setNumeroFacturation("CARD-GLOBAL");
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(domainService.create(eq(request), eq(moyenPaiement), isNull())).thenReturn(created);

        FacturationResponse response = service.create(request);

        assertThat(response.pays()).isNull();
        verify(countryDomainService, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void create_should_throw_when_moyen_pays_pair_already_exists() {
        UUID moyenPaiementId = UUID.randomUUID();
        UUID paysId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, paysId, "77 000 00 00");
        org.mockito.Mockito.doThrow(new BadArgumentException("facturation.alreadyExists"))
                .when(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, paysId, null);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BadArgumentException.class);
    }

    @Test
    void update_should_exclude_current_id_from_uniqueness_check() {
        UUID id = UUID.randomUUID();
        UUID moyenPaiementId = UUID.randomUUID();
        FacturationRequest request = new FacturationRequest(moyenPaiementId, null, "NEW-NUMBER");
        Facturation existing = new Facturation();
        existing.setId(id);
        MoyenPaiement moyenPaiement = new MoyenPaiement();
        moyenPaiement.setId(moyenPaiementId);
        when(domainService.findById(id)).thenReturn(existing);
        when(moyenPaiementService.findById(moyenPaiementId)).thenReturn(moyenPaiement);
        when(domainService.save(existing)).thenReturn(existing);

        service.update(id, request);

        verify(domainService).ensureUniqueMoyenPaysPair(moyenPaiementId, null, id);
        assertThat(existing.getNumeroFacturation()).isEqualTo("NEW-NUMBER");
    }

    @Test
    void activate_should_set_actif_true() {
        UUID id = UUID.randomUUID();
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setActif(false);
        when(domainService.findById(id)).thenReturn(facturation);
        when(domainService.save(facturation)).thenReturn(facturation);

        service.activate(id);

        assertThat(facturation.isActif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        UUID id = UUID.randomUUID();
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setActif(true);
        when(domainService.findById(id)).thenReturn(facturation);
        when(domainService.save(facturation)).thenReturn(facturation);

        service.deactivate(id);

        assertThat(facturation.isActif()).isFalse();
    }

    @Test
    void delete_should_delegate_to_domain_service() {
        UUID id = UUID.randomUUID();
        Facturation facturation = new Facturation();
        facturation.setId(id);
        when(domainService.findById(id)).thenReturn(facturation);

        service.delete(id);

        verify(domainService).delete(facturation);
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: compile error — `FacturationResponse`/`IFacturationService`/`FacturationServiceImpl` don't exist yet.

- [ ] **Step 5: Create `FacturationResponse`**

```java
package org.store.paiement.application.dto;

import org.store.country.application.dto.CountryResponse;
import org.store.paiement.domain.model.Facturation;

import java.util.UUID;

public record FacturationResponse(
        UUID id,
        MoyenPaiementResponse moyenPaiement,
        CountryResponse pays,
        String numeroFacturation,
        boolean actif
) {
    public FacturationResponse(Facturation facturation) {
        this(
                facturation.getId(),
                new MoyenPaiementResponse(facturation.getMoyenPaiement()),
                facturation.getPays() != null ? new CountryResponse(facturation.getPays()) : null,
                facturation.getNumeroFacturation(),
                facturation.isActif()
        );
    }
}
```

- [ ] **Step 6: Create `IFacturationService`**

```java
package org.store.paiement.application.service;

import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;

import java.util.UUID;

public interface IFacturationService {

    FacturationResponse create(FacturationRequest request);

    FacturationResponse update(UUID id, FacturationRequest request);

    FacturationResponse activate(UUID id);

    FacturationResponse deactivate(UUID id);

    void delete(UUID id);

    FacturationResponse findResponseById(UUID id);
}
```

(`findByIdAvailableForCurrentCountry` is intentionally NOT declared here — it is added in Task 5, once `IEntrepriseService` is already wired into this service by Task 4 for `findSelectOptions`. Adding it here too would require a 5-arg constructor before Task 4 introduces it, breaking the 4-arg constructor this task's own test uses.)

- [ ] **Step 7: Create `FacturationServiceImpl`**

```java
package org.store.paiement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.FacturationDomainService;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FacturationServiceImpl implements IFacturationService {

    private final FacturationDomainService domainService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;

    public FacturationServiceImpl(FacturationDomainService domainService,
                                  IMoyenPaiementService moyenPaiementService,
                                  CountryDomainService countryDomainService,
                                  ValidatorService validatorService) {
        this.domainService = domainService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
    }

    @Override
    @Transactional
    public FacturationResponse create(FacturationRequest request) {
        validatorService.validate(request);
        domainService.ensureUniqueMoyenPaysPair(request.moyenPaiementId(), request.paysId(), null);

        MoyenPaiement moyenPaiement = moyenPaiementService.findById(request.moyenPaiementId());
        Country pays = request.paysId() != null ? countryDomainService.findById(request.paysId()) : null;
        Facturation created = domainService.create(request, moyenPaiement, pays);

        return new FacturationResponse(created);
    }

    @Override
    @Transactional
    public FacturationResponse update(UUID id, FacturationRequest request) {
        validatorService.validate(request);
        Facturation facturation = domainService.findById(id);
        domainService.ensureUniqueMoyenPaysPair(request.moyenPaiementId(), request.paysId(), id);

        MoyenPaiement moyenPaiement = moyenPaiementService.findById(request.moyenPaiementId());
        Country pays = request.paysId() != null ? countryDomainService.findById(request.paysId()) : null;

        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());

        return new FacturationResponse(domainService.save(facturation));
    }

    @Override
    @Transactional
    public FacturationResponse activate(UUID id) {
        Facturation facturation = domainService.findById(id);
        facturation.setActif(true);
        return new FacturationResponse(domainService.save(facturation));
    }

    @Override
    @Transactional
    public FacturationResponse deactivate(UUID id) {
        Facturation facturation = domainService.findById(id);
        facturation.setActif(false);
        return new FacturationResponse(domainService.save(facturation));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    @Override
    public FacturationResponse findResponseById(UUID id) {
        return new FacturationResponse(domainService.findById(id));
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: PASS (7/7).

- [ ] **Step 9: Write the controller test**

Create `src/test/java/org/store/paiement/presentation/FacturationControllerTest.java`:

```java
package org.store.paiement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.service.IFacturationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacturationControllerTest {

    private MockMvc mockMvc;
    private IFacturationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(IFacturationService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FacturationController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void create_should_return_201() throws Exception {
        FacturationRequest body = new FacturationRequest(UUID.randomUUID(), null, "77 000 00 00");
        MoyenPaiementResponse moyenPaiement = new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true, List.of());
        FacturationResponse response = new FacturationResponse(UUID.randomUUID(), moyenPaiement, (CountryResponse) null, "77 000 00 00", true);
        when(service.create(any(FacturationRequest.class))).thenReturn(response);

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroFacturation").value("77 000 00 00"));
    }

    @Test
    void create_should_return_400_when_numeroFacturation_blank() throws Exception {
        FacturationRequest body = new FacturationRequest(UUID.randomUUID(), null, "");

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_should_return_400_when_moyenPaiementId_missing() throws Exception {
        String bodyWithoutMoyen = """
                {"paysId": null, "numeroFacturation": "77 000 00 00"}
                """;

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutMoyen))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 10: Create `FacturationController`**

```java
package org.store.paiement.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;

import java.util.UUID;

@RestController
@RequestMapping(FacturationController.BASE_PATH)
public class FacturationController {

    public static final String BASE_PATH = "/api/v1/facturations";

    private final IFacturationService service;

    public FacturationController(IFacturationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_READ')")
    public ResponseEntity<FacturationResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FACTURATION_CREATE')")
    public ResponseEntity<FacturationResponse> create(@Valid @RequestBody FacturationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody FacturationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 11: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationControllerTest`
Expected: PASS (3/3).

- [ ] **Step 12: Commit**

```bash
git add src/main/java/org/store/paiement/application/dto/FacturationResponse.java \
        src/main/java/org/store/paiement/application/service/IFacturationService.java \
        src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java \
        src/main/java/org/store/paiement/presentation/FacturationController.java \
        src/main/resources/security/roles-permissions.yml \
        src/main/resources/messages.properties src/main/resources/messages_en.properties \
        src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java \
        src/test/java/org/store/paiement/presentation/FacturationControllerTest.java
git commit -m "feat(paiement): add Facturation CRUD service, controller, and permissions"
```

---

### Task 3: Paginated filtered list — `FacturationFilter` + `GET /api/v1/facturations`

**Files:**
- Create: `src/main/java/org/store/paiement/application/dto/FacturationFilter.java`
- Modify: `src/main/java/org/store/paiement/domain/repository/FacturationRepository.java`
- Modify: `src/main/java/org/store/paiement/domain/service/FacturationDomainService.java`
- Modify: `src/main/java/org/store/paiement/application/service/IFacturationService.java`
- Modify: `src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java`
- Modify: `src/main/java/org/store/paiement/presentation/FacturationController.java`
- Modify: `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java` (append)
- Modify: `src/test/java/org/store/paiement/presentation/FacturationControllerTest.java` (append)

**Interfaces:**
- Produces: `FacturationFilter(UUID moyenPaiementId, UUID paysId, Boolean actif, LocalDate createdStartDate, LocalDate createdEndDate, int page, int size)` with `toPageable()`/`createdStartDateTime()`/`createdEndDateTime()`, `IFacturationService.findAll(FacturationFilter): Page<FacturationResponse>`, `GET /api/v1/facturations?moyenPaiementId=&paysId=&actif=&createdStartDate=&createdEndDate=&page=&size=`.

- [ ] **Step 1: Create `FacturationFilter`**

```java
package org.store.paiement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FacturationFilter(
        UUID moyenPaiementId,
        UUID paysId,
        Boolean actif,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public LocalDateTime createdStartDateTime() {
        return createdStartDate != null ? createdStartDate.atStartOfDay() : null;
    }

    public LocalDateTime createdEndDateTime() {
        return createdEndDate != null ? createdEndDate.plusDays(1).atStartOfDay() : null;
    }
}
```

- [ ] **Step 2: Write the failing service test (append to `FacturationServiceImplTest`)**

Add imports: `org.springframework.data.domain.Page`, `org.springframework.data.domain.Pageable`, `org.mockito.ArgumentCaptor`, `org.store.paiement.application.dto.FacturationFilter` — plus static import `org.mockito.Mockito.verify` (already present if added in Task 2's file — check before duplicating the import).

Append:

```java
    @Test
    void findAll_should_validate_and_delegate_to_domain_service() {
        FacturationFilter filter = new FacturationFilter(null, null, null, null, null, 0, 10);
        when(domainService.findResponsesByFilter(filter)).thenReturn(Page.empty());

        service.findAll(filter);

        org.mockito.Mockito.verify(validatorService).validate(filter);
        org.mockito.Mockito.verify(domainService).findResponsesByFilter(filter);
    }
```

- [ ] **Step 3: Run to verify it fails**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: compile error — `findAll` doesn't exist yet.

- [ ] **Step 4: Add the repository query with explicit aliases**

Add to `FacturationRepository.java`: imports `org.springframework.data.domain.Page`, `org.springframework.data.domain.Pageable`, `java.time.LocalDateTime`, then:

```java
    @Query(value = """
            SELECT new org.store.paiement.application.dto.FacturationResponse(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR facturation.pays.id = :paysId)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND (:createdStart IS NULL OR facturation.createdAt >= :createdStart)
              AND (:createdEnd IS NULL OR facturation.createdAt < :createdEnd)
            ORDER BY facturation.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR facturation.pays.id = :paysId)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND (:createdStart IS NULL OR facturation.createdAt >= :createdStart)
              AND (:createdEnd IS NULL OR facturation.createdAt < :createdEnd)
            """)
    Page<FacturationResponse> findResponsesByFilter(@Param("moyenPaiementId") UUID moyenPaiementId,
                                                      @Param("paysId") UUID paysId,
                                                      @Param("actif") Boolean actif,
                                                      @Param("createdStart") LocalDateTime createdStart,
                                                      @Param("createdEnd") LocalDateTime createdEnd,
                                                      Pageable pageable);
```

Add import `org.store.paiement.application.dto.FacturationResponse` to this file too.

- [ ] **Step 5: Add the domain service delegation — destructures the filter**

Add to `FacturationDomainService.java`: imports `org.springframework.data.domain.Page`, `org.store.paiement.application.dto.FacturationFilter`, `org.store.paiement.application.dto.FacturationResponse`, then:

```java
    public Page<FacturationResponse> findResponsesByFilter(FacturationFilter filter) {
        return repository.findResponsesByFilter(
                filter.moyenPaiementId(), filter.paysId(), filter.actif(),
                filter.createdStartDateTime(), filter.createdEndDateTime(),
                filter.toPageable());
    }
```

- [ ] **Step 6: Add the application service method**

Add to `IFacturationService.java` (imports `org.springframework.data.domain.Page`, `org.store.paiement.application.dto.FacturationFilter`):

```java
    Page<FacturationResponse> findAll(FacturationFilter filter);
```

Add to `FacturationServiceImpl.java` (import `org.springframework.data.domain.Page`, `org.store.paiement.application.dto.FacturationFilter`):

```java
    @Override
    public Page<FacturationResponse> findAll(FacturationFilter filter) {
        validatorService.validate(filter);
        return domainService.findResponsesByFilter(filter);
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: PASS (8/8).

- [ ] **Step 8: Add the controller endpoint + its test**

Append to `FacturationControllerTest.java` (imports `org.springframework.data.domain.Page`, `org.springframework.data.domain.PageImpl`, `org.springframework.data.domain.PageRequest`, `java.util.List`, `org.store.paiement.application.dto.FacturationFilter`, static import `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get`):

```java
    @Test
    void list_should_return_200_with_page() throws Exception {
        MoyenPaiementResponse moyenPaiement = new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true, List.of());
        FacturationResponse response = new FacturationResponse(UUID.randomUUID(), moyenPaiement, (CountryResponse) null, "77 000 00 00", true);
        when(service.findAll(any(FacturationFilter.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(response), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(FacturationController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numeroFacturation").value("77 000 00 00"));
    }
```

Add to `FacturationController.java` (imports `org.springframework.data.domain.Page`, `org.store.paiement.application.dto.FacturationFilter`, `java.time.LocalDate`, `org.springframework.format.annotation.DateTimeFormat`):

```java
    @GetMapping
    @PreAuthorize("hasAuthority('FACTURATION_READ')")
    public ResponseEntity<Page<FacturationResponse>> list(@RequestParam(required = false) UUID moyenPaiementId,
                                                            @RequestParam(required = false) UUID paysId,
                                                            @RequestParam(required = false) Boolean actif,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        FacturationFilter filter = new FacturationFilter(moyenPaiementId, paysId, actif, createdStartDate, createdEndDate, page, size);
        return ResponseEntity.ok(service.findAll(filter));
    }
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationControllerTest`
Expected: PASS (4/4).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/org/store/paiement/application/dto/FacturationFilter.java \
        src/main/java/org/store/paiement/domain/repository/FacturationRepository.java \
        src/main/java/org/store/paiement/domain/service/FacturationDomainService.java \
        src/main/java/org/store/paiement/application/service/IFacturationService.java \
        src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java \
        src/main/java/org/store/paiement/presentation/FacturationController.java \
        src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java \
        src/test/java/org/store/paiement/presentation/FacturationControllerTest.java
git commit -m "feat(paiement): add paginated filtered Facturation listing"
```

---

### Task 4: Subscription-payment select endpoint — `GET /api/v1/facturations/select`

**Files:**
- Create: `src/main/java/org/store/paiement/application/dto/FacturationOptionResponse.java`
- Modify: `src/main/java/org/store/paiement/domain/repository/FacturationRepository.java`
- Modify: `src/main/java/org/store/paiement/domain/service/FacturationDomainService.java`
- Modify: `src/main/java/org/store/paiement/application/service/IFacturationService.java`
- Modify: `src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java`
- Modify: `src/main/java/org/store/paiement/presentation/FacturationController.java`
- Modify: `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java` (append)
- Modify: `src/test/java/org/store/paiement/presentation/FacturationControllerTest.java` (append)

**Interfaces:**
- Consumes: `IEntrepriseService.findCurrentUserCountryId(): UUID` (existing, from Task 1).
- Produces: `FacturationOptionResponse(UUID facturationId, String moyenLibelle, String numeroFacturation)`, `IFacturationService.findSelectOptions(): List<FacturationOptionResponse>`, `GET /api/v1/facturations/select` (permission `SUBSCRIPTION_PAY`, not `FACTURATION_READ`).

- [ ] **Step 1: Create `FacturationOptionResponse`**

```java
package org.store.paiement.application.dto;

import java.util.UUID;

public record FacturationOptionResponse(
        UUID facturationId,
        String moyenLibelle,
        String numeroFacturation
) {
}
```

- [ ] **Step 2: Write the failing service tests (append to `FacturationServiceImplTest`)**

Add imports: `java.util.List`, `org.store.entreprise.application.service.IEntrepriseService`, `org.store.paiement.application.dto.FacturationOptionResponse`.

This task adds a 5th constructor dependency (`IEntrepriseService`) to `FacturationServiceImpl`. Update the test class's field declarations and `@BeforeEach` (added in Task 2) first:

```java
    private IEntrepriseService entrepriseService;
```

(add this field alongside the existing `domainService`/`moyenPaiementService`/`countryDomainService`/`validatorService` fields)

In `setUp()`, add `entrepriseService = mock(IEntrepriseService.class);` and change the constructor call from:
```java
        service = new FacturationServiceImpl(domainService, moyenPaiementService, countryDomainService, validatorService);
```
to:
```java
        service = new FacturationServiceImpl(domainService, moyenPaiementService, countryDomainService, validatorService, entrepriseService);
```

Then append the new tests:

```java
    @Test
    void findSelectOptions_should_resolve_current_country_and_return_matching_options() {
        UUID countryId = UUID.randomUUID();
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(countryId);
        FacturationOptionResponse option = new FacturationOptionResponse(UUID.randomUUID(), "Wave", "77 000 00 00");
        when(domainService.findSelectOptions(countryId)).thenReturn(List.of(option));

        List<FacturationOptionResponse> result = service.findSelectOptions();

        assertThat(result).containsExactly(option);
    }

    @Test
    void findSelectOptions_should_pass_null_when_current_user_has_no_entreprise() {
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(null);
        when(domainService.findSelectOptions(null)).thenReturn(List.of());

        List<FacturationOptionResponse> result = service.findSelectOptions();

        assertThat(result).isEmpty();
    }
```

- [ ] **Step 3: Run to verify it fails**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: compile error — `findSelectOptions` doesn't exist yet.

- [ ] **Step 4: Add the repository query with explicit aliases**

Add to `FacturationRepository.java` (import `java.util.List`, `org.store.paiement.application.dto.FacturationOptionResponse`):

```java
    @Query("""
            SELECT new org.store.paiement.application.dto.FacturationOptionResponse(
                facturation.id, facturation.moyenPaiement.libelle, facturation.numeroFacturation)
            FROM Facturation facturation
            WHERE facturation.actif = true
              AND (facturation.pays IS NULL OR facturation.pays.id = :countryId)
            ORDER BY facturation.moyenPaiement.libelle ASC
            """)
    List<FacturationOptionResponse> findSelectOptions(@Param("countryId") UUID countryId);
```

- [ ] **Step 5: Add the domain service delegation**

Add to `FacturationDomainService.java` (import `java.util.List`, `org.store.paiement.application.dto.FacturationOptionResponse`):

```java
    public List<FacturationOptionResponse> findSelectOptions(UUID countryId) {
        return repository.findSelectOptions(countryId);
    }
```

- [ ] **Step 6: Add the application service method**

Add to `IFacturationService.java` (import `java.util.List`, `org.store.paiement.application.dto.FacturationOptionResponse`):

```java
    List<FacturationOptionResponse> findSelectOptions();
```

Add to `FacturationServiceImpl.java` (imports `java.util.List`, `org.store.entreprise.application.service.IEntrepriseService`, `org.store.paiement.application.dto.FacturationOptionResponse`):

Add the field and thread it through the constructor:

```java
    private final IEntrepriseService entrepriseService;

    public FacturationServiceImpl(FacturationDomainService domainService,
                                  IMoyenPaiementService moyenPaiementService,
                                  CountryDomainService countryDomainService,
                                  ValidatorService validatorService,
                                  IEntrepriseService entrepriseService) {
        this.domainService = domainService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
        this.entrepriseService = entrepriseService;
    }
```

(replace the existing 4-arg constructor from Task 2 with this 5-arg one — same body plus the new assignment)

Then add the method:

```java
    @Override
    public List<FacturationOptionResponse> findSelectOptions() {
        UUID countryId = entrepriseService.findCurrentUserCountryId();
        return domainService.findSelectOptions(countryId);
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: PASS (10/10).

- [ ] **Step 8: Add the controller endpoint + its test**

Append to `FacturationControllerTest.java`:

```java
    @Test
    void select_should_return_200_with_options() throws Exception {
        FacturationOptionResponse option = new FacturationOptionResponse(UUID.randomUUID(), "Wave", "77 000 00 00");
        when(service.findSelectOptions()).thenReturn(List.of(option));

        mockMvc.perform(get(FacturationController.BASE_PATH + "/select"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moyenLibelle").value("Wave"))
                .andExpect(jsonPath("$[0].numeroFacturation").value("77 000 00 00"));
    }
```

(add import `org.store.paiement.application.dto.FacturationOptionResponse` to the test file.)

Add to `FacturationController.java` (**before** the `/{id}` GET mapping, so Spring's path matching doesn't try to parse `"select"` as a UUID path variable — imports `java.util.List`):

```java
    @GetMapping("/select")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PAY')")
    public ResponseEntity<List<FacturationOptionResponse>> select() {
        return ResponseEntity.ok(service.findSelectOptions());
    }
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw test -Dtest=FacturationControllerTest`
Expected: PASS (5/5).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/org/store/paiement/application/dto/FacturationOptionResponse.java \
        src/main/java/org/store/paiement/domain/repository/FacturationRepository.java \
        src/main/java/org/store/paiement/domain/service/FacturationDomainService.java \
        src/main/java/org/store/paiement/application/service/IFacturationService.java \
        src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java \
        src/main/java/org/store/paiement/presentation/FacturationController.java \
        src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java \
        src/test/java/org/store/paiement/presentation/FacturationControllerTest.java
git commit -m "feat(paiement): add GET /facturations/select for subscription payment"
```

---

### Task 5: `PreuvePaiement` integration — `facturationId` replaces `moyenPaiementId`

**Files:**
- Modify: `src/main/java/org/store/paiement/application/service/IFacturationService.java`
- Modify: `src/main/java/org/store/paiement/application/service/impl/FacturationServiceImpl.java`
- Modify: `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java` (append)
- Modify: `src/main/java/org/store/abonnement/application/dto/PreuvePaiementRequest.java`
- Modify: `src/main/java/org/store/abonnement/application/service/impl/PreuvePaiementServiceImpl.java`
- Modify: `src/test/java/org/store/abonnement/application/service/PreuvePaiementServiceImplTest.java`

**Interfaces:**
- Consumes: `IEntrepriseService.findCurrentUserCountryId(): UUID` (existing, already injected into `FacturationServiceImpl` by Task 4).
- Produces: `IFacturationService.findByIdAvailableForCurrentCountry(UUID): Facturation` (throws `BadArgumentException("facturation.notAvailableForCountry")` on country mismatch), `PreuvePaiementRequest(UUID facturationId, String referenceTransaction)` — `moyenPaiementId` is gone.

- [ ] **Step 0: Write the failing test for `findByIdAvailableForCurrentCountry`**

Append to `src/test/java/org/store/paiement/application/service/impl/FacturationServiceImplTest.java` (add import `org.store.common.exceptions.BadArgumentException` if not already present from an earlier task):

```java
    @Test
    void findByIdAvailableForCurrentCountry_should_return_facturation_when_global() {
        UUID id = UUID.randomUUID();
        Facturation facturation = new Facturation();
        facturation.setId(id);
        when(domainService.findById(id)).thenReturn(facturation);

        Facturation result = service.findByIdAvailableForCurrentCountry(id);

        assertThat(result).isEqualTo(facturation);
        verify(entrepriseService, org.mockito.Mockito.never()).findCurrentUserCountryId();
    }

    @Test
    void findByIdAvailableForCurrentCountry_should_return_facturation_when_pays_matches_current_country() {
        UUID id = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        Country pays = new Country();
        pays.setId(countryId);
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setPays(pays);
        when(domainService.findById(id)).thenReturn(facturation);
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(countryId);

        Facturation result = service.findByIdAvailableForCurrentCountry(id);

        assertThat(result).isEqualTo(facturation);
    }

    @Test
    void findByIdAvailableForCurrentCountry_should_throw_when_pays_does_not_match_current_country() {
        UUID id = UUID.randomUUID();
        Country pays = new Country();
        pays.setId(UUID.randomUUID());
        Facturation facturation = new Facturation();
        facturation.setId(id);
        facturation.setPays(pays);
        when(domainService.findById(id)).thenReturn(facturation);
        when(entrepriseService.findCurrentUserCountryId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.findByIdAvailableForCurrentCountry(id))
                .isInstanceOf(BadArgumentException.class);
    }
```

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: compile error — `findByIdAvailableForCurrentCountry` doesn't exist yet.

- [ ] **Step 0b: Implement `findByIdAvailableForCurrentCountry`**

Add to `IFacturationService.java` (import `org.store.paiement.domain.model.Facturation`):

```java
    /** Resolves a Facturation by id and enforces it is available for the current user's entreprise country. */
    Facturation findByIdAvailableForCurrentCountry(UUID id);
```

Add to `FacturationServiceImpl.java` (import `org.store.common.exceptions.BadArgumentException`):

```java
    @Override
    public Facturation findByIdAvailableForCurrentCountry(UUID id) {
        Facturation facturation = domainService.findById(id);
        Country pays = facturation.getPays();
        if (pays != null) {
            UUID currentCountryId = entrepriseService.findCurrentUserCountryId();
            if (!pays.getId().equals(currentCountryId)) {
                throw new BadArgumentException("facturation.notAvailableForCountry");
            }
        }
        return facturation;
    }
```

Run: `./mvnw test -Dtest=FacturationServiceImplTest`
Expected: PASS (all tests in the file, including the 3 new ones).

- [ ] **Step 1: Write the failing tests**

In `src/test/java/org/store/abonnement/application/service/PreuvePaiementServiceImplTest.java`:

Change the field declaration:
```java
    @Mock private IMoyenPaiementService moyenPaiementService;
```
to:
```java
    @Mock private org.store.paiement.application.service.IFacturationService facturationService;
```

Remove the now-unused import `org.store.paiement.application.service.IMoyenPaiementService` if nothing else in the file references it (check first — the file also has an import of `org.store.paiement.domain.model.MoyenPaiement`, which stays since `MoyenPaiement` objects are still built in test setup).

Find the two existing tests using `PreuvePaiementRequest request = new PreuvePaiementRequest(UUID.randomUUID(), "TXN-123");` and `when(moyenPaiementService.findById(request.moyenPaiementId())).thenReturn(moyen);` — change to:

```java
    @Test
    void create_should_persist_preuve_and_set_moyen_from_facturation() {
        UUID facturationId = UUID.randomUUID();
        PreuvePaiementRequest request = new PreuvePaiementRequest(facturationId, "TXN-123");
        MoyenPaiement moyen = new MoyenPaiement();
        moyen.setId(UUID.randomUUID());
        moyen.setLibelle("Wave");
        org.store.paiement.domain.model.Facturation facturation = new org.store.paiement.domain.model.Facturation();
        facturation.setId(facturationId);
        facturation.setMoyenPaiement(moyen);
        when(paiementAbonnementDomainService.findById(factureId)).thenReturn(facture);
        when(facturationService.findByIdAvailableForCurrentCountry(facturationId)).thenReturn(facturation);

        PreuvePaiementResponse response = service.create(factureId, request, null);

        assertThat(response).isNotNull();
        verify(preuvePaiementDomainService).save(any(PreuvePaiement.class));
    }

    @Test
    void create_should_propagate_badArgument_when_facturation_not_available_for_country() {
        UUID facturationId = UUID.randomUUID();
        PreuvePaiementRequest request = new PreuvePaiementRequest(facturationId, "TXN-123");
        when(paiementAbonnementDomainService.findById(factureId)).thenReturn(facture);
        when(facturationService.findByIdAvailableForCurrentCountry(facturationId))
                .thenThrow(new BadArgumentException("facturation.notAvailableForCountry"));

        assertThatThrownBy(() -> service.create(factureId, request, null))
                .isInstanceOf(BadArgumentException.class);
    }
```

(Adjust the pre-existing test bodies around these two — the file's `@BeforeEach` already sets up `facture`/`factureId` and stubs `paiementAbonnementService.ensurePaiementAccessibleByCaller`/`ensurePaiementIsFactureGeneree` as needed; reuse whatever the existing `create_should_*` tests already set up for those, changing only the moyen-resolution mocking as shown. Keep every other existing test in the file — `validate`, `reject`, `getImage` — completely untouched; they don't reference `moyenPaiementService`/`facturationId` at all.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=PreuvePaiementServiceImplTest`
Expected: compile error — `PreuvePaiementRequest` still has `moyenPaiementId`, `facturationService` mock doesn't match the real class's constructor yet.

- [ ] **Step 3: Update `PreuvePaiementRequest`**

```java
package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PreuvePaiementRequest(
        @NotNull UUID facturationId,
        @NotBlank @Size(max = 255) String referenceTransaction
) {
}
```

- [ ] **Step 4: Update `PreuvePaiementServiceImpl`**

Change the field/constructor: replace `private final IMoyenPaiementService moyenPaiementService;` with `private final org.store.paiement.application.service.IFacturationService facturationService;` (add a proper top-level import `org.store.paiement.application.service.IFacturationService` instead of the fully-qualified inline reference, and remove the `org.store.paiement.application.service.IMoyenPaiementService` import), same position in the constructor parameter list and assignment.

Change the `create` method body — replace:
```java
        preuve.setMoyen(moyenPaiementService.findById(request.moyenPaiementId()));
```
with:
```java
        org.store.paiement.domain.model.Facturation facturation = facturationService.findByIdAvailableForCurrentCountry(request.facturationId());
        preuve.setMoyen(facturation.getMoyenPaiement());
```
(add a proper top-level import `org.store.paiement.domain.model.Facturation` instead of the fully-qualified inline reference).

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest=PreuvePaiementServiceImplTest`
Expected: PASS (all tests in the file, including the 2 new/changed ones).

- [ ] **Step 6: Run the full backend suite**

Run: `./mvnw clean test`
Expected: all green. Note: `PreuvePaiementControllerTest` does not test the `create`/`payer` endpoint (confirmed — it only tests `validate`/`reject`/`getImage`), so it needs no changes and should already pass unmodified.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/store/abonnement/application/dto/PreuvePaiementRequest.java \
        src/main/java/org/store/abonnement/application/service/impl/PreuvePaiementServiceImpl.java \
        src/test/java/org/store/abonnement/application/service/PreuvePaiementServiceImplTest.java
git commit -m "feat(abonnement): resolve PreuvePaiement.moyen from Facturation instead of raw moyenPaiementId"
```

---

### Task 6: Frontend — `features/facturation/` DTOs, api adapter, hooks

**Files:**
- Create: `store-frontend/src/features/facturation/domain/dtos/facturation-response.ts`
- Create: `store-frontend/src/features/facturation/domain/dtos/facturation-option.ts`
- Create: `store-frontend/src/features/facturation/infrastructure/facturation-api.ts`
- Create: `store-frontend/src/features/facturation/application/facturation-query-keys.ts`
- Create: `store-frontend/src/features/facturation/application/useFacturationList.ts`
- Create: `store-frontend/src/features/facturation/application/useCreateFacturation.ts`
- Create: `store-frontend/src/features/facturation/application/useUpdateFacturation.ts`
- Create: `store-frontend/src/features/facturation/application/useActivateFacturation.ts`
- Create: `store-frontend/src/features/facturation/application/useDeactivateFacturation.ts`
- Create: `store-frontend/src/features/facturation/application/useDeleteFacturation.ts`
- Create: `store-frontend/src/features/facturation/application/useFacturationOptions.ts`

**Interfaces:**
- Consumes: `MoyenPaiementResponse` (`@/features/moyen-paiement/domain/dtos/moyen-paiement-response`, existing), `Country` (`@/features/country/domain/dtos/country`, existing).
- Produces: `FacturationResponse` type, `FacturationOption` type, `facturationApi.{list,create,update,activate,deactivate,delete,select}`, one hook per file (rule 52) mirroring `features/moyen-paiement/`'s exact shape.

- [ ] **Step 1: Create the DTO types**

`src/features/facturation/domain/dtos/facturation-response.ts`:

```ts
import type { MoyenPaiementResponse } from '@/features/moyen-paiement/domain/dtos/moyen-paiement-response'
import type { Country } from '@/features/country/domain/dtos/country'

/**
 * Miroir frontend de `org.store.paiement.application.dto.FacturationResponse`.
 */
export type FacturationResponse = {
  id: string
  moyenPaiement: MoyenPaiementResponse
  pays: Country | null
  numeroFacturation: string
  actif: boolean
}
```

`src/features/facturation/domain/dtos/facturation-option.ts`:

```ts
/**
 * Miroir frontend de `org.store.paiement.application.dto.FacturationOptionResponse`.
 * Retourné par `GET /api/v1/facturations/select`.
 */
export type FacturationOption = {
  facturationId: string
  moyenLibelle: string
  numeroFacturation: string
}
```

- [ ] **Step 2: Create the api adapter**

`src/features/facturation/infrastructure/facturation-api.ts`:

```ts
import { apiClient } from '@/common/infrastructure/api-client'
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { FacturationResponse } from '@/features/facturation/domain/dtos/facturation-response'
import type { FacturationOption } from '@/features/facturation/domain/dtos/facturation-option'

const BASE_PATH = '/api/v1/facturations'

type FacturationRequest = { moyenPaiementId: string; paysId: string | null; numeroFacturation: string }

type FacturationListParams = {
  moyenPaiementId?: string
  paysId?: string
  actif?: boolean
  createdStartDate?: string
  createdEndDate?: string
  page: number
  size: number
}

export const facturationApi = {
  async list(params: FacturationListParams): Promise<PageResponse<FacturationResponse>> {
    const { data } = await apiClient.get<PageResponse<FacturationResponse>>(BASE_PATH, { params })
    return data
  },

  async create(payload: FacturationRequest): Promise<FacturationResponse> {
    const { data } = await apiClient.post<FacturationResponse>(BASE_PATH, payload)
    return data
  },

  async update(id: string, payload: FacturationRequest): Promise<FacturationResponse> {
    const { data } = await apiClient.put<FacturationResponse>(`${BASE_PATH}/${id}`, payload)
    return data
  },

  async activate(id: string): Promise<FacturationResponse> {
    const { data } = await apiClient.patch<FacturationResponse>(`${BASE_PATH}/${id}/activate`)
    return data
  },

  async deactivate(id: string): Promise<FacturationResponse> {
    const { data } = await apiClient.patch<FacturationResponse>(`${BASE_PATH}/${id}/deactivate`)
    return data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`${BASE_PATH}/${id}`)
  },

  async select(): Promise<FacturationOption[]> {
    const { data } = await apiClient.get<FacturationOption[]>(`${BASE_PATH}/select`)
    return data
  },
}
```

- [ ] **Step 3: Create the query-keys file**

`src/features/facturation/application/facturation-query-keys.ts`:

```ts
export const facturationKeys = {
  all: ['facturations'] as const,
  list: () => [...facturationKeys.all, 'list'] as const,
  select: () => [...facturationKeys.all, 'select'] as const,
}
```

- [ ] **Step 4: Create the hooks — one per file**

`src/features/facturation/application/useFacturationList.ts`:

```ts
'use client'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

type FacturationListFilter = {
  moyenPaiementId?: string
  paysId?: string
  actif?: boolean
  createdStartDate?: string
  createdEndDate?: string
  page: number
  size: number
}

export function useFacturationList(filter: FacturationListFilter) {
  return useQuery({
    queryKey: [...facturationKeys.list(), filter],
    queryFn: () => facturationApi.list(filter),
    placeholderData: keepPreviousData,
  })
}
```

`src/features/facturation/application/useCreateFacturation.ts`:

```ts
'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

export function useCreateFacturation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { moyenPaiementId: string; paysId: string | null; numeroFacturation: string }) =>
      facturationApi.create(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: facturationKeys.all }),
  })
}
```

`src/features/facturation/application/useUpdateFacturation.ts`:

```ts
'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

export function useUpdateFacturation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: { moyenPaiementId: string; paysId: string | null; numeroFacturation: string } }) =>
      facturationApi.update(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: facturationKeys.all }),
  })
}
```

`src/features/facturation/application/useActivateFacturation.ts`:

```ts
'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

export function useActivateFacturation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => facturationApi.activate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: facturationKeys.all }),
  })
}
```

`src/features/facturation/application/useDeactivateFacturation.ts`:

```ts
'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

export function useDeactivateFacturation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => facturationApi.deactivate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: facturationKeys.all }),
  })
}
```

`src/features/facturation/application/useDeleteFacturation.ts`:

```ts
'use client'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

export function useDeleteFacturation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => facturationApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: facturationKeys.all }),
  })
}
```

`src/features/facturation/application/useFacturationOptions.ts`:

```ts
'use client'
import { useQuery } from '@tanstack/react-query'
import { facturationApi } from '@/features/facturation/infrastructure/facturation-api'
import { facturationKeys } from '@/features/facturation/application/facturation-query-keys'

/**
 * `GET /api/v1/facturations/select` — options de paiement disponibles
 * pour l'entreprise courante (pays résolu côté serveur).
 */
export function useFacturationOptions() {
  return useQuery({
    queryKey: facturationKeys.select(),
    queryFn: () => facturationApi.select(),
    staleTime: 60_000,
  })
}
```

- [ ] **Step 5: Verify types compile**

Run: `cd store-frontend && npx tsc --noEmit`
Expected: no error in these new files (errors may appear in files not yet updated that will consume them — none exist yet at this point in the plan).

- [ ] **Step 6: Commit**

```bash
git add store-frontend/src/features/facturation/
git commit -m "feat(facturation): add DTOs, api adapter, and hooks"
```

---

### Task 7: Frontend — admin CRUD UI (`FacturationTable`, `FacturationFormDialog`, page, tab registration)

**Files:**
- Create: `store-frontend/src/features/facturation/presentation/FacturationTable.tsx`
- Create: `store-frontend/src/features/facturation/presentation/FacturationFormDialog.tsx`
- Create: `store-frontend/src/app/(dashboard)/dashboard/administration/facturation/page.tsx`
- Create: `store-frontend/src/app/(dashboard)/dashboard/administration/facturation/FacturationPage.tsx`
- Modify: `store-frontend/src/app/(dashboard)/dashboard/administration/_tabs.ts`
- Modify: `store-frontend/src/app/(dashboard)/dashboard/administration/layout.tsx`
- Modify: `store-frontend/src/messages/fr.json`, `en.json`

**Interfaces:**
- Consumes: `useMoyenPaiementSelectList` (`@/features/moyen-paiement/application/useMoyenPaiementSelectList`, existing from Task 1 — used for the moyen combobox, auto-resolves nothing here since this is an ADMIN-only global form with no entreprise, so it returns everything unfiltered, matching the ADMIN branch already implemented), `useCountries` (existing), `Combobox` (existing, rule 54).
- Produces: admin CRUD page at `/dashboard/administration/facturation`, gated by `FACTURATION_READ`.

- [ ] **Step 1: Add the tab registration**

In `src/app/(dashboard)/dashboard/administration/_tabs.ts`, add `'facturation'` to the `AdministrationTabKey` union (alongside `'moyensPaiement'`), and add to `ADMINISTRATION_TABS`:

```ts
  { key: 'facturation', href: '/dashboard/administration/facturation', requiredPermission: 'FACTURATION_READ' },
```

In `src/app/(dashboard)/dashboard/administration/layout.tsx`, find the `TAB_ICONS` map (or equivalent icon-per-tab mapping) and add an entry for `facturation` using the `Receipt` icon from `lucide-react` (check the file's existing import list first — add `Receipt` to the existing `lucide-react` import line rather than a new import statement).

- [ ] **Step 2: Add i18n — `nav` label + full `facturation` namespace**

In both `src/messages/fr.json` and `src/messages/en.json`, inside `dashboard.administration.nav` (sibling of the existing `"moyensPaiement": "Moyens de paiement"` entry), add:
- FR: `"facturation": "Facturation"`
- EN: `"facturation": "Facturation"`

Add a new top-level `dashboard.administration.facturation` namespace (sibling of `dashboard.administration.moyensPaiement`), mirroring that namespace's shape. FR (`fr.json`):

```json
"facturation": {
  "metaTitle": "Facturation — Administration",
  "createAction": "Ajouter",
  "badge": { "actif": "Actif", "inactif": "Inactif" },
  "empty": {
    "title": "Aucune facturation",
    "description": "Ajoutez un numéro de facturation pour un moyen de paiement."
  },
  "form": {
    "createTitle": "Nouvelle facturation",
    "createDescription": "Associez un moyen de paiement à un numéro de facturation.",
    "editTitle": "Modifier la facturation",
    "editDescription": "Mettez à jour le moyen, le pays ou le numéro.",
    "moyenLabel": "Moyen de paiement",
    "moyenPlaceholder": "Choisir un moyen",
    "paysLabel": "Pays",
    "paysPlaceholder": "Global (tous pays)",
    "numeroLabel": "Numéro de facturation",
    "numeroPlaceholder": "ex : 77 000 00 00",
    "cancel": "Annuler",
    "submit": "Enregistrer",
    "submitting": "Enregistrement…"
  },
  "confirmDelete": {
    "title": "Supprimer cette facturation ?",
    "description": "Cette action est définitive.",
    "confirm": "Supprimer"
  },
  "validation": {
    "moyenRequired": "Le moyen de paiement est requis",
    "numeroRequired": "Le numéro de facturation est requis",
    "numeroMax": "100 caractères maximum"
  },
  "table": {
    "moyen": "Moyen de paiement",
    "pays": "Pays",
    "paysGlobal": "Global",
    "numero": "Numéro",
    "statut": "Statut"
  },
  "rowActions": {
    "openMenu": "Ouvrir les actions",
    "edit": "Modifier",
    "activate": "Activer",
    "deactivate": "Désactiver",
    "delete": "Supprimer"
  },
  "toasts": {
    "created": "Facturation créée",
    "updated": "Facturation mise à jour",
    "deleted": "Facturation supprimée",
    "activated": "Facturation activée",
    "deactivated": "Facturation désactivée"
  }
}
```

EN (`en.json`), same shape:

```json
"facturation": {
  "metaTitle": "Facturation — Administration",
  "createAction": "Add",
  "badge": { "actif": "Active", "inactif": "Inactive" },
  "empty": {
    "title": "No facturation",
    "description": "Add a billing number for a payment method."
  },
  "form": {
    "createTitle": "New facturation",
    "createDescription": "Associate a payment method with a billing number.",
    "editTitle": "Edit facturation",
    "editDescription": "Update the method, country, or number.",
    "moyenLabel": "Payment method",
    "moyenPlaceholder": "Choose a method",
    "paysLabel": "Country",
    "paysPlaceholder": "Global (all countries)",
    "numeroLabel": "Billing number",
    "numeroPlaceholder": "e.g. 77 000 00 00",
    "cancel": "Cancel",
    "submit": "Save",
    "submitting": "Saving…"
  },
  "confirmDelete": {
    "title": "Delete this facturation?",
    "description": "This action is permanent.",
    "confirm": "Delete"
  },
  "validation": {
    "moyenRequired": "Payment method is required",
    "numeroRequired": "Billing number is required",
    "numeroMax": "100 characters maximum"
  },
  "table": {
    "moyen": "Payment method",
    "pays": "Country",
    "paysGlobal": "Global",
    "numero": "Number",
    "statut": "Status"
  },
  "rowActions": {
    "openMenu": "Open actions",
    "edit": "Edit",
    "activate": "Activate",
    "deactivate": "Deactivate",
    "delete": "Delete"
  },
  "toasts": {
    "created": "Facturation created",
    "updated": "Facturation updated",
    "deleted": "Facturation deleted",
    "activated": "Facturation activated",
    "deactivated": "Facturation deactivated"
  }
}
```

- [ ] **Step 3: Create `FacturationFormDialog.tsx`**

```tsx
'use client'
import { zodResolver } from '@hookform/resolvers/zod'
import { useTranslations } from 'next-intl'
import { useEffect, useMemo } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { Combobox } from '@/common/presentation/ui/combobox'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/common/presentation/ui/dialog'
import { FormField } from '@/common/presentation/shared/FormField'
import { Label } from '@/common/presentation/ui/label'
import { useCountries } from '@/features/country/application/useCountries'
import { useMoyenPaiementSelectList } from '@/features/moyen-paiement/application/useMoyenPaiementSelectList'
import { useCreateFacturation } from '@/features/facturation/application/useCreateFacturation'
import { useUpdateFacturation } from '@/features/facturation/application/useUpdateFacturation'
import type { FacturationResponse } from '@/features/facturation/domain/dtos/facturation-response'

type FormValues = { moyenPaiementId: string; paysId: string; numeroFacturation: string }

type FacturationFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  target?: FacturationResponse
}

export function FacturationFormDialog({ open, onOpenChange, target }: FacturationFormDialogProps) {
  const t = useTranslations('dashboard.administration.facturation.form')
  const tToast = useTranslations('dashboard.administration.facturation.toasts')
  const isEdit = Boolean(target)
  const moyensQuery = useMoyenPaiementSelectList()
  const countriesQuery = useCountries()

  const schema = useMemo(
    () => z.object({
      moyenPaiementId: z.string().min(1, t('validation.moyenRequired')),
      paysId: z.string(),
      numeroFacturation: z.string().min(1, t('validation.numeroRequired')).max(100, t('validation.numeroMax')),
    }),
    [t],
  )

  const defaultValues: FormValues = {
    moyenPaiementId: target?.moyenPaiement.id ?? '',
    paysId: target?.pays?.id ?? '',
    numeroFacturation: target?.numeroFacturation ?? '',
  }

  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues })

  useEffect(() => {
    if (open) form.reset(defaultValues)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, target, form])

  const createMutation = useCreateFacturation()
  const updateMutation = useUpdateFacturation()
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  const moyenItems = moyensQuery.data?.content ?? []
  const countryItems = useMemo(() => (countriesQuery.data ?? []).map((c) => ({ value: c.id, label: c.name })), [countriesQuery.data])

  function handleSubmit(values: FormValues) {
    const payload = {
      moyenPaiementId: values.moyenPaiementId,
      paysId: values.paysId || null,
      numeroFacturation: values.numeroFacturation.trim(),
    }
    if (isEdit && target) {
      runMutationWithToast(updateMutation, { id: target.id, payload }, {
        successMessage: tToast('updated'),
        onSuccess: () => onOpenChange(false),
      })
    } else {
      runMutationWithToast(createMutation, payload, {
        successMessage: tToast('created'),
        onSuccess: () => onOpenChange(false),
      })
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{isEdit ? t('editTitle') : t('createTitle')}</DialogTitle>
          <DialogDescription>{isEdit ? t('editDescription') : t('createDescription')}</DialogDescription>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="flex flex-col gap-4">
          <div className="space-y-2">
            <Label required>{t('moyenLabel')}</Label>
            <Controller
              control={form.control}
              name="moyenPaiementId"
              render={({ field }) => (
                <Combobox
                  items={moyenItems}
                  value={field.value}
                  onValueChange={field.onChange}
                  placeholder={t('moyenPlaceholder')}
                  ariaLabel={t('moyenLabel')}
                  emptyLabel="—"
                />
              )}
            />
            {form.formState.errors.moyenPaiementId ? (
              <p className="text-xs text-destructive">{form.formState.errors.moyenPaiementId.message}</p>
            ) : null}
          </div>
          <div className="space-y-2">
            <Label>{t('paysLabel')}</Label>
            <Controller
              control={form.control}
              name="paysId"
              render={({ field }) => (
                <Combobox
                  items={countryItems}
                  value={field.value}
                  onValueChange={field.onChange}
                  placeholder={t('paysPlaceholder')}
                  ariaLabel={t('paysLabel')}
                  emptyLabel="—"
                />
              )}
            />
          </div>
          <FormField<FormValues> name="numeroFacturation" label={t('numeroLabel')} placeholder={t('numeroPlaceholder')} required />
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>{t('cancel')}</Button>
            <Button type="submit" disabled={isSubmitting}>{isSubmitting ? t('submitting') : t('submit')}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
```

Note: `FormField` requires being inside a `<FormProvider>` (see the fix already applied to `MoyenPaiementFormDialog.tsx` this session) — wrap the `<form>` element above in `<FormProvider {...form}>...</FormProvider>` (import `FormProvider` from `react-hook-form` alongside `Controller, useForm`). This is called out explicitly here because the exact same bug (missing `FormProvider`) was just found and fixed in a sibling component — do not reintroduce it.

- [ ] **Step 4: Create `FacturationTable.tsx`**

```tsx
'use client'
import { useTranslations } from 'next-intl'
import { Badge } from '@/common/presentation/ui/badge'
import { Button } from '@/common/presentation/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/common/presentation/ui/dropdown-menu'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/common/presentation/ui/table'
import { MoreHorizontal } from 'lucide-react'
import type { FacturationResponse } from '@/features/facturation/domain/dtos/facturation-response'

type FacturationTableProps = {
  rows: FacturationResponse[]
  onEdit: (row: FacturationResponse) => void
  onActivate: (row: FacturationResponse) => void
  onDeactivate: (row: FacturationResponse) => void
  onDelete: (row: FacturationResponse) => void
}

export function FacturationTable({ rows, onEdit, onActivate, onDeactivate, onDelete }: FacturationTableProps) {
  const t = useTranslations('dashboard.administration.facturation')

  return (
    <div className="rounded-md border border-border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('table.moyen')}</TableHead>
            <TableHead>{t('table.pays')}</TableHead>
            <TableHead>{t('table.numero')}</TableHead>
            <TableHead>{t('table.statut')}</TableHead>
            <TableHead className="w-10" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              <TableCell className="font-medium">{row.moyenPaiement.libelle}</TableCell>
              <TableCell>
                {row.pays ? row.pays.name : <Badge variant="outline">{t('table.paysGlobal')}</Badge>}
              </TableCell>
              <TableCell>{row.numeroFacturation}</TableCell>
              <TableCell>
                <Badge variant={row.actif ? 'default' : 'secondary'}>
                  {row.actif ? t('badge.actif') : t('badge.inactif')}
                </Badge>
              </TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger
                    render={
                      <Button variant="ghost" size="icon" aria-label={t('rowActions.openMenu')}>
                        <MoreHorizontal className="size-4" aria-hidden="true" />
                      </Button>
                    }
                  />
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onEdit(row)}>{t('rowActions.edit')}</DropdownMenuItem>
                    {row.actif ? (
                      <DropdownMenuItem onClick={() => onDeactivate(row)}>{t('rowActions.deactivate')}</DropdownMenuItem>
                    ) : (
                      <DropdownMenuItem onClick={() => onActivate(row)}>{t('rowActions.activate')}</DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => onDelete(row)}>
                      {t('rowActions.delete')}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
```

- [ ] **Step 5: Create `FacturationPage.tsx`** (mirrors `MoyensPaiementPage.tsx`'s structure exactly, minus the countries-management dialogs which don't apply here)

```tsx
'use client'

import { CreditCard, Plus } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { useState } from 'react'

import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { ConfirmDialog } from '@/common/presentation/shared/ConfirmDialog'
import { EmptyState } from '@/common/presentation/shared/EmptyState'
import { LoadingState } from '@/common/presentation/shared/LoadingState'
import { useActivateFacturation } from '@/features/facturation/application/useActivateFacturation'
import { useDeactivateFacturation } from '@/features/facturation/application/useDeactivateFacturation'
import { useDeleteFacturation } from '@/features/facturation/application/useDeleteFacturation'
import { useFacturationList } from '@/features/facturation/application/useFacturationList'
import { FacturationFormDialog } from '@/features/facturation/presentation/FacturationFormDialog'
import { FacturationTable } from '@/features/facturation/presentation/FacturationTable'
import type { FacturationResponse } from '@/features/facturation/domain/dtos/facturation-response'

export function FacturationPage() {
  const t = useTranslations('dashboard.administration.facturation')

  const [formOpen, setFormOpen] = useState(false)
  const [formTarget, setFormTarget] = useState<FacturationResponse | undefined>(undefined)
  const [deactivateTarget, setDeactivateTarget] = useState<FacturationResponse | undefined>(undefined)
  const [deleteTarget, setDeleteTarget] = useState<FacturationResponse | undefined>(undefined)

  const { data, isLoading } = useFacturationList({ page: 0, size: 50 })
  const rows = data?.content ?? []

  const activateMutation = useActivateFacturation()
  const deactivateMutation = useDeactivateFacturation()
  const deleteMutation = useDeleteFacturation()

  function openCreate() {
    setFormTarget(undefined)
    setFormOpen(true)
  }

  function openEdit(row: FacturationResponse) {
    setFormTarget(row)
    setFormOpen(true)
  }

  function handleActivate(row: FacturationResponse) {
    runMutationWithToast(activateMutation, row.id, { successMessage: t('toasts.activated') })
  }

  function askDeactivate(row: FacturationResponse) {
    setDeactivateTarget(row)
  }

  function handleDeactivateConfirmed() {
    if (!deactivateTarget) return
    runMutationWithToast(deactivateMutation, deactivateTarget.id, {
      successMessage: t('toasts.deactivated'),
      onSettled: () => setDeactivateTarget(undefined),
    })
  }

  function askDelete(row: FacturationResponse) {
    setDeleteTarget(row)
  }

  function handleDeleteConfirmed() {
    if (!deleteTarget) return
    runMutationWithToast(deleteMutation, deleteTarget.id, {
      successMessage: t('toasts.deleted'),
      onSettled: () => setDeleteTarget(undefined),
    })
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Button type="button" onClick={openCreate} className="shrink-0">
          <Plus className="size-4" aria-hidden="true" />
          <span className="hidden sm:inline">{t('createAction')}</span>
        </Button>
      </div>

      {isLoading ? (
        <LoadingState rows={4} />
      ) : rows.length === 0 ? (
        <EmptyState
          title={t('empty.title')}
          description={t('empty.description')}
          icon={<CreditCard aria-hidden="true" className="size-8" />}
          action={
            <Button onClick={openCreate}>
              <Plus aria-hidden="true" className="size-4" />
              <span className="hidden sm:inline">{t('createAction')}</span>
            </Button>
          }
        />
      ) : (
        <FacturationTable
          rows={rows}
          onEdit={openEdit}
          onActivate={handleActivate}
          onDeactivate={askDeactivate}
          onDelete={askDelete}
        />
      )}

      <FacturationFormDialog open={formOpen} onOpenChange={setFormOpen} target={formTarget} />

      <ConfirmDialog
        open={Boolean(deactivateTarget)}
        onOpenChange={(open) => { if (!open) setDeactivateTarget(undefined) }}
        title={t('confirmDeactivate.title')}
        description={t('confirmDeactivate.description')}
        confirmLabel={t('confirmDeactivate.confirm')}
        destructive
        onConfirm={handleDeactivateConfirmed}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => { if (!open) setDeleteTarget(undefined) }}
        title={t('confirmDelete.title')}
        description={t('confirmDelete.description')}
        confirmLabel={t('confirmDelete.confirm')}
        destructive
        onConfirm={handleDeleteConfirmed}
      />
    </div>
  )
}
```

(Note: this plan's `FacturationTable` props don't include `confirmDeactivate`/`confirmDelete` i18n keys yet in Step 2's JSON blocks — add `"confirmDeactivate": { "title": "Désactiver cette facturation ?", "description": "Elle ne sera plus proposée aux propriétaires.", "confirm": "Désactiver" }` to both the FR and EN `facturation` namespaces from Step 2, sibling of `confirmDelete`, mirroring `moyensPaiement.confirmDeactivate`'s exact shape and wording pattern.)

- [ ] **Step 6: Create the route files**

`src/app/(dashboard)/dashboard/administration/facturation/page.tsx`:

```tsx
import type { Metadata } from 'next'
import { getTranslations } from 'next-intl/server'
import { FacturationPage } from './FacturationPage'

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('dashboard.administration.facturation')
  return { title: t('metaTitle') }
}

export default function Page() {
  return <FacturationPage />
}
```

(Check `src/app/(dashboard)/dashboard/administration/moyens-paiement/page.tsx` first to confirm this exact shape matches the project's established route-file convention — copy its structure precisely if it differs in any detail, e.g. default export naming or metadata generation style.)

- [ ] **Step 7: Verify types compile and tests pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: `tsc` clean, `vitest` stays at 342/342 (no test file for any of these new components today, matching the `moyensPaiement` admin page's own untested precedent).

- [ ] **Step 8: Manual check**

Start the dev server, navigate to `/dashboard/administration/facturation` as ADMIN: confirm the tab is visible (now that `FACTURATION_READ` is actually granted, unlike `MOYEN_PAIEMENT_READ`'s pre-existing bug), create a facturation for an existing moyen with no country (global), create another with a country, confirm the uniqueness error surfaces as a toast (not a raw 500) when attempting a duplicate `(moyen, pays)` pair.

- [ ] **Step 9: Commit**

```bash
git add store-frontend/src/features/facturation/presentation/ \
        "store-frontend/src/app/(dashboard)/dashboard/administration/facturation/" \
        "store-frontend/src/app/(dashboard)/dashboard/administration/_tabs.ts" \
        "store-frontend/src/app/(dashboard)/dashboard/administration/layout.tsx" \
        store-frontend/src/messages/fr.json store-frontend/src/messages/en.json
git commit -m "feat(facturation): add admin CRUD page and tab registration"
```

---

### Task 8: Frontend — `SubmitPaiementForm.tsx` uses Facturation instead of `MoyenPaiementSelect`

**Files:**
- Modify: `store-frontend/src/features/abonnement/domain/dtos/preuve-paiement-request.ts`
- Modify: `store-frontend/src/features/abonnement/presentation/SubmitPaiementForm.tsx`
- Modify: `store-frontend/src/messages/fr.json`, `en.json`

**Interfaces:**
- Consumes: `useFacturationOptions()` (Task 6), `FacturationOption` type (Task 6).
- Produces: `SubmitPaiementForm` submits `{ facturationId, referenceTransaction }` instead of `{ moyenPaiementId, referenceTransaction }`; the payment-destination banner now shows the selected option's real `numeroFacturation` instead of the static `NEXT_PUBLIC_SUBSCRIPTION_PAYMENT_PHONE` env var.

- [ ] **Step 1: Update `PreuvePaiementRequest` (frontend type)**

```ts
export type PreuvePaiementRequest = {
  facturationId: string
  referenceTransaction: string
}
```

- [ ] **Step 2: Add an i18n key for the moyen combobox's empty placeholder**

Add `"moyenPlaceholder": "Aucun moyen disponible"` (FR) / `"moyenPlaceholder": "No method available"` (EN) to `dashboard.entreprise.paiements.submitDialog.fields` in both message files, sibling of the existing `"moyen"` key (used when `useFacturationOptions()` returns an empty list — no billing option configured for the entreprise's country yet).

- [ ] **Step 3: Update `SubmitPaiementForm.tsx`**

Remove the import:
```ts
import { MoyenPaiementSelect } from '@/features/moyen-paiement/presentation/MoyenPaiementSelect'
```
Replace with:
```ts
import { Combobox } from '@/common/presentation/ui/combobox'
import { useFacturationOptions } from '@/features/facturation/application/useFacturationOptions'
```

Change the `FormValues` type:
```ts
type FormValues = {
  facturationId: string
  referenceTransaction: string
}
```

Change the zod schema's field name from `moyenPaiementId` to `facturationId` (keep the same validation rule, `z.string().min(1, t('validation.moyenRequired'))`), and the `defaultValues` object's key too.

Add the options query and the selected-option lookup, right after `const submitMutation = useSubmitPaiement()`:
```ts
  const facturationOptionsQuery = useFacturationOptions()
  const facturationOptions = facturationOptionsQuery.data ?? []
  const selectedFacturationId = form.watch('facturationId')
  const selectedOption = facturationOptions.find((option) => option.facturationId === selectedFacturationId)
```

Remove the `const paymentPhone = process.env.NEXT_PUBLIC_SUBSCRIPTION_PAYMENT_PHONE` line entirely.

Replace the banner block:
```tsx
        {paymentPhone && (
          <div className="flex gap-3 rounded-lg border border-blue-200 bg-blue-50 p-3 dark:border-blue-800/40 dark:bg-blue-900/20">
            <Phone className="mt-0.5 size-4 shrink-0 text-blue-600 dark:text-blue-400" aria-hidden="true" />
            <div className="flex flex-col gap-0.5">
              <span className="text-sm font-medium text-blue-800 dark:text-blue-300">
                {t('fields.paymentPhoneLabel')}
              </span>
              <span className="font-mono text-base font-bold tracking-wide text-blue-900 dark:text-blue-200">
                {paymentPhone}
              </span>
              <span className="text-xs text-blue-700 dark:text-blue-400">
                {t('fields.paymentPhoneHint')}
              </span>
            </div>
          </div>
        )}
```
with:
```tsx
        {selectedOption && (
          <div className="flex gap-3 rounded-lg border border-blue-200 bg-blue-50 p-3 dark:border-blue-800/40 dark:bg-blue-900/20">
            <Phone className="mt-0.5 size-4 shrink-0 text-blue-600 dark:text-blue-400" aria-hidden="true" />
            <div className="flex flex-col gap-0.5">
              <span className="text-sm font-medium text-blue-800 dark:text-blue-300">
                {t('fields.paymentPhoneLabel')}
              </span>
              <span className="font-mono text-base font-bold tracking-wide text-blue-900 dark:text-blue-200">
                {selectedOption.numeroFacturation}
              </span>
              <span className="text-xs text-blue-700 dark:text-blue-400">
                {t('fields.paymentPhoneHint')}
              </span>
            </div>
          </div>
        )}
```

Replace the moyen field's `Controller`:
```tsx
            <Controller
              control={form.control}
              name="moyenPaiementId"
              render={({ field }) => (
                <MoyenPaiementSelect
                  value={field.value}
                  onValueChange={field.onChange}
                  ariaLabel={t('fields.moyen')}
                />
              )}
            />
```
with:
```tsx
            <Controller
              control={form.control}
              name="facturationId"
              render={({ field }) => (
                <Combobox
                  items={facturationOptions.map((option) => ({ value: option.facturationId, label: option.moyenLibelle }))}
                  value={field.value}
                  onValueChange={field.onChange}
                  placeholder={t('fields.moyenPlaceholder')}
                  ariaLabel={t('fields.moyen')}
                  emptyLabel="—"
                />
              )}
            />
```

Update the error-message lookups and `handleSubmitForm`'s payload construction — change `form.formState.errors.moyenPaiementId?.message` to `form.formState.errors.facturationId?.message`, and change:
```ts
        payload: {
          moyenPaiementId: values.moyenPaiementId,
          referenceTransaction: values.referenceTransaction.trim(),
        },
```
to:
```ts
        payload: {
          facturationId: values.facturationId,
          referenceTransaction: values.referenceTransaction.trim(),
        },
```

- [ ] **Step 4: Verify types compile and tests pass**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: `tsc` clean, `vitest` stays at 342/342 (no test file exists for `SubmitPaiementForm.tsx` today).

- [ ] **Step 5: Manual check**

Start the dev server, log in as an OWNER whose entreprise has at least one `Facturation` configured for its country (or a global one), open the "Soumettre un paiement" dialog: confirm the combobox lists the expected moyens, and picking one shows the correct `numeroFacturation` in the blue banner before submission.

- [ ] **Step 6: Commit**

```bash
git add store-frontend/src/features/abonnement/domain/dtos/preuve-paiement-request.ts \
        store-frontend/src/features/abonnement/presentation/SubmitPaiementForm.tsx \
        store-frontend/src/messages/fr.json store-frontend/src/messages/en.json
git commit -m "feat(abonnement): submit payment via Facturation instead of raw MoyenPaiement"
```

---

### Task 9: Final verification

**Files:** none (verification only).

- [ ] **Step 1: Full backend suite**

Run: `./mvnw clean test`
Expected: all green. Baseline before this plan was 1080 (post Task-1 + its final-review fix wave); expect 1080 + roughly 21 new tests across Tasks 1–5 (4 + 7 + 1 + 2 + 2 new/changed in `FacturationDomainServiceTest`/`FacturationServiceImplTest`/`FacturationControllerTest`/`PreuvePaiementServiceImplTest`) — do not treat a mismatch from this estimate as a failure signal by itself, just confirm 0 failures/errors.

- [ ] **Step 2: Full frontend suite**

Run: `cd store-frontend && npx tsc --noEmit && npx vitest run`
Expected: `tsc` clean, vitest stays at 342/342 (no new test files planned in this plan for the frontend — matches the existing untested-admin-page precedent already established for `moyensPaiement`).

- [ ] **Step 3: Manual QA checklist**

- Admin → Facturation: create/edit/activate/deactivate/delete a facturation; confirm the uniqueness error (duplicate moyen+pays, including the global case) shows a friendly message, not a raw 500.
- Admin → Facturation tab is actually visible in the sidebar (unlike the pre-existing `moyensPaiement` bug, now also fixed separately) — confirm `FACTURATION_READ` is genuinely granted to ADMIN.
- OWNER → subscription payment submission: confirm the moyen combobox only shows options valid for the entreprise's country (global + country-specific), the billing number banner updates when the selection changes, and submitting succeeds end-to-end (a `PreuvePaiement` row is created with the correct `moyen`).
- Attempt (e.g. via a direct API call or a temporarily-misconfigured `Facturation`) to submit a `facturationId` whose `pays` doesn't match the entreprise's country — confirm the 400 `facturation.notAvailableForCountry` response.

- [ ] **Step 4: Report**

Summarize pass/fail of Steps 1–3 back to the user. Do not commit anything in this task — it's verification-only.
