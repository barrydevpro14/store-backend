package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientJpaRepository;

@Service
public class FactureClientDomainService extends GlobalService<FactureClient, FactureClientJpaRepository> {
    public FactureClientDomainService(FactureClientJpaRepository repository) {
        super(repository);
    }
}
