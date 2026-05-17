package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Promotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        String nom,
        String description,
        ReductionType reductionType,
        BigDecimal valeurReduction,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean actif,
        PlanAbonnementSummaryResponse plan
) {
    public PromotionResponse(Promotion promotion) {
        this(
                promotion.getId(),
                promotion.getNom(),
                promotion.getDescription(),
                promotion.getReductionType(),
                promotion.getValeurReduction(),
                promotion.getDateDebut(),
                promotion.getDateFin(),
                promotion.isActif(),
                promotion.getPlan() == null ? null : new PlanAbonnementSummaryResponse(promotion.getPlan())
        );
    }
}
