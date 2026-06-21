package com.kanta.auth.application.passport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.auth.infrastructure.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class PassportExchangeUseCase {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public PassportExchangeUseCase(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    public String exchange(String accessToken) {
        var claims = jwtTokenProvider.verify(accessToken);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("userId", claims.userId());
        payload.put("username", claims.username());
        payload.put("role", claims.role());

        try {
            var json = objectMapper.writeValueAsString(payload);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Passport 직렬화에 실패했습니다.", exception);
        }
    }
}
