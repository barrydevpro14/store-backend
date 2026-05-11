package org.store.magasin.application.service;

import org.springframework.stereotype.Service;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

@Service
public class MagasinServiceImpl implements IMagasinService {

    private final MagasinRepository magasinRepository;

    public MagasinServiceImpl(MagasinRepository magasinRepository) {
        this.magasinRepository = magasinRepository;
    }

    @Override
    public Magasin create(MagasinRequest magasinRequest, Entreprise entreprise) {
        Magasin magasin = new Magasin();
        magasin.setEntreprise(entreprise);
        magasin.setNom(magasinRequest.nom());
        magasin.setAdresse(magasinRequest.adresse());
        return magasinRepository.save(magasin);
    }
}
