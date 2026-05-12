package org.store.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = DatePatternValidation.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DatePattern {

    String pattern() default "yyyy-MM-dd";

    String message() default "{validation.date.pattern.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
