package com.kanta.kanban.presentation.internal;

import com.kanta.kanban.presentation.card.CardResponse;
import java.util.List;

public record InternalCardSearchResponse(List<CardResponse> content) {
}
