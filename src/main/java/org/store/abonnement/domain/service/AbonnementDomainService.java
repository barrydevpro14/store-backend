package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.common.service.GlobalService;

@Service
public class AbonnementDomainService extends GlobalService<Abonnement, AbonnementRepository> {
    public AbonnementDomainService(AbonnementRepository repository) {
        super(repository);
    }
}
