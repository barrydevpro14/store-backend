package org.store.entreprise.application.dto;

import jakarta.validation.constraints.NotBlank;

public record EntrepriseRequest(
        @NotBlank String sigle,
        @NotBlank String raisonSociale,
        String ninea,
        String rccm,
        @NotBlank String adresse
) {
}
