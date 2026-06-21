package com.kanta.kanban.presentation.card;

import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.time.Instant;
import java.util.UUID;

public record ChangeCardStatusResponse(
    UUID id,
    CardStatus status,
    Instant updatedAt
) {
}
