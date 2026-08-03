package org.store.stock.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EntreeStockUpdateRequest(
        @NotNull @Min(1) Integer quantite,
        @NotNull @DecimalMin("0.01") BigDecimal prixAchat,
        @NotNull @DecimalMin("0.01") BigDecimal prixVente
) {
}
