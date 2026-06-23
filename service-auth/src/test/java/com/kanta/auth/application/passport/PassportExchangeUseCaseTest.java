package com.kanta.auth.application.passport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.auth.common.DomainException;
import com.kanta.auth.infrastructure.jwt.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PassportExchangeUseCaseTest {

    private JwtTokenProvider jwtTokenProvider;
    private ObjectMapper objectMapper;
    private PassportExchangeUseCase passportExchangeUseCase;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        objectMapper = new ObjectMapper();
        passportExchangeUseCase = new PassportExchangeUseCase(jwtTokenProvider, objectMapper);
    }

    @Test
    void 유효한_accessToken은_userId_username_role을_담은_passport로_인코딩된다() throws Exception {
        var claims = new JwtTokenProvider.AccessTokenClaims(
            "11111111-1111-1111-1111-111111111111", "user@example.com", "MEMBER"
        );
        when(jwtTokenProvider.verify("valid-token")).thenReturn(claims);

        var passport = passportExchangeUseCase.exchange("valid-token");

        var decodedJson = new String(Base64.getDecoder().decode(passport), StandardCharsets.UTF_8);
        Map<String, Object> fields = objectMapper.readValue(decodedJson, Map.class);

        assertThat(fields.keySet()).containsExactlyInAnyOrder("userId", "username", "role");
        assertThat(fields.get("userId")).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(fields.get("username")).isEqualTo("user@example.com");
        assertThat(fields.get("role")).isEqualTo("MEMBER");
    }

    @Test
    void 만료된_토큰은_jwtTokenProvider에서_던진_TOKEN_EXPIRED_예외가_그대로_전파된다() {
        when(jwtTokenProvider.verify("expired-token"))
            .thenThrow(new DomainException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", "TOKEN_EXPIRED"));

        assertThatThrownBy(() -> passportExchangeUseCase.exchange("expired-token"))
            .isInstanceOf(DomainException.class)
            .satisfies(exception -> {
                var domainException = (DomainException) exception;
                assertThat(domainException.getErrorCode()).isEqualTo("TOKEN_EXPIRED");
                assertThat(domainException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }

    @Test
    void 유효하지_않은_토큰은_jwtTokenProvider에서_던진_INVALID_TOKEN_예외가_그대로_전파된다() {
        when(jwtTokenProvider.verify("invalid-token"))
            .thenThrow(new DomainException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "INVALID_TOKEN"));

        assertThatThrownBy(() -> passportExchangeUseCase.exchange("invalid-token"))
            .isInstanceOf(DomainException.class)
            .satisfies(exception -> {
                var domainException = (DomainException) exception;
                assertThat(domainException.getErrorCode()).isEqualTo("INVALID_TOKEN");
            });
    }

    @Test
    void objectMapper가_직렬화에_실패하면_IllegalStateException으로_래핑한다() throws Exception {
        var claims = new JwtTokenProvider.AccessTokenClaims("u1", "user@example.com", "MEMBER");
        when(jwtTokenProvider.verify("valid-token")).thenReturn(claims);

        var failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        var useCaseWithFailingMapper = new PassportExchangeUseCase(jwtTokenProvider, failingObjectMapper);

        assertThatThrownBy(() -> useCaseWithFailingMapper.exchange("valid-token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Passport 직렬화에 실패했습니다.");
    }
}
