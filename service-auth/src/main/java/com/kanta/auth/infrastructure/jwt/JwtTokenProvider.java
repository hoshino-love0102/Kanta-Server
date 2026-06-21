package com.kanta.auth.infrastructure.jwt;

import com.kanta.auth.common.DomainException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenTtlSeconds;

    public JwtTokenProvider(
        @Value("${kanta.jwt.secret}") String secret,
        @Value("${kanta.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public String issueAccessToken(String userId, String username, String role) {
        var now = Instant.now();
        return Jwts.builder()
            .subject(userId)
            .claim("username", username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
            .signWith(key)
            .compact();
    }

    public AccessTokenClaims verify(String accessToken) {
        try {
            var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

            return new AccessTokenClaims(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("role", String.class)
            );
        } catch (ExpiredJwtException exception) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", "TOKEN_EXPIRED");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "INVALID_TOKEN");
        }
    }

    public record AccessTokenClaims(String userId, String username, String role) {
    }
}
