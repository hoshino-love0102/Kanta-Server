package com.kanta.kanban.application.workspace;

import com.kanta.kanban.domain.workspace.entity.WorkspaceMemberCache;
import com.kanta.kanban.domain.workspace.repository.WorkspaceMemberCacheRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceMemberCacheService {
    private final WorkspaceMemberCacheRepository workspaceMemberCacheRepository;

    public WorkspaceMemberCacheService(WorkspaceMemberCacheRepository workspaceMemberCacheRepository) {
        this.workspaceMemberCacheRepository = workspaceMemberCacheRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> findDisplayNames(Collection<UUID> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        return workspaceMemberCacheRepository.findByIdInAndActiveTrue(memberIds).stream()
            .collect(Collectors.toMap(WorkspaceMemberCache::getId, WorkspaceMemberCache::getDisplayName));
    }

    @Transactional
    public void upsert(UUID memberId, UUID workspaceId, String userId, String displayName, String role, boolean active) {
        var member = workspaceMemberCacheRepository.findById(memberId)
            .orElseGet(() -> new WorkspaceMemberCache(memberId, workspaceId, userId, displayName, role, active));
        member.update(workspaceId, userId, displayName, role, active);
        workspaceMemberCacheRepository.save(member);
    }

    @Transactional
    public void remove(UUID memberId) {
        workspaceMemberCacheRepository.findById(memberId)
            .ifPresent(member -> {
                member.update(member.getWorkspaceId(), member.getUserId(), member.getDisplayName(), member.getRole(), false);
                workspaceMemberCacheRepository.save(member);
            });
    }
}
