package com.kanta.github.infrastructure.common;

import java.net.http.HttpClient;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class RestClientHttp1Config {

    @Bean
    public RestClientCustomizer http1RestClientCustomizer() {
        var httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }
}
