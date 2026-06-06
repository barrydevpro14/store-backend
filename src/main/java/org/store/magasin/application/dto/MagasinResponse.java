package org.store.magasin.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.magasin.presentation.MagasinController;

import java.util.UUID;

public record MagasinResponse(
        UUID id,
        String nom,
        String adresse,
        boolean actif,
        UUID entrepriseId,
        String logo
) {
    public MagasinResponse(Magasin magasin) {
        this(
                magasin.getId(),
                magasin.getNom(),
                magasin.getAdresse(),
                magasin.isActif(),
                magasin.getEntreprise().getId(),
                magasin.getLogo() != null ? MagasinController.BASE_PATH + "/" + magasin.getId() + "/logo" : null
        );
    }
}
