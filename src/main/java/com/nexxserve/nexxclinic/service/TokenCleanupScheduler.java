package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.repository.RefreshTokenRepository;
import com.nexxserve.nexxclinic.repository.RevokedTokenRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically cleans up expired refresh tokens and revoked access token records
 * from the database to prevent unbounded table growth.
 */
@Service
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    public TokenCleanupScheduler(
            RefreshTokenRepository refreshTokenRepository,
            RevokedTokenRepository revokedTokenRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Daily token maintenance job (default: 2:00 AM server time).
     */
    @Scheduled(cron = "${app.security.token-cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void runScheduledCleanup() {
        log.info("Running scheduled token cleanup job...");
        try {
            purgeExpiredTokens();
        } catch (Exception e) {
            log.error("Token cleanup job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Purges tokens where expiresAt is before the current timestamp.
     * Can be invoked manually or in tests.
     *
     * @return the total number of expired tokens deleted
     */
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        revokedTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Token maintenance completed for records expired before {}", now);
    }
}
