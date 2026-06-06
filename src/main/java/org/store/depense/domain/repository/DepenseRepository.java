package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseParCategorieResponse;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.domain.model.Depense;

import java.util.List;
import java.util.UUID;


public interface DepenseRepository extends BaseRepository<Depense> {

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseResponse(depense)
            FROM Depense depense
            WHERE depense.magasin.entreprise.id = :entrepriseId
              AND depense.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR depense.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR depense.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND depense.dateDepense >= :#{#filter.fromDateSentinel()}
              AND depense.dateDepense <= :#{#filter.toDateSentinel()}
              AND depense.createdAt >= :#{#filter.createdStartDateTime()}
              AND depense.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY depense.createdAt DESC
            """)
    Page<DepenseResponse> findResponsesByFilter(@Param("filter") DepenseFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseTotalResponse(
                :#{#filter.magasinId},
                COALESCE(SUM(depense.montant), 0),
                COUNT(depense)
            )
            FROM Depense depense
            WHERE depense.magasin.entreprise.id = :entrepriseId
              AND depense.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.categoryId} IS NULL OR depense.category.id = :#{#filter.categoryId})
              AND (:#{#filter.modePaiementAsEnum()} IS NULL OR depense.modePaiement = :#{#filter.modePaiementAsEnum()})
              AND depense.dateDepense >= :#{#filter.fromDateSentinel()}
              AND depense.dateDepense <= :#{#filter.toDateSentinel()}
              AND depense.createdAt >= :#{#filter.createdStartDateTime()}
              AND depense.createdAt <  :#{#filter.createdEndDateTime()}
            """)
    DepenseTotalResponse computeTotal(@Param("filter") DepenseFilter filter,
                                      @Param("entrepriseId") UUID entrepriseId);

    /**
     * Aggregation par catégorie via SQL natif — évite le problème de type-inference
     * Hibernate 6 + PostgreSQL 16 sur les paramètres LocalDate en JPQL.
     * Les dates sont passées en chaîne ISO (yyyy-MM-dd) et castées côté SQL.
     */
    @Query(value = """
            SELECT d.category_id        AS categoryId,
                   c.nom                AS categoryNom,
                   COALESCE(SUM(d.montant), 0)  AS montantTotal,
                   COUNT(d.id)          AS nombreDepenses
            FROM depense d
            JOIN category_depense c ON c.id = d.category_id
            JOIN magasin m           ON m.id = d.magasin_id
            WHERE m.entreprise_id = :entrepriseId
              AND d.magasin_id     = :magasinId
              AND d.date_depense  >= CAST(:startDate AS DATE)
              AND d.date_depense  <= CAST(:endDate   AS DATE)
            GROUP BY d.category_id, c.nom
            ORDER BY montantTotal DESC
            """, nativeQuery = true)
    List<DepenseParCategorieProjection> computeByCategory(@Param("magasinId") UUID magasinId,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate,
                                                          @Param("entrepriseId") UUID entrepriseId);
}
