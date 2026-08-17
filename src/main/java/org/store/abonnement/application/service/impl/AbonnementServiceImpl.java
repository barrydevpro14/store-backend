package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.AbonnementDetailsResponse;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.ChangerPlanRequest;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.PlanFeaturesResponse;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IAbonnementQuotaService;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
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

import java.math.BigDecimal;
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
    private final IPlanAbonnementTarifService tarifService;
    private final ICouponService couponService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final IAbonnementQuotaService quotaService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final SubscriptionProperties subscriptionProperties;
    private final ValidatorService validatorService;

    public AbonnementServiceImpl(AbonnementDomainService abonnementDomainService,
                                 IPaiementAbonnementService paiementAbonnementService,
                                 IPlanAbonnementService planAbonnementService,
                                 IPlanAbonnementTarifService tarifService,
                                 ICouponService couponService,
                                 IEntrepriseService entrepriseService,
                                 ICurrentUserService currentUserService,
                                 IAbonnementQuotaService quotaService,
                                 SubscriptionAmountCalculator amountCalculator,
                                 SubscriptionProperties subscriptionProperties,
                                 ValidatorService validatorService) {
        this.abonnementDomainService = abonnementDomainService;
        this.paiementAbonnementService = paiementAbonnementService;
        this.planAbonnementService = planAbonnementService;
        this.tarifService = tarifService;
        this.couponService = couponService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.quotaService = quotaService;
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

        PeriodiciteAbonnement periodicite = subscribeRequest.periodiciteAsEnum();

        PlanAbonnementTarif tarif = tarifService.findByPlanAndPeriodicite(plan, periodicite)
                .orElseThrow(() -> new EntityException("tarif.notFound"));

        Coupon coupon = couponService
                .findApplicable(currentEntrepriseId, plan.getId(), periodicite)
                .orElse(null);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(tarif, coupon));

        Abonnement abonnement = abonnementDomainService.createPending(entreprise, plan, periodicite);

        paiementAbonnementService.createFactureGeneree(
                new FactureGenereeCommand(abonnement, tarif, coupon, breakdown, LocalDate.now()));

        consumeTrialIfAny(entreprise);

        return new SubscribeResponse(new AbonnementResponse(abonnement), breakdown);
    }

    /**
     * OWNER requests plan and/or periodicite change for the next billing cycle. The current plan stays
     * active until the scheduler validates the next invoice; validate() then applies prochainPlan and
     * prochainePeriodicite. If an unpaid invoice (FACTURE_GENEREE/EN_RETARD) already exists, its
     * tarif, coupon and amounts are recalculated immediately.
     */
    @Override
    @Transactional
    public AbonnementResponse changerPlan(ChangerPlanRequest request) {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        Abonnement abonnement = abonnementDomainService.findCurrent(currentEntrepriseId)
                .orElseThrow(() -> new EntityException("abonnement.noActive"));
        ensureBelongsToCurrentEntreprise(abonnement);

        PlanAbonnement planEffectif = request.planId() != null
                ? planAbonnementService.findById(request.planId())
                : abonnement.getPlanAbonnement();

        PeriodiciteAbonnement periodiciteEffective = request.periodiciteAsEnum() != null
                ? request.periodiciteAsEnum()
                : abonnement.getPeriodicite();

        boolean planChange = request.planId() != null
                && !abonnement.getPlanAbonnement().getId().equals(request.planId());
        boolean periodiciteChange = request.periodiciteAsEnum() != null
                && request.periodiciteAsEnum() != abonnement.getPeriodicite();

        if (!planChange && !periodiciteChange) {
            throw new BadArgumentException("abonnement.changement.aucuneModification");
        }

        if (planChange) ensurePlanSubscribable(planEffectif);
        if (planChange) quotaService.ensureMagasinQuotaForPlan(currentEntrepriseId, planEffectif);

        PlanAbonnementTarif tarif = tarifService.findByPlanAndPeriodicite(planEffectif, periodiciteEffective)
                .orElseThrow(() -> new EntityException("tarif.notFound"));

        if (planChange) abonnement.setProchainPlan(planEffectif);
        if (periodiciteChange) abonnement.setProchainePeriodicite(periodiciteEffective);

        abonnementDomainService.save(abonnement);

        paiementAbonnementService.findFactureNonPayeeByAbonnement(abonnement.getId()).ifPresent(facture -> {
            Coupon coupon = couponService.findApplicable(currentEntrepriseId, planEffectif.getId(), periodiciteEffective).orElse(null);
            SubscriptionAmountInputs inputs = new SubscriptionAmountInputs(tarif, coupon);
            paiementAbonnementService.recalculerFactureNonPayee(facture, inputs, amountCalculator.calculate(inputs));
        });

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
    public AbonnementDetailsResponse findDetailsById(UUID id) {
        Abonnement abonnement = abonnementDomainService.findById(id);

        PeriodiciteAbonnement periodicite = abonnement.getPeriodicite();

        return new AbonnementDetailsResponse(new AbonnementResponse(abonnement), periodicite, resolvePrix(abonnement, periodicite));
    }

    private BigDecimal resolvePrix(Abonnement abonnement, PeriodiciteAbonnement periodicite) {
        if (abonnement.getStatut() == AbonnementStatut.TRIAL) {
            return BigDecimal.ZERO;
        }

        return tarifService.findByPlanAndPeriodicite(abonnement.getPlanAbonnement(), periodicite)
                .map(PlanAbonnementTarif::getPrix)
                .orElse(BigDecimal.ZERO);
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
