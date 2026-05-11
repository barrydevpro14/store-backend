package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.repository.PaiementAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class PaiementAchatDomainService extends GlobalService<PaiementAchat, PaiementAchatRepository> {
    public PaiementAchatDomainService(PaiementAchatRepository repository) {
        super(repository);
    }
}
