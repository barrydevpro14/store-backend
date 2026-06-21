package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.PlanFeaturesResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.ISubscriptionTypeService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;
import org.store.property.SubscriptionProperties;
import org.store.common.exceptions.BadArgumentException;
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
import java.util.UUID;

/**
 * Orchestrates the Abonnement lifecycle: OWNER signup creates a TRIAL row, paid subscribe creates an
 * EN_ATTENTE row, validation activates it. Trial windows live in the Abonnement table with
 * {@code statut=TRIAL}; the trial-consumed flag on the entreprise stays in sync.
 */
@Service
@Transactional(readOnly = true)
public class AbonnementServiceImpl implements IAbonnementService {

    private final AbonnementDomainService abonnementDomainService;
    private final TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    private final ISubscriptionTypeService subscriptionTypeService;
    private final CouponDomainService couponDomainService;
    private final PromotionDomainService promotionDomainService;
    private final UtilisationCouponDomainService utilisationCouponDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final SubscriptionProperties subscriptionProperties;
    private final ValidatorService validatorService;

    public AbonnementServiceImpl(AbonnementDomainService abonnementDomainService,
                                 TypePlanAbonnementDomainService typePlanAbonnementDomainService,
                                 ISubscriptionTypeService subscriptionTypeService,
                                 CouponDomainService couponDomainService,
                                 PromotionDomainService promotionDomainService,
                                 UtilisationCouponDomainService utilisationCouponDomainService,
                                 IEntrepriseService entrepriseService,
                                 ICurrentUserService currentUserService,
                                 SubscriptionAmountCalculator amountCalculator,
                                 SubscriptionProperties subscriptionProperties,
                                 ValidatorService validatorService) {
        this.abonnementDomainService = abonnementDomainService;
        this.typePlanAbonnementDomainService = typePlanAbonnementDomainService;
        this.subscriptionTypeService = subscriptionTypeService;
        this.couponDomainService = couponDomainService;
        this.promotionDomainService = promotionDomainService;
        this.utilisationCouponDomainService = utilisationCouponDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.amountCalculator = amountCalculator;
        this.subscriptionProperties = subscriptionProperties;
        this.validatorService = validatorService;
    }

    /**
     * Creates the TRIAL Abonnement for a fresh OWNER signup. Looks up the first active type on the
     * trial plan and persists the row with {@code statut=TRIAL}, dateDebut today and
     * dateFin today + {@code subscription.trial-days}.
     */
    @Override
    @Transactional
    public Abonnement createTrialForSignup(Entreprise entreprise) {
        TypePlanAbonnement trialType = typePlanAbonnementDomainService.findFirstActifTrial()
                .orElseThrow(() -> new EntityException("plan.trial.notFound"));
        return abonnementDomainService.createTrial(entreprise, trialType, subscriptionProperties.trialDays());
    }

    /**
     * Owner-facing subscribe flow. Loads the chosen type, derives the plan from {@code type.plan}, validates
     * both, applies the optional coupon + active promotion, computes the amount breakdown, persists the
     * Abonnement in EN_ATTENTE, reserves the coupon (if any) and consumes the trial window on the entreprise.
     */
    @Override
    @Transactional
    public SubscribeResponse subscribe(SubscribeRequest subscribeRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID currentEntrepriseId = currentUser.entrepriseId();
        Entreprise entreprise = entrepriseService.findById(currentEntrepriseId);

        TypePlanAbonnement type = subscriptionTypeService.findById(subscribeRequest.typeId());
        ensureTypeActif(type);
        PlanAbonnement plan = type.getPlan();
        ensurePlanSubscribable(plan);

        Coupon coupon = resolveCoupon(subscribeRequest.couponCode(), plan.getId());

        Promotion promotion = promotionDomainService
                .findFirstActivePromotionForPlan(plan.getId(), LocalDate.now())
                .orElse(null);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(plan, type, promotion, coupon));

        Abonnement abonnement = abonnementDomainService.createPending(entreprise, type);
        abonnementDomainService.setRenouvellementAuto(abonnement, subscribeRequest.renouvellementAuto());

        if (coupon != null) {
            reserveCoupon(coupon, entreprise, abonnement);
        }

        consumeTrialIfAny(entreprise);

        return new SubscribeResponse(
                new AbonnementResponse(abonnement),
                breakdown,
                coupon == null ? null : coupon.getCode(),
                promotion == null ? null : promotion.getNom()
        );
    }

    /**
     * Flags the entreprise as having consumed its trial. The TRIAL Abonnement row is left in
     * place — it expires naturally at {@code dateFin} and stays as historical record.
     */
    @Override
    public void consumeTrialIfAny(Entreprise entreprise) {
        if (!entreprise.isTrialUsed()) {
            entreprise.setTrialUsed(true);
        }
    }

    /** Delegates to the domain service; throws {@code EntityException} when missing. */
    @Override
    public Abonnement findById(UUID id) {
        return abonnementDomainService.findById(id);
    }

    /** Toggles {@code renouvellementAuto} on an Abonnement scoped to the caller's entreprise. */
    @Override
    @Transactional
    public AbonnementResponse updateRenouvellementAuto(UUID abonnementId, RenouvellementAutoRequest renouvellementAutoRequest) {
        Abonnement abonnement = ensureBelongsToCurrentEntreprise(abonnementDomainService.findById(abonnementId));
        return new AbonnementResponse(
                abonnementDomainService.setRenouvellementAuto(abonnement, renouvellementAutoRequest.renouvellementAuto()));
    }

    /** ADMIN count — no auto-scoping; counts all Abonnements in the given date range. */
    @Override
    public long countByCreatedDateRange(String startDate, String endDate) {
        return abonnementDomainService.countByCreatedBetween(startDate, endDate);
    }

    /** ADMIN listing — no auto-scoping; the caller sees every Abonnement. */
    @Override
    public Page<AbonnementResponse> findAll(AbonnementFilter filter) {
        validatorService.validate(filter);
        return abonnementDomainService.findResponses(filter);
    }

    /** Owner history — forces {@code entrepriseId} to the caller's entreprise on the filter. */
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

    /**
     * Returns the caller's "current" Abonnement view: ACTIF paid row if present, otherwise a TRIAL row
     * within its window. Throws {@code abonnement.noActive} when neither is found.
     */
    @Override
    public CurrentAbonnementResponse findMyCurrent() {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        Abonnement current = abonnementDomainService.findCurrent(currentEntrepriseId)
                .orElseThrow(() -> new EntityException("abonnement.noActive"));
        return buildCurrent(current);
    }

    /** Builds the current-view: AbonnementResponse + days left + plan features (works for ACTIF and TRIAL). */
    @Override
    public CurrentAbonnementResponse buildCurrent(Abonnement abonnement) {
        long joursRestants = abonnement.getDateFin() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), abonnement.getDateFin()));

        PlanAbonnement plan = abonnement.getTypePlanAbonnement().getPlan();

        return new CurrentAbonnementResponse(
                new AbonnementResponse(abonnement),
                joursRestants,
                new PlanFeaturesResponse(plan)
        );
    }

    /**
     * Returns {@code true} when the entreprise has an ACTIF or a still-running TRIAL Abonnement.
     * Used as the login subscription gate.
     */
    @Override
    public boolean hasActiveSubscription(UUID entrepriseId) {
        return abonnementDomainService.findCurrent(entrepriseId).isPresent();
    }

    /**
     * @param dates 
     * @return List of abonnements expirings
     */
    @Override
    public List<Abonnement> findExpiringOnDates(List<LocalDate> dates) {
        return abonnementDomainService.findExpiringOnDates(dates);
    }

    /** Throws {@code BadArgumentException("plan.notSubscribable")} when the plan is inactive, hidden or trial. */
    @Override
    public void ensurePlanSubscribable(PlanAbonnement plan) {
        if (!plan.isActif() || !plan.isVisible()) {
            throw new BadArgumentException("plan.notSubscribable");
        }
    }

    /** Throws {@code BadArgumentException("subscriptionType.notSubscribable")} when the type is inactive. */
    public void ensureTypeActif(TypePlanAbonnement type) {
        if (!type.isActif()) {
            throw new BadArgumentException("subscriptionType.notSubscribable");
        }
    }

    /**
     * Loads the coupon by code and validates applicability (window, remaining usage, plan match). Returns
     * {@code null} when the code is null/blank.
     */
    public Coupon resolveCoupon(String code, UUID planId) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Coupon coupon = couponDomainService.findByCode(code)
                .orElseThrow(() -> new EntityException("coupon.notFound", code));

        LocalDate today = LocalDate.now();
        if (!coupon.isActif() || today.isBefore(coupon.getDateDebut()) || today.isAfter(coupon.getDateFin())) {
            throw new BadArgumentException("coupon.expired", coupon.getCode());
        }
        if (coupon.getNombreUtilisationsMax() > 0
                && coupon.getNombreUtilisations() >= coupon.getNombreUtilisationsMax()) {
            throw new BadArgumentException("coupon.exhausted", coupon.getCode());
        }
        if (coupon.getPlan() != null && !coupon.getPlan().getId().equals(planId)) {
            throw new BadArgumentException("coupon.notApplicable", coupon.getCode());
        }
        return coupon;
    }

    /** Throws {@code ForbiddenException("abonnement.notOwned")} when the Abonnement is not owned by the caller. */
    @Override
    public Abonnement ensureBelongsToCurrentEntreprise(Abonnement abonnement) {
        return OwnershipHelper.ensureOwnership(
                abonnement,
                abonnement.getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "abonnement.notOwned"
        );
    }

    /** Delegates to the domain services: creates {@code UtilisationCoupon} and increments {@code coupon.nombreUtilisations}. */
    public void reserveCoupon(Coupon coupon, Entreprise entreprise, Abonnement abonnement) {
        utilisationCouponDomainService.create(coupon, entreprise, abonnement);
        couponDomainService.incrementUsage(coupon);
    }
}
