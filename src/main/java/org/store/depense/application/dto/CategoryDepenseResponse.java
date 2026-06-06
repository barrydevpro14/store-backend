package org.store.depense.application.dto;

import org.store.depense.domain.model.CategoryDepense;

import java.util.UUID;

public record CategoryDepenseResponse(
        UUID id,
        String nom,
        String description,
        boolean actif
) {
    public CategoryDepenseResponse(CategoryDepense categoryDepense) {
        this(categoryDepense.getId(), categoryDepense.getNom(), categoryDepense.getDescription(), categoryDepense.isActif());
    }
}
