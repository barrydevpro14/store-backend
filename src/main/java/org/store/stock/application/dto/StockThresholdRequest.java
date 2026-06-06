package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockThresholdRequest(
        @NotNull @Min(0) Integer seuilApprovisionnement
) {
}
