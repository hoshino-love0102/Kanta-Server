package com.kanta.kanban.application.card;

import com.kanta.kanban.application.board.BoardService;
import com.kanta.kanban.application.outbox.OutboxEventWriter;
import com.kanta.kanban.application.workspace.WorkspaceMemberCacheService;
import com.kanta.kanban.common.BadRequestException;
import com.kanta.kanban.common.NotFoundException;
import com.kanta.kanban.common.PageResponse;
import com.kanta.kanban.domain.card.WorkspaceMemberResolver;
import com.kanta.kanban.domain.card.entity.Card;
import com.kanta.kanban.domain.card.entity.CardMoveLog;
import com.kanta.kanban.domain.card.enumeration.CardSource;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
import com.kanta.kanban.domain.card.enumeration.MovedByType;
import com.kanta.kanban.domain.card.repository.CardMoveLogRepository;
import com.kanta.kanban.domain.card.repository.CardRepository;
import com.kanta.kanban.infrastructure.security.PassportHolder;
import com.kanta.kanban.presentation.card.CardMoveLogResponse;
import com.kanta.kanban.presentation.card.CardResponse;
import com.kanta.kanban.presentation.card.ChangeCardStatusRequest;
import com.kanta.kanban.presentation.card.ChangeCardStatusResponse;
import com.kanta.kanban.presentation.card.CreateCardRequest;
import com.kanta.kanban.presentation.card.CreateCardResponse;
import com.kanta.kanban.presentation.card.UpdateCardRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
    private final BoardService boardService;
    private final CardRepository cardRepository;
    private final CardMoveLogRepository cardMoveLogRepository;
    private final WorkspaceMemberResolver workspaceMemberResolver;
    private final WorkspaceMemberCacheService workspaceMemberCacheService;
    private final OutboxEventWriter outboxEventWriter;

    public CardService(
        BoardService boardService,
        CardRepository cardRepository,
        CardMoveLogRepository cardMoveLogRepository,
        WorkspaceMemberResolver workspaceMemberResolver,
        WorkspaceMemberCacheService workspaceMemberCacheService,
        OutboxEventWriter outboxEventWriter
    ) {
        this.boardService = boardService;
        this.cardRepository = cardRepository;
        this.cardMoveLogRepository = cardMoveLogRepository;
        this.workspaceMemberResolver = workspaceMemberResolver;
        this.workspaceMemberCacheService = workspaceMemberCacheService;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getCards(UUID boardId, CardStatus status, UUID assigneeMemberId, int page, int size) {
        boardService.findBoard(boardId);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var cards = cardRepository.findCards(boardId, status, assigneeMemberId, pageable);
        var assigneeMemberIds = cards.getContent().stream()
            .map(Card::getAssigneeMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        var displayNames = workspaceMemberCacheService.findDisplayNames(assigneeMemberIds);
        var cardResponses = cards.getContent().stream()
            .map(card -> CardResponse.from(
                card,
                card.getAssigneeMemberId() == null ? null : displayNames.get(card.getAssigneeMemberId())
            ))
            .toList();

        return new PageResponse<>(cardResponses, cards.getTotalElements(), cards.getTotalPages());
    }

    @Transactional
    public CreateCardResponse create(UUID boardId, CreateCardRequest request) {
        var board = boardService.findBoard(boardId);
        var card = new Card(
            board,
            request.title().trim(),
            CardStatus.TODO,
            CardSource.MANUAL,
            request.assigneeMemberId(),
            request.dueDate()
        );
        var savedCard = cardRepository.save(card);
        appendCardCreated(savedCard);
        return CreateCardResponse.from(savedCard);
    }

    @Transactional
    public CardResponse update(UUID cardId, UpdateCardRequest request) {
        var card = findCard(cardId);

        if (request.title() != null && !request.title().isBlank()) {
            card.setTitle(request.title().trim());
        }
        if (request.assigneeMemberId() != null) {
            card.setAssigneeMemberId(request.assigneeMemberId());
        }
        if (request.dueDate() != null) {
            card.setDueDate(request.dueDate());
        }
        card.setUpdatedAt(Instant.now());

        var displayName = workspaceMemberCacheService.findDisplayNames(
            card.getAssigneeMemberId() == null ? Set.of() : Set.of(card.getAssigneeMemberId())
        ).get(card.getAssigneeMemberId());
        return CardResponse.from(card, displayName);
    }

    @Transactional
    public ChangeCardStatusResponse changeStatus(UUID cardId, ChangeCardStatusRequest request) {
        var card = findCard(cardId);
        var fromStatus = card.getStatus();
        var toStatus = request.toStatus();

        if (fromStatus == toStatus) {
            throw new BadRequestException("이미 같은 상태입니다.", "INVALID_STATUS_TRANSITION");
        }

        var userId = PassportHolder.current().requireUserId();
        var workspaceId = card.getBoard().getWorkspaceId();
        var movedByMemberId = workspaceMemberResolver.resolve(workspaceId, userId);

        card.setStatus(toStatus);
        card.setUpdatedAt(Instant.now());

        cardMoveLogRepository.save(
            new CardMoveLog(card, movedByMemberId, MovedByType.USER, fromStatus, toStatus)
        );
        appendCardMoved(card, fromStatus, toStatus, movedByMemberId, MovedByType.USER);

        return new ChangeCardStatusResponse(card.getId(), card.getStatus(), card.getUpdatedAt());
    }

    @Transactional
    public void delete(UUID cardId) {
        var card = findCard(cardId);
        appendCardDeleted(card);
        cardRepository.delete(card);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> searchByTitle(UUID boardId, String titleContains) {
        boardService.findBoard(boardId);
        var cards = cardRepository.findByBoardIdAndTitleContains(boardId, titleContains, PageRequest.of(0, 10));
        var assigneeMemberIds = cards.stream()
            .map(Card::getAssigneeMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        var displayNames = workspaceMemberCacheService.findDisplayNames(assigneeMemberIds);
        return cards.stream()
            .map(card -> CardResponse.from(
                card,
                card.getAssigneeMemberId() == null ? null : displayNames.get(card.getAssigneeMemberId())
            ))
            .toList();
    }

    @Transactional
    public void moveBySystem(UUID cardId, CardStatus toStatus) {
        var card = findCard(cardId);
        var fromStatus = card.getStatus();
        if (fromStatus == toStatus) {
            return;
        }
        card.setStatus(toStatus);
        card.setUpdatedAt(Instant.now());
        cardMoveLogRepository.save(new CardMoveLog(card, null, MovedByType.SYSTEM, fromStatus, toStatus));
        appendCardMoved(card, fromStatus, toStatus, null, MovedByType.SYSTEM);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardMoveLogResponse> getMoveLogs(UUID cardId, int page, int size) {
        if (!cardRepository.existsById(cardId)) {
            throw new NotFoundException("카드를 찾을 수 없습니다.", "CARD_NOT_FOUND");
        }

        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var logs = cardMoveLogRepository.findByCard_IdOrderByMovedAtAsc(cardId, pageable)
            .map(CardMoveLogResponse::from);
        return new PageResponse<>(logs.getContent(), logs.getTotalElements(), logs.getTotalPages());
    }

    private Card findCard(UUID cardId) {
        return cardRepository.findById(cardId)
            .orElseThrow(() -> new NotFoundException("카드를 찾을 수 없습니다.", "CARD_NOT_FOUND"));
    }

    private void appendCardCreated(Card card) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("cardId", card.getId());
        payload.put("boardId", card.getBoard().getId());
        payload.put("workspaceId", card.getBoard().getWorkspaceId());
        payload.put("title", card.getTitle());
        payload.put("status", card.getStatus().name());
        payload.put("source", card.getSource().name());
        payload.put("assigneeMemberId", card.getAssigneeMemberId());
        payload.put("dueDate", card.getDueDate());
        payload.put("createdAt", card.getCreatedAt());
        outboxEventWriter.append("CARD", card.getId(), "card.created", payload);
    }

    private void appendCardMoved(
        Card card, CardStatus fromStatus, CardStatus toStatus, UUID movedByMemberId, MovedByType movedByType
    ) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("cardId", card.getId());
        payload.put("boardId", card.getBoard().getId());
        payload.put("workspaceId", card.getBoard().getWorkspaceId());
        payload.put("fromStatus", fromStatus.name());
        payload.put("toStatus", toStatus.name());
        payload.put("movedByMemberId", movedByMemberId);
        payload.put("movedByType", movedByType.name());
        payload.put("updatedAt", card.getUpdatedAt());
        outboxEventWriter.append("CARD", card.getId(), "card.moved", payload);
    }

    private void appendCardDeleted(Card card) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("cardId", card.getId());
        payload.put("boardId", card.getBoard().getId());
        payload.put("workspaceId", card.getBoard().getWorkspaceId());
        payload.put("deletedAt", Instant.now());
        outboxEventWriter.append("CARD", card.getId(), "card.deleted", payload);
    }
}
