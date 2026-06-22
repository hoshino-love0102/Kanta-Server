package com.kanta.websockethub.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class CardEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CardEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public CardEventConsumer(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "${kanta.kafka.topics.card-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        Map<String, Object> event;
        try {
            event = objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception exception) {
            log.warn("card.events payload 파싱에 실패했습니다: {}", payload, exception);
            return;
        }

        var boardId = event.get("boardId");
        if (boardId == null) {
            log.warn("card.events payload에 boardId가 없습니다: {}", payload);
            return;
        }

        messagingTemplate.convertAndSend("/topic/boards/" + boardId, event);
    }
}
