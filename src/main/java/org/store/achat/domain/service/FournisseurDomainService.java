package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurRepository;
import org.store.common.service.GlobalService;

@Service
public class FournisseurDomainService extends GlobalService<Fournisseur, FournisseurRepository> {
    public FournisseurDomainService(FournisseurRepository repository) {
        super(repository);
    }
}
