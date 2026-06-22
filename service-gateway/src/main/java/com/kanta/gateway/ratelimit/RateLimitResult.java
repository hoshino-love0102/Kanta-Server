package com.kanta.gateway.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {
    public static RateLimitResult permit() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult exceeded(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
