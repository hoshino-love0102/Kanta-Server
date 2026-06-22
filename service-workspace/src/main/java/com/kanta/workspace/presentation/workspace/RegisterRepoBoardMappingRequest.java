package com.kanta.workspace.presentation.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterRepoBoardMappingRequest(
    @NotBlank String githubRepo,
    @NotNull UUID boardId
) {
}
