package com.kanta.github.presentation.webhook;

import com.kanta.github.application.webhook.WebhookIngestService;
import com.kanta.github.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/github")
public class GithubWebhookController {
    private final WebhookIngestService webhookIngestService;

    public GithubWebhookController(WebhookIngestService webhookIngestService) {
        this.webhookIngestService = webhookIngestService;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<ApiResponse<WebhookReceivedResponse>> receive(
        @RequestBody String rawBody,
        @RequestHeader("X-Hub-Signature-256") String signature,
        @RequestHeader("X-GitHub-Delivery") String deliveryId,
        @RequestHeader("X-GitHub-Event") String eventType
    ) {
        webhookIngestService.ingestPush(rawBody, signature, deliveryId, eventType);
        return ResponseEntity.status(202).body(ApiResponse.accepted(new WebhookReceivedResponse(true, deliveryId)));
    }
}
