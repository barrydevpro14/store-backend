package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteJpaRepository;

@Service
public class LigneCommandeVenteDomainService extends GlobalService<LigneCommandeVente, LigneCommandeVenteJpaRepository> {
    public LigneCommandeVenteDomainService(LigneCommandeVenteJpaRepository repository) {
        super(repository);
    }
}
