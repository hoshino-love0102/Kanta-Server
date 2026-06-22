package com.kanta.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class WorkspaceRateLimitGatewayFilterFactoryTest {
    private static final String WORKSPACE_ID = UUID.randomUUID().toString();

    @Test
    void meeting_notes는_workspace_한도_초과시_429를_반환한다() {
        var factory = factoryWithWorkspaceLookup(1, 100, 60);
        var filter = factory.apply(config("meeting-notes"));
        var chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        filter.filter(meetingExchange(), chain).block();
        var secondExchange = meetingExchange();
        filter.filter(secondExchange, chain).block();

        assertThat(chainCalls).hasValue(1);
        assertThat(secondExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(secondExchange.getResponse().getHeaders().getFirst("Retry-After")).isNotBlank();
    }

    @Test
    void github_webhook은_workspace_한도_초과시_거부하지_않고_지연_헤더를_전달한다() {
        var factory = factoryWithWorkspaceLookup(10, 100, 1);
        var filter = factory.apply(config("github-webhook"));
        var latestRequest = new AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest>();
        GatewayFilterChain chain = exchange -> {
            latestRequest.set(exchange.getRequest());
            return Mono.empty();
        };

        filter.filter(githubWebhookExchange(), chain).block();
        assertThat(latestRequest.get().getHeaders().containsKey("X-Kanta-Rate-Limit-Exceeded")).isFalse();

        filter.filter(githubWebhookExchange(), chain).block();

        assertThat(latestRequest.get().getHeaders().getFirst("X-Kanta-Rate-Limit-Exceeded")).isEqualTo("true");
        assertThat(latestRequest.get().getHeaders().getFirst("X-Kanta-Rate-Limit-Retry-After")).isNotBlank();
    }

    private WorkspaceRateLimitGatewayFilterFactory factoryWithWorkspaceLookup(
        int meetingPerMinuteLimit,
        int meetingPerDayLimit,
        int githubWebhookPerMinuteLimit
    ) {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body("""
                {"status":200,"message":"OK","data":{"workspaceId":"%s"},"code":null}
                """.formatted(WORKSPACE_ID))
            .build());

        return new WorkspaceRateLimitGatewayFilterFactory(
            new ObjectMapper(),
            WebClient.builder().exchangeFunction(exchangeFunction),
            "http://kanban",
            "http://workspace",
            meetingPerMinuteLimit,
            meetingPerDayLimit,
            githubWebhookPerMinuteLimit
        );
    }

    private WorkspaceRateLimitGatewayFilterFactory.Config config(String policy) {
        var config = new WorkspaceRateLimitGatewayFilterFactory.Config();
        config.setPolicy(policy);
        return config;
    }

    private MockServerWebExchange meetingExchange() {
        var request = MockServerHttpRequest.post("/api/meeting-notes")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-User-Passport", "passport")
            .body("""
                {"boardId":"%s","rawText":"회의록"}
                """.formatted(UUID.randomUUID()));
        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange githubWebhookExchange() {
        var request = MockServerHttpRequest.post("/api/github/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"repository":{"full_name":"kanta/server"},"commits":[{"id":"abc","message":"work"}]}
                """);
        return MockServerWebExchange.from(request);
    }
}
