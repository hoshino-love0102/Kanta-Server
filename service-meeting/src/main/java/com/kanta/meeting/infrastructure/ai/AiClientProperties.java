package com.kanta.meeting.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kanta.ai")
public record AiClientProperties(String baseUrl, String timezone) {
    public AiClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8000";
        }
        if (timezone == null || timezone.isBlank()) {
            timezone = "Asia/Seoul";
        }
    }
}
