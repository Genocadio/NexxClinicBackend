package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AccessTokenInfo;
import com.nexxserve.nexxclinic.auth.JwtService;
import com.nexxserve.nexxclinic.auth.TokenBundle;
import com.nexxserve.nexxclinic.entity.RefreshToken;
import com.nexxserve.nexxclinic.entity.RevokedToken;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.repository.RefreshTokenRepository;
import com.nexxserve.nexxclinic.repository.RevokedTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final long refreshExpirationDays;

    public SessionTokenService(
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            RevokedTokenRepository revokedTokenRepository,
            @Value("${app.jwt.refresh-expiration-days:14}") long refreshExpirationDays
    ) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Transactional
    public TokenBundle issueSession(Worker worker) {
        enforceSessionLimitBeforeIssue(worker, null);

        String accessToken = jwtService.generateAccessToken(worker);
        String refreshTokenRaw = generateRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setWorker(worker);
        refreshToken.setTokenHash(hashToken(refreshTokenRaw));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshTokenRepository.save(refreshToken);

        purgeExpiredTokens();
        return new TokenBundle(accessToken, refreshTokenRaw);
    }

    @Transactional
    public Optional<TokenBundle> rotateRefreshToken(String refreshTokenRaw) {
        Optional<RefreshToken> existingOptional = refreshTokenRepository.findByTokenHash(hashToken(refreshTokenRaw))
                .filter(this::isRefreshTokenUsable);
        if (existingOptional.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken existing = existingOptional.get();
        Worker worker = existing.getWorker();

        enforceSessionLimitBeforeIssue(worker, existing.getId());

        String newAccessToken = jwtService.generateAccessToken(worker);
        String newRefreshRaw = generateRefreshTokenValue();

        RefreshToken replacement = new RefreshToken();
        replacement.setWorker(worker);
        replacement.setTokenHash(hashToken(newRefreshRaw));
        replacement.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        refreshTokenRepository.save(replacement);

        existing.setRevokedAt(LocalDateTime.now());
        existing.setRevokeReason("ROTATED");
        existing.setReplacedByTokenId(replacement.getId());
        refreshTokenRepository.save(existing);

        purgeExpiredTokens();
        return Optional.of(new TokenBundle(newAccessToken, newRefreshRaw));
    }

    @Transactional
    public boolean revokeRefreshToken(String refreshTokenRaw, String reason) {
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByTokenHash(hashToken(refreshTokenRaw));
        if (tokenOptional.isEmpty()) {
            return false;
        }

        RefreshToken token = tokenOptional.get();
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(LocalDateTime.now());
            token.setRevokeReason(reason);
            refreshTokenRepository.save(token);
        }

        return true;
    }

    @Transactional
    public void revokeAllRefreshTokensForUser(UUID userId, String reason) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByWorkerIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.setRevokeReason(reason);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    @Transactional(readOnly = true)
    public int countActiveRefreshSessionsForUser(UUID userId) {
        return refreshTokenRepository
                .findAllByWorkerIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(userId, LocalDateTime.now())
                .size();
    }

    @Transactional(readOnly = true)
    public int resolveEffectiveMaxSessions(Worker worker) {
        Integer configured = worker.getMaxActiveSessions();
        if (configured == null || configured < 1) {
            return Worker.DEFAULT_MAX_ACTIVE_SESSIONS;
        }
        return configured;
    }

    @Transactional
    public void revokeOldestRefreshTokensForUserToLimit(UUID userId, int maxSessions, String reason) {
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByWorkerIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(userId, LocalDateTime.now());
        int sessionsToRevoke = activeTokens.size() - maxSessions;
        if (sessionsToRevoke <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < sessionsToRevoke; i++) {
            RefreshToken token = activeTokens.get(i);
            token.setRevokedAt(now);
            token.setRevokeReason(reason);
        }
        refreshTokenRepository.saveAll(activeTokens.subList(0, sessionsToRevoke));
    }

    @Transactional
    public void revokeAccessTokenByJti(String tokenId, LocalDateTime expiresAt, UUID revokedByUserId, String reason) {
        if (tokenId == null || tokenId.isBlank() || revokedTokenRepository.existsByTokenId(tokenId)) {
            return;
        }

        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setTokenId(tokenId);
        revokedToken.setExpiresAt(expiresAt);
        revokedToken.setRevokedByUserId(revokedByUserId);
        revokedToken.setReason(reason);
        revokedTokenRepository.save(revokedToken);
        purgeExpiredTokens();
    }

    @Transactional(readOnly = true)
    public boolean isAccessTokenRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByTokenId(tokenId);
    }

    public LocalDateTime toLocalDateTime(AccessTokenInfo tokenInfo) {
        return LocalDateTime.ofInstant(tokenInfo.expiresAt(), ZoneOffset.UTC);
    }

    private boolean isRefreshTokenUsable(RefreshToken token) {
        return token.getRevokedAt() == null && token.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        revokedTokenRepository.deleteByExpiresAtBefore(now);
    }

    private void enforceSessionLimitBeforeIssue(Worker worker, UUID tokenIdToExcludeFromCount) {
        int maxSessions = resolveEffectiveMaxSessions(worker);
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAllByWorkerIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(worker.getId(), LocalDateTime.now());

        if (tokenIdToExcludeFromCount != null) {
            activeTokens.removeIf(token -> tokenIdToExcludeFromCount.equals(token.getId()));
        }

        int sessionsToRevoke = activeTokens.size() - maxSessions + 1;
        if (sessionsToRevoke <= 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < sessionsToRevoke; i++) {
            RefreshToken token = activeTokens.get(i);
            token.setRevokedAt(now);
            token.setRevokeReason("MAX_SESSIONS_EXCEEDED");
        }
        refreshTokenRepository.saveAll(activeTokens.subList(0, sessionsToRevoke));
    }
}
