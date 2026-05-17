package org.store.magasin.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

import java.util.UUID;

@Service
public class MagasinDomainService extends GlobalService<Magasin, MagasinRepository> {
    public MagasinDomainService(MagasinRepository repository) {
        super(repository);
    }

    public Magasin create(MagasinRequest magasinRequest, Entreprise entreprise) {
        Magasin magasin = new Magasin();
        magasin.setEntreprise(entreprise);
        magasin.setNom(magasinRequest.nom());
        magasin.setAdresse(magasinRequest.adresse());
        magasin.setActif(true);
        return save(magasin);
    }

    /** Listing pagine filtre (nom LIKE insensitive, actif) scope entreprise. */
    public Page<MagasinResponse> findResponsesByFilter(MagasinFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    /** Pose ou remplace le logo. orphanRemoval supprime auto l'ancienne PieceJointe. */
    public Magasin setLogo(Magasin magasin, PieceJointe logo) {
        magasin.setLogo(logo);
        return save(magasin);
    }

    /** Supprime le logo (orphanRemoval supprime la PieceJointe). */
    public Magasin clearLogo(Magasin magasin) {
        magasin.setLogo(null);
        return save(magasin);
    }
}
