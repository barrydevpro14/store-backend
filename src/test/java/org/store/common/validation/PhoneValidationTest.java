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
            "+221770000000",
            "+221751234567",
            "+33612345678",
            "+14155551234",
            "+447911123456",
            "+861234567890",
            "+12"
    })
    void should_accept_valid_e164_numbers(String value) {
        assertThat(validator.isValid(value, context)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "770000000",
            "0612345678",
            "+0221770000000",
            "+",
            "+1",
            "+1234567890123456",
            "+221 770000000",
            "+221-770000000",
            "+221ABC0000000"
    })
    void should_reject_invalid_numbers(String value) {
        assertThat(validator.isValid(value, context)).isFalse();
    }
}
