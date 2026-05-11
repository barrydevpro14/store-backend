package org.store.inventaire.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.repository.InventaireRepository;

@Service
public class InventaireDomainService extends GlobalService<Inventaire, InventaireRepository> {
    public InventaireDomainService(InventaireRepository repository) {
        super(repository);
    }
}
