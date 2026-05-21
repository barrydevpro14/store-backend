package org.store.abonnement.application.service.impl;

import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypePlanAbonnement;

public record SubscriptionAmountInputs(
        PlanAbonnement plan,
        TypePlanAbonnement type,
        Promotion promotion,
        Coupon coupon
) {
}
