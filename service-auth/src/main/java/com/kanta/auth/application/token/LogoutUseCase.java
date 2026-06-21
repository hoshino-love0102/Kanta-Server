package com.kanta.auth.application.token;

import com.kanta.auth.domain.token.repository.RefreshTokenRepository;
import com.kanta.auth.infrastructure.jwt.RefreshTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, RefreshTokenGenerator refreshTokenGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        var tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
            .filter(token -> token.getRevokedAt() == null)
            .ifPresent(token -> token.revoke(null));
    }
}
