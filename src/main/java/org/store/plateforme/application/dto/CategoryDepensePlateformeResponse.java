package org.store.plateforme.application.dto;

import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public record CategoryDepensePlateformeResponse(
        UUID id,
        String nom,
        String description,
        boolean actif
) {
    public CategoryDepensePlateformeResponse(CategoryDepensePlateforme category) {
        this(category.getId(), category.getNom(), category.getDescription(), category.isActif());
    }
}
