package org.store.magasin.application.dto;

import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public record MagasinSummaryResponse(
        UUID id,
        String nom
) {
    public MagasinSummaryResponse(Magasin magasin) {
        this(magasin.getId(), magasin.getNom());
    }
}
