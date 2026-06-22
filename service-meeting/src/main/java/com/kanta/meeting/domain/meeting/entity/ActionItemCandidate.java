package com.kanta.meeting.domain.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "action_item_candidate")
public class ActionItemCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID meetingNoteId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 120)
    private String assigneeHint;

    @Column(length = 20)
    private String dueDateHint;

    private UUID assigneeMemberId;

    private LocalDate dueDate;

    protected ActionItemCandidate() {
    }

    public ActionItemCandidate(
        UUID meetingNoteId,
        String title,
        String assigneeHint,
        String dueDateHint,
        UUID assigneeMemberId,
        LocalDate dueDate
    ) {
        this.meetingNoteId = meetingNoteId;
        this.title = title;
        this.assigneeHint = assigneeHint;
        this.dueDateHint = dueDateHint;
        this.assigneeMemberId = assigneeMemberId;
        this.dueDate = dueDate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMeetingNoteId() {
        return meetingNoteId;
    }

    public String getTitle() {
        return title;
    }

    public String getAssigneeHint() {
        return assigneeHint;
    }

    public String getDueDateHint() {
        return dueDateHint;
    }

    public UUID getAssigneeMemberId() {
        return assigneeMemberId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}
