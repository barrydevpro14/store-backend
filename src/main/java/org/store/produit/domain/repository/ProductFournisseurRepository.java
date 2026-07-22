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

    /**
     * Recherche paginée de variantes (ProductFournisseur) de l'entreprise par nom produit, référence, fournisseur ou qualité.
     * Sans filtre de stock — toutes les variantes du catalogue sont retournées.
     * Pattern LIKE PRÉ-CONSTRUIT via {@link org.store.common.tools.LikePatternHelper} pour éviter l'inférence bytea de Hibernate 7/PostgreSQL.
     */
    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductFournisseurResponse(pf)
            FROM ProductFournisseur pf
            WHERE pf.product.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL
                   OR LOWER(pf.product.nom) LIKE :searchPattern
                   OR LOWER(pf.product.reference) LIKE :searchPattern
                   OR LOWER(pf.fournisseur.nom) LIKE :searchPattern
                   OR LOWER(pf.fournisseur.prenom) LIKE :searchPattern
                   OR LOWER(pf.product.categoryProduct.libelle) LIKE :searchPattern)
            ORDER BY pf.product.nom ASC
            """,
           countQuery = """
            SELECT COUNT(pf)
            FROM ProductFournisseur pf
            WHERE pf.product.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL
                   OR LOWER(pf.product.nom) LIKE :searchPattern
                   OR LOWER(pf.product.reference) LIKE :searchPattern
                   OR LOWER(pf.fournisseur.nom) LIKE :searchPattern
                   OR LOWER(pf.fournisseur.prenom) LIKE :searchPattern
                   OR LOWER(pf.product.categoryProduct.libelle) LIKE :searchPattern)
            """)
    Page<ProductFournisseurResponse> searchResponsesByEntrepriseAndTerm(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);
}
