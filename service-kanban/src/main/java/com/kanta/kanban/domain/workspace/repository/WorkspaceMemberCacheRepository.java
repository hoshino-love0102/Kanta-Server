package com.kanta.kanban.domain.workspace.repository;

import com.kanta.kanban.domain.workspace.entity.WorkspaceMemberCache;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberCacheRepository extends JpaRepository<WorkspaceMemberCache, UUID> {
    Optional<WorkspaceMemberCache> findByWorkspaceIdAndUserIdAndActiveTrue(UUID workspaceId, String userId);

    List<WorkspaceMemberCache> findByIdInAndActiveTrue(Collection<UUID> ids);
}
