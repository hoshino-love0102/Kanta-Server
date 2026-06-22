package com.kanta.github.infrastructure.kanban;

import com.fasterxml.jackson.databind.JsonNode;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestKanbanCardFinder implements KanbanCardFinder {
    private final RestClient restClient;

    public RestKanbanCardFinder(RestClient.Builder restClientBuilder, KanbanClientProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<CardMatch> findByTitleContains(UUID boardId, String query) {
        try {
            var response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/boards/{boardId}/cards/search")
                    .queryParam("titleContains", query)
                    .build(boardId))
                .retrieve()
                .body(JsonNode.class);

            if (response == null) {
                return Optional.empty();
            }
            var items = response.path("data").path("content");
            if (!items.isArray() || items.isEmpty()) {
                return Optional.empty();
            }
            var first = items.get(0);
            return Optional.of(new CardMatch(UUID.fromString(first.path("id").asText()), first.path("title").asText()));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
