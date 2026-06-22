package com.kanta.workspace.domain.workspace.repository;

import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import com.kanta.workspace.domain.workspace.enumeration.MemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndEmail(UUID workspaceId, String email);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, String userId);

    long countByWorkspaceIdAndRoleAndStatus(UUID workspaceId, MemberRole role, MemberStatus status);
}
