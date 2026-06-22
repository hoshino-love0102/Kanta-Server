package com.kanta.github.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.domain.outbox.entity.OutboxEvent;
import com.kanta.github.domain.outbox.repository.OutboxEventRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventWriter {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void append(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        var eventId = UUID.randomUUID();
        var enrichedPayload = new LinkedHashMap<String, Object>();
        enrichedPayload.put("eventId", eventId);
        enrichedPayload.put("eventType", eventType);
        enrichedPayload.putAll(payload);

        outboxEventRepository.save(
            new OutboxEvent(eventId, aggregateType, aggregateId, eventType, toJson(enrichedPayload))
        );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Outbox payload 직렬화에 실패했습니다.", exception);
        }
    }
}
