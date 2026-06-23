package com.kanta.kanban.application.board;

import com.kanta.kanban.common.NotFoundException;
import com.kanta.kanban.domain.board.entity.Board;
import com.kanta.kanban.domain.board.repository.BoardRepository;
import com.kanta.kanban.presentation.board.BoardResponse;
import com.kanta.kanban.presentation.board.CreateBoardRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @Transactional
    public BoardResponse create(CreateBoardRequest request) {
        var board = new Board(request.workspaceId(), request.name().trim());
        return BoardResponse.from(boardRepository.save(board));
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> listByWorkspace(UUID workspaceId) {
        return boardRepository.findByWorkspaceId(workspaceId).stream()
            .map(BoardResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(UUID boardId) {
        return BoardResponse.from(findBoard(boardId));
    }

    @Transactional(readOnly = true)
    public Board findBoard(UUID boardId) {
        return boardRepository.findById(boardId)
            .orElseThrow(() -> new NotFoundException("보드를 찾을 수 없습니다.", "BOARD_NOT_FOUND"));
    }
}
