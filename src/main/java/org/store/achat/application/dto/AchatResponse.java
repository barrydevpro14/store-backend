package org.store.achat.application.dto;

public record AchatResponse(
        CommandeAchatResponse commande,
        FactureAchatResponse facture
) {
}
