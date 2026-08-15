package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.FacturationAbonnementScheduler;
import org.store.abonnement.application.service.impl.FactureGenereeCommand;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.entreprise.domain.model.Entreprise;
import org.store.notification.application.event.FactureAbonnementGenereeEvent;
import org.store.property.SubscriptionProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturationAbonnementSchedulerTest {

    @Mock private IAbonnementService abonnementService;
    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private ICouponService couponService;
    @Mock private IUtilisationCouponService utilisationCouponService;
    @Mock private IPlanAbonnementTarifService tarifService;
    @Mock private SubscriptionAmountCalculator amountCalculator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SubscriptionProperties subscriptionProperties;

    @InjectMocks
    private FacturationAbonnementScheduler scheduler;

    private UUID entrepriseId;
    private PlanAbonnement plan;
    private PlanAbonnementTarif tarif;
    private Abonnement abonnement;
    private LocalDate today;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        today = LocalDate.now();
        targetDate = today.plusDays(10);

        when(subscriptionProperties.joursAvantEcheance()).thenReturn(10);

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());

        tarif = new PlanAbonnementTarif();
        tarif.setId(UUID.randomUUID());
        tarif.setPlan(plan);
        tarif.setPeriodicite(PeriodiciteAbonnement.MENSUEL);
        tarif.setPrix(new BigDecimal("19900"));

        abonnement = new Abonnement();
        abonnement.setId(UUID.randomUUID());
        abonnement.setEntreprise(entreprise);
        abonnement.setPlanAbonnement(plan);
        abonnement.setPeriodicite(PeriodiciteAbonnement.MENSUEL);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        abonnement.setDateFin(targetDate);
    }

    @Test
    void genererFacturesMensuelles_should_generate_invoice_for_eligible_abonnement() {
        PaiementAbonnement facture = new PaiementAbonnement();
        facture.setId(UUID.randomUUID());
        facture.setMontantFinal(new BigDecimal("19900"));

        SubscriptionAmountBreakdown breakdown = new SubscriptionAmountBreakdown(
                new BigDecimal("19900"), BigDecimal.ZERO, new BigDecimal("19900"));

        when(abonnementService.findAbonnementsToFacture(targetDate)).thenReturn(List.of(abonnement));
        when(tarifService.findByPlanAndPeriodicite(plan, PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.of(tarif));
        when(couponService.findApplicable(entrepriseId, plan.getId(), PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(breakdown);
        when(paiementAbonnementService.createFactureGeneree(any(FactureGenereeCommand.class))).thenReturn(facture);

        scheduler.genererFacturesMensuelles();

        verify(paiementAbonnementService).createFactureGeneree(any(FactureGenereeCommand.class));
        verify(eventPublisher).publishEvent(any(FactureAbonnementGenereeEvent.class));
    }

    @Test
    void genererFacturesMensuelles_should_skip_when_no_eligible_abonnement() {
        when(abonnementService.findAbonnementsToFacture(targetDate)).thenReturn(List.of());

        scheduler.genererFacturesMensuelles();

        verify(paiementAbonnementService, never()).createFactureGeneree(any(FactureGenereeCommand.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void genererFacturesMensuelles_should_apply_coupon_when_found() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());

        PaiementAbonnement facture = new PaiementAbonnement();
        facture.setId(UUID.randomUUID());
        facture.setMontantFinal(new BigDecimal("14900"));

        SubscriptionAmountBreakdown breakdown = new SubscriptionAmountBreakdown(
                new BigDecimal("19900"), new BigDecimal("5000"), new BigDecimal("14900"));

        when(abonnementService.findAbonnementsToFacture(targetDate)).thenReturn(List.of(abonnement));
        when(tarifService.findByPlanAndPeriodicite(plan, PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.of(tarif));
        when(couponService.findApplicable(entrepriseId, plan.getId(), PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.of(coupon));
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(breakdown);
        when(paiementAbonnementService.createFactureGeneree(any(FactureGenereeCommand.class))).thenReturn(facture);

        scheduler.genererFacturesMensuelles();

        verify(utilisationCouponService).createWithPaiement(eq(coupon), eq(abonnement), eq(facture));
        verify(couponService).incrementUsage(coupon);
        verify(couponService).deactivateIfExhausted(coupon);
    }

    @Test
    void genererFacturesMensuelles_should_use_prochain_plan_when_set() {
        PlanAbonnement prochainPlan = new PlanAbonnement();
        prochainPlan.setId(UUID.randomUUID());
        abonnement.setProchainPlan(prochainPlan);

        PlanAbonnementTarif prochainTarif = new PlanAbonnementTarif();
        prochainTarif.setId(UUID.randomUUID());
        prochainTarif.setPlan(prochainPlan);
        prochainTarif.setPeriodicite(PeriodiciteAbonnement.MENSUEL);
        prochainTarif.setPrix(new BigDecimal("29900"));

        PaiementAbonnement facture = new PaiementAbonnement();
        facture.setId(UUID.randomUUID());
        facture.setMontantFinal(new BigDecimal("29900"));

        SubscriptionAmountBreakdown breakdown = new SubscriptionAmountBreakdown(
                new BigDecimal("29900"), BigDecimal.ZERO, new BigDecimal("29900"));

        when(abonnementService.findAbonnementsToFacture(targetDate)).thenReturn(List.of(abonnement));
        when(tarifService.findByPlanAndPeriodicite(prochainPlan, PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.of(prochainTarif));
        when(couponService.findApplicable(entrepriseId, prochainPlan.getId(), PeriodiciteAbonnement.MENSUEL)).thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(breakdown);
        when(paiementAbonnementService.createFactureGeneree(any(FactureGenereeCommand.class))).thenReturn(facture);

        scheduler.genererFacturesMensuelles();

        verify(tarifService).findByPlanAndPeriodicite(prochainPlan, PeriodiciteAbonnement.MENSUEL);
        verify(couponService).findApplicable(entrepriseId, prochainPlan.getId(), PeriodiciteAbonnement.MENSUEL);
    }
}
