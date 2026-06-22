package com.kanta.meeting.infrastructure.ai;

import com.kanta.meeting.domain.meeting.AiMeetingSummarizer;
import com.kanta.meeting.domain.meeting.MeetingSummarizationResult;
import com.kanta.meeting.domain.meeting.MeetingSummarizationResult.ActionItemCandidateDraft;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaiveAiMeetingSummarizer implements AiMeetingSummarizer {
    private static final int MAX_CANDIDATES = 10;
    private static final int SUMMARY_MAX_LENGTH = 200;

    @Override
    public MeetingSummarizationResult summarize(String rawText) {
        var sentences = List.of(rawText.split("(?<=[.!?]|[다요죠]\\.)\\s*|\\R+")).stream()
            .map(String::trim)
            .filter(sentence -> !sentence.isEmpty())
            .limit(MAX_CANDIDATES)
            .toList();

        var candidates = sentences.stream()
            .map(sentence -> new ActionItemCandidateDraft(sentence, null, null))
            .toList();

        var summary = rawText.trim().length() > SUMMARY_MAX_LENGTH
            ? rawText.trim().substring(0, SUMMARY_MAX_LENGTH) + "..."
            : rawText.trim();

        return new MeetingSummarizationResult(summary, candidates);
    }
}
