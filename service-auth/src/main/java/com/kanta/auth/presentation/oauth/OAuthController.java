package com.kanta.auth.presentation.oauth;

import com.kanta.auth.application.oauth.OAuthLoginUseCase;
import com.kanta.auth.common.ApiResponse;
import com.kanta.auth.presentation.token.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/oauth")
public class OAuthController {
    private final OAuthLoginUseCase oAuthLoginUseCase;

    public OAuthController(OAuthLoginUseCase oAuthLoginUseCase) {
        this.oAuthLoginUseCase = oAuthLoginUseCase;
    }

    @GetMapping("/{provider}/redirect")
    public ResponseEntity<Void> redirect(@PathVariable String provider) {
        var authorizeUrl = oAuthLoginUseCase.buildAuthorizeUrl(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, authorizeUrl)
            .build();
    }

    @GetMapping("/{provider}/callback")
    public ApiResponse<TokenResponse> callback(
        @PathVariable String provider,
        @RequestParam String code,
        @RequestParam String state
    ) {
        var result = oAuthLoginUseCase.handleCallback(provider, code, state);
        return ApiResponse.ok(new TokenResponse(result.accessToken(), result.refreshToken(), result.expiresIn()));
    }
}
