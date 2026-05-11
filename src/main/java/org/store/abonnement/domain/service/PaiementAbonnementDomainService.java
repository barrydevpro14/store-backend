package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.repository.PaiementAbonnementRepository;
import org.store.common.service.GlobalService;

@Service
public class PaiementAbonnementDomainService extends GlobalService<PaiementAbonnement, PaiementAbonnementRepository> {
    public PaiementAbonnementDomainService(PaiementAbonnementRepository repository) {
        super(repository);
    }
}
