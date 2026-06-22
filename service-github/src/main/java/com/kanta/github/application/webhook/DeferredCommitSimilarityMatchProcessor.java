package com.kanta.github.application.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.outbox.repository.OutboxEventRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeferredCommitSimilarityMatchProcessor {
    private static final String EVENT_TYPE = "commit.similarity-match.requested";

    private final OutboxEventRepository outboxEventRepository;
    private final CommitSimilarityMatcher commitSimilarityMatcher;
    private final CommitCardLinkRepository commitCardLinkRepository;
    private final ObjectMapper objectMapper;
    private final int pollSize;

    public DeferredCommitSimilarityMatchProcessor(
        OutboxEventRepository outboxEventRepository,
        CommitSimilarityMatcher commitSimilarityMatcher,
        CommitCardLinkRepository commitCardLinkRepository,
        ObjectMapper objectMapper,
        @Value("${kanta.deferred-similarity.poll-size:${kanta.outbox.poll-size}}") int pollSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.commitSimilarityMatcher = commitSimilarityMatcher;
        this.commitCardLinkRepository = commitCardLinkRepository;
        this.objectMapper = objectMapper;
        this.pollSize = pollSize;
    }

    @Scheduled(fixedDelayString = "${kanta.deferred-similarity.poll-delay-ms:${kanta.outbox.poll-delay-ms}}")
    @Transactional
    public void processPendingRequests() {
        var events = outboxEventRepository.findByEventTypeAndPublishedAtIsNullOrderByOccurredAtAsc(
            EVENT_TYPE,
            PageRequest.of(0, pollSize)
        );

        for (var event : events) {
            try {
                var payload = objectMapper.readTree(event.getPayload());
                var boardId = UUID.fromString(payload.path("boardId").asText());
                var commitSha = payload.path("commitSha").asText();
                var commitMessage = payload.path("commitMessage").asText("");

                commitSimilarityMatcher.match(boardId, commitMessage)
                    .ifPresent(scored -> commitCardLinkRepository.save(
                        new CommitCardLink(
                            boardId,
                            commitSha,
                            commitMessage,
                            scored.card().id(),
                            scored.card().title(),
                            scored.score(),
                            MatchStatus.PENDING_CONFIRMATION
                        )
                    ));
                event.markPublished();
            } catch (Exception exception) {
                event.markFailed(exception.getMessage());
            }
        }
    }
}
