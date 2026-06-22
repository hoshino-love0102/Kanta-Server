package com.kanta.meeting.application.meeting;

import com.kanta.meeting.common.NotFoundException;
import com.kanta.meeting.domain.kanban.KanbanCardClient;
import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import com.kanta.meeting.domain.meeting.repository.ActionItemCandidateRepository;
import com.kanta.meeting.domain.meeting.repository.MeetingNoteRepository;
import com.kanta.meeting.presentation.meeting.CreateMeetingNoteRequest;
import com.kanta.meeting.presentation.meeting.CreateMeetingNoteResponse;
import com.kanta.meeting.presentation.meeting.MeetingNoteResponse;
import com.kanta.meeting.presentation.meeting.RegisterCardsRequest;
import com.kanta.meeting.presentation.meeting.RegisterCardsResponse;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingNoteService {
    private final MeetingNoteRepository meetingNoteRepository;
    private final ActionItemCandidateRepository actionItemCandidateRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final KanbanCardClient kanbanCardClient;

    public MeetingNoteService(
        MeetingNoteRepository meetingNoteRepository,
        ActionItemCandidateRepository actionItemCandidateRepository,
        ApplicationEventPublisher eventPublisher,
        KanbanCardClient kanbanCardClient
    ) {
        this.meetingNoteRepository = meetingNoteRepository;
        this.actionItemCandidateRepository = actionItemCandidateRepository;
        this.eventPublisher = eventPublisher;
        this.kanbanCardClient = kanbanCardClient;
    }

    @Transactional
    public CreateMeetingNoteResponse create(CreateMeetingNoteRequest request) {
        var note = meetingNoteRepository.save(new MeetingNote(request.boardId(), request.rawText()));
        eventPublisher.publishEvent(new MeetingNoteCreatedEvent(note.getId()));
        return new CreateMeetingNoteResponse(note.getId(), note.getStatus().name());
    }

    @Transactional(readOnly = true)
    public MeetingNoteResponse get(UUID meetingNoteId) {
        var note = findNote(meetingNoteId);
        var candidates = actionItemCandidateRepository.findByMeetingNoteId(meetingNoteId);
        return MeetingNoteResponse.from(note, candidates);
    }

    @Transactional
    public RegisterCardsResponse registerCards(UUID meetingNoteId, RegisterCardsRequest request) {
        var note = findNote(meetingNoteId);
        var createdCardIds = request.items().stream()
            .map(item -> kanbanCardClient.createCard(note.getBoardId(), item.title(), item.assigneeMemberId(), item.dueDate()))
            .toList();
        return new RegisterCardsResponse(createdCardIds);
    }

    private MeetingNote findNote(UUID meetingNoteId) {
        return meetingNoteRepository.findById(meetingNoteId)
            .orElseThrow(() -> new NotFoundException("회의록을 찾을 수 없습니다.", "MEETING_NOTE_NOT_FOUND"));
    }
}
