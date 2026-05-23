package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.ProductFilter;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.ProductResponse(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.nom} IS NULL
                  OR :#{#filter.nom} = ''
                  OR LOWER(produit.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%'))
              )
              AND (
                  :#{#filter.reference} IS NULL
                  OR :#{#filter.reference} = ''
                  OR LOWER(produit.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR produit.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR produit.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY produit.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(produit)
            FROM Product produit
            WHERE produit.entreprise.id = :entrepriseId
              AND (
                  :#{#filter.nom} IS NULL
                  OR :#{#filter.nom} = ''
                  OR LOWER(produit.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%'))
              )
              AND (
                  :#{#filter.reference} IS NULL
                  OR :#{#filter.reference} = ''
                  OR LOWER(produit.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%'))
              )
              AND (:#{#filter.createdStartDateTime()} IS NULL OR produit.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR produit.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<ProductResponse> findResponsesByFilter(@Param("filter") ProductFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
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
            """)
    Page<Product> searchByEntrepriseWithActiveLots(@Param("searchPattern") String searchPattern,
                                                   @Param("magasinId") UUID magasinId,
                                                   @Param("entrepriseId") UUID entrepriseId,
                                                   Pageable pageable);
}
