package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.CouponFilter;
import org.store.abonnement.application.dto.CouponRequest;
import org.store.abonnement.application.dto.CouponResponse;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Coupon;

import java.util.Optional;
import java.util.UUID;

public interface ICouponService {

    /**
     * Création d'un coupon. Unicité du code contrôlée, fenêtre temporelle validée, cohérence type/valeur de réduction vérifiée.
     */
    CouponResponse create(CouponRequest couponRequest);

    /**
     * Lecture interne par id.
     */
    Coupon findById(UUID id);

    /**
     * Lecture par id en `Response`. Throw `EntityException("coupon.notFound")` si introuvable.
     */
    CouponResponse findResponseById(UUID id);

    /**
     * Listing paginé filtré.
     */
    Page<CouponResponse> findAll(CouponFilter filter);

    /**
     * Mise à jour. Unicité du code revérifiée si changement.
     */
    CouponResponse update(UUID id, CouponRequest couponRequest);

    /**
     * Activation du coupon.
     */
    CouponResponse activate(UUID id);

    /**
     * Désactivation du coupon.
     */
    CouponResponse deactivate(UUID id);

    /**
     * Suppression.
     */
    void delete(UUID id);

    /**
     * Throw `UniqueResourceException("coupon.code.alreadyExists")` si un coupon porte déjà ce code.
     */
    void ensureCodeAvailable(String code);

    /** Returns the first coupon applicable for this billing cycle (plan + periodicite + date window), or empty. */
    Optional<Coupon> findApplicable(UUID entrepriseId, UUID planId, PeriodiciteAbonnement periodicite);

    /** Returns the first global coupon (entreprise IS NULL) applicable for a given plan + periodicite, or empty. */
    Optional<Coupon> findApplicableGlobal(UUID planId, PeriodiciteAbonnement periodicite);

    /** Increments the coupon's usage counter and persists. */
    Coupon incrementUsage(Coupon coupon);

    /** Decrements the coupon's usage counter and persists. */
    Coupon decrementUsage(Coupon coupon);

    /** Deactivates the coupon when its usage quota is exhausted. */
    void deactivateIfExhausted(Coupon coupon);
}
