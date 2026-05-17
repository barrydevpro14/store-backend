package org.store.abonnement.application.dto;

import java.util.List;

public record PublicCatalogResponse(
        List<PublicPlanResponse> plans,
        List<SubscriptionTypeResponse> subscriptionTypes,
        List<PromotionResponse> globalPromotions
) {
}
