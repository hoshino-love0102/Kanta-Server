package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
    @NotBlank @Email String email,
    @NotNull MemberRole role
) {
}
