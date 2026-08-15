package org.store.abonnement.application.service.impl;

import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

public record SubscriptionAmountInputs(
        PlanAbonnementTarif tarif,
        Coupon coupon
) {}
