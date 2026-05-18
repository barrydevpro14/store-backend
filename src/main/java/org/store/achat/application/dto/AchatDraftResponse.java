package org.store.achat.application.dto;

/**
 * Réponse retournée par {@code POST /api/v1/purchases} : commande en statut DRAFT,
 * sans facture ni mouvements stock (matérialisés à la validation).
 */
public record AchatDraftResponse(
        CommandeAchatResponse commande
) {
}
