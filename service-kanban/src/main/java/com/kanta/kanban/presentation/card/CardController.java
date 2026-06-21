package com.kanta.kanban.presentation.card;

import com.kanta.kanban.application.card.CardService;
import com.kanta.kanban.common.ApiResponse;
import com.kanta.kanban.common.PageResponse;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import com.kanta.kanban.infrastructure.security.UserAccess;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@UserAccess
@RestController
@RequestMapping
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/boards/{boardId}/cards")
    public ApiResponse<PageResponse<CardResponse>> getCards(
        @PathVariable UUID boardId,
        @RequestParam(required = false) CardStatus status,
        @RequestParam(required = false) UUID assigneeMemberId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(cardService.getCards(boardId, status, assigneeMemberId, page, size));
    }

    @PostMapping("/boards/{boardId}/cards")
    public ResponseEntity<ApiResponse<CreateCardResponse>> create(
        @PathVariable UUID boardId,
        @Valid @RequestBody CreateCardRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(cardService.create(boardId, request)));
    }

    @PatchMapping("/cards/{cardId}")
    public ApiResponse<CardResponse> update(
        @PathVariable UUID cardId,
        @RequestBody UpdateCardRequest request
    ) {
        return ApiResponse.ok(cardService.update(cardId, request));
    }

    @PatchMapping("/cards/{cardId}/status")
    public ApiResponse<ChangeCardStatusResponse> changeStatus(
        @PathVariable UUID cardId,
        @Valid @RequestBody ChangeCardStatusRequest request
    ) {
        return ApiResponse.ok(cardService.changeStatus(cardId, request));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> delete(@PathVariable UUID cardId) {
        cardService.delete(cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/{cardId}/move-logs")
    public ApiResponse<CardMoveLogsResponse> getMoveLogs(@PathVariable UUID cardId) {
        return ApiResponse.ok(cardService.getMoveLogs(cardId));
    }
}
