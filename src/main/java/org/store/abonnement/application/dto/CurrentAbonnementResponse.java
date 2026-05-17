package org.store.abonnement.application.dto;

public record CurrentAbonnementResponse(
        AbonnementResponse abonnement,
        long joursRestants,
        boolean isTrial,
        PlanFeaturesResponse fonctionnalites
) {
}
