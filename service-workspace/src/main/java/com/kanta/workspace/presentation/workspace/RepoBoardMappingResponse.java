package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.domain.workspace.entity.RepoBoardMapping;
import java.util.UUID;

public record RepoBoardMappingResponse(UUID id, UUID workspaceId, String githubRepo, UUID boardId) {
    public static RepoBoardMappingResponse from(RepoBoardMapping mapping) {
        return new RepoBoardMappingResponse(
            mapping.getId(),
            mapping.getWorkspaceId(),
            mapping.getGithubRepo(),
            mapping.getBoardId()
        );
    }
}
