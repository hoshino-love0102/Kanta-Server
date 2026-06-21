package com.kanta.auth.presentation.token;

import com.kanta.auth.application.token.LogoutUseCase;
import com.kanta.auth.application.token.TokenRefreshUseCase;
import com.kanta.auth.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class TokenController {
    private final TokenRefreshUseCase tokenRefreshUseCase;
    private final LogoutUseCase logoutUseCase;

    public TokenController(TokenRefreshUseCase tokenRefreshUseCase, LogoutUseCase logoutUseCase) {
        this.tokenRefreshUseCase = tokenRefreshUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        var result = tokenRefreshUseCase.refresh(request.refreshToken());
        return ApiResponse.ok(new TokenResponse(result.accessToken(), result.refreshToken(), result.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        logoutUseCase.logout(request.refreshToken());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
