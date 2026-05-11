package org.store.achat.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatRepository;
import org.store.common.service.GlobalService;

@Service
public class CommandeAchatDomainService extends GlobalService<CommandeAchat, CommandeAchatRepository> {
    public CommandeAchatDomainService(CommandeAchatRepository repository) {
        super(repository);
    }
}
