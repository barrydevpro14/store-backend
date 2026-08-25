package org.store.plateforme.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.dto.DataSelect;
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

    @Query(value = """
            SELECT new org.store.common.dto.DataSelect(CAST(c.id AS string), c.nom)
            FROM CategoryDepensePlateforme c
            WHERE c.actif = true
              AND (:q IS NULL OR :q = '' OR LOWER(c.nom) LIKE :qPattern)
            ORDER BY c.nom ASC
            """,
           countQuery = """
            SELECT COUNT(c)
            FROM CategoryDepensePlateforme c
            WHERE c.actif = true
              AND (:q IS NULL OR :q = '' OR LOWER(c.nom) LIKE :qPattern)
            """)
    Page<DataSelect> findSelectItems(
            @Param("q") String q,
            @Param("qPattern") String qPattern,
            Pageable pageable);
}
