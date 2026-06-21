package com.kanta.auth.domain.principal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "principal")
public class Principal {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String role;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Principal() {
    }

    public Principal(UUID userId, String email, String displayName, String role) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.updatedAt = Instant.now();
    }

    public void update(String email, String displayName, String role) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }
}
