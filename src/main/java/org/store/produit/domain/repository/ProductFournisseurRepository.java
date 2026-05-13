package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.Optional;
import java.util.UUID;

public interface ProductFournisseurRepository extends BaseRepository<ProductFournisseur> {

    @Query("""
            SELECT new org.store.produit.application.dto.ProductFournisseurResponse(pf)
            FROM ProductFournisseur pf
            WHERE pf.product.entreprise.id = :entrepriseId
            """)
    Page<ProductFournisseurResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    @Query("""
            SELECT new org.store.produit.application.dto.ProductFournisseurResponse(pf)
            FROM ProductFournisseur pf
            WHERE pf.product.id = :productId
            """)
    Page<ProductFournisseurResponse> findResponsesByProductId(@Param("productId") UUID productId, Pageable pageable);

    Optional<ProductFournisseur> findByProductIdAndFournisseurId(UUID productId, UUID fournisseurId);

    boolean existsByProductIdAndFournisseurId(UUID productId, UUID fournisseurId);
}
