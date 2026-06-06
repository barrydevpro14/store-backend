package org.store.abonnement.application.dto;

public record SubscribeResponse(
        AbonnementResponse abonnement,
        SubscriptionAmountBreakdown breakdown,
        String couponCodeApplied,
        String promotionNomApplied
) {
}
