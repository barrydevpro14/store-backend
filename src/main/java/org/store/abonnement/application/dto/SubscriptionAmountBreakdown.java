package org.store.abonnement.application.dto;

import java.math.BigDecimal;

public record SubscriptionAmountBreakdown(
        BigDecimal prixDeBase,
        BigDecimal reductionType,
        BigDecimal reductionPromotion,
        BigDecimal reductionCoupon,
        BigDecimal montantAPayer
) {
}
