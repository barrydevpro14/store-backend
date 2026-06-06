package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneVenteRequest(
        @NotNull UUID productId,
        @NotNull UUID qualityId,
        @NotNull UUID fournisseurId,
        @NotNull @Positive Integer quantite,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixUnitaire
) {
}
