package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Payload de {@code PUT /api/v1/ventes/orders/{commandeId}/lignes/{ligneId}} :
 * édite quantité / prixUnitaire d'une ligne tant que la commande est en DRAFT.
 * Le {@code productFournisseur} est immuable (pour changer de variante, supprimer et recréer la ligne).
 */
public record LigneVenteUpdateRequest(
        @NotNull @Positive Integer quantite,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixUnitaire
) {
}
