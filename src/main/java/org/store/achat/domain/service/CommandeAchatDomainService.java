package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatJpaRepository;
import org.store.common.service.GlobalService;

@Service
public class CommandeAchatDomainService extends GlobalService<CommandeAchat, CommandeAchatJpaRepository> {
    public CommandeAchatDomainService(CommandeAchatJpaRepository repository) {
        super(repository);
    }
}
