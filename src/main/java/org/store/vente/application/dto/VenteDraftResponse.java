package org.store.vente.application.dto;

/**
 * Réponse retournée par {@code POST /api/v1/ventes} : commande en statut DRAFT,
 * sans facture ni sorties stock (matérialisées à la validation).
 */
public record VenteDraftResponse(
        CommandeVenteResponse commande
) {
}
