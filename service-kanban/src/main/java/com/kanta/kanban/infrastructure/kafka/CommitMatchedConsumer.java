package com.kanta.kanban.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.kanban.application.card.CardService;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CommitMatchedConsumer {
    private static final Set<String> CARD_MOVING_STATUSES = Set.of("AUTO_CONFIRMED", "CONFIRMED");

    private final ObjectMapper objectMapper;
    private final CardService cardService;

    public CommitMatchedConsumer(ObjectMapper objectMapper, CardService cardService) {
        this.objectMapper = objectMapper;
        this.cardService = cardService;
    }

    @KafkaListener(topics = "${kanta.kafka.topics.commit-matched}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) throws Exception {
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
        });

        var matchStatus = String.valueOf(event.get("matchStatus"));
        if (!CARD_MOVING_STATUSES.contains(matchStatus)) {
            return;
        }
        var cardIdValue = event.get("cardId");
        if (cardIdValue == null) {
            return;
        }
        cardService.moveBySystem(UUID.fromString(cardIdValue.toString()), CardStatus.IN_PROGRESS);
    }
}
