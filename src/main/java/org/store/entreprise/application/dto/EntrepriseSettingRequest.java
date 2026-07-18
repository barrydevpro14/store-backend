package org.store.entreprise.application.dto;

import jakarta.validation.constraints.Pattern;

public record EntrepriseSettingRequest(
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.couleurPrimaire.format}")
        String couleurPrimaire
) {}
