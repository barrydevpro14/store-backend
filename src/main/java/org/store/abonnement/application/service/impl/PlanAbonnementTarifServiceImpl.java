package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.service.PlanAbonnementTarifDomainService;

import java.util.List;
import java.util.Optional;

/**
 * Expose les tarifs du catalogue (plan + périodicité + prix) à la couche application.
 */
@Service
@Transactional(readOnly = true)
public class PlanAbonnementTarifServiceImpl implements IPlanAbonnementTarifService {

    private final PlanAbonnementTarifDomainService planAbonnementTarifDomainService;

    public PlanAbonnementTarifServiceImpl(PlanAbonnementTarifDomainService planAbonnementTarifDomainService) {
        this.planAbonnementTarifDomainService = planAbonnementTarifDomainService;
    }

    /** Délègue la résolution plan + périodicité → tarif au domain service. */
    @Override
    public Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite) {
        return planAbonnementTarifDomainService.findByPlanAndPeriodicite(plan, periodicite);
    }

    /** Retourne tous les tarifs d'un plan. */
    @Override
    public List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan) {
        return planAbonnementTarifDomainService.findByPlan(plan);
    }
}
