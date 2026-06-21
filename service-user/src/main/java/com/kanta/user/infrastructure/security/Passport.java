package com.kanta.user.infrastructure.security;

public record Passport(
    String userId,
    String username,
    String role
) {
    public String requireUserId() {
        return userId;
    }
}
