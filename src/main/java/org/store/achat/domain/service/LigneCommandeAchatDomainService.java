package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class LigneCommandeAchatDomainService extends GlobalService<LigneCommandeAchat, LigneCommandeAchatRepository> {
    public LigneCommandeAchatDomainService(LigneCommandeAchatRepository repository) {
        super(repository);
    }
}
