package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.repository.RefreshTokenRepository;
import com.nexxserve.nexxclinic.repository.RevokedTokenRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenCleanupScheduler scheduler;

    @Test
    @DisplayName("purgeExpiredTokens invokes deleteByExpiresAtBefore on both repositories with current cutoff")
    void purgeExpiredTokens_invokesRepositories() {
        scheduler.purgeExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        verify(revokedTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("runScheduledCleanup runs purgeExpiredTokens cleanly without throwing")
    void runScheduledCleanup_executesWithoutException() {
        scheduler.runScheduledCleanup();

        verify(refreshTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        verify(revokedTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
