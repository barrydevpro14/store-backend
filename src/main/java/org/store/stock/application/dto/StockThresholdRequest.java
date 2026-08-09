package org.store.stock.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockThresholdRequest(
        @NotNull @DecimalMin("0") BigDecimal seuilApprovisionnement
) {
}
