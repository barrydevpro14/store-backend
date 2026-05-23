package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.CategoryProductFilter;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.domain.model.CategoryProduct;

import java.util.Optional;
import java.util.UUID;

public interface CategoryProductRepository extends BaseRepository<CategoryProduct> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.CategoryProductResponse(category)
            FROM CategoryProduct category
            WHERE category.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.libelle} IS NULL
                  OR :#{#filter.libelle} = ''
                  OR LOWER(category.libelle) LIKE LOWER(CONCAT('%', :#{#filter.libelle}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR category.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR category.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY category.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(category)
            FROM CategoryProduct category
            WHERE category.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.libelle} IS NULL
                  OR :#{#filter.libelle} = ''
                  OR LOWER(category.libelle) LIKE LOWER(CONCAT('%', :#{#filter.libelle}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR category.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR category.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<CategoryProductResponse> findResponsesByFilter(@Param("filter") CategoryProductFilter filter,
                                                       @Param("entrepriseId") UUID entrepriseId,
                                                       Pageable pageable);

    Optional<CategoryProduct> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);
}
