package org.store.abonnement.application.service;

import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.UtilisationCoupon;

import java.util.Optional;
import java.util.UUID;

public interface IUtilisationCouponService {

    /** Creates a coupon usage record linked to the generated invoice (billing scheduler). */
    UtilisationCoupon createWithPaiement(Coupon coupon, Abonnement abonnement, PaiementAbonnement paiement);

    /** Returns the coupon ID reserved for the given abonnement, or empty when none. */
    Optional<UUID> findCouponIdByAbonnementId(UUID abonnementId);

    /** Deletes all coupon usage records for the given abonnement (coupon release on payment rejection). */
    void deleteByAbonnementId(UUID abonnementId);
}
