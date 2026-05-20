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
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.application.service.ISubscriptionTypeService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Orchestre le cycle de vie des abonnements : création de trial (interne) et souscription propriétaire.
 */
@Service
@Transactional(readOnly = true)
public class AbonnementServiceImpl implements IAbonnementService {

    private final AbonnementDomainService abonnementDomainService;
    private final IPlanAbonnementService planAbonnementService;
    private final ISubscriptionTypeService subscriptionTypeService;
    private final CouponDomainService couponDomainService;
    private final PromotionDomainService promotionDomainService;
    private final UtilisationCouponDomainService utilisationCouponDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ValidatorService validatorService;

    public AbonnementServiceImpl(AbonnementDomainService abonnementDomainService,
                                 IPlanAbonnementService planAbonnementService,
                                 ISubscriptionTypeService subscriptionTypeService,
                                 CouponDomainService couponDomainService,
                                 PromotionDomainService promotionDomainService,
                                 UtilisationCouponDomainService utilisationCouponDomainService,
                                 IEntrepriseService entrepriseService,
                                 ICurrentUserService currentUserService,
                                 SubscriptionAmountCalculator amountCalculator,
                                 ValidatorService validatorService) {
        this.abonnementDomainService = abonnementDomainService;
        this.planAbonnementService = planAbonnementService;
        this.subscriptionTypeService = subscriptionTypeService;
        this.couponDomainService = couponDomainService;
        this.promotionDomainService = promotionDomainService;
        this.utilisationCouponDomainService = utilisationCouponDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.amountCalculator = amountCalculator;
        this.validatorService = validatorService;
    }

    /** Création d'un abonnement trial à 30 jours pour l'inscription propriétaire. */
    @Override
    @Transactional
    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan) {
        return abonnementDomainService.createTrial(entreprise, plan);
    }

    /**
     * Souscription OWNER.
     *
     * Étapes :
     * 1. Résout l'entreprise du caller.
     * 2. Charge plan + type + coupon optionnel, vérifie qu'ils sont applicables.
     * 3. Cherche une promotion automatique active pour le plan (à `today`).
     * 4. Calcule le breakdown des montants via `SubscriptionAmountCalculator`.
     * 5. Crée l'`Abonnement` en EN_ATTENTE (sans dateDebut/dateFin — fixés au paiement).
     * 6. Si coupon : crée `UtilisationCoupon` et incrémente `coupon.nombreUtilisations` (réserve la place).
     */
    @Override
    @Transactional
    public SubscribeResponse subscribe(SubscribeRequest subscribeRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Entreprise entreprise = entrepriseService.findById(currentUser.entrepriseId());

        PlanAbonnement plan = planAbonnementService.findById(subscribeRequest.planId());
        ensurePlanSubscribable(plan);

        TypeAbonnement type = subscriptionTypeService.findById(subscribeRequest.typeId());
        ensureTypeActif(type);

        Coupon coupon = resolveCoupon(subscribeRequest.couponCode(), plan.getId());

        Promotion promotion = promotionDomainService
                .findFirstActivePromotionForPlan(plan.getId(), LocalDate.now())
                .orElse(null);

        SubscriptionAmountBreakdown breakdown = amountCalculator.calculate(
                new SubscriptionAmountInputs(plan, type, promotion, coupon));

        Abonnement abonnement = abonnementDomainService.createPending(entreprise, plan, type);
        abonnementDomainService.setRenouvellementAuto(abonnement, subscribeRequest.renouvellementAuto());

        if (coupon != null) {
            reserveCoupon(coupon, entreprise, abonnement);
        }

        return new SubscribeResponse(
                new AbonnementResponse(abonnement),
                breakdown,
                coupon == null ? null : coupon.getCode(),
                promotion == null ? null : promotion.getNom()
        );
    }

    /** Délègue au domain service ; lève `EntityException` si introuvable. */
    @Override
    public Abonnement findById(UUID id) {
        return abonnementDomainService.findById(id);
    }

    /** Bascule `renouvellementAuto` sur un abonnement scopé à l'entreprise du caller. */
    @Override
    @Transactional
    public AbonnementResponse updateRenouvellementAuto(UUID abonnementId, RenouvellementAutoRequest request) {
        Abonnement abonnement = ensureBelongsToCurrentEntreprise(abonnementDomainService.findById(abonnementId));
        return new AbonnementResponse(
                abonnementDomainService.setRenouvellementAuto(abonnement, request.renouvellementAuto()));
    }

    /** Listing paginé filtré (ADMIN). Aucun auto-scoping ; ADMIN voit tous les abonnements. */
    @Override
    public Page<AbonnementResponse> findAll(AbonnementFilter filter) {
        validatorService.validate(filter);
        return abonnementDomainService.findResponses(filter);
    }

    /** Historique paginé du propriétaire : force `entrepriseId` sur l'entreprise du caller. */
    @Override
    public Page<AbonnementResponse> findMyHistory(AbonnementFilter filter) {
        validatorService.validate(filter);
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        AbonnementFilter scoped = new AbonnementFilter(
                entrepriseId, filter.statut(), filter.planId(), filter.page(), filter.size());
        return abonnementDomainService.findResponses(scoped);
    }

    /** Abonnement courant ACTIF du caller + jours restants + flag trial + fonctionnalités. */
    @Override
    public CurrentAbonnementResponse findMyCurrent() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        Abonnement abonnement = abonnementDomainService.findCurrentActif(entrepriseId)
                .orElseThrow(() -> new EntityException("abonnement.noActive"));

        long joursRestants = abonnement.getDateFin() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), abonnement.getDateFin()));

        return new CurrentAbonnementResponse(
                new AbonnementResponse(abonnement),
                joursRestants,
                abonnement.getPlan() != null && abonnement.getPlan().isTrial(),
                abonnement.getPlan() == null ? null : new PlanFeaturesResponse(abonnement.getPlan())
        );
    }

    /** Lève `BadArgumentException` si le plan n'est pas souscriptible (inactif, invisible ou trial). */
    @Override
    public void ensurePlanSubscribable(PlanAbonnement plan) {
        if (!plan.isActif() || !plan.isVisible() || plan.isTrial()) {
            throw new BadArgumentException("plan.notSubscribable");
        }
    }

    /** Lève `BadArgumentException` si le type d'abonnement est désactivé. */
    public void ensureTypeActif(TypeAbonnement type) {
        if (!type.isActif()) {
            throw new BadArgumentException("subscriptionType.notSubscribable");
        }
    }

    /**
     * Cherche le coupon par code et valide son applicabilité (fenêtre, utilisations restantes, plan match).
     * Retourne null si `code` blank/null.
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

    /** Lève `ForbiddenException` si l'abonnement n'appartient pas à l'entreprise du caller. */
    @Override
    public Abonnement ensureBelongsToCurrentEntreprise(Abonnement abonnement) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!abonnement.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("abonnement.notOwned");
        }
        return abonnement;
    }

    /** Délègue aux domain services : crée `UtilisationCoupon` et incrémente `coupon.nombreUtilisations`. */
    public void reserveCoupon(Coupon coupon, Entreprise entreprise, Abonnement abonnement) {
        utilisationCouponDomainService.create(coupon, entreprise, abonnement);
        couponDomainService.incrementUsage(coupon);
    }
}
