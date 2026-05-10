package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireJpaRepository;

@Service
public class ProprietaireDomainService extends GlobalService<Proprietaire, ProprietaireJpaRepository> {
    public ProprietaireDomainService(ProprietaireJpaRepository repository) {
        super(repository);
    }
}
