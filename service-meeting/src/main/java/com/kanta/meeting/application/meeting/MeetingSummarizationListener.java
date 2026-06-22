package com.kanta.meeting.application.meeting;

import com.kanta.meeting.application.outbox.OutboxEventWriter;
import com.kanta.meeting.domain.meeting.AiMeetingSummarizer;
import com.kanta.meeting.domain.meeting.MeetingSummarizationResult;
import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import com.kanta.meeting.domain.meeting.repository.ActionItemCandidateRepository;
import com.kanta.meeting.domain.meeting.repository.MeetingNoteRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MeetingSummarizationListener {
    private static final Logger log = LoggerFactory.getLogger(MeetingSummarizationListener.class);

    private final MeetingNoteRepository meetingNoteRepository;
    private final ActionItemCandidateRepository actionItemCandidateRepository;
    private final AiMeetingSummarizer aiMeetingSummarizer;
    private final OutboxEventWriter outboxEventWriter;

    public MeetingSummarizationListener(
        MeetingNoteRepository meetingNoteRepository,
        ActionItemCandidateRepository actionItemCandidateRepository,
        AiMeetingSummarizer aiMeetingSummarizer,
        OutboxEventWriter outboxEventWriter
    ) {
        this.meetingNoteRepository = meetingNoteRepository;
        this.actionItemCandidateRepository = actionItemCandidateRepository;
        this.aiMeetingSummarizer = aiMeetingSummarizer;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onMeetingNoteCreated(MeetingNoteCreatedEvent event) {
        var note = meetingNoteRepository.findById(event.meetingNoteId()).orElse(null);
        if (note == null) {
            return;
        }

        var result = summarize(note);
        if (result == null) {
            note.fail();
            return;
        }

        result.actionItemCandidates().forEach(candidate ->
            actionItemCandidateRepository.save(
                new ActionItemCandidate(note.getId(), candidate.title(), candidate.assigneeHint(), candidate.dueDateHint(), null, null)
            )
        );
        note.complete(result.summary());
        appendMeetingSummarized(note);
    }

    private MeetingSummarizationResult summarize(MeetingNote note) {
        try {
            return aiMeetingSummarizer.summarize(note.getRawText());
        } catch (Exception exception) {
            log.error("회의록 요약 처리에 실패했습니다. meetingNoteId={}", note.getId(), exception);
            return null;
        }
    }

    private void appendMeetingSummarized(MeetingNote note) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("meetingNoteId", note.getId());
        payload.put("boardId", note.getBoardId());
        payload.put("status", note.getStatus().name());
        payload.put("summarizedAt", Instant.now());
        outboxEventWriter.append("MEETING_NOTE", note.getId(), "meeting.summarized", payload);
    }
}
