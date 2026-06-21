package com.kanta.kanban.presentation.card;

import com.kanta.kanban.domain.card.entity.Card;
import com.kanta.kanban.domain.card.enumeration.CardSource;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCardResponse(
    UUID id,
    UUID boardId,
    String title,
    CardStatus status,
    CardSource source,
    UUID assigneeMemberId,
    LocalDate dueDate,
    Instant createdAt
) {
    public static CreateCardResponse from(Card card) {
        return new CreateCardResponse(
            card.getId(),
            card.getBoard().getId(),
            card.getTitle(),
            card.getStatus(),
            card.getSource(),
            card.getAssigneeMemberId(),
            card.getDueDate(),
            card.getCreatedAt()
        );
    }
}
