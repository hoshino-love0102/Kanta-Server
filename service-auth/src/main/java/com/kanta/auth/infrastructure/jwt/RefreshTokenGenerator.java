package com.kanta.auth.infrastructure.jwt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        var bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashed = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
