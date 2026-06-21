package com.kanta.auth.infrastructure.oauth;

public record OAuthProviderProfile(
    String providerUserId,
    String email,
    String displayName
) {
}
