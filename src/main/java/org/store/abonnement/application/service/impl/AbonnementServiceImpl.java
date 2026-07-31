package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.PlanFeaturesResponse;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.property.SubscriptionProperties;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.OwnershipHelper;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the Abonnement lifecycle: OWNER signup creates a TRIAL row, paid subscribe creates an
 * EN_ATTENTE row with an immediate FACTURE_GENEREE invoice. Validation activates the subscription
 * and extends dateFin by 1 month on each renewal.
 */
@Service
@Transactional(readOnly = true)
public class AbonnementServiceImpl implements IAbonnementService {

    private final AbonnementDomainService abonnementDomainService;
    private final IPaiementAbonnementService paiementAbonnementService;
    private final IPlanAbonnementService planAbonnementService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final SubscriptionProperties subscriptionProperties;
    private final ValidatorService validatorService;

    public AbonnementServiceImpl(AbonnementDomainService abonnementDomainService,
                                 IPaiementAbonnementService paiementAbonnementService,
                                 IPlanAbonnementService planAbonnementService,
                                 IEntrepriseService entrepriseService,
                                 ICurrentUserService currentUserService,
                                 SubscriptionAmountCalculator amountCalculator,
                                 SubscriptionProperties subscriptionProperties,
                                 ValidatorService validatorService) {
        this.abonnementDomainService = abonnementDomainService;
        this.paiementAbonnementService = paiementAbonnementService;
        this.planAbonnementService = planAbonnementService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.amountCalculator = amountCalculator;
        this.subscriptionProperties = subscriptionProperties;
        this.validatorService = validatorService;
    }

    /**
     * Creates the TRIAL Abonnement for a fresh OWNER signup. Looks up the active trial plan and
     * persists the row with statut=TRIAL, dateDebut today and dateFin today + trial-days.
     */
    @Override
    @Transactional
    public Abonnement createTrialForSignup(Entreprise entreprise) {
        PlanAbonnement trialPlan = planAbonnementService.findFirstTrialActif();
        return abonnementDomainService.createTrial(entreprise, trialPlan, subscriptionProperties.trialDays());
    }

    /**
     * Owner-facing subscribe flow. Validates the plan, computes the amount breakdown,
     * persists the Abonnement in EN_ATTENTE and creates an immediate FACTURE_GENEREE invoice.
     */
    @Override
    @Transactional
    public SubscribeResponse subscribe(SubscribeRequest subscribeRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID currentEntrepriseId = currentUser.entrepriseId();
        Entreprise entreprise = entrepriseService.findById(currentEntrepriseId);

        if (abonnementDomainService.isInactif(currentEntrepriseId)) {
            throw new ForbiddenException("abonnement.inactif.cannotSubscribe");
        }

        if (abonnementDomainService.hasPendingByEntreprise(currentEntrepriseId)) {
            throw new BadArgumentException("abonnement.alreadyPending");
        }

        PlanAbonnement plan = planAbonnementService.findById(subscribeRequest.planId());
        ensurePlanSubscribable(plan);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(plan, null));

        Abonnement abonnement = abonnementDomainService.createPending(entreprise, plan);

        paiementAbonnementService.createFactureGeneree(abonnement, breakdown, LocalDate.now());

        consumeTrialIfAny(entreprise);

        return new SubscribeResponse(new AbonnementResponse(abonnement), breakdown);
    }

    /**
     * OWNER requests plan change for the next billing cycle. The current plan stays active until
     * the scheduler validates the next invoice; then validate() switches planAbonnement <- prochainPlan.
     */
    @Override
    @Transactional
    public AbonnementResponse changerPlan(UUID planId) {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        Abonnement abonnement = abonnementDomainService.findCurrent(currentEntrepriseId)
                .orElseThrow(() -> new EntityException("abonnement.noActive"));
        ensureBelongsToCurrentEntreprise(abonnement);

        if (abonnement.getPlanAbonnement().getId().equals(planId)) {
            throw new BadArgumentException("abonnement.planUnchanged");
        }
        PlanAbonnement prochainPlan = planAbonnementService.findById(planId);
        ensurePlanSubscribable(prochainPlan);

        abonnement.setProchainPlan(prochainPlan);
        abonnementDomainService.save(abonnement);
        return new AbonnementResponse(abonnement);
    }

    @Override
    public void consumeTrialIfAny(Entreprise entreprise) {
        if (!entreprise.isTrialUsed()) {
            entreprise.setTrialUsed(true);
        }
    }

    @Override
    public Abonnement findById(UUID id) {
        return abonnementDomainService.findById(id);
    }

    @Override
    @Transactional
    public AbonnementResponse cancelByAdmin(UUID abonnementId) {
        Abonnement abonnement = abonnementDomainService.findById(abonnementId);
        return new AbonnementResponse(abonnementDomainService.cancel(abonnement));
    }

    @Override
    @Transactional
    public AbonnementResponse reactivateByAdmin(UUID abonnementId) {
        Abonnement abonnement = abonnementDomainService.findById(abonnementId);
        return new AbonnementResponse(abonnementDomainService.reactivate(abonnement));
    }

    @Override
    public long countByCreatedDateRange(String startDate, String endDate) {
        return abonnementDomainService.countByCreatedBetween(startDate, endDate);
    }

    @Override
    public Page<AbonnementResponse> findAll(AbonnementFilter filter) {
        validatorService.validate(filter);
        return abonnementDomainService.findResponses(filter);
    }

    @Override
    public Page<AbonnementResponse> findMyHistory(AbonnementFilter filter) {
        validatorService.validate(filter);
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        AbonnementFilter scoped = new AbonnementFilter(
                currentEntrepriseId, filter.statut(), filter.planId(),
                filter.startDate(), filter.endDate(),
                filter.page(), filter.size());
        return abonnementDomainService.findResponses(scoped);
    }

    @Override
    public CurrentAbonnementResponse findMyCurrent() {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        return abonnementDomainService.findCurrent(currentEntrepriseId)
                .map(this::buildCurrent)
                .orElse(null);
    }

    @Override
    public AbonnementResponse findMyPending() {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        return abonnementDomainService.findPendingResponseByEntreprise(currentEntrepriseId)
                .orElse(null);
    }

    @Override
    public CurrentAbonnementResponse buildCurrent(Abonnement abonnement) {
        long joursRestants = abonnement.getDateFin() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), abonnement.getDateFin()));

        PlanAbonnement plan = abonnement.getPlanAbonnement();

        return new CurrentAbonnementResponse(
                new AbonnementResponse(abonnement),
                joursRestants,
                new PlanFeaturesResponse(plan)
        );
    }

    @Override
    public boolean hasActiveSubscription(UUID entrepriseId) {
        return abonnementDomainService.findCurrent(entrepriseId).isPresent();
    }

    @Override
    public boolean isSuspendedByEntreprise(UUID entrepriseId) {
        return abonnementDomainService.isSuspendu(entrepriseId);
    }

    @Override
    public boolean isInactifByEntreprise(UUID entrepriseId) {
        return abonnementDomainService.isInactif(entrepriseId);
    }

    @Override
    public Optional<org.store.abonnement.domain.enums.AbonnementStatut> findCurrentNonActiveStatut(UUID entrepriseId) {
        return abonnementDomainService.findCurrentNonActiveStatut(entrepriseId);
    }

    @Override
    public List<Abonnement> findExpiringOnDates(List<LocalDate> dates) {
        return abonnementDomainService.findExpiringOnDates(dates);
    }

    @Override
    public List<Abonnement> findAbonnementsToFacture(LocalDate targetDate) {
        return abonnementDomainService.findAbonnementsToFacture(targetDate);
    }

    @Override
    public Abonnement suspend(Abonnement abonnement) {
        return abonnementDomainService.suspend(abonnement);
    }

    @Override
    public void ensurePlanSubscribable(PlanAbonnement plan) {
        if (!plan.isActif() || !plan.isVisible() || plan.isTrial()) {
            throw new BadArgumentException("plan.notSubscribable");
        }
    }

    @Override
    public Abonnement ensureBelongsToCurrentEntreprise(Abonnement abonnement) {
        return OwnershipHelper.ensureOwnership(
                abonnement,
                abonnement.getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "abonnement.notOwned"
        );
    }

    @Override
    public long countByStatut(AbonnementStatut statut) {
        return abonnementDomainService.countByStatut(statut);
    }

}
