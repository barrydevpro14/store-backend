package org.store.abonnement.application.dto;

/**
 * Owner "current subscription" view. {@code abonnement} is always populated (either ACTIF or TRIAL).
 * The trial vs. paid distinction is carried by {@code abonnement.statut} — no separate flag needed.
 */
public record CurrentAbonnementResponse(
        AbonnementResponse abonnement,
        long joursRestants,
        PlanFeaturesResponse fonctionnalites
) {
}
