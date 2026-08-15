package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.UUID;

public record PlanAbonnementSummaryResponse(
        UUID id,
        String nom
) {
    public PlanAbonnementSummaryResponse(PlanAbonnement plan) {
        this(plan.getId(), plan.getNom());
    }
}
