package org.store.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidation implements ConstraintValidator<Phone, String> {
    private final static String PHONE_PATTERN = "^(70|75|76|77|78|33)\\d{7,9}$";

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        return s.matches(PHONE_PATTERN);
    }
}
