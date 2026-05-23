package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AbonnementRepository extends BaseRepository<Abonnement> {

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

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.AbonnementResponse(abonnement)
            FROM Abonnement abonnement
            LEFT JOIN FETCH abonnement.typePlanAbonnement type
            LEFT JOIN FETCH type.plan
            LEFT JOIN FETCH abonnement.entreprise
            WHERE (:#{#filter.entrepriseId}        IS NULL OR abonnement.entreprise.id = :#{#filter.entrepriseId})
              AND (:#{#filter.statutAsEnum()}      IS NULL OR abonnement.statut        = :#{#filter.statutAsEnum()})
              AND (:#{#filter.planId}              IS NULL OR type.plan.id             = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR abonnement.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR abonnement.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY abonnement.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(abonnement)
            FROM Abonnement abonnement
            JOIN abonnement.typePlanAbonnement type
            WHERE (:#{#filter.entrepriseId}        IS NULL OR abonnement.entreprise.id = :#{#filter.entrepriseId})
              AND (:#{#filter.statutAsEnum()}      IS NULL OR abonnement.statut        = :#{#filter.statutAsEnum()})
              AND (:#{#filter.planId}              IS NULL OR type.plan.id             = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR abonnement.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR abonnement.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<AbonnementResponse> findResponsesByFilter(@Param("filter") AbonnementFilter filter, Pageable pageable);
}
