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
            SELECT new org.store.produit.application.dto.ProductResponse(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
            """)
    Page<ProductResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<Product> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    @Query("""
            SELECT DISTINCT produit FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:searchTerm IS NULL
                   OR LOWER(produit.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                   OR LOWER(produit.reference) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
              AND EXISTS (
                  SELECT 1 FROM EntreeStock entree
                  WHERE entree.produit = produit
                    AND entree.magasin.id = :magasinId
                    AND entree.quantiteRestante > 0
              )
            """)
    Page<Product> searchByEntrepriseWithActiveLots(@Param("searchTerm") String searchTerm,
                                                   @Param("magasinId") UUID magasinId,
                                                   @Param("entrepriseId") UUID entrepriseId,
                                                   Pageable pageable);
}
