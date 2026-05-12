package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.common.service.GlobalService;

import java.util.Optional;

@Service
public class PlanAbonnementDomainService extends GlobalService<PlanAbonnement, PlanAbonnementRepository> {
    public PlanAbonnementDomainService(PlanAbonnementRepository repository) {
        super(repository);
    }

    public Optional<PlanAbonnement> findFirstTrialActif() {
        return repository.findFirstByTrialTrueAndActifTrue();
    }
}
