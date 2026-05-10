package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class PlanAbonnementDomainService extends GlobalService<PlanAbonnement, PlanAbonnementJpaRepository> {
    public PlanAbonnementDomainService(PlanAbonnementJpaRepository repository) {
        super(repository);
    }
}
