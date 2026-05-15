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

    @Query("""
            SELECT DISTINCT p FROM Product p
            WHERE p.entreprise.id = :entrepriseId
              AND (:searchTerm IS NULL
                   OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                   OR LOWER(p.reference) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
              AND EXISTS (
                  SELECT 1 FROM EntreeStock e
                  WHERE e.produit = p
                    AND e.magasin.id = :magasinId
                    AND e.quantiteRestante > 0
              )
            """)
    Page<Product> searchByEntrepriseWithActiveLots(@Param("searchTerm") String searchTerm,
                                                   @Param("magasinId") UUID magasinId,
                                                   @Param("entrepriseId") UUID entrepriseId,
                                                   Pageable pageable);
}
