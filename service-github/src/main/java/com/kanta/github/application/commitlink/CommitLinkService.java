package com.kanta.github.application.commitlink;

import com.kanta.github.application.outbox.OutboxEventWriter;
import com.kanta.github.common.BadRequestException;
import com.kanta.github.common.NotFoundException;
import com.kanta.github.domain.commitlink.entity.CommitCardLink;
import com.kanta.github.domain.commitlink.enumeration.MatchStatus;
import com.kanta.github.domain.commitlink.repository.CommitCardLinkRepository;
import com.kanta.github.domain.kanban.KanbanCardClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommitLinkService {
    private final CommitCardLinkRepository commitCardLinkRepository;
    private final KanbanCardClient kanbanCardClient;
    private final OutboxEventWriter outboxEventWriter;

    public CommitLinkService(
        CommitCardLinkRepository commitCardLinkRepository,
        KanbanCardClient kanbanCardClient,
        OutboxEventWriter outboxEventWriter
    ) {
        this.commitCardLinkRepository = commitCardLinkRepository;
        this.kanbanCardClient = kanbanCardClient;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional(readOnly = true)
    public List<CommitCardLink> getPending() {
        return commitCardLinkRepository.findByMatchStatusOrderByCreatedAtAsc(MatchStatus.PENDING_CONFIRMATION);
    }

    @Transactional
    public CommitCardLink confirm(UUID id) {
        var link = getPendingOrThrow(id);
        link.confirm();
        appendCommitMatched(link);
        return link;
    }

    @Transactional
    public CommitCardLink reject(UUID id) {
        var link = getPendingOrThrow(id);
        var newCardId = kanbanCardClient.createCard(link.getBoardId(), link.getCommitMessage(), null, null);
        link.reject(newCardId);
        appendCommitMatched(link);
        return link;
    }

    private CommitCardLink getPendingOrThrow(UUID id) {
        var link = commitCardLinkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("커밋-카드 연결 후보를 찾을 수 없습니다.", "COMMIT_LINK_NOT_FOUND"));
        if (link.getMatchStatus() != MatchStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException("이미 처리된 후보입니다.", "COMMIT_LINK_ALREADY_PROCESSED");
        }
        return link;
    }

    private void appendCommitMatched(CommitCardLink link) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("commitCardLinkId", link.getId());
        payload.put("commitSha", link.getCommitSha());
        payload.put("cardId", link.getCardId());
        payload.put("newCardId", link.getNewCardId());
        payload.put("matchStatus", link.getMatchStatus().name());
        payload.put("matchedAt", Instant.now());
        outboxEventWriter.append("COMMIT_CARD_LINK", link.getId(), "commit.matched", payload);
    }
}
