package com.kanta.workspace.domain.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_repo_board_mapping")
public class RepoBoardMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 200)
    private String githubRepo;

    @Column(nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected RepoBoardMapping() {
    }

    public RepoBoardMapping(UUID workspaceId, String githubRepo, UUID boardId) {
        this.workspaceId = workspaceId;
        this.githubRepo = githubRepo;
        this.boardId = boardId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
