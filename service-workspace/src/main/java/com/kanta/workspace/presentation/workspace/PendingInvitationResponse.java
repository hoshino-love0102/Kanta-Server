package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.entity.Workspace;
import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import java.util.UUID;

public record PendingInvitationResponse(
    UUID workspaceId,
    String workspaceName,
    String role
) {
    public static PendingInvitationResponse from(WorkspaceMember member, Workspace workspace) {
        return new PendingInvitationResponse(
            member.getWorkspaceId(),
            workspace.getName(),
            member.getRole().name()
        );
    }
}
