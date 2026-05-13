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

    @Query("""
            SELECT new org.store.produit.application.dto.CategoryProductResponse(c)
            FROM CategoryProduct c
            WHERE c.entreprise.id = :entrepriseId
            """)
    Page<CategoryProductResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<CategoryProduct> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);

    boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId);
}
