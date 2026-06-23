package com.kanta.kanban.application.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.kanban.application.board.BoardService;
import com.kanta.kanban.application.outbox.OutboxEventWriter;
import com.kanta.kanban.application.workspace.WorkspaceMemberCacheService;
import com.kanta.kanban.common.BadRequestException;
import com.kanta.kanban.common.NotFoundException;
import com.kanta.kanban.domain.card.WorkspaceMemberResolver;
import com.kanta.kanban.domain.board.entity.Board;
import com.kanta.kanban.domain.card.entity.Card;
import com.kanta.kanban.domain.card.entity.CardMoveLog;
import com.kanta.kanban.domain.card.enumeration.CardSource;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import com.kanta.kanban.domain.card.enumeration.MovedByType;
import com.kanta.kanban.domain.card.repository.CardMoveLogRepository;
import com.kanta.kanban.domain.card.repository.CardRepository;
import com.kanta.kanban.infrastructure.security.Passport;
import com.kanta.kanban.infrastructure.security.PassportHolder;
import com.kanta.kanban.presentation.card.ChangeCardStatusRequest;
import com.kanta.kanban.presentation.card.CreateCardRequest;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardServiceTest {

    private final BoardService boardService = mock(BoardService.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final CardMoveLogRepository cardMoveLogRepository = mock(CardMoveLogRepository.class);
    private final WorkspaceMemberResolver workspaceMemberResolver = mock(WorkspaceMemberResolver.class);
    private final WorkspaceMemberCacheService workspaceMemberCacheService = mock(WorkspaceMemberCacheService.class);
    private final OutboxEventWriter outboxEventWriter = mock(OutboxEventWriter.class);

    private final CardService cardService = new CardService(
        boardService,
        cardRepository,
        cardMoveLogRepository,
        workspaceMemberResolver,
        workspaceMemberCacheService,
        outboxEventWriter
    );

    @AfterEach
    void clearPassport() {
        PassportHolder.clear();
    }

    private Card newCard(Board board, CardStatus status) throws Exception {
        var card = new Card(board, "title", status, CardSource.MANUAL, null, null);
        setId(card, UUID.randomUUID());
        return card;
    }

    private void setId(Card card, UUID id) throws Exception {
        Field field = Card.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(card, id);
    }

    @Test
    void 카드를_생성하면_card_created_이벤트를_발행한다() {
        var workspaceId = UUID.randomUUID();
        var boardId = UUID.randomUUID();
        var board = new Board(workspaceId, "board");
        when(boardService.findBoard(boardId)).thenReturn(board);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateCardRequest("새 카드", null, LocalDate.now());
        var response = cardService.create(boardId, request);

        assertThat(response).isNotNull();
        verify(cardRepository).save(any(Card.class));
        verify(outboxEventWriter).append(eq("CARD"), eq((UUID) null), eq("card.created"), anyMap());
    }

    @Test
    void 카드_상태_변경시_상태와_무브로그_이벤트가_갱신된다() throws Exception {
        var workspaceId = UUID.randomUUID();
        var board = new Board(workspaceId, "board");
        var card = newCard(board, CardStatus.TODO);
        var cardId = card.getId();
        var movedByMemberId = UUID.randomUUID();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(workspaceMemberResolver.resolve(eq(workspaceId), anyString())).thenReturn(movedByMemberId);
        PassportHolder.set(new Passport("user-1", "user", "MEMBER"));

        var response = cardService.changeStatus(cardId, new ChangeCardStatusRequest(CardStatus.IN_PROGRESS));

        assertThat(response.status()).isEqualTo(CardStatus.IN_PROGRESS);
        assertThat(card.getStatus()).isEqualTo(CardStatus.IN_PROGRESS);
        verify(cardMoveLogRepository).save(any(CardMoveLog.class));
        verify(outboxEventWriter).append(eq("CARD"), eq(cardId), eq("card.moved"), anyMap());
    }

    @Test
    void 동일한_상태로_변경을_시도하면_BadRequestException이_발생한다() throws Exception {
        var board = new Board(UUID.randomUUID(), "board");
        var card = newCard(board, CardStatus.TODO);
        var cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.changeStatus(cardId, new ChangeCardStatusRequest(CardStatus.TODO)))
            .isInstanceOf(BadRequestException.class);

        verify(cardMoveLogRepository, never()).save(any(CardMoveLog.class));
        verify(outboxEventWriter, never()).append(any(), any(), eq("card.moved"), anyMap());
    }

    @Test
    void changeStatus시_카드가_없으면_NotFoundException이_발생한다() {
        var cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.changeStatus(cardId, new ChangeCardStatusRequest(CardStatus.DONE)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void moveBySystem은_상태가_변경되면_무브로그와_이벤트를_기록한다() throws Exception {
        var board = new Board(UUID.randomUUID(), "board");
        var card = newCard(board, CardStatus.TODO);
        var cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.moveBySystem(cardId, CardStatus.DONE);

        assertThat(card.getStatus()).isEqualTo(CardStatus.DONE);
        verify(cardMoveLogRepository).save(any(CardMoveLog.class));
        verify(outboxEventWriter).append(eq("CARD"), eq(cardId), eq("card.moved"), anyMap());
    }

    @Test
    void moveBySystem은_fromStatus와_toStatus가_같으면_아무것도_하지_않는다() throws Exception {
        var board = new Board(UUID.randomUUID(), "board");
        var card = newCard(board, CardStatus.IN_PROGRESS);
        var cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.moveBySystem(cardId, CardStatus.IN_PROGRESS);

        assertThat(card.getStatus()).isEqualTo(CardStatus.IN_PROGRESS);
        verify(cardMoveLogRepository, never()).save(any(CardMoveLog.class));
        verify(outboxEventWriter, never()).append(any(), any(), anyString(), anyMap());
    }

    @Test
    void moveBySystem시_카드가_없으면_NotFoundException이_발생한다() {
        var cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.moveBySystem(cardId, CardStatus.DONE))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 카드를_삭제하면_card_deleted_이벤트를_발행하고_저장소에서_삭제한다() throws Exception {
        var board = new Board(UUID.randomUUID(), "board");
        var card = newCard(board, CardStatus.TODO);
        var cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.delete(cardId);

        verify(outboxEventWriter).append(eq("CARD"), eq(cardId), eq("card.deleted"), anyMap());
        verify(cardRepository).delete(card);
    }

    @Test
    void delete시_카드가_없으면_NotFoundException이_발생한다() {
        var cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.delete(cardId))
            .isInstanceOf(NotFoundException.class);

        verify(outboxEventWriter, never()).append(any(), any(), anyString(), anyMap());
        verify(cardRepository, never()).delete(any(Card.class));
    }

    @Test
    void getMoveLogs_조회시_카드가_없으면_NotFoundException이_발생한다() {
        var cardId = UUID.randomUUID();
        when(cardRepository.existsById(cardId)).thenReturn(false);

        assertThatThrownBy(() -> cardService.getMoveLogs(cardId, 0, 10))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void changeStatus시_PassportHolder에_사용자_정보가_없으면_UnauthorizedException이_발생한다() throws Exception {
        var board = new Board(UUID.randomUUID(), "board");
        var card = newCard(board, CardStatus.TODO);
        var cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.changeStatus(cardId, new ChangeCardStatusRequest(CardStatus.DONE)))
            .isInstanceOf(com.kanta.kanban.common.UnauthorizedException.class);
    }

    @Test
    void getCards_조회시_보드가_없으면_NotFoundException이_발생한다() {
        var boardId = UUID.randomUUID();
        when(boardService.findBoard(boardId)).thenThrow(new NotFoundException("보드를 찾을 수 없습니다.", "BOARD_NOT_FOUND"));

        assertThatThrownBy(() -> cardService.getCards(boardId, null, null, 0, 10))
            .isInstanceOf(NotFoundException.class);
    }
}
