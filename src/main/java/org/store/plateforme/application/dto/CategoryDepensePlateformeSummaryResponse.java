package org.store.plateforme.application.dto;

import org.store.plateforme.domain.model.CategoryDepensePlateforme;

import java.util.UUID;

public record CategoryDepensePlateformeSummaryResponse(
        UUID id,
        String nom
) {
    public CategoryDepensePlateformeSummaryResponse(CategoryDepensePlateforme category) {
        this(category.getId(), category.getNom());
    }
}
