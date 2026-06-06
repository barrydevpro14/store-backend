package org.store.produit.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryProductRequest(
        @NotBlank @Size(max = 255) String libelle,
        @Size(max = 255) String description
) {
}
