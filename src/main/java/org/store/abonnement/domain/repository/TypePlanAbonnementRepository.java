package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
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

    @Query("""
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypePlanAbonnement type
            WHERE type.plan.id = :planId
              AND (:#{#filter.nom}        IS NULL OR LOWER(type.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}      IS NULL OR type.actif      = :#{#filter.actif})
              AND (:#{#filter.recommande} IS NULL OR type.recommande = :#{#filter.recommande})
            """)
    Page<SubscriptionTypeResponse> findResponsesByFilter(@Param("planId") UUID planId,
                                                        @Param("filter") SubscriptionTypeFilter filter,
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
