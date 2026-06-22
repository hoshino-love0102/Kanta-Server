package com.kanta.github.infrastructure.ai;

import com.kanta.github.domain.commitlink.CommitSimilarityMatcher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * ai-pipeline 유사도 매칭(gRPC) 연동 전 임시 구현. 항상 후보 없음을 반환한다.
 */
@Component
public class NaiveCommitSimilarityMatcher implements CommitSimilarityMatcher {
    @Override
    public Optional<ScoredCardMatch> match(UUID boardId, String commitMessage) {
        return Optional.empty();
    }
}
