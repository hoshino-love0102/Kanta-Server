package com.kanta.kanban.domain.card.entity;

import com.kanta.kanban.domain.card.enumeration.CardStatus;
import com.kanta.kanban.domain.card.enumeration.MovedByType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_move_log")
public class CardMoveLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    private UUID movedByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MovedByType movedByType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardStatus toStatus;

    @Column(nullable = false)
    private Instant movedAt = Instant.now();

    protected CardMoveLog() {
    }

    public CardMoveLog(Card card, UUID movedByMemberId, MovedByType movedByType, CardStatus fromStatus, CardStatus toStatus) {
        this.card = card;
        this.movedByMemberId = movedByMemberId;
        this.movedByType = movedByType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMovedByMemberId() {
        return movedByMemberId;
    }

    public CardStatus getFromStatus() {
        return fromStatus;
    }

    public CardStatus getToStatus() {
        return toStatus;
    }

    public Instant getMovedAt() {
        return movedAt;
    }
}
