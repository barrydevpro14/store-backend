# SaaS Platform P&L (Dépenses/Revenus/Bénéfice) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the SaaS ADMIN a period-scoped P&L view (revenu / dépenses plateforme / bénéfice, filterable by country and abonnement, over month/quarter/year/custom range) without touching any existing admin-reporting code.

**Architecture:** New backend module `org.store.plateforme` (global CRUD for platform expenses, mirrors the existing `depense` module), a new `Revenu` entity fed by a brand-new Spring event (`RevenuRecordedEvent`) fired from one added line in `PaiementAbonnementServiceImpl.validate()`, and a new standalone `PlateformeReportingController`. Frontend: one shared `dateHelpers.ts` extension (quarter/year presets) plus a new `features/plateforme-depense` DDD slice backing two new Administration pages.

**Tech Stack:** Java 21 / Spring Boot 4 / Spring Data JPA / Flyway / JUnit 5 + Mockito + AssertJ (backend). Next.js 16 / React 19 / TypeScript / TanStack Query / Vitest (frontend).

**Spec:** `.claude/2026-08-23-depenses-revenus-saas-design.md` — read it alongside this plan; this plan does not repeat the spec's rationale, only exact file paths and code.

## Global Constraints

- **Nothing existing is modified** except exactly one new line in `PaiementAbonnementServiceImpl.validate()`. `AdminReportingController`, `PeriodReportResponse`, `AdminOverviewStatsResponse`, `getStatistiquesPaiement`, `PaiementAbonnementStatsResponse`, `sumValidatedRevenueForYear`, `ReportingPage.tsx`, `PeriodTab.tsx` are all untouched.
- New permissions (`PLATFORM_EXPENSE_{CREATE,READ,UPDATE,DELETE}`, `PLATFORM_EXPENSE_CATEGORY_{CREATE,READ,UPDATE,DELETE}`, `PLATFORM_REPORT_READ`) are declared and granted **only** to the ADMIN role in `roles-permissions.yml`. They are **not** added to `PermissionCode.java` (that enum only holds permissions Java code checks programmatically; `@PreAuthorize` uses raw string literals, and most existing permissions — `EXPENSE_*`, `REPORT_FINANCIAL` included — already aren't in it).
- `REPORT_FINANCIAL` must never be reused for this feature — it's also granted to OWNER and MANAGER in the YAML today.
- IDs are `UUID`. Money is `BigDecimal precision=19 scale=2`. Follow existing package layout: `domain/{model,repository,service}`, `infrastructure/repository`, `application/{dto,service}`, `presentation`.
- Rule 40 (backend): every `<X>Filter` used for a paginated list carries its own natural ordering — `DepensePlateformeFilter` orders by `dateDepense DESC` (mirrors `Depense`, which has a natural business date); `CategoryDepensePlateformeFilter` orders by `createdAt DESC` (mirrors `CategoryDepense`, which has no natural date).
- Rule 30 (backend): methods over 3 params get grouped into a Filter/Command record.
- Rule 46 (frontend): one component per file. Rule 52: one hook per file. Rule 33: Filter DTOs with ≥2 criteria are records, not loose params.
- No DB migration for `Revenu`/`DepensePlateforme`/`CategoryDepensePlateforme` beyond V85/V86 — no other schema change needed.

---

## Partie A — Backend

### Task 1: Flyway migrations V85 (plateforme expense tables) + V86 (revenu table + backfill)

**Files:**
- Create: `src/main/resources/db/migration/V85__create_plateforme_depense_tables.sql`
- Create: `src/main/resources/db/migration/V86__create_revenu_table.sql`

**Interfaces:**
- Produces: tables `category_depense_plateforme`, `depense_plateforme`, `revenu` that Tasks 3/4/5 map their entities onto.

This task has no Java test cycle — migrations are verified by booting the app (Task 7 runs the full suite, which boots the Spring context via Flyway on the test datasource). Steps:

- [ ] **Step 1: Write `V85__create_plateforme_depense_tables.sql`**

```sql
CREATE TABLE category_depense_plateforme (
    id          UUID            PRIMARY KEY,
    nom         VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    actif       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT uk_category_depense_plateforme_nom UNIQUE (nom)
);

CREATE TABLE depense_plateforme (
    id                 UUID            PRIMARY KEY,
    category_id        UUID            NOT NULL,
    country_id         UUID,
    moyen_paiement_id  UUID            NOT NULL,
    libelle            VARCHAR(200)    NOT NULL,
    description        TEXT,
    date_depense        DATE            NOT NULL,
    montant            DECIMAL(19, 2)  NOT NULL,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    CONSTRAINT fk_depense_plateforme_category  FOREIGN KEY (category_id) REFERENCES category_depense_plateforme(id),
    CONSTRAINT fk_depense_plateforme_country   FOREIGN KEY (country_id)  REFERENCES country(id),
    CONSTRAINT fk_depense_plateforme_moyen     FOREIGN KEY (moyen_paiement_id) REFERENCES moyen_paiement(id)
);
```

- [ ] **Step 2: Write `V86__create_revenu_table.sql`**

```sql
CREATE TABLE revenu (
    id             UUID            PRIMARY KEY,
    entreprise_id  UUID            NOT NULL,
    country_id     UUID            NOT NULL,
    montant        DECIMAL(19, 2)  NOT NULL,
    date_paiement  DATE            NOT NULL,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255),
    CONSTRAINT fk_revenu_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise(id),
    CONSTRAINT fk_revenu_country    FOREIGN KEY (country_id)    REFERENCES country(id)
);

INSERT INTO revenu (id, entreprise_id, country_id, montant, date_paiement, created_at, updated_at)
SELECT gen_random_uuid(), a.entreprise_id, e.country_id, pa.montant_final, pa.date_paiement, pa.created_at, pa.updated_at
FROM paiement_abonnement pa
JOIN abonnement a  ON pa.abonnement_id = a.id
JOIN entreprise e  ON a.entreprise_id  = e.id
WHERE pa.statut = 'VALIDE';
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V85__create_plateforme_depense_tables.sql src/main/resources/db/migration/V86__create_revenu_table.sql
git commit -m "feat(plateforme): V85/V86 migrations for platform expense + revenu tables"
```

---

### Task 2: `roles-permissions.yml` — new ADMIN-only permissions

**Files:**
- Modify: `src/main/resources/security/roles-permissions.yml`

**Interfaces:**
- Produces: permission strings `PLATFORM_EXPENSE_CREATE`, `PLATFORM_EXPENSE_READ`, `PLATFORM_EXPENSE_UPDATE`, `PLATFORM_EXPENSE_DELETE`, `PLATFORM_EXPENSE_CATEGORY_CREATE`, `PLATFORM_EXPENSE_CATEGORY_READ`, `PLATFORM_EXPENSE_CATEGORY_UPDATE`, `PLATFORM_EXPENSE_CATEGORY_DELETE`, `PLATFORM_REPORT_READ` — consumed by `@PreAuthorize` in Tasks 3/4/6.

No Java test — `RolesPermissionsSyncServiceImplTest` already covers the generic sync mechanism; this task only adds data. Verified by Task 7 (the app boots with `security.rbac.sync=true` in the test profile and syncs these rows).

- [ ] **Step 1: Add the permission declarations**

In the top `permissions:` list, right after the existing `# EXPENSE` block (the one ending `- EXPENSE_PAY`, found via `grep -n "# EXPENSE" src/main/resources/security/roles-permissions.yml`), insert:

```yaml
  # PLATFORM EXPENSE (SaaS vendor's own operating costs — admin only)
  - PLATFORM_EXPENSE_CREATE
  - PLATFORM_EXPENSE_READ
  - PLATFORM_EXPENSE_UPDATE
  - PLATFORM_EXPENSE_DELETE
  - PLATFORM_EXPENSE_CATEGORY_CREATE
  - PLATFORM_EXPENSE_CATEGORY_READ
  - PLATFORM_EXPENSE_CATEGORY_UPDATE
  - PLATFORM_EXPENSE_CATEGORY_DELETE
  - PLATFORM_REPORT_READ
```

- [ ] **Step 2: Grant them to ADMIN only**

In the `roles:` section, find the `ADMIN` role block (`grep -n "libelle: ADMIN" src/main/resources/security/roles-permissions.yml`, its `permissions:` list runs from there to just before `- libelle: OWNER`). Right after its existing `- REPORT_FINANCIAL` line, add:

```yaml
      - PLATFORM_EXPENSE_CREATE
      - PLATFORM_EXPENSE_READ
      - PLATFORM_EXPENSE_UPDATE
      - PLATFORM_EXPENSE_DELETE
      - PLATFORM_EXPENSE_CATEGORY_CREATE
      - PLATFORM_EXPENSE_CATEGORY_READ
      - PLATFORM_EXPENSE_CATEGORY_UPDATE
      - PLATFORM_EXPENSE_CATEGORY_DELETE
      - PLATFORM_REPORT_READ
```

Do **not** add any of these 9 lines to the OWNER or MANAGER role blocks.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/security/roles-permissions.yml
git commit -m "feat(plateforme): declare PLATFORM_EXPENSE_* and PLATFORM_REPORT_READ permissions, ADMIN only"
```

---

### Task 3: `CategoryDepensePlateforme` CRUD (entity → controller, mirrors `CategoryDepense` minus `Entreprise` scoping)

**Files:**
- Create: `src/main/java/org/store/plateforme/domain/model/CategoryDepensePlateforme.java`
- Create: `src/main/java/org/store/plateforme/domain/repository/CategoryDepensePlateformeRepository.java`
- Create: `src/main/java/org/store/plateforme/domain/service/CategoryDepensePlateformeDomainService.java`
- Create: `src/main/java/org/store/plateforme/infrastructure/repository/CategoryDepensePlateformeJpaRepository.java`
- Create: `src/main/java/org/store/plateforme/application/dto/CategoryDepensePlateformeRequest.java`
- Create: `src/main/java/org/store/plateforme/application/dto/CategoryDepensePlateformeResponse.java`
- Create: `src/main/java/org/store/plateforme/application/dto/CategoryDepensePlateformeSummaryResponse.java`
- Create: `src/main/java/org/store/plateforme/application/dto/CategoryDepensePlateformeFilter.java`
- Create: `src/main/java/org/store/plateforme/application/service/ICategoryDepensePlateformeService.java`
- Create: `src/main/java/org/store/plateforme/application/service/impl/CategoryDepensePlateformeServiceImpl.java`
- Create: `src/main/java/org/store/plateforme/presentation/CategoryDepensePlateformeController.java`
- Test: `src/test/java/org/store/plateforme/application/service/CategoryDepensePlateformeServiceImplTest.java`
- Test: `src/test/java/org/store/plateforme/presentation/CategoryDepensePlateformeControllerTest.java`

**Interfaces:**
- Produces: `ICategoryDepensePlateformeService.findById(UUID id) → CategoryDepensePlateforme` (raw entity — consumed by Task 4's `DepensePlateformeServiceImpl.create/update`).
- Produces: `CategoryDepensePlateformeSummaryResponse(UUID id, String nom)` — consumed by `DepensePlateformeResponse` in Task 4.
- Endpoints: `POST/GET/GET{id}/PUT/DELETE /api/v1/admin/plateforme/expense-categories`.

- [ ] **Step 1: Write the entity**

```java
package org.store.plateforme.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;

@Getter
@Setter
@Entity
@Table(name = CategoryDepensePlateforme.TABLE_NAME, uniqueConstraints = @UniqueConstraint(name = "uk_category_depense_plateforme_nom", columnNames = {"nom"}))
public class CategoryDepensePlateforme extends AuditableEntity {
    public static final String TABLE_NAME = "category_depense_plateforme";

    private String nom;

    private String description;

    private boolean actif = true;
}
```

- [ ] **Step 2: Write the domain repository port + JPA adapter**

```java
package org.store.plateforme.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.Optional;

public interface CategoryDepensePlateformeRepository extends BaseRepository<CategoryDepensePlateforme> {

    Optional<CategoryDepensePlateforme> findByNom(String nom);

    @Query("SELECT COUNT(c) > 0 FROM CategoryDepensePlateforme c WHERE LOWER(c.nom) = LOWER(:nom)")
    boolean existsByNom(@Param("nom") String nom);

    @Query(value = """
            SELECT new org.store.plateforme.application.dto.CategoryDepensePlateformeResponse(category)
            FROM CategoryDepensePlateforme category
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(category.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR category.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            ORDER BY category.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(category)
            FROM CategoryDepensePlateforme category
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(category.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR category.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            """)
    Page<CategoryDepensePlateformeResponse> findResponsesByFilter(
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
```

```java
package org.store.plateforme.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.repository.CategoryDepensePlateformeRepository;

import java.util.UUID;

public interface CategoryDepensePlateformeJpaRepository extends JpaRepository<CategoryDepensePlateforme, UUID>, CategoryDepensePlateformeRepository {
}
```

- [ ] **Step 3: Write the domain service**

```java
package org.store.plateforme.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.repository.CategoryDepensePlateformeRepository;

@Service
public class CategoryDepensePlateformeDomainService extends GlobalService<CategoryDepensePlateforme, CategoryDepensePlateformeRepository> {
    public CategoryDepensePlateformeDomainService(CategoryDepensePlateformeRepository repository) {
        super(repository);
    }

    public CategoryDepensePlateforme create(CategoryDepensePlateformeRequest request) {
        CategoryDepensePlateforme category = new CategoryDepensePlateforme();
        category.setNom(request.nom());
        category.setDescription(request.description());
        category.setActif(request.actif() == null || request.actif());
        return save(category);
    }

    public boolean existsByNom(String nom) {
        return repository.existsByNom(nom);
    }

    public Page<CategoryDepensePlateformeResponse> findResponses(CategoryDepensePlateformeFilter filter) {
        return repository.findResponsesByFilter(
                filter.nom(), LikePatternHelper.toLikePattern(filter.nom()),
                filter.actif(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }
}
```

- [ ] **Step 4: Write the DTOs**

```java
package org.store.plateforme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryDepensePlateformeRequest(
        @NotBlank @Size(max = 100) String nom,
        @Size(max = 500) String description,
        Boolean actif
) {
}
```

```java
package org.store.plateforme.application.dto;

import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public record CategoryDepensePlateformeResponse(
        UUID id,
        String nom,
        String description,
        boolean actif
) {
    public CategoryDepensePlateformeResponse(CategoryDepensePlateforme category) {
        this(category.getId(), category.getNom(), category.getDescription(), category.isActif());
    }
}
```

```java
package org.store.plateforme.application.dto;

import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public record CategoryDepensePlateformeSummaryResponse(
        UUID id,
        String nom
) {
    public CategoryDepensePlateformeSummaryResponse(CategoryDepensePlateforme category) {
        this(category.getId(), category.getNom());
    }
}
```

```java
package org.store.plateforme.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.validation.DatePattern;

public record CategoryDepensePlateformeFilter(
        String nom,
        Boolean actif,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
```

- [ ] **Step 5: Write the application service interface + impl**

```java
package org.store.plateforme.application.service;

import org.springframework.data.domain.Page;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public interface ICategoryDepensePlateformeService {

    CategoryDepensePlateformeResponse create(CategoryDepensePlateformeRequest request);

    CategoryDepensePlateforme findById(UUID id);

    CategoryDepensePlateformeResponse findResponseById(UUID id);

    Page<CategoryDepensePlateformeResponse> findAll(CategoryDepensePlateformeFilter filter);

    CategoryDepensePlateformeResponse update(UUID id, CategoryDepensePlateformeRequest request);

    void delete(UUID id);
}
```

```java
package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;

import java.util.UUID;

/** CRUD des catégories de dépense plateforme — référentiel global, non scopé par entreprise. */
@Service
@Transactional(readOnly = true)
public class CategoryDepensePlateformeServiceImpl implements ICategoryDepensePlateformeService {

    private final CategoryDepensePlateformeDomainService domainService;

    public CategoryDepensePlateformeServiceImpl(CategoryDepensePlateformeDomainService domainService) {
        this.domainService = domainService;
    }

    /** Crée la catégorie après vérification d'unicité du nom (globale). */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse create(CategoryDepensePlateformeRequest request) {
        ensureNomAvailable(request.nom());
        return new CategoryDepensePlateformeResponse(domainService.create(request));
    }

    @Override
    public CategoryDepensePlateforme findById(UUID id) {
        return domainService.findById(id);
    }

    @Override
    public CategoryDepensePlateformeResponse findResponseById(UUID id) {
        return new CategoryDepensePlateformeResponse(domainService.findById(id));
    }

    @Override
    public Page<CategoryDepensePlateformeResponse> findAll(CategoryDepensePlateformeFilter filter) {
        return domainService.findResponses(filter);
    }

    /** Met à jour la catégorie après contrôle d'unicité du nom (si changé). */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse update(UUID id, CategoryDepensePlateformeRequest request) {
        CategoryDepensePlateforme category = domainService.findById(id);
        if (!category.getNom().equals(request.nom())) {
            ensureNomAvailable(request.nom());
        }
        category.setNom(request.nom());
        category.setDescription(request.description());
        if (request.actif() != null) {
            category.setActif(request.actif());
        }
        return new CategoryDepensePlateformeResponse(domainService.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    /** Lève UniqueResourceException si une catégorie portant ce nom existe déjà. */
    private void ensureNomAvailable(String nom) {
        if (domainService.existsByNom(nom)) {
            throw new UniqueResourceException("categoryDepensePlateforme.nom.alreadyExists", nom);
        }
    }
}
```

- [ ] **Step 6: Write the controller**

```java
package org.store.plateforme.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;

import java.util.UUID;

@RestController
@RequestMapping(CategoryDepensePlateformeController.BASE_PATH)
public class CategoryDepensePlateformeController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/expense-categories";

    private final ICategoryDepensePlateformeService service;

    public CategoryDepensePlateformeController(ICategoryDepensePlateformeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_CREATE')")
    public ResponseEntity<CategoryDepensePlateformeResponse> create(@Valid @RequestBody CategoryDepensePlateformeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_READ')")
    public ResponseEntity<Page<CategoryDepensePlateformeResponse>> list(@RequestParam(required = false) String nom,
                                                                        @RequestParam(required = false) Boolean actif,
                                                                        @RequestParam(required = false) String startDate,
                                                                        @RequestParam(required = false) String endDate,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findAll(new CategoryDepensePlateformeFilter(nom, actif, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_READ')")
    public ResponseEntity<CategoryDepensePlateformeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_UPDATE')")
    public ResponseEntity<CategoryDepensePlateformeResponse> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody CategoryDepensePlateformeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Add i18n key** — append one line to each file (mirrors the exact style of the existing `categoryDepense.nom.alreadyExists` key already in both files):

`src/main/resources/messages.properties` (FR, default):
```
categoryDepensePlateforme.nom.alreadyExists=Une catégorie de dépense plateforme portant le nom "{0}" existe déjà
```

`src/main/resources/messages_en.properties`:
```
categoryDepensePlateforme.nom.alreadyExists=A platform expense category with name "{0}" already exists
```

- [ ] **Step 8: Write the failing tests**

`CategoryDepensePlateformeServiceImplTest` (mirrors `CategoryDepenseServiceImplTest` minus the entreprise scoping):

```java
package org.store.plateforme.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.impl.CategoryDepensePlateformeServiceImpl;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDepensePlateformeServiceImplTest {

    @Mock private CategoryDepensePlateformeDomainService domainService;
    @InjectMocks private CategoryDepensePlateformeServiceImpl service;

    @Test
    void create_should_persist_when_nom_available() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", "Serveurs cloud", true);
        CategoryDepensePlateforme saved = new CategoryDepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setNom("Hébergement");
        saved.setActif(true);

        when(domainService.existsByNom("Hébergement")).thenReturn(false);
        when(domainService.create(request)).thenReturn(saved);

        CategoryDepensePlateformeResponse response = service.create(request);

        assertThat(response.nom()).isEqualTo("Hébergement");
    }

    @Test
    void create_should_throw_when_nom_already_taken() {
        CategoryDepensePlateformeRequest request = new CategoryDepensePlateformeRequest("Hébergement", null, true);
        when(domainService.existsByNom("Hébergement")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);
    }
}
```

`CategoryDepensePlateformeControllerTest` (mirrors `CategoryDepenseController` test pattern via `MockMvcBuilders.standaloneSetup`):

```java
package org.store.plateforme.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryDepensePlateformeControllerTest {

    private MockMvc mockMvc;
    private ICategoryDepensePlateformeService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(ICategoryDepensePlateformeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryDepensePlateformeController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void should_return_201_when_category_created() throws Exception {
        CategoryDepensePlateformeRequest body = new CategoryDepensePlateformeRequest("Hébergement", "desc", true);
        when(service.create(any(CategoryDepensePlateformeRequest.class)))
                .thenReturn(new CategoryDepensePlateformeResponse(java.util.UUID.randomUUID(), "Hébergement", "desc", true));

        mockMvc.perform(post(CategoryDepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Hébergement"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        CategoryDepensePlateformeRequest body = new CategoryDepensePlateformeRequest("", null, true);

        mockMvc.perform(post(CategoryDepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 9: Run tests to verify they fail**

Run: `./mvnw test -Dtest=CategoryDepensePlateformeServiceImplTest,CategoryDepensePlateformeControllerTest`
Expected: FAIL — classes under test don't exist yet (compile error) if Steps 1-6 weren't done yet; if done in order, skip to Step 10.

- [ ] **Step 10: Run tests to verify they pass**

Run: `./mvnw test -Dtest=CategoryDepensePlateformeServiceImplTest,CategoryDepensePlateformeControllerTest`
Expected: PASS, 4/4 green.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/org/store/plateforme src/test/java/org/store/plateforme src/main/resources/messages.properties src/main/resources/messages_en.properties
git commit -m "feat(plateforme): CategoryDepensePlateforme CRUD (global admin-only expense categories)"
```

---

### Task 4: `DepensePlateforme` CRUD (entity → controller, mirrors `Depense` minus `Magasin` scoping, plus optional `country`)

**Files:**
- Create: `src/main/java/org/store/plateforme/domain/model/DepensePlateforme.java`
- Create: `src/main/java/org/store/plateforme/domain/repository/DepensePlateformeRepository.java`
- Create: `src/main/java/org/store/plateforme/domain/service/DepensePlateformeDomainService.java`
- Create: `src/main/java/org/store/plateforme/infrastructure/repository/DepensePlateformeJpaRepository.java`
- Create: `src/main/java/org/store/plateforme/application/dto/DepensePlateformeRequest.java`
- Create: `src/main/java/org/store/plateforme/application/dto/DepensePlateformeResponse.java`
- Create: `src/main/java/org/store/plateforme/application/dto/DepensePlateformeFilter.java`
- Create: `src/main/java/org/store/plateforme/application/dto/DepensePlateformeTotalResponse.java`
- Create: `src/main/java/org/store/plateforme/application/service/IDepensePlateformeService.java`
- Create: `src/main/java/org/store/plateforme/application/service/impl/DepensePlateformeServiceImpl.java`
- Create: `src/main/java/org/store/plateforme/presentation/DepensePlateformeController.java`
- Test: `src/test/java/org/store/plateforme/application/service/DepensePlateformeServiceImplTest.java`
- Test: `src/test/java/org/store/plateforme/presentation/DepensePlateformeControllerTest.java`

**Interfaces:**
- Consumes: `ICategoryDepensePlateformeService.findById(UUID)` (Task 3), `IMoyenPaiementService.findById(UUID)` (existing), `CountryDomainService.findById(UUID)` (existing).
- Produces: `IDepensePlateformeService.computeTotal(String startDate, String endDate, UUID countryId) → BigDecimal` — consumed by Task 6 (`PlateformeReportingServiceImpl`).
- Endpoints: `POST/GET/GET{id}/PUT/DELETE /api/v1/admin/plateforme/depenses` + `GET /api/v1/admin/plateforme/depenses/total`.

- [ ] **Step 1: Write the entity**

```java
package org.store.plateforme.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;
import org.store.paiement.domain.model.MoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = DepensePlateforme.TABLE_NAME)
public class DepensePlateforme extends AuditableEntity {
    public static final String TABLE_NAME = "depense_plateforme";

    @ManyToOne(fetch = FetchType.LAZY)
    private CategoryDepensePlateforme category;

    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dateDepense;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moyen_paiement_id", nullable = false)
    private MoyenPaiement modePaiement;

    /** Nullable — null = global/shared cost, not attributable to one market. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Country country;
}
```

- [ ] **Step 2: Write the domain repository port + JPA adapter**

```java
package org.store.plateforme.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.domain.model.DepensePlateforme;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepensePlateformeRepository extends BaseRepository<DepensePlateforme> {

    @Query("""
            SELECT new org.store.plateforme.application.dto.DepensePlateformeResponse(depense)
            FROM DepensePlateforme depense
            WHERE (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:countryId IS NULL OR depense.country.id = :countryId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            ORDER BY depense.dateDepense DESC
            """)
    Page<DepensePlateformeResponse> findResponsesByFilter(
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("countryId") UUID countryId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.plateforme.application.dto.DepensePlateformeTotalResponse(
                COALESCE(SUM(depense.montant), 0),
                COUNT(depense)
            )
            FROM DepensePlateforme depense
            WHERE (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:countryId IS NULL OR depense.country.id = :countryId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            """)
    DepensePlateformeTotalResponse computeTotal(
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("countryId") UUID countryId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /** Simple period+country sum, used by the reporting endpoint (no category/moyen/libelle filters there). */
    @Query("""
            SELECT COALESCE(SUM(depense.montant), 0)
            FROM DepensePlateforme depense
            WHERE (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
              AND (:countryId IS NULL OR depense.country.id = :countryId)
            """)
    BigDecimal sumByPeriod(@Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("countryId") UUID countryId);
}
```

```java
package org.store.plateforme.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.repository.DepensePlateformeRepository;

import java.util.UUID;

public interface DepensePlateformeJpaRepository extends JpaRepository<DepensePlateforme, UUID>, DepensePlateformeRepository {
}
```

- [ ] **Step 3: Write the domain service**

```java
package org.store.plateforme.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.country.domain.model.Country;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.repository.DepensePlateformeRepository;

import java.math.BigDecimal;

@Service
public class DepensePlateformeDomainService extends GlobalService<DepensePlateforme, DepensePlateformeRepository> {
    public DepensePlateformeDomainService(DepensePlateformeRepository repository) {
        super(repository);
    }

    /** Crée et persiste une dépense plateforme après résolution des FK par le service applicatif. */
    public DepensePlateforme create(DepensePlateformeRequest request, CategoryDepensePlateforme category, MoyenPaiement moyen, Country country) {
        DepensePlateforme depense = new DepensePlateforme();
        depense.setCategory(category);
        depense.setLibelle(request.libelle());
        depense.setDescription(request.description());
        depense.setDateDepense(request.dateDepense());
        depense.setMontant(request.montant());
        depense.setModePaiement(moyen);
        depense.setCountry(country);
        return save(depense);
    }

    public Page<DepensePlateformeResponse> findResponsesByFilter(DepensePlateformeFilter filter) {
        return repository.findResponsesByFilter(
                filter.categoryId(), filter.moyenPaiementId(), filter.countryId(),
                filter.libelle(), LikePatternHelper.toLikePattern(filter.libelle()),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter) {
        return repository.computeTotal(
                filter.categoryId(), filter.moyenPaiementId(), filter.countryId(),
                filter.libelle(), LikePatternHelper.toLikePattern(filter.libelle()),
                filter.startDate(), filter.endDate());
    }

    public BigDecimal sumByPeriod(String startDate, String endDate, java.util.UUID countryId) {
        return repository.sumByPeriod(startDate, endDate, countryId);
    }
}
```

- [ ] **Step 4: Write the DTOs**

```java
package org.store.plateforme.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DepensePlateformeRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 200) String libelle,
        @Size(max = 1000) String description,
        @NotNull LocalDate dateDepense,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotNull UUID moyenPaiementId,
        UUID countryId
) {
}
```

```java
package org.store.plateforme.application.dto;

import org.store.common.tools.DateHelper;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.plateforme.domain.model.DepensePlateforme;

import java.math.BigDecimal;
import java.util.UUID;

public record DepensePlateformeResponse(
        UUID id,
        CategoryDepensePlateformeSummaryResponse category,
        String libelle,
        String description,
        String dateDepense,
        BigDecimal montant,
        MoyenPaiementResponse modePaiement,
        CountryResponse country,
        String createdAt
) {
    public DepensePlateformeResponse(DepensePlateforme depense) {
        this(
                depense.getId(),
                depense.getCategory() != null ? new CategoryDepensePlateformeSummaryResponse(depense.getCategory()) : null,
                depense.getLibelle(),
                depense.getDescription(),
                DateHelper.format(depense.getDateDepense()),
                depense.getMontant(),
                depense.getModePaiement() != null ? new MoyenPaiementResponse(depense.getModePaiement()) : null,
                depense.getCountry() != null ? new CountryResponse(depense.getCountry()) : null,
                DateHelper.format(depense.getCreatedAt())
        );
    }
}
```

```java
package org.store.plateforme.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.validation.DatePattern;

import java.util.UUID;

public record DepensePlateformeFilter(
        UUID categoryId,
        UUID moyenPaiementId,
        UUID countryId,
        String libelle,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
```

```java
package org.store.plateforme.application.dto;

import java.math.BigDecimal;

public record DepensePlateformeTotalResponse(
        BigDecimal montantTotal,
        long nombreDepenses
) {
    public DepensePlateformeTotalResponse(BigDecimal montantTotal, Long nombreDepenses) {
        this(
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                nombreDepenses != null ? nombreDepenses : 0L
        );
    }
}
```

- [ ] **Step 5: Write the application service interface + impl**

```java
package org.store.plateforme.application.service;

import org.springframework.data.domain.Page;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface IDepensePlateformeService {

    DepensePlateformeResponse create(DepensePlateformeRequest request);

    DepensePlateformeResponse findResponseById(UUID id);

    Page<DepensePlateformeResponse> findAll(DepensePlateformeFilter filter);

    DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter);

    /** Simple period+country sum — consumed by PlateformeReportingServiceImpl, no category/moyen/libelle filters. */
    BigDecimal computeTotal(String startDate, String endDate, UUID countryId);

    DepensePlateformeResponse update(UUID id, DepensePlateformeRequest request);

    void delete(UUID id);
}
```

```java
package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.application.service.IDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.service.DepensePlateformeDomainService;

import java.math.BigDecimal;
import java.util.UUID;

/** Orchestre le CRUD des dépenses plateforme : résolution FK category/moyen/country et agrégation totale. */
@Service
@Transactional(readOnly = true)
public class DepensePlateformeServiceImpl implements IDepensePlateformeService {

    private final DepensePlateformeDomainService domainService;
    private final ICategoryDepensePlateformeService categoryService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;

    public DepensePlateformeServiceImpl(DepensePlateformeDomainService domainService,
                                        ICategoryDepensePlateformeService categoryService,
                                        IMoyenPaiementService moyenPaiementService,
                                        CountryDomainService countryDomainService,
                                        ValidatorService validatorService) {
        this.domainService = domainService;
        this.categoryService = categoryService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
    }

    @Override
    @Transactional
    public DepensePlateformeResponse create(DepensePlateformeRequest request) {
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;
        return new DepensePlateformeResponse(domainService.create(request, category, moyen, country));
    }

    @Override
    public DepensePlateformeResponse findResponseById(UUID id) {
        return new DepensePlateformeResponse(domainService.findById(id));
    }

    @Override
    public Page<DepensePlateformeResponse> findAll(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.findResponsesByFilter(filter);
    }

    @Override
    public DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.computeTotal(filter);
    }

    @Override
    public BigDecimal computeTotal(String startDate, String endDate, UUID countryId) {
        return domainService.sumByPeriod(startDate, endDate, countryId);
    }

    @Override
    @Transactional
    public DepensePlateformeResponse update(UUID id, DepensePlateformeRequest request) {
        DepensePlateforme depense = domainService.findById(id);
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;

        depense.setCategory(category);
        depense.setLibelle(request.libelle());
        depense.setDescription(request.description());
        depense.setDateDepense(request.dateDepense());
        depense.setMontant(request.montant());
        depense.setModePaiement(moyen);
        depense.setCountry(country);

        return new DepensePlateformeResponse(domainService.save(depense));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }
}
```

- [ ] **Step 6: Write the controller**

```java
package org.store.plateforme.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;

import java.util.UUID;

@RestController
@RequestMapping(DepensePlateformeController.BASE_PATH)
public class DepensePlateformeController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/depenses";

    private final IDepensePlateformeService service;

    public DepensePlateformeController(IDepensePlateformeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CREATE')")
    public ResponseEntity<DepensePlateformeResponse> create(@Valid @RequestBody DepensePlateformeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<Page<DepensePlateformeResponse>> list(@RequestParam(required = false) UUID categoryId,
                                                                @RequestParam(required = false) UUID moyenPaiementId,
                                                                @RequestParam(required = false) UUID countryId,
                                                                @RequestParam(required = false) String libelle,
                                                                @RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findAll(
                new DepensePlateformeFilter(categoryId, moyenPaiementId, countryId, libelle, startDate, endDate, page, size)));
    }

    @GetMapping("/total")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<DepensePlateformeTotalResponse> computeTotal(@RequestParam(required = false) UUID categoryId,
                                                                       @RequestParam(required = false) UUID moyenPaiementId,
                                                                       @RequestParam(required = false) UUID countryId,
                                                                       @RequestParam(required = false) String libelle,
                                                                       @RequestParam(required = false) String startDate,
                                                                       @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(service.computeTotal(
                new DepensePlateformeFilter(categoryId, moyenPaiementId, countryId, libelle, startDate, endDate, 0, 1)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<DepensePlateformeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_UPDATE')")
    public ResponseEntity<DepensePlateformeResponse> update(@PathVariable UUID id,
                                                            @Valid @RequestBody DepensePlateformeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Write the failing tests**

`DepensePlateformeServiceImplTest`:

```java
package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.service.impl.DepensePlateformeServiceImpl;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.service.DepensePlateformeDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepensePlateformeServiceImplTest {

    @Mock private DepensePlateformeDomainService domainService;
    @Mock private ICategoryDepensePlateformeService categoryService;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private CountryDomainService countryDomainService;
    @Mock private ValidatorService validatorService;
    @InjectMocks private DepensePlateformeServiceImpl service;

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID MOYEN_ID = UUID.randomUUID();
    private static final UUID COUNTRY_ID = UUID.randomUUID();

    private CategoryDepensePlateforme category() {
        CategoryDepensePlateforme c = new CategoryDepensePlateforme();
        c.setId(CATEGORY_ID);
        c.setNom("Hébergement");
        return c;
    }

    private MoyenPaiement moyen() {
        MoyenPaiement m = new MoyenPaiement();
        m.setId(MOYEN_ID);
        m.setLibelle("Virement");
        return m;
    }

    private Country country() {
        Country c = new Country();
        c.setId(COUNTRY_ID);
        c.setName("Sénégal");
        c.setCountryCode("SN");
        c.setCurrency("XOF");
        return c;
    }

    @Test
    void create_should_resolve_country_when_countryId_present() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Serveur AWS", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("500000.00"), MOYEN_ID, COUNTRY_ID);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Serveur AWS");
        saved.setMontant(new BigDecimal("500000.00"));
        saved.setModePaiement(moyen());
        saved.setCountry(country());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(countryDomainService.findById(COUNTRY_ID)).thenReturn(country());
        when(domainService.create(eq(request), any(), any(), any())).thenReturn(saved);

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.libelle()).isEqualTo("Serveur AWS");
        assertThat(response.country()).isNotNull();
        assertThat(response.country().countryCode()).isEqualTo("SN");
    }

    @Test
    void create_should_pass_null_country_when_countryId_absent() {
        DepensePlateformeRequest request = new DepensePlateformeRequest(
                CATEGORY_ID, "Outil SaaS global", null, LocalDate.of(2026, 8, 1),
                new BigDecimal("50000.00"), MOYEN_ID, null);

        DepensePlateforme saved = new DepensePlateforme();
        saved.setId(UUID.randomUUID());
        saved.setCategory(category());
        saved.setLibelle("Outil SaaS global");
        saved.setMontant(new BigDecimal("50000.00"));
        saved.setModePaiement(moyen());

        when(categoryService.findById(CATEGORY_ID)).thenReturn(category());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyen());
        when(domainService.create(eq(request), any(), any(), eq(null))).thenReturn(saved);

        DepensePlateformeResponse response = service.create(request);

        assertThat(response.country()).isNull();
    }

    @Test
    void computeTotal_with_period_and_country_should_delegate_to_domainService_sumByPeriod() {
        when(domainService.sumByPeriod("2026-08-01", "2026-08-31", COUNTRY_ID))
                .thenReturn(new BigDecimal("750000.00"));

        BigDecimal total = service.computeTotal("2026-08-01", "2026-08-31", COUNTRY_ID);

        assertThat(total).isEqualByComparingTo("750000.00");
        verify(domainService).sumByPeriod("2026-08-01", "2026-08-31", COUNTRY_ID);
    }

    @Test
    void findAll_should_validate_filter_before_delegating() {
        DepensePlateformeFilter filter = new DepensePlateformeFilter(null, null, null, null, null, null, 0, 10);
        when(domainService.findResponsesByFilter(filter)).thenReturn(org.springframework.data.domain.Page.empty());

        service.findAll(filter);

        verify(validatorService).validate(filter);
    }
}
```

`DepensePlateformeControllerTest` (2 cases — happy path create + total endpoint, mirrors `DepenseControllerTest`):

```java
package org.store.plateforme.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.plateforme.application.dto.CategoryDepensePlateformeSummaryResponse;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepensePlateformeControllerTest {

    private MockMvc mockMvc;
    private IDepensePlateformeService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID categoryId;
    private UUID moyenId;

    @BeforeEach
    void setUp() {
        service = mock(IDepensePlateformeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DepensePlateformeController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
        categoryId = UUID.randomUUID();
        moyenId = UUID.randomUUID();
    }

    @Test
    void should_return_201_when_depense_created() throws Exception {
        DepensePlateformeRequest body = new DepensePlateformeRequest(categoryId, "Serveur AWS", "desc",
                LocalDate.of(2026, 8, 1), new BigDecimal("500000.00"), moyenId, null);
        DepensePlateformeResponse sample = new DepensePlateformeResponse(
                UUID.randomUUID(),
                new CategoryDepensePlateformeSummaryResponse(categoryId, "Hébergement"),
                "Serveur AWS", "desc", "2026-08-01",
                new BigDecimal("500000.00"),
                new MoyenPaiementResponse(moyenId, "Virement", true),
                null,
                "2026-08-01 10:00:00");
        when(service.create(any(DepensePlateformeRequest.class))).thenReturn(sample);

        mockMvc.perform(post(DepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Serveur AWS"));
    }

    @Test
    void should_return_200_with_total() throws Exception {
        when(service.computeTotal(any())).thenReturn(new DepensePlateformeTotalResponse(new BigDecimal("750000.00"), 3L));

        mockMvc.perform(get(DepensePlateformeController.BASE_PATH + "/total")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTotal").value(750000.00))
                .andExpect(jsonPath("$.nombreDepenses").value(3));
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `./mvnw test -Dtest=DepensePlateformeServiceImplTest,DepensePlateformeControllerTest`
Expected: PASS, 6/6 green.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/org/store/plateforme src/test/java/org/store/plateforme
git commit -m "feat(plateforme): DepensePlateforme CRUD with optional country attribution"
```

---

### Task 5: `Revenu` entity + `RevenuRecordedEvent` + listener + one added line in `validate()`

**Files:**
- Create: `src/main/java/org/store/abonnement/domain/model/Revenu.java`
- Create: `src/main/java/org/store/abonnement/domain/repository/RevenuRepository.java`
- Create: `src/main/java/org/store/abonnement/domain/service/RevenuDomainService.java`
- Create: `src/main/java/org/store/abonnement/infrastructure/repository/RevenuJpaRepository.java`
- Create: `src/main/java/org/store/abonnement/application/service/IRevenuService.java`
- Create: `src/main/java/org/store/abonnement/application/service/impl/RevenuServiceImpl.java`
- Create: `src/main/java/org/store/notification/application/event/RevenuRecordedEvent.java`
- Create: `src/main/java/org/store/abonnement/application/listener/RevenuEventListener.java`
- Modify: `src/main/java/org/store/abonnement/application/service/impl/PaiementAbonnementServiceImpl.java` (exactly one new line, in `validate()`)
- Test: `src/test/java/org/store/abonnement/application/service/RevenuServiceImplTest.java`
- Test: `src/test/java/org/store/abonnement/application/listener/RevenuEventListenerTest.java`
- Test: `src/test/java/org/store/abonnement/application/service/PaiementAbonnementServiceImplTest.java` (add one assertion to the existing `validate_should_...` test — file already exists, find it via `find src/test -iname PaiementAbonnementServiceImplTest.java`)

**Interfaces:**
- Produces: `IRevenuService.getTotalForPeriod(RevenuPeriodFilter filter) → BigDecimal` — consumed by Task 6 (`PlateformeReportingServiceImpl`). **Corrected post-review** (ledger: rule 30 max-3-params) from the originally-drafted 4-raw-param signature — see the note after Step 5 below for the exact corrected shape.
- Consumes: `IAbonnementService.findById(UUID) → Abonnement` (existing, already confirmed to return the raw entity with `.getEntreprise()`).

> **Post-implementation correction (ledger ruling, applied via fix round 1):** the code below (Steps 2, 3, 5, 6, 8) originally used raw 4-parameter method signatures on `RevenuDomainService.sumByPeriod`, `IRevenuService.record`, and `IRevenuService.getTotalForPeriod`, and a single-letter JPQL alias (`r`) on `RevenuRepository.sumByPeriod`. Task review caught both as violations of `BACKEND_CODING_CONVENTIONS.md` rules 30 (max 3 params — no exemption for domain/application services, only Spring Data repositories) and 32 (explicit JPQL alias names, no exemption). The shipped code introduces two new records in `org.store.abonnement.application.dto`: `RevenuRecordCommand(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant)` for `record()`, and `RevenuPeriodFilter(String startDate, String endDate, UUID countryId, UUID abonnementId)` for `getTotalForPeriod()`. `RevenuDomainService.sumByPeriod` takes `(RevenuPeriodFilter filter, UUID resolvedEntrepriseId)` — 2 params, reusing the filter's startDate/endDate/countryId and ignoring its abonnementId (already resolved by the caller). `RevenuRepository.sumByPeriod` keeps its 4 `@Param`s (repository exemption applies) but its JPQL alias is `revenu`, not `r`. The code blocks below are left as originally drafted for historical reference; **Task 6 below has been updated to call the corrected signature.**

- [ ] **Step 1: Write the entity**

```java
package org.store.abonnement.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.country.domain.model.Country;
import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row per validated subscription payment — the sole source of truth for platform revenue. */
@Getter
@Setter
@Entity
@Table(name = Revenu.TABLE_NAME)
public class Revenu extends AuditableEntity {
    public static final String TABLE_NAME = "revenu";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Country country;

    private BigDecimal montant;

    private LocalDate datePaiement;
}
```

- [ ] **Step 2: Write the domain repository port + JPA adapter**

```java
package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.domain.model.Revenu;
import org.store.common.repository.BaseRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface RevenuRepository extends BaseRepository<Revenu> {

    @Query("""
            SELECT COALESCE(SUM(r.montant), 0)
            FROM Revenu r
            WHERE (:startDate IS NULL OR :startDate = '' OR r.datePaiement >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR r.datePaiement <= CAST(:endDate AS date))
              AND (:countryId IS NULL OR r.country.id = :countryId)
              AND (:entrepriseId IS NULL OR r.entreprise.id = :entrepriseId)
            """)
    BigDecimal sumByPeriod(@Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("countryId") UUID countryId,
                           @Param("entrepriseId") UUID entrepriseId);
}
```

```java
package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.repository.RevenuRepository;

import java.util.UUID;

public interface RevenuJpaRepository extends JpaRepository<Revenu, UUID>, RevenuRepository {
}
```

- [ ] **Step 3: Write the domain service**

```java
package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.repository.RevenuRepository;
import org.store.common.service.GlobalService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RevenuDomainService extends GlobalService<Revenu, RevenuRepository> {
    public RevenuDomainService(RevenuRepository repository) {
        super(repository);
    }

    public BigDecimal sumByPeriod(String startDate, String endDate, UUID countryId, UUID entrepriseId) {
        return repository.sumByPeriod(startDate, endDate, countryId, entrepriseId);
    }
}
```

- [ ] **Step 4: Write the event record**

```java
package org.store.notification.application.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fired once a subscription payment reaches VALIDE. Carries only primitive ids —
 * never the Entreprise entity — because this event is consumed by an @Async
 * listener where the originating Hibernate session/lazy proxies are gone
 * (same class of bug already fixed once on PaiementAbonnementValidatedEvent).
 */
public record RevenuRecordedEvent(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant) {}
```

- [ ] **Step 5: Write `IRevenuService` + impl**

```java
package org.store.abonnement.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface IRevenuService {

    /** Persists one Revenu row. Called only from RevenuEventListener, on a validated payment. */
    void record(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant);

    BigDecimal getTotalForPeriod(String startDate, String endDate, UUID countryId, UUID abonnementId);
}
```

```java
package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IRevenuService;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Persists validated-payment revenue rows and aggregates them for the platform P&L reporting endpoint. */
@Service
@Transactional(readOnly = true)
public class RevenuServiceImpl implements IRevenuService {

    private final RevenuDomainService revenuDomainService;
    private final IEntrepriseService entrepriseService;
    private final CountryDomainService countryDomainService;
    private final IAbonnementService abonnementService;

    public RevenuServiceImpl(RevenuDomainService revenuDomainService,
                             IEntrepriseService entrepriseService,
                             CountryDomainService countryDomainService,
                             IAbonnementService abonnementService) {
        this.revenuDomainService = revenuDomainService;
        this.entrepriseService = entrepriseService;
        this.countryDomainService = countryDomainService;
        this.abonnementService = abonnementService;
    }

    /** Builds the Revenu row from ids only — both FKs resolved via cheap, fresh PK lookups (not stale proxies). */
    @Override
    @Transactional
    public void record(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant) {
        Revenu revenu = new Revenu();
        revenu.setEntreprise(entrepriseService.findById(entrepriseId));
        revenu.setCountry(countryDomainService.findById(countryId));
        revenu.setDatePaiement(datePaiement);
        revenu.setMontant(montant);
        revenuDomainService.save(revenu);
    }

    /** When abonnementId is set, resolves it to the owning entreprise (Abonnement is 1:1 per entreprise) before querying. */
    @Override
    public BigDecimal getTotalForPeriod(String startDate, String endDate, UUID countryId, UUID abonnementId) {
        UUID entrepriseId = abonnementId != null
                ? abonnementService.findById(abonnementId).getEntreprise().getId()
                : null;
        return revenuDomainService.sumByPeriod(startDate, endDate, countryId, entrepriseId);
    }
}
```

- [ ] **Step 6: Write the listener**

```java
package org.store.abonnement.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.abonnement.application.service.IRevenuService;
import org.store.notification.application.event.RevenuRecordedEvent;

/** Subscribes to RevenuRecordedEvent (fired from PaiementAbonnementServiceImpl.validate()) and persists the Revenu row. */
@Component
public class RevenuEventListener {

    private final IRevenuService revenuService;

    public RevenuEventListener(IRevenuService revenuService) {
        this.revenuService = revenuService;
    }

    @Async
    @EventListener
    public void onRevenuRecorded(RevenuRecordedEvent event) {
        revenuService.record(event.entrepriseId(), event.countryId(), event.datePaiement(), event.montant());
    }
}
```

- [ ] **Step 7: Add the one line to `PaiementAbonnementServiceImpl.validate()`**

In `src/main/java/org/store/abonnement/application/service/impl/PaiementAbonnementServiceImpl.java`, add the import:

```java
import org.store.notification.application.event.RevenuRecordedEvent;
```

Then, in `validate()`, right after the existing `notificationEventPublisher.publishPaiementValidated(...)` call (currently the last statement before `UserPrincipal caller = ...`), insert:

```java
notificationEventPublisher.publishEvent(new RevenuRecordedEvent(
        entrepriseId,
        abonnement.getEntreprise().getCountry().getId(),
        validatedPaiement.getDatePaiement(),
        validatedPaiement.getMontantFinal()));
```

`entrepriseId` is already a local variable in that method (`UUID entrepriseId = abonnement.getEntreprise().getId();`, declared 3 lines above) — reuse it, don't call `.getEntreprise().getId()` twice.

- [ ] **Step 8: Write the failing tests**

`RevenuServiceImplTest`:

```java
package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.impl.RevenuServiceImpl;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenuServiceImplTest {

    @Mock private RevenuDomainService revenuDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private CountryDomainService countryDomainService;
    @Mock private IAbonnementService abonnementService;
    @InjectMocks private RevenuServiceImpl service;

    @Test
    void record_should_resolve_entreprise_and_country_then_save() {
        UUID entrepriseId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Country country = new Country();
        country.setId(countryId);

        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(countryDomainService.findById(countryId)).thenReturn(country);

        service.record(entrepriseId, countryId, LocalDate.of(2026, 8, 15), new BigDecimal("15000.00"));

        ArgumentCaptor<Revenu> captor = ArgumentCaptor.forClass(Revenu.class);
        verify(revenuDomainService).save(captor.capture());
        assertThat(captor.getValue().getEntreprise()).isEqualTo(entreprise);
        assertThat(captor.getValue().getCountry()).isEqualTo(country);
        assertThat(captor.getValue().getMontant()).isEqualByComparingTo("15000.00");
    }

    @Test
    void getTotalForPeriod_should_resolve_abonnementId_to_entrepriseId() {
        UUID abonnementId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);

        when(abonnementService.findById(abonnementId)).thenReturn(abonnement);
        when(revenuDomainService.sumByPeriod("2026-08-01", "2026-08-31", null, entrepriseId))
                .thenReturn(new BigDecimal("300000.00"));

        BigDecimal total = service.getTotalForPeriod("2026-08-01", "2026-08-31", null, abonnementId);

        assertThat(total).isEqualByComparingTo("300000.00");
    }

    @Test
    void getTotalForPeriod_should_pass_null_entrepriseId_when_abonnementId_absent() {
        when(revenuDomainService.sumByPeriod("2026-08-01", "2026-08-31", null, null))
                .thenReturn(new BigDecimal("450000.00"));

        BigDecimal total = service.getTotalForPeriod("2026-08-01", "2026-08-31", null, null);

        assertThat(total).isEqualByComparingTo("450000.00");
        verifyNoInteractions(abonnementService);
    }
}
```

`RevenuEventListenerTest`:

```java
package org.store.abonnement.application.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.IRevenuService;
import org.store.notification.application.event.RevenuRecordedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevenuEventListenerTest {

    @Mock private IRevenuService revenuService;
    @InjectMocks private RevenuEventListener listener;

    @Test
    void onRevenuRecorded_should_delegate_to_revenuService_record() {
        UUID entrepriseId = UUID.randomUUID();
        UUID countryId = UUID.randomUUID();
        LocalDate datePaiement = LocalDate.of(2026, 8, 15);
        BigDecimal montant = new BigDecimal("15000.00");

        listener.onRevenuRecorded(new RevenuRecordedEvent(entrepriseId, countryId, datePaiement, montant));

        verify(revenuService).record(entrepriseId, countryId, datePaiement, montant);
    }
}
```

For `PaiementAbonnementServiceImplTest`: locate the existing `validate_should_...` test(s) via `grep -n "void validate" src/test/java/org/store/abonnement/application/service/PaiementAbonnementServiceImplTest.java`, and add this assertion at the end of that test method (alongside the existing `verify(notificationEventPublisher).publishPaiementValidated(...)` line, if present — do not remove any existing assertion):

```java
verify(notificationEventPublisher).publishEvent(any(RevenuRecordedEvent.class));
```

Add the import `import org.store.notification.application.event.RevenuRecordedEvent;` and `import static org.mockito.ArgumentMatchers.any;` if not already present in that test file.

- [ ] **Step 9: Run tests to verify they pass**

Run: `./mvnw test -Dtest=RevenuServiceImplTest,RevenuEventListenerTest,PaiementAbonnementServiceImplTest`
Expected: PASS — 3 new tests green, all pre-existing `PaiementAbonnementServiceImplTest` cases still green.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/org/store/abonnement/domain/model/Revenu.java src/main/java/org/store/abonnement/domain/repository/RevenuRepository.java src/main/java/org/store/abonnement/domain/service/RevenuDomainService.java src/main/java/org/store/abonnement/infrastructure/repository/RevenuJpaRepository.java src/main/java/org/store/abonnement/application/service/IRevenuService.java src/main/java/org/store/abonnement/application/service/impl/RevenuServiceImpl.java src/main/java/org/store/notification/application/event/RevenuRecordedEvent.java src/main/java/org/store/abonnement/application/listener/RevenuEventListener.java src/main/java/org/store/abonnement/application/service/impl/PaiementAbonnementServiceImpl.java src/test/java/org/store/abonnement/application/service/RevenuServiceImplTest.java src/test/java/org/store/abonnement/application/listener/RevenuEventListenerTest.java src/test/java/org/store/abonnement/application/service/PaiementAbonnementServiceImplTest.java
git commit -m "feat(abonnement): Revenu entity fed by a new event on payment validation, zero existing-behavior change"
```

---

### Task 6: `PlateformeReportingController` — the P&L endpoint

**Files:**
- Create: `src/main/java/org/store/plateforme/application/dto/PlateformePeriodFilter.java`
- Create: `src/main/java/org/store/plateforme/application/dto/PlateformePeriodReportResponse.java`
- Create: `src/main/java/org/store/plateforme/application/service/IPlateformeReportingService.java`
- Create: `src/main/java/org/store/plateforme/application/service/impl/PlateformeReportingServiceImpl.java`
- Create: `src/main/java/org/store/plateforme/presentation/PlateformeReportingController.java`
- Test: `src/test/java/org/store/plateforme/application/service/PlateformeReportingServiceImplTest.java`
- Test: `src/test/java/org/store/plateforme/presentation/PlateformeReportingControllerTest.java`

**Interfaces:**
- Consumes: `IRevenuService.getTotalForPeriod(RevenuPeriodFilter filter)` (Task 5, corrected signature — `RevenuPeriodFilter` lives in `org.store.abonnement.application.dto`), `IDepensePlateformeService.computeTotal(String, String, UUID)` (Task 4).
- Endpoint: `GET /api/v1/admin/plateforme/reporting/period?startDate=&endDate=&countryId=&abonnementId=`.

- [ ] **Step 1: Write the DTOs**

```java
package org.store.plateforme.application.dto;

import java.util.UUID;

public record PlateformePeriodFilter(String startDate, String endDate, UUID countryId, UUID abonnementId) {}
```

```java
package org.store.plateforme.application.dto;

import java.math.BigDecimal;

public record PlateformePeriodReportResponse(BigDecimal revenu, BigDecimal depensesPlateforme, BigDecimal benefice) {}
```

- [ ] **Step 2: Write the service interface + impl**

```java
package org.store.plateforme.application.service;

import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;

public interface IPlateformeReportingService {
    PlateformePeriodReportResponse getPeriodReport(PlateformePeriodFilter filter);
}
```

```java
package org.store.plateforme.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.service.IRevenuService;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.math.BigDecimal;

/**
 * Computes the platform P&L for a period: revenu (Revenu table, scoped by country + abonnement),
 * dépenses plateforme (scoped by country only), bénéfice = revenu − dépenses, mirroring whatever
 * country scoping the two operands share.
 */
@Service
@Transactional(readOnly = true)
public class PlateformeReportingServiceImpl implements IPlateformeReportingService {

    private final IRevenuService revenuService;
    private final IDepensePlateformeService depensePlateformeService;

    public PlateformeReportingServiceImpl(IRevenuService revenuService, IDepensePlateformeService depensePlateformeService) {
        this.revenuService = revenuService;
        this.depensePlateformeService = depensePlateformeService;
    }

    @Override
    public PlateformePeriodReportResponse getPeriodReport(PlateformePeriodFilter filter) {
        RevenuPeriodFilter revenuFilter = new RevenuPeriodFilter(filter.startDate(), filter.endDate(), filter.countryId(), filter.abonnementId());
        BigDecimal revenu = revenuService.getTotalForPeriod(revenuFilter);
        BigDecimal depensesPlateforme = depensePlateformeService.computeTotal(filter.startDate(), filter.endDate(), filter.countryId());
        return new PlateformePeriodReportResponse(revenu, depensesPlateforme, revenu.subtract(depensesPlateforme));
    }
}
```

- [ ] **Step 3: Write the controller**

```java
package org.store.plateforme.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.util.UUID;

@RestController
@RequestMapping(PlateformeReportingController.BASE_PATH)
public class PlateformeReportingController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/reporting";

    private final IPlateformeReportingService service;

    public PlateformeReportingController(IPlateformeReportingService service) {
        this.service = service;
    }

    @GetMapping("/period")
    @PreAuthorize("hasAuthority('PLATFORM_REPORT_READ')")
    public ResponseEntity<PlateformePeriodReportResponse> period(@RequestParam(required = false) String startDate,
                                                                  @RequestParam(required = false) String endDate,
                                                                  @RequestParam(required = false) UUID countryId,
                                                                  @RequestParam(required = false) UUID abonnementId) {
        return ResponseEntity.ok(service.getPeriodReport(new PlateformePeriodFilter(startDate, endDate, countryId, abonnementId)));
    }
}
```

- [ ] **Step 4: Write the failing tests**

`PlateformeReportingServiceImplTest`:

```java
package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.service.IRevenuService;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.impl.PlateformeReportingServiceImpl;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateformeReportingServiceImplTest {

    @Mock private IRevenuService revenuService;
    @Mock private IDepensePlateformeService depensePlateformeService;
    @InjectMocks private PlateformeReportingServiceImpl service;

    @Test
    void getPeriodReport_should_compute_benefice_as_revenu_minus_depenses_globally() {
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", null, null);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", null, null))).thenReturn(new BigDecimal("1000000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", null)).thenReturn(new BigDecimal("300000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.revenu()).isEqualByComparingTo("1000000.00");
        assertThat(response.depensesPlateforme()).isEqualByComparingTo("300000.00");
        assertThat(response.benefice()).isEqualByComparingTo("700000.00");
    }

    @Test
    void getPeriodReport_should_scope_both_operands_by_the_same_countryId() {
        UUID countryId = UUID.randomUUID();
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", countryId, null);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", countryId, null))).thenReturn(new BigDecimal("400000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", countryId)).thenReturn(new BigDecimal("100000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.benefice()).isEqualByComparingTo("300000.00");
    }

    @Test
    void getPeriodReport_with_abonnementId_should_leave_depenses_unaffected() {
        UUID abonnementId = UUID.randomUUID();
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", null, abonnementId);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", null, abonnementId))).thenReturn(new BigDecimal("50000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", null)).thenReturn(new BigDecimal("300000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.depensesPlateforme()).isEqualByComparingTo("300000.00");
        assertThat(response.benefice()).isEqualByComparingTo("-250000.00");
    }
}
```

`PlateformeReportingControllerTest`:

```java
package org.store.plateforme.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlateformeReportingControllerTest {

    private MockMvc mockMvc;
    private IPlateformeReportingService service;

    @BeforeEach
    void setUp() {
        service = mock(IPlateformeReportingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlateformeReportingController(service)).build();
    }

    @Test
    void should_return_200_with_period_report() throws Exception {
        when(service.getPeriodReport(any()))
                .thenReturn(new PlateformePeriodReportResponse(new BigDecimal("1000000.00"), new BigDecimal("300000.00"), new BigDecimal("700000.00")));

        mockMvc.perform(get(PlateformeReportingController.BASE_PATH + "/period")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenu").value(1000000.00))
                .andExpect(jsonPath("$.benefice").value(700000.00));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./mvnw test -Dtest=PlateformeReportingServiceImplTest,PlateformeReportingControllerTest`
Expected: PASS, 4/4 green.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/store/plateforme/application/dto/PlateformePeriodFilter.java src/main/java/org/store/plateforme/application/dto/PlateformePeriodReportResponse.java src/main/java/org/store/plateforme/application/service/IPlateformeReportingService.java src/main/java/org/store/plateforme/application/service/impl/PlateformeReportingServiceImpl.java src/main/java/org/store/plateforme/presentation/PlateformeReportingController.java src/test/java/org/store/plateforme/application/service/PlateformeReportingServiceImplTest.java src/test/java/org/store/plateforme/presentation/PlateformeReportingControllerTest.java
git commit -m "feat(plateforme): PlateformeReportingController — revenu/depenses/benefice period endpoint"
```

---

### Task 7: Full backend suite — verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full suite**

Run: `./mvnw test`
Expected: BUILD SUCCESS, all tests green including every pre-existing test class (confirms zero regression on `AdminReportingControllerTest`, `AdminReportingServiceImplTest`, `PaiementAbonnementServiceImplTest`, `PaiementAbonnementControllerTest`, `RolesPermissionsSyncServiceImplTest`).

- [ ] **Step 2: If anything fails, fix and re-run** — do not proceed to Partie B until this is green.

- [ ] **Step 3: No commit for this task** — it's a verification checkpoint, not a code change.

---

## Partie B — Frontend

All paths below are relative to `store-frontend/`.

### Task 8: Shared period selector — add `quarter` and `year` presets

**Files:**
- Modify: `src/common/tools/dateHelpers.ts`
- Modify: `src/common/presentation/shared/period-selector-props.ts`
- Modify: `src/messages/fr.json` (`common.periodSelector`)
- Modify: `src/messages/en.json` (`common.periodSelector`)
- Test: `src/test/common/tools/dateHelpers.test.ts` (new file)

**Interfaces:**
- Produces: `ReportPeriod` type now includes `'quarter' | 'year'`; `getDateRange('quarter'|'year', ...)` — consumed by Task 11's reporting page.

This is a shared, additive change — every existing consumer (`PeriodTab`, `VentesReportingPage`, `ReportingMainPage`, `DepenseReportingPage`) picks it up automatically with no code change on their side, since they all just render whatever `PERIODS` contains and call `getDateRange` with whatever `period` value they hold.

- [ ] **Step 1: Write the failing test**

```typescript
import { describe, expect, it, vi, afterEach } from 'vitest'

import { getDateRange } from '@/common/tools/dateHelpers'

describe('getDateRange', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('quarter returns the 1st day of the current calendar quarter through today', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 7, 15)) // August 15, 2026 — Q3

    const { from, to } = getDateRange('quarter', '', '')

    expect(from).toBe('2026-07-01')
    expect(to).toBe('2026-08-15')
  })

  it('quarter handles the first month of a quarter correctly', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 0, 5)) // January 5, 2026 — Q1

    const { from, to } = getDateRange('quarter', '', '')

    expect(from).toBe('2026-01-01')
    expect(to).toBe('2026-01-05')
  })

  it('year returns January 1st of the current year through today', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 7, 15))

    const { from, to } = getDateRange('year', '', '')

    expect(from).toBe('2026-01-01')
    expect(to).toBe('2026-08-15')
  })
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `npx vitest run src/test/common/tools/dateHelpers.test.ts`
Expected: FAIL — `getDateRange` doesn't yet handle `'quarter'`/`'year'`, falls through to the `custom` branch and returns `{ from: '', to: '' }` (empty strings, since `customFrom`/`customTo` are both `''` in the test).

- [ ] **Step 3: Implement**

In `src/common/tools/dateHelpers.ts`, change the type on line 16 and add two branches to `getDateRange` right after the existing `month` branch (before the `// custom` fallback comment):

```typescript
export type ReportPeriod = 'yesterday' | 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'
```

```typescript
  if (period === 'quarter') {
    const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3
    const first = new Date(now.getFullYear(), quarterStartMonth, 1)
    return { from: formatDateISO(first), to: formatDateISO(now) }
  }

  if (period === 'year') {
    const first = new Date(now.getFullYear(), 0, 1)
    return { from: formatDateISO(first), to: formatDateISO(now) }
  }
```

Also update the function's doc comment to list the two new branches, mirroring the existing `- month : ...` line style.

- [ ] **Step 4: Run the test to verify it passes**

Run: `npx vitest run src/test/common/tools/dateHelpers.test.ts`
Expected: PASS, 3/3 green.

- [ ] **Step 5: Add the presets to `PERIODS` and i18n**

In `src/common/presentation/shared/period-selector-props.ts`, insert two entries between `month` and `custom`:

```typescript
export const PERIODS: { key: ReportPeriod; labelKey: string }[] = [
  { key: 'yesterday', labelKey: 'yesterday' },
  { key: 'today',     labelKey: 'today'     },
  { key: 'week',      labelKey: 'week'      },
  { key: 'month',     labelKey: 'month'     },
  { key: 'quarter',   labelKey: 'quarter'   },
  { key: 'year',      labelKey: 'year'      },
  { key: 'custom',    labelKey: 'custom'    },
]
```

In `src/messages/fr.json`, inside `common.periodSelector` (found via `grep -n '"periodSelector"' src/messages/fr.json`), add two keys right after `"month": "Ce mois",`:

```json
      "quarter": "Ce trimestre",
      "year": "Cette année",
```

In `src/messages/en.json`, same spot, after `"month": "This month",`:

```json
      "quarter": "This quarter",
      "year": "This year",
```

- [ ] **Step 6: Run the full frontend suite + typecheck to confirm no regression**

Run: `npx vitest run && npx tsc --noEmit`
Expected: both clean — this change is purely additive to a shared file, no existing test should break.

- [ ] **Step 7: Commit**

```bash
git add src/common/tools/dateHelpers.ts src/common/presentation/shared/period-selector-props.ts src/messages/fr.json src/messages/en.json src/test/common/tools/dateHelpers.test.ts
git commit -m "feat(reporting): add quarter/year presets to the shared period selector"
```

---

### Task 9: `features/plateforme-depense/` — DTOs, repository ports, api adapters, hooks

**Files:**
- Create: `src/features/plateforme-depense/domain/dtos/category-depense-plateforme.ts`
- Create: `src/features/plateforme-depense/domain/dtos/category-depense-plateforme-request.ts`
- Create: `src/features/plateforme-depense/domain/dtos/category-depense-plateforme-summary.ts`
- Create: `src/features/plateforme-depense/domain/dtos/category-depense-plateforme-filter.ts`
- Create: `src/features/plateforme-depense/domain/dtos/depense-plateforme.ts`
- Create: `src/features/plateforme-depense/domain/dtos/depense-plateforme-request.ts`
- Create: `src/features/plateforme-depense/domain/dtos/depense-plateforme-filter.ts`
- Create: `src/features/plateforme-depense/domain/dtos/depense-plateforme-total.ts`
- Create: `src/features/plateforme-depense/domain/dtos/plateforme-period-filter.ts`
- Create: `src/features/plateforme-depense/domain/dtos/plateforme-period-report.ts`
- Create: `src/features/plateforme-depense/domain/category-depense-plateforme-repository.ts`
- Create: `src/features/plateforme-depense/domain/depense-plateforme-repository.ts`
- Create: `src/features/plateforme-depense/infrastructure/category-depense-plateforme-api.ts`
- Create: `src/features/plateforme-depense/infrastructure/depense-plateforme-api.ts`
- Create: `src/features/plateforme-depense/infrastructure/plateforme-reporting-api.ts`
- Create: `src/features/plateforme-depense/application/plateforme-depense-query-keys.ts`
- Create: `src/features/plateforme-depense/application/use-category-depense-plateforme-mutation.ts`
- Create: `src/features/plateforme-depense/application/useCategoryDepensePlateformeList.ts`
- Create: `src/features/plateforme-depense/application/useCreateCategoryDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/useUpdateCategoryDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/useDeleteCategoryDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/use-depense-plateforme-mutation.ts`
- Create: `src/features/plateforme-depense/application/useDepensePlateformePage.ts`
- Create: `src/features/plateforme-depense/application/useDepensePlateformeTotal.ts`
- Create: `src/features/plateforme-depense/application/useCreateDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/useUpdateDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/useDeleteDepensePlateforme.ts`
- Create: `src/features/plateforme-depense/application/usePlateformePeriodReport.ts`
- Test: `src/test/features/plateforme-depense/infrastructure/depense-plateforme-api.test.ts`
- Test: `src/test/features/plateforme-depense/infrastructure/plateforme-reporting-api.test.ts`

**Interfaces:**
- Consumes: `Country` type + `useCountries()` from `@/features/country` (existing, reused as-is), `MoyenPaiementResponse` type + `useMoyenPaiementList()` from `@/features/moyen-paiement` (existing, reused as-is), `PageResponse<T>` from `@/common/domain/dtos/page-response` (existing).
- Produces: everything below — consumed by Task 10 (CRUD page) and Task 11 (reporting page).

- [ ] **Step 1: Write the DTOs**

```typescript
// domain/dtos/category-depense-plateforme.ts
export type CategoryDepensePlateforme = {
  id: string
  nom: string
  description?: string | null
  actif: boolean
}
```

```typescript
// domain/dtos/category-depense-plateforme-request.ts
export type CategoryDepensePlateformeRequest = {
  nom: string
  description?: string | null
  actif?: boolean | null
}
```

```typescript
// domain/dtos/category-depense-plateforme-summary.ts
export type CategoryDepensePlateformeSummary = {
  id: string
  nom: string
}
```

```typescript
// domain/dtos/category-depense-plateforme-filter.ts
export type CategoryDepensePlateformeFilter = {
  nom?: string
  actif?: boolean
  startDate?: string
  endDate?: string
  page: number
  size: number
}
```

```typescript
// domain/dtos/depense-plateforme.ts
import type { MoyenPaiementResponse } from '@/features/moyen-paiement/domain/dtos/moyen-paiement-response'
import type { Country } from '@/features/country/domain/dtos/country'
import type { CategoryDepensePlateformeSummary } from './category-depense-plateforme-summary'

export type DepensePlateforme = {
  id: string
  category: CategoryDepensePlateformeSummary
  libelle: string
  description?: string | null
  dateDepense: string
  montant: number
  modePaiement: MoyenPaiementResponse
  country: Country | null
  createdAt: string
}
```

```typescript
// domain/dtos/depense-plateforme-request.ts
export type DepensePlateformeRequest = {
  categoryId: string
  libelle: string
  description?: string | null
  dateDepense: string
  montant: number
  moyenPaiementId: string
  countryId?: string | null
}
```

```typescript
// domain/dtos/depense-plateforme-filter.ts
export type DepensePlateformeFilter = {
  categoryId?: string
  moyenPaiementId?: string
  countryId?: string
  libelle?: string
  startDate?: string
  endDate?: string
  page: number
  size: number
}
```

```typescript
// domain/dtos/depense-plateforme-total.ts
export type DepensePlateformeTotal = {
  montantTotal: number
  nombreDepenses: number
}
```

```typescript
// domain/dtos/plateforme-period-filter.ts
export type PlateformePeriodFilter = {
  startDate: string
  endDate: string
  countryId?: string
  abonnementId?: string
}
```

```typescript
// domain/dtos/plateforme-period-report.ts
export type PlateformePeriodReport = {
  revenu: number
  depensesPlateforme: number
  benefice: number
}
```

- [ ] **Step 2: Write the repository ports**

```typescript
// domain/category-depense-plateforme-repository.ts
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { CategoryDepensePlateforme } from './dtos/category-depense-plateforme'
import type { CategoryDepensePlateformeFilter } from './dtos/category-depense-plateforme-filter'
import type { CategoryDepensePlateformeRequest } from './dtos/category-depense-plateforme-request'

/** Port for `/api/v1/admin/plateforme/expense-categories`. */
export interface ICategoryDepensePlateformeRepository {
  list(filter: CategoryDepensePlateformeFilter): Promise<PageResponse<CategoryDepensePlateforme>>
  findById(id: string): Promise<CategoryDepensePlateforme>
  create(payload: CategoryDepensePlateformeRequest): Promise<CategoryDepensePlateforme>
  update(id: string, payload: CategoryDepensePlateformeRequest): Promise<CategoryDepensePlateforme>
  delete(id: string): Promise<void>
}
```

```typescript
// domain/depense-plateforme-repository.ts
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { DepensePlateforme } from './dtos/depense-plateforme'
import type { DepensePlateformeFilter } from './dtos/depense-plateforme-filter'
import type { DepensePlateformeRequest } from './dtos/depense-plateforme-request'
import type { DepensePlateformeTotal } from './dtos/depense-plateforme-total'

/** Port for `/api/v1/admin/plateforme/depenses`. */
export interface IDepensePlateformeRepository {
  list(filter: DepensePlateformeFilter): Promise<PageResponse<DepensePlateforme>>
  total(filter: DepensePlateformeFilter): Promise<DepensePlateformeTotal>
  findById(id: string): Promise<DepensePlateforme>
  create(payload: DepensePlateformeRequest): Promise<DepensePlateforme>
  update(id: string, payload: DepensePlateformeRequest): Promise<DepensePlateforme>
  delete(id: string): Promise<void>
}
```

- [ ] **Step 3: Write the api adapters**

```typescript
// infrastructure/category-depense-plateforme-api.ts
import { apiClient } from '@/common/infrastructure/api-client'
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'
import type { CategoryDepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-filter'
import type { CategoryDepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-request'
import type { ICategoryDepensePlateformeRepository } from '@/features/plateforme-depense/domain/category-depense-plateforme-repository'

const BASE_PATH = '/api/v1/admin/plateforme/expense-categories'

export const categoryDepensePlateformeApi: ICategoryDepensePlateformeRepository = {
  async list(filter: CategoryDepensePlateformeFilter): Promise<PageResponse<CategoryDepensePlateforme>> {
    const params: Record<string, unknown> = { page: filter.page, size: filter.size }
    if (filter.nom) params.nom = filter.nom
    if (filter.actif !== undefined) params.actif = filter.actif
    if (filter.startDate) params.startDate = filter.startDate
    if (filter.endDate) params.endDate = filter.endDate
    const { data } = await apiClient.get<PageResponse<CategoryDepensePlateforme>>(BASE_PATH, { params })
    return data
  },

  async findById(id: string): Promise<CategoryDepensePlateforme> {
    const { data } = await apiClient.get<CategoryDepensePlateforme>(`${BASE_PATH}/${id}`)
    return data
  },

  async create(payload: CategoryDepensePlateformeRequest): Promise<CategoryDepensePlateforme> {
    const { data } = await apiClient.post<CategoryDepensePlateforme>(BASE_PATH, payload)
    return data
  },

  async update(id: string, payload: CategoryDepensePlateformeRequest): Promise<CategoryDepensePlateforme> {
    const { data } = await apiClient.put<CategoryDepensePlateforme>(`${BASE_PATH}/${id}`, payload)
    return data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`${BASE_PATH}/${id}`)
  },
}
```

```typescript
// infrastructure/depense-plateforme-api.ts
import { apiClient } from '@/common/infrastructure/api-client'
import type { PageResponse } from '@/common/domain/dtos/page-response'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'
import type { DepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-request'
import type { DepensePlateformeTotal } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-total'
import type { IDepensePlateformeRepository } from '@/features/plateforme-depense/domain/depense-plateforme-repository'

const BASE_PATH = '/api/v1/admin/plateforme/depenses'

function buildListParams(filter: DepensePlateformeFilter): Record<string, unknown> {
  const params: Record<string, unknown> = { page: filter.page, size: filter.size }
  if (filter.categoryId) params.categoryId = filter.categoryId
  if (filter.moyenPaiementId) params.moyenPaiementId = filter.moyenPaiementId
  if (filter.countryId) params.countryId = filter.countryId
  if (filter.libelle) params.libelle = filter.libelle
  if (filter.startDate) params.startDate = filter.startDate
  if (filter.endDate) params.endDate = filter.endDate
  return params
}

export const depensePlateformeApi: IDepensePlateformeRepository = {
  async list(filter: DepensePlateformeFilter): Promise<PageResponse<DepensePlateforme>> {
    const { data } = await apiClient.get<PageResponse<DepensePlateforme>>(BASE_PATH, { params: buildListParams(filter) })
    return data
  },

  async total(filter: DepensePlateformeFilter): Promise<DepensePlateformeTotal> {
    const { data } = await apiClient.get<DepensePlateformeTotal>(`${BASE_PATH}/total`, { params: buildListParams(filter) })
    return data
  },

  async findById(id: string): Promise<DepensePlateforme> {
    const { data } = await apiClient.get<DepensePlateforme>(`${BASE_PATH}/${id}`)
    return data
  },

  async create(payload: DepensePlateformeRequest): Promise<DepensePlateforme> {
    const { data } = await apiClient.post<DepensePlateforme>(BASE_PATH, payload)
    return data
  },

  async update(id: string, payload: DepensePlateformeRequest): Promise<DepensePlateforme> {
    const { data } = await apiClient.put<DepensePlateforme>(`${BASE_PATH}/${id}`, payload)
    return data
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`${BASE_PATH}/${id}`)
  },
}
```

```typescript
// infrastructure/plateforme-reporting-api.ts
import { apiClient } from '@/common/infrastructure/api-client'
import type { PlateformePeriodFilter } from '@/features/plateforme-depense/domain/dtos/plateforme-period-filter'
import type { PlateformePeriodReport } from '@/features/plateforme-depense/domain/dtos/plateforme-period-report'

const BASE_PATH = '/api/v1/admin/plateforme/reporting'

export async function fetchPlateformePeriodReport(filter: PlateformePeriodFilter): Promise<PlateformePeriodReport> {
  const params: Record<string, unknown> = { startDate: filter.startDate, endDate: filter.endDate }
  if (filter.countryId) params.countryId = filter.countryId
  if (filter.abonnementId) params.abonnementId = filter.abonnementId
  const { data } = await apiClient.get<PlateformePeriodReport>(`${BASE_PATH}/period`, { params })
  return data
}
```

- [ ] **Step 4: Write the query-keys factory**

```typescript
// application/plateforme-depense-query-keys.ts
import type { CategoryDepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-filter'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'
import type { PlateformePeriodFilter } from '@/features/plateforme-depense/domain/dtos/plateforme-period-filter'

export const plateformeDepenseKeys = {
  all: ['plateforme-depenses'] as const,
  lists: () => [...plateformeDepenseKeys.all, 'list'] as const,
  list: (filter: DepensePlateformeFilter) => [...plateformeDepenseKeys.lists(), filter] as const,
  totals: () => [...plateformeDepenseKeys.all, 'total'] as const,
  total: (filter: DepensePlateformeFilter) => [...plateformeDepenseKeys.totals(), filter] as const,
  categories: () => [...plateformeDepenseKeys.all, 'categories'] as const,
  categoryList: (filter: CategoryDepensePlateformeFilter) => [...plateformeDepenseKeys.categories(), filter] as const,
  reporting: () => [...plateformeDepenseKeys.all, 'reporting'] as const,
  periodReport: (filter: PlateformePeriodFilter) => [...plateformeDepenseKeys.reporting(), filter] as const,
}
```

- [ ] **Step 5: Write the mutation factories + hooks**

```typescript
// application/use-category-depense-plateforme-mutation.ts
'use client'

import { useMutation, useQueryClient, type UseMutationOptions } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'

export function useCategoryDepensePlateformeMutation<TArg>(
  mutationFn: (arg: TArg) => Promise<CategoryDepensePlateforme | void>,
  options?: Omit<UseMutationOptions<CategoryDepensePlateforme | void, unknown, TArg>, 'mutationFn'>,
) {
  const queryClient = useQueryClient()

  return useMutation<CategoryDepensePlateforme | void, unknown, TArg>({
    mutationFn,
    ...options,
    onSuccess(data, variables, onMutateResult, context) {
      queryClient.invalidateQueries({ queryKey: plateformeDepenseKeys.categories() })
      options?.onSuccess?.(data, variables, onMutateResult, context)
    },
  })
}
```

```typescript
// application/useCategoryDepensePlateformeList.ts
'use client'

import { useQuery } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import { categoryDepensePlateformeApi } from '@/features/plateforme-depense/infrastructure/category-depense-plateforme-api'
import type { CategoryDepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-filter'

const ALL_FILTER: CategoryDepensePlateformeFilter = { page: 0, size: 200 }

export function useCategoryDepensePlateformeList() {
  return useQuery({
    queryKey: plateformeDepenseKeys.categoryList(ALL_FILTER),
    queryFn: () => categoryDepensePlateformeApi.list(ALL_FILTER),
    select: (data) => data.content,
    staleTime: 5 * 60_000,
  })
}
```

```typescript
// application/useCreateCategoryDepensePlateforme.ts
'use client'

import { useCategoryDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-category-depense-plateforme-mutation'
import { categoryDepensePlateformeApi } from '@/features/plateforme-depense/infrastructure/category-depense-plateforme-api'
import type { CategoryDepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-request'

export function useCreateCategoryDepensePlateforme() {
  return useCategoryDepensePlateformeMutation<CategoryDepensePlateformeRequest>((payload) => categoryDepensePlateformeApi.create(payload))
}
```

```typescript
// application/useUpdateCategoryDepensePlateforme.ts
'use client'

import { useCategoryDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-category-depense-plateforme-mutation'
import { categoryDepensePlateformeApi } from '@/features/plateforme-depense/infrastructure/category-depense-plateforme-api'
import type { CategoryDepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme-request'

export function useUpdateCategoryDepensePlateforme() {
  return useCategoryDepensePlateformeMutation<{ id: string; payload: CategoryDepensePlateformeRequest }>(
    ({ id, payload }) => categoryDepensePlateformeApi.update(id, payload),
  )
}
```

```typescript
// application/useDeleteCategoryDepensePlateforme.ts
'use client'

import { useCategoryDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-category-depense-plateforme-mutation'
import { categoryDepensePlateformeApi } from '@/features/plateforme-depense/infrastructure/category-depense-plateforme-api'

export function useDeleteCategoryDepensePlateforme() {
  return useCategoryDepensePlateformeMutation<string>((id) => categoryDepensePlateformeApi.delete(id))
}
```

```typescript
// application/use-depense-plateforme-mutation.ts
'use client'

import { useMutation, useQueryClient, type UseMutationOptions } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'

export function useDepensePlateformeMutation<TArg>(
  mutationFn: (arg: TArg) => Promise<DepensePlateforme | void>,
  options?: Omit<UseMutationOptions<DepensePlateforme | void, unknown, TArg>, 'mutationFn'>,
) {
  const queryClient = useQueryClient()

  return useMutation<DepensePlateforme | void, unknown, TArg>({
    mutationFn,
    ...options,
    onSuccess(data, variables, onMutateResult, context) {
      queryClient.invalidateQueries({ queryKey: plateformeDepenseKeys.lists() })
      queryClient.invalidateQueries({ queryKey: plateformeDepenseKeys.totals() })
      options?.onSuccess?.(data, variables, onMutateResult, context)
    },
  })
}
```

```typescript
// application/useDepensePlateformePage.ts
'use client'

import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'

export function useDepensePlateformePage(filter: DepensePlateformeFilter, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: plateformeDepenseKeys.list(filter),
    queryFn: () => depensePlateformeApi.list(filter),
    placeholderData: keepPreviousData,
    enabled: options?.enabled ?? true,
  })
}
```

```typescript
// application/useDepensePlateformeTotal.ts
'use client'

import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'

export function useDepensePlateformeTotal(filter: DepensePlateformeFilter, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: plateformeDepenseKeys.total(filter),
    queryFn: () => depensePlateformeApi.total(filter),
    placeholderData: keepPreviousData,
    enabled: options?.enabled ?? true,
  })
}
```

```typescript
// application/useCreateDepensePlateforme.ts
'use client'

import { useDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-depense-plateforme-mutation'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'
import type { DepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-request'

export function useCreateDepensePlateforme() {
  return useDepensePlateformeMutation<DepensePlateformeRequest>((payload) => depensePlateformeApi.create(payload))
}
```

```typescript
// application/useUpdateDepensePlateforme.ts
'use client'

import { useDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-depense-plateforme-mutation'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'
import type { DepensePlateformeRequest } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-request'

export function useUpdateDepensePlateforme() {
  return useDepensePlateformeMutation<{ id: string; payload: DepensePlateformeRequest }>(
    ({ id, payload }) => depensePlateformeApi.update(id, payload),
  )
}
```

```typescript
// application/useDeleteDepensePlateforme.ts
'use client'

import { useDepensePlateformeMutation } from '@/features/plateforme-depense/application/use-depense-plateforme-mutation'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'

export function useDeleteDepensePlateforme() {
  return useDepensePlateformeMutation<string>((id) => depensePlateformeApi.delete(id))
}
```

```typescript
// application/usePlateformePeriodReport.ts
'use client'

import { useQuery } from '@tanstack/react-query'

import { plateformeDepenseKeys } from '@/features/plateforme-depense/application/plateforme-depense-query-keys'
import { fetchPlateformePeriodReport } from '@/features/plateforme-depense/infrastructure/plateforme-reporting-api'
import type { PlateformePeriodFilter } from '@/features/plateforme-depense/domain/dtos/plateforme-period-filter'

export function usePlateformePeriodReport(filter: PlateformePeriodFilter) {
  return useQuery({
    queryKey: plateformeDepenseKeys.periodReport(filter),
    queryFn: () => fetchPlateformePeriodReport(filter),
  })
}
```

- [ ] **Step 6: Write the failing api-adapter tests**

```typescript
// src/test/features/plateforme-depense/infrastructure/depense-plateforme-api.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest'

import { apiClient } from '@/common/infrastructure/api-client'
import { depensePlateformeApi } from '@/features/plateforme-depense/infrastructure/depense-plateforme-api'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'

vi.mock('@/common/infrastructure/api-client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))

describe('depensePlateformeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list omits undefined filter fields from params', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [], totalElements: 0 } })
    const filter: DepensePlateformeFilter = { page: 0, size: 10 }

    await depensePlateformeApi.list(filter)

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/plateforme/depenses', {
      params: { page: 0, size: 10 },
    })
  })

  it('list includes countryId when set', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { content: [], totalElements: 0 } })
    const countryId = 'country-1'

    await depensePlateformeApi.list({ page: 0, size: 10, countryId })

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/plateforme/depenses', {
      params: { page: 0, size: 10, countryId },
    })
  })

  it('total hits the /total sub-path with the same param-building logic', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { montantTotal: 0, nombreDepenses: 0 } })

    await depensePlateformeApi.total({ page: 0, size: 1, startDate: '2026-08-01', endDate: '2026-08-31' })

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/plateforme/depenses/total', {
      params: { page: 0, size: 1, startDate: '2026-08-01', endDate: '2026-08-31' },
    })
  })
})
```

```typescript
// src/test/features/plateforme-depense/infrastructure/plateforme-reporting-api.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest'

import { apiClient } from '@/common/infrastructure/api-client'
import { fetchPlateformePeriodReport } from '@/features/plateforme-depense/infrastructure/plateforme-reporting-api'

vi.mock('@/common/infrastructure/api-client', () => ({
  apiClient: { get: vi.fn() },
}))

describe('fetchPlateformePeriodReport', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('omits countryId/abonnementId when not set', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { revenu: 0, depensesPlateforme: 0, benefice: 0 } })

    await fetchPlateformePeriodReport({ startDate: '2026-08-01', endDate: '2026-08-31' })

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/plateforme/reporting/period', {
      params: { startDate: '2026-08-01', endDate: '2026-08-31' },
    })
  })

  it('includes countryId and abonnementId when set', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ data: { revenu: 0, depensesPlateforme: 0, benefice: 0 } })

    await fetchPlateformePeriodReport({
      startDate: '2026-08-01', endDate: '2026-08-31', countryId: 'c-1', abonnementId: 'a-1',
    })

    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/admin/plateforme/reporting/period', {
      params: { startDate: '2026-08-01', endDate: '2026-08-31', countryId: 'c-1', abonnementId: 'a-1' },
    })
  })
})
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `npx vitest run src/test/features/plateforme-depense`
Expected: PASS, 5/5 green.

- [ ] **Step 8: Run typecheck**

Run: `npx tsc --noEmit`
Expected: clean.

- [ ] **Step 9: Commit**

```bash
git add src/features/plateforme-depense src/test/features/plateforme-depense
git commit -m "feat(plateforme-depense): DDD data layer — dtos, repository ports, api adapters, hooks"
```

---

### Task 10: Admin CRUD page — `/dashboard/administration/depenses`

**Files:**
- Create: `src/features/plateforme-depense/presentation/category-depense-plateforme-form-props.ts`
- Create: `src/features/plateforme-depense/presentation/CategoryDepensePlateformeForm.tsx`
- Create: `src/features/plateforme-depense/presentation/category-depense-plateforme-manager-dialog-props.ts`
- Create: `src/features/plateforme-depense/presentation/CategoryDepensePlateformeManagerDialog.tsx`
- Create: `src/features/plateforme-depense/presentation/depense-plateforme-form-props.ts`
- Create: `src/features/plateforme-depense/presentation/DepensePlateformeForm.tsx`
- Create: `src/features/plateforme-depense/presentation/depense-plateforme-form-dialog-props.ts`
- Create: `src/features/plateforme-depense/presentation/DepensePlateformeFormDialog.tsx`
- Create: `src/features/plateforme-depense/presentation/depense-plateforme-table-props.ts`
- Create: `src/features/plateforme-depense/presentation/DepensePlateformeTable.tsx`
- Create: `src/features/plateforme-depense/presentation/depense-plateforme-filters-props.ts`
- Create: `src/features/plateforme-depense/presentation/DepensePlateformeFilters.tsx`
- Create: `src/app/(dashboard)/dashboard/administration/depenses/DepensesPlateformePage.tsx`
- Create: `src/app/(dashboard)/dashboard/administration/depenses/page.tsx`
- Modify: `src/app/(dashboard)/dashboard/administration/_tabs.ts`
- Modify: `src/messages/fr.json` (new `dashboard.administration.depensesPlateforme` namespace)
- Modify: `src/messages/en.json` (same namespace, EN)

Categories are managed inline via a small manager dialog (flat list, no pagination — reference/config data, same cardinality class as `MoyenPaiement`, mirrors `MoyensPaiementPage`'s pattern). The `DepensePlateforme` listing itself is paginated + filtered with the deferred-search gate (rule 47), mirroring `DepensesPage.tsx` minus the magasin scoping.

**Interfaces:**
- Consumes everything from Task 9, plus `useCountries()` (existing, `@/features/country`), `useMoyenPaiementList()` (existing, `@/features/moyen-paiement`), `usePermission` (existing), `useDeferredSearch` (existing), `runMutationWithToast` (existing), `ConfirmDialog`/`DataTable`/`Combobox`/`Pagination`/`EmptyState`/`LoadingState`/`SearchPromptState`/`DateRangeFilter` (existing shared components, all already used identically by `DepensesPage.tsx`).

- [ ] **Step 1: Write `CategoryDepensePlateformeForm.tsx` + props**

```typescript
// presentation/category-depense-plateforme-form-props.ts
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'

export type CategoryDepensePlateformeFormProps = {
  target?: CategoryDepensePlateforme
  onCancel: () => void
  onSubmitted: () => void
}
```

```typescript
// presentation/CategoryDepensePlateformeForm.tsx
'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useTranslations } from 'next-intl'
import { useId, useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { Input } from '@/common/presentation/ui/input'
import { Label } from '@/common/presentation/ui/label'
import { useCreateCategoryDepensePlateforme } from '@/features/plateforme-depense/application/useCreateCategoryDepensePlateforme'
import { useUpdateCategoryDepensePlateforme } from '@/features/plateforme-depense/application/useUpdateCategoryDepensePlateforme'
import type { CategoryDepensePlateformeFormProps } from './category-depense-plateforme-form-props'

type FormValues = { nom: string; description?: string }

export function CategoryDepensePlateformeForm({ target, onCancel, onSubmitted }: CategoryDepensePlateformeFormProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.categoryForm')
  const tToast = useTranslations('dashboard.administration.depensesPlateforme.toasts')
  const nomId = useId()
  const descriptionId = useId()

  const schema = useMemo(
    () => z.object({
      nom: z.string().min(1, t('validation.nomRequired')).max(100, t('validation.nomMax')),
      description: z.string().max(500, t('validation.descriptionMax')).optional().default(''),
    }),
    [t],
  )

  const isEdit = Boolean(target)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: 'onTouched',
    defaultValues: { nom: target?.nom ?? '', description: target?.description ?? '' },
  })

  const createMutation = useCreateCategoryDepensePlateforme()
  const updateMutation = useUpdateCategoryDepensePlateforme()
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  function handleSubmit(values: FormValues) {
    const payload = { nom: values.nom.trim(), description: values.description?.trim() || null, actif: true }

    if (target) {
      runMutationWithToast(updateMutation, { id: target.id, payload }, { successMessage: tToast('categoryUpdated'), onSuccess: onSubmitted })
    } else {
      runMutationWithToast(createMutation, payload, { successMessage: tToast('categoryCreated'), onSuccess: onSubmitted })
    }
  }

  const nomError = form.formState.errors.nom?.message

  return (
    <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
      <div className="space-y-2">
        <Label htmlFor={nomId} required>{t('fields.nom')}</Label>
        <Input id={nomId} type="text" {...form.register('nom')} aria-invalid={Boolean(nomError)} />
        {nomError ? <p className="text-xs text-destructive">{nomError}</p> : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor={descriptionId}>{t('fields.description')}</Label>
        <Input id={descriptionId} type="text" {...form.register('description')} />
      </div>

      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>{t('cancel')}</Button>
        <Button type="submit" disabled={isSubmitting}>{isSubmitting ? t('submitting') : isEdit ? t('save') : t('create')}</Button>
      </div>
    </form>
  )
}
```

- [ ] **Step 2: Write `CategoryDepensePlateformeManagerDialog.tsx` + props**

```typescript
// presentation/category-depense-plateforme-manager-dialog-props.ts
export type CategoryDepensePlateformeManagerDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}
```

```typescript
// presentation/CategoryDepensePlateformeManagerDialog.tsx
'use client'

import { useTranslations } from 'next-intl'
import { useState } from 'react'
import { Plus } from 'lucide-react'

import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { ConfirmDialog } from '@/common/presentation/shared/ConfirmDialog'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/common/presentation/ui/dialog'
import { EmptyState } from '@/common/presentation/shared/EmptyState'
import { LoadingState } from '@/common/presentation/shared/LoadingState'
import { useCategoryDepensePlateformeList } from '@/features/plateforme-depense/application/useCategoryDepensePlateformeList'
import { useDeleteCategoryDepensePlateforme } from '@/features/plateforme-depense/application/useDeleteCategoryDepensePlateforme'
import { CategoryDepensePlateformeForm } from './CategoryDepensePlateformeForm'
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'
import type { CategoryDepensePlateformeManagerDialogProps } from './category-depense-plateforme-manager-dialog-props'

export function CategoryDepensePlateformeManagerDialog({ open, onOpenChange }: CategoryDepensePlateformeManagerDialogProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.categoryManager')
  const tToast = useTranslations('dashboard.administration.depensesPlateforme.toasts')

  const [formTarget, setFormTarget] = useState<CategoryDepensePlateforme | undefined>(undefined)
  const [formOpen, setFormOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<CategoryDepensePlateforme | undefined>(undefined)

  const { data: categories = [], isLoading } = useCategoryDepensePlateformeList()
  const deleteMutation = useDeleteCategoryDepensePlateforme()

  function openCreate() {
    setFormTarget(undefined)
    setFormOpen(true)
  }

  function openEdit(category: CategoryDepensePlateforme) {
    setFormTarget(category)
    setFormOpen(true)
  }

  function handleDeleteConfirmed() {
    if (!deleteTarget) return
    runMutationWithToast(deleteMutation, deleteTarget.id, {
      successMessage: tToast('categoryDeleted'),
      onSettled: () => setDeleteTarget(undefined),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[95dvh] flex-col sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t('title')}</DialogTitle>
        </DialogHeader>

        {formOpen ? (
          <CategoryDepensePlateformeForm
            target={formTarget}
            onCancel={() => setFormOpen(false)}
            onSubmitted={() => setFormOpen(false)}
          />
        ) : (
          <div className="flex flex-col gap-3">
            <Button type="button" onClick={openCreate} className="self-end">
              <Plus aria-hidden="true" className="size-4" />
              {t('createAction')}
            </Button>

            {isLoading ? (
              <LoadingState rows={3} />
            ) : categories.length === 0 ? (
              <EmptyState title={t('empty.title')} description={t('empty.description')} />
            ) : (
              <ul className="flex flex-col divide-y divide-border">
                {categories.map((category) => (
                  <li key={category.id} className="flex items-center justify-between py-2">
                    <span className="text-sm text-foreground">{category.nom}</span>
                    <div className="flex gap-2">
                      <Button type="button" variant="ghost" size="sm" onClick={() => openEdit(category)}>
                        {t('edit')}
                      </Button>
                      <Button type="button" variant="ghost" size="sm" onClick={() => setDeleteTarget(category)}>
                        {t('delete')}
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <ConfirmDialog
          open={Boolean(deleteTarget)}
          onOpenChange={(nextOpen) => { if (!nextOpen) setDeleteTarget(undefined) }}
          title={t('confirmDelete.title')}
          description={t('confirmDelete.description')}
          confirmLabel={t('confirmDelete.confirm')}
          destructive
          onConfirm={handleDeleteConfirmed}
        />
      </DialogContent>
    </Dialog>
  )
}
```

- [ ] **Step 3: Write `DepensePlateformeForm.tsx` + props (adds `country` on top of the `DepenseForm` template)**

```typescript
// presentation/depense-plateforme-form-props.ts
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'

export type DepensePlateformeFormProps = {
  categories: CategoryDepensePlateforme[]
  target?: DepensePlateforme
  onCancel: () => void
  onSubmitted: () => void
}
```

```typescript
// presentation/DepensePlateformeForm.tsx
'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useTranslations } from 'next-intl'
import { useEffect, useId, useMemo } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'

import { runMutationWithToast } from '@/common/application/mutation-toast'
import { Button } from '@/common/presentation/ui/button'
import { Combobox } from '@/common/presentation/ui/combobox'
import { Input } from '@/common/presentation/ui/input'
import { Label } from '@/common/presentation/ui/label'
import { MoneyInput } from '@/common/presentation/shared/MoneyInput'
import { todayISO } from '@/common/tools/dateHelpers'
import { useCountries } from '@/features/country/application/useCountries'
import { useCreateDepensePlateforme } from '@/features/plateforme-depense/application/useCreateDepensePlateforme'
import { useUpdateDepensePlateforme } from '@/features/plateforme-depense/application/useUpdateDepensePlateforme'
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
import type { DepensePlateformeFormProps } from './depense-plateforme-form-props'

type FormValues = {
  categoryId: string
  libelle: string
  description?: string
  dateDepense: string
  montant: number
  moyenPaiementId: string
  countryId?: string
}

export function DepensePlateformeForm({ categories, target, onCancel, onSubmitted }: DepensePlateformeFormProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.form')
  const tToast = useTranslations('dashboard.administration.depensesPlateforme.toasts')
  const moyensQuery = useMoyenPaiementList()
  const countriesQuery = useCountries()

  const libelleId = useId()
  const descriptionId = useId()
  const dateId = useId()
  const montantId = useId()
  const today = todayISO()

  const schema = useMemo(
    () => z.object({
      categoryId: z.string().min(1, t('validation.categoryRequired')),
      libelle: z.string().min(1, t('validation.libelleRequired')).max(200, t('validation.libelleMax')),
      description: z.string().max(1000, t('validation.descriptionMax')).optional().default(''),
      dateDepense: z.string().min(1, t('validation.dateRequired')),
      montant: z.number({ message: t('validation.montantInvalid') }).positive(t('validation.montantPositive')),
      moyenPaiementId: z.string().min(1, t('validation.modePaiementRequired')),
      countryId: z.string().optional(),
    }),
    [t],
  )

  const isEdit = Boolean(target)

  const defaultValues: FormValues = target
    ? {
        categoryId: target.category?.id ?? '',
        libelle: target.libelle,
        description: target.description ?? '',
        dateDepense: target.dateDepense,
        montant: target.montant,
        moyenPaiementId: target.modePaiement?.id ?? '',
        countryId: target.country?.id ?? '',
      }
    : { categoryId: '', libelle: '', description: '', dateDepense: today, montant: 0, moyenPaiementId: '', countryId: '' }

  const form = useForm<FormValues>({ resolver: zodResolver(schema), mode: 'onTouched', defaultValues })

  useEffect(() => {
    form.reset(defaultValues)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target])

  const createMutation = useCreateDepensePlateforme()
  const updateMutation = useUpdateDepensePlateforme()
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  function handleSubmit(values: FormValues) {
    const payload = {
      categoryId: values.categoryId,
      libelle: values.libelle.trim(),
      description: values.description?.trim() || null,
      dateDepense: values.dateDepense,
      montant: values.montant,
      moyenPaiementId: values.moyenPaiementId,
      countryId: values.countryId || null,
    }

    if (target) {
      runMutationWithToast(updateMutation, { id: target.id, payload }, { successMessage: tToast('updated'), onSuccess: onSubmitted })
    } else {
      runMutationWithToast(createMutation, payload, { successMessage: tToast('created'), onSuccess: onSubmitted })
    }
  }

  const categoryItems = useMemo(() => categories.map((c) => ({ value: c.id, label: c.nom })), [categories])
  const moyenItems = useMemo(() => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })), [moyensQuery.data])
  const countryItems = useMemo(() => (countriesQuery.data ?? []).map((c) => ({ value: c.id, label: c.name })), [countriesQuery.data])

  const categoryError = form.formState.errors.categoryId?.message
  const libelleError = form.formState.errors.libelle?.message
  const dateError = form.formState.errors.dateDepense?.message
  const montantError = form.formState.errors.montant?.message

  return (
    <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(handleSubmit)}>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-2">
          <Label required>{t('fields.category')}</Label>
          <Combobox
            items={categoryItems}
            value={form.watch('categoryId') || ''}
            onValueChange={(v) => { if (v !== null) form.setValue('categoryId', v, { shouldDirty: true, shouldTouch: true }) }}
            placeholder={t('fields.categoryPlaceholder')}
            ariaLabel={t('fields.category')}
            emptyLabel="—"
          />
          {categoryError ? <p className="text-xs text-destructive">{categoryError}</p> : null}
        </div>

        <div className="space-y-2">
          <Label required>{t('fields.modePaiement')}</Label>
          <Combobox
            items={moyenItems}
            value={form.watch('moyenPaiementId')}
            onValueChange={(v) => { if (v !== null) form.setValue('moyenPaiementId', v, { shouldDirty: true }) }}
            placeholder={t('fields.modePaiementPlaceholder')}
            ariaLabel={t('fields.modePaiement')}
            emptyLabel="—"
          />
        </div>
      </div>

      <div className="space-y-2">
        <Label>{t('fields.country')}</Label>
        <Combobox
          items={countryItems}
          value={form.watch('countryId') || ''}
          onValueChange={(v) => form.setValue('countryId', v ?? '', { shouldDirty: true })}
          placeholder={t('fields.countryPlaceholder')}
          ariaLabel={t('fields.country')}
          emptyLabel={t('fields.countryGlobal')}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor={libelleId} required>{t('fields.libelle')}</Label>
        <Input id={libelleId} type="text" {...form.register('libelle')} aria-invalid={Boolean(libelleError)} />
        {libelleError ? <p className="text-xs text-destructive">{libelleError}</p> : null}
      </div>

      <div className="space-y-2">
        <Label htmlFor={descriptionId}>{t('fields.description')}</Label>
        <Input id={descriptionId} type="text" {...form.register('description')} />
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor={dateId} required>{t('fields.dateDepense')}</Label>
          <Input id={dateId} type="date" max={today} {...form.register('dateDepense')} aria-invalid={Boolean(dateError)} />
          {dateError ? <p className="text-xs text-destructive">{dateError}</p> : null}
        </div>

        <div className="space-y-2">
          <Label htmlFor={montantId} required>{t('fields.montant')}</Label>
          <Controller
            control={form.control}
            name="montant"
            render={({ field }) => (
              <MoneyInput id={montantId} value={field.value} onChange={field.onChange} onBlur={field.onBlur} aria-invalid={Boolean(montantError)} />
            )}
          />
          {montantError ? <p className="text-xs text-destructive">{montantError}</p> : null}
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>{t('cancel')}</Button>
        <Button type="submit" disabled={isSubmitting}>{isSubmitting ? t('submitting') : isEdit ? t('save') : t('create')}</Button>
      </div>
    </form>
  )
}
```

- [ ] **Step 4: Write `DepensePlateformeFormDialog.tsx` + props**

```typescript
// presentation/depense-plateforme-form-dialog-props.ts
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'

export type DepensePlateformeFormDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  categories: CategoryDepensePlateforme[]
  target?: DepensePlateforme
  onSubmitted?: () => void
}
```

```typescript
// presentation/DepensePlateformeFormDialog.tsx
'use client'

import { useTranslations } from 'next-intl'

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/common/presentation/ui/dialog'
import { DepensePlateformeForm } from './DepensePlateformeForm'
import type { DepensePlateformeFormDialogProps } from './depense-plateforme-form-dialog-props'

export function DepensePlateformeFormDialog({ open, onOpenChange, categories, target, onSubmitted }: DepensePlateformeFormDialogProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.dialog')

  function handleClose() {
    onOpenChange(false)
  }

  function handleSubmitted() {
    handleClose()
    onSubmitted?.()
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[95dvh] flex-col sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{target ? t('editTitle') : t('createTitle')}</DialogTitle>
          <DialogDescription>{target ? t('editDescription') : t('createDescription')}</DialogDescription>
        </DialogHeader>
        <div className="min-h-0 flex-1 overflow-y-auto">
          <DepensePlateformeForm categories={categories} target={target} onCancel={handleClose} onSubmitted={handleSubmitted} />
        </div>
      </DialogContent>
    </Dialog>
  )
}
```

- [ ] **Step 5: Write `DepensePlateformeTable.tsx` + props (adds a `country` column on top of the `DepenseTable` template)**

```typescript
// presentation/depense-plateforme-table-props.ts
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'

export type DepensePlateformeTableProps = {
  rows: DepensePlateforme[]
  canUpdate: boolean
  canDelete: boolean
  onEdit: (row: DepensePlateforme) => void
  onDelete: (row: DepensePlateforme) => void
}
```

```typescript
// presentation/DepensePlateformeTable.tsx
'use client'

import { useFormatter, useTranslations } from 'next-intl'
import { useMemo } from 'react'
import type { ColumnDef } from '@tanstack/react-table'
import { MoreHorizontal } from 'lucide-react'

import { DataTable } from '@/common/presentation/shared/DataTable'
import { Button } from '@/common/presentation/ui/button'
import { useCurrencyLabel } from '@/common/application/use-currency-label'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'
import type { DepensePlateformeTableProps } from './depense-plateforme-table-props'

export function DepensePlateformeTable({ rows, canUpdate, canDelete, onEdit, onDelete }: DepensePlateformeTableProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.table')
  const currencyLabel = useCurrencyLabel()
  const format = useFormatter()

  const columns = useMemo<ColumnDef<DepensePlateforme>[]>(
    () => [
      { accessorKey: 'dateDepense', header: t('date'), cell: ({ row }) => <span className="text-muted-foreground">{row.original.dateDepense}</span> },
      { accessorKey: 'libelle', header: t('libelle'), cell: ({ row }) => <span className="font-medium text-foreground">{row.original.libelle}</span> },
      { accessorKey: 'category', header: t('category'), cell: ({ row }) => <span className="text-muted-foreground">{row.original.category?.nom ?? '—'}</span> },
      { accessorKey: 'country', header: t('country'), cell: ({ row }) => <span className="text-muted-foreground">{row.original.country?.name ?? t('global')}</span> },
      {
        accessorKey: 'montant',
        header: `${t('montant')} (${currencyLabel})`,
        cell: ({ row }) => <span className="font-medium text-foreground">{format.number(row.original.montant)}</span>,
      },
      { accessorKey: 'modePaiement', header: t('moyen'), cell: ({ row }) => <span className="text-muted-foreground">{row.original.modePaiement?.libelle ?? '—'}</span> },
      {
        id: 'actions',
        header: () => null,
        cell: ({ row }) => (
          (canUpdate || canDelete) ? (
            <div className="flex gap-1">
              {canUpdate ? (
                <Button type="button" variant="ghost" size="icon" onClick={() => onEdit(row.original)} aria-label={t('rowActions.edit')}>
                  <MoreHorizontal aria-hidden="true" className="size-4" />
                </Button>
              ) : null}
              {canDelete ? (
                <Button type="button" variant="ghost" size="sm" onClick={() => onDelete(row.original)}>
                  {t('rowActions.delete')}
                </Button>
              ) : null}
            </div>
          ) : null
        ),
      },
    ],
    [t, format, currencyLabel, canUpdate, canDelete, onEdit, onDelete],
  )

  return <DataTable data={rows} columns={columns} />
}
```

- [ ] **Step 6: Write `DepensePlateformeFilters.tsx` + props**

```typescript
// presentation/depense-plateforme-filters-props.ts
import type { CategoryDepensePlateforme } from '@/features/plateforme-depense/domain/dtos/category-depense-plateforme'

export type DepensePlateformeFiltersProps = {
  categoryId?: string
  moyenPaiementId?: string
  countryId?: string
  libelle?: string
  startDate?: string
  endDate?: string
  categories: CategoryDepensePlateforme[]
  onCategoryChange: (value: string | undefined) => void
  onMoyenPaiementChange: (value: string | undefined) => void
  onCountryChange: (value: string | undefined) => void
  onLibelleChange: (value: string | undefined) => void
  onStartDateChange: (value: string | undefined) => void
  onEndDateChange: (value: string | undefined) => void
  onSearch: () => void
  onReset: () => void
}
```

```typescript
// presentation/DepensePlateformeFilters.tsx
'use client'

import { useTranslations } from 'next-intl'
import { useMemo, useRef } from 'react'

import { Combobox } from '@/common/presentation/ui/combobox'
import { DateRangeFilter } from '@/common/presentation/shared/DateRangeFilter'
import { Input } from '@/common/presentation/ui/input'
import { Label } from '@/common/presentation/ui/label'
import { useCountries } from '@/features/country/application/useCountries'
import { useMoyenPaiementList } from '@/features/moyen-paiement/application/useMoyenPaiementList'
import type { DepensePlateformeFiltersProps } from './depense-plateforme-filters-props'

const DEBOUNCE_MS = 400

export function DepensePlateformeFilters({
  categoryId, moyenPaiementId, countryId, libelle, startDate, endDate,
  categories, onCategoryChange, onMoyenPaiementChange, onCountryChange,
  onLibelleChange, onStartDateChange, onEndDateChange, onSearch, onReset,
}: DepensePlateformeFiltersProps) {
  const t = useTranslations('dashboard.administration.depensesPlateforme.filters')
  const moyensQuery = useMoyenPaiementList()
  const countriesQuery = useCountries()
  const debounceRef = useRef<ReturnType<typeof setTimeout>>(undefined)

  const categoryItems = useMemo(() => categories.map((c) => ({ value: c.id, label: c.nom })), [categories])
  const moyenItems = useMemo(() => (moyensQuery.data ?? []).map((m) => ({ value: m.id, label: m.libelle })), [moyensQuery.data])
  const countryItems = useMemo(() => (countriesQuery.data ?? []).map((c) => ({ value: c.id, label: c.name })), [countriesQuery.data])

  function handleLibelleChange(value: string | undefined) {
    onLibelleChange(value)
    clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(onSearch, DEBOUNCE_MS)
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-border bg-card p-3">
      <Input
        placeholder={t('libellePlaceholder')}
        value={libelle ?? ''}
        onChange={(e) => handleLibelleChange(e.target.value || undefined)}
        className="h-9"
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="space-y-1.5">
          <Label className="text-xs font-medium text-muted-foreground">{t('categoryLabel')}</Label>
          <Combobox items={categoryItems} value={categoryId ?? ''} onValueChange={(v) => { onCategoryChange(v || undefined); onSearch() }} placeholder={t('categoryAll')} ariaLabel={t('categoryLabel')} emptyLabel="—" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs font-medium text-muted-foreground">{t('moyenLabel')}</Label>
          <Combobox items={moyenItems} value={moyenPaiementId ?? ''} onValueChange={(v) => { onMoyenPaiementChange(v || undefined); onSearch() }} placeholder={t('moyenAll')} ariaLabel={t('moyenLabel')} emptyLabel="—" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs font-medium text-muted-foreground">{t('countryLabel')}</Label>
          <Combobox items={countryItems} value={countryId ?? ''} onValueChange={(v) => { onCountryChange(v || undefined); onSearch() }} placeholder={t('countryAll')} ariaLabel={t('countryLabel')} emptyLabel="—" />
        </div>
      </div>

      <DateRangeFilter
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={(v) => { onStartDateChange(v); onSearch() }}
        onEndDateChange={(v) => { onEndDateChange(v); onSearch() }}
      />

      <div className="flex justify-end">
        <button type="button" onClick={onReset} className="text-xs text-muted-foreground underline underline-offset-2">
          {t('reset')}
        </button>
      </div>
    </div>
  )
}
```

- [ ] **Step 7: Write the page composition**

```typescript
// app/(dashboard)/dashboard/administration/depenses/DepensesPlateformePage.tsx
'use client'

import { Plus, Receipt, SearchX, Tags } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { useMemo, useState } from 'react'

import { runMutationWithToast } from '@/common/application/mutation-toast'
import { useDeferredSearch } from '@/common/application/use-deferred-search'
import { usePermission } from '@/common/application/usePermission'
import { Button } from '@/common/presentation/ui/button'
import { ConfirmDialog } from '@/common/presentation/shared/ConfirmDialog'
import { EmptyState } from '@/common/presentation/shared/EmptyState'
import { LoadingState } from '@/common/presentation/shared/LoadingState'
import { Pagination } from '@/common/presentation/shared/Pagination'
import { SearchPromptState } from '@/common/presentation/shared/SearchPromptState'
import { useCategoryDepensePlateformeList } from '@/features/plateforme-depense/application/useCategoryDepensePlateformeList'
import { useDeleteDepensePlateforme } from '@/features/plateforme-depense/application/useDeleteDepensePlateforme'
import { useDepensePlateformePage } from '@/features/plateforme-depense/application/useDepensePlateformePage'
import { CategoryDepensePlateformeManagerDialog } from '@/features/plateforme-depense/presentation/CategoryDepensePlateformeManagerDialog'
import { DepensePlateformeFilters } from '@/features/plateforme-depense/presentation/DepensePlateformeFilters'
import { DepensePlateformeFormDialog } from '@/features/plateforme-depense/presentation/DepensePlateformeFormDialog'
import { DepensePlateformeTable } from '@/features/plateforme-depense/presentation/DepensePlateformeTable'
import type { DepensePlateforme } from '@/features/plateforme-depense/domain/dtos/depense-plateforme'
import type { DepensePlateformeFilter } from '@/features/plateforme-depense/domain/dtos/depense-plateforme-filter'

const INITIAL_FILTER: DepensePlateformeFilter = {
  categoryId: undefined, moyenPaiementId: undefined, countryId: undefined,
  libelle: undefined, startDate: undefined, endDate: undefined, page: 0, size: 10,
}

export function DepensesPlateformePage() {
  const t = useTranslations('dashboard.administration.depensesPlateforme')

  const { draft, setDraft, active, isReady, search, setActiveFilter, reset } = useDeferredSearch<DepensePlateformeFilter>(INITIAL_FILTER)

  const [formOpen, setFormOpen] = useState(false)
  const [formTarget, setFormTarget] = useState<DepensePlateforme | undefined>(undefined)
  const [deleteTarget, setDeleteTarget] = useState<DepensePlateforme | undefined>(undefined)
  const [categoryManagerOpen, setCategoryManagerOpen] = useState(false)

  const categoriesQuery = useCategoryDepensePlateformeList()
  const categories = categoriesQuery.data ?? []
  const listQuery = useDepensePlateformePage(active ?? INITIAL_FILTER, { enabled: isReady })
  const deleteMutation = useDeleteDepensePlateforme()

  const canCreate = usePermission('PLATFORM_EXPENSE_CREATE')
  const canUpdate = usePermission('PLATFORM_EXPENSE_UPDATE')
  const canDelete = usePermission('PLATFORM_EXPENSE_DELETE')

  function openCreate() {
    setFormTarget(undefined)
    setFormOpen(true)
  }

  function openEdit(depense: DepensePlateforme) {
    setFormTarget(depense)
    setFormOpen(true)
  }

  function handleDeleteConfirmed() {
    if (!deleteTarget) return
    runMutationWithToast(deleteMutation, deleteTarget.id, { successMessage: t('toasts.deleted'), onSettled: () => setDeleteTarget(undefined) })
  }

  function handlePageChange(nextPage: number) {
    setActiveFilter((previous) => ({ ...previous, page: nextPage }))
  }

  function handleSizeChange(nextSize: number) {
    setActiveFilter((previous) => ({ ...previous, size: nextSize, page: 0 }))
  }

  const rows = listQuery.data?.content ?? []
  const isInitialLoading = isReady && listQuery.isLoading && !listQuery.data
  const hasResults = isReady && rows.length > 0
  const noResults = isReady && !isInitialLoading && rows.length === 0

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-2">
        {canCreate ? (
          <Button type="button" onClick={openCreate} className="shrink-0">
            <Plus aria-hidden="true" className="size-4" />
            <span className="hidden sm:inline">{t('createAction')}</span>
          </Button>
        ) : null}
        <Button type="button" variant="outline" onClick={() => setCategoryManagerOpen(true)} className="shrink-0">
          <Tags aria-hidden="true" className="size-4" />
          <span className="hidden sm:inline">{t('manageCategoriesAction')}</span>
        </Button>
        <div className="flex-1">
          <DepensePlateformeFilters
            categoryId={draft.categoryId}
            moyenPaiementId={draft.moyenPaiementId}
            countryId={draft.countryId}
            libelle={draft.libelle}
            startDate={draft.startDate}
            endDate={draft.endDate}
            categories={categories}
            onCategoryChange={(v) => setDraft((p) => ({ ...p, categoryId: v }))}
            onMoyenPaiementChange={(v) => setDraft((p) => ({ ...p, moyenPaiementId: v }))}
            onCountryChange={(v) => setDraft((p) => ({ ...p, countryId: v }))}
            onLibelleChange={(v) => setDraft((p) => ({ ...p, libelle: v }))}
            onStartDateChange={(v) => setDraft((p) => ({ ...p, startDate: v }))}
            onEndDateChange={(v) => setDraft((p) => ({ ...p, endDate: v }))}
            onSearch={search}
            onReset={reset}
          />
        </div>
      </div>

      {!isReady ? (
        <SearchPromptState onSearch={search} />
      ) : isInitialLoading ? (
        <LoadingState rows={5} />
      ) : noResults ? (
        <EmptyState
          title={t('noResults.title')}
          description={t('noResults.description')}
          icon={<SearchX className="size-8" aria-hidden="true" />}
          action={<Button variant="outline" onClick={reset}>{t('noResults.action')}</Button>}
        />
      ) : hasResults ? (
        <div className="flex flex-col gap-4">
          <DepensePlateformeTable rows={rows} canUpdate={canUpdate} canDelete={canDelete} onEdit={openEdit} onDelete={(d) => setDeleteTarget(d)} />
          {listQuery.data ? (
            <Pagination
              pageNumber={listQuery.data.number}
              pageSize={listQuery.data.size}
              totalElements={listQuery.data.totalElements}
              totalPages={listQuery.data.totalPages}
              onPageChange={handlePageChange}
              onSizeChange={handleSizeChange}
            />
          ) : null}
        </div>
      ) : (
        <EmptyState
          title={t('empty.title')}
          description={t('empty.description')}
          icon={<Receipt className="size-8" aria-hidden="true" />}
          action={canCreate ? <Button onClick={openCreate}><Plus aria-hidden="true" className="size-4" />{t('empty.action')}</Button> : undefined}
        />
      )}

      <DepensePlateformeFormDialog open={formOpen} onOpenChange={setFormOpen} categories={categories} target={formTarget} onSubmitted={search} />

      <CategoryDepensePlateformeManagerDialog open={categoryManagerOpen} onOpenChange={setCategoryManagerOpen} />

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

```typescript
// app/(dashboard)/dashboard/administration/depenses/page.tsx
import type { Metadata } from 'next'
import { getTranslations } from 'next-intl/server'
import { DepensesPlateformePage } from './DepensesPlateformePage'

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('dashboard.administration.depensesPlateforme')
  return { title: t('metaTitle') }
}

export default function Page() {
  return <DepensesPlateformePage />
}
```

- [ ] **Step 8: Register the nav tab**

In `src/app/(dashboard)/dashboard/administration/_tabs.ts`, add `'depensesPlateforme'` to the `AdministrationTabKey` union and one entry to `ADMINISTRATION_TABS`, gated by the new `PLATFORM_EXPENSE_READ` permission (mirrors the `moyensPaiement` entry's shape exactly):

```typescript
  | 'depensesPlateforme'
```

```typescript
  { key: 'depensesPlateforme', href: '/dashboard/administration/depenses', requiredPermission: 'PLATFORM_EXPENSE_READ' },
```

- [ ] **Step 9: Add the i18n namespace**

In `src/messages/fr.json`, inside `dashboard.administration` (find via `grep -n '"administration"' src/messages/fr.json`), add a new `depensesPlateforme` key alongside the existing `moyensPaiement` one:

```json
    "depensesPlateforme": {
      "metaTitle": "Dépenses plateforme — Administration",
      "createAction": "Nouvelle dépense",
      "manageCategoriesAction": "Gérer les catégories",
      "empty": { "title": "Aucune dépense", "description": "Enregistrez les coûts d'exploitation de la plateforme.", "action": "Créer une dépense" },
      "noResults": { "title": "Aucun résultat", "description": "Aucune dépense ne correspond à ces filtres.", "action": "Réinitialiser" },
      "confirmDelete": { "title": "Supprimer la dépense ?", "description": "Cette action est irréversible.", "confirm": "Supprimer" },
      "toasts": { "created": "Dépense créée", "updated": "Dépense mise à jour", "deleted": "Dépense supprimée", "categoryCreated": "Catégorie créée", "categoryUpdated": "Catégorie mise à jour", "categoryDeleted": "Catégorie supprimée" },
      "table": { "date": "Date", "libelle": "Libellé", "category": "Catégorie", "country": "Pays", "global": "Global", "montant": "Montant", "moyen": "Moyen", "rowActions": { "edit": "Modifier", "delete": "Supprimer" } },
      "filters": { "libellePlaceholder": "Rechercher…", "categoryLabel": "Catégorie", "categoryAll": "Toutes", "moyenLabel": "Moyen de paiement", "moyenAll": "Tous", "countryLabel": "Pays", "countryAll": "Tous", "reset": "Réinitialiser" },
      "dialog": { "createTitle": "Nouvelle dépense", "createDescription": "Enregistrer une dépense plateforme.", "editTitle": "Modifier la dépense", "editDescription": "Mettre à jour cette dépense." },
      "form": {
        "fields": { "category": "Catégorie", "categoryPlaceholder": "Choisir…", "modePaiement": "Moyen de paiement", "modePaiementPlaceholder": "Choisir…", "country": "Pays", "countryPlaceholder": "Choisir…", "countryGlobal": "Global (aucun pays)", "libelle": "Libellé", "description": "Description", "dateDepense": "Date", "montant": "Montant" },
        "validation": { "categoryRequired": "Catégorie requise", "libelleRequired": "Libellé requis", "libelleMax": "200 caractères maximum", "descriptionMax": "1000 caractères maximum", "dateRequired": "Date requise", "montantInvalid": "Montant invalide", "montantPositive": "Le montant doit être positif", "modePaiementRequired": "Moyen de paiement requis" },
        "cancel": "Annuler", "submitting": "Enregistrement…", "save": "Enregistrer", "create": "Créer"
      },
      "categoryForm": {
        "fields": { "nom": "Nom", "description": "Description" },
        "validation": { "nomRequired": "Nom requis", "nomMax": "100 caractères maximum", "descriptionMax": "500 caractères maximum" },
        "cancel": "Annuler", "submitting": "Enregistrement…", "save": "Enregistrer", "create": "Créer"
      },
      "categoryManager": {
        "title": "Catégories de dépense plateforme", "createAction": "Nouvelle catégorie", "edit": "Modifier", "delete": "Supprimer",
        "empty": { "title": "Aucune catégorie", "description": "Créez une première catégorie." },
        "confirmDelete": { "title": "Supprimer la catégorie ?", "description": "Cette action est irréversible.", "confirm": "Supprimer" }
      }
    },
```

Mirror the same structure in `src/messages/en.json` with English copy (same keys, translated values — e.g. `"createAction": "New expense"`, `"manageCategoriesAction": "Manage categories"`, etc.).

- [ ] **Step 10: Manual UI check**

Run the frontend dev server (`npm run dev`), log in as an ADMIN account, navigate to `/dashboard/administration/depenses`: confirm the page is empty until "Rechercher"-equivalent filter interaction (rule 47 gate via `SearchPromptState`), create a category via "Gérer les catégories", create a depense with and without a country, edit it, delete it, and confirm a non-ADMIN account (OWNER/MANAGER) gets `PermissionGuard`'s forbidden card if they navigate to the URL directly (they don't hold `PLATFORM_EXPENSE_READ`).

- [ ] **Step 11: Run typecheck**

Run: `npx tsc --noEmit`
Expected: clean.

- [ ] **Step 12: Commit**

```bash
git add src/features/plateforme-depense/presentation src/app/'(dashboard)'/dashboard/administration/depenses src/app/'(dashboard)'/dashboard/administration/_tabs.ts src/messages/fr.json src/messages/en.json
git commit -m "feat(plateforme-depense): admin CRUD page for platform expenses + categories"
```

---

### Task 11: Reporting page — `/dashboard/administration/depenses/reporting` + 2-tab sub-nav

**Files:**
- Create: `src/app/(dashboard)/dashboard/administration/depenses/_tabs.ts`
- Create: `src/app/(dashboard)/dashboard/administration/depenses/layout.tsx`
- Create: `src/app/(dashboard)/dashboard/administration/depenses/reporting/DepensesPlateformeReportingPage.tsx`
- Create: `src/app/(dashboard)/dashboard/administration/depenses/reporting/page.tsx`
- Modify: `src/messages/fr.json` (add `dashboard.administration.depensesPlateforme.nav` + `.reporting`)
- Modify: `src/messages/en.json` (same, EN)

Task 10's page is moved under this new sub-nav automatically (the tabs live in a `layout.tsx` wrapping both `depenses/page.tsx` — already created in Task 10 — and `depenses/reporting/page.tsx`), mirroring exactly how `/dashboard/depenses` + `/dashboard/depenses/reporting` already coexist under `DEPENSE_TABS`.

**Interfaces:**
- Consumes: Task 8 (`PeriodSelector`, `getDateRange`), Task 9 (`usePlateformePeriodReport`), existing `useCountries()`, existing `useAbonnementAdminList` (`@/features/abonnement/application/useAbonnementAdminList`, already used by `AbonnementListPage.tsx` — reused here to source the abonnement-picker Combobox from `{ id, entrepriseSigle }` rows, no new endpoint), existing `KpiCard` from `@/app/(dashboard)/dashboard/administration/reporting/KpiCard` (the `danger`-variant-capable one, distinct from `@/app/(dashboard)/dashboard/reporting/KpiCard`).

- [ ] **Step 1: Write `_tabs.ts` + `layout.tsx`**

```typescript
// app/(dashboard)/dashboard/administration/depenses/_tabs.ts
import type { GuardedNavItem } from '@/common/application/nav-guard'

export type DepensePlateformeTabKey = 'liste' | 'reporting'

export type DepensePlateformeTab = GuardedNavItem & {
  key: DepensePlateformeTabKey
  href: string
}

export const DEPENSE_PLATEFORME_TABS: DepensePlateformeTab[] = [
  { key: 'liste',     href: '/dashboard/administration/depenses',           requiredPermission: 'PLATFORM_EXPENSE_READ' },
  { key: 'reporting', href: '/dashboard/administration/depenses/reporting', requiredPermission: 'PLATFORM_REPORT_READ' },
]
```

```typescript
// app/(dashboard)/dashboard/administration/depenses/layout.tsx
'use client'

import { BarChart2, List } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useTranslations } from 'next-intl'
import type { ReactNode } from 'react'

import { navGuard } from '@/common/application/nav-guard'
import { PermissionGuard } from '@/common/presentation/shared/PermissionGuard'
import { useAuthStore } from '@/features/security/application/auth-store'
import { cn } from '@/lib/utils'

import { DEPENSE_PLATEFORME_TABS, type DepensePlateformeTabKey } from './_tabs'

const TAB_ICONS: Record<DepensePlateformeTabKey, ReactNode> = {
  liste:     <List      aria-hidden="true" className="size-4" />,
  reporting: <BarChart2 aria-hidden="true" className="size-4" />,
}

export default function DepensesPlateformeLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname()
  const user = useAuthStore((state) => state.user)
  const t = useTranslations('dashboard.administration.depensesPlateforme.nav')

  const visibleTabs = navGuard.filterVisible(DEPENSE_PLATEFORME_TABS, user)

  return (
    <PermissionGuard requiredPermission="PLATFORM_EXPENSE_READ">
      <div className="flex flex-col gap-4">
        <nav aria-label={t('ariaLabel')} className="flex flex-wrap gap-1 border-b border-border pb-2">
          {visibleTabs.map((tab) => {
            const isActive = tab.href === '/dashboard/administration/depenses'
              ? pathname === '/dashboard/administration/depenses'
              : pathname.startsWith(tab.href)
            return (
              <Link
                key={tab.href}
                href={tab.href}
                aria-current={isActive ? 'page' : undefined}
                className={cn(
                  'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50',
                  isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                )}
              >
                {TAB_ICONS[tab.key]}
                {t(tab.key)}
              </Link>
            )
          })}
        </nav>
        {children}
      </div>
    </PermissionGuard>
  )
}
```

- [ ] **Step 2: Write the reporting page**

```typescript
// app/(dashboard)/dashboard/administration/depenses/reporting/DepensesPlateformeReportingPage.tsx
'use client'

import { Banknote, TrendingDown, TrendingUp } from 'lucide-react'
import { useFormatter, useTranslations } from 'next-intl'
import { useState } from 'react'

import { Combobox } from '@/common/presentation/ui/combobox'
import { Label } from '@/common/presentation/ui/label'
import { LoadingState } from '@/common/presentation/shared/LoadingState'
import { PeriodSelector } from '@/common/presentation/shared/PeriodSelector'
import { getDateRange, todayISO } from '@/common/tools/dateHelpers'
import type { ReportPeriod } from '@/common/presentation/shared/period-selector-props'
import { useCountries } from '@/features/country/application/useCountries'
import { useAbonnementAdminList } from '@/features/abonnement/application/useAbonnementAdminList'
import { usePlateformePeriodReport } from '@/features/plateforme-depense/application/usePlateformePeriodReport'
import { useCurrencyLabel } from '@/common/application/use-currency-label'
import { KpiCard } from '@/app/(dashboard)/dashboard/administration/reporting/KpiCard'

export function DepensesPlateformeReportingPage() {
  const t = useTranslations('dashboard.administration.depensesPlateforme.reporting')
  const format = useFormatter()
  const currencyLabel = useCurrencyLabel()

  const today = todayISO()
  const [period, setPeriod] = useState<ReportPeriod>('month')
  const [customFrom, setCustomFrom] = useState(today)
  const [customTo, setCustomTo] = useState(today)
  const [countryId, setCountryId] = useState<string | undefined>(undefined)
  const [abonnementId, setAbonnementId] = useState<string | undefined>(undefined)

  const { from, to } = getDateRange(period, customFrom, customTo)

  const countriesQuery = useCountries()
  const countryItems = (countriesQuery.data ?? []).map((c) => ({ value: c.id, label: c.name }))

  const abonnementsQuery = useAbonnementAdminList({ page: 0, size: 100 })
  const abonnementItems = (abonnementsQuery.data?.content ?? []).map((a) => ({ value: a.id, label: a.entrepriseSigle ?? a.id }))

  const reportQuery = usePlateformePeriodReport({ startDate: from, endDate: to, countryId, abonnementId })

  const revenu = reportQuery.data?.revenu ?? 0
  const depenses = reportQuery.data?.depensesPlateforme ?? 0
  const benefice = reportQuery.data?.benefice ?? 0
  const periodLabel = from === to ? from : `${from} → ${to}`

  return (
    <div className="flex flex-col gap-6">
      <PeriodSelector
        period={period}
        customFrom={customFrom}
        customTo={customTo}
        onPeriodChange={setPeriod}
        onCustomFromChange={setCustomFrom}
        onCustomToChange={setCustomTo}
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label className="text-xs font-medium text-muted-foreground">{t('countryLabel')}</Label>
          <Combobox items={countryItems} value={countryId ?? ''} onValueChange={(v) => setCountryId(v || undefined)} placeholder={t('countryAll')} ariaLabel={t('countryLabel')} emptyLabel="—" />
        </div>
        <div className="space-y-1.5">
          <Label className="text-xs font-medium text-muted-foreground">{t('abonnementLabel')}</Label>
          <Combobox items={abonnementItems} value={abonnementId ?? ''} onValueChange={(v) => setAbonnementId(v || undefined)} placeholder={t('abonnementAll')} ariaLabel={t('abonnementLabel')} emptyLabel="—" />
        </div>
      </div>

      {reportQuery.isLoading ? (
        <LoadingState rows={3} />
      ) : (
        <>
          <p className="text-xs text-muted-foreground">
            {t('periodLabel')} <span className="font-medium text-foreground">{periodLabel}</span>
          </p>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <KpiCard
              label={t('kpi.revenu')}
              value={`${format.number(revenu)} ${currencyLabel}`}
              icon={<TrendingUp aria-hidden="true" className="size-5" />}
              variant="success"
            />
            <KpiCard
              label={t('kpi.depenses')}
              value={`${format.number(depenses)} ${currencyLabel}`}
              icon={<TrendingDown aria-hidden="true" className="size-5" />}
              variant="warning"
            />
            <KpiCard
              label={t('kpi.benefice')}
              value={`${format.number(benefice)} ${currencyLabel}`}
              icon={<Banknote aria-hidden="true" className="size-5" />}
              variant={benefice >= 0 ? 'success' : 'danger'}
              sub={abonnementId ? t('kpi.beneficeGlobalHint') : undefined}
            />
          </div>
        </>
      )}
    </div>
  )
}
```

```typescript
// app/(dashboard)/dashboard/administration/depenses/reporting/page.tsx
import type { Metadata } from 'next'
import { getTranslations } from 'next-intl/server'
import { DepensesPlateformeReportingPage } from './DepensesPlateformeReportingPage'

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations('dashboard.administration.depensesPlateforme.reporting')
  return { title: t('metaTitle') }
}

export default function Page() {
  return <DepensesPlateformeReportingPage />
}
```

- [ ] **Step 3: Add the nav + reporting i18n keys**

In `src/messages/fr.json`, inside the `depensesPlateforme` block written in Task 10, add two sibling keys (`nav` and `reporting`):

```json
      "nav": { "ariaLabel": "Sous-navigation dépenses plateforme", "liste": "Dépenses", "reporting": "Reporting" },
      "reporting": {
        "metaTitle": "Reporting dépenses plateforme — Administration",
        "periodLabel": "Période :",
        "countryLabel": "Pays", "countryAll": "Tous pays",
        "abonnementLabel": "Abonnement", "abonnementAll": "Tous",
        "kpi": { "revenu": "Revenu", "depenses": "Dépenses plateforme", "benefice": "Bénéfice", "beneficeGlobalHint": "Dépenses et bénéfice restent globaux (non filtrés par abonnement)" }
      }
```

Mirror the same two keys in `src/messages/en.json` with English copy.

- [ ] **Step 4: Manual UI check**

Confirm `/dashboard/administration/depenses` now shows the 2-tab sub-nav (Dépenses / Reporting), the reporting tab shows month/quarter/year/custom presets, picking a country changes all 3 KPI cards, picking an abonnement changes only Revenu (Dépenses/Bénéfice show the `sub` hint text and stay at their global value), and bénéfice card turns red when negative.

- [ ] **Step 5: Run typecheck**

Run: `npx tsc --noEmit`
Expected: clean.

- [ ] **Step 6: Commit**

```bash
git add "src/app/(dashboard)/dashboard/administration/depenses/_tabs.ts" "src/app/(dashboard)/dashboard/administration/depenses/layout.tsx" "src/app/(dashboard)/dashboard/administration/depenses/reporting" src/messages/fr.json src/messages/en.json
git commit -m "feat(plateforme-depense): P&L reporting page with country/abonnement filters"
```

---

### Task 12: Full frontend suite + typecheck — verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full suite**

Run: `npx vitest run`
Expected: all green, including every pre-existing test — confirms zero regression on `PeriodTab`, `ReportingMainPage`, `VentesReportingPage`, `DepenseReportingPage`, `ADMINISTRATION_TABS`-consuming tests, `PermissionGuard`.

- [ ] **Step 2: Run the typecheck**

Run: `npx tsc --noEmit`
Expected: exit 0.

- [ ] **Step 3: If anything fails, fix and re-run.**

- [ ] **Step 4: No commit for this task** — verification checkpoint only.

---

## Self-Review

**Spec coverage:**
- `plateforme` module (2.1) → Tasks 1, 3, 4.
- `Revenu` entity + event-driven creation (2.2) → Task 5.
- New separate reporting endpoint, nothing existing touched (2.3) → Task 6.
- Migrations V85/V86 (2.4) → Task 1.
- Shared period selector quarter/year (3.1) → Task 8.
- CRUD page (3.2) → Task 10.
- Reporting page (3.3) → Task 11.
- Permissions, ADMIN only, not in `PermissionCode.java` (Global Constraints, corrected during planning) → Task 2.
- `REPORT_FINANCIAL` avoided (Global Constraints) → Task 6 uses `PLATFORM_REPORT_READ` instead.
- No `by-category` breakdown (out of scope per spec §5) → absent from Tasks 4/9/10/11 by design.

**Placeholder scan:** no TBD/TODO; every step has real code or an exact command.

**Type consistency checked:** `DepensePlateformeFilter` fields match across DTO (Task 4 Step 4), repository params (Task 4 Step 2), frontend DTO (Task 9 Step 1) and frontend api adapter (Task 9 Step 3). `PlateformePeriodReportResponse`/`PlateformePeriodReport` fields (`revenu`/`depensesPlateforme`/`benefice`) match across Task 6 and Tasks 9/11. `IRevenuService.getTotalForPeriod` signature is consistent between Task 5 (definition) and Task 6 (call site). `RevenuRecordedEvent` fields match between Task 5 Step 4 (definition), Step 7 (publish call), and Step 8 (listener + tests).
