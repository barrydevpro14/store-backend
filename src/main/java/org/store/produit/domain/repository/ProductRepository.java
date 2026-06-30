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

    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductResponse(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(CONCAT(produit.nom,produit.reference)) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(CONCAT(produit.reference , produit.categoryProduct.libelle)) LIKE :referencePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', produit.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', produit.createdAt) <= CAST(:endDate AS date))
            ORDER BY produit.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(CONCAT(produit.nom,produit.reference)) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(CONCAT(produit.reference , produit.categoryProduct.libelle)) LIKE :referencePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', produit.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', produit.createdAt) <= CAST(:endDate AS date))
            """)
    Page<ProductResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("reference") String reference,
            @Param("referencePattern") String referencePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    Optional<Product> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    /**
     * Recherche produits par nom OU référence avec lots actifs dans
     * le magasin scopé. Le pattern LIKE est PRÉ-CONSTRUIT côté domain
     * service via {@link org.store.common.tools.LikePatternHelper} —
     * raison : Hibernate 7 sur PostgreSQL inférait parfois le type
     * d'un `:searchTerm` bare en bytea (utilisé deux fois avec
     * contextes ambigus), déclenchant `lower(bytea) does not exist`.
     * Bind un String pré-formé verrouille le type sur varchar.
     */
    @Query("""
            SELECT DISTINCT produit FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL
                   OR LOWER(produit.nom) LIKE :searchPattern
                   OR LOWER(produit.reference) LIKE :searchPattern)
              AND EXISTS (
                  SELECT 1 FROM EntreeStock entree
                  WHERE entree.produit = produit
                    AND entree.magasin.id = :magasinId
                    AND entree.quantiteRestante > 0
                    AND entree.annulee = false
              )
            ORDER BY produit.nom ASC
            """)
    Page<Product> searchByEntrepriseWithActiveLots(@Param("searchPattern") String searchPattern,
                                                   @Param("magasinId") UUID magasinId,
                                                   @Param("entrepriseId") UUID entrepriseId,
                                                   Pageable pageable);
}
