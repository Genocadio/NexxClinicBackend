package com.nexxserve.nexxclinic.auth;

import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final Key key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        logger.info("JwtService initialized with expiration: {} minutes", expirationMinutes);
    }

    public String generateAccessToken(Worker worker) {
        Instant now = Instant.now();
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(worker.getId().toString())
                .id(tokenId)
                .claim("principal", resolvePrincipal(worker))
                .claim("roles", worker.getRoles().stream().map(Enum::name).toList())
                .claim("tokenType", TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
        
        logger.info("Generated access token for user: {} (ID: {}) with roles: {}", 
            resolvePrincipal(worker), worker.getId(), worker.getRoles());
        
        return token;
    }

    public AccessTokenInfo parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(token).getPayload();
            String tokenType = String.valueOf(claims.getOrDefault("tokenType", ""));
            if (!Objects.equals(tokenType, TOKEN_TYPE_ACCESS)) {
                logger.warn("Invalid token type: {}", tokenType);
                throw new IllegalArgumentException("Invalid token type");
            }

            UUID userId = UUID.fromString(claims.getSubject());
            Object rawRoles = claims.get("roles");
            Set<RoleName> roles = Set.of();
            if (rawRoles instanceof List<?> roleList) {
                roles = roleList.stream()
                        .map(String::valueOf)
                        .map(RoleName::valueOf)
                        .collect(Collectors.toSet());
            }

            String principal = String.valueOf(claims.getOrDefault("principal", claims.getSubject()));
            logger.debug("Successfully parsed token for principal: {} with roles: {}", principal, roles);
            
            return new AccessTokenInfo(
                    userId,
                    principal,
                    roles,
                    claims.getId(),
                    claims.getExpiration().toInstant(),
                    tokenType
            );
        } catch (Exception e) {
            logger.error("Failed to parse access token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public AuthenticatedUser toAuthenticatedUser(AccessTokenInfo tokenInfo) {
        logger.debug("Converting token info to authenticated user: {}", tokenInfo.principal());
        return new AuthenticatedUser(
                tokenInfo.userId(),
                tokenInfo.principal(),
                tokenInfo.roles(),
                tokenInfo.tokenId(),
                tokenInfo.expiresAt()
        );
    }

    private String resolvePrincipal(Worker worker) {
        if (worker.getEmail() != null && !worker.getEmail().isBlank()) {
            return worker.getEmail();
        }
        if (worker.getUsername() != null && !worker.getUsername().isBlank()) {
            return worker.getUsername();
        }
        if (worker.getPhoneNumber() != null && !worker.getPhoneNumber().isBlank()) {
            return worker.getPhoneNumber();
        }
        return worker.getId().toString();
    }
}
