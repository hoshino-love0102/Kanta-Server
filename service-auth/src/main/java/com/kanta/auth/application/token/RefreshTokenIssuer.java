package com.kanta.auth.application.token;

import com.kanta.auth.common.DomainException;
import com.kanta.auth.domain.token.entity.RefreshToken;
import com.kanta.auth.domain.token.repository.RefreshTokenRepository;
import com.kanta.auth.infrastructure.jwt.RefreshTokenGenerator;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenIssuer {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final long ttlDays;

    public RefreshTokenIssuer(
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenGenerator refreshTokenGenerator,
        @Value("${kanta.refresh-token.ttl-days}") long ttlDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.ttlDays = ttlDays;
    }

    public String issue(UUID userId) {
        var rawToken = refreshTokenGenerator.generate();
        var tokenHash = refreshTokenGenerator.hash(rawToken);
        var expiresAt = Instant.now().plusSeconds(ttlDays * 24 * 60 * 60);

        refreshTokenRepository.save(new RefreshToken(userId, tokenHash, expiresAt));
        return rawToken;
    }

    public String reissue(UUID userId, RefreshToken previous) {
        var rawToken = refreshTokenGenerator.generate();
        var tokenHash = refreshTokenGenerator.hash(rawToken);
        var expiresAt = Instant.now().plusSeconds(ttlDays * 24 * 60 * 60);

        var next = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(next);

        var revokedRows = refreshTokenRepository.revokeIfActive(previous.getId(), Instant.now(), next.getId());
        if (revokedRows == 0) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다.", "INVALID_REFRESH_TOKEN");
        }

        return rawToken;
    }
}
