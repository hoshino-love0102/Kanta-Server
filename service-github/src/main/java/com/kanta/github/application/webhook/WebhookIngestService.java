package com.kanta.github.application.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.github.application.outbox.OutboxEventWriter;
import com.kanta.github.common.BadRequestException;
import com.kanta.github.common.DomainException;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import com.kanta.github.domain.webhook.entity.GithubWebhookEvent;
import com.kanta.github.domain.webhook.repository.GithubWebhookEventRepository;
import com.kanta.github.domain.workspace.WorkspaceRepoResolver;
import com.kanta.github.infrastructure.webhook.GithubSignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookIngestService {
    private static final Logger log = LoggerFactory.getLogger(WebhookIngestService.class);

    private final GithubSignatureVerifier signatureVerifier;
    private final GithubWebhookEventRepository githubWebhookEventRepository;
    private final CommitCardLinkRepository commitCardLinkRepository;
    private final WorkspaceRepoResolver workspaceRepoResolver;
    private final KanbanCardFinder kanbanCardFinder;
    private final CommitSimilarityMatcher commitSimilarityMatcher;
    private final IssueReferenceParser issueReferenceParser;
    private final OutboxEventWriter outboxEventWriter;
    private final ObjectMapper objectMapper;

    public WebhookIngestService(
        GithubSignatureVerifier signatureVerifier,
        GithubWebhookEventRepository githubWebhookEventRepository,
        CommitCardLinkRepository commitCardLinkRepository,
        WorkspaceRepoResolver workspaceRepoResolver,
        KanbanCardFinder kanbanCardFinder,
        CommitSimilarityMatcher commitSimilarityMatcher,
        IssueReferenceParser issueReferenceParser,
        OutboxEventWriter outboxEventWriter,
        ObjectMapper objectMapper
    ) {
        this.signatureVerifier = signatureVerifier;
        this.githubWebhookEventRepository = githubWebhookEventRepository;
        this.commitCardLinkRepository = commitCardLinkRepository;
        this.workspaceRepoResolver = workspaceRepoResolver;
        this.kanbanCardFinder = kanbanCardFinder;
        this.commitSimilarityMatcher = commitSimilarityMatcher;
        this.issueReferenceParser = issueReferenceParser;
        this.outboxEventWriter = outboxEventWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingestPush(String rawBody, String signatureHeader, String deliveryId, String eventType) {
        if (!signatureVerifier.verify(rawBody, signatureHeader)) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "GitHub 서명 검증에 실패했습니다.", "INVALID_SIGNATURE");
        }
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new BadRequestException("X-GitHub-Delivery 헤더가 필요합니다.", "MISSING_DELIVERY_ID");
        }

        var payloadHash = sha256Hex(rawBody);
        if (githubWebhookEventRepository.findByDeliveryId(deliveryId).isPresent()) {
            log.info("중복 수신된 webhook delivery는 무시합니다. deliveryId={}", deliveryId);
            return;
        }
        githubWebhookEventRepository.save(new GithubWebhookEvent(deliveryId, payloadHash, eventType));

        if (!"push".equals(eventType)) {
            return;
        }

        var payload = parsePayload(rawBody);
        var githubRepo = payload.path("repository").path("full_name").asText(null);
        var ref = payload.path("ref").asText(null);
        var mapping = githubRepo == null ? null : workspaceRepoResolver.resolve(githubRepo).orElse(null);
        if (mapping == null) {
            log.warn("repo-board 매핑을 찾을 수 없어 매칭을 건너뜁니다. githubRepo={}", githubRepo);
            return;
        }

        for (var commitNode : payload.path("commits")) {
            processCommit(mapping.boardId(), commitNode, ref);
        }
    }

    private void processCommit(UUID boardId, JsonNode commitNode, String ref) {
        var commitSha = commitNode.path("id").asText(null);
        var commitMessage = commitNode.path("message").asText("");
        if (commitSha == null) {
            return;
        }

        var issueCode = issueReferenceParser.parse(ref, commitMessage).orElse(null);
        var autoMatch = issueCode == null
            ? Optional.<com.kanta.github.domain.kanban.CardMatch>empty()
            : kanbanCardFinder.findByTitleContains(boardId, issueCode);

        if (autoMatch.isPresent()) {
            var card = autoMatch.get();
            var link = commitCardLinkRepository.save(
                new CommitCardLink(boardId, commitSha, commitMessage, card.id(), card.title(), null, MatchStatus.AUTO_CONFIRMED)
            );
            appendCommitMatched(link);
            return;
        }

        var similarityMatch = commitSimilarityMatcher.match(boardId, commitMessage);
        similarityMatch.ifPresent(scored -> commitCardLinkRepository.save(
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
    }

    private void appendCommitMatched(CommitCardLink link) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("commitCardLinkId", link.getId());
        payload.put("commitSha", link.getCommitSha());
        payload.put("cardId", link.getCardId());
        payload.put("matchStatus", link.getMatchStatus().name());
        payload.put("matchedAt", Instant.now());
        outboxEventWriter.append("COMMIT_CARD_LINK", link.getId(), "commit.matched", payload);
    }

    private JsonNode parsePayload(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new BadRequestException("webhook payload 형식이 올바르지 않습니다.", "INVALID_PAYLOAD");
        }
    }

    private String sha256Hex(String rawBody) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawBody.getBytes(StandardCharsets.UTF_8));
            var hex = new StringBuilder(digest.length * 2);
            for (var b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("payload hash 계산에 실패했습니다.", exception);
        }
    }
}
