package com.kanta.user.application.user;

import java.util.UUID;

public record UpsertUserResult(
    UUID userId,
    String email,
    String displayName,
    String role
) {
}
