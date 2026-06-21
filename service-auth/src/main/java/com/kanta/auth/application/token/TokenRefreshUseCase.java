package com.kanta.auth.application.token;

import com.kanta.auth.application.oauth.TokenPairResponse;
import com.kanta.auth.common.DomainException;
import com.kanta.auth.domain.principal.repository.PrincipalRepository;
import com.kanta.auth.domain.token.repository.RefreshTokenRepository;
import com.kanta.auth.infrastructure.jwt.JwtTokenProvider;
import com.kanta.auth.infrastructure.jwt.RefreshTokenGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenRefreshUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PrincipalRepository principalRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenRefreshUseCase(
        RefreshTokenRepository refreshTokenRepository,
        PrincipalRepository principalRepository,
        RefreshTokenGenerator refreshTokenGenerator,
        RefreshTokenIssuer refreshTokenIssuer,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.principalRepository = principalRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public TokenPairResponse refresh(String rawRefreshToken) {
        var tokenHash = refreshTokenGenerator.hash(rawRefreshToken);
        var refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(this::invalidRefreshToken);

        if (!refreshToken.isActive()) {
            throw invalidRefreshToken();
        }

        var principal = principalRepository.findById(refreshToken.getUserId())
            .orElseThrow(this::invalidRefreshToken);

        var newRawRefreshToken = refreshTokenIssuer.reissue(principal.getUserId(), refreshToken);
        var accessToken = jwtTokenProvider.issueAccessToken(
            principal.getUserId().toString(), principal.getEmail(), principal.getRole()
        );

        return new com.kanta.auth.application.oauth.TokenPairResponse(
            accessToken, newRawRefreshToken, jwtTokenProvider.getAccessTokenTtlSeconds()
        );
    }

    private DomainException invalidRefreshToken() {
        return new DomainException(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다.", "INVALID_REFRESH_TOKEN");
    }
}
