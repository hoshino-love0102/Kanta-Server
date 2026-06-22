package com.kanta.meeting.infrastructure.security;

public record Passport(
    String userId,
    String username,
    String role
) {
    public String requireUserId() {
        return userId;
    }
}
