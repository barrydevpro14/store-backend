package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TypePlanAbonnementRepository extends BaseRepository<TypePlanAbonnement> {

    boolean existsByPlanIdAndNom(UUID planId, String nom);

    boolean existsByPlanIdAndNomAndIdNot(UUID planId, String nom, UUID id);

    List<TypePlanAbonnement> findByPlanIdOrderByOrdreAsc(UUID planId);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypePlanAbonnement type
            WHERE type.plan.id = :planId
              AND (:nom IS NULL OR :nom = '' OR LOWER(type.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR type.actif = :actif)
              AND (:recommande IS NULL OR type.recommande = :recommande)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', type.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', type.createdAt) <= CAST(:endDate AS date))
            ORDER BY type.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(type)
            FROM TypePlanAbonnement type
            WHERE type.plan.id = :planId
              AND (:nom IS NULL OR :nom = '' OR LOWER(type.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR type.actif = :actif)
              AND (:recommande IS NULL OR type.recommande = :recommande)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', type.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', type.createdAt) <= CAST(:endDate AS date))
            """)
    Page<SubscriptionTypeResponse> findResponsesByFilter(
            @Param("planId") UUID planId,
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("recommande") Boolean recommande,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypePlanAbonnement type
            WHERE type.plan.id = :planId
              AND type.actif = true
            ORDER BY type.ordre ASC, type.dureeMois ASC
            """)
    List<SubscriptionTypeResponse> findActifResponsesByPlanId(@Param("planId") UUID planId);

    /**
     * Same shape as {@link #findActifResponsesByPlanId(UUID)} but excludes trial-flagged types. Used by
     * the OWNER subscribable catalog so the trial duration is never offered as a paid choice — the only
     * trial type seeds the OWNER's TRIAL Abonnement at signup and is not a valid `subscribe` payload.
     */
    @Query("""
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypePlanAbonnement type
            WHERE type.plan.id = :planId
              AND type.actif = true
              AND type.trial = false
            ORDER BY type.ordre ASC, type.dureeMois ASC
            """)
    List<SubscriptionTypeResponse> findActifNonTrialResponsesByPlanId(@Param("planId") UUID planId);

    /**
     * Returns the trial type — the {@code TypePlanAbonnement} flagged {@code trial=true}. The signup
     * flow binds the TRIAL Abonnement to it. Domain invariant: at most one row exists with
     * {@code trial=true} (enforced by {@code DataInitializer}), so neither ORDER BY nor LIMIT is needed.
     */
    @Query("""
            SELECT type
            FROM TypePlanAbonnement type
            WHERE type.trial = true
            """)
    Optional<TypePlanAbonnement> findByTrial();
}
