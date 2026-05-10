package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class LigneCommandeAchatDomainService extends GlobalService<LigneCommandeAchat, LigneCommandeAchatJpaRepository> {
    public LigneCommandeAchatDomainService(LigneCommandeAchatJpaRepository repository) {
        super(repository);
    }
}
