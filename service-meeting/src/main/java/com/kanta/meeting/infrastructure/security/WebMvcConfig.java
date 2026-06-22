package com.kanta.meeting.infrastructure.security;

import com.kanta.meeting.infrastructure.ratelimit.MeetingNoteRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final UserAccessInterceptor userAccessInterceptor;
    private final MeetingNoteRateLimitInterceptor meetingNoteRateLimitInterceptor;

    public WebMvcConfig(
        UserAccessInterceptor userAccessInterceptor,
        MeetingNoteRateLimitInterceptor meetingNoteRateLimitInterceptor
    ) {
        this.userAccessInterceptor = userAccessInterceptor;
        this.meetingNoteRateLimitInterceptor = meetingNoteRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAccessInterceptor);
        registry.addInterceptor(meetingNoteRateLimitInterceptor).addPathPatterns("/meeting-notes");
    }
}
