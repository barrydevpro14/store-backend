package org.store.magasin.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinJpaRepository;

@Service
public class MagasinDomainService extends GlobalService<Magasin, MagasinJpaRepository> {
    public MagasinDomainService(MagasinJpaRepository repository) {
        super(repository);
    }
}
