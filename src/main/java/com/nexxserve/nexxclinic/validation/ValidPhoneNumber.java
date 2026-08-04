package com.nexxserve.nexxclinic.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Accepts {@code null}/{@code blank} (optional fields) or a phone number made of
 * digits with an optional leading {@code +} and separators (space, dash, dot,
 * parenthesis) — 7 to 15 digits in total.
 */
@Documented
@Constraint(validatedBy = ValidPhoneNumberValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidPhoneNumber {

    String message() default "must be a valid phone number (7-15 digits, optional leading +)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
