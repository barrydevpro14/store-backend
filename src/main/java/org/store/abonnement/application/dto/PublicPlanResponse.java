package org.store.abonnement.application.dto;

import org.store.abonnement.domain.model.PlanAbonnement;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicPlanResponse(
        UUID id,
        String nom,
        String description,
        BigDecimal prix,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        boolean trial,
        int ordre,
        List<PromotionResponse> promotions
) {
    public PublicPlanResponse(PlanAbonnement plan, List<PromotionResponse> promotions) {
        this(
                plan.getId(),
                plan.getNom(),
                plan.getDescription(),
                plan.getPrix(),
                plan.getNombreMagasinsMax(),
                plan.getNombreEmployesMax(),
                plan.isGestionStock(),
                plan.isGestionVente(),
                plan.isGestionAchat(),
                plan.isGestionComptabilite(),
                plan.isTrial(),
                plan.getOrdre(),
                promotions
        );
    }
}
