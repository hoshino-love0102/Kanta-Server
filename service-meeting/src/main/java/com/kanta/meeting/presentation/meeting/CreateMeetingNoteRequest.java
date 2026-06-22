package com.kanta.meeting.presentation.meeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMeetingNoteRequest(
    @NotNull UUID boardId,
    @NotBlank String rawText
) {
}
