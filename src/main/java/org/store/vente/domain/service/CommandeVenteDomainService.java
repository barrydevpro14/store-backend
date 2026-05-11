package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

@Service
public class CommandeVenteDomainService extends GlobalService<CommandeVente, CommandeVenteRepository> {
    public CommandeVenteDomainService(CommandeVenteRepository repository) {
        super(repository);
    }
}
