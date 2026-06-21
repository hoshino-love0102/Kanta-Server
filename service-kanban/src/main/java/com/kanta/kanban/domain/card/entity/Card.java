package com.kanta.kanban.domain.card.entity;

import com.kanta.kanban.domain.board.entity.Board;
import com.kanta.kanban.domain.card.enumeration.CardSource;
import com.kanta.kanban.domain.card.enumeration.CardStatus;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CardSource source;

    private UUID assigneeMemberId;
    private LocalDate dueDate;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Card() {
    }

    public Card(Board board, String title, CardStatus status, CardSource source, UUID assigneeMemberId, LocalDate dueDate) {
        this.board = board;
        this.title = title;
        this.status = status;
        this.source = source;
        this.assigneeMemberId = assigneeMemberId;
        this.dueDate = dueDate;
    }

    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public CardSource getSource() {
        return source;
    }

    public UUID getAssigneeMemberId() {
        return assigneeMemberId;
    }

    public void setAssigneeMemberId(UUID assigneeMemberId) {
        this.assigneeMemberId = assigneeMemberId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
