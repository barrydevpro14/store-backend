package org.store.achat.application.dto;

import java.util.List;

/**
 * Réponse retournée par {@code POST /api/v1/achats} : commande DRAFT avec ses lignes initiales
 * (IDs inclus pour la suppression individuelle), sans facture ni entrées stock.
 */
public record AchatDraftResponse(
        CommandeAchatResponse commande,
        List<LigneCommandeAchatResponse> lignes
) {
}
