package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.model.TarifAvecCoupon;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanAbonnementTarifRepository extends BaseRepository<PlanAbonnementTarif> {

    Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    boolean existsByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan);

    @Query("""
            SELECT new org.store.abonnement.domain.model.TarifAvecCoupon(t, c)
            FROM PlanAbonnementTarif t
            LEFT JOIN Coupon c
                ON c.entreprise IS NULL
               AND c.actif = true
               AND (c.planAbonnement IS NULL OR c.planAbonnement.id = t.plan.id)
               AND (c.periodicite IS NULL OR c.periodicite = t.periodicite)
               AND :today BETWEEN c.dateDebut AND c.dateFin
               AND (c.nombreUtilisationsMax = 0 OR c.nombreUtilisations < c.nombreUtilisationsMax)
            WHERE t.plan.id = :planId
              AND t.actif = true
            ORDER BY COALESCE(t.ordre, 0) ASC
            """)
    List<TarifAvecCoupon> findActifWithCoupon(@Param("planId") UUID planId, @Param("today") LocalDate today);
}
