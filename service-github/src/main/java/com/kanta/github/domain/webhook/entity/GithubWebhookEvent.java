package com.kanta.github.domain.webhook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_webhook_event")
public class GithubWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String deliveryId;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    protected GithubWebhookEvent() {
    }

    public GithubWebhookEvent(String deliveryId, String payloadHash, String eventType) {
        this.deliveryId = deliveryId;
        this.payloadHash = payloadHash;
        this.eventType = eventType;
    }

    public UUID getId() {
        return id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }
}
