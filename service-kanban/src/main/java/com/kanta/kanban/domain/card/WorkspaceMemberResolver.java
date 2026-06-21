package com.kanta.kanban.domain.card;

import java.util.UUID;

public interface WorkspaceMemberResolver {
    UUID resolve(UUID workspaceId, String userId);
}
