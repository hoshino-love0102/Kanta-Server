package com.kanta.auth.domain.token.repository;

import com.kanta.auth.domain.token.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query(
        "update RefreshToken t set t.revokedAt = :revokedAt, t.replacedById = :replacedById "
            + "where t.id = :id and t.revokedAt is null"
    )
    int revokeIfActive(
        @Param("id") UUID id,
        @Param("revokedAt") Instant revokedAt,
        @Param("replacedById") UUID replacedById
    );
}
