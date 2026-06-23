package com.kanta.auth.presentation.oauth;

import com.kanta.auth.application.oauth.OAuthLoginUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {
    private final OAuthLoginUseCase oAuthLoginUseCase;
    private final String frontendRedirectUri;

    public OAuthController(
        OAuthLoginUseCase oAuthLoginUseCase,
        @Value("${kanta.oauth.frontend-redirect-uri}") String frontendRedirectUri
    ) {
        this.oAuthLoginUseCase = oAuthLoginUseCase;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @GetMapping("/{provider}/redirect")
    public ResponseEntity<Void> redirect(@PathVariable String provider) {
        var authorizeUrl = oAuthLoginUseCase.buildAuthorizeUrl(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, authorizeUrl)
            .build();
    }

    // 토큰을 JSON으로 응답하면 OAuth provider가 보낸 브라우저가 이 API 자체의 응답 화면에
    // 멈춰서 SPA로 돌아가지 못한다. 프론트엔드 redirect URI에 토큰을 쿼리 파라미터로 실어
    // 302로 보내야 로그인 플로우가 끝까지 이어진다.
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
        @PathVariable String provider,
        @RequestParam String code,
        @RequestParam String state
    ) {
        var result = oAuthLoginUseCase.handleCallback(provider, code, state);
        var redirectUri = UriComponentsBuilder.fromUriString(frontendRedirectUri)
            .queryParam("accessToken", result.accessToken())
            .queryParam("refreshToken", result.refreshToken())
            .queryParam("expiresIn", result.expiresIn())
            .build(true)
            .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, redirectUri)
            .build();
    }
}
