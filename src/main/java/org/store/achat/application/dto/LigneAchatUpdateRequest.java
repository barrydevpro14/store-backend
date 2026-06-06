package org.store.achat.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de {@code PUT /api/v1/purchases/orders/{commandeId}/lignes/{ligneId}} :
 * édite quantité / prix / traçabilité lot d'une ligne tant que la commande est en DRAFT.
 */
public record LigneAchatUpdateRequest(
        @NotNull @Positive Integer quantite,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixAchat,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixVente,
        @Size(max = 100) String numeroLot,
        LocalDate dateExpiration
) {
}
