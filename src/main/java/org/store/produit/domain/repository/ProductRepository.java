package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product> {

    @Query("""
            SELECT new org.store.produit.application.dto.ProductResponse(p)
            FROM Product p
            WHERE p.entreprise.id = :entrepriseId
            """)
    Page<ProductResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<Product> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);
}
