package com.kanta.meeting.presentation.meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterCardsRequest(
    @NotEmpty @Valid List<Item> items
) {
    public record Item(
        @NotNull UUID candidateId,
        UUID assigneeMemberId,
        LocalDate dueDate
    ) {
    }
}
