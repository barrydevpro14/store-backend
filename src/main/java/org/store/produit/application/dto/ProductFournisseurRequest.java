package org.store.produit.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFournisseurRequest(
        @NotNull UUID productId,
        @NotNull UUID fournisseurId,
        @NotNull UUID qualityId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixAchat,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixVente,
        @Size(max = 100) String referenceFournisseur,
        @Size(max = 100) String origine
) {
}
