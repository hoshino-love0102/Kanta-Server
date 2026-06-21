package com.kanta.user.application.user;

public record OAuthAccountResponse(
    String provider,
    String linkedAt
) {
}
