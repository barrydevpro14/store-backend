package org.store.magasin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record MagasinRequest(
        @NotBlank String nom,
        @NotBlank String adresse,
        String telephone
) {
}
