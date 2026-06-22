package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.entity.Workspace;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
    UUID id,
    String name,
    String githubOrg,
    Instant createdAt
) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getGithubOrg(),
            workspace.getCreatedAt()
        );
    }
}
