package com.kanta.github.presentation.commitlink;

import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import java.util.UUID;

public record PendingCommitLinkResponse(
    UUID id,
    String commitSha,
    String commitMessage,
    UUID candidateCardId,
    String candidateCardTitle,
    Double similarityScore,
    String matchStatus
) {
    public static PendingCommitLinkResponse from(CommitCardLink link) {
        return new PendingCommitLinkResponse(
            link.getId(),
            link.getCommitSha(),
            link.getCommitMessage(),
            link.getCandidateCardId(),
            link.getCandidateCardTitle(),
            link.getSimilarityScore(),
            link.getMatchStatus().name()
        );
    }
}
