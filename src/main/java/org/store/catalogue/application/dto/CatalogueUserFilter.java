package org.store.catalogue.application.dto;

import jakarta.validation.constraints.Min;

public record CatalogueUserFilter(
        String reference,
        String libelle,
        String categorie,
        String createdStartDate,
        String createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {}
