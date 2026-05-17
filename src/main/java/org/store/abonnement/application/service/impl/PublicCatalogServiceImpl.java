package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.TypeAbonnementDomainService;

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
     * Charge en 3 queries : plans visibles+actifs, types actifs, promotions actives ce jour.
     * Partitionne les promotions par plan (clé null = globales) puis attache chaque sous-liste à son plan.
     */
    @Override
    public PublicCatalogResponse findCatalog() {
        LocalDate today = LocalDate.now();

        List<PlanAbonnement> plans = planAbonnementDomainService.findAllVisibleAndActif();
        List<SubscriptionTypeResponse> subscriptionTypes = typeAbonnementDomainService.findAllActifResponses();
        List<PromotionResponse> activePromotions = promotionDomainService.findAllActifResponses(today);

        Map<UUID, List<PromotionResponse>> promotionsByPlanId = activePromotions.stream()
                .filter(p -> p.plan() != null)
                .collect(Collectors.groupingBy(p -> p.plan().id()));

        List<PromotionResponse> globalPromotions = activePromotions.stream()
                .filter(p -> p.plan() == null)
                .toList();

        List<PublicPlanResponse> publicPlans = plans.stream()
                .map(plan -> new PublicPlanResponse(plan, promotionsByPlanId.getOrDefault(plan.getId(), List.of())))
                .toList();

        return new PublicCatalogResponse(publicPlans, subscriptionTypes, globalPromotions);
    }
}
