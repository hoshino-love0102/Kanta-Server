package com.kanta.meeting.infrastructure.ratelimit;

import com.kanta.meeting.infrastructure.security.PassportHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MeetingNoteRateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter;

    public MeetingNoteRateLimitInterceptor(
        @Value("${kanta.rate-limit.meeting-notes.per-minute}") int perMinuteLimit,
        @Value("${kanta.rate-limit.meeting-notes.per-day}") int perDayLimit
    ) {
        this.rateLimiter = new RateLimiter(perMinuteLimit, perDayLimit);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        var result = rateLimiter.tryAcquire(PassportHolder.current().requireUserId());
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
        return true;
    }

    @Scheduled(fixedRate = 600_000)
    public void evictStaleEntries() {
        rateLimiter.evictStaleEntries();
    }
}
