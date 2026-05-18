package org.store.abonnement.application.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.TypeAbonnementDomainService;
import org.store.config.RedisCacheConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Construit le catalogue public d'abonnement (plans + types + promotions actives) pour la landing publique.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogServiceImpl implements IPublicCatalogService {

    private final PlanAbonnementDomainService planAbonnementDomainService;
    private final TypeAbonnementDomainService typeAbonnementDomainService;
    private final PromotionDomainService promotionDomainService;

    public PublicCatalogServiceImpl(PlanAbonnementDomainService planAbonnementDomainService,
                                    TypeAbonnementDomainService typeAbonnementDomainService,
                                    PromotionDomainService promotionDomainService) {
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.typeAbonnementDomainService = typeAbonnementDomainService;
        this.promotionDomainService = promotionDomainService;
    }

    /**
     * Charge en 4 queries projetées : plans visibles+actifs, types actifs, promotions globales actives (plan IS NULL),
     * promotions scopées actives (plan IS NOT NULL). Groupe les scopées par planId et les attache au plan via `withPromotions`.
     * <p>Résultat mis en cache (TTL 5 min) — invalidé à chaque CUD admin sur Plan, Promotion, SubscriptionType.
     */
    @Override
    @Cacheable(RedisCacheConfig.PUBLIC_CATALOG)
    public PublicCatalogResponse findCatalog() {
        LocalDate today = LocalDate.now();

        List<PublicPlanResponse> plansWithoutPromotions = planAbonnementDomainService.findPublicResponses();
        List<SubscriptionTypeResponse> subscriptionTypes = typeAbonnementDomainService.findAllActifResponses();
        List<PromotionResponse> globalPromotions = promotionDomainService.findActiveGlobalResponses(today);
        List<PromotionResponse> scopedPromotions = promotionDomainService.findActiveScopedResponses(today);

        Map<UUID, List<PromotionResponse>> promotionsByPlanId = scopedPromotions.stream()
                .collect(Collectors.groupingBy(promotion -> promotion.plan().id()));

        List<PublicPlanResponse> plans = plansWithoutPromotions.stream()
                .map(plan -> plan.withPromotions(promotionsByPlanId.getOrDefault(plan.id(), List.of())))
                .toList();

        return new PublicCatalogResponse(plans, subscriptionTypes, globalPromotions);
    }
}
