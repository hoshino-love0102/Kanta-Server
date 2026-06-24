package com.kanta.github.application.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher.ScoredCardMatch;
import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardClient;
import com.kanta.github.domain.outbox.entity.OutboxEvent;
import com.kanta.github.domain.outbox.repository.OutboxEventRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class DeferredCommitSimilarityMatchProcessorTest {
    @Test
    void 지연된_유사도_매칭_이벤트를_처리해_pending_link를_저장한다() {
        var outboxEventRepository = org.mockito.Mockito.mock(OutboxEventRepository.class);
        var commitSimilarityMatcher = org.mockito.Mockito.mock(CommitSimilarityMatcher.class);
        var commitCardLinkRepository = org.mockito.Mockito.mock(CommitCardLinkRepository.class);
        var kanbanCardClient = org.mockito.Mockito.mock(KanbanCardClient.class);
        var boardId = UUID.randomUUID();
        var cardId = UUID.randomUUID();
        var event = new OutboxEvent(
            UUID.randomUUID(),
            "COMMIT_SIMILARITY_MATCH",
            UUID.randomUUID(),
            "commit.similarity-match.requested",
            """
                {"boardId":"%s","commitSha":"abc123","commitMessage":"implement webhook"}
                """.formatted(boardId)
        );
        var processor = new DeferredCommitSimilarityMatchProcessor(
            outboxEventRepository,
            commitSimilarityMatcher,
            commitCardLinkRepository,
            kanbanCardClient,
            new ObjectMapper(),
            10
        );

        when(outboxEventRepository.findByEventTypeAndPublishedAtIsNullOrderByOccurredAtAsc(
            eq("commit.similarity-match.requested"),
            any(Pageable.class)
        )).thenReturn(List.of(event));
        when(commitSimilarityMatcher.match(boardId, "implement webhook"))
            .thenReturn(Optional.of(new ScoredCardMatch(new CardMatch(cardId, "Webhook 구현"), 0.91)));

        processor.processPendingRequests();

        var captor = ArgumentCaptor.forClass(CommitCardLink.class);
        verify(commitCardLinkRepository).save(captor.capture());
        verify(commitSimilarityMatcher).match(boardId, "implement webhook");

        var link = captor.getValue();
        assertThat(link.getBoardId()).isEqualTo(boardId);
        assertThat(link.getCommitSha()).isEqualTo("abc123");
        assertThat(link.getCandidateCardId()).isEqualTo(cardId);
        assertThat(link.getMatchStatus()).isEqualTo(MatchStatus.PENDING_CONFIRMATION);
    }
}
