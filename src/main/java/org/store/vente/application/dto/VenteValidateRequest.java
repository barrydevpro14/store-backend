package org.store.vente.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload de {@code POST /api/v1/ventes/{commandeId}/validate} : matérialise la commande DRAFT
 * en consommant le stock FIFO, créant la facture client (dateEcheance saisie ici) et appliquant
 * éventuellement un premier paiement. Bascule statut → DELIVERED.
 */
public record VenteValidateRequest(
        @NotNull @FutureOrPresent LocalDate dateEcheance,
        @Valid PaiementVenteRequest premierPaiement
) {
}
