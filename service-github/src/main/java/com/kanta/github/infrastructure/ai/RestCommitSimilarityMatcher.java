package com.kanta.github.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestCommitSimilarityMatcher implements CommitSimilarityMatcher {
    private static final Logger log = LoggerFactory.getLogger(RestCommitSimilarityMatcher.class);

    private final RestClient restClient;
    private final KanbanCardFinder kanbanCardFinder;

    public RestCommitSimilarityMatcher(
        RestClient.Builder restClientBuilder,
        AiClientProperties properties,
        KanbanCardFinder kanbanCardFinder
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.kanbanCardFinder = kanbanCardFinder;
    }

    @Override
    public Optional<ScoredCardMatch> match(UUID boardId, String commitMessage) {
        var candidates = kanbanCardFinder.findCandidates(boardId, commitMessage);
        if (candidates.isEmpty()) {
            log.info("AI 매칭 후보가 없어 호출을 건너뜁니다. boardId={}, commitMessage={}", boardId, commitMessage);
            return Optional.empty();
        }

        try {
            var requestBody = new AiCommitMatchRequest(boardId.toString(), null, commitMessage, candidates.stream()
                .map(AiCardCandidate::from)
                .toList());
            log.info("AI 커밋 매칭 요청. candidates={}", candidates.size());
            var response = restClient.post()
                .uri("/v1/commit/match")
                .body(requestBody)
                .retrieve()
                .body(AiCommitMatchResponse.class);
            log.info("AI 커밋 매칭 응답. response={}", response);
            if (response == null || response.matches() == null || response.matches().isEmpty()) {
                return Optional.empty();
            }
            var first = response.matches().get(0);
            return Optional.of(new ScoredCardMatch(
                new CardMatch(UUID.fromString(first.cardId()), first.title()),
                first.score()
            ));
        } catch (Exception exception) {
            log.warn("AI 서버 커밋 매칭 호출에 실패했습니다. boardId={}", boardId, exception);
            return Optional.empty();
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record AiCommitMatchRequest(
        String boardId,
        String commitSha,
        String commitMessage,
        List<AiCardCandidate> candidates
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record AiCardCandidate(String cardId, String title, String description) {
        private static AiCardCandidate from(CardMatch card) {
            return new AiCardCandidate(card.id().toString(), card.title(), null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record AiCommitMatchResponse(List<AiCommitMatch> matches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record AiCommitMatch(String cardId, String title, double score, String reason) {
    }
}
