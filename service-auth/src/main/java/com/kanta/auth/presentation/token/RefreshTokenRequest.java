package com.kanta.auth.presentation.token;

public record RefreshTokenRequest(
    String refreshToken
) {
}
