package com.kanta.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PassportExchangeGatewayFilterFactory
    extends AbstractGatewayFilterFactory<PassportExchangeGatewayFilterFactory.Config> {

    private final WebClient authServiceWebClient;
    private final String internalGatewaySecret;

    public PassportExchangeGatewayFilterFactory(
        WebClient authServiceWebClient,
        @Value("${kanta.gateway.internal-secret}") String internalGatewaySecret
    ) {
        super(Config.class);
        this.authServiceWebClient = authServiceWebClient;
        this.internalGatewaySecret = internalGatewaySecret;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return unauthorized(exchange.getResponse(), "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            }

            return authServiceWebClient.post()
                .uri("/passport")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-Internal-Gateway-Secret", internalGatewaySecret)
                .retrieve()
                .bodyToMono(PassportApiResponse.class)
                .flatMap(response -> {
                    var request = exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.remove(HttpHeaders.AUTHORIZATION);
                            headers.set("X-User-Passport", response.data().passport());
                        })
                        .build();
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .onErrorResume(error -> unauthorized(exchange.getResponse(), "INVALID_TOKEN", "유효하지 않거나 만료된 토큰입니다."));
        };
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String code, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var body = String.format(
            "{\"status\":401,\"message\":\"%s\",\"data\":null,\"code\":\"%s\"}", message, code
        );
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
    }

    public record PassportApiResponse(int status, String message, PassportData data, String code) {
    }

    public record PassportData(String passport) {
    }
}
