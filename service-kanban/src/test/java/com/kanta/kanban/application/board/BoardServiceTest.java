package com.kanta.kanban.application.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanta.kanban.common.NotFoundException;
import com.kanta.kanban.domain.board.entity.Board;
import com.kanta.kanban.domain.board.repository.BoardRepository;
import com.kanta.kanban.presentation.board.CreateBoardRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BoardServiceTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final BoardService boardService = new BoardService(boardRepository);

    @Test
    void 보드를_생성하면_저장하고_응답을_반환한다() {
        var workspaceId = UUID.randomUUID();
        var request = new CreateBoardRequest(workspaceId, " 새 보드 ");
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = boardService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("새 보드");
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void 존재하는_보드를_조회하면_응답을_반환한다() {
        var board = new Board(UUID.randomUUID(), "board");
        var boardId = UUID.randomUUID();
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

        var response = boardService.get(boardId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("board");
    }

    @Test
    void 존재하지_않는_보드를_조회하면_NotFoundException이_발생한다() {
        var boardId = UUID.randomUUID();
        when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.get(boardId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findBoard는_존재하는_보드_엔티티를_반환한다() {
        var board = new Board(UUID.randomUUID(), "board");
        var boardId = UUID.randomUUID();
        when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));

        var found = boardService.findBoard(boardId);

        assertThat(found).isEqualTo(board);
    }

    @Test
    void findBoard는_존재하지_않으면_NotFoundException이_발생한다() {
        var boardId = UUID.randomUUID();
        when(boardRepository.findById(boardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.findBoard(boardId))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 워크스페이스의_보드_목록을_조회한다() {
        var workspaceId = UUID.randomUUID();
        var board = new Board(workspaceId, "board");
        when(boardRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(board));

        var responses = boardService.listByWorkspace(workspaceId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("board");
    }

    @Test
    void 보드가_없는_워크스페이스는_빈_목록을_반환한다() {
        var workspaceId = UUID.randomUUID();
        when(boardRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of());

        var responses = boardService.listByWorkspace(workspaceId);

        assertThat(responses).isEmpty();
    }
}
