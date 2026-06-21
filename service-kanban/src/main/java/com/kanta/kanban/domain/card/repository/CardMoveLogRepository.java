package com.kanta.kanban.domain.card.repository;

import com.kanta.kanban.domain.card.entity.CardMoveLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardMoveLogRepository extends JpaRepository<CardMoveLog, UUID> {
    List<CardMoveLog> findByCard_IdOrderByMovedAtAsc(UUID cardId);
}
