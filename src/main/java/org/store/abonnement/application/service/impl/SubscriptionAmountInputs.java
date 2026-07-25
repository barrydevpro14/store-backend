package org.store.abonnement.application.service.impl;

import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;

public record SubscriptionAmountInputs(
        PlanAbonnement plan,
        Coupon coupon
) {}
