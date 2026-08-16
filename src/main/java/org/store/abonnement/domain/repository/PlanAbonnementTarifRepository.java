package org.store.abonnement.domain.repository;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface PlanAbonnementTarifRepository extends BaseRepository<PlanAbonnementTarif> {

    Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    boolean existsByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan);
}
