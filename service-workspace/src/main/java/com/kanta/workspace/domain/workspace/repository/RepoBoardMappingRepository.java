package com.kanta.workspace.domain.workspace.repository;

import com.kanta.workspace.domain.workspace.entity.RepoBoardMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoBoardMappingRepository extends JpaRepository<RepoBoardMapping, UUID> {
    Optional<RepoBoardMapping> findByGithubRepo(String githubRepo);

    boolean existsByGithubRepo(String githubRepo);

    List<RepoBoardMapping> findByWorkspaceId(UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
