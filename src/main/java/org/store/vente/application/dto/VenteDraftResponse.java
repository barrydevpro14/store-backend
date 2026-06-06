package org.store.vente.application.dto;

import java.util.List;

/**
 * Réponse retournée par {@code POST /api/v1/ventes} : commande en statut DRAFT
 * avec ses lignes initiales (IDs inclus pour permettre la suppression individuelle),
 * sans facture ni sorties stock (matérialisées à la validation).
 */
public record VenteDraftResponse(
        CommandeVenteResponse commande,
        List<LigneCommandeVenteResponse> lignes
) {
}
