package com.kanta.user.application.user;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String displayName,
    List<OAuthAccountResponse> oauthAccounts
) {
}
