package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.AbonnementServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementServiceImplTest {

    @Mock private AbonnementDomainService abonnementDomainService;
    @Mock private ISubscriptionTypeService subscriptionTypeService;
    @Mock private CouponDomainService couponDomainService;
    @Mock private PromotionDomainService promotionDomainService;
    @Mock private UtilisationCouponDomainService utilisationCouponDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private SubscriptionAmountCalculator amountCalculator;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private AbonnementServiceImpl service;

    private UUID entrepriseId;
    private UUID planId;
    private UUID typeId;
    private Entreprise entreprise;
    private PlanAbonnement plan;
    private TypePlanAbonnement type;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        planId = UUID.randomUUID();
        typeId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Pro");
        plan.setPrix(new BigDecimal("19900"));
        plan.setActif(true);
        plan.setVisible(true);
        plan.setTrial(false);

        type = new TypePlanAbonnement();
        type.setId(typeId);
        type.setPlan(plan);
        type.setNom("Annuel");
        type.setDureeMois(12);
        type.setActif(true);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null,
                "owner", "OWNER", List.of("SUBSCRIPTION_CREATE"));
    }

    private SubscriptionAmountBreakdown sampleBreakdown(String montant) {
        return new SubscriptionAmountBreakdown(
                new BigDecimal("238800.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal(montant));
    }

    @Test
    void subscribe_should_create_pending_abonnement_without_coupon() {
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);
        Abonnement pending = pendingAbonnement();
        SubscriptionAmountBreakdown breakdown = sampleBreakdown("238800.00");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(promotionDomainService.findFirstActivePromotionForPlan(eq(planId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(breakdown);
        when(abonnementDomainService.createPending(entreprise, type)).thenReturn(pending);
        when(abonnementDomainService.setRenouvellementAuto(pending, false)).thenReturn(pending);

        SubscribeResponse response = service.subscribe(request);

        assertThat(response.abonnement().id()).isEqualTo(pending.getId());
        assertThat(response.abonnement().statut()).isEqualTo(AbonnementStatut.EN_ATTENTE);
        assertThat(response.breakdown()).isSameAs(breakdown);
        assertThat(response.couponCodeApplied()).isNull();
        assertThat(response.promotionNomApplied()).isNull();

        verify(utilisationCouponDomainService, never()).create(any(), any(), any());
        verify(couponDomainService, never()).incrementUsage(any(Coupon.class));
    }

    @Test
    void subscribe_should_apply_coupon_and_increment_usage() {
        SubscribeRequest request = new SubscribeRequest(planId, typeId, "PROMO10", true);
        Coupon coupon = validCoupon();

        Abonnement pending = pendingAbonnement();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(couponDomainService.findByCode("PROMO10")).thenReturn(Optional.of(coupon));
        when(promotionDomainService.findFirstActivePromotionForPlan(eq(planId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(sampleBreakdown("210000.00"));
        when(abonnementDomainService.createPending(entreprise, type)).thenReturn(pending);
        when(abonnementDomainService.setRenouvellementAuto(pending, true)).thenReturn(pending);

        SubscribeResponse response = service.subscribe(request);

        assertThat(response.couponCodeApplied()).isEqualTo("PROMO10");
        verify(couponDomainService).incrementUsage(coupon);
        verify(utilisationCouponDomainService).create(coupon, entreprise, pending);
    }

    @Test
    void subscribe_should_apply_active_promotion_for_plan() {
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);
        Promotion promotion = new Promotion();
        promotion.setNom("Black Friday");

        Abonnement pending = pendingAbonnement();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(promotionDomainService.findFirstActivePromotionForPlan(eq(planId), any(LocalDate.class)))
                .thenReturn(Optional.of(promotion));
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(sampleBreakdown("190000.00"));
        when(abonnementDomainService.createPending(entreprise, type)).thenReturn(pending);
        when(abonnementDomainService.setRenouvellementAuto(pending, false)).thenReturn(pending);

        SubscribeResponse response = service.subscribe(request);

        assertThat(response.promotionNomApplied()).isEqualTo("Black Friday");
    }

    @Test
    void subscribe_should_mark_entreprise_trial_used_when_subscribing() {
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);
        Abonnement pending = pendingAbonnement();
        entreprise.setTrialUsed(false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(promotionDomainService.findFirstActivePromotionForPlan(eq(planId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class)))
                .thenReturn(sampleBreakdown("238800.00"));
        when(abonnementDomainService.createPending(entreprise, type)).thenReturn(pending);
        when(abonnementDomainService.setRenouvellementAuto(pending, false)).thenReturn(pending);

        service.subscribe(request);

        assertThat(entreprise.isTrialUsed()).isTrue();
    }

    @Test
    void subscribe_should_throw_when_type_inactive() {
        type.setActif(false);
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);

        verify(abonnementDomainService, never()).createPending(any(), any());
    }

    @Test
    void subscribe_should_throw_when_plan_inactive() {
        plan.setActif(false);
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);

        verify(abonnementDomainService, never()).createPending(any(), any());
    }

    @Test
    void subscribe_should_throw_when_plan_is_trial() {
        plan.setTrial(true);
        SubscribeRequest request = new SubscribeRequest(planId, typeId, null, false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void subscribe_should_throw_when_coupon_not_found() {
        SubscribeRequest request = new SubscribeRequest(planId, typeId, "GHOST", false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(couponDomainService.findByCode("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void subscribe_should_throw_when_coupon_expired() {
        Coupon coupon = validCoupon();
        coupon.setDateFin(LocalDate.now().minusDays(1));
        SubscribeRequest request = new SubscribeRequest(planId, typeId, "EXPIRED", false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(couponDomainService.findByCode("EXPIRED")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void subscribe_should_throw_when_coupon_exhausted() {
        Coupon coupon = validCoupon();
        coupon.setNombreUtilisationsMax(5);
        coupon.setNombreUtilisations(5);
        SubscribeRequest request = new SubscribeRequest(planId, typeId, "FULL", false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(couponDomainService.findByCode("FULL")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void subscribe_should_throw_when_coupon_not_applicable_to_plan() {
        Coupon coupon = validCoupon();
        PlanAbonnement otherPlan = new PlanAbonnement();
        otherPlan.setId(UUID.randomUUID());
        coupon.setPlan(otherPlan);

        SubscribeRequest request = new SubscribeRequest(planId, typeId, "OTHER_PLAN", false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(subscriptionTypeService.findById(typeId)).thenReturn(type);
        when(couponDomainService.findByCode("OTHER_PLAN")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void updateRenouvellementAuto_should_toggle_and_save() {
        Abonnement abonnement = pendingAbonnement();
        abonnement.setRenouvellementAuto(false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnement.getId())).thenReturn(abonnement);
        when(abonnementDomainService.setRenouvellementAuto(abonnement, true)).thenAnswer(inv -> {
            abonnement.setRenouvellementAuto(true);
            return abonnement;
        });

        AbonnementResponse response = service.updateRenouvellementAuto(
                abonnement.getId(), new RenouvellementAutoRequest(true));

        assertThat(response.renouvellementAuto()).isTrue();
        assertThat(abonnement.isRenouvellementAuto()).isTrue();
    }

    @Test
    void updateRenouvellementAuto_should_throw_when_other_entreprise() {
        Abonnement abonnement = pendingAbonnement();
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        abonnement.setEntreprise(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnement.getId())).thenReturn(abonnement);

        assertThatThrownBy(() -> service.updateRenouvellementAuto(
                abonnement.getId(), new RenouvellementAutoRequest(true)))
                .isInstanceOf(ForbiddenException.class);

        verify(abonnementDomainService, never()).setRenouvellementAuto(any(), anyBoolean());
    }

    @Test
    void findAll_should_delegate_unchanged_for_admin() {
        AbonnementFilter filter = new AbonnementFilter(null, "ACTIF", null, 0, 10);
        Page<AbonnementResponse> page = new PageImpl<>(java.util.List.of());
        when(abonnementDomainService.findResponses(filter)).thenReturn(page);

        assertThat(service.findAll(filter)).isSameAs(page);
    }

    @Test
    void findMyHistory_should_force_entrepriseId_from_current_user() {
        AbonnementFilter filter = new AbonnementFilter(null, "EN_ATTENTE", null, 0, 10);
        Page<AbonnementResponse> page = new PageImpl<>(java.util.List.of());

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findResponses(any(AbonnementFilter.class))).thenReturn(page);

        service.findMyHistory(filter);

        verify(abonnementDomainService).findResponses(
                org.mockito.ArgumentMatchers.argThat(f -> entrepriseId.equals(f.entrepriseId())
                        && "EN_ATTENTE".equals(f.statut())));
    }

    @Test
    void findMyCurrent_should_return_paid_abonnement_when_active() {
        Abonnement abonnement = pendingAbonnement();
        abonnement.setStatut(AbonnementStatut.ACTIF);
        abonnement.setActif(true);
        abonnement.setDateDebut(LocalDate.now().minusDays(10));
        abonnement.setDateFin(LocalDate.now().plusDays(20));

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(abonnement));

        CurrentAbonnementResponse response = service.findMyCurrent();

        assertThat(response.joursRestants()).isEqualTo(20);
        assertThat(response.abonnement().id()).isEqualTo(abonnement.getId());
        assertThat(response.abonnement().statut()).isEqualTo(AbonnementStatut.ACTIF);
        assertThat(response.fonctionnalites()).isNotNull();
    }

    @Test
    void findMyCurrent_should_return_trial_abonnement_when_running() {
        Abonnement trial = pendingAbonnement();
        trial.setStatut(AbonnementStatut.TRIAL);
        trial.setActif(true);
        trial.setDateDebut(LocalDate.now().minusDays(15));
        trial.setDateFin(LocalDate.now().plusDays(15));

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(trial));

        CurrentAbonnementResponse response = service.findMyCurrent();

        assertThat(response.abonnement().statut()).isEqualTo(AbonnementStatut.TRIAL);
        assertThat(response.joursRestants()).isEqualTo(15);
        assertThat(response.fonctionnalites()).isNotNull();
    }

    @Test
    void findMyCurrent_should_throw_when_no_active_and_no_trial() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findMyCurrent())
                .isInstanceOf(EntityException.class);
    }

    @Test
    void hasActiveSubscription_should_return_true_when_current_exists() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(pendingAbonnement()));

        assertThat(service.hasActiveSubscription(entrepriseId)).isTrue();
    }

    @Test
    void hasActiveSubscription_should_return_false_when_no_current() {
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.empty());

        assertThat(service.hasActiveSubscription(entrepriseId)).isFalse();
    }

    private Abonnement pendingAbonnement() {
        Abonnement a = new Abonnement();
        a.setId(UUID.randomUUID());
        a.setEntreprise(entreprise);
        a.setTypePlanAbonnement(type);
        a.setStatut(AbonnementStatut.EN_ATTENTE);
        a.setActif(false);
        return a;
    }

    private Coupon validCoupon() {
        Coupon coupon = new Coupon();
        coupon.setCode("PROMO10");
        coupon.setActif(true);
        coupon.setDateDebut(LocalDate.now().minusDays(7));
        coupon.setDateFin(LocalDate.now().plusDays(30));
        coupon.setReductionType(ReductionType.POURCENTAGE);
        coupon.setValeurReduction(new BigDecimal("10"));
        coupon.setNombreUtilisationsMax(100);
        coupon.setNombreUtilisations(0);
        return coupon;
    }
}
