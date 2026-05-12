package org.store.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UuidValidatorTest {

    private final UuidValidator validator = new UuidValidator();

    @Test
    void should_accept_null() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void should_accept_empty_string() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "550e8400-e29b-41d4-a716-446655440000",
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff"
    })
    void should_accept_valid_uuid(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-uuid",
            "550e8400-e29b-41d4-a716",
            "550e8400e29b41d4a716446655440000",
            "550e8400-e29b-41d4-a716-44665544000Z",
            "1234"
    })
    void should_reject_invalid_uuid(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }
}
