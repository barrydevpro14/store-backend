package org.store.abonnement.application.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.common.exceptions.EntityException;

@Service
public class PlanAbonnementServiceImpl implements IPlanAbonnementService {

    private final PlanAbonnementRepository planAbonnementRepository;

    public PlanAbonnementServiceImpl(PlanAbonnementRepository planAbonnementRepository) {
        this.planAbonnementRepository = planAbonnementRepository;
    }

    @Override
    public PlanAbonnement findFirstTrialActif() {
        return planAbonnementRepository.findFirstByTrialTrueAndActifTrue()
                .orElseThrow(() -> new EntityException("plan.trial.notFound"));
    }
}
