package org.store.achat.application.dto;

import java.util.List;
import java.util.UUID;

public record AchatResponse(
        CommandeAchatResponse commande,
        FactureAchatResponse facture,
        List<UUID> entreesStockIds
) {
}
