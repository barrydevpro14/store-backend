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
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.AbonnementServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.property.SubscriptionProperties;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementServiceImplTest {

    @Mock private AbonnementDomainService abonnementDomainService;
    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private IPlanAbonnementService planAbonnementService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private SubscriptionAmountCalculator amountCalculator;
    @Mock private SubscriptionProperties subscriptionProperties;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private AbonnementServiceImpl service;

    private UUID entrepriseId;
    private UUID planId;
    private Entreprise entreprise;
    private PlanAbonnement plan;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        planId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(planId);
        plan.setNom("Pro");
        plan.setPrix(new BigDecimal("19900"));
        plan.setActif(true);
        plan.setVisible(true);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null,
                "owner", null, null, "OWNER", List.of("SUBSCRIPTION_CREATE"));
    }

    private SubscriptionAmountBreakdown sampleBreakdown(String montant) {
        return new SubscriptionAmountBreakdown(
                new BigDecimal("19900.00"), BigDecimal.ZERO,
                new BigDecimal(montant));
    }

    @Test
    void subscribe_should_create_pending_abonnement() {
        SubscribeRequest request = new SubscribeRequest(planId);
        Abonnement pending = pendingAbonnement();
        SubscriptionAmountBreakdown breakdown = sampleBreakdown("19900.00");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(abonnementDomainService.hasPendingByEntreprise(entrepriseId)).thenReturn(false);
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(breakdown);
        when(abonnementDomainService.createPending(entreprise, plan)).thenReturn(pending);

        SubscribeResponse response = service.subscribe(request);

        assertThat(response.abonnement().id()).isEqualTo(pending.getId());
        assertThat(response.abonnement().statut()).isEqualTo(AbonnementStatut.EN_ATTENTE);
        assertThat(response.breakdown()).isSameAs(breakdown);
    }

    @Test
    void subscribe_should_mark_entreprise_trial_used_when_subscribing() {
        SubscribeRequest request = new SubscribeRequest(planId);
        Abonnement pending = pendingAbonnement();
        entreprise.setTrialUsed(false);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(abonnementDomainService.hasPendingByEntreprise(entrepriseId)).thenReturn(false);
        when(planAbonnementService.findById(planId)).thenReturn(plan);
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(sampleBreakdown("19900.00"));
        when(abonnementDomainService.createPending(entreprise, plan)).thenReturn(pending);

        service.subscribe(request);

        assertThat(entreprise.isTrialUsed()).isTrue();
    }

    @Test
    void subscribe_should_throw_when_plan_inactive() {
        plan.setActif(false);
        SubscribeRequest request = new SubscribeRequest(planId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(abonnementDomainService.hasPendingByEntreprise(entrepriseId)).thenReturn(false);
        when(planAbonnementService.findById(planId)).thenReturn(plan);

        assertThatThrownBy(() -> service.subscribe(request))
                .isInstanceOf(BadArgumentException.class);

        verify(abonnementDomainService, never()).createPending(any(), any());
    }

    @Test
    void findAll_should_delegate_unchanged_for_admin() {
        AbonnementFilter filter = new AbonnementFilter(null, "ACTIF", null, null, null, 0, 10);
        Page<AbonnementResponse> page = new PageImpl<>(java.util.List.of());
        when(abonnementDomainService.findResponses(filter)).thenReturn(page);

        assertThat(service.findAll(filter)).isSameAs(page);
    }

    @Test
    void findMyHistory_should_force_entrepriseId_from_current_user() {
        AbonnementFilter filter = new AbonnementFilter(null, "EN_ATTENTE", null, null, null, 0, 10);
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
        a.setPlanAbonnement(plan);
        a.setStatut(AbonnementStatut.EN_ATTENTE);
        return a;
    }

    private Abonnement activeAbonnement() {
        Abonnement a = pendingAbonnement();
        a.setStatut(AbonnementStatut.ACTIF);
        a.setDateDebut(LocalDate.now().minusDays(15));
        a.setDateFin(LocalDate.now().plusDays(15));
        return a;
    }

    @Test
    void createTrialForSignup_should_create_trial_when_plan_exists() {
        PlanAbonnement trialPlan = new PlanAbonnement();
        trialPlan.setId(UUID.randomUUID());
        Abonnement trial = new Abonnement();
        trial.setId(UUID.randomUUID());
        trial.setStatut(AbonnementStatut.TRIAL);

        when(planAbonnementService.findFirstTrialActif()).thenReturn(trialPlan);
        when(subscriptionProperties.trialDays()).thenReturn(30);
        when(abonnementDomainService.createTrial(entreprise, trialPlan, 30)).thenReturn(trial);

        Abonnement result = service.createTrialForSignup(entreprise);

        assertThat(result.getStatut()).isEqualTo(AbonnementStatut.TRIAL);
        verify(abonnementDomainService).createTrial(entreprise, trialPlan, 30);
    }

    @Test
    void createTrialForSignup_should_throw_when_no_trial_plan() {
        when(planAbonnementService.findFirstTrialActif()).thenThrow(new EntityException("plan.trial.notFound"));

        assertThatThrownBy(() -> service.createTrialForSignup(entreprise))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void changerPlan_should_set_prochain_plan_when_valid() {
        UUID newPlanId = UUID.randomUUID();
        PlanAbonnement newPlan = new PlanAbonnement();
        newPlan.setId(newPlanId);
        newPlan.setActif(true);
        newPlan.setVisible(true);
        Abonnement current = activeAbonnement();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(current));
        when(planAbonnementService.findById(newPlanId)).thenReturn(newPlan);
        when(abonnementDomainService.save(current)).thenReturn(current);

        AbonnementResponse response = service.changerPlan(newPlanId);

        assertThat(current.getProchainPlan()).isEqualTo(newPlan);
        assertThat(response).isNotNull();
        verify(abonnementDomainService).save(current);
    }

    @Test
    void changerPlan_should_throw_when_same_plan() {
        Abonnement current = activeAbonnement();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.changerPlan(planId))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void changerPlan_should_throw_when_no_active_abonnement() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findCurrent(entrepriseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changerPlan(UUID.randomUUID()))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void cancelByAdmin_should_cancel_abonnement() {
        Abonnement abonnement = activeAbonnement();
        Abonnement cancelled = activeAbonnement();
        cancelled.setStatut(AbonnementStatut.SUSPENDU);

        when(abonnementDomainService.findById(abonnement.getId())).thenReturn(abonnement);
        when(abonnementDomainService.cancel(abonnement)).thenReturn(cancelled);

        AbonnementResponse response = service.cancelByAdmin(abonnement.getId());

        assertThat(response.statut()).isEqualTo(AbonnementStatut.SUSPENDU);
        verify(abonnementDomainService).cancel(abonnement);
    }

    @Test
    void findMyPending_should_delegate_to_domain() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findPendingResponseByEntreprise(entrepriseId))
                .thenReturn(Optional.empty());

        Optional<AbonnementResponse> result = service.findMyPending();

        assertThat(result).isEmpty();
        verify(abonnementDomainService).findPendingResponseByEntreprise(entrepriseId);
    }

    @Test
    void findExpiringOnDates_should_delegate_to_domain() {
        List<LocalDate> dates = List.of(LocalDate.now(), LocalDate.now().plusDays(3));
        Abonnement abonnement = activeAbonnement();
        when(abonnementDomainService.findExpiringOnDates(dates)).thenReturn(List.of(abonnement));

        List<Abonnement> result = service.findExpiringOnDates(dates);

        assertThat(result).containsExactly(abonnement);
    }

    @Test
    void ensurePlanSubscribable_should_pass_when_active_and_visible() {
        plan.setTrial(false);
        service.ensurePlanSubscribable(plan);
    }

    @Test
    void ensurePlanSubscribable_should_throw_when_inactive() {
        plan.setActif(false);

        assertThatThrownBy(() -> service.ensurePlanSubscribable(plan))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensurePlanSubscribable_should_throw_when_not_visible() {
        plan.setVisible(false);

        assertThatThrownBy(() -> service.ensurePlanSubscribable(plan))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensurePlanSubscribable_should_throw_when_trial() {
        plan.setTrial(true);

        assertThatThrownBy(() -> service.ensurePlanSubscribable(plan))
                .isInstanceOf(BadArgumentException.class);
    }
}
