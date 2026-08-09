package org.store.inventaire.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneInventaireRequest(
        @NotNull UUID productFournisseurId,
        @NotNull @DecimalMin("0") BigDecimal quantiteReelle,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixUnitaire
) {
}
