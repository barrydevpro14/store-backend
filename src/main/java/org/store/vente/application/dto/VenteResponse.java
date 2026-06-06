package org.store.vente.application.dto;

public record VenteResponse(
        CommandeVenteResponse commande,
        FactureClientResponse facture
) {
}
