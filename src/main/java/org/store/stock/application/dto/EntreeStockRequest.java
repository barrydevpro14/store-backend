package org.store.stock.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EntreeStockRequest(
        @NotNull UUID magasinId,
        @NotNull UUID productFournisseurId,
        @NotNull @Positive Integer quantite,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixAchat,
        @Size(max = 100) String numeroLot,
        LocalDate dateExpiration,
        @Size(max = 500) String commentaire
) {
}
