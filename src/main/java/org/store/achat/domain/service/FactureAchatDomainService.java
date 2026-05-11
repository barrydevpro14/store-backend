package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class FactureAchatDomainService extends GlobalService<FactureAchat, FactureAchatRepository> {
    public FactureAchatDomainService(FactureAchatRepository repository) {
        super(repository);
    }
}
