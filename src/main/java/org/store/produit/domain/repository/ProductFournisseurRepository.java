package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.UUID;

public interface ProductFournisseurRepository extends BaseRepository<ProductFournisseur> {

    @Query("""
            SELECT new org.store.produit.application.dto.ProductFournisseurResponse(productFournisseur)
            FROM ProductFournisseur productFournisseur
            WHERE productFournisseur.product.entreprise.id = :entrepriseId
            """)
    Page<ProductFournisseurResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    @Query("""
            SELECT new org.store.produit.application.dto.ProductFournisseurResponse(productFournisseur)
            FROM ProductFournisseur productFournisseur
            WHERE productFournisseur.product.id = :productId
            """)
    Page<ProductFournisseurResponse> findResponsesByProductId(@Param("productId") UUID productId, Pageable pageable);

    boolean existsByProductIdAndFournisseurIdAndQualityId(UUID productId, UUID fournisseurId, UUID qualityId);

    java.util.Optional<ProductFournisseur> findTop1ByProductIdAndFournisseurIdAndQualityId(UUID productId, UUID fournisseurId, UUID qualityId);
}
