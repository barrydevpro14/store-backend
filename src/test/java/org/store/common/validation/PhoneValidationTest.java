package org.store.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneValidationTest {

    private PhoneValidation validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PhoneValidation();
        context = null;
    }

    @Test
    void should_accept_null() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void should_accept_empty_string() {
        assertThat(validator.isValid("", context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "770000000",
            "751234567",
            "761234567",
            "771234567",
            "781234567",
            "331234567",
            "77123456789"
    })
    void should_accept_valid_senegal_numbers(String value) {
        assertThat(validator.isValid(value, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "771234",
            "12345678",
            "8012345678",
            "77ABCD123",
            "+221770000000",
            "77 1234567",
            "771234567890123"
    })
    void should_reject_invalid_numbers(String value) {
        assertThat(validator.isValid(value, context)).isFalse();
    }
}
