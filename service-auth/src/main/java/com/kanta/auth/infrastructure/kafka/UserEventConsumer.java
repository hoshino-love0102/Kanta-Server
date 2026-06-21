package com.kanta.auth.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.auth.application.principal.PrincipalCacheService;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {
    private final ObjectMapper objectMapper;
    private final PrincipalCacheService principalCacheService;

    public UserEventConsumer(ObjectMapper objectMapper, PrincipalCacheService principalCacheService) {
        this.objectMapper = objectMapper;
        this.principalCacheService = principalCacheService;
    }

    @KafkaListener(topics = "${kanta.kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) throws Exception {
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
        });

        var eventType = text(event, "eventType");
        if (!"user.created".equals(eventType) && !"user.updated".equals(eventType)) {
            return;
        }

        var userId = UUID.fromString(text(event, "userId"));
        var email = text(event, "email");
        var displayName = text(event, "displayName");
        var role = text(event, "role");

        principalCacheService.upsert(userId, email, displayName, role);
    }

    private String text(Map<String, Object> event, String key) {
        var value = event.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("user 이벤트에 " + key + " 값이 없습니다.");
        }
        return value.toString();
    }
}
