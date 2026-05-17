package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.SubscriptionRules;

import java.util.UUID;

/**
 * Gère le catalogue des coupons de promotion (codes promo applicables à un plan optionnel), ADMIN-only.
 */
@Service
@Transactional(readOnly = true)
public class CouponServiceImpl implements ICouponService {

    private final CouponDomainService couponDomainService;
    private final IPlanAbonnementService planAbonnementService;
    private final ValidatorService validatorService;

    public CouponServiceImpl(CouponDomainService couponDomainService,
                             IPlanAbonnementService planAbonnementService,
                             ValidatorService validatorService) {
        this.couponDomainService = couponDomainService;
        this.planAbonnementService = planAbonnementService;
        this.validatorService = validatorService;
    }

    /** Crée un coupon après contrôles d'unicité, période et cohérence réduction, et résolution du plan optionnel. */
    @Override
    @Transactional
    public CouponResponse create(CouponRequest couponRequest) {
        ensureCodeAvailable(couponRequest.code());
        SubscriptionRules.ensurePeriodValid(
                couponRequest.dateDebut(), couponRequest.dateFin(), "coupon.invalidPeriod");
        SubscriptionRules.ensureReductionConsistent(
                couponRequest.reductionTypeAsEnum(),
                couponRequest.valeurReduction(),
                "coupon.reduction.invalid");

        PlanAbonnement plan = planAbonnementService.findByIdOrNull(couponRequest.planId());
        return new CouponResponse(couponDomainService.create(couponRequest, plan));
    }

    /** Délègue au domain service. */
    @Override
    public Coupon findById(UUID id) {
        return couponDomainService.findById(id);
    }

    /** Retourne le coupon en `Response`. */
    @Override
    public CouponResponse findResponseById(UUID id) {
        return new CouponResponse(couponDomainService.findById(id));
    }

    /** Liste paginée filtrée. */
    @Override
    public Page<CouponResponse> findAll(CouponFilter filter) {
        validatorService.validate(filter);
        return couponDomainService.findResponses(filter);
    }

    /** Met à jour ; revérifie unicité (si code changé), période, cohérence réduction. */
    @Override
    @Transactional
    public CouponResponse update(UUID id, CouponRequest couponRequest) {
        Coupon coupon = couponDomainService.findById(id);

        if (!coupon.getCode().equals(couponRequest.code())) {
            ensureCodeAvailable(couponRequest.code());
        }
        SubscriptionRules.ensurePeriodValid(
                couponRequest.dateDebut(), couponRequest.dateFin(), "coupon.invalidPeriod");
        SubscriptionRules.ensureReductionConsistent(
                couponRequest.reductionTypeAsEnum(),
                couponRequest.valeurReduction(),
                "coupon.reduction.invalid");

        PlanAbonnement plan = planAbonnementService.findByIdOrNull(couponRequest.planId());
        couponDomainService.applyRequest(coupon, couponRequest, plan);
        return new CouponResponse(couponDomainService.save(coupon));
    }

    /** Force `actif=true`. */
    @Override
    @Transactional
    public CouponResponse activate(UUID id) {
        Coupon coupon = couponDomainService.findById(id);
        return new CouponResponse(couponDomainService.setActive(coupon, true));
    }

    /** Force `actif=false`. */
    @Override
    @Transactional
    public CouponResponse deactivate(UUID id) {
        Coupon coupon = couponDomainService.findById(id);
        return new CouponResponse(couponDomainService.setActive(coupon, false));
    }

    /** Supprime le coupon. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Coupon coupon = couponDomainService.findById(id);
        couponDomainService.delete(coupon);
    }

    /** Lève `UniqueResourceException` si un coupon porte déjà ce code. */
    @Override
    public void ensureCodeAvailable(String code) {
        if (couponDomainService.existsByCode(code)) {
            throw new UniqueResourceException("coupon.code.alreadyExists", code);
        }
    }
}
