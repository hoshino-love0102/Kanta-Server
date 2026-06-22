package com.kanta.meeting.presentation.meeting;

import java.util.UUID;

public record CreateMeetingNoteResponse(
    UUID id,
    String status
) {
}
