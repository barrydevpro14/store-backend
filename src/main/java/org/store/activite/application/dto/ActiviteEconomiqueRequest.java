package org.store.activite.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActiviteEconomiqueRequest(
        @NotBlank @Size(max = 150) String libelle,
        @Size(max = 500) String description
) {
}
