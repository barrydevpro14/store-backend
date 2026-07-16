package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.domain.model.CategoryDepense;

import java.util.Optional;
import java.util.UUID;

public interface CategoryDepenseRepository extends BaseRepository<CategoryDepense> {

    Optional<CategoryDepense> findByNomAndEntrepriseId(String nom, UUID entrepriseId);

    @Query("SELECT COUNT(c) > 0 FROM CategoryDepense c WHERE LOWER(c.nom) = LOWER(:nom) AND c.entreprise.id = :entrepriseId")
    boolean existsByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") UUID entrepriseId);

    @Query(value = """
            SELECT new org.store.depense.application.dto.CategoryDepenseResponse(category)
            FROM CategoryDepense category
            WHERE category.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(category.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR category.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            ORDER BY category.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(category)
            FROM CategoryDepense category
            WHERE category.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(category.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR category.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            """)
    Page<CategoryDepenseResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
