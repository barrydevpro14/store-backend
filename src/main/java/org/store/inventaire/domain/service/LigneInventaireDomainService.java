package org.store.inventaire.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.repository.LigneInventaireRepository;

@Service
public class LigneInventaireDomainService extends GlobalService<LigneInventaire, LigneInventaireRepository> {
    public LigneInventaireDomainService(LigneInventaireRepository repository) {
        super(repository);
    }
}
