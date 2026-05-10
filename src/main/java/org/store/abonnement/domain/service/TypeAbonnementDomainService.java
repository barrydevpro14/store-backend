package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.TypeAbonnementJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class TypeAbonnementDomainService extends GlobalService<TypeAbonnement, TypeAbonnementJpaRepository> {
    public TypeAbonnementDomainService(TypeAbonnementJpaRepository repository) {
        super(repository);
    }
}
