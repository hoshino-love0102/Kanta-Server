package com.kanta.github.application.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.kanban.KanbanCardClient;
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
    private final KanbanCardClient kanbanCardClient;
    private final ObjectMapper objectMapper;
    private final int pollSize;

    public DeferredCommitSimilarityMatchProcessor(
        OutboxEventRepository outboxEventRepository,
        CommitSimilarityMatcher commitSimilarityMatcher,
        CommitCardLinkRepository commitCardLinkRepository,
        KanbanCardClient kanbanCardClient,
        ObjectMapper objectMapper,
        @Value("${kanta.deferred-similarity.poll-size:${kanta.outbox.poll-size}}") int pollSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.commitSimilarityMatcher = commitSimilarityMatcher;
        this.commitCardLinkRepository = commitCardLinkRepository;
        this.kanbanCardClient = kanbanCardClient;
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

                var match = commitSimilarityMatcher.match(boardId, commitMessage);
                if (match.isPresent()) {
                    var scored = match.get();
                    commitCardLinkRepository.save(
                        new CommitCardLink(
                            boardId,
                            commitSha,
                            commitMessage,
                            scored.card().id(),
                            scored.card().title(),
                            scored.score(),
                            MatchStatus.PENDING_CONFIRMATION
                        )
                    );
                } else {
                    var title = toCardTitle(commitMessage);
                    var newCardId = kanbanCardClient.createCard(boardId, title, null, null);
                    commitCardLinkRepository.save(
                        new CommitCardLink(boardId, commitSha, commitMessage, newCardId, title, null, MatchStatus.AUTO_CREATED)
                    );
                }
                event.markPublished();
            } catch (Exception exception) {
                event.markFailed(exception.getMessage());
            }
        }
    }

    private String toCardTitle(String commitMessage) {
        var firstLine = commitMessage.strip().lines().findFirst().orElse("").strip();
        if (firstLine.isEmpty()) {
            firstLine = "GitHub 커밋";
        }
        return firstLine.length() > 200 ? firstLine.substring(0, 200) : firstLine;
    }
}
