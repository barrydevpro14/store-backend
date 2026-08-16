package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record PlanAbonnementTarifResponse(
        UUID id,
        PeriodiciteAbonnement periodicite,
        BigDecimal prix,
        boolean actif,
        boolean recommande,
        Integer ordre,
        BigDecimal montantAPayer,
        BigDecimal reductionCoupon,
        Integer reductionPourcentage
) {
    public PlanAbonnementTarifResponse(PlanAbonnementTarif tarif) {
        this(tarif.getId(), tarif.getPeriodicite(), tarif.getPrix(), tarif.isActif(), tarif.isRecommande(), tarif.getOrdre(),
                null, null, null);
    }

    public PlanAbonnementTarifResponse(PlanAbonnementTarif tarif, SubscriptionAmountBreakdown breakdown, Coupon coupon) {
        this(tarif.getId(), tarif.getPeriodicite(), tarif.getPrix(), tarif.isActif(), tarif.isRecommande(), tarif.getOrdre(),
                breakdown.montantAPayer(), breakdown.reductionCoupon(),
                computePourcentage(tarif.getPrix(), breakdown.reductionCoupon(), coupon));
    }

    private static Integer computePourcentage(BigDecimal prix, BigDecimal reductionCoupon, Coupon coupon) {
        if (coupon.getReductionType() == ReductionType.POURCENTAGE) {
            return coupon.getValeurReduction().setScale(0, RoundingMode.HALF_UP).intValue();
        }
        if (prix.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return reductionCoupon.multiply(BigDecimal.valueOf(100))
                .divide(prix, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
