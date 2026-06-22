package com.kanta.workspace.application.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.workspace.application.outbox.OutboxEventWriter;
import com.kanta.workspace.common.BadRequestException;
import com.kanta.workspace.common.ForbiddenException;
import com.kanta.workspace.common.NotFoundException;
import com.kanta.workspace.domain.workspace.entity.Workspace;
import com.kanta.workspace.domain.workspace.entity.WorkspaceMember;
import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import com.kanta.workspace.domain.workspace.enumeration.MemberStatus;
import com.kanta.workspace.domain.workspace.repository.RepoBoardMappingRepository;
import com.kanta.workspace.domain.workspace.repository.WorkspaceMemberRepository;
import com.kanta.workspace.domain.workspace.repository.WorkspaceRepository;
import com.kanta.workspace.infrastructure.security.Passport;
import com.kanta.workspace.infrastructure.security.PassportHolder;
import com.kanta.workspace.presentation.workspace.CreateWorkspaceRequest;
import com.kanta.workspace.presentation.workspace.InviteMemberRequest;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private RepoBoardMappingRepository repoBoardMappingRepository;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    private WorkspaceService workspaceService;

    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(
            workspaceRepository, workspaceMemberRepository, repoBoardMappingRepository, outboxEventWriter
        );
        lenient().when(workspaceRepository.findById(workspaceId))
            .thenReturn(Optional.of(new Workspace("워크스페이스", null)));
    }

    @AfterEach
    void tearDown() {
        PassportHolder.clear();
    }

    private void actingAs(String userId) {
        PassportHolder.set(new Passport(userId, "user-" + userId, "USER"));
    }

    private WorkspaceMember member(MemberRole role, MemberStatus status) {
        WorkspaceMember member = status == MemberStatus.ACTIVE
            ? WorkspaceMember.owner(workspaceId, UUID.randomUUID().toString(), null, "name")
            : WorkspaceMember.invited(workspaceId, "someone@example.com", role);
        member.changeRole(role);
        setId(member, UUID.randomUUID());
        if (status == MemberStatus.ACTIVE && role != MemberRole.OWNER) {
            setStatus(member, MemberStatus.ACTIVE);
        }
        return member;
    }

    private void setId(WorkspaceMember member, UUID id) {
        setField(member, "id", id);
    }

    private void setStatus(WorkspaceMember member, MemberStatus status) {
        setField(member, "status", status);
    }

    private void setUserId(WorkspaceMember member, String userId) {
        setField(member, "userId", userId);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void create_워크스페이스를_생성하면_생성자가_OWNER로_등록된다() {
        actingAs("user-1");
        when(workspaceRepository.save(any())).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            setField(workspace, "id", UUID.randomUUID());
            return workspace;
        });
        when(workspaceMemberRepository.save(any())).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            setId(member, UUID.randomUUID());
            return member;
        });

        var response = workspaceService.create(new CreateWorkspaceRequest("팀 워크스페이스", null));

        assertThat(response.name()).isEqualTo("팀 워크스페이스");
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
        verify(outboxEventWriter).append(any(), any(), any(), any());
    }

    @Test
    void invite_OWNER로_초대하면_거부된다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        actingAs(actingOwner.getUserId());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actingOwner.getUserId()))
            .thenReturn(Optional.of(actingOwner));

        assertThatThrownBy(() ->
            workspaceService.invite(workspaceId, new InviteMemberRequest("new@example.com", MemberRole.OWNER))
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void invite_이미_초대된_이메일이면_거부된다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        actingAs(actingOwner.getUserId());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actingOwner.getUserId()))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, "dup@example.com"))
            .thenReturn(Optional.of(member(MemberRole.MEMBER, MemberStatus.INVITED)));

        assertThatThrownBy(() ->
            workspaceService.invite(workspaceId, new InviteMemberRequest("DUP@example.com", MemberRole.MEMBER))
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void invite_일반_멤버는_초대할_수_없다() {
        var actingMember = member(MemberRole.MEMBER, MemberStatus.ACTIVE);
        setUserId(actingMember, "member-1");
        actingAs("member-1");
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "member-1"))
            .thenReturn(Optional.of(actingMember));

        assertThatThrownBy(() ->
            workspaceService.invite(workspaceId, new InviteMemberRequest("new@example.com", MemberRole.MEMBER))
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void invite_워크스페이스_멤버가_아니면_거부된다() {
        actingAs("stranger");
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "stranger"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            workspaceService.invite(workspaceId, new InviteMemberRequest("new@example.com", MemberRole.MEMBER))
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void invite_존재하지_않는_워크스페이스면_NotFound() {
        var unknownWorkspaceId = UUID.randomUUID();
        actingAs("user-1");
        when(workspaceRepository.findById(unknownWorkspaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            workspaceService.invite(unknownWorkspaceId, new InviteMemberRequest("new@example.com", MemberRole.MEMBER))
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void changeRole_자기_자신의_역할은_변경할_수_없다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        actingAs(actingOwner.getUserId());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actingOwner.getUserId()))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(actingOwner.getId())).thenReturn(Optional.of(actingOwner));

        assertThatThrownBy(() ->
            workspaceService.changeRole(workspaceId, actingOwner.getId(), MemberRole.ADMIN)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void changeRole_ADMIN은_OWNER의_역할을_변경할_수_없다() {
        var actingAdmin = member(MemberRole.ADMIN, MemberStatus.ACTIVE);
        setUserId(actingAdmin, "admin-1");
        actingAs("admin-1");
        var targetOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "admin-1"))
            .thenReturn(Optional.of(actingAdmin));
        when(workspaceMemberRepository.findById(targetOwner.getId())).thenReturn(Optional.of(targetOwner));

        assertThatThrownBy(() ->
            workspaceService.changeRole(workspaceId, targetOwner.getId(), MemberRole.MEMBER)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void changeRole_OWNER_역할로는_이양할_수_없다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        setUserId(actingOwner, "owner-1");
        actingAs("owner-1");
        var targetMember = member(MemberRole.MEMBER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "owner-1"))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(targetMember.getId())).thenReturn(Optional.of(targetMember));

        assertThatThrownBy(() ->
            workspaceService.changeRole(workspaceId, targetMember.getId(), MemberRole.OWNER)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeRole_마지막_OWNER는_강등할_수_없다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        setUserId(actingOwner, "owner-1");
        actingAs("owner-1");
        var targetOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "owner-1"))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(targetOwner.getId())).thenReturn(Optional.of(targetOwner));
        when(workspaceMemberRepository.countByWorkspaceIdAndRoleAndStatus(
            workspaceId, MemberRole.OWNER, MemberStatus.ACTIVE
        )).thenReturn(1L);

        assertThatThrownBy(() ->
            workspaceService.changeRole(workspaceId, targetOwner.getId(), MemberRole.ADMIN)
        ).isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeRole_OWNER가_둘_이상이면_OWNER도_강등_가능하다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        setUserId(actingOwner, "owner-1");
        actingAs("owner-1");
        var targetOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "owner-1"))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(targetOwner.getId())).thenReturn(Optional.of(targetOwner));
        when(workspaceMemberRepository.countByWorkspaceIdAndRoleAndStatus(
            workspaceId, MemberRole.OWNER, MemberStatus.ACTIVE
        )).thenReturn(2L);

        var response = workspaceService.changeRole(workspaceId, targetOwner.getId(), MemberRole.ADMIN);

        assertThat(response.role()).isEqualTo("ADMIN");
        verify(outboxEventWriter).append(any(), any(), any(), any());
    }

    @Test
    void removeMember_자기_자신은_제거할_수_없다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        actingAs(actingOwner.getUserId());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actingOwner.getUserId()))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(actingOwner.getId())).thenReturn(Optional.of(actingOwner));

        assertThatThrownBy(() ->
            workspaceService.removeMember(workspaceId, actingOwner.getId())
        ).isInstanceOf(ForbiddenException.class);

        verify(workspaceMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_ADMIN은_OWNER를_제거할_수_없다() {
        var actingAdmin = member(MemberRole.ADMIN, MemberStatus.ACTIVE);
        setUserId(actingAdmin, "admin-1");
        actingAs("admin-1");
        var targetOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "admin-1"))
            .thenReturn(Optional.of(actingAdmin));
        when(workspaceMemberRepository.findById(targetOwner.getId())).thenReturn(Optional.of(targetOwner));

        assertThatThrownBy(() ->
            workspaceService.removeMember(workspaceId, targetOwner.getId())
        ).isInstanceOf(ForbiddenException.class);

        verify(workspaceMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_마지막_OWNER는_제거할_수_없다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        setUserId(actingOwner, "owner-1");
        actingAs("owner-1");
        var targetOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "owner-1"))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(targetOwner.getId())).thenReturn(Optional.of(targetOwner));
        when(workspaceMemberRepository.countByWorkspaceIdAndRoleAndStatus(
            workspaceId, MemberRole.OWNER, MemberStatus.ACTIVE
        )).thenReturn(1L);

        assertThatThrownBy(() ->
            workspaceService.removeMember(workspaceId, targetOwner.getId())
        ).isInstanceOf(BadRequestException.class);

        verify(workspaceMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_일반_멤버는_정상적으로_제거되고_이벤트가_발행된다() {
        var actingOwner = member(MemberRole.OWNER, MemberStatus.ACTIVE);
        setUserId(actingOwner, "owner-1");
        actingAs("owner-1");
        var targetMember = member(MemberRole.MEMBER, MemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "owner-1"))
            .thenReturn(Optional.of(actingOwner));
        when(workspaceMemberRepository.findById(targetMember.getId())).thenReturn(Optional.of(targetMember));

        workspaceService.removeMember(workspaceId, targetMember.getId());

        verify(workspaceMemberRepository).delete(targetMember);
        verify(outboxEventWriter).append(any(), any(), any(), any());
    }
}
