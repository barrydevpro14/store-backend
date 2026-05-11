package org.store.magasin.application.service;

import org.store.magasin.application.dto.MagasinRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;

public interface IMagasinService {

    Magasin create(MagasinRequest magasinRequest, Entreprise entreprise);
}
