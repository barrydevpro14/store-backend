package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlanAbonnementRequest(
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 1000) String description,
        @NotNull @PositiveOrZero BigDecimal prix,
        @PositiveOrZero int nombreMagasinsMax,
        @PositiveOrZero int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        boolean actif,
        boolean visible,
        boolean trial,
        @PositiveOrZero int ordre
) {
}
