package com.kanta.meeting.presentation.meeting;

import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import java.time.LocalDate;
import java.util.UUID;

public record ActionItemCandidateResponse(
    UUID candidateId,
    String title,
    String assigneeHint,
    String dueDateHint,
    UUID assigneeMemberId,
    LocalDate dueDate
) {
    public static ActionItemCandidateResponse from(ActionItemCandidate candidate) {
        return new ActionItemCandidateResponse(
            candidate.getId(),
            candidate.getTitle(),
            candidate.getAssigneeHint(),
            candidate.getDueDateHint(),
            candidate.getAssigneeMemberId(),
            candidate.getDueDate()
        );
    }
}
