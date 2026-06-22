package com.kanta.meeting.domain.meeting.entity;

import com.kanta.meeting.domain.meeting.enumeration.MeetingNoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_note")
public class MeetingNote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID boardId;

    @Column(nullable = false, length = 120)
    private String creatorUserId;

    @Lob
    @Column(nullable = false)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingNoteStatus status;

    @Lob
    private String aiSummary;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    protected MeetingNote() {
    }

    public MeetingNote(UUID boardId, String creatorUserId, String rawText) {
        this.boardId = boardId;
        this.creatorUserId = creatorUserId;
        this.rawText = rawText;
        this.status = MeetingNoteStatus.PROCESSING;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public boolean isCompleted() {
        return status == MeetingNoteStatus.COMPLETED;
    }

    public String getRawText() {
        return rawText;
    }

    public MeetingNoteStatus getStatus() {
        return status;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void complete(String aiSummary) {
        this.status = MeetingNoteStatus.COMPLETED;
        this.aiSummary = aiSummary;
        this.updatedAt = Instant.now();
    }

    public void fail() {
        this.status = MeetingNoteStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}
