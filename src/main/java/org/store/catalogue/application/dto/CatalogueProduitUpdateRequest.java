package org.store.catalogue.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CatalogueProduitUpdateRequest(
        @NotBlank String reference,
        @NotBlank String libelle,
        String categorie,
        String description
) {
}
