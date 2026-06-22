package com.kanta.meeting.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanta.meeting.common.ApiResponse;
import com.kanta.meeting.common.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PassportFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    public PassportFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws IOException {
        try {
            var header = request.getHeader("X-User-Passport");
            if (header != null && !header.isBlank()) {
                PassportHolder.set(parsePassport(header));
            }
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            response.setStatus(exception.getStatus().value());
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(
                response.getWriter(),
                new ApiResponse<Void>(
                    exception.getStatus().value(),
                    exception.getMessage(),
                    null,
                    exception.getErrorCode()
                )
            );
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException(exception);
        } finally {
            PassportHolder.clear();
        }
    }

    private Passport parsePassport(String header) {
        var json = header;
        try {
            json = new String(Base64.getDecoder().decode(header), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // Gateway implementations may pass either Base64 JSON or raw JSON.
        }

        try {
            return objectMapper.readValue(json, Passport.class);
        } catch (Exception exception) {
            throw new UnauthorizedException("Passport 형식이 올바르지 않습니다.");
        }
    }
}
