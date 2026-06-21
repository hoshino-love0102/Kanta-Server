package com.kanta.auth.application.oauth;

import com.kanta.auth.application.principal.PrincipalCacheService;
import com.kanta.auth.application.token.RefreshTokenIssuer;
import com.kanta.auth.common.DomainException;
import com.kanta.auth.domain.oauth.entity.OAuthAccount;
import com.kanta.auth.domain.oauth.repository.OAuthAccountRepository;
import com.kanta.auth.domain.principal.entity.Principal;
import com.kanta.auth.domain.principal.repository.PrincipalRepository;
import com.kanta.auth.infrastructure.grpc.UserGrpcClient;
import com.kanta.auth.infrastructure.jwt.JwtTokenProvider;
import com.kanta.auth.infrastructure.oauth.OAuthProviderClientRegistry;
import com.kanta.auth.infrastructure.oauth.OAuthStateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthLoginUseCase {
    private final OAuthProviderClientRegistry providerClientRegistry;
    private final OAuthStateService oAuthStateService;
    private final UserGrpcClient userGrpcClient;
    private final PrincipalCacheService principalCacheService;
    private final PrincipalRepository principalRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public OAuthLoginUseCase(
        OAuthProviderClientRegistry providerClientRegistry,
        OAuthStateService oAuthStateService,
        UserGrpcClient userGrpcClient,
        PrincipalCacheService principalCacheService,
        PrincipalRepository principalRepository,
        OAuthAccountRepository oAuthAccountRepository,
        JwtTokenProvider jwtTokenProvider,
        RefreshTokenIssuer refreshTokenIssuer
    ) {
        this.providerClientRegistry = providerClientRegistry;
        this.oAuthStateService = oAuthStateService;
        this.userGrpcClient = userGrpcClient;
        this.principalCacheService = principalCacheService;
        this.principalRepository = principalRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenIssuer = refreshTokenIssuer;
    }

    public String buildAuthorizeUrl(String provider) {
        var client = providerClientRegistry.resolve(provider);
        var state = oAuthStateService.issue(client.provider());
        return client.buildAuthorizeUrl(state);
    }

    @Transactional
    public TokenPairResponse handleCallback(String provider, String code, String state) {
        var client = providerClientRegistry.resolve(provider);
        oAuthStateService.verify(client.provider(), state);

        var profile = client.exchangeCodeForProfile(code);

        var existingAccount = oAuthAccountRepository.findByProviderAndProviderUserId(client.provider(), profile.providerUserId());

        Principal principal;
        if (existingAccount.isPresent()) {
            var userId = existingAccount.get().getUserId();
            principal = principalRepository.findById(userId)
                .orElseThrow(() -> new DomainException(
                    HttpStatus.UNAUTHORIZED, "사용자 정보를 확인할 수 없습니다.", "PRINCIPAL_NOT_FOUND"
                ));
        } else {
            var upserted = userGrpcClient.upsertUser(profile.email(), profile.displayName());
            principal = principalCacheService.upsert(
                upserted.userId(), upserted.email(), upserted.displayName(), upserted.role()
            );
            oAuthAccountRepository.save(new OAuthAccount(upserted.userId(), client.provider(), profile.providerUserId()));
        }

        var accessToken = jwtTokenProvider.issueAccessToken(
            principal.getUserId().toString(), principal.getEmail(), principal.getRole()
        );
        var refreshToken = refreshTokenIssuer.issue(principal.getUserId());

        return new TokenPairResponse(accessToken, refreshToken, jwtTokenProvider.getAccessTokenTtlSeconds());
    }
}
