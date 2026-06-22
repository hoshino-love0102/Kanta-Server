package com.kanta.meeting.domain.meeting;

import java.util.List;

public record MeetingSummarizationResult(
    String summary,
    List<ActionItemCandidateDraft> actionItemCandidates
) {
    public record ActionItemCandidateDraft(String title, String assigneeHint, String dueDateHint) {
    }
}
