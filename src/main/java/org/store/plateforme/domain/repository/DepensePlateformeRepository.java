package org.store.plateforme.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.domain.model.DepensePlateforme;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepensePlateformeRepository extends BaseRepository<DepensePlateforme> {

    @Query("""
            SELECT new org.store.plateforme.application.dto.DepensePlateformeResponse(depense)
            FROM DepensePlateforme depense
            WHERE (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:countryId IS NULL OR depense.country.id = :countryId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            ORDER BY depense.dateDepense DESC
            """)
    Page<DepensePlateformeResponse> findResponsesByFilter(
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("countryId") UUID countryId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.plateforme.application.dto.DepensePlateformeTotalResponse(
                COALESCE(SUM(depense.montant), 0),
                COUNT(depense)
            )
            FROM DepensePlateforme depense
            WHERE (:categoryId IS NULL OR depense.category.id = :categoryId)
              AND (:moyenPaiementId IS NULL OR depense.modePaiement.id = :moyenPaiementId)
              AND (:countryId IS NULL OR depense.country.id = :countryId)
              AND (:libelle IS NULL OR :libelle = '' OR LOWER(depense.libelle) LIKE :libellePattern)
              AND (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
            """)
    DepensePlateformeTotalResponse computeTotal(
            @Param("categoryId") UUID categoryId,
            @Param("moyenPaiementId") UUID moyenPaiementId,
            @Param("countryId") UUID countryId,
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /** Simple period+country sum, used by the reporting endpoint (no category/moyen/libelle filters there). */
    @Query("""
            SELECT COALESCE(SUM(depense.montant), 0)
            FROM DepensePlateforme depense
            WHERE (:startDate IS NULL OR :startDate = '' OR depense.dateDepense >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR depense.dateDepense <= CAST(:endDate AS date))
              AND (:countryId IS NULL OR depense.country.id = :countryId)
            """)
    BigDecimal sumByPeriod(@Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("countryId") UUID countryId);
}
