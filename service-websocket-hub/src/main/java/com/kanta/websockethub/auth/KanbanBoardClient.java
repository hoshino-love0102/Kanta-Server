package com.kanta.websockethub.auth;

import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KanbanBoardClient {
    private final RestClient restClient;

    public KanbanBoardClient(@Value("${kanta.client.kanban-service-url}") String kanbanServiceUrl) {
        this.restClient = RestClient.create(kanbanServiceUrl);
    }

    public UUID findWorkspaceId(UUID boardId, String passportValue) {
        try {
            var response = restClient.get()
                .uri("/boards/{boardId}", boardId)
                .header("X-User-Passport", passportValue)
                .retrieve()
                .body(Map.class);

            @SuppressWarnings("unchecked")
            var data = (Map<String, Object>) response.get("data");
            return UUID.fromString((String) data.get("workspaceId"));
        } catch (RestClientResponseException exception) {
            throw new SubscriptionDeniedException("보드를 조회할 수 없습니다.", exception);
        }
    }
}
