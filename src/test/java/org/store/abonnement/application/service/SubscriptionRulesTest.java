package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.tools.SubscriptionRules;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionRulesTest {

    @Test
    void ensureReductionConsistent_should_pass_when_both_null() {
        assertThatCode(() -> SubscriptionRules.ensureReductionConsistent(null, null, "invalid"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureReductionConsistent_should_pass_when_pourcentage_valid() {
        assertThatCode(() -> SubscriptionRules.ensureReductionConsistent(
                ReductionType.POURCENTAGE, new BigDecimal("15"), "invalid"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureReductionConsistent_should_pass_when_montant_fixe_valid() {
        assertThatCode(() -> SubscriptionRules.ensureReductionConsistent(
                ReductionType.MONTANT_FIXE, new BigDecimal("5000"), "invalid"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureReductionConsistent_should_throw_when_type_without_value() {
        assertThatThrownBy(() -> SubscriptionRules.ensureReductionConsistent(
                ReductionType.POURCENTAGE, null, "reduction.invalid"))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensureReductionConsistent_should_throw_when_value_without_type() {
        assertThatThrownBy(() -> SubscriptionRules.ensureReductionConsistent(
                null, new BigDecimal("10"), "reduction.invalid"))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensureReductionConsistent_should_throw_when_pourcentage_above_100() {
        assertThatThrownBy(() -> SubscriptionRules.ensureReductionConsistent(
                ReductionType.POURCENTAGE, new BigDecimal("150"), "reduction.invalid"))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void ensurePeriodValid_should_pass_when_dateFin_after_dateDebut() {
        assertThatCode(() -> SubscriptionRules.ensurePeriodValid(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "invalidPeriod"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensurePeriodValid_should_pass_when_dateFin_equal_dateDebut() {
        LocalDate sameDate = LocalDate.of(2026, 6, 15);
        assertThatCode(() -> SubscriptionRules.ensurePeriodValid(sameDate, sameDate, "invalidPeriod"))
                .doesNotThrowAnyException();
    }

    @Test
    void ensurePeriodValid_should_throw_when_dateFin_before_dateDebut() {
        assertThatThrownBy(() -> SubscriptionRules.ensurePeriodValid(
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), "invalidPeriod"))
                .isInstanceOf(BadArgumentException.class);
    }
}
