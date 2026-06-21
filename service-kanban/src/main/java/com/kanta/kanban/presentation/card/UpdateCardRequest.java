package com.kanta.kanban.presentation.card;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateCardRequest(
    String title,
    UUID assigneeMemberId,
    LocalDate dueDate
) {
}
