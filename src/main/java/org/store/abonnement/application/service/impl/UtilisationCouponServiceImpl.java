package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;

import java.util.Optional;
import java.util.UUID;

/**
 * Manages coupon usage records: creation at billing time, lookup for reservation checks, and deletion
 * on payment rejection.
 */
@Service
@Transactional(readOnly = true)
public class UtilisationCouponServiceImpl implements IUtilisationCouponService {

    private final UtilisationCouponDomainService utilisationCouponDomainService;

    public UtilisationCouponServiceImpl(UtilisationCouponDomainService utilisationCouponDomainService) {
        this.utilisationCouponDomainService = utilisationCouponDomainService;
    }

    /** Creates a coupon usage record linked to the generated invoice (billing scheduler). */
    @Override
    @Transactional
    public UtilisationCoupon createWithPaiement(Coupon coupon, Abonnement abonnement, PaiementAbonnement paiement) {
        return utilisationCouponDomainService.createWithPaiement(coupon, abonnement, paiement);
    }

    /** Returns the coupon ID reserved for the given abonnement, or empty when none. */
    @Override
    public Optional<UUID> findCouponIdByAbonnementId(UUID abonnementId) {
        return utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId);
    }

    /** Deletes all coupon usage records for the given abonnement (coupon release on payment rejection). */
    @Override
    @Transactional
    public void deleteByAbonnementId(UUID abonnementId) {
        utilisationCouponDomainService.deleteByAbonnementId(abonnementId);
    }
}
