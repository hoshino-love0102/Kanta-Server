package com.kanta.kanban.presentation.card;

import java.util.List;

public record CardMoveLogsResponse(
    List<CardMoveLogResponse> content
) {
}
