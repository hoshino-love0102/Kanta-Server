package com.kanta.github.application.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.application.outbox.OutboxEventWriter;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import com.kanta.github.domain.webhook.repository.GithubWebhookEventRepository;
import com.kanta.github.domain.workspace.RepoBoardMapping;
import com.kanta.github.domain.workspace.WorkspaceRepoResolver;
import com.kanta.github.infrastructure.webhook.GithubSignatureVerifier;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookIngestServiceTest {
    @Mock
    private GithubSignatureVerifier signatureVerifier;
    @Mock
    private GithubWebhookEventRepository githubWebhookEventRepository;
    @Mock
    private CommitCardLinkRepository commitCardLinkRepository;
    @Mock
    private WorkspaceRepoResolver workspaceRepoResolver;
    @Mock
    private KanbanCardFinder kanbanCardFinder;
    @Mock
    private CommitSimilarityMatcher commitSimilarityMatcher;
    @Mock
    private OutboxEventWriter outboxEventWriter;

    @Test
    void rate_limit_초과_웹훅은_유사도_매칭을_즉시_호출하지_않고_outbox에_지연_요청을_남긴다() {
        var boardId = UUID.randomUUID();
        var workspaceId = UUID.randomUUID();
        var service = new WebhookIngestService(
            signatureVerifier,
            githubWebhookEventRepository,
            commitCardLinkRepository,
            workspaceRepoResolver,
            kanbanCardFinder,
            commitSimilarityMatcher,
            new IssueReferenceParser(),
            outboxEventWriter,
            new ObjectMapper()
        );
        var rawBody = """
            {
              "repository": {"full_name": "kanta/server"},
              "ref": "refs/heads/main",
              "commits": [{"id": "abc123", "message": "implement webhook"}]
            }
            """;

        when(signatureVerifier.verify(rawBody, "signature")).thenReturn(true);
        when(githubWebhookEventRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.empty());
        when(workspaceRepoResolver.resolve("kanta/server")).thenReturn(Optional.of(new RepoBoardMapping(workspaceId, boardId)));

        service.ingestPush(rawBody, "signature", "delivery-1", "push", true);

        verify(commitSimilarityMatcher, never()).match(any(), any());
        verify(outboxEventWriter).append(
            eq("COMMIT_SIMILARITY_MATCH"),
            any(UUID.class),
            eq("commit.similarity-match.requested"),
            org.mockito.ArgumentMatchers.<Map<String, Object>>any()
        );
    }
}
