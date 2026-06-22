package com.kanta.github.domain.kanban;

import java.util.Optional;
import java.util.UUID;

public interface KanbanCardFinder {
    Optional<CardMatch> findByTitleContains(UUID boardId, String query);
}
