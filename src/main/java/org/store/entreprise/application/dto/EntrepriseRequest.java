package org.store.entreprise.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EntrepriseRequest(
        @NotBlank String sigle,
        @NotBlank String raisonSociale,
        String ninea,
        String rccm,
        @NotBlank String adresse,
        @NotNull UUID countryId,
        String telephone,
        @NotNull UUID activiteEconomiqueId
) {
}
