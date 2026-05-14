package org.store.stock.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.TypeAjustement;

import java.math.BigDecimal;
import java.util.UUID;

public record AjustementStockRequest(
        @NotNull UUID magasinId,
        @NotNull UUID productId,
        @NotNull TypeAjustement type,
        @NotNull @Positive Integer quantite,
        UUID productFournisseurId,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixAchat,
        @NotNull MotifAjustement motif,
        @Size(max = 500) String commentaire
) {
}
