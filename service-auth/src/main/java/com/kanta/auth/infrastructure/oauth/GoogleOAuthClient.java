package com.kanta.auth.infrastructure.oauth;

import com.kanta.auth.common.BadRequestException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthClient implements OAuthProviderClient {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient = RestClient.create();

    public GoogleOAuthClient(
        @Value("${kanta.oauth.google.client-id}") String clientId,
        @Value("${kanta.oauth.google.client-secret}") String clientSecret,
        @Value("${kanta.oauth.google.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String provider() {
        return "GOOGLE";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + encode(clientId)
            + "&redirect_uri=" + encode(redirectUri)
            + "&response_type=code"
            + "&scope=" + encode("openid email profile")
            + "&state=" + encode(state);
    }

    @Override
    public OAuthProviderProfile exchangeCodeForProfile(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        var tokenResponse = restClient.post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        var accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new BadRequestException("Google OAuth 토큰 교환에 실패했습니다.", "OAUTH_TOKEN_EXCHANGE_FAILED");
        }

        var userInfo = restClient.get()
            .uri("https://www.googleapis.com/oauth2/v3/userinfo")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(Map.class);

        var emailVerified = userInfo.get("email_verified");
        var isVerified = Boolean.TRUE.equals(emailVerified) || "true".equals(String.valueOf(emailVerified));
        if (!isVerified) {
            throw new BadRequestException("Google 계정에서 검증된 이메일을 확인할 수 없습니다.", "OAUTH_EMAIL_NOT_VERIFIED");
        }

        var providerUserId = (String) userInfo.get("sub");
        var email = (String) userInfo.get("email");
        var displayName = (String) userInfo.getOrDefault("name", email);

        return new OAuthProviderProfile(providerUserId, email, displayName);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
