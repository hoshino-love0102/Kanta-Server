package com.kanta.github.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import com.kanta.github.domain.kanban.CardMatch;
import com.kanta.github.domain.kanban.KanbanCardFinder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestCommitSimilarityMatcher implements CommitSimilarityMatcher {
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
            return Optional.empty();
        }

        try {
            var response = restClient.post()
                .uri("/v1/commit/match")
                .body(new AiCommitMatchRequest(boardId.toString(), null, commitMessage, candidates.stream()
                    .map(AiCardCandidate::from)
                    .toList()))
                .retrieve()
                .body(AiCommitMatchResponse.class);
            if (response == null || response.matches() == null || response.matches().isEmpty()) {
                return Optional.empty();
            }
            var first = response.matches().get(0);
            return Optional.of(new ScoredCardMatch(
                new CardMatch(UUID.fromString(first.cardId()), first.title()),
                first.score()
            ));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private record AiCommitMatchRequest(
        String boardId,
        String commitSha,
        String commitMessage,
        List<AiCardCandidate> candidates
    ) {
    }

    private record AiCardCandidate(String cardId, String title, String description) {
        private static AiCardCandidate from(CardMatch card) {
            return new AiCardCandidate(card.id().toString(), card.title(), null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiCommitMatchResponse(List<AiCommitMatch> matches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiCommitMatch(String cardId, String title, double score, String reason) {
    }
}
