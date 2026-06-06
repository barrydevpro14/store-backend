package org.store.achat.application.dto;

import java.util.List;

public record AchatDetailsResponse(
        CommandeAchatResponse commande,
        FactureAchatResponse facture,
        List<LigneCommandeAchatResponse> lignes
) {
}
