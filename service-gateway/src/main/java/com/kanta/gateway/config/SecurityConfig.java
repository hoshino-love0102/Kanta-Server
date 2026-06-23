package com.kanta.gateway.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${kanta.cors.allowed-origin-patterns}") String allowedOriginPatterns
    ) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOriginPatterns.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        // service-websocket-hub already sets its own CORS/origin headers for SockJS
        // handshake endpoints (kanta.websocket.allowed-origin-patterns). Applying the
        // gateway's CORS on top would duplicate Access-Control-Allow-Origin, which
        // browsers reject outright, so /ws/** is left untouched here.
        return exchange -> exchange.getRequest().getPath().value().startsWith("/api/ws")
            ? null
            : source.getCorsConfiguration(exchange);
    }
}
