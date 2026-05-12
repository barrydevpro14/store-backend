package org.store.common.validation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class DatePatternValidationTest {

    private DatePatternValidation newValidator(String pattern) {
        DatePattern annotation = Mockito.mock(DatePattern.class);
        Mockito.when(annotation.pattern()).thenReturn(pattern);
        DatePatternValidation validator = new DatePatternValidation();
        validator.initialize(annotation);
        return validator;
    }

    @Test
    void should_accept_null() {
        assertThat(newValidator("yyyy-MM-dd").isValid(null, null)).isTrue();
    }

    @Test
    void should_accept_empty_string() {
        assertThat(newValidator("yyyy-MM-dd").isValid("", null)).isTrue();
    }

    @Test
    void should_accept_valid_iso_date() {
        assertThat(newValidator("yyyy-MM-dd").isValid("2026-05-12", null)).isTrue();
    }

    @Test
    void should_reject_invalid_date() {
        assertThat(newValidator("yyyy-MM-dd").isValid("not-a-date", null)).isFalse();
    }

    @Test
    void should_reject_wrong_format() {
        assertThat(newValidator("yyyy-MM-dd").isValid("12/05/2026", null)).isFalse();
    }

    @Test
    void should_accept_custom_pattern() {
        assertThat(newValidator("dd/MM/yyyy").isValid("12/05/2026", null)).isTrue();
        assertThat(newValidator("dd/MM/yyyy").isValid("2026-05-12", null)).isFalse();
    }
}
