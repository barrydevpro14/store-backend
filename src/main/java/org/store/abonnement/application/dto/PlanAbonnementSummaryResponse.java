package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanAbonnementSummaryResponse(
        UUID id,
        String nom,
        BigDecimal prix
) {
    public PlanAbonnementSummaryResponse(PlanAbonnement plan) {
        this(plan.getId(), plan.getNom(), plan.getPrix());
    }
}
