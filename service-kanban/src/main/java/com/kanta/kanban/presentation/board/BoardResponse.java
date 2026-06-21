package com.kanta.kanban.presentation.board;

import com.kanta.kanban.domain.board.entity.Board;
import java.time.Instant;
import java.util.UUID;

public record BoardResponse(
    UUID id,
    UUID workspaceId,
    String name,
    Instant createdAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
            board.getId(),
            board.getWorkspaceId(),
            board.getName(),
            board.getCreatedAt()
        );
    }
}
