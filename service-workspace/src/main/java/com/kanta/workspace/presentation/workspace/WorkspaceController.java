package com.kanta.workspace.presentation.workspace;

import com.kanta.workspace.application.workspace.WorkspaceService;
import com.kanta.workspace.common.ApiResponse;
import com.kanta.workspace.common.PageResponse;
import com.kanta.workspace.infrastructure.security.UserAccess;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@UserAccess
@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(workspaceService.create(request)));
    }

    @GetMapping("/me")
    public ApiResponse<List<WorkspaceResponse>> getMyWorkspaces() {
        return ApiResponse.ok(workspaceService.getMyWorkspaces());
    }

    @GetMapping("/invitations/me")
    public ApiResponse<List<PendingInvitationResponse>> getMyInvitations() {
        return ApiResponse.ok(workspaceService.getMyInvitations());
    }

    @PostMapping("/{workspaceId}/invitations/accept")
    public ApiResponse<MemberResponse> acceptInvitation(@PathVariable UUID workspaceId) {
        return ApiResponse.ok(workspaceService.acceptInvitation(workspaceId));
    }

    @GetMapping("/{workspaceId}/members")
    public ApiResponse<PageResponse<MemberResponse>> getMembers(
        @PathVariable UUID workspaceId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(workspaceService.getMembers(workspaceId, page, size));
    }

    @PostMapping("/{workspaceId}/members/invite")
    public ResponseEntity<ApiResponse<InviteMemberResponse>> invite(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody InviteMemberRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(workspaceService.invite(workspaceId, request)));
    }

    @PatchMapping("/{workspaceId}/members/{memberId}/role")
    public ApiResponse<ChangeMemberRoleResponse> changeRole(
        @PathVariable UUID workspaceId,
        @PathVariable UUID memberId,
        @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        return ApiResponse.ok(workspaceService.changeRole(workspaceId, memberId, request.role()));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID workspaceId, @PathVariable UUID memberId) {
        workspaceService.removeMember(workspaceId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workspaceId}/repo-mappings")
    public ResponseEntity<ApiResponse<RepoBoardMappingResponse>> registerRepoBoardMapping(
        @PathVariable UUID workspaceId,
        @Valid @RequestBody RegisterRepoBoardMappingRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(workspaceService.registerRepoBoardMapping(workspaceId, request)));
    }
}
