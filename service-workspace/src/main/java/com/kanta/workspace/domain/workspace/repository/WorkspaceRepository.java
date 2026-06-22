package com.kanta.workspace.domain.workspace.repository;

import com.kanta.workspace.domain.workspace.entity.Workspace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
