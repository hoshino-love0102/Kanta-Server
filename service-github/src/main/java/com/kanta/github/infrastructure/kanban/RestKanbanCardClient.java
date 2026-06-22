package com.kanta.github.infrastructure.kanban;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.common.BadRequestException;
import com.kanta.github.domain.kanban.KanbanCardClient;
import com.kanta.github.infrastructure.security.PassportHolder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestKanbanCardClient implements KanbanCardClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestKanbanCardClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, KanbanClientProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public UUID createCard(UUID boardId, String title, UUID assigneeMemberId, LocalDate dueDate) {
        var body = new LinkedHashMap<String, Object>();
        body.put("title", title);
        body.put("assigneeMemberId", assigneeMemberId);
        body.put("dueDate", dueDate);

        var response = restClient.post()
            .uri("/boards/{boardId}/cards", boardId)
            .header("X-User-Passport", buildPassportHeader())
            .body(body)
            .retrieve()
            .onStatus(status -> status.value() >= 400, (request, clientResponse) -> {
                throw new BadRequestException("칸반 카드 생성에 실패했습니다.", "KANBAN_CARD_CREATE_FAILED");
            })
            .body(JsonNode.class);

        var cardIdNode = response == null ? null : response.path("data").path("id");
        if (cardIdNode == null || !cardIdNode.isTextual()) {
            throw new BadRequestException("칸반 카드 생성 응답이 올바르지 않습니다.", "KANBAN_CARD_CREATE_FAILED");
        }
        try {
            return UUID.fromString(cardIdNode.asText());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("칸반 카드 생성 응답이 올바르지 않습니다.", "KANBAN_CARD_CREATE_FAILED");
        }
    }

    private String buildPassportHeader() {
        var passport = PassportHolder.current();
        try {
            var json = objectMapper.writeValueAsString(passport);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Passport 직렬화에 실패했습니다.", exception);
        }
    }
}
