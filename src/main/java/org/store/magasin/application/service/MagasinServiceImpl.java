package org.store.magasin.application.service;

import org.springframework.stereotype.Service;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.service.MagasinDomainService;

import java.util.UUID;

@Service
public class MagasinServiceImpl implements IMagasinService {

    private final MagasinDomainService magasinDomainService;

    public MagasinServiceImpl(MagasinDomainService magasinDomainService) {
        this.magasinDomainService = magasinDomainService;
    }

    @Override
    public Magasin create(MagasinRequest magasinRequest, Entreprise entreprise) {
        Magasin magasin = new Magasin();
        magasin.setEntreprise(entreprise);
        magasin.setNom(magasinRequest.nom());
        magasin.setAdresse(magasinRequest.adresse());
        return magasinDomainService.save(magasin);
    }

    @Override
    public Magasin findById(UUID id) {
        return magasinDomainService.findById(id);
    }
}
