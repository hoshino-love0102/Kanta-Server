package com.kanta.meeting.presentation.meeting;

import java.util.List;
import java.util.UUID;

public record RegisterCardsResponse(
    List<UUID> createdCardIds
) {
}
