package org.store.produit.application.dto;

import org.store.produit.domain.model.Quality;

import java.util.UUID;

public record QualityResponse(
        UUID id,
        String libelle,
        String description,
        UUID entrepriseId
) {
    public QualityResponse(Quality quality) {
        this(
                quality.getId(),
                quality.getLibelle(),
                quality.getDescription(),
                quality.getEntreprise().getId()
        );
    }
}
