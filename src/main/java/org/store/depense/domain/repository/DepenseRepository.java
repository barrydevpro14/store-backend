package org.store.depense.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
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
              AND depense.magasin.id = :magasinId
              AND (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            ORDER BY depense.dateDepense DESC
            """)
    Page<DepenseResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.depense.application.dto.DepenseTotalResponse(
                :magasinId,
                COALESCE(SUM(depense.montant), 0),
                COUNT(depense)
            )
            FROM Depense depense
            WHERE depense.magasin.entreprise.id = :entrepriseId
              AND depense.magasin.id = :magasinId
              AND (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            """)
    DepenseTotalResponse computeTotal(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * Aggregation par catégorie via SQL natif — dates passées en String ISO castées côté SQL.
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
