package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.repository.PlanAbonnementTarifRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.Optional;

/**
 * Gère les tarifs (plan + périodicité + prix) du catalogue d'abonnements.
 */
@Service
public class PlanAbonnementTarifDomainService extends GlobalService<PlanAbonnementTarif, PlanAbonnementTarifRepository> {

    public PlanAbonnementTarifDomainService(PlanAbonnementTarifRepository repository) {
        super(repository);
    }

    public Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        return repository.findByPlanAndPeriodicite(plan, periodicite);
    }

    public List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan) {
        return repository.findByPlan(plan);
    }
}
