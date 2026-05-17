package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypeAbonnement;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionAmountCalculatorTest {

    private final SubscriptionAmountCalculator calculator = new SubscriptionAmountCalculator();

    private PlanAbonnement plan(String prix) {
        PlanAbonnement p = new PlanAbonnement();
        p.setPrix(new BigDecimal(prix));
        return p;
    }

    private TypeAbonnement type(int dureeMois, ReductionType reductionType, String valeur) {
        TypeAbonnement t = new TypeAbonnement();
        t.setDureeMois(dureeMois);
        t.setReductionType(reductionType);
        t.setValeurReduction(valeur == null ? null : new BigDecimal(valeur));
        return t;
    }

    private Promotion promotion(ReductionType reductionType, String valeur) {
        Promotion p = new Promotion();
        p.setReductionType(reductionType);
        p.setValeurReduction(new BigDecimal(valeur));
        return p;
    }

    private Coupon coupon(ReductionType reductionType, String valeur) {
        Coupon c = new Coupon();
        c.setReductionType(reductionType);
        c.setValeurReduction(new BigDecimal(valeur));
        return c;
    }

    @Test
    void should_compute_base_price_without_any_reduction() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("10000"), type(1, null, null), null, null));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("10000.00");
        assertThat(breakdown.reductionType()).isEqualByComparingTo("0");
        assertThat(breakdown.reductionPromotion()).isEqualByComparingTo("0");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("0");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("10000.00");
    }

    @Test
    void should_multiply_by_duree_mois() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("10000"), type(12, null, null), null, null));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("120000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("120000.00");
    }

    @Test
    void should_apply_pourcentage_reduction_from_type() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("10000"), type(12, ReductionType.POURCENTAGE, "15"), null, null));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("120000.00");
        assertThat(breakdown.reductionType()).isEqualByComparingTo("18000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("102000.00");
    }

    @Test
    void should_apply_montant_fixe_reduction_from_type() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("10000"), type(12, ReductionType.MONTANT_FIXE, "5000"), null, null));

        assertThat(breakdown.reductionType()).isEqualByComparingTo("5000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("115000.00");
    }

    @Test
    void should_apply_promotion_then_coupon_sequentially() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("10000"), type(12, ReductionType.POURCENTAGE, "10"),
                promotion(ReductionType.POURCENTAGE, "20"),
                coupon(ReductionType.MONTANT_FIXE, "1000")));

        // base 120000, type -10% = 108000, promo -20% sur 108000 = 86400, coupon -1000 = 85400
        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("120000.00");
        assertThat(breakdown.reductionType()).isEqualByComparingTo("12000.00");
        assertThat(breakdown.reductionPromotion()).isEqualByComparingTo("21600.00");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("1000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("85400.00");
    }

    @Test
    void should_clamp_montant_to_zero_when_reductions_exceed_base() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(new SubscriptionAmountInputs(
                plan("1000"), type(1, ReductionType.MONTANT_FIXE, "5000"), null, null));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("1000.00");
        assertThat(breakdown.reductionType()).isEqualByComparingTo("5000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("0.00");
    }
}
