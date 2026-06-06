package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.depense.application.dto.CategoryDepenseFilter;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.domain.model.CategoryDepense;

import java.util.Optional;
import java.util.UUID;

public interface CategoryDepenseRepository extends BaseRepository<CategoryDepense> {

    Optional<CategoryDepense> findByNomAndEntrepriseId(String nom, UUID entrepriseId);

    boolean existsByNomAndEntrepriseId(String nom, UUID entrepriseId);

    /**
     * Paginated listing of categories scoped to an entreprise. SpEL placeholders honor the rule-40
     * pattern : optional name LIKE, optional actif, optional created-at date range, ORDER BY
     * {@code category.createdAt DESC} (replaces the previous {@code category.nom ASC} — rule 40
     * supersedes alphabetical sort across every CRUD list).
     */
    @Query(value = """
            SELECT new org.store.depense.application.dto.CategoryDepenseResponse(category)
            FROM CategoryDepense category
            WHERE category.entreprise.id = :entrepriseId
              AND (:#{#filter.nom}    IS NULL OR LOWER(category.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}  IS NULL OR category.actif      = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR category.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR category.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY category.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(category)
            FROM CategoryDepense category
            WHERE category.entreprise.id = :entrepriseId
              AND (:#{#filter.nom}    IS NULL OR LOWER(category.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}  IS NULL OR category.actif      = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR category.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR category.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<CategoryDepenseResponse> findResponsesByFilter(@Param("entrepriseId") UUID entrepriseId,
                                                       @Param("filter") CategoryDepenseFilter filter,
                                                       Pageable pageable);
}
