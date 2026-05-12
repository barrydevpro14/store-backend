package org.store.common.validation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.store.abonnement.domain.enums.AbonnementStatut;

import static org.assertj.core.api.Assertions.assertThat;

class EnumValidatorTest {

    private EnumValidator newValidator(boolean ignoreCase) {
        EnumValue annotation = Mockito.mock(EnumValue.class);
        Mockito.doReturn(AbonnementStatut.class).when(annotation).enumClass();
        Mockito.when(annotation.ignoreCase()).thenReturn(ignoreCase);
        EnumValidator validator = new EnumValidator();
        validator.initialize(annotation);
        return validator;
    }

    @Test
    void should_accept_null() {
        assertThat(newValidator(false).isValid(null, null)).isTrue();
    }

    @Test
    void should_accept_empty_string() {
        assertThat(newValidator(false).isValid("", null)).isTrue();
    }

    @Test
    void should_accept_valid_enum_name_case_sensitive() {
        assertThat(newValidator(false).isValid("ACTIF", null)).isTrue();
    }

    @Test
    void should_reject_wrong_case_when_case_sensitive() {
        assertThat(newValidator(false).isValid("actif", null)).isFalse();
    }

    @Test
    void should_accept_any_case_when_ignore_case_true() {
        EnumValidator validator = newValidator(true);
        assertThat(validator.isValid("actif", null)).isTrue();
        assertThat(validator.isValid("Actif", null)).isTrue();
        assertThat(validator.isValid("ACTIF", null)).isTrue();
    }

    @Test
    void should_reject_unknown_value() {
        assertThat(newValidator(false).isValid("INCONNU", null)).isFalse();
    }
}
