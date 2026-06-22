package com.kanta.workspace.presentation.workspace;

import java.util.UUID;

public record ChangeMemberRoleResponse(
    UUID memberId,
    String role
) {
}
