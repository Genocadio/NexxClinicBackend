package com.nexxserve.nexxclinic.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Thread-safe in-memory rate limiter to protect authentication endpoints from brute-force attacks.
 * Tracks consecutive failed login attempts by normalized identifier (username, email, or phone).
 */
@Service
public class LoginRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiterService.class);

    private final int maxAttempts;
    private final Duration lockoutDuration;
    private final Map<String, AttemptTracker> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiterService(
            @Value("${app.security.login.max-failed-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lockout-duration-minutes:5}") int lockoutDurationMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutDurationMinutes);
    }

    /**
     * Checks if the given identifier is currently locked out from logging in.
     */
    public boolean isBlocked(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        String key = normalize(identifier);
        AttemptTracker tracker = attempts.get(key);
        if (tracker == null) {
            return false;
        }

        if (tracker.lockedUntil != null) {
            if (Instant.now().isBefore(tracker.lockedUntil)) {
                return true;
            }
            // Lockout has expired; clear tracker
            attempts.remove(key);
            return false;
        }

        return false;
    }

    /**
     * Gets the remaining minutes of lockout for a blocked identifier.
     */
    public long getRemainingLockoutMinutes(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return 0;
        }
        AttemptTracker tracker = attempts.get(normalize(identifier));
        if (tracker == null || tracker.lockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), tracker.lockedUntil).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        long minutes = (seconds + 59) / 60; // ceiling
        return Math.max(1, minutes);
    }

    /**
     * Records a failed login attempt for the given identifier.
     * If failed attempts reach or exceed maxAttempts, locks out the identifier.
     */
    public void recordFailedAttempt(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String key = normalize(identifier);
        attempts.compute(key, (k, current) -> {
            Instant now = Instant.now();
            if (current == null) {
                return new AttemptTracker(1, now, null);
            }

            // If existing lockout expired, reset counter to 1
            if (current.lockedUntil != null && now.isAfter(current.lockedUntil)) {
                return new AttemptTracker(1, now, null);
            }

            // Sliding window: attempts older than the lockout duration no longer count,
            // so the effective rate-limit window equals the lockout duration.
            if (Duration.between(current.lastAttempt, now).compareTo(lockoutDuration) > 0) {
                return new AttemptTracker(1, now, null);
            }

            int newCount = current.failedCount + 1;
            Instant lockedUntil = null;
            if (newCount >= maxAttempts) {
                lockedUntil = now.plus(lockoutDuration);
                log.warn("Brute-force protection: identifier '{}' exceeded {} failed attempts. Locked until {}.",
                        key, maxAttempts, lockedUntil);
            } else {
                log.debug("Recorded failed login attempt ({}/{}) for identifier '{}'", newCount, maxAttempts, key);
            }

            return new AttemptTracker(newCount, now, lockedUntil);
        });
    }

    /**
     * Clears failed attempt counters upon a successful authentication.
     */
    public void recordSuccessfulAttempt(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        attempts.remove(normalize(identifier));
    }

    /**
     * Explicitly resets rate-limit tracking for an identifier (useful in tests or admin unlocking).
     */
    public void reset(String identifier) {
        if (identifier != null && !identifier.isBlank()) {
            attempts.remove(normalize(identifier));
        }
    }

    /**
     * Periodic garbage collection of expired attempt records (runs every 30 minutes).
     */
    @Scheduled(fixedRate = 1800000)
    public void cleanupExpiredTrackers() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            AttemptTracker tracker = entry.getValue();
            if (tracker.lockedUntil != null) {
                return now.isAfter(tracker.lockedUntil);
            }
            // If inactive for longer than lockoutDuration, discard
            return Duration.between(tracker.lastAttempt, now).compareTo(lockoutDuration) > 0;
        });
    }

    private String normalize(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }

    private static class AttemptTracker {
        final int failedCount;
        final Instant lastAttempt;
        final Instant lockedUntil;

        AttemptTracker(int failedCount, Instant lastAttempt, Instant lockedUntil) {
            this.failedCount = failedCount;
            this.lastAttempt = lastAttempt;
            this.lockedUntil = lockedUntil;
        }
    }
}
