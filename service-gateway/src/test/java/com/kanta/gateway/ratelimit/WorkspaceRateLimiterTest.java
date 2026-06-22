package com.kanta.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WorkspaceRateLimiterTest {
    @Test
    void token_bucket은_시간이_지나면_분당_한도를_다시_채운다() {
        var clock = new MutableClock();
        var rateLimiter = new WorkspaceRateLimiter(clock);

        assertThat(rateLimiter.tryAcquire("workspace-1", 1, 0).allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("workspace-1", 1, 0).allowed()).isFalse();

        clock.advanceSeconds(60);

        assertThat(rateLimiter.tryAcquire("workspace-1", 1, 0).allowed()).isTrue();
    }

    @Test
    void 일일_한도는_하루_윈도우가_지나면_초기화된다() {
        var clock = new MutableClock();
        var rateLimiter = new WorkspaceRateLimiter(clock);

        assertThat(rateLimiter.tryAcquire("workspace-1", 100, 1).allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("workspace-1", 100, 1).allowed()).isFalse();

        clock.advanceSeconds(86_400);

        assertThat(rateLimiter.tryAcquire("workspace-1", 100, 1).allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-06-22T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
