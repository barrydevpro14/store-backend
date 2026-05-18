package org.store.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidation implements ConstraintValidator<Phone, String> {

    /**
     * Format international E.164 (ITU-T) : {@code +} suivi d'un code pays (1er chiffre ≥ 1)
     * puis du numéro abonné, total max 15 chiffres après le {@code +}.
     * <p>Exemples valides : {@code +221770000000} (Sénégal), {@code +33612345678} (France),
     * {@code +14155551234} (USA).
     */
    private static final String PHONE_PATTERN = "^\\+[1-9]\\d{1,14}$";

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        return s.matches(PHONE_PATTERN);
    }
}
