package com.kanta.auth.infrastructure.oauth;

public interface OAuthProviderClient {
    String provider();

    String buildAuthorizeUrl(String state);

    OAuthProviderProfile exchangeCodeForProfile(String code);
}
