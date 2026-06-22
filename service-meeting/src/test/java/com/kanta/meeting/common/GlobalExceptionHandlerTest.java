package com.kanta.meeting.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kanta.meeting.infrastructure.ratelimit.RateLimitExceededException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(handler)
            .build();
    }

    @Test
    void 도메인_예외는_상태코드와_에러코드를_그대로_응답한다() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("MEETING_NOTE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("회의록을 찾을 수 없습니다."));
    }

    @Test
    void rate_limit_초과시_429와_Retry_After_헤더를_응답한다() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "5"))
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void 검증_실패시_400과_VALIDATION_ERROR를_응답한다() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType("application/json")
                .content("{\"name\": \"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 깨진_JSON_요청은_400과_VALIDATION_ERROR를_응답한다() throws Exception {
        mockMvc.perform(post("/test/validate")
                .contentType("application/json")
                .content("not-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 경로변수_타입_불일치는_400과_VALIDATION_ERROR를_응답한다() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 처리되지_않은_예외는_500과_INTERNAL_SERVER_ERROR를_응답한다() throws Exception {
        mockMvc.perform(get("/test/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void 존재하지_않는_리소스_경로는_404와_NOT_FOUND를_응답한다() {
        var response = handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "/no/such/path"));

        org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void notFound() {
            throw new NotFoundException("회의록을 찾을 수 없습니다.", "MEETING_NOTE_NOT_FOUND");
        }

        @GetMapping("/test/rate-limit")
        public void rateLimit() {
            throw new RateLimitExceededException(5);
        }

        @PostMapping("/test/validate")
        public void validate(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/type-mismatch/{id}")
        public void typeMismatch(@PathVariable UUID id) {
        }

        @GetMapping("/test/boom")
        public void boom() {
            throw new RuntimeException("boom");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
