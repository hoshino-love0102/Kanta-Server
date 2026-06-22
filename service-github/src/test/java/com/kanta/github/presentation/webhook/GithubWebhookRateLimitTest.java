package com.kanta.github.presentation.webhook;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kanta.github.application.webhook.WebhookIngestService;
import com.kanta.github.common.GlobalExceptionHandler;
import com.kanta.github.infrastructure.ratelimit.WebhookRateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GithubWebhookRateLimitTest {
    private static final int PER_MINUTE_LIMIT = 2;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var webhookIngestService = mock(WebhookIngestService.class);
        var controller = new GithubWebhookController(webhookIngestService);
        var rateLimitInterceptor = new WebhookRateLimitInterceptor(PER_MINUTE_LIMIT, 5000);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(rateLimitInterceptor)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void 분당_한도_내의_요청은_정상_처리된다() throws Exception {
        for (int i = 0; i < PER_MINUTE_LIMIT; i++) {
            sendWebhook().andExpect(status().isAccepted());
        }
    }

    @Test
    void 분당_한도를_초과하면_429와_Retry_After_헤더가_반환된다() throws Exception {
        for (int i = 0; i < PER_MINUTE_LIMIT; i++) {
            sendWebhook().andExpect(status().isAccepted());
        }

        sendWebhook()
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.status").value(429));
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook() throws Exception {
        return mockMvc.perform(
            post("/github/webhooks")
                .header("X-Hub-Signature-256", "sha256=test")
                .header("X-GitHub-Delivery", "delivery-id")
                .header("X-GitHub-Event", "push")
                .contentType("application/json")
                .content("{}")
        );
    }
}
