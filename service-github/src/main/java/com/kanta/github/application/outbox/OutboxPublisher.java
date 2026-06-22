package com.kanta.github.application.outbox;

import com.kanta.github.domain.outbox.repository.OutboxEventRepository;
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
    private final String commitMatchedTopic;
    private final int pollSize;

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${kanta.kafka.topics.commit-matched}") String commitMatchedTopic,
        @Value("${kanta.outbox.poll-size}") int pollSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.commitMatchedTopic = commitMatchedTopic;
        this.pollSize = pollSize;
    }

    @Scheduled(fixedDelayString = "${kanta.outbox.poll-delay-ms}")
    @Transactional
    public void publishPendingEvents() {
        var events = outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, pollSize));

        for (var event : events) {
            try {
                kafkaTemplate.send(commitMatchedTopic, event.getAggregateId().toString(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception exception) {
                event.markFailed(exception.getMessage());
            }
        }
    }
}
