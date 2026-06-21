package com.kanta.auth.presentation.token;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {
}
