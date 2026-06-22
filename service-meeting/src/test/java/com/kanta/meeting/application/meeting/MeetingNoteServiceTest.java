package com.kanta.meeting.application.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.meeting.common.NotFoundException;
import com.kanta.meeting.domain.kanban.KanbanCardClient;
import com.kanta.meeting.domain.meeting.entity.ActionItemCandidate;
import com.kanta.meeting.domain.meeting.entity.MeetingNote;
import com.kanta.meeting.domain.meeting.repository.ActionItemCandidateRepository;
import com.kanta.meeting.domain.meeting.repository.MeetingNoteRepository;
import com.kanta.meeting.presentation.meeting.CreateMeetingNoteRequest;
import com.kanta.meeting.presentation.meeting.RegisterCardsRequest;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MeetingNoteServiceTest {

    @Mock
    private MeetingNoteRepository meetingNoteRepository;

    @Mock
    private ActionItemCandidateRepository actionItemCandidateRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private KanbanCardClient kanbanCardClient;

    private MeetingNoteService meetingNoteService;

    private final UUID boardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meetingNoteService = new MeetingNoteService(
            meetingNoteRepository, actionItemCandidateRepository, eventPublisher, kanbanCardClient
        );
    }

    private void setId(MeetingNote note, UUID id) {
        try {
            Field field = MeetingNote.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(note, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void create_회의록을_저장하고_PROCESSING_상태로_이벤트를_발행한다() {
        when(meetingNoteRepository.save(any())).thenAnswer(invocation -> {
            MeetingNote note = invocation.getArgument(0);
            setId(note, UUID.randomUUID());
            return note;
        });

        var response = meetingNoteService.create(new CreateMeetingNoteRequest(boardId, "오늘 회의에서는 로그인 기능을 다음 주까지 마치기로 했습니다."));

        assertThat(response.status()).isEqualTo("PROCESSING");
        verify(eventPublisher).publishEvent(any(MeetingNoteCreatedEvent.class));
    }

    @Test
    void get_존재하지_않는_회의록이면_NotFound() {
        var unknownId = UUID.randomUUID();
        when(meetingNoteRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingNoteService.get(unknownId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_요약과_액션아이템_후보를_함께_반환한다() {
        var note = new MeetingNote(boardId, "raw text");
        var noteId = UUID.randomUUID();
        setId(note, noteId);
        note.complete("요약된 내용");
        when(meetingNoteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(actionItemCandidateRepository.findByMeetingNoteId(noteId)).thenReturn(
            List.of(new ActionItemCandidate(noteId, "로그인 기능 구현", "우진님", "다음 주", null, null))
        );

        var response = meetingNoteService.get(noteId);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.aiSummary()).isEqualTo("요약된 내용");
        assertThat(response.actionItemCandidates()).hasSize(1);
        assertThat(response.actionItemCandidates().get(0).title()).isEqualTo("로그인 기능 구현");
    }

    @Test
    void registerCards_존재하지_않는_회의록이면_NotFound() {
        var unknownId = UUID.randomUUID();
        when(meetingNoteRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            meetingNoteService.registerCards(unknownId, new RegisterCardsRequest(List.of(
                new RegisterCardsRequest.Item("로그인 기능 구현", null, null)
            )))
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void registerCards_선택한_항목을_칸반_카드로_등록하고_생성된_ID_목록을_반환한다() {
        var note = new MeetingNote(boardId, "raw text");
        var noteId = UUID.randomUUID();
        setId(note, noteId);
        when(meetingNoteRepository.findById(noteId)).thenReturn(Optional.of(note));

        var assigneeMemberId = UUID.randomUUID();
        var dueDate = LocalDate.of(2026, 6, 28);
        var createdCardId = UUID.randomUUID();
        when(kanbanCardClient.createCard(boardId, "로그인 기능 구현", assigneeMemberId, dueDate)).thenReturn(createdCardId);

        var response = meetingNoteService.registerCards(noteId, new RegisterCardsRequest(List.of(
            new RegisterCardsRequest.Item("로그인 기능 구현", assigneeMemberId, dueDate)
        )));

        assertThat(response.createdCardIds()).containsExactly(createdCardId);
    }
}
