package org.store.abonnement.application.service;

import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.UtilisationCoupon;

public interface IUtilisationCouponService {

    /** Creates a coupon usage record linked to the generated invoice (billing scheduler). */
    UtilisationCoupon createWithPaiement(Coupon coupon, Abonnement abonnement, PaiementAbonnement paiement);
}
