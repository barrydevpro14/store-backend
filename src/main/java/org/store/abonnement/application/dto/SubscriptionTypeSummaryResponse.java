package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.TypePlanAbonnement;

import java.util.UUID;

public record SubscriptionTypeSummaryResponse(
        UUID id,
        String nom,
        int dureeMois
) {
    public SubscriptionTypeSummaryResponse(TypePlanAbonnement type) {
        this(type.getId(), type.getNom(), type.getDureeMois());
    }
}
