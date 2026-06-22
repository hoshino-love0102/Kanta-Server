package com.kanta.github.domain.kanban;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface KanbanCardFinder {
    Optional<CardMatch> findByTitleContains(UUID boardId, String query);

    default List<CardMatch> findCandidates(UUID boardId, String query) {
        return findByTitleContains(boardId, query).stream().toList();
    }
}
