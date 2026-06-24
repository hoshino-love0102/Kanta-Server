package com.kanta.workspace.presentation.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record RegisterRepoBoardMappingRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", message = "githubRepo는 owner/repo 형식이어야 합니다.")
    String githubRepo,
    @NotNull UUID boardId
) {
}
