package com.kanta.meeting.application.meeting;

import com.kanta.meeting.common.BadRequestException;
import com.kanta.meeting.common.ForbiddenException;
import com.kanta.meeting.common.NotFoundException;
import com.kanta.meeting.domain.kanban.KanbanCardClient;
import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import com.kanta.meeting.domain.meeting.repository.ActionItemCandidateRepository;
import com.kanta.meeting.domain.meeting.repository.MeetingNoteRepository;
import com.kanta.meeting.infrastructure.security.PassportHolder;
import com.kanta.meeting.presentation.meeting.CreateMeetingNoteRequest;
import com.kanta.meeting.presentation.meeting.CreateMeetingNoteResponse;
import com.kanta.meeting.presentation.meeting.MeetingNoteResponse;
import com.kanta.meeting.presentation.meeting.RegisterCardsRequest;
import com.kanta.meeting.presentation.meeting.RegisterCardsResponse;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
        var creatorUserId = PassportHolder.current().requireUserId();
        var note = meetingNoteRepository.save(new MeetingNote(request.boardId(), creatorUserId, request.rawText()));
        eventPublisher.publishEvent(new MeetingNoteCreatedEvent(note.getId()));
        return new CreateMeetingNoteResponse(note.getId(), note.getStatus().name());
    }

    @Transactional(readOnly = true)
    public MeetingNoteResponse get(UUID meetingNoteId) {
        var note = findNote(meetingNoteId);
        requireOwner(note);
        var candidates = actionItemCandidateRepository.findByMeetingNoteId(meetingNoteId);
        return MeetingNoteResponse.from(note, candidates);
    }

    @Transactional
    public RegisterCardsResponse registerCards(UUID meetingNoteId, RegisterCardsRequest request) {
        var note = findNote(meetingNoteId);
        requireOwner(note);
        if (!note.isCompleted()) {
            throw new BadRequestException("요약이 완료된 회의록만 카드로 등록할 수 있습니다.", "MEETING_NOTE_NOT_COMPLETED");
        }

        var candidateIds = request.items().stream().map(RegisterCardsRequest.Item::candidateId).toList();
        var candidates = actionItemCandidateRepository.findByIdInAndMeetingNoteId(candidateIds, meetingNoteId);
        var candidateById = candidates.stream().collect(Collectors.toMap(ActionItemCandidate::getId, c -> c));

        var createdCardIds = request.items().stream()
            .map(item -> registerCard(note, item, candidateById))
            .toList();
        return new RegisterCardsResponse(createdCardIds);
    }

    private UUID registerCard(MeetingNote note, RegisterCardsRequest.Item item, Map<UUID, ActionItemCandidate> candidateById) {
        var candidate = candidateById.get(item.candidateId());
        if (candidate == null) {
            throw new NotFoundException("해당 회의록의 액션 아이템 후보를 찾을 수 없습니다.", "ACTION_ITEM_CANDIDATE_NOT_FOUND");
        }
        if (candidate.isRegistered()) {
            throw new BadRequestException("이미 카드로 등록된 액션 아이템입니다.", "ACTION_ITEM_CANDIDATE_ALREADY_REGISTERED");
        }

        var cardId = kanbanCardClient.createCard(note.getBoardId(), candidate.getTitle(), item.assigneeMemberId(), item.dueDate());
        candidate.markRegistered();
        return cardId;
    }

    private MeetingNote findNote(UUID meetingNoteId) {
        return meetingNoteRepository.findById(meetingNoteId)
            .orElseThrow(() -> new NotFoundException("회의록을 찾을 수 없습니다.", "MEETING_NOTE_NOT_FOUND"));
    }

    private void requireOwner(MeetingNote note) {
        var userId = PassportHolder.current().requireUserId();
        if (!note.getCreatorUserId().equals(userId)) {
            throw new ForbiddenException("이 회의록에 접근할 권한이 없습니다.", "MEETING_NOTE_ACCESS_DENIED");
        }
    }
}
