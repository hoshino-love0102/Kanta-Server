package com.kanta.github.domain.workspace;

import java.util.Optional;

public interface WorkspaceRepoResolver {
    Optional<RepoBoardMapping> resolve(String githubRepo);
}
