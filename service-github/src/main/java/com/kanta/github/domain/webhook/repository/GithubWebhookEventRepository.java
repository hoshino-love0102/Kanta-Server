package com.kanta.github.domain.webhook.repository;

import com.kanta.github.domain.webhook.entity.GithubWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubWebhookEventRepository extends JpaRepository<GithubWebhookEvent, UUID> {
    Optional<GithubWebhookEvent> findByDeliveryId(String deliveryId);
}
