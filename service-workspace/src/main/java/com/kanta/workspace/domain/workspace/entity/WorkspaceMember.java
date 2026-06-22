package com.kanta.workspace.domain.workspace.entity;

import com.kanta.workspace.domain.workspace.enumeration.MemberRole;
import com.kanta.workspace.domain.workspace.enumeration.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_member")
public class WorkspaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    private String userId;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(length = 120)
    private String displayName;

    @Column(length = 120)
    private String githubUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    protected WorkspaceMember() {
    }

    public static WorkspaceMember owner(UUID workspaceId, String userId, String email, String displayName) {
        var member = new WorkspaceMember();
        member.workspaceId = workspaceId;
        member.userId = userId;
        member.email = email;
        member.displayName = displayName;
        member.role = MemberRole.OWNER;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    public static WorkspaceMember invited(UUID workspaceId, String email, MemberRole role) {
        var member = new WorkspaceMember();
        member.workspaceId = workspaceId;
        member.email = email;
        member.role = role;
        member.status = MemberStatus.INVITED;
        return member;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public MemberRole getRole() {
        return role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public void changeRole(MemberRole role) {
        this.role = role;
    }
}
