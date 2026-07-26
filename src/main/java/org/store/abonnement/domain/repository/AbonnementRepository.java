package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbonnementRepository extends BaseRepository<Abonnement> {

    @Query("SELECT COUNT(abonnement) FROM Abonnement abonnement WHERE abonnement.statut = :statut")
    long countByStatut(@Param("statut") AbonnementStatut statut);

    @Query("SELECT COUNT(a) > 0 FROM Abonnement a WHERE a.entreprise.id = :entrepriseId AND a.statut = :statut")
    boolean existsByEntrepriseIdAndStatut(@Param("entrepriseId") UUID entrepriseId,
                                          @Param("statut") AbonnementStatut statut);

    /**
     * Returns the most recently created SUSPENDU or INACTIF subscription for the enterprise.
     * Used to resolve the JWT restricted scope: SUSPENDU = non-payment, INACTIF = admin-deactivated.
     * Pageable.ofSize(1) ensures only the newest row is returned.
     */
    @Query("""
            SELECT a FROM Abonnement a
            WHERE a.entreprise.id = :entrepriseId
              AND a.statut IN (
                  org.store.abonnement.domain.enums.AbonnementStatut.SUSPENDU,
                  org.store.abonnement.domain.enums.AbonnementStatut.INACTIF
              )
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<Abonnement> findLatestSuspendedOrInactif(@Param("entrepriseId") UUID entrepriseId,
                                                  Pageable pageable);

    @Query("""
            SELECT abonnement
            FROM Abonnement abonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.statut        = :statut
            ORDER BY abonnement.dateFin DESC NULLS LAST, abonnement.id DESC
            """)
    Optional<Abonnement> findFirstByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId,
                                                       @Param("statut") AbonnementStatut statut);

    /**
     * Loads the entreprise's pending (EN_ATTENTE) Abonnement as a projected response, joins fetched
     * for a single round-trip. At most one EN_ATTENTE row exists per entreprise (subscribe guard).
     */
    @Query("""
            SELECT new org.store.abonnement.application.dto.AbonnementResponse(abonnement)
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            LEFT JOIN FETCH abonnement.planAbonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.statut        = org.store.abonnement.domain.enums.AbonnementStatut.EN_ATTENTE
            """)
    Optional<AbonnementResponse> findPendingResponseByEntreprise(@Param("entrepriseId") UUID entrepriseId);

    @Query("""
            SELECT MAX(abonnement.dateFin)
            FROM Abonnement abonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.statut        = org.store.abonnement.domain.enums.AbonnementStatut.ACTIF
              AND abonnement.id           <> :excludeAbonnementId
            """)
    Optional<LocalDate> findLatestActifDateFin(@Param("entrepriseId") UUID entrepriseId,
                                               @Param("excludeAbonnementId") UUID excludeAbonnementId);

    /**
     * Picks the entreprise's "current" subscription: ACTIF first, otherwise a TRIAL whose
     * {@code dateFin >= today}. Expired trials and EN_ATTENTE rows are excluded.
     * Returns a List + Pageable so callers can request at most 1 row and never hit
     * NonUniqueResultException if a data anomaly (ACTIF + valid TRIAL) slips through.
     */
    @Query("""
            SELECT abonnement
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.planAbonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND (
                   abonnement.statut = org.store.abonnement.domain.enums.AbonnementStatut.ACTIF
                OR (
                       abonnement.statut = org.store.abonnement.domain.enums.AbonnementStatut.TRIAL
                   AND (abonnement.dateFin IS NULL OR abonnement.dateFin >= :today)
                   )
              )
            ORDER BY
                CASE abonnement.statut
                    WHEN org.store.abonnement.domain.enums.AbonnementStatut.ACTIF THEN 0
                    WHEN org.store.abonnement.domain.enums.AbonnementStatut.TRIAL THEN 1
                    ELSE 2 END,
                abonnement.dateFin DESC NULLS LAST,
                abonnement.id DESC
            """)
    List<Abonnement> findCurrentByEntreprise(@Param("entrepriseId") UUID entrepriseId,
                                             @Param("today") LocalDate today,
                                             Pageable pageable);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.AbonnementResponse(abonnement)
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.planAbonnement
            LEFT JOIN FETCH abonnement.entreprise
            WHERE (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:statut IS NULL OR abonnement.statut = :statut)
              AND (:planId IS NULL OR abonnement.planAbonnement.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', abonnement.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', abonnement.createdAt) <= CAST(:endDate AS date))
            ORDER BY abonnement.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(abonnement)
            FROM Abonnement abonnement
            WHERE (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:statut IS NULL OR abonnement.statut = :statut)
              AND (:planId IS NULL OR abonnement.planAbonnement.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', abonnement.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', abonnement.createdAt) <= CAST(:endDate AS date))
            """)
    Page<AbonnementResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("statut") AbonnementStatut statut,
            @Param("planId") UUID planId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    /** Finds active/trial subscriptions expiring exactly on the given date (for 1/3/5-day alerts). */
    @Query("SELECT a FROM Abonnement a WHERE a.dateFin = :date AND a.statut IN ('ACTIF', 'TRIAL')")
    List<Abonnement> findByDateFinAndStatutActifOrTrial(@Param("date") LocalDate date);

    /** Finds active/trial subscriptions expiring on any of the given alert dates (today+1, today+3, today+5). */
    @Query("SELECT a FROM Abonnement a WHERE a.dateFin IN :dates AND a.statut IN :statuts")
    List<Abonnement> findByDateFinInAndStatutActifOrTrial(@Param("dates") List<LocalDate> dates , List<AbonnementStatut> statuts);

    /** Counts all Abonnements whose createdAt falls within the given date range (both bounds optional). */
    @Query("""
            SELECT COUNT(a)
            FROM Abonnement a
            WHERE (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', a.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', a.createdAt) <= CAST(:endDate AS date))
            """)
    long countByCreatedBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /** Finds ACTIF abonnements whose dateFin = targetDate with no FACTURE_GENEREE/EN_ATTENTE_VALIDATION payment at that deadline (anti-duplicate guard). */
    @Query("""
            SELECT a FROM Abonnement a
            LEFT JOIN FETCH a.planAbonnement
            LEFT JOIN FETCH a.entreprise
            WHERE a.statut = org.store.abonnement.domain.enums.AbonnementStatut.ACTIF
              AND a.dateFin = :targetDate
              AND NOT EXISTS (
                  SELECT 1 FROM PaiementAbonnement p
                  WHERE p.abonnement = a
                    AND p.dateEcheance = a.dateFin
                    AND p.statut IN (
                        org.store.abonnement.domain.enums.StatutPaiementAbonnement.FACTURE_GENEREE,
                        org.store.abonnement.domain.enums.StatutPaiementAbonnement.EN_ATTENTE_VALIDATION
                    )
              )
            """)
    List<Abonnement> findAbonnementsToFacture(@Param("targetDate") LocalDate targetDate);
}
