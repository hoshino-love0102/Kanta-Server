package com.kanta.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kanta.auth.common.BadRequestException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OAuthStateServiceTest {

    private static final String SECRET = "test-secret-key-must-be-long-enough-for-hmac-sha-256-algorithm";

    private OAuthStateService oAuthStateService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        oAuthStateService = new OAuthStateService(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void issue로_발급한_state는_같은_provider로_verify를_통과한다() {
        var state = oAuthStateService.issue("google");

        assertThatCode(() -> oAuthStateService.verify("google", state)).doesNotThrowAnyException();
    }

    @Test
    void issue로_발급한_state를_다른_provider로_verify하면_INVALID_STATE_예외가_발생한다() {
        var state = oAuthStateService.issue("google");

        assertThatThrownBy(() -> oAuthStateService.verify("kakao", state))
            .isInstanceOf(BadRequestException.class)
            .satisfies(exception -> {
                var badRequestException = (BadRequestException) exception;
                assertThat(badRequestException.getErrorCode()).isEqualTo("INVALID_STATE");
            });
    }

    @Test
    void 만료된_state는_INVALID_STATE_예외가_발생한다() {
        var now = Instant.now();
        var expiredState = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("provider", "google")
            .issuedAt(Date.from(now.minusSeconds(600)))
            .expiration(Date.from(now.minusSeconds(1)))
            .signWith(key)
            .compact();

        assertThatThrownBy(() -> oAuthStateService.verify("google", expiredState))
            .isInstanceOf(BadRequestException.class)
            .satisfies(exception -> {
                var badRequestException = (BadRequestException) exception;
                assertThat(badRequestException.getErrorCode()).isEqualTo("INVALID_STATE");
            });
    }

    @Test
    void 위조된_서명을_가진_state는_INVALID_STATE_예외가_발생한다() {
        var tamperedKey = Keys.hmacShaKeyFor(
            "different-secret-key-also-long-enough-for-hmac-sha-256".getBytes(StandardCharsets.UTF_8)
        );
        var now = Instant.now();
        var tamperedState = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("provider", "google")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300)))
            .signWith(tamperedKey)
            .compact();

        assertThatThrownBy(() -> oAuthStateService.verify("google", tamperedState))
            .isInstanceOf(BadRequestException.class)
            .satisfies(exception -> {
                var badRequestException = (BadRequestException) exception;
                assertThat(badRequestException.getErrorCode()).isEqualTo("INVALID_STATE");
            });
    }

    @Test
    void 형식이_올바르지_않은_문자열은_INVALID_STATE_예외가_발생한다() {
        assertThatThrownBy(() -> oAuthStateService.verify("google", "not-a-valid-jwt"))
            .isInstanceOf(BadRequestException.class)
            .satisfies(exception -> {
                var badRequestException = (BadRequestException) exception;
                assertThat(badRequestException.getErrorCode()).isEqualTo("INVALID_STATE");
            });
    }
}
