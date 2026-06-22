package com.kanta.workspace.presentation.workspace;

import java.util.UUID;

public record InviteMemberResponse(
    UUID memberId,
    String status
) {
}
