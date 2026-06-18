package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.domain.model.CategoryProduct;

import java.util.Optional;
import java.util.UUID;

public interface CategoryProductRepository extends BaseRepository<CategoryProduct> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.CategoryProductResponse(category)
            FROM CategoryProduct category
            WHERE category.entreprise.id = :entrepriseId
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(CONCAT(category.libelle,category.description)) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            ORDER BY category.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(category)
            FROM CategoryProduct category
            WHERE category.entreprise.id = :entrepriseId
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(CONCAT(category.libelle,category.description)) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', category.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', category.createdAt) <= CAST(:endDate AS date))
            """)
    Page<CategoryProductResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    Optional<CategoryProduct> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);
}
