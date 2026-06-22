package com.kanta.websockethub.security;

public record Passport(
    String userId,
    String username,
    String role
) {
}
