package org.store.vente.application.dto;

import java.util.List;

public record VenteDetailsResponse(
        CommandeVenteResponse commande,
        FactureClientResponse facture,
        List<LigneCommandeVenteResponse> lignes,
        List<PaiementVenteResponse> paiements
) {
}
