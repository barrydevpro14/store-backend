package org.store.achat.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de {@code POST /api/v1/purchases/{commandeId}/validate} : facture saisie au moment
 * de la matérialisation (entrées stock + journal + bascule statut RECEPTIONNEE).
 */
public record AchatValidateRequest(
        @NotNull @Valid FactureAchatCreateRequest facture
) {
}
