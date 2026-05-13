package org.store.produit.application.dto;

import org.store.produit.domain.model.Quality;

import java.util.UUID;

public record QualitySummaryResponse(
        UUID id,
        String libelle
) {
    public QualitySummaryResponse(Quality quality) {
        this(quality.getId(), quality.getLibelle());
    }
}
