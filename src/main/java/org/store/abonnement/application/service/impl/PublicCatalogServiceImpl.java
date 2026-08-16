package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Builds the public subscription catalog (plans only) for the public landing page and OWNER subscribe flow.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogServiceImpl implements IPublicCatalogService {

    private final PlanAbonnementDomainService planAbonnementDomainService;
    private final ICouponService couponService;
    private final SubscriptionAmountCalculator amountCalculator;

    public PublicCatalogServiceImpl(PlanAbonnementDomainService planAbonnementDomainService,
                                    ICouponService couponService,
                                    SubscriptionAmountCalculator amountCalculator) {
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.couponService = couponService;
        this.amountCalculator = amountCalculator;
    }

    /** Returns all active + visible plans (including trial) for the public landing, with global coupon info embedded per tarif. */
    @Override
    public PublicCatalogResponse findCatalog() {
        List<PublicPlanResponse> plans = planAbonnementDomainService.findPublicPlans().stream()
                .map(this::toEnrichedResponse)
                .toList();

        return new PublicCatalogResponse(plans);
    }

    /** Returns active + visible + non-trial plans for the OWNER subscribe screen, with global coupon info embedded per tarif. */
    @Override
    public PublicCatalogResponse findSubscribableCatalog() {
        List<PublicPlanResponse> plans = planAbonnementDomainService.findSubscribablePlans().stream()
                .map(this::toEnrichedResponse)
                .toList();

        return new PublicCatalogResponse(plans);
    }

    private PublicPlanResponse toEnrichedResponse(PlanAbonnement plan) {
        List<PlanAbonnementTarifResponse> enrichedTarifs = plan.getTarifs().stream()
                .filter(PlanAbonnementTarif::isActif)
                .sorted(Comparator.comparingInt(t -> t.getOrdre() != null ? t.getOrdre() : 0))
                .map(tarif -> toEnrichedTarifResponse(plan, tarif))
                .toList();

        return new PublicPlanResponse(plan, enrichedTarifs);
    }

    private PlanAbonnementTarifResponse toEnrichedTarifResponse(PlanAbonnement plan, PlanAbonnementTarif tarif) {
        if (plan.isTrial()) {
            return new PlanAbonnementTarifResponse(tarif);
        }

        Optional<Coupon> coupon = couponService.findApplicableGlobal(plan.getId(), tarif.getPeriodicite());

        if (coupon.isEmpty()) {
            return new PlanAbonnementTarifResponse(tarif);
        }

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(new SubscriptionAmountInputs(tarif, coupon.get()));
        return new PlanAbonnementTarifResponse(tarif, breakdown, coupon.get());
    }
}
