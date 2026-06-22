package com.kanta.github.domain.kanban;

import java.time.LocalDate;
import java.util.UUID;

public interface KanbanCardClient {
    UUID createCard(UUID boardId, String title, UUID assigneeMemberId, LocalDate dueDate);
}
