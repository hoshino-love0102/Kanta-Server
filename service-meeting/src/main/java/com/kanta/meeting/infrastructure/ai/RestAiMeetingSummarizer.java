package com.kanta.meeting.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kanta.meeting.domain.meeting.AiMeetingSummarizer;
import com.kanta.meeting.domain.meeting.MeetingSummarizationResult;
import com.kanta.meeting.domain.meeting.MeetingSummarizationResult.ActionItemCandidateDraft;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestAiMeetingSummarizer implements AiMeetingSummarizer {
    private final RestClient restClient;
    private final AiClientProperties properties;
    private final NaiveAiMeetingSummarizer fallback = new NaiveAiMeetingSummarizer();

    public RestAiMeetingSummarizer(RestClient.Builder restClientBuilder, AiClientProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public MeetingSummarizationResult summarize(MeetingNoteAiRequest request) {
        try {
            var response = restClient.post()
                .uri("/v1/meeting/action-items")
                .body(AiMeetingRequest.from(request, properties.timezone()))
                .retrieve()
                .body(AiMeetingResponse.class);
            if (response == null) {
                return fallback.summarize(request);
            }
            return response.toResult();
        } catch (Exception exception) {
            return fallback.summarize(request);
        }
    }

    private record AiMeetingRequest(
        String meetingNoteId,
        String boardId,
        String rawText,
        LocalDate currentDate,
        String timezone
    ) {
        private static AiMeetingRequest from(MeetingNoteAiRequest request, String timezone) {
            return new AiMeetingRequest(
                request.meetingNoteId().toString(),
                request.boardId().toString(),
                request.rawText(),
                LocalDate.now(ZoneId.of(timezone)),
                timezone
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiMeetingResponse(String summary, List<AiActionItem> actionItems) {
        private MeetingSummarizationResult toResult() {
            var candidates = actionItems == null ? List.<ActionItemCandidateDraft>of() : actionItems.stream()
                .map(item -> new ActionItemCandidateDraft(item.title(), item.assigneeName(), item.dueDate()))
                .toList();
            return new MeetingSummarizationResult(summary == null ? "" : summary, candidates);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiActionItem(String title, String assigneeName, String dueDate) {
    }
}
