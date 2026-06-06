package org.store.depense.application.dto;

import org.store.depense.domain.model.CategoryDepense;

import java.util.UUID;

public record CategoryDepenseSummaryResponse(
        UUID id,
        String nom
) {
    public CategoryDepenseSummaryResponse(CategoryDepense categoryDepense) {
        this(categoryDepense.getId(), categoryDepense.getNom());
    }
}
