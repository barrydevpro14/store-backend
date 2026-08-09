package org.store.stock.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EntreeStockUpdateRequest(
        @NotNull @Positive BigDecimal quantite,
        @NotNull @DecimalMin("0.01") BigDecimal prixAchat,
        @NotNull @DecimalMin("0.01") BigDecimal prixVente
) {
}
