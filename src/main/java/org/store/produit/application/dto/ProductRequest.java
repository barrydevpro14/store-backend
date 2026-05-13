package org.store.produit.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = 255) String nom,
        @NotBlank @Size(max = 255) String reference,
        @Size(max = 1000) String description,
        @NotNull UUID categoryProductId,
        @NotNull UUID qualityId
) {
}
