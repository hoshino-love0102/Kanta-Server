package com.kanta.workspace.domain.workspace.repository;

import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndEmail(UUID workspaceId, String email);
}
