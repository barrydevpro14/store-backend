package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.repository.PaiementAbonnementJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class PaiementAbonnementDomainService extends GlobalService<PaiementAbonnement, PaiementAbonnementJpaRepository> {
    public PaiementAbonnementDomainService(PaiementAbonnementJpaRepository repository) {
        super(repository);
    }
}
