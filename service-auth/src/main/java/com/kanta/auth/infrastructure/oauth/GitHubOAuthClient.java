package com.kanta.auth.infrastructure.oauth;

import com.kanta.auth.common.BadRequestException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GitHubOAuthClient implements OAuthProviderClient {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient = RestClient.create();

    public GitHubOAuthClient(
        @Value("${kanta.oauth.github.client-id}") String clientId,
        @Value("${kanta.oauth.github.client-secret}") String clientSecret,
        @Value("${kanta.oauth.github.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String provider() {
        return "GITHUB";
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return "https://github.com/login/oauth/authorize"
            + "?client_id=" + encode(clientId)
            + "&redirect_uri=" + encode(redirectUri)
            + "&scope=" + encode("read:user user:email")
            + "&state=" + encode(state);
    }

    @Override
    public OAuthProviderProfile exchangeCodeForProfile(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        var tokenResponse = restClient.post()
            .uri("https://github.com/login/oauth/access_token")
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        var accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new BadRequestException("GitHub OAuth 토큰 교환에 실패했습니다.", "OAUTH_TOKEN_EXCHANGE_FAILED");
        }

        var userResponse = restClient.get()
            .uri("https://api.github.com/user")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(Map.class);

        var providerUserId = String.valueOf(userResponse.get("id"));
        var email = fetchVerifiedPrimaryEmail(accessToken);
        var displayName = (String) userResponse.getOrDefault("name", userResponse.get("login"));

        return new OAuthProviderProfile(providerUserId, email, displayName);
    }

    @SuppressWarnings("unchecked")
    private String fetchVerifiedPrimaryEmail(String accessToken) {
        List<Map<String, Object>> emails = restClient.get()
            .uri("https://api.github.com/user/emails")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(List.class);

        return emails.stream()
            .filter(entry -> Boolean.TRUE.equals(entry.get("primary")) && Boolean.TRUE.equals(entry.get("verified")))
            .map(entry -> (String) entry.get("email"))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("GitHub 계정에서 검증된 이메일을 확인할 수 없습니다.", "OAUTH_EMAIL_NOT_VERIFIED"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
