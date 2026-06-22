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
@Table(name = "workspace")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 200)
    private String githubOrg;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Workspace() {
    }

    public Workspace(String name, String githubOrg) {
        this.name = name;
        this.githubOrg = githubOrg;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGithubOrg() {
        return githubOrg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
