package com.kanta.meeting.domain.meeting;

public interface AiMeetingSummarizer {
    MeetingSummarizationResult summarize(String rawText);
}
