package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class FactureAchatDomainService extends GlobalService<FactureAchat, FactureAchatJpaRepository> {
    public FactureAchatDomainService(FactureAchatJpaRepository repository) {
        super(repository);
    }
}
