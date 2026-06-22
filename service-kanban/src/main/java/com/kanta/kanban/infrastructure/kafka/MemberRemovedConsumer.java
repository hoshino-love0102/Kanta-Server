package com.kanta.kanban.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.kanban.application.workspace.WorkspaceMemberCacheService;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MemberRemovedConsumer {
    private final ObjectMapper objectMapper;
    private final WorkspaceMemberCacheService workspaceMemberCacheService;

    public MemberRemovedConsumer(ObjectMapper objectMapper, WorkspaceMemberCacheService workspaceMemberCacheService) {
        this.objectMapper = objectMapper;
        this.workspaceMemberCacheService = workspaceMemberCacheService;
    }

    @KafkaListener(topics = "${kanta.kafka.topics.member-removed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) throws Exception {
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
        });

        var memberId = uuid(event, "workspaceMemberId");
        workspaceMemberCacheService.remove(memberId);
    }

    private UUID uuid(Map<String, Object> event, String key) {
        var value = event.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("member.removed 이벤트에 " + key + " 값이 없습니다.");
        }
        return UUID.fromString(value.toString());
    }
}
