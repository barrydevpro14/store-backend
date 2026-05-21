package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the public subscription catalog (plans + their durations + active promotions) for the public
 * landing page. Durations are now nested under their plan — a subscription type belongs to exactly one plan.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogServiceImpl implements IPublicCatalogService {

    private final PlanAbonnementDomainService planAbonnementDomainService;
    private final TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    private final PromotionDomainService promotionDomainService;

    public PublicCatalogServiceImpl(PlanAbonnementDomainService planAbonnementDomainService,
                                    TypePlanAbonnementDomainService typePlanAbonnementDomainService,
                                    PromotionDomainService promotionDomainService) {
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.typePlanAbonnementDomainService = typePlanAbonnementDomainService;
        this.promotionDomainService = promotionDomainService;
    }

    /**
     * Loads visible + active plans, attaches their active durations and scoped promotions to each, and
     * returns global promotions (plan IS NULL) at the top level.
     */
    @Override
    public PublicCatalogResponse findCatalog() {
        LocalDate today = LocalDate.now();

        List<PublicPlanResponse> plansBase = planAbonnementDomainService.findPublicResponses();
        List<PromotionResponse> globalPromotions = promotionDomainService.findActiveGlobalResponses(today);
        List<PromotionResponse> scopedPromotions = promotionDomainService.findActiveScopedResponses(today);

        Map<UUID, List<PromotionResponse>> promotionsByPlanId = scopedPromotions.stream()
                .collect(Collectors.groupingBy(promotion -> promotion.plan().id()));

        List<PublicPlanResponse> plans = plansBase.stream()
                .map(plan -> plan
                        .withPromotions(promotionsByPlanId.getOrDefault(plan.id(), List.of()))
                        .withSubscriptionTypes(typesForPlan(plan.id())))
                .toList();

        return new PublicCatalogResponse(plans, globalPromotions);
    }

    /** Loads the active durations of a given plan, sorted by ordre then dureeMois. */
    private List<SubscriptionTypeResponse> typesForPlan(UUID planId) {
        return typePlanAbonnementDomainService.findActifResponsesByPlanId(planId);
    }
}
