package com.nexxserve.nexxclinic.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidPhoneNumberValidatorTest {

    private final ValidPhoneNumberValidator validator = new ValidPhoneNumberValidator();

    @Test
    void acceptsNullOrBlankAsOptional() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("", null));
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    void acceptsValidPhoneNumbers() {
        assertTrue(validator.isValid("0788123456", null));
        assertTrue(validator.isValid("+250788123456", null));
        assertTrue(validator.isValid("+250 788 123 456", null));
        assertTrue(validator.isValid("+1 (555) 123-4567", null));
        assertTrue(validator.isValid("250788123456", null));
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        assertFalse(validator.isValid("abc", null));
        assertFalse(validator.isValid("123456", null));
        assertFalse(validator.isValid("12-34-56", null));
        assertFalse(validator.isValid("+", null));
        assertFalse(validator.isValid("+2507881234567890123456", null));
        assertFalse(validator.isValid("788-12-3", null));
    }
}
