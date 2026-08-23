package org.store.plateforme.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryDepensePlateformeRequest(
        @NotBlank @Size(max = 100) String nom,
        @Size(max = 500) String description,
        Boolean actif
) {
}
