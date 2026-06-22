package com.kanta.meeting.infrastructure.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
    private static final long MINUTE_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long DAY_WINDOW_MILLIS = Duration.ofDays(1).toMillis();

    private final Map<String, Window> minuteWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> dayWindows = new ConcurrentHashMap<>();
    private final int perMinuteLimit;
    private final int perDayLimit;
    private final Clock clock;

    public RateLimiter(int perMinuteLimit, int perDayLimit) {
        this(perMinuteLimit, perDayLimit, Clock.systemUTC());
    }

    public RateLimiter(int perMinuteLimit, int perDayLimit, Clock clock) {
        this.perMinuteLimit = perMinuteLimit;
        this.perDayLimit = perDayLimit;
        this.clock = clock;
    }

    public RateLimitResult tryAcquire(String key) {
        var now = clock.millis();

        var minuteCount = increment(minuteWindows, key, MINUTE_WINDOW_MILLIS, now);
        if (minuteCount > perMinuteLimit) {
            return RateLimitResult.exceeded(secondsUntilReset(minuteWindows.get(key), MINUTE_WINDOW_MILLIS, now));
        }

        var dayCount = increment(dayWindows, key, DAY_WINDOW_MILLIS, now);
        if (dayCount > perDayLimit) {
            return RateLimitResult.exceeded(secondsUntilReset(dayWindows.get(key), DAY_WINDOW_MILLIS, now));
        }

        return RateLimitResult.permit();
    }

    public void evictStaleEntries() {
        var now = clock.millis();
        minuteWindows.entrySet().removeIf(entry -> now - entry.getValue().start >= MINUTE_WINDOW_MILLIS * 2);
        dayWindows.entrySet().removeIf(entry -> now - entry.getValue().start >= DAY_WINDOW_MILLIS * 2);
    }

    private int increment(Map<String, Window> windows, String key, long windowMillis, long now) {
        var window = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.start >= windowMillis) {
                window.start = now;
                window.count.set(0);
            }
            return window.count.incrementAndGet();
        }
    }

    private long secondsUntilReset(Window window, long windowMillis, long now) {
        var elapsed = now - window.start;
        return Math.max(1, Duration.ofMillis(windowMillis - elapsed).toSeconds());
    }

    private static final class Window {
        private long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}
