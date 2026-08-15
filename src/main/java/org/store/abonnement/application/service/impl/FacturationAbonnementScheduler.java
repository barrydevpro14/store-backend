package org.store.abonnement.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.notification.application.event.FactureAbonnementGenereeEvent;
import org.store.property.SubscriptionProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily scheduler: generates a FACTURE_GENEREE invoice N days before each active subscription's
 * dateFin (N = subscription.jours-avant-echeance). Auto-applies the first eligible coupon
 * (enterprise-scoped or global). An existsByAbonnementIdAndDateEcheance guard prevents duplicates.
 */
@Component
public class FacturationAbonnementScheduler {

    private static final Logger log = LoggerFactory.getLogger(FacturationAbonnementScheduler.class);

    private final IAbonnementService abonnementService;
    private final IPaiementAbonnementService paiementAbonnementService;
    private final ICouponService couponService;
    private final IUtilisationCouponService utilisationCouponService;
    private final IPlanAbonnementTarifService tarifService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionProperties subscriptionProperties;

    public FacturationAbonnementScheduler(IAbonnementService abonnementService,
                                          IPaiementAbonnementService paiementAbonnementService,
                                          ICouponService couponService,
                                          IUtilisationCouponService utilisationCouponService,
                                          IPlanAbonnementTarifService tarifService,
                                          SubscriptionAmountCalculator amountCalculator,
                                          ApplicationEventPublisher eventPublisher,
                                          SubscriptionProperties subscriptionProperties) {
        this.abonnementService = abonnementService;
        this.paiementAbonnementService = paiementAbonnementService;
        this.couponService = couponService;
        this.utilisationCouponService = utilisationCouponService;
        this.tarifService = tarifService;
        this.amountCalculator = amountCalculator;
        this.eventPublisher = eventPublisher;
        this.subscriptionProperties = subscriptionProperties;
    }

    @Scheduled(cron = "${cron.facturation.abonnement}")
    @Transactional
    public void genererFacturesMensuelles() {
        LocalDate targetDate = LocalDate.now().plusDays(subscriptionProperties.joursAvantEcheance());
        List<Abonnement> aFacturer = abonnementService.findAbonnementsToFacture(targetDate);

        log.info("FacturationAbonnementScheduler: {} abonnement(s) to bill for deadline {}", aFacturer.size(), targetDate);

        aFacturer.forEach(abonnement -> genererFacture(abonnement, targetDate));
    }

    /** Resolves tarif (plan + periodicite effectifs), finds applicable coupon, creates the invoice, then publishes the event. */
    private void genererFacture(Abonnement abonnement, LocalDate dateEcheance) {
        PlanAbonnement planEffectif = resolvePlanEffectif(abonnement);
        PeriodiciteAbonnement periodiciteEffective = resolvePeriodiciteEffective(abonnement);
        UUID entrepriseId = abonnement.getEntreprise().getId();

        PlanAbonnementTarif tarif = tarifService
                .findByPlanAndPeriodicite(planEffectif, periodiciteEffective)
                .orElseThrow(() -> new org.store.common.exceptions.EntityException("tarif.notFound"));

        Coupon coupon = couponService
                .findApplicable(entrepriseId, planEffectif.getId(), periodiciteEffective)
                .orElse(null);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(tarif, coupon));

        PaiementAbonnement facture = paiementAbonnementService.createFactureGeneree(
                new FactureGenereeCommand(abonnement, tarif, coupon, breakdown, dateEcheance));

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

    /** Returns prochainePeriodicite if a change was requested, otherwise the current periodicite. */
    private PeriodiciteAbonnement resolvePeriodiciteEffective(Abonnement abonnement) {
        return abonnement.getProchainePeriodicite() != null
                ? abonnement.getProchainePeriodicite()
                : abonnement.getPeriodicite();
    }

    /** Records coupon usage, increments counter, and deactivates coupon if quota exhausted. */
    private void applyCoupon(Coupon coupon, Abonnement abonnement, PaiementAbonnement facture) {
        utilisationCouponService.createWithPaiement(coupon, abonnement, facture);
        couponService.incrementUsage(coupon);
        couponService.deactivateIfExhausted(coupon);
    }
}
