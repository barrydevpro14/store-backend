package org.store.entreprise.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;

@Service
public class EntrepriseDomainService extends GlobalService<Entreprise, EntrepriseRepository> {
    public EntrepriseDomainService(EntrepriseRepository repository) {
        super(repository);
    }
}
