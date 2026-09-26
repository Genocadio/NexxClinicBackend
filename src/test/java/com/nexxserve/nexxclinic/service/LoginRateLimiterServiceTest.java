package com.nexxserve.nexxclinic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterServiceTest {

    private LoginRateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        // 3 max attempts, 10 minutes lockout
        rateLimiter = new LoginRateLimiterService(3, 10);
    }

    @Test
    @DisplayName("isBlocked returns false initially for any identifier")
    void isBlocked_initiallyFalse() {
        assertFalse(rateLimiter.isBlocked("admin@clinic.com"));
        assertFalse(rateLimiter.isBlocked("doctor"));
        assertEquals(0, rateLimiter.getRemainingLockoutMinutes("admin@clinic.com"));
    }

    @Test
    @DisplayName("blocks identifier after reaching max failed attempts")
    void recordsFailedAttempts_andBlocksWhenThresholdReached() {
        String identifier = "user@example.com";

        rateLimiter.recordFailedAttempt(identifier);
        assertFalse(rateLimiter.isBlocked(identifier));

        rateLimiter.recordFailedAttempt(identifier);
        assertFalse(rateLimiter.isBlocked(identifier));

        // 3rd attempt triggers lockout
        rateLimiter.recordFailedAttempt(identifier);
        assertTrue(rateLimiter.isBlocked(identifier));
        assertTrue(rateLimiter.getRemainingLockoutMinutes(identifier) > 0);
        assertTrue(rateLimiter.getRemainingLockoutMinutes(identifier) <= 10);
    }

    @Test
    @DisplayName("normalizes identifiers (case-insensitive and trimmed)")
    void normalizesIdentifiers() {
        rateLimiter.recordFailedAttempt("  Admin@Clinic.COM ");
        rateLimiter.recordFailedAttempt("ADMIN@CLINIC.COM");
        rateLimiter.recordFailedAttempt("admin@clinic.com");

        assertTrue(rateLimiter.isBlocked("admin@clinic.com"));
        assertTrue(rateLimiter.isBlocked("ADMIN@CLINIC.COM"));
        assertTrue(rateLimiter.isBlocked("  admin@clinic.com  "));
    }

    @Test
    @DisplayName("recordSuccessfulAttempt clears failed attempts counter")
    void recordSuccessfulAttempt_clearsLockout() {
        String identifier = "nurse@clinic.com";

        rateLimiter.recordFailedAttempt(identifier);
        rateLimiter.recordFailedAttempt(identifier);
        rateLimiter.recordSuccessfulAttempt(identifier);

        // Counter is reset; 1 more failed attempt should not trigger lockout
        rateLimiter.recordFailedAttempt(identifier);
        assertFalse(rateLimiter.isBlocked(identifier));
    }

    @Test
    @DisplayName("reset explicitly unlocks an identifier")
    void reset_unlocksIdentifier() {
        String identifier = "locked@clinic.com";
        rateLimiter.recordFailedAttempt(identifier);
        rateLimiter.recordFailedAttempt(identifier);
        rateLimiter.recordFailedAttempt(identifier);

        assertTrue(rateLimiter.isBlocked(identifier));

        rateLimiter.reset(identifier);
        assertFalse(rateLimiter.isBlocked(identifier));
        assertEquals(0, rateLimiter.getRemainingLockoutMinutes(identifier));
    }
}
