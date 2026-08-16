package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TarifAvecCoupon;

import java.util.List;

/**
 * Builds the public subscription catalog for the landing page and OWNER subscribe flow.
 * Tarifs actifs + coupon global applicable sont résolus en une seule requête JPQL par plan.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogServiceImpl implements IPublicCatalogService {

    private final IPlanAbonnementService planService;
    private final IPlanAbonnementTarifService tarifService;
    private final SubscriptionAmountCalculator amountCalculator;

    public PublicCatalogServiceImpl(IPlanAbonnementService planService,
                                    IPlanAbonnementTarifService tarifService,
                                    SubscriptionAmountCalculator amountCalculator) {
        this.planService = planService;
        this.tarifService = tarifService;
        this.amountCalculator = amountCalculator;
    }

    /** Tous les plans actifs + visibles (y compris trial) pour la landing publique. */
    @Override
    public PublicCatalogResponse findCatalog() {
        List<PublicPlanResponse> plans = planService.findPublicPlans().stream()
                .map(this::toEnrichedResponse)
                .toList();

        return new PublicCatalogResponse(plans);
    }

    /** Plans actifs + visibles + non-trial pour l'écran OWNER de souscription. */
    @Override
    public PublicCatalogResponse findSubscribableCatalog() {
        List<PublicPlanResponse> plans = planService.findSubscribablePlans().stream()
                .map(this::toEnrichedResponse)
                .toList();

        return new PublicCatalogResponse(plans);
    }

    private PublicPlanResponse toEnrichedResponse(PlanAbonnement plan) {
        List<PlanAbonnementTarifResponse> enrichedTarifs = tarifService.findActifWithCoupon(plan.getId()).stream()
                .map(tac -> toTarifResponse(plan, tac))
                .toList();

        return new PublicPlanResponse(plan, enrichedTarifs);
    }

    private PlanAbonnementTarifResponse toTarifResponse(PlanAbonnement plan, TarifAvecCoupon tac) {
        if (plan.isTrial() || tac.coupon() == null) {
            return new PlanAbonnementTarifResponse(tac.tarif());
        }

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(tac.tarif(), tac.coupon()));

        return new PlanAbonnementTarifResponse(tac.tarif(), breakdown, tac.coupon());
    }
}
