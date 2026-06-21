package com.kanta.auth.application.oauth;

public record TokenPairResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {
}
