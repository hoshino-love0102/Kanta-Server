package com.kanta.workspace.application.workspace;

import com.kanta.workspace.application.outbox.OutboxEventWriter;
import com.kanta.workspace.common.BadRequestException;
import com.kanta.workspace.common.ForbiddenException;
import com.kanta.workspace.common.NotFoundException;
import com.kanta.workspace.domain.workspace.entity.Workspace;
import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import com.kanta.workspace.domain.workspace.enumeration.MemberStatus;
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
import java.util.Locale;
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

        var passport = PassportHolder.current();
        var owner = WorkspaceMember.owner(workspace.getId(), passport.requireUserId(), null, passport.username());
        workspaceMemberRepository.save(owner);
        publishMemberUpdated(owner);

        return WorkspaceResponse.from(workspace);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(UUID workspaceId) {
        requireActiveMember(workspaceId);
        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
            .filter(WorkspaceMember::isActive)
            .map(MemberResponse::from)
            .toList();
    }

    @Transactional
    public InviteMemberResponse invite(UUID workspaceId, InviteMemberRequest request) {
        requireManager(workspaceId);

        if (request.role() == MemberRole.OWNER) {
            throw new BadRequestException("OWNER 역할은 초대로 부여할 수 없습니다.", "CANNOT_INVITE_AS_OWNER");
        }

        var normalizedEmail = normalizeEmail(request.email());
        if (workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, normalizedEmail).isPresent()) {
            throw new BadRequestException("이미 초대되었거나 가입된 이메일입니다.", "MEMBER_ALREADY_INVITED");
        }

        var member = WorkspaceMember.invited(workspaceId, normalizedEmail, request.role());
        workspaceMemberRepository.save(member);
        return new InviteMemberResponse(member.getId(), member.getStatus().name());
    }

    @Transactional
    public ChangeMemberRoleResponse changeRole(UUID workspaceId, UUID memberId, MemberRole role) {
        var actingMember = requireManager(workspaceId);
        var member = findMember(workspaceId, memberId);

        if (member.getId().equals(actingMember.getId())) {
            throw new ForbiddenException("자기 자신의 역할은 변경할 수 없습니다.", "CANNOT_MODIFY_SELF");
        }
        if (member.getRole() == MemberRole.OWNER && actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("OWNER는 OWNER만 변경할 수 있습니다.", "INSUFFICIENT_ROLE");
        }
        if (role == MemberRole.OWNER) {
            throw new BadRequestException("OWNER 역할은 이양할 수 없습니다.", "CANNOT_GRANT_OWNER");
        }
        if (member.getRole() == MemberRole.OWNER && isLastOwner(workspaceId)) {
            throw new BadRequestException("마지막 OWNER는 강등할 수 없습니다.", "LAST_OWNER_PROTECTED");
        }

        member.changeRole(role);

        if (member.isActive()) {
            publishMemberUpdated(member);
        }

        return new ChangeMemberRoleResponse(member.getId(), member.getRole().name());
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId) {
        var actingMember = requireManager(workspaceId);
        var member = findMember(workspaceId, memberId);

        if (member.getId().equals(actingMember.getId())) {
            throw new ForbiddenException("자기 자신은 제거할 수 없습니다.", "CANNOT_MODIFY_SELF");
        }
        if (member.getRole() == MemberRole.OWNER && actingMember.getRole() != MemberRole.OWNER) {
            throw new ForbiddenException("OWNER는 OWNER만 제거할 수 있습니다.", "INSUFFICIENT_ROLE");
        }
        if (member.getRole() == MemberRole.OWNER && isLastOwner(workspaceId)) {
            throw new BadRequestException("마지막 OWNER는 제거할 수 없습니다.", "LAST_OWNER_PROTECTED");
        }

        var wasActive = member.isActive();
        workspaceMemberRepository.delete(member);

        if (wasActive) {
            publishMemberRemoved(member);
        }
    }

    private boolean isLastOwner(UUID workspaceId) {
        return workspaceMemberRepository.countByWorkspaceIdAndRoleAndStatus(
            workspaceId, MemberRole.OWNER, MemberStatus.ACTIVE
        ) <= 1;
    }

    private WorkspaceMember requireActiveMember(UUID workspaceId) {
        findWorkspace(workspaceId);
        var userId = PassportHolder.current().requireUserId();
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .filter(WorkspaceMember::isActive)
            .orElseThrow(() -> new ForbiddenException("워크스페이스 멤버가 아닙니다.", "NOT_WORKSPACE_MEMBER"));
    }

    private WorkspaceMember requireManager(UUID workspaceId) {
        var member = requireActiveMember(workspaceId);
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new ForbiddenException("권한이 없습니다.", "INSUFFICIENT_ROLE");
        }
        return member;
    }

    private Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("워크스페이스를 찾을 수 없습니다.", "WORKSPACE_NOT_FOUND"));
    }

    private WorkspaceMember findMember(UUID workspaceId, UUID memberId) {
        var member = workspaceMemberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("멤버를 찾을 수 없습니다.", "MEMBER_NOT_FOUND"));
        if (!member.getWorkspaceId().equals(workspaceId)) {
            throw new NotFoundException("멤버를 찾을 수 없습니다.", "MEMBER_NOT_FOUND");
        }
        return member;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
