package org.store.magasin.application.dto;

import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public record MagasinResponse(
        UUID id,
        String nom,
        String adresse,
        boolean actif,
        UUID entrepriseId
) {
    public MagasinResponse(Magasin magasin) {
        this(
                magasin.getId(),
                magasin.getNom(),
                magasin.getAdresse(),
                magasin.isActif(),
                magasin.getEntreprise().getId()
        );
    }
}
