package org.store.entreprise.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntrepriseRequest(
        @NotBlank String sigle,
        @NotBlank String raisonSociale,
        String ninea,
        String rccm,
        @NotBlank String adresse,
        @Size(min = 3, max = 3) String currency
) {
}
