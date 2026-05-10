package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class FournisseurDomainService extends GlobalService<Fournisseur, FournisseurJpaRepository> {
    public FournisseurDomainService(FournisseurJpaRepository repository) {
        super(repository);
    }
}
