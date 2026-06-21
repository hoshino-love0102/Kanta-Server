package com.kanta.kanban.domain.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_member_cache")
public class WorkspaceMemberCache {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 120)
    private String userId;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(length = 50)
    private String role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant syncedAt = Instant.now();

    protected WorkspaceMemberCache() {
    }

    public WorkspaceMemberCache(UUID id, UUID workspaceId, String userId, String displayName, String role, boolean active) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
    }

    public void update(UUID workspaceId, String userId, String displayName, String role, boolean active) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.syncedAt = Instant.now();
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

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }
}
