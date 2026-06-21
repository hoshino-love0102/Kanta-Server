package com.kanta.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient authServiceWebClient(@Value("${kanta.gateway.auth-service-url}") String authServiceUrl) {
        return WebClient.builder().baseUrl(authServiceUrl).build();
    }
}
