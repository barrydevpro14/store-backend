package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PaiementAbonnementDetailsResponse;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.PaiementAbonnementStatsResponse;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.common.repository.BaseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaiementAbonnementRepository extends BaseRepository<PaiementAbonnement> {

    /**
     * Loads the entreprise's current unpaid factures (FACTURE_GENEREE or EN_RETARD) with their
     * full preuve history fetch-joined, most recent first — used by the OWNER dashboard
     * regardless of the abonnement's own statut (EN_ATTENTE, ACTIF, or SUSPENDU can all have an
     * unpaid facture). Built directly via PaiementAbonnementDetailsResponse(PaiementAbonnement).
     */
    @Query("""
            SELECT new org.store.abonnement.application.dto.PaiementAbonnementDetailsResponse(paiement)
            FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.preuves
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            LEFT JOIN FETCH abonnement.planAbonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND paiement.statut IN (
                  org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE,
                  org.store.abonnement.domain.enums.StatutPaiementAbonnement.EN_RETARD
              )
            ORDER BY paiement.createdAt DESC
            """)
    List<PaiementAbonnementDetailsResponse> findCurrentUnpaidFacturesByEntreprise(@Param("entrepriseId") UUID entrepriseId,
                                                                                   Pageable pageable);

    @Query("SELECT COUNT(paiement) FROM PaiementAbonnement paiement WHERE paiement.statut = :statut")
    long countByStatut(@Param("statut") StatutPaiementAbonnement statut);

    @Query("""
            SELECT COALESCE(SUM(paiement.montantFinal), 0)
            FROM PaiementAbonnement paiement
            WHERE paiement.statut = org.store.abonnement.domain.enums.StatutPaiementAbonnement.VALIDE
              AND paiement.datePaiement >= :startOfYear
              AND paiement.datePaiement <  :startOfNextYear
            """)
    BigDecimal sumValidatedRevenueForYear(@Param("startOfYear") LocalDate startOfYear,
                                          @Param("startOfNextYear") LocalDate startOfNextYear);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.PaiementAbonnementResponse(paiement)
            FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            LEFT JOIN FETCH abonnement.planAbonnement
            WHERE (:statut IS NULL OR paiement.statut = :statut)
              AND (:abonnementId IS NULL OR abonnement.id = :abonnementId)
              AND (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:startDate IS NULL OR :startDate = '' OR paiement.dateEcheance >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR paiement.dateEcheance <= CAST(:endDate AS date))
            ORDER BY paiement.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(paiement)
            FROM PaiementAbonnement paiement
            JOIN paiement.abonnement abonnement
            WHERE (:statut IS NULL OR paiement.statut = :statut)
              AND (:abonnementId IS NULL OR abonnement.id = :abonnementId)
              AND (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:startDate IS NULL OR :startDate = '' OR paiement.dateEcheance >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR paiement.dateEcheance <= CAST(:endDate AS date))
            """)
    Page<PaiementAbonnementResponse> findResponsesByFilter(
            @Param("statut") StatutPaiementAbonnement statut,
            @Param("abonnementId") UUID abonnementId,
            @Param("entrepriseId") UUID entrepriseId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT COUNT(paiement)
            FROM PaiementAbonnement paiement
            WHERE (:statut IS NULL OR paiement.statut = :statut)
              AND (:startDate IS NULL  OR paiement.dateEcheance >= :startDate)
              AND (:endDate   IS NULL  OR paiement.dateEcheance <= :endDate)
            """)
    long countByStatutAndCreatedBetween(@Param("statut") StatutPaiementAbonnement statut,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /** Finds invoices that are overdue: FACTURE_GENEREE with dateEcheance < today. */
    @Query("""
            SELECT paiement FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            WHERE paiement.statut = org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE
              AND paiement.dateEcheance < :today
            """)
    List<PaiementAbonnement> findOverdueInvoices(@Param("today") LocalDate today);

    @Query("""
            SELECT COALESCE(SUM(paiement.montantFinal), 0)
            FROM PaiementAbonnement paiement
            WHERE paiement.statut = org.store.abonnement.domain.enums.StatutPaiementAbonnement.VALIDE
              AND (:startDate IS NULL  OR paiement.dateEcheance >= :startDate)
              AND (:endDate   IS NULL OR paiement.dateEcheance <= :endDate)
            """)
    BigDecimal sumValidatedRevenueForPeriod(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("""
    SELECT new org.store.abonnement.application.dto.PaiementAbonnementStatsResponse(
        CAST(COUNT(CASE
            WHEN paiement.statut =
                org.store.abonnement.domain.enums.StatutPaiementAbonnement.VALIDE
            THEN paiement.id
        END) AS long),
        COALESCE(
            SUM(
                CASE
                    WHEN paiement.statut =
                        org.store.abonnement.domain.enums.StatutPaiementAbonnement.VALIDE
                    THEN paiement.montantFinal
                    ELSE 0
                END
            ),
            0
        )
    )
    FROM PaiementAbonnement paiement
    WHERE
        (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE' , paiement.dateEcheance) >= CAST(:startDate as DATE))
        AND
        (:endDate IS NULL OR :endDate = '' OR FUNCTION('DATE' , paiement.dateEcheance) <= CAST(:endDate as DATE))
    """)
    PaiementAbonnementStatsResponse getStatistiquesPaiement(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /** Finds FACTURE_GENEREE invoices with dateEcheance on any of the given alert dates. */
    @Query("""
            SELECT paiement FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            WHERE paiement.statut = org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE
              AND paiement.dateEcheance IN :dates
            """)
    List<PaiementAbonnement> findFacturesAbonnementDues(@Param("dates") List<LocalDate> dates);

    /**
     * Returns the most recent FACTURE_GENEREE or EN_RETARD invoice for a given abonnement —
     * i.e. invoices not yet submitted by the owner. Used to recalculate amounts on plan change.
     */
    @Query("""
            SELECT paiement FROM PaiementAbonnement paiement
            WHERE paiement.abonnement.id = :abonnementId
              AND paiement.statut IN (
                  org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE,
                  org.store.abonnement.domain.enums.StatutPaiementAbonnement.EN_RETARD
              )
            ORDER BY paiement.createdAt DESC
            """)
    List<PaiementAbonnement> findFacturesNonPayeesByAbonnement(@Param("abonnementId") UUID abonnementId, Pageable pageable);
}
