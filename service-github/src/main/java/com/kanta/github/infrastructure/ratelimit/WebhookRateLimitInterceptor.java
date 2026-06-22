package com.kanta.github.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WebhookRateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter;

    public WebhookRateLimitInterceptor(
        @Value("${kanta.rate-limit.webhook.per-minute}") int perMinuteLimit,
        @Value("${kanta.rate-limit.webhook.per-day}") int perDayLimit
    ) {
        this.rateLimiter = new RateLimiter(perMinuteLimit, perDayLimit);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var result = rateLimiter.tryAcquire(clientIp(request));
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
        return true;
    }

    @Scheduled(fixedRate = 600_000)
    public void evictStaleEntries() {
        rateLimiter.evictStaleEntries();
    }

    private String clientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
