package com.kanta.meeting.presentation.meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterCardsRequest(
    @NotEmpty @Valid List<Item> items
) {
    public record Item(
        @NotBlank String title,
        UUID assigneeMemberId,
        LocalDate dueDate
    ) {
    }
}
