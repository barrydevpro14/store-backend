package org.store.inventaire.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.repository.InventaireJpaRepository;

@Service
public class InventaireDomainService extends GlobalService<Inventaire, InventaireJpaRepository> {
    public InventaireDomainService(InventaireJpaRepository repository) {
        super(repository);
    }
}
