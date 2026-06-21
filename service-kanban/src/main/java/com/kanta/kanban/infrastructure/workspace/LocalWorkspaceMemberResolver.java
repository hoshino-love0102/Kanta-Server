package com.kanta.kanban.infrastructure.workspace;

import com.kanta.kanban.common.ForbiddenException;
import com.kanta.kanban.domain.card.WorkspaceMemberResolver;
import com.kanta.kanban.domain.workspace.repository.WorkspaceMemberCacheRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalWorkspaceMemberResolver implements WorkspaceMemberResolver {
    private final WorkspaceMemberCacheRepository workspaceMemberCacheRepository;

    public LocalWorkspaceMemberResolver(WorkspaceMemberCacheRepository workspaceMemberCacheRepository) {
        this.workspaceMemberCacheRepository = workspaceMemberCacheRepository;
    }

    @Override
    public UUID resolve(UUID workspaceId, String userId) {
        return workspaceMemberCacheRepository.findByWorkspaceIdAndUserIdAndActiveTrue(workspaceId, userId)
            .map(member -> member.getId())
            .orElseThrow(() -> new ForbiddenException("워크스페이스 멤버가 아닙니다.", "WORKSPACE_MEMBER_NOT_FOUND"));
    }
}
