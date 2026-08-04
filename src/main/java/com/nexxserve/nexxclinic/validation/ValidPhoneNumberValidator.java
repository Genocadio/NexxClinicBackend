package com.nexxserve.nexxclinic.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private static final Pattern DIGITS_WITH_OPTIONAL_PLUS = Pattern.compile("^\\+?[0-9]{7,15}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Optional field: presence/blank-ness is governed by @NotNull/@NotBlank.
            return true;
        }
        String normalized = value.replaceAll("[\\s\\-().]", "");
        return DIGITS_WITH_OPTIONAL_PLUS.matcher(normalized).matches();
    }
}
