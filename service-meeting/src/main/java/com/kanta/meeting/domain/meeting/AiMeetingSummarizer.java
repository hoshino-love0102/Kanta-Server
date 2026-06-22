package com.kanta.meeting.domain.meeting;

public interface AiMeetingSummarizer {
    MeetingSummarizationResult summarize(MeetingNoteAiRequest request);

    record MeetingNoteAiRequest(java.util.UUID meetingNoteId, java.util.UUID boardId, String rawText) {
    }
}
