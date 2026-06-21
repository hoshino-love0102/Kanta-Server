package com.kanta.kanban.presentation.card;

import com.kanta.kanban.domain.card.entity.Card;
import com.kanta.kanban.domain.card.enumeration.CardSource;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
    UUID id,
    UUID boardId,
    String title,
    CardStatus status,
    CardSource source,
    UUID assigneeMemberId,
    String assigneeDisplayName,
    LocalDate dueDate,
    Instant createdAt,
    Instant updatedAt
) {
    public static CardResponse from(Card card) {
        return from(card, null);
    }

    public static CardResponse from(Card card, String assigneeDisplayName) {
        return new CardResponse(
            card.getId(),
            card.getBoard().getId(),
            card.getTitle(),
            card.getStatus(),
            card.getSource(),
            card.getAssigneeMemberId(),
            assigneeDisplayName,
            card.getDueDate(),
            card.getCreatedAt(),
            card.getUpdatedAt()
        );
    }
}
