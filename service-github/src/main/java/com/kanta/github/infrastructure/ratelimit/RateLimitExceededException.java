package com.kanta.github.infrastructure.ratelimit;

import com.kanta.github.common.DomainException;
import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends DomainException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", "RATE_LIMIT_EXCEEDED");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
