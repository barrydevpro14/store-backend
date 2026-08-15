package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Component;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.domain.enums.ReductionType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule le montant à payer pour une souscription : prix de base (tarif.prix) puis réduction
 * du coupon. Aucun montant n'est jamais négatif (clamp à zéro).
 */
@Component
public class SubscriptionAmountCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    /**
     * Applique la réduction du coupon (si présent) sur le prix du tarif et retourne le détail.
     */
    public SubscriptionAmountBreakdown calculate(SubscriptionAmountInputs inputs) {
        BigDecimal prixDeBase = inputs.tarif().getPrix()
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal reductionCoupon = inputs.coupon() == null ? BigDecimal.ZERO
                : reductionOf(prixDeBase, inputs.coupon().getReductionType(), inputs.coupon().getValeurReduction());

        BigDecimal montantAPayer = clamp(prixDeBase.subtract(reductionCoupon));

        return new SubscriptionAmountBreakdown(
                prixDeBase,
                reductionCoupon.setScale(SCALE, RoundingMode.HALF_UP),
                montantAPayer
        );
    }

    private BigDecimal reductionOf(BigDecimal base, ReductionType reductionType, BigDecimal valeur) {
        if (reductionType == null || valeur == null) {
            return BigDecimal.ZERO;
        }
        if (reductionType == ReductionType.POURCENTAGE) {
            return base.multiply(valeur)
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        }
        return valeur.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
                : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
