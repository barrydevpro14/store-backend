package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByAbonnementIdAndStatut(UUID abonnementId, StatutPaiementAbonnement statut);

    /**
     * Loads the entreprise's latest EN_ATTENTE_VALIDATION Paiement attached to its pending
     * (EN_ATTENTE) Abonnement, projected as a response. Returns empty when the OWNER hasn't
     * submitted a paiement yet — used by the frontend to toggle the "soumettre" CTA.
     */
    @Query("""
            SELECT new org.store.abonnement.application.dto.PaiementAbonnementResponse(paiement)
            FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            LEFT JOIN FETCH abonnement.planAbonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.statut        = org.store.abonnement.domain.enums.AbonnementStatut.EN_ATTENTE
              AND paiement.statut          = org.store.abonnement.domain.enums.StatutPaiementAbonnement.EN_ATTENTE_VALIDATION
            ORDER BY paiement.createdAt DESC
            """)
    List<PaiementAbonnementResponse> findPendingResponsesByEntreprise(@Param("entrepriseId") UUID entrepriseId,
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

    /** Counts payments matching an optional statut and optional dateEcheance range. */
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

    /** Finds invoices that are overdue: FACTURE_GENEREE or EN_ATTENTE_VALIDATION with dateEcheance < today. */
    @Query("""
            SELECT paiement FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            WHERE paiement.statut IN (
                org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE,
                org.store.abonnement.domain.enums.StatutPaiementAbonnement.EN_ATTENTE_VALIDATION
            )
            AND paiement.dateEcheance < :today
            """)
    List<PaiementAbonnement> findOverdueInvoices(@Param("today") LocalDate today);

    /** Sums montantFinal of VALIDE payments whose dateEcheance falls within the given period. */
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

        CAST(COUNT(CASE
            WHEN paiement.statut =
                org.store.abonnement.domain.enums.StatutPaiementAbonnement.REJETE
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
}
