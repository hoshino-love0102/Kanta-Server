package com.kanta.kanban.presentation.card;

import com.kanta.kanban.domain.card.enumeration.CardStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCardStatusRequest(
    @NotNull(message = "toStatus는 필수입니다.")
    CardStatus toStatus
) {
}
