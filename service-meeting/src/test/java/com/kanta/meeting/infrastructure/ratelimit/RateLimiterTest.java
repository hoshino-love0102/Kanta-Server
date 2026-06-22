package com.kanta.meeting.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void 분당_한도_이내의_요청은_허용된다() {
        var rateLimiter = new RateLimiter(3, 100);

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("1.2.3.4").allowed()).isTrue();
        }
    }

    @Test
    void 분당_한도를_초과하면_거부되고_재시도_시간이_포함된다() {
        var clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"));
        var rateLimiter = new RateLimiter(2, 100, clock);

        rateLimiter.tryAcquire("1.2.3.4");
        rateLimiter.tryAcquire("1.2.3.4");
        var result = rateLimiter.tryAcquire("1.2.3.4");

        assertThat(result.allowed()).isFalse();
        assertThat(result.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void 분당_윈도우가_지나면_요청이_다시_허용된다() {
        var clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"));
        var rateLimiter = new RateLimiter(1, 100, clock);

        assertThat(rateLimiter.tryAcquire("1.2.3.4").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("1.2.3.4").allowed()).isFalse();

        clock.advanceSeconds(61);

        assertThat(rateLimiter.tryAcquire("1.2.3.4").allowed()).isTrue();
    }

    @Test
    void 일일_한도를_초과하면_거부된다() {
        var clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"));
        var rateLimiter = new RateLimiter(1000, 2, clock);

        rateLimiter.tryAcquire("1.2.3.4");
        clock.advanceSeconds(61);
        rateLimiter.tryAcquire("1.2.3.4");
        clock.advanceSeconds(61);
        var result = rateLimiter.tryAcquire("1.2.3.4");

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void 서로_다른_키는_독립적으로_제한된다() {
        var rateLimiter = new RateLimiter(1, 100);

        assertThat(rateLimiter.tryAcquire("1.2.3.4").allowed()).isTrue();
        assertThat(rateLimiter.tryAcquire("5.6.7.8").allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

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
    }
}
