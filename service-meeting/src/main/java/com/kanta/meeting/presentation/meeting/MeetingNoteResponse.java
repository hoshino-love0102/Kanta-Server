package com.kanta.meeting.presentation.meeting;

import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import java.util.List;
import java.util.UUID;

public record MeetingNoteResponse(
    UUID id,
    UUID boardId,
    String status,
    String aiSummary,
    List<ActionItemCandidateResponse> actionItemCandidates
) {
    public static MeetingNoteResponse from(MeetingNote note, List<ActionItemCandidate> candidates) {
        return new MeetingNoteResponse(
            note.getId(),
            note.getBoardId(),
            note.getStatus().name(),
            note.getAiSummary(),
            candidates.stream().map(ActionItemCandidateResponse::from).toList()
        );
    }
}
