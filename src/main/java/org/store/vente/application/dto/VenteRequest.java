package org.store.vente.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Payload de {@code POST /api/v1/ventes} : crée la commande de vente en DRAFT (lignes seulement,
 * sans facture ni consommation stock). La {@code dateEcheance} et le {@code premierPaiement}
 * sont saisis à la validation ({@link VenteValidateRequest}).
 */
public record VenteRequest(
        UUID clientId,
        @NotEmpty @Valid List<LigneVenteRequest> lignes
) {
}
