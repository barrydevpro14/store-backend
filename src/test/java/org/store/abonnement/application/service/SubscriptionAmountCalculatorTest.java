package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionAmountCalculatorTest {

    private final SubscriptionAmountCalculator calculator = new SubscriptionAmountCalculator();

    private PlanAbonnementTarif tarif(String prix) {
        PlanAbonnementTarif t = new PlanAbonnementTarif();
        t.setPrix(new BigDecimal(prix));
        return t;
    }

    private Coupon coupon(ReductionType reductionType, String valeur) {
        Coupon c = new Coupon();
        c.setReductionType(reductionType);
        c.setValeurReduction(new BigDecimal(valeur));
        return c;
    }

    @Test
    void should_compute_base_price_without_any_reduction() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(
                new SubscriptionAmountInputs(tarif("10000"), null));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("10000.00");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("0");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("10000.00");
    }

    @Test
    void should_apply_pourcentage_coupon() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(
                new SubscriptionAmountInputs(tarif("10000"), coupon(ReductionType.POURCENTAGE, "15")));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("10000.00");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("1500.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("8500.00");
    }

    @Test
    void should_apply_montant_fixe_coupon() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(
                new SubscriptionAmountInputs(tarif("10000"), coupon(ReductionType.MONTANT_FIXE, "1000")));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("10000.00");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("1000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("9000.00");
    }

    @Test
    void should_clamp_montant_to_zero_when_reduction_exceeds_base() {
        SubscriptionAmountBreakdown breakdown = calculator.calculate(
                new SubscriptionAmountInputs(tarif("1000"), coupon(ReductionType.MONTANT_FIXE, "5000")));

        assertThat(breakdown.prixDeBase()).isEqualByComparingTo("1000.00");
        assertThat(breakdown.reductionCoupon()).isEqualByComparingTo("5000.00");
        assertThat(breakdown.montantAPayer()).isEqualByComparingTo("0.00");
    }
}
