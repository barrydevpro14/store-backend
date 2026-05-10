package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.repository.PaiementAchatJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class PaiementAchatDomainService extends GlobalService<PaiementAchat, PaiementAchatJpaRepository> {
    public PaiementAchatDomainService(PaiementAchatJpaRepository repository) {
        super(repository);
    }
}
