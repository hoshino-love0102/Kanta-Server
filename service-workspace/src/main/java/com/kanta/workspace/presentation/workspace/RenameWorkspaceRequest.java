package com.kanta.workspace.presentation.workspace;

import jakarta.validation.constraints.NotBlank;

public record RenameWorkspaceRequest(
    @NotBlank String name
) {
}
