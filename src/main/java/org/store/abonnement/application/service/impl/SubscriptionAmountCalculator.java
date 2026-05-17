package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Component;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.domain.enums.ReductionType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcule le montant à payer pour une souscription en appliquant séquentiellement :
 * prix de base (plan.prix × dureeMois) → réduction du type → réduction d'une promotion active → réduction du coupon.
 * Aucun montant n'est jamais négatif (clamp à zéro).
 */
@Component
public class SubscriptionAmountCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    /**
     * Applique les réductions dans l'ordre type → promotion → coupon et retourne le détail.
     */
    public SubscriptionAmountBreakdown calculate(SubscriptionAmountInputs inputs) {
        BigDecimal prixDeBase = inputs.plan().getPrix()
                .multiply(BigDecimal.valueOf(inputs.type().getDureeMois()))
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal reductionType = reductionOf(prixDeBase,
                inputs.type().getReductionType(), inputs.type().getValeurReduction());
        BigDecimal apresType = clamp(prixDeBase.subtract(reductionType));

        BigDecimal reductionPromotion = inputs.promotion() == null ? BigDecimal.ZERO
                : reductionOf(apresType, inputs.promotion().getReductionType(), inputs.promotion().getValeurReduction());
        BigDecimal apresPromotion = clamp(apresType.subtract(reductionPromotion));

        BigDecimal reductionCoupon = inputs.coupon() == null ? BigDecimal.ZERO
                : reductionOf(apresPromotion, inputs.coupon().getReductionType(), inputs.coupon().getValeurReduction());
        BigDecimal montantAPayer = clamp(apresPromotion.subtract(reductionCoupon));

        return new SubscriptionAmountBreakdown(
                prixDeBase,
                reductionType.setScale(SCALE, RoundingMode.HALF_UP),
                reductionPromotion.setScale(SCALE, RoundingMode.HALF_UP),
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
