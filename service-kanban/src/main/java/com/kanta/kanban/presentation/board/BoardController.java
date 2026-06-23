package com.kanta.kanban.presentation.board;

import com.kanta.kanban.application.board.BoardService;
import com.kanta.kanban.common.ApiResponse;
import com.kanta.kanban.infrastructure.security.UserAccess;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@UserAccess
@RestController
@RequestMapping("/boards")
public class BoardController {
    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BoardResponse>> create(@Valid @RequestBody CreateBoardRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(boardService.create(request)));
    }

    @GetMapping
    public ApiResponse<List<BoardResponse>> listByWorkspace(@RequestParam UUID workspaceId) {
        return ApiResponse.ok(boardService.listByWorkspace(workspaceId));
    }

    @GetMapping("/{boardId}")
    public ApiResponse<BoardResponse> get(@PathVariable UUID boardId) {
        return ApiResponse.ok(boardService.get(boardId));
    }
}
