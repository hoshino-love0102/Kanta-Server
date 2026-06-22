package com.kanta.github.domain.outbox.repository;

import com.kanta.github.domain.outbox.entity.OutboxEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByEventTypeAndPublishedAtIsNullOrderByOccurredAtAsc(String eventType, Pageable pageable);
}
