package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.TypeAbonnement;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionTypeResponse(
        UUID id,
        String nom,
        int dureeMois,
        ReductionType reductionType,
        BigDecimal valeurReduction,
        boolean recommande,
        boolean actif,
        int ordre
) {
    public SubscriptionTypeResponse(TypeAbonnement type) {
        this(
                type.getId(),
                type.getNom(),
                type.getDureeMois(),
                type.getReductionType(),
                type.getValeurReduction(),
                type.isRecommande(),
                type.isActif(),
                type.getOrdre()
        );
    }
}
