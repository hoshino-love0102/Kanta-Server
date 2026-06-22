package com.kanta.github.presentation.webhook;

public record WebhookReceivedResponse(boolean received, String deliveryId) {
}
