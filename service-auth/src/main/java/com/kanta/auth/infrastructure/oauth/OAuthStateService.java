package com.kanta.auth.infrastructure.oauth;

import com.kanta.auth.common.BadRequestException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateService {
    private static final long STATE_TTL_SECONDS = 300;

    private final SecretKey key;

    public OAuthStateService(@Value("${kanta.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String provider) {
        var now = Instant.now();
        return Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("provider", provider)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(STATE_TTL_SECONDS)))
            .signWith(key)
            .compact();
    }

    public void verify(String provider, String state) {
        try {
            var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(state)
                .getPayload();

            if (!provider.equals(claims.get("provider", String.class))) {
                throw new BadRequestException("OAuth state가 올바르지 않습니다.", "INVALID_STATE");
            }
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadRequestException("OAuth state가 올바르지 않거나 만료되었습니다.", "INVALID_STATE");
        }
    }
}
