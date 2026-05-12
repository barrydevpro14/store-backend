package org.store.magasin.application.service;

import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public interface IMagasinService {

    Magasin create(MagasinRequest magasinRequest, Entreprise entreprise);

    Magasin findById(UUID id);
}
