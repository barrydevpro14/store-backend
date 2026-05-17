package org.store.abonnement.application.service.impl;

import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.model.PieceJointe;

/**
 * Regroupe les 4 inputs nécessaires à la création d'un paiement d'abonnement
 * (cible le respect de la règle "max 3 paramètres par méthode").
 */
public record PaiementAbonnementCreationContext(
        Abonnement abonnement,
        PaiementAbonnementRequest paiementAbonnementRequest,
        SubscriptionAmountBreakdown breakdown,
        PieceJointe preuve
) {
}
