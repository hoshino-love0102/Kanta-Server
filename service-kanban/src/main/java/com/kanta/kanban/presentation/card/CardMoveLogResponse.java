package com.kanta.kanban.presentation.card;

import com.kanta.kanban.domain.card.entity.CardMoveLog;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.time.Instant;
import java.util.UUID;

public record CardMoveLogResponse(
    UUID id,
    CardStatus fromStatus,
    CardStatus toStatus,
    UUID movedByMemberId,
    Instant movedAt
) {
    public static CardMoveLogResponse from(CardMoveLog moveLog) {
        return new CardMoveLogResponse(
            moveLog.getId(),
            moveLog.getFromStatus(),
            moveLog.getToStatus(),
            moveLog.getMovedByMemberId(),
            moveLog.getMovedAt()
        );
    }
}
