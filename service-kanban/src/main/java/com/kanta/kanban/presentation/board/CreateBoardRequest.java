package com.kanta.kanban.presentation.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBoardRequest(
    @NotNull(message = "workspaceId는 필수입니다.")
    UUID workspaceId,

    @NotBlank(message = "보드 이름은 필수입니다.")
    String name
) {
}
