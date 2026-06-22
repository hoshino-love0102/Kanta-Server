package com.kanta.gateway.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkspaceRateLimiter {
    private static final long DAY_WINDOW_MILLIS = Duration.ofDays(1).toMillis();

    private final Map<String, TokenBucket> minuteBuckets = new ConcurrentHashMap<>();
    private final Map<String, DayWindow> dayWindows = new ConcurrentHashMap<>();
    private final Clock clock;

    public WorkspaceRateLimiter() {
        this(Clock.systemUTC());
    }

    public WorkspaceRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public RateLimitResult tryAcquire(String key, int perMinuteLimit, int perDayLimit) {
        var now = clock.millis();
        var minuteResult = acquireMinuteToken(key, perMinuteLimit, now);
        if (!minuteResult.allowed()) {
            return minuteResult;
        }

        if (perDayLimit > 0) {
            var dayResult = acquireDayQuota(key, perDayLimit, now);
            if (!dayResult.allowed()) {
                return dayResult;
            }
        }

        return RateLimitResult.permit();
    }

    public void evictStaleEntries() {
        var now = clock.millis();
        minuteBuckets.entrySet().removeIf(entry -> now - entry.getValue().lastRefillMillis >= Duration.ofMinutes(10).toMillis());
        dayWindows.entrySet().removeIf(entry -> now - entry.getValue().startMillis >= DAY_WINDOW_MILLIS * 2);
    }

    private RateLimitResult acquireMinuteToken(String key, int perMinuteLimit, long now) {
        var bucket = minuteBuckets.computeIfAbsent(key, ignored -> new TokenBucket(perMinuteLimit, now));
        synchronized (bucket) {
            refill(bucket, perMinuteLimit, now);
            if (bucket.tokens < 1.0d) {
                return RateLimitResult.exceeded(secondsUntilNextToken(bucket, perMinuteLimit, now));
            }
            bucket.tokens -= 1.0d;
            return RateLimitResult.permit();
        }
    }

    private void refill(TokenBucket bucket, int perMinuteLimit, long now) {
        var elapsedMillis = now - bucket.lastRefillMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        var refillTokens = elapsedMillis * (perMinuteLimit / 60_000.0d);
        bucket.tokens = Math.min(perMinuteLimit, bucket.tokens + refillTokens);
        bucket.lastRefillMillis = now;
    }

    private long secondsUntilNextToken(TokenBucket bucket, int perMinuteLimit, long now) {
        var missingTokens = 1.0d - bucket.tokens;
        var millis = Math.ceil(missingTokens * 60_000.0d / perMinuteLimit);
        return Math.max(1, (long) Math.ceil(millis / 1000.0d));
    }

    private RateLimitResult acquireDayQuota(String key, int perDayLimit, long now) {
        var window = dayWindows.computeIfAbsent(key, ignored -> new DayWindow(now));
        synchronized (window) {
            if (now - window.startMillis >= DAY_WINDOW_MILLIS) {
                window.startMillis = now;
                window.count = 0;
            }
            window.count += 1;
            if (window.count > perDayLimit) {
                return RateLimitResult.exceeded(secondsUntilDayReset(window, now));
            }
            return RateLimitResult.permit();
        }
    }

    private long secondsUntilDayReset(DayWindow window, long now) {
        var elapsedMillis = now - window.startMillis;
        return Math.max(1, Duration.ofMillis(DAY_WINDOW_MILLIS - elapsedMillis).toSeconds());
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillMillis;

        private TokenBucket(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillMillis = now;
        }
    }

    private static final class DayWindow {
        private long startMillis;
        private int count;

        private DayWindow(long startMillis) {
            this.startMillis = startMillis;
        }
    }
}
