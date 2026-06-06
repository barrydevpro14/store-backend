package org.store.produit.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductFournisseurPrixVenteRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixVente
) {
}
