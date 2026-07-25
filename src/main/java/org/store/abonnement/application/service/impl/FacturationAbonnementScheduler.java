package org.store.abonnement.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.notification.application.event.FactureAbonnementGenereeEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily scheduler: generates a FACTURE_GENEREE invoice 10 days before each active subscription's
 * dateFin. Auto-applies the first eligible coupon (enterprise-scoped or global). An
 * existsByAbonnementIdAndDateEcheance guard prevents duplicate invoices.
 */
@Component
public class FacturationAbonnementScheduler {

    private static final Logger log = LoggerFactory.getLogger(FacturationAbonnementScheduler.class);
    private static final int JOURS_AVANT_ECHEANCE = 10;

    private final AbonnementDomainService abonnementDomainService;
    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final ICouponService couponService;
    private final IUtilisationCouponService utilisationCouponService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public FacturationAbonnementScheduler(AbonnementDomainService abonnementDomainService,
                                          PaiementAbonnementDomainService paiementAbonnementDomainService,
                                          ICouponService couponService,
                                          IUtilisationCouponService utilisationCouponService,
                                          SubscriptionAmountCalculator amountCalculator,
                                          ApplicationEventPublisher eventPublisher) {
        this.abonnementDomainService = abonnementDomainService;
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.couponService = couponService;
        this.utilisationCouponService = utilisationCouponService;
        this.amountCalculator = amountCalculator;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "${cron.facturation.abonnement}")
    @Transactional
    public void genererFacturesMensuelles() {
        LocalDate targetDate = LocalDate.now().plusDays(JOURS_AVANT_ECHEANCE);
        List<Abonnement> aFacturer = abonnementDomainService.findAbonnementsToFacture(targetDate);

        log.info("FacturationAbonnementScheduler: {} abonnement(s) to bill for deadline {}", aFacturer.size(), targetDate);

        aFacturer.forEach(abonnement -> genererFacture(abonnement, targetDate));
    }

    /** Resolves effective plan, finds applicable coupon, creates the invoice, then publishes the event. */
    private void genererFacture(Abonnement abonnement, LocalDate dateEcheance) {
        PlanAbonnement planEffectif = resolvePlanEffectif(abonnement);
        UUID entrepriseId = abonnement.getEntreprise().getId();

        Coupon coupon = couponService
                .findApplicable(entrepriseId, planEffectif.getId())
                .orElse(null);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(planEffectif, coupon));

        PaiementAbonnement facture = paiementAbonnementDomainService.createFactureGeneree(
                abonnement,
                breakdown.prixDeBase(),
                breakdown.reductionCoupon(),
                breakdown.montantAPayer(),
                dateEcheance);

        if (coupon != null) {
            applyCoupon(coupon, abonnement, facture);
        }

        eventPublisher.publishEvent(new FactureAbonnementGenereeEvent(
                abonnement.getId(),
                entrepriseId,
                facture.getMontantFinal(),
                dateEcheance));

        log.info("FacturationAbonnementScheduler: invoice generated — abonnement={} montant={} deadline={}",
                abonnement.getId(), facture.getMontantFinal(), dateEcheance);
    }

    /** Returns prochainPlan if a plan change was requested, otherwise the current plan. */
    private PlanAbonnement resolvePlanEffectif(Abonnement abonnement) {
        return abonnement.getProchainPlan() != null
                ? abonnement.getProchainPlan()
                : abonnement.getPlanAbonnement();
    }

    /** Records coupon usage, increments counter, and deactivates coupon if quota exhausted. */
    private void applyCoupon(Coupon coupon, Abonnement abonnement, PaiementAbonnement facture) {
        utilisationCouponService.createWithPaiement(coupon, abonnement, facture);
        couponService.incrementUsage(coupon);
        couponService.deactivateIfExhausted(coupon);
    }
}
