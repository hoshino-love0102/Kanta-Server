package com.kanta.github.infrastructure.security;

import com.kanta.github.infrastructure.ratelimit.WebhookRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final UserAccessInterceptor userAccessInterceptor;
    private final WebhookRateLimitInterceptor webhookRateLimitInterceptor;

    public WebMvcConfig(UserAccessInterceptor userAccessInterceptor, WebhookRateLimitInterceptor webhookRateLimitInterceptor) {
        this.userAccessInterceptor = userAccessInterceptor;
        this.webhookRateLimitInterceptor = webhookRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAccessInterceptor);
        registry.addInterceptor(webhookRateLimitInterceptor).addPathPatterns("/github/webhooks");
    }
}
