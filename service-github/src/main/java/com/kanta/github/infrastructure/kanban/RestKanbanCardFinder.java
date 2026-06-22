package com.kanta.github.infrastructure.kanban;

import com.fasterxml.jackson.databind.JsonNode;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import java.util.ArrayList;
import java.util.List;
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
        return findCandidates(boardId, query).stream().findFirst();
    }

    @Override
    public List<CardMatch> findCandidates(UUID boardId, String query) {
        try {
            var response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/boards/{boardId}/cards/search")
                    .queryParam("titleContains", query)
                    .build(boardId))
                .retrieve()
                .body(JsonNode.class);

            if (response == null) {
                return List.of();
            }
            var items = response.path("data").path("content");
            if (!items.isArray() || items.isEmpty()) {
                return List.of();
            }
            var candidates = new ArrayList<CardMatch>();
            for (var item : items) {
                candidates.add(new CardMatch(UUID.fromString(item.path("id").asText()), item.path("title").asText()));
            }
            return candidates;
        } catch (Exception exception) {
            return List.of();
        }
    }
}
