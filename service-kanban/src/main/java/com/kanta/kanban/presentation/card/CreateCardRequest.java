package com.kanta.kanban.presentation.card;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCardRequest(
    @NotBlank(message = "카드 제목은 필수입니다.")
    String title,
    UUID assigneeMemberId,
    LocalDate dueDate
) {
}
