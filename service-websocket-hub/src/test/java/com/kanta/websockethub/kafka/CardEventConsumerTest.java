package com.kanta.websockethub.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class CardEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final CardEventConsumer consumer = new CardEventConsumer(objectMapper, messagingTemplate);

    @Test
    void boardId가_있으면_해당_board_topic으로_브로드캐스트한다() {
        var payload = "{\"eventId\":\"e1\",\"eventType\":\"card.created\",\"boardId\":\"board-1\",\"cardId\":\"card-1\"}";

        consumer.consume(payload);

        verify(messagingTemplate).convertAndSend(eq("/topic/boards/board-1"), any(Object.class));
    }

    @Test
    void boardId가_없으면_브로드캐스트하지_않는다() {
        var payload = "{\"eventId\":\"e1\",\"eventType\":\"card.created\"}";

        consumer.consume(payload);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void 잘못된_JSON이면_예외를_던지지_않고_무시한다() {
        consumer.consume("not-a-json");

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
