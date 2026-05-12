package org.store.abonnement.application.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.common.exceptions.EntityException;

@Service
public class PlanAbonnementServiceImpl implements IPlanAbonnementService {

    private final PlanAbonnementDomainService planAbonnementDomainService;

    public PlanAbonnementServiceImpl(PlanAbonnementDomainService planAbonnementDomainService) {
        this.planAbonnementDomainService = planAbonnementDomainService;
    }

    @Override
    public PlanAbonnement findFirstTrialActif() {
        return planAbonnementDomainService.findFirstTrialActif()
                .orElseThrow(() -> new EntityException("plan.trial.notFound"));
    }
}
