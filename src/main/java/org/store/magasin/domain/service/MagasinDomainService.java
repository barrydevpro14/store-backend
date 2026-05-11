package org.store.magasin.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

@Service
public class MagasinDomainService extends GlobalService<Magasin, MagasinRepository> {
    public MagasinDomainService(MagasinRepository repository) {
        super(repository);
    }
}
