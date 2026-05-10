package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.repository.AbonnementJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class AbonnementDomainService extends GlobalService<Abonnement, AbonnementJpaRepository> {
    public AbonnementDomainService(AbonnementJpaRepository repository) {
        super(repository);
    }
}
