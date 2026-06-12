package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.RevokedToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {

    boolean existsByTokenId(String tokenId);

    Optional<RevokedToken> findByTokenId(String tokenId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
