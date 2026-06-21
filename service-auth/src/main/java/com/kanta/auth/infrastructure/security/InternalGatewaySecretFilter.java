package com.kanta.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.auth.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalGatewaySecretFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final String expectedSecret;

    public InternalGatewaySecretFilter(
        ObjectMapper objectMapper,
        @Value("${kanta.internal.gateway-secret}") String expectedSecret
    ) {
        this.objectMapper = objectMapper;
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws IOException, jakarta.servlet.ServletException {
        if (!"/passport".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        var secretHeader = request.getHeader("X-Internal-Gateway-Secret");
        if (secretHeader == null || !secretHeader.equals(expectedSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(
                response.getWriter(),
                new ApiResponse<Void>(401, "게이트웨이 전용 엔드포인트입니다.", null, "INVALID_TOKEN")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
