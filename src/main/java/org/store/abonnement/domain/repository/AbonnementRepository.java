package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbonnementRepository extends BaseRepository<Abonnement> {

    @Query("SELECT COUNT(abonnement) FROM Abonnement abonnement WHERE abonnement.statut = :statut")
    long countByStatut(@Param("statut") AbonnementStatut statut);

    @Query("""
            SELECT abonnement
            FROM Abonnement abonnement
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.statut        = :statut
            ORDER BY abonnement.dateFin DESC NULLS LAST, abonnement.id DESC
            """)
    Optional<Abonnement> findFirstByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId,
                                                       @Param("statut") AbonnementStatut statut);

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
     *
     * Single-row return is guaranteed by the {@code abonnement_one_actif_per_entreprise} partial
     * unique index (V14) + {@link AbonnementDomainService#activate} which EXPIREs any sibling
     * actif=true row before flipping the paid Abonnement to ACTIF. The ORDER BY still ranks
     * ACTIF before TRIAL as a defensive tie-break.
     */
    @Query("""
            SELECT abonnement
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.typePlanAbonnement type
            LEFT JOIN FETCH type.plan
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
    Optional<Abonnement> findCurrentByEntreprise(@Param("entrepriseId") UUID entrepriseId,
                                                 @Param("today") LocalDate today);

    /**
     * EXPIREs every sibling actif=true Abonnement on the entreprise — to be called from
     * {@link org.store.abonnement.domain.service.AbonnementDomainService#activate} before a paid
     * Abonnement is flipped to actif=true, so the {@code abonnement_one_actif_per_entreprise}
     * partial unique index (V14) is preserved.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Abonnement abonnement
            SET abonnement.actif = false,
                abonnement.statut = org.store.abonnement.domain.enums.AbonnementStatut.EXPIRE
            WHERE abonnement.entreprise.id = :entrepriseId
              AND abonnement.id           <> :exceptId
              AND abonnement.actif         = true
            """)
    int expireOtherActifByEntreprise(@Param("entrepriseId") UUID entrepriseId,
                                     @Param("exceptId") UUID exceptId);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.AbonnementResponse(abonnement)
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.typePlanAbonnement type
            LEFT JOIN FETCH type.plan
            LEFT JOIN FETCH abonnement.entreprise
            WHERE (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:statut IS NULL OR abonnement.statut = :statut)
              AND (:planId IS NULL OR type.plan.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', abonnement.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', abonnement.createdAt) <= CAST(:endDate AS date))
            ORDER BY abonnement.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(abonnement)
            FROM Abonnement abonnement
            JOIN abonnement.typePlanAbonnement type
            WHERE (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
              AND (:statut IS NULL OR abonnement.statut = :statut)
              AND (:planId IS NULL OR type.plan.id = :planId)
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
    @Query("SELECT a FROM Abonnement a WHERE a.dateFin IN :dates AND a.statut IN ('ACTIF', 'TRIAL')")
    List<Abonnement> findByDateFinInAndStatutActifOrTrial(@Param("dates") List<LocalDate> dates);
}
