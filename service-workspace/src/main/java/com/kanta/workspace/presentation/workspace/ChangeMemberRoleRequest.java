package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleRequest(
    @NotNull MemberRole role
) {
}
