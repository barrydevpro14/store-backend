package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.TypeAbonnementRepository;
import org.store.common.service.GlobalService;

@Service
public class TypeAbonnementDomainService extends GlobalService<TypeAbonnement, TypeAbonnementRepository> {
    public TypeAbonnementDomainService(TypeAbonnementRepository repository) {
        super(repository);
    }
}
