package com.kanta.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.gateway.ratelimit.RateLimitAction;
import com.kanta.gateway.ratelimit.RateLimitResult;
import com.kanta.gateway.ratelimit.WorkspaceRateLimiter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class WorkspaceRateLimitGatewayFilterFactory
    extends AbstractGatewayFilterFactory<WorkspaceRateLimitGatewayFilterFactory.Config> {
    private static final String DELAYED_HEADER = "X-Kanta-Rate-Limit-Exceeded";
    private static final String RETRY_AFTER_HEADER = "X-Kanta-Rate-Limit-Retry-After";

    private final ObjectMapper objectMapper;
    private final WorkspaceRateLimiter rateLimiter;
    private final WebClient kanbanServiceWebClient;
    private final WebClient workspaceServiceWebClient;
    private final int meetingPerMinuteLimit;
    private final int meetingPerDayLimit;
    private final int githubWebhookPerMinuteLimit;

    public WorkspaceRateLimitGatewayFilterFactory(
        ObjectMapper objectMapper,
        WebClient.Builder webClientBuilder,
        @Value("${kanta.gateway.kanban-service-url}") String kanbanServiceUrl,
        @Value("${kanta.gateway.workspace-service-url}") String workspaceServiceUrl,
        @Value("${kanta.rate-limit.meeting-notes.per-minute}") int meetingPerMinuteLimit,
        @Value("${kanta.rate-limit.meeting-notes.per-day}") int meetingPerDayLimit,
        @Value("${kanta.rate-limit.github-webhook.per-minute}") int githubWebhookPerMinuteLimit
    ) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.rateLimiter = new WorkspaceRateLimiter();
        this.kanbanServiceWebClient = webClientBuilder.baseUrl(kanbanServiceUrl).build();
        this.workspaceServiceWebClient = webClientBuilder.baseUrl(workspaceServiceUrl).build();
        this.meetingPerMinuteLimit = meetingPerMinuteLimit;
        this.meetingPerDayLimit = meetingPerDayLimit;
        this.githubWebhookPerMinuteLimit = githubWebhookPerMinuteLimit;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("policy");
    }

    @Override
    public GatewayFilter apply(Config config) {
        var policy = RateLimitPolicy.from(config.policy(), meetingPerMinuteLimit, meetingPerDayLimit, githubWebhookPerMinuteLimit);
        return (exchange, chain) -> {
            if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
                return chain.filter(exchange);
            }

            return readBody(exchange.getRequest())
                .flatMap(body -> resolveWorkspaceId(policy, exchange.getRequest(), body)
                    .flatMap(workspaceId -> applyRateLimit(policy, workspaceId, body, exchange.getResponse()))
                    .defaultIfEmpty(RateLimitResult.permit())
                    .flatMap(result -> {
                        var requestBuilder = decorateRequest(exchange.getRequest(), body).mutate();
                        if (!result.allowed() && policy.action() == RateLimitAction.DEFER) {
                            requestBuilder.header(DELAYED_HEADER, "true");
                            requestBuilder.header(RETRY_AFTER_HEADER, String.valueOf(result.retryAfterSeconds()));
                        }
                        if (!result.allowed() && policy.action() == RateLimitAction.REJECT) {
                            return tooManyRequests(exchange.getResponse(), result.retryAfterSeconds());
                        }
                        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                    }));
        };
    }

    private Mono<byte[]> readBody(ServerHttpRequest request) {
        return DataBufferUtils.join(request.getBody())
            .map(dataBuffer -> {
                var bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            })
            .defaultIfEmpty(new byte[0]);
    }

    private Mono<String> resolveWorkspaceId(RateLimitPolicy policy, ServerHttpRequest request, byte[] body) {
        if (policy.action() == RateLimitAction.REJECT) {
            return resolveMeetingWorkspaceId(request, body);
        }
        return resolveGithubWebhookWorkspaceId(body);
    }

    private Mono<String> resolveMeetingWorkspaceId(ServerHttpRequest request, byte[] body) {
        var boardId = parseBody(body).path("boardId").asText(null);
        if (boardId == null || boardId.isBlank()) {
            return Mono.empty();
        }

        var passport = request.getHeaders().getFirst("X-User-Passport");
        return kanbanServiceWebClient.get()
            .uri("/boards/{boardId}", boardId)
            .headers(headers -> {
                if (passport != null && !passport.isBlank()) {
                    headers.set("X-User-Passport", passport);
                }
            })
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> response.path("data").path("workspaceId").asText(null))
            .filter(workspaceId -> workspaceId != null && !workspaceId.isBlank())
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<String> resolveGithubWebhookWorkspaceId(byte[] body) {
        var githubRepo = parseBody(body).path("repository").path("full_name").asText(null);
        if (githubRepo == null || githubRepo.isBlank()) {
            return Mono.empty();
        }

        return workspaceServiceWebClient.get()
            .uri(uriBuilder -> uriBuilder.path("/internal/repo-mappings/lookup")
                .queryParam("githubRepo", githubRepo)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> response.path("data").path("workspaceId").asText(null))
            .filter(workspaceId -> workspaceId != null && !workspaceId.isBlank())
            .onErrorResume(error -> Mono.empty());
    }

    private Mono<RateLimitResult> applyRateLimit(RateLimitPolicy policy, String workspaceId, byte[] body, ServerHttpResponse response) {
        var result = rateLimiter.tryAcquire(policy.name() + ":" + workspaceId, policy.perMinuteLimit(), policy.perDayLimit());
        if (!result.allowed()) {
            response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
        }
        return Mono.just(result);
    }

    private JsonNode parseBody(byte[] body) {
        try {
            return objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return objectMapper.missingNode();
        }
    }

    private ServerHttpRequest decorateRequest(ServerHttpRequest request, byte[] body) {
        return new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<org.springframework.core.io.buffer.DataBuffer> getBody() {
                return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body));
            }
        };
    }

    private Mono<Void> tooManyRequests(ServerHttpResponse response, long retryAfterSeconds) {
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        var body = "{\"status\":429,\"message\":\"Rate limit 정책을 초과했습니다.\",\"data\":null,\"code\":\"RATE_LIMIT_EXCEEDED\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    @Scheduled(fixedRate = 600_000)
    public void evictStaleEntries() {
        rateLimiter.evictStaleEntries();
    }

    public static class Config {
        private String policy;

        public String policy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }
    }

    private record RateLimitPolicy(String name, int perMinuteLimit, int perDayLimit, RateLimitAction action) {
        private static RateLimitPolicy from(String policy, int meetingPerMinute, int meetingPerDay, int githubWebhookPerMinute) {
            return switch (policy) {
                case "meeting-notes" -> new RateLimitPolicy(policy, meetingPerMinute, meetingPerDay, RateLimitAction.REJECT);
                case "github-webhook" -> new RateLimitPolicy(policy, githubWebhookPerMinute, 0, RateLimitAction.DEFER);
                default -> throw new IllegalArgumentException("지원하지 않는 rate limit policy입니다: " + policy);
            };
        }
    }
}
