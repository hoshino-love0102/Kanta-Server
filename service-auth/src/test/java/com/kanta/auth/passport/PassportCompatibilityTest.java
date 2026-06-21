package com.kanta.auth.passport;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.auth.application.passport.PassportExchangeUseCase;
import com.kanta.auth.infrastructure.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * service-kanban의 PassportFilter#parsePassport(헤더 -> Base64 디코드 -> JSON -> Passport(userId, username, role))와
 * 동일한 절차로 디코딩해, service-auth가 만든 passport 문자열이 그 형식과 정확히 맞는지 검증한다.
 */
@SpringBootTest
class PassportCompatibilityTest {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PassportExchangeUseCase passportExchangeUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void passportMatchesKanbanRecordShape() throws Exception {
        var accessToken = jwtTokenProvider.issueAccessToken(
            "11111111-1111-1111-1111-111111111111", "user@example.com", "MEMBER"
        );

        var passport = passportExchangeUseCase.exchange(accessToken);

        var decodedJson = new String(Base64.getDecoder().decode(passport), StandardCharsets.UTF_8);
        Map<String, Object> fields = objectMapper.readValue(decodedJson, Map.class);

        assertThat(fields.keySet()).containsExactlyInAnyOrder("userId", "username", "role");
        assertThat(fields.get("userId")).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(fields.get("username")).isEqualTo("user@example.com");
        assertThat(fields.get("role")).isEqualTo("MEMBER");
    }
}
