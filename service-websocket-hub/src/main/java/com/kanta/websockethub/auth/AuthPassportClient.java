package com.kanta.websockethub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.websockethub.security.Passport;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthPassportClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalGatewaySecret;

    public AuthPassportClient(
        ObjectMapper objectMapper,
        @Value("${kanta.client.auth-service-url}") String authServiceUrl,
        @Value("${kanta.internal.gateway-secret}") String internalGatewaySecret
    ) {
        this.restClient = RestClient.create(authServiceUrl);
        this.objectMapper = objectMapper;
        this.internalGatewaySecret = internalGatewaySecret;
    }

    public ExchangedPassport exchange(String bearerAccessToken) {
        var response = restClient.post()
            .uri("/passport")
            .header("Authorization", "Bearer " + bearerAccessToken)
            .header("X-Internal-Gateway-Secret", internalGatewaySecret)
            .retrieve()
            .body(Map.class);

        @SuppressWarnings("unchecked")
        var data = (Map<String, Object>) response.get("data");
        var passportValue = (String) data.get("passport");
        return new ExchangedPassport(passportValue, parsePassport(passportValue));
    }

    private Passport parsePassport(String header) {
        var json = header;
        try {
            json = new String(Base64.getDecoder().decode(header), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // 구현에 따라 base64 JSON 또는 raw JSON일 수 있음
        }
        try {
            return objectMapper.readValue(json, Passport.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Passport 형식이 올바르지 않습니다.", exception);
        }
    }

    public record ExchangedPassport(String rawValue, Passport passport) {
    }
}
