package com.kanta.github.infrastructure.kanban;

import com.fasterxml.jackson.databind.JsonNode;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestKanbanCardFinder implements KanbanCardFinder {
    private static final Logger log = LoggerFactory.getLogger(RestKanbanCardFinder.class);
    private static final int MIN_TOKEN_LENGTH = 2;
    private static final int MAX_CANDIDATES = 10;

    private final RestClient restClient;

    public RestKanbanCardFinder(RestClient.Builder restClientBuilder, KanbanClientProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<CardMatch> findByTitleContains(UUID boardId, String query) {
        return findCandidates(boardId, query).stream().findFirst();
    }

    // 카드 제목(query 전체)이 commit message에 포함되는지가 아니라, commit message의 각 단어가
    // 카드 제목에 포함되는지를 봐야 한다. 그래야 "결제 모듈 리팩토링 진행중" 같은 긴 커밋 메시지가
    // "결제 모듈 리팩토링" 카드를 후보로 찾을 수 있다.
    @Override
    public List<CardMatch> findCandidates(UUID boardId, String query) {
        var tokens = query.split("[\\s,.:;!?()\\[\\]\"']+");
        var merged = new LinkedHashMap<UUID, CardMatch>();

        for (var token : tokens) {
            if (token.length() < MIN_TOKEN_LENGTH || merged.size() >= MAX_CANDIDATES) {
                continue;
            }
            for (var candidate : searchByTitleContains(boardId, token)) {
                merged.putIfAbsent(candidate.id(), candidate);
            }
        }

        return List.copyOf(merged.values());
    }

    private List<CardMatch> searchByTitleContains(UUID boardId, String titleContains) {
        try {
            var response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/boards/{boardId}/cards/search")
                    .queryParam("titleContains", titleContains)
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
            log.warn("카드 후보 조회에 실패했습니다. boardId={}, titleContains={}", boardId, titleContains, exception);
            return List.of();
        }
    }
}
