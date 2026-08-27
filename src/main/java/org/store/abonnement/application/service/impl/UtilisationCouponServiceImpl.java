package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.UtilisationCoupon;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;

/**
 * Manages coupon usage records: creation at billing time, linking the coupon application to the
 * generated invoice (billing scheduler).
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
}
