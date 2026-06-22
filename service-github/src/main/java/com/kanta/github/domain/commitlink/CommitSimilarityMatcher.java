package com.kanta.github.domain.commitlink;

import com.kanta.github.domain.kanban.CardMatch;
import java.util.Optional;
import java.util.UUID;

public interface CommitSimilarityMatcher {
    Optional<ScoredCardMatch> match(UUID boardId, String commitMessage);

    record ScoredCardMatch(CardMatch card, double score) {
    }
}
