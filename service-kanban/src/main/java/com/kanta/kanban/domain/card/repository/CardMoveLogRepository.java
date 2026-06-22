package com.kanta.kanban.domain.card.repository;

import com.kanta.kanban.domain.card.entity.CardMoveLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardMoveLogRepository extends JpaRepository<CardMoveLog, UUID> {
    Page<CardMoveLog> findByCard_IdOrderByMovedAtAsc(UUID cardId, Pageable pageable);
}
