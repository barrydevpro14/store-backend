package org.store.magasin.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Entreprise;
import org.store.magasin.domain.repository.EntrepriseRepository;

@Service
public class EntrepriseDomainService extends GlobalService<Entreprise, EntrepriseRepository> {
    public EntrepriseDomainService(EntrepriseRepository repository) {
        super(repository);
    }
}
