package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.repository.ClientRepository;

@Service
public class ClientDomainService extends GlobalService<Client, ClientRepository> {
    public ClientDomainService(ClientRepository repository) {
        super(repository);
    }
}
