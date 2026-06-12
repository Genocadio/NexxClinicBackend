package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByWorkerIdAndRevokedAtIsNull(UUID workerId);

    List<RefreshToken> findAllByWorkerIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtAsc(UUID workerId, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
