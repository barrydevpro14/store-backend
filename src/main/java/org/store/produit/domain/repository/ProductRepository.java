package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.ProductSelectorResponse;
import org.store.produit.application.dto.ProductVariantSearchResponse;
import org.store.produit.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductResponse(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(CONCAT(produit.nom,produit.reference)) LIKE :nomPattern
              OR LOWER(produit.categoryProduct.libelle) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(produit.reference) LIKE :referencePattern
               OR LOWER(produit.categoryProduct.libelle) LIKE :referencePattern OR LOWER(produit.nom) LIKE :referencePattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', produit.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', produit.createdAt) <= CAST(:endDate AS date))
            ORDER BY produit.nom ASC
            """,
           countQuery = """
            SELECT COUNT(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR :nom = '' OR LOWER(CONCAT(produit.nom,produit.reference)) LIKE :nomPattern
              OR LOWER(produit.categoryProduct.libelle) LIKE :nomPattern)
              AND (:reference IS NULL OR :reference = '' OR LOWER(CONCAT(produit.reference , produit.categoryProduct.libelle)) LIKE :referencePattern
              OR LOWER(produit.nom) LIKE :referencePattern)
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

    @Query("SELECT p FROM Product p WHERE LOWER(p.reference) = LOWER(:reference) AND LOWER(p.nom) = LOWER(:nom) AND p.entreprise.id = :entrepriseId")
    Optional<Product> findByReferenceAndNomAndEntrepriseId(@Param("reference") String reference, @Param("nom") String nom, @Param("entrepriseId") UUID entrepriseId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.reference) = LOWER(:reference) AND LOWER(p.nom) = LOWER(:nom) AND p.entreprise.id = :entrepriseId")
    boolean existsByReferenceAndNomAndEntrepriseId(@Param("reference") String reference, @Param("nom") String nom, @Param("entrepriseId") UUID entrepriseId);

    /**
     * Recherche variantes (ProductFournisseur) ayant du stock actif dans le magasin scopé.
     * Retourne un DTO plat avec label pré-construit (sans le suffixe "(N dispo)" laissé au frontend).
     * Pattern LIKE PRÉ-CONSTRUIT via {@link org.store.common.tools.LikePatternHelper} — workaround
     * Hibernate 7/PostgreSQL bytea type-inference sur paramètres liés deux fois.
     */
    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductVariantSearchResponse(
                pf.id,
                produit.id,
                quality.id,
                fournisseur.id,
                CONCAT(
                    CASE WHEN produit.reference IS NOT NULL
                         THEN CONCAT(produit.nom, ' (', produit.reference, ')')
                         ELSE produit.nom END,
                    ' — ',
                    produit.categoryProduct.libelle,
                    ' — ',
                    fournisseur.nom,
                    ' — ',
                    quality.libelle
                ),
                pf.prixAchat,
                pf.prixVente,
                stock.quantiteDisponible
            )
            FROM ProductFournisseur pf
            JOIN pf.product produit
            JOIN pf.quality quality
            JOIN pf.fournisseur fournisseur
            JOIN Stock stock ON stock.productFournisseur = pf AND stock.magasin.id = :magasinId
            WHERE produit.entreprise.id = :entrepriseId
              AND stock.quantiteDisponible > 0
              AND (:searchPattern IS NULL
                   OR LOWER(produit.nom) LIKE :searchPattern
                   OR LOWER(produit.reference) LIKE :searchPattern
                   OR LOWER(produit.categoryProduct.libelle) LIKE :searchPattern)
            ORDER BY produit.nom ASC, quality.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(pf.id)
            FROM ProductFournisseur pf
            JOIN pf.product produit
            JOIN pf.quality quality
            JOIN pf.fournisseur fournisseur
            JOIN Stock stock ON stock.productFournisseur = pf AND stock.magasin.id = :magasinId
            WHERE produit.entreprise.id = :entrepriseId
              AND stock.quantiteDisponible > 0
              AND (:searchPattern IS NULL
                   OR LOWER(produit.nom) LIKE :searchPattern
                   OR LOWER(produit.reference) LIKE :searchPattern
                   OR LOWER(produit.categoryProduct.libelle) LIKE :searchPattern)
            """)
    Page<ProductVariantSearchResponse> searchVariantsByEntrepriseWithStock(
            @Param("searchPattern") String searchPattern,
            @Param("magasinId") UUID magasinId,
            @Param("entrepriseId") UUID entrepriseId,
            Pageable pageable);

    /**
     * Recherche produits par nom OU référence dans une entreprise, SANS
     * filtre de stock. Utilisé par les contextes d'ajout de stock
     * (achat, entrée stock initiale) où l'absence de stock est justement
     * la raison d'ajouter le produit.
     */
    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductSelectorResponse(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL
                   OR LOWER(produit.nom) LIKE :searchPattern
                   OR LOWER(produit.reference) LIKE :searchPattern
                   OR LOWER(produit.categoryProduct.libelle) LIKE :searchPattern)
            ORDER BY produit.nom ASC
            """,
           countQuery = """
            SELECT COUNT(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (:searchPattern IS NULL
                   OR LOWER(produit.nom) LIKE :searchPattern
                   OR LOWER(produit.reference) LIKE :searchPattern
                   OR LOWER(produit.categoryProduct.libelle) LIKE :searchPattern)
            """)
    Page<ProductSelectorResponse> searchResponsesByEntreprise(@Param("searchPattern") String searchPattern,
                                                              @Param("entrepriseId") UUID entrepriseId,
                                                              Pageable pageable);
}
