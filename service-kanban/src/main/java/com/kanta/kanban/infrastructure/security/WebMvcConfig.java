package com.kanta.kanban.infrastructure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final UserAccessInterceptor userAccessInterceptor;

    public WebMvcConfig(UserAccessInterceptor userAccessInterceptor) {
        this.userAccessInterceptor = userAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAccessInterceptor);
    }
}
