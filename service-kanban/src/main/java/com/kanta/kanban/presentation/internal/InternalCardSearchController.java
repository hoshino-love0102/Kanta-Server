package com.kanta.kanban.presentation.internal;

import com.kanta.kanban.application.card.CardService;
import com.kanta.kanban.common.ApiResponse;
import com.kanta.kanban.presentation.card.CardResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/boards/{boardId}/cards")
public class InternalCardSearchController {
    private final CardService cardService;

    public InternalCardSearchController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/search")
    public ApiResponse<InternalCardSearchResponse> search(
        @PathVariable UUID boardId,
        @RequestParam String titleContains
    ) {
        return ApiResponse.ok(new InternalCardSearchResponse(cardService.searchByTitle(boardId, titleContains)));
    }
}
