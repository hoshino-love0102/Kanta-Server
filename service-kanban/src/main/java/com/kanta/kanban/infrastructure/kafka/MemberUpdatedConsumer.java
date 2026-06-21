package com.kanta.kanban.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.kanban.application.workspace.WorkspaceMemberCacheService;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MemberUpdatedConsumer {
    private final ObjectMapper objectMapper;
    private final WorkspaceMemberCacheService workspaceMemberCacheService;

    public MemberUpdatedConsumer(ObjectMapper objectMapper, WorkspaceMemberCacheService workspaceMemberCacheService) {
        this.objectMapper = objectMapper;
        this.workspaceMemberCacheService = workspaceMemberCacheService;
    }

    @KafkaListener(topics = "${kanta.kafka.topics.member-updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) throws Exception {
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
        });

        var memberId = uuid(event, "workspaceMemberId");
        var workspaceId = uuid(event, "workspaceId");
        var userId = text(event, "userId");
        var displayName = text(event, "displayName");
        var role = optionalText(event, "role");
        var active = optionalBoolean(event, "active", true);

        workspaceMemberCacheService.upsert(memberId, workspaceId, userId, displayName, role, active);
    }

    private UUID uuid(Map<String, Object> event, String key) {
        return UUID.fromString(text(event, key));
    }

    private String text(Map<String, Object> event, String key) {
        var value = event.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("member.updated 이벤트에 " + key + " 값이 없습니다.");
        }
        return value.toString();
    }

    private String optionalText(Map<String, Object> event, String key) {
        var value = event.get(key);
        return value == null ? null : value.toString();
    }

    private boolean optionalBoolean(Map<String, Object> event, String key, boolean defaultValue) {
        var value = event.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }
}
