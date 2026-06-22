package com.kanta.workspace.application.workspace;

import com.kanta.workspace.application.outbox.OutboxEventWriter;
import com.kanta.workspace.common.BadRequestException;
import com.kanta.workspace.common.NotFoundException;
import com.kanta.workspace.domain.workspace.entity.Workspace;
import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import com.kanta.workspace.domain.workspace.repository.WorkspaceMemberRepository;
import com.kanta.workspace.domain.workspace.repository.WorkspaceRepository;
import com.kanta.workspace.infrastructure.security.PassportHolder;
import com.kanta.workspace.presentation.workspace.ChangeMemberRoleResponse;
import com.kanta.workspace.presentation.workspace.CreateWorkspaceRequest;
import com.kanta.workspace.presentation.workspace.InviteMemberRequest;
import com.kanta.workspace.presentation.workspace.InviteMemberResponse;
import com.kanta.workspace.presentation.workspace.MemberResponse;
import com.kanta.workspace.presentation.workspace.WorkspaceResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final OutboxEventWriter outboxEventWriter;

    public WorkspaceService(
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        OutboxEventWriter outboxEventWriter
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        var workspace = workspaceRepository.save(new Workspace(request.name().trim(), request.githubOrg()));

        var userId = PassportHolder.current().requireUserId();
        var owner = WorkspaceMember.owner(workspace.getId(), userId, null, PassportHolder.current().username());
        workspaceMemberRepository.save(owner);
        publishMemberUpdated(owner);

        return WorkspaceResponse.from(workspace);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(UUID workspaceId) {
        findWorkspace(workspaceId);
        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
            .filter(WorkspaceMember::isActive)
            .map(MemberResponse::from)
            .toList();
    }

    @Transactional
    public InviteMemberResponse invite(UUID workspaceId, InviteMemberRequest request) {
        findWorkspace(workspaceId);

        if (workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, request.email()).isPresent()) {
            throw new BadRequestException("이미 초대되었거나 가입된 이메일입니다.", "MEMBER_ALREADY_INVITED");
        }

        var member = WorkspaceMember.invited(workspaceId, request.email(), request.role());
        workspaceMemberRepository.save(member);
        return new InviteMemberResponse(member.getId(), member.getStatus().name());
    }

    @Transactional
    public ChangeMemberRoleResponse changeRole(UUID workspaceId, UUID memberId, MemberRole role) {
        var member = findMember(workspaceId, memberId);
        member.changeRole(role);

        if (member.isActive()) {
            publishMemberUpdated(member);
        }

        return new ChangeMemberRoleResponse(member.getId(), member.getRole().name());
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId) {
        var member = findMember(workspaceId, memberId);
        var wasActive = member.isActive();
        workspaceMemberRepository.delete(member);

        if (wasActive) {
            publishMemberRemoved(member);
        }
    }

    private Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("워크스페이스를 찾을 수 없습니다.", "WORKSPACE_NOT_FOUND"));
    }

    private WorkspaceMember findMember(UUID workspaceId, UUID memberId) {
        findWorkspace(workspaceId);
        var member = workspaceMemberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("멤버를 찾을 수 없습니다.", "MEMBER_NOT_FOUND"));
        if (!member.getWorkspaceId().equals(workspaceId)) {
            throw new NotFoundException("멤버를 찾을 수 없습니다.", "MEMBER_NOT_FOUND");
        }
        return member;
    }

    private void publishMemberUpdated(WorkspaceMember member) {
        outboxEventWriter.append(
            "WORKSPACE_MEMBER",
            member.getId(),
            "member.updated",
            Map.of(
                "workspaceMemberId", member.getId(),
                "workspaceId", member.getWorkspaceId(),
                "userId", member.getUserId(),
                "displayName", member.getDisplayName() == null ? "" : member.getDisplayName(),
                "role", member.getRole().name(),
                "active", true
            )
        );
    }

    private void publishMemberRemoved(WorkspaceMember member) {
        outboxEventWriter.append(
            "WORKSPACE_MEMBER",
            member.getId(),
            "member.updated",
            Map.of(
                "workspaceMemberId", member.getId(),
                "workspaceId", member.getWorkspaceId(),
                "userId", member.getUserId(),
                "displayName", member.getDisplayName() == null ? "" : member.getDisplayName(),
                "role", member.getRole().name(),
                "active", false
            )
        );
    }
}
