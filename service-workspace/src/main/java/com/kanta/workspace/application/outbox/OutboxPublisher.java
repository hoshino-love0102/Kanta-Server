package com.kanta.workspace.application.outbox;

import com.kanta.workspace.domain.outbox.repository.OutboxEventRepository;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "kanta.outbox", name = "publisher-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String memberUpdatedTopic;
    private final int pollSize;

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${kanta.kafka.topics.member-updated}") String memberUpdatedTopic,
        @Value("${kanta.outbox.poll-size}") int pollSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.memberUpdatedTopic = memberUpdatedTopic;
        this.pollSize = pollSize;
    }

    @Scheduled(fixedDelayString = "${kanta.outbox.poll-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, pollSize));

        for (var event : events) {
            try {
                kafkaTemplate.send(memberUpdatedTopic, event.getAggregateId().toString(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception exception) {
                event.markFailed(exception.getMessage());
            }
        }
    }
}
