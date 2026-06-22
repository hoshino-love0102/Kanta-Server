package com.kanta.workspace.presentation.workspace;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
    @NotBlank String name,
    String githubOrg
) {
}
