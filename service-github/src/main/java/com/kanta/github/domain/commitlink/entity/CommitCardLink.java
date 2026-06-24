package com.kanta.github.domain.commitlink.entity;

import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commit_card_link")
public class CommitCardLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID boardId;

    @Column(nullable = false, length = 64)
    private String commitSha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String commitMessage;

    private UUID candidateCardId;

    @Column(length = 500)
    private String candidateCardTitle;

    private Double similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MatchStatus matchStatus;

    private UUID cardId;

    private UUID newCardId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    protected CommitCardLink() {
    }

    public CommitCardLink(
        UUID boardId,
        String commitSha,
        String commitMessage,
        UUID candidateCardId,
        String candidateCardTitle,
        Double similarityScore,
        MatchStatus matchStatus
    ) {
        this.boardId = boardId;
        this.commitSha = commitSha;
        this.commitMessage = commitMessage;
        this.candidateCardId = candidateCardId;
        this.candidateCardTitle = candidateCardTitle;
        this.similarityScore = similarityScore;
        this.matchStatus = matchStatus;
        if (matchStatus == MatchStatus.AUTO_CONFIRMED || matchStatus == MatchStatus.AUTO_CREATED) {
            this.cardId = candidateCardId;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public UUID getCandidateCardId() {
        return candidateCardId;
    }

    public String getCandidateCardTitle() {
        return candidateCardTitle;
    }

    public Double getSimilarityScore() {
        return similarityScore;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getNewCardId() {
        return newCardId;
    }

    public void confirm() {
        this.matchStatus = MatchStatus.CONFIRMED;
        this.cardId = this.candidateCardId;
        this.updatedAt = Instant.now();
    }

    public void reject(UUID newCardId) {
        this.matchStatus = MatchStatus.REJECTED;
        this.newCardId = newCardId;
        this.updatedAt = Instant.now();
    }
}
