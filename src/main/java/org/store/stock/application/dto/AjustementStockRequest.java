package org.store.stock.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.TypeAjustement;

import java.math.BigDecimal;
import java.util.UUID;

public record AjustementStockRequest(
        @NotNull UUID stockId,
        @NotNull TypeAjustement type,
        @NotNull @Positive BigDecimal quantite,
        @NotNull MotifAjustement motif,
        @Size(max = 500) String commentaire
) {
}
