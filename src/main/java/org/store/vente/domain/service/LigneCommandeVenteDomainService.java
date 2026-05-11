package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteRepository;

@Service
public class LigneCommandeVenteDomainService extends GlobalService<LigneCommandeVente, LigneCommandeVenteRepository> {
    public LigneCommandeVenteDomainService(LigneCommandeVenteRepository repository) {
        super(repository);
    }
}
