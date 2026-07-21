package org.store.inventaire.application.service;

import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.domain.model.Inventaire;

import java.util.Optional;
import java.util.UUID;

public interface IRapportInventaireService {

    void create(Inventaire inventaire, RapportInventaireCommand command);

    Optional<RapportInventaireResponse> findResponseByInventaireId(UUID inventaireId, UUID entrepriseId);
}
